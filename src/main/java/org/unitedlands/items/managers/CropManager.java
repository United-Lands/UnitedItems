package org.unitedlands.items.managers;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.unitedlands.items.customitems.crops.*;
import org.unitedlands.items.util.DataManager;
import org.unitedlands.items.util.PermissionsManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import static org.unitedlands.items.util.DataManager.log;

public class CropManager implements Listener {

    private final Map<String, CustomCrop> cropSets = new HashMap<>();
    private final DataManager dataManager;
    private final PermissionsManager permissionsManager;

    public CropManager(PermissionsManager permissionsManager, Plugin plugin, DataManager dataManager) {
        this.permissionsManager = permissionsManager;
        this.dataManager = dataManager;

        cropSets.put("bellpepper", new BellPepper());
        cropSets.put("blueberry", new Blueberry());
        cropSets.put("broccoli", new Broccoli());
        cropSets.put("celery", new Celery());
        cropSets.put("chilipepper", new ChiliPepper());
        cropSets.put("coffeeplant", new CoffeePlant());
        cropSets.put("corn", new Corn());
        cropSets.put("cucumber", new Cucumber());
        cropSets.put("garlic", new Garlic());
        cropSets.put("grapes", new Grapes());
        cropSets.put("lettuce", new Lettuce());
        cropSets.put("onion", new Onion());
        cropSets.put("pea", new Pea());
        cropSets.put("peanut", new Peanut());
        cropSets.put("pineapple", new Pineapple());
        cropSets.put("raspberry", new Raspberry());
        cropSets.put("rice", new Rice());
        cropSets.put("soybean", new SoyBean());
        cropSets.put("strawberry", new Strawberry());
        cropSets.put("tomato", new Tomato());

        dataManager.loadCrops(cropSets);

        Bukkit.getScheduler().runTaskLater(plugin,
                () -> log("Crops in memory after load: " + dataManager.getCropCount()), 100L);
    }

    // Detect if a crop is custom and what it is.
    public CustomCrop detectCrop(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return null;
        CustomStack customStack = CustomStack.byItemStack(item);

        if (customStack == null)
            return null;

        // Check if item is a seed instead of a fully-grown crop.
        for (CustomCrop crop : cropSets.values()) {
            if (customStack.getId().equalsIgnoreCase(crop.getSeedItemId())) {
                return crop; // Return the crop that corresponds to this seed.
            }
        }
        return null;
    }

