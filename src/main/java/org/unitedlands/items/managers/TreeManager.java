package org.unitedlands.items.managers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.unitedlands.UnitedLib;
import org.unitedlands.items.customitems.saplings.*;
import org.unitedlands.items.util.DataManager;
import org.unitedlands.items.util.PermissionsManager;
import org.unitedlands.utils.Logger;

import java.util.HashMap;
import java.util.Map;

public class TreeManager implements Listener {

    private final Map<String, CustomSapling> saplingSets;
    private final DataManager dataManager;
    private final PermissionsManager permissionsManager;
    private final Plugin plugin;

    public TreeManager(Plugin plugin, PermissionsManager permissionsManager, DataManager dataManager) {
        this.saplingSets = new HashMap<>();
        this.permissionsManager = permissionsManager;
        this.dataManager = dataManager;
        this.plugin = plugin;

        saplingSets.put("ancient_oak_sapling", new AncientOak());
        saplingSets.put("avocado_sapling", new Avocado());
        saplingSets.put("banana_sapling", new Banana());
        saplingSets.put("lemon_sapling", new Lemon());
        saplingSets.put("mango_sapling", new Mango());
        saplingSets.put("midas_jungle_sapling", new MidasJungle());
        saplingSets.put("midas_oak_sapling", new MidasOak());
        saplingSets.put("olive_sapling", new Olive());
        saplingSets.put("orange_sapling", new Orange());
        saplingSets.put("pear_sapling", new Pear());

        dataManager.loadSaplings(saplingSets);

        Bukkit.getScheduler().runTaskLater(plugin,
                () -> Logger.log("Saplings in memory after load: " + dataManager.getSaplingCount()), 100L);
    }

