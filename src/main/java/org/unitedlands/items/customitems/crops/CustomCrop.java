package org.unitedlands.items.customitems.crops;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Farmland;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.unitedlands.UnitedLib;
import org.unitedlands.items.UnitedItems;
import org.unitedlands.items.util.DataManager;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Objects;

public class CustomCrop {

    private final UnitedItems plugin;
    private static final Random random = new Random();

    private String id;
    private List<String> growthStages;
    private String finalStage;
    private Set<Material> validSoils;
    private String seedItemId;
    private Set<Biome> allowedBiomes;
    private boolean isPersistentHarvest;
    private String dropItem;
    private int dropAmount;

    public CustomCrop(UnitedItems plugin, String key) {
        this.plugin = plugin;
        loadCrop(key);
    }

    public void loadCrop(String key) {

        var config = plugin.getCropsConfig().get();

        this.id = key;
        this.growthStages = config.getStringList(key + ".stages.growth");
        this.finalStage = config.getString(key + ".stages.final");

        Set<Material> validSoils = new HashSet<>();
        for (String soil : config.getStringList(key + ".valid-soils")) {
            validSoils.add(Material.getMaterial(soil));
        }
        this.validSoils = validSoils;
        this.seedItemId = config.getString(key + ".seed-item");

        Registry<Biome> biomeRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
        Set<Biome> allowedBiomes = new HashSet<>();
        for (String biome : config.getStringList(key + ".allowed-biomes")) {
            var biomeKey = biome.toLowerCase();
            Biome optionalBiome = biomeRegistry.get(new NamespacedKey("minecraft", biomeKey));
            if (optionalBiome != null)
                allowedBiomes.add(optionalBiome);
        }
        this.allowedBiomes = allowedBiomes;

        this.isPersistentHarvest = config.getBoolean(key + ".persistent-harvest");
        this.dropItem = config.getString(key + ".drop-item");
        this.dropAmount = config.getInt(key + ".drop-amount");
    }

    public void startRandomGrowthTask(Location location, DataManager dataManager) {
        // Base delay: 180-300 seconds.
        int baseDelaySeconds = 180 + random.nextInt(120);
        int randomDelay = 20 * baseDelaySeconds;

        // Check the block below the crop for farmland hydration.
        Block blockBelow = location.clone().add(0, -1, 0).getBlock();
        if (blockBelow.getType() == Material.FARMLAND) {
            Farmland farmland = (Farmland) blockBelow.getBlockData();
            // If the farmland is dry, double the delay.
            if (farmland.getMoisture() == 0) {
                randomDelay *= 2;
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                // Cancel the task if the crop has been removed.
                if (!dataManager.hasCrop(location)) {
                    cancel();
                    return;
                }

                CustomCrop crop = dataManager.getCrop(location);
                int growthStage = dataManager.getCropStage(location);

                if (growthStage < crop.getMaxGrowthStage()) {
                    crop.placeCrop(location, growthStage + 1);
                    dataManager.updateCropStage(location, growthStage + 1);

                    // Reschedule another growth cycle with the updated delay.
                    startRandomGrowthTask(location, dataManager);
                } else {
                    cancel(); // Crop is fully grown, stop task.
                }
            }
        }.runTaskLater(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("UnitedItems")), randomDelay);
    }

    public String getId() {
        return id;
    }

    public boolean canGrowInBiome(Biome biome) {
        return allowedBiomes == null || allowedBiomes.isEmpty() || allowedBiomes.contains(biome);
    }

    public boolean canBePlantedOn(Material material) {
        return validSoils.contains(material);
    }

    public String getNextGrowthStage(int stage) {
        return (stage < growthStages.size() - 1) ? growthStages.get(stage + 1) : finalStage;
    }

    public boolean isFullyGrown(int stage) {
        return stage >= getMaxGrowthStage();
    }

    public void placeCrop(Location location, int growthStage) {
        int adjustedStage = Math.max(growthStage - 1, 0);
        if (adjustedStage < growthStages.size()) {
            UnitedLib.getInstance().getItemFactory().placeBlock(growthStages.get(adjustedStage), location);
        } else {
            UnitedLib.getInstance().getItemFactory().placeBlock(finalStage, location);
        }
    }

    public String getSeedItemId() {
        return seedItemId;
    }

    public boolean canBeHarvestedWithoutBreaking() {
        return isPersistentHarvest;
    }

    public void harvestWithoutBreaking(Location location, Player player, DataManager dataManager) {
        if (!canBeHarvestedWithoutBreaking())
            return;

        int growthStage = dataManager.getCropStage(location);

        if (growthStage != getMaxGrowthStage()) {
            return;
        }

        for (ItemStack drop : getHarvestDrops()) {
            location.getWorld().dropItemNaturally(location.clone().add(0.5, 0.5, 0.5), drop);
        }

        // Reset to Stage 1
        dataManager.updateCropStage(location, 1);
        placeCrop(location, 1);

        // Restart natural growth.
        Bukkit.getScheduler().runTaskLater(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("UnitedItems")),
                () -> startRandomGrowthTask(location, dataManager), 20 * 5); // 5-second delay before regrowing starts.
    }

    public List<ItemStack> getHarvestDrops() {
        var dropItem = UnitedLib.getInstance().getItemFactory().getItemStack(this.dropItem, dropAmount);
        if (dropItem == null)
            return new ArrayList<>();
        return List.of(dropItem);
    }

    public int getMaxGrowthStage() {
        return growthStages.size() + 1;
    }

    public void onPlant(Player player, Location location) {

    }

    public void onGrow(Location location) {

    }

    public void onPrematureHarvest(Location location) {
        var seed = UnitedLib.getInstance().getItemFactory().getItemStack(seedItemId, 1);
        if (seed != null) {
            location.getWorld().dropItemNaturally(location, seed);
        }
    }

    public void onHarvest(Location location) {
        var seed = UnitedLib.getInstance().getItemFactory().getItemStack(seedItemId, 1, getMaxGrowthStage() + 1);
        if (seed != null) {
            location.getWorld().dropItemNaturally(location, seed);
        }
        var drop = UnitedLib.getInstance().getItemFactory().getItemStack(dropItem, dropAmount);
        if (seed != null) {
            location.getWorld().dropItemNaturally(location, drop);
        }
    }
}