    // Main method to handle crop interactions.
    // Prioritise event to ensure proper persistent harvesting logic.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void handleCropInteraction(PlayerInteractEvent event) {

        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            // Only process the off-hand if the main hand is empty.
            ItemStack mainHandItem = event.getPlayer().getInventory().getItemInMainHand();
            if (mainHandItem.getType() != Material.AIR) {
                return;
            }
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null)
            return;

        Location loc = clickedBlock.getLocation();

        // If this block is a registered crop...
        if (dataManager.hasCrop(loc)) {

            CustomCrop growingCrop = dataManager.getCrop(loc);
            int growthStage = dataManager.getCropStage(loc);

            // If the player is not in an area with sufficient permissions, cancel.
            if (!permissionsManager.canInteract(event.getPlayer(), clickedBlock)) {
                event.setCancelled(true);
                return;
            }

            // If the crop is fully grown but persistent harvest is disabled,
            // cancel the event immediately so nothing happens.
            if (growthStage == growingCrop.getMaxGrowthStage() && !growingCrop.canBeHarvestedWithoutBreaking()) {
                event.setCancelled(true);
                return;
            }

            ItemStack heldItem = event.getItem();
            boolean isHoldingBonemeal = heldItem != null && heldItem.getType() == Material.BONE_MEAL;

            // Handle bonemeal usage.
            if (isHoldingBonemeal) {
                if (growthStage < growingCrop.getMaxGrowthStage()) {
                    int newStage = growthStage + 1;
                    growingCrop.placeCrop(loc, newStage);
                    dataManager.updateCropStage(loc, newStage);
                    heldItem.setAmount(heldItem.getAmount() - 1);
                    loc.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, loc.clone().add(0.5, 0.5, 0.5), 10,
                            0.3, 0.3, 0.3);
                    loc.getWorld().playSound(loc, org.bukkit.Sound.ITEM_BONE_MEAL_USE, 1.0f, 1.0f);
                    event.setCancelled(true);
                    return;
                }
                // If bonemeal is applied on a fully grown crop and persistent harvest is
                // enabled, harvest it.
                if (growthStage == growingCrop.getMaxGrowthStage() && growingCrop.canBeHarvestedWithoutBreaking()) {
                    growingCrop.harvestWithoutBreaking(loc, event.getPlayer(), dataManager);
                    growingCrop.placeCrop(loc, 1);
                    dataManager.updateCropStage(loc, 1);
                    growingCrop.startRandomGrowthTask(loc, dataManager);
                    loc.getWorld().playSound(loc, org.bukkit.Sound.BLOCK_CROP_BREAK, 1.0f, 1.0f);
                    event.setCancelled(true);
                    return;
                }
            }

            // If not holding bonemeal and the crop is fully grown and harvestable, process
            // harvest.
            if (growthStage == growingCrop.getMaxGrowthStage() && growingCrop.canBeHarvestedWithoutBreaking()) {
                growingCrop.harvestWithoutBreaking(loc, event.getPlayer(), dataManager);
                growingCrop.placeCrop(loc, 1);
                dataManager.updateCropStage(loc, 1);
                growingCrop.startRandomGrowthTask(loc, dataManager);
                event.setCancelled(true);
                return;
            }
        }

        // Handle planting new crops.
        CustomCrop crop = detectCrop(event.getItem());
        if (crop == null)
            return;

        // Permissions to plant?
        if (!permissionsManager.canInteract(event.getPlayer(), clickedBlock)) {
            event.setCancelled(true);
            return;
        }

        if (!crop.canBePlantedOn(clickedBlock.getType()))
            return;
        Block above = clickedBlock.getRelative(0, 1, 0);
        if (!above.getType().equals(Material.AIR))
            return;

        Biome biome = above.getBiome();
        if (!crop.canGrowInBiome(biome)) {
            event.setCancelled(true);
            return;
        }

        crop.placeCrop(above.getLocation(), 1);
        dataManager.addCrop(above.getLocation(), crop, 1);
        above.getWorld().playSound(above.getLocation(), org.bukkit.Sound.ITEM_CROP_PLANT, 1.0f, 1.0f);
        crop.startRandomGrowthTask(above.getLocation(), dataManager);

        Objects.requireNonNull(event.getItem()).setAmount(event.getItem().getAmount() - 1);
        event.setCancelled(true);
    }

    @EventHandler
    // Handle breaking crops.
    public void onCropBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (!dataManager.hasCrop(loc))
            return;
        Player player = event.getPlayer();
        CustomCrop crop = dataManager.getCrop(loc);
        int growthStage = dataManager.getCropStage(loc);

        // If fully grown, harvest it.
        if (crop.isFullyGrown(growthStage)) {
            crop.onHarvest(loc, player);
            dataManager.removeCrop(loc);
        } else {
            // Otherwise, break the crop.
            dataManager.removeCrop(loc);
            loc.getBlock().setType(Material.AIR);
        }
    }

    @EventHandler(ignoreCancelled = true)
    // Check if entity-made explosions can break crops.
    public void onEntityExplode(EntityExplodeEvent event) {
        // Try to see if a player caused this (wind charge, projectile...)
        Player sourcePlayer = null;
        if (event.getEntity() instanceof Projectile projectile &&
                projectile.getShooter() instanceof Player shooter) {
            sourcePlayer = shooter;
        }

        // Get all blocks the explosion wants to break
        Iterator<Block> it = event.blockList().iterator();
        while (it.hasNext()) {
            Block b = it.next();
            Location loc = b.getLocation();

            // See if any are custom crops.
            if (!dataManager.hasCrop(loc)) {
                continue;
            }

            // Safety, if cause is unknown then don't break.
            if (sourcePlayer == null) {
                it.remove();
                continue;
            }

            // If the player isn't trusted, don't let the explosion break the crop.
            if (!permissionsManager.canInteract(sourcePlayer, b)) {
                it.remove();
            }
        }
    }

    @EventHandler
    // Stops dry farmland from turning to dirt if not hydrated, mimicking vanilla
    // behaviour.
    public void onFarmlandDry(BlockFadeEvent event) {
        // Check if the block that is about to fade is farmland.
        if (event.getBlock().getType() == Material.FARMLAND) {
            Location cropLocation = event.getBlock().getLocation().clone().add(0, 1, 0);
            // If there's a custom crop registered at this location, cancel the drying.
            if (dataManager.hasCrop(cropLocation)) {
                event.setCancelled(true);
            }
        }
    }


}
