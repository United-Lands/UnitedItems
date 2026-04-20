package org.unitedlands.items.listeners;

import org.bukkit.block.Biome;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerFishEvent.State;
import org.bukkit.inventory.ItemStack;
import org.unitedlands.UnitedLib;
import org.unitedlands.items.UnitedItems;
import org.unitedlands.items.util.LootConfig;
import org.unitedlands.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FishingListener implements Listener {

    private final UnitedItems plugin;
    private final Random random = new Random();

    public FishingListener(UnitedItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {

        Logger.log("PlayerFishEvent", "UnitedItems");

        if (event.getState() != State.CAUGHT_FISH)
            return;
        if (!(event.getCaught() instanceof Item caughtItem))
            return;

        Logger.log("PlayerFishEvent process...", "UnitedItems");

        Player player = event.getPlayer();
        Biome biome = player.getLocation().getBlock().getBiome();

        LootConfig lootConfig = plugin.getFishingLoot();
        List<LootConfig.LootItem> winners = new ArrayList<>();

        for (LootConfig.LootEntry entry : lootConfig.getEntries()) {

            Logger.log("Entry check", "UnitedItems");

            if (!entry.enabled())
                continue;
            if (!entry.biomes().contains(biome))
                continue;

            Logger.log("Entry check 2", "UnitedItems");


            for (LootConfig.LootItem lootItem : entry.items()) {
                if (random.nextDouble() * 100 <= lootItem.chance()) {
                    Logger.log("Adding winner", "UnitedItems");
                    winners.add(lootItem);
                }
            }
        }

        Logger.log("winners " + winners.size(), "UnitedItems");

        if (winners.isEmpty())
            return;

        LootConfig.LootItem chosen = winners.get(random.nextInt(winners.size()));
        ItemStack replacement = UnitedLib.getInstance()
                .getItemFactory()
                .getItemStack(chosen.itemId(), chosen.minAmount(), chosen.maxAmount());

        if (replacement == null)
            return;

        Logger.log("winners " + winners.size(), "UnitedItems");

        caughtItem.setItemStack(replacement);
    }
}