package org.unitedlands.items.customitems.armours;

import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.unitedlands.UnitedLib;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class NecromancerArmour extends CustomArmour {

    private final Plugin plugin;
    private final NamespacedKey revivesUsedKey;

    public NecromancerArmour(Plugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.revivesUsedKey = new NamespacedKey(plugin, "necromancer_revives_used");
    }

    @Override
    public void handleResurrect(Player player, EntityResurrectEvent event) {
        // Event is cancelled if they are revived for other reasons (totem).
        // We only want to fire LAST.
        if (!event.isCancelled()) return;

        int maxRevives = plugin.getConfig().getInt("items.necromancer-armour.max-revives");

        // Fetch how many times the player has already revived this life.
        Integer usedRevives = player.getPersistentDataContainer().get(revivesUsedKey, PersistentDataType.INTEGER);
        if (usedRevives == null) {
            usedRevives = 0;
        }

        // If they have hit their limit, let them die normally.
        if (usedRevives >= maxRevives) return;

        // Un-cancel the event to forcefully trigger the resurrection.
        event.setCancelled(false);

        // Increment the revive counter for this life.
        int newUsedCount = usedRevives + 1;
        player.getPersistentDataContainer().set(revivesUsedKey, PersistentDataType.INTEGER, newUsedCount);

        // Fun animations.
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.1);
        player.sendMessage("§c§lU§f§lL §7§lDeaths? §8§l» §5You elude Death... for now.");
    }


    @Override
    public void handleRespawn(Player player) {
        // Clears the revive cooldown whenever the player respawns after an actual death.
        player.getPersistentDataContainer().remove(revivesUsedKey);
    }

    @Override
    public void handleTarget(Player player, EntityTargetLivingEntityEvent event) {
        org.bukkit.entity.Entity hunter = event.getEntity();
        List<String> ignoredTypes = plugin.getConfig().getStringList("items.necromancer-armour.ignored-mobs");

        // Check if the entity type matches the config list
        if (ignoredTypes.contains(hunter.getType().name())) {
            // Allow targeting if the player specifically provoked them.
            if (event.getReason() == EntityTargetLivingEntityEvent.TargetReason.TARGET_ATTACKED_ENTITY) {
                return;
            }

            // Otherwise, they ignore the player.
            event.setCancelled(true);
        }
    }

    @Override
    public void handleMobKill(Player killer, EntityDeathEvent event) {
        ConfigurationSection assistantSection = plugin.getConfig().getConfigurationSection("items.necromancer-armour.assistants");
        if (assistantSection == null) return;

        var assistants = new ArrayList<>(assistantSection.getKeys(false));
        if (assistants.isEmpty()) return;

        String randomAssistant = assistants.get(ThreadLocalRandom.current().nextInt(0, assistants.size()));

        double chance = plugin.getConfig().getDouble("items.necromancer-armour.assistants." + randomAssistant + ".spawn-chance", 0.25);
        if (ThreadLocalRandom.current().nextDouble() > chance) return;

        int amount = plugin.getConfig().getInt("items.necromancer-armour.assistants." + randomAssistant + ".amount", 1);

        org.bukkit.Location loc = event.getEntity().getLocation();

        for (int i = 0; i < amount; i++) {
            UnitedLib.getInstance().getMobFactory().createMobAtLocation(randomAssistant, loc, killer, 1);
        }

        loc.getWorld().spawnParticle(Particle.SOUL, loc.add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.05);
        loc.getWorld().playSound(loc, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.5f, 0.5f);
    }
}