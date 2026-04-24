package org.unitedlands.items.listeners;

import org.bukkit.block.Biome;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.unitedlands.UnitedLib;
import org.unitedlands.items.UnitedItems;
import org.unitedlands.items.util.LootConfig;
import org.unitedlands.items.util.LootConfig.LootEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MobKillListener implements Listener {

    private final UnitedItems plugin;
    private final Random random = new Random();

    private LootConfig mobLoots;
    private Map<String, LootEntry> mobLootMap;

    public MobKillListener(UnitedItems plugin) {
        this.plugin = plugin;
        reloadLootConfig();
    }

    public void reloadLootConfig() {

        mobLoots = new LootConfig(plugin, "mobs");
        mobLootMap = new HashMap<>();

        for (var entry : mobLoots.getEntries())
            mobLootMap.put(entry.key(), entry);
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {

        if (!(event.getEntity() instanceof LivingEntity) || (event.getEntity() instanceof Player))
            return;

        if (event.getDamageSource() == null || event.getDamageSource().getCausingEntity() == null
                || !(event.getDamageSource().getCausingEntity() instanceof Player))
            return;

        var entityType = event.getEntityType().toString();

        if (!mobLootMap.containsKey(entityType))
            return;

        Biome biome = event.getEntity().getLocation().getBlock().getBiome();

        List<LootConfig.LootItem> winners = new ArrayList<>();
        var entry = mobLootMap.get(entityType);

        if (!entry.enabled())
            return;
        if (entry.biomes() != null && !entry.biomes().isEmpty()) {
            if (!entry.biomes().contains(biome))
                return;
        }

        for (LootConfig.LootItem lootItem : entry.items()) {
            if (random.nextDouble() * 100 <= lootItem.chance()) {
                winners.add(lootItem);
            }
        }

        if (winners.isEmpty())
            return;

        LootConfig.LootItem chosen = winners.get(random.nextInt(winners.size()));
        ItemStack drop = UnitedLib.getInstance()
                .getItemFactory()
                .getItemStack(chosen.itemId(), chosen.minAmount(), chosen.maxAmount());

        if (drop == null)
            return;

        event.getDrops().add(drop);
    }
}