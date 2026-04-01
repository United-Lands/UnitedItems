package org.unitedlands.items.customitems.potions;

import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;

public class MilkLingering extends CustomPotion {
    @Override
    public void onLingeringCloud(Player thrower, AreaEffectCloud cloud, AreaEffectCloudApplyEvent event) {
        for (LivingEntity target : event.getAffectedEntities()) {
            target.clearActivePotionEffects();
        }
    }
}