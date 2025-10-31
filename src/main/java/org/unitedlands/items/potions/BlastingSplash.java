package org.unitedlands.items.potions;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PotionSplashEvent;

public class BlastingSplash extends CustomPotion {

    @Override
    public void onSplash(Player thrower, PotionSplashEvent event) {
        var loc = event.getEntity().getLocation();
        event.getEntity().getWorld().createExplosion(loc, 3.0F, false, false, thrower);
        event.getEntity().getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

        // Manually damage the thrower if they're in range.
        double radius = 6.0;
        double radiusSq = radius * radius;
        if (thrower.getLocation().distanceSquared(loc) <= radiusSq) {
            double selfDamageHearts = 2.0;
            thrower.damage(selfDamageHearts, thrower);
        }
    }

}