    // Detect if a held item is a custom sapling.
    public CustomSapling detectSapling(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return null;

        return saplingSets.get(UnitedLib.getInstance().getItemFactory().getId(item).toLowerCase());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
    public void onInteractSapling(PlayerInteractEvent event) {
        handleSaplingInteraction(event);
    }

    // Handle sapling placement.
    public void handleSaplingInteraction(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getItem() == null) {
            return;
        }

        CustomSapling sapling = detectSapling(event.getItem());
        if (sapling == null) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();

        if (!permissionsManager.canInteract(event.getPlayer(), clickedBlock)) {
            event.setCancelled(true);
            return;
        }

        if (!(clickedBlock.getType() == Material.GRASS_BLOCK || clickedBlock.getType() == Material.DIRT
                || clickedBlock.getType() == Material.PODZOL || clickedBlock.getType() == Material.SHORT_GRASS
                || clickedBlock.getType() == Material.TALL_GRASS || clickedBlock.getType() == Material.DEAD_BUSH
                || clickedBlock.getType() == Material.SNOW)) {
            event.setCancelled(true);
            return;
        }

        // Check if the clicked block is short grass or tall grass.
        if (clickedBlock.getType() == Material.SHORT_GRASS ||
                clickedBlock.getType() == Material.TALL_GRASS ||
                clickedBlock.getType() == Material.DEAD_BUSH ||
                clickedBlock.getType() == Material.SNOW) {
            clickedBlock.setType(Material.AIR); // Remove the grass
            clickedBlock = clickedBlock.getRelative(0, -1, 0); // Get the block below/
        }

        // Ensure sapling can only be planted on valid ground.
        if (!(clickedBlock.getType() == Material.GRASS_BLOCK || clickedBlock.getType() == Material.DIRT
                || clickedBlock.getType() == Material.PODZOL)) {
            event.setCancelled(true);
            return;
        }

        Biome biome = clickedBlock.getBiome();
        if (!sapling.canGrowInBiome(biome)) {
            event.setCancelled(true);
            return;
        }

        Block above = clickedBlock.getRelative(0, 1, 0);
        if (!above.getType().equals(Material.AIR)) {
            event.setCancelled(true);
            return;
        }

        // Plant the sapling.
        above.setType(sapling.getVanillaSapling());
        dataManager.addSapling(above.getLocation(), sapling);

        // Reduce the item count.
        event.getItem().setAmount(event.getItem().getAmount() - 1);

        // Cancel the event to prevent further processing.
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    // Handle tree construction.
    public void onGrow(StructureGrowEvent event) {

        if (event.isCancelled()) {
            return;
        }

        Location location = event.getLocation().toBlockLocation();
        CustomSapling sapling = dataManager.getSapling(location);

        // Additional checks for jungle saplings, prevents mixed-sapling use to make big tree exploit.
        if (location.getBlock().getType() == Material.JUNGLE_SAPLING) {
            // Look for any 2×2 square that includes this location.
            int[] offs = {0, -1};
            for (int dx : offs)
                for (int dz : offs) {
                    Location base = location.clone().add(dx, 0, dz);
                    Block b00 = base.getBlock();
                    Block b10 = base.clone().add(1, 0, 0).getBlock();
                    Block b01 = base.clone().add(0, 0, 1).getBlock();
                    Block b11 = base.clone().add(1, 0, 1).getBlock();

                    boolean isBigJungle =
                            b00.getType() == Material.JUNGLE_SAPLING &&
                                    b10.getType() == Material.JUNGLE_SAPLING &&
                                    b01.getType() == Material.JUNGLE_SAPLING &&
                                    b11.getType() == Material.JUNGLE_SAPLING;

                    if (isBigJungle) {
                        // Check what custom saplings were recorded at those positions.
                        CustomSapling s00 = dataManager.getSapling(b00.getLocation());
                        CustomSapling s10 = dataManager.getSapling(b10.getLocation());
                        CustomSapling s01 = dataManager.getSapling(b01.getLocation());
                        CustomSapling s11 = dataManager.getSapling(b11.getLocation());

                        boolean anyCustom = s00 != null || s10 != null || s01 != null || s11 != null;
                        boolean allCustom = s00 != null && s10 != null && s01 != null && s11 != null;
                        boolean sameId = allCustom &&
                                s00.getId().equalsIgnoreCase(s10.getId()) &&
                                s00.getId().equalsIgnoreCase(s01.getId()) &&
                                s00.getId().equalsIgnoreCase(s11.getId());

                        if (anyCustom && !sameId) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
        }

        if (sapling != null) {

            event.setCancelled(true); // Stop the vanilla tree from growing.

            Biome biome = location.getBlock().getBiome();

            if (!sapling.canGrowInBiome(biome)) {
                return;
            }

            if (event.getBlocks().size() <= 1) {
                return;
            }

            // Remove the sapling
            location.getBlock().setType(Material.AIR);
            dataManager.removeSapling(location);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {

                for (BlockState block : event.getBlocks()) {

                    Location blockLocation = block.getLocation().toBlockLocation();
                    Material blockMaterial = block.getBlockData().getMaterial();

                    // Create the stem.
                    if (blockMaterial.toString().endsWith("_LOG")) {
                        if (sapling.isUsingVanillaStem()) {
                            blockLocation.getBlock().setType(sapling.getStemBlock());
                        } else if (sapling.getStemReplaceBlockName() != null) {
                            UnitedLib.getInstance().getItemFactory().placeBlock(sapling.getStemReplaceBlockName(), location);
                        }
                    }

                    // Create the leaves.
                    else if (block.getType() == Material.OAK_LEAVES || block.getType() == Material.JUNGLE_LEAVES) {
                        if (sapling.getCustomLeavesName() != null) {
                            blockLocation.getBlock().setType(Material.AIR);
                            String leafType = sapling.isSuccessful() ? sapling.getFruitedLeavesName()
                                    : sapling.getCustomLeavesName();
                            UnitedLib.getInstance().getItemFactory().placeBlock(leafType, location);
                        }
                    }
                }

            }, 1L);

        }
    }

    @EventHandler
    // Handle custom tree leaf decay.
    public void onDecay(LeavesDecayEvent event) {
        CustomSapling sapling = dataManager.getSapling(event.getBlock().getLocation());
        if (sapling != null) {
            event.setCancelled(true);
            event.getBlock().setType(Material.AIR);
        }
    }

    @EventHandler
    // Remove blocks from the map if they're broken.
    public void onTreeBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getBlock().getLocation();
        if (dataManager.hasSapling(loc)) {
            // If the block is being broken by a player without permissions, cancel the
            // event.
            if (!permissionsManager.canInteract(player, event.getBlock())) {
                event.setCancelled(true);
                return;
            }
            // Retrieve the custom sapling registered at this location.
            CustomSapling sapling = dataManager.getSapling(loc);
            // Prevent the default vanilla sapling drop.
            event.setDropItems(false);
            // Set the block to air (in case it hasn't been removed already).
            event.getBlock().setType(Material.AIR);
            // Drop the custom sapling item.
            loc.getWorld().dropItemNaturally(loc, sapling.getSeedItem());
            // Remove the sapling from the DataManager.
            dataManager.removeSapling(loc);
        }
    }


}
