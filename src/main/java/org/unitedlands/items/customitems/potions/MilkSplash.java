package org.unitedlands.items.customitems.potions;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PotionSplashEvent;

public class MilkSplash extends CustomPotion {

    @Override
    public void onSplash(Player thrower, PotionSplashEvent event) {
        for (LivingEntity target : event.getAffectedEntities()) {
           target.clearActivePotionEffects();
        }
    }

}
