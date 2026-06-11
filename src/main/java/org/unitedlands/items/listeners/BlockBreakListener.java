package org.unitedlands.items.listeners;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.inventory.ItemStack;
import org.unitedlands.UnitedLib;
import org.unitedlands.items.UnitedItems;
import org.unitedlands.items.util.LootConfig;

import java.util.*;

public class BlockBreakListener implements Listener {

    private final UnitedItems plugin;
    private final Random random = new Random();
    private Map<String, LootConfig.LootEntry> blockLootMap;

    public BlockBreakListener(UnitedItems plugin) {
        this.plugin = plugin;
        reloadLootConfig();
    }

    public void reloadLootConfig() {
        LootConfig blockLoot = new LootConfig(plugin, "blocks");
        blockLootMap = new HashMap<>();

        for (LootConfig.LootEntry entry : blockLoot.getEntries()) {
            blockLootMap.put(entry.key(), entry);
            }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {

        // Stops blocks dropping items in creative mode, mimicking vanilla mechanics.
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) return;

        handleBlockDrop(event.getBlock());
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        handleBlockDrop(event.getBlock());
    }

    private void handleBlockDrop(Block block) {
        String blockType = block.getType().toString();

        if (!blockLootMap.containsKey(blockType)) return;

        LootConfig.LootEntry entry = blockLootMap.get(blockType);

        if (!entry.enabled()) return;

        List<LootConfig.LootItem> winners = new ArrayList<>();

        for (LootConfig.LootItem lootItem : entry.items()) {
            if (random.nextDouble() * 100 <= lootItem.chance()) {
                winners.add(lootItem);
            }
        }

        if (winners.isEmpty()) return;

        LootConfig.LootItem chosen = winners.get(random.nextInt(winners.size()));
        ItemStack drop = UnitedLib.getInstance()
                .getItemFactory()
                .getItemStack(chosen.itemId(), chosen.minAmount(), chosen.maxAmount());

        if (drop == null) return;

        block.getWorld().dropItemNaturally(block.getLocation(), drop);
    }
}