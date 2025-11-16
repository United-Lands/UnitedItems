package org.unitedlands.items.customitems.potions;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.plugin.Plugin;

public class BlastingLingering extends CustomPotion {
    private final Plugin plugin;

    public BlastingLingering(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSplash(Player thrower, PotionSplashEvent event) {
        var loc = event.getEntity().getLocation();
        for (int i = 0; i < 10; i++) {
            int delay = i * 10;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                loc.getWorld().createExplosion(loc, 3F, false, false, thrower);
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.0f);

                // Damage the player if they stand in the cloud.
                double radius = 6.0;
                double radiusSq = radius * radius;
                if (thrower.isOnline() &&
                        thrower.getWorld().equals(loc.getWorld()) &&
                        thrower.getLocation().distanceSquared(loc) <= radiusSq) {

                    double selfDamageHearts = 2.0;
                    thrower.damage(selfDamageHearts, thrower);
                }
            }, delay);
        }
    }
}