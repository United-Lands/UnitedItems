package org.unitedlands.items.customitems.armours;

import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.List;

public abstract class CustomArmour {

    // Apply effects to the player
    public void applyEffects(Player player) {
    }

    // Get the list of potion effects applied by this armor
    public List<PotionEffectType> getAppliedEffects() {
        return Collections.emptyList();
    }

    // Handle damage logic for the armour.
    public void handlePlayerDamage(Player player, EntityDamageEvent event) {
    }

    // Handle exp pickup logic for the armour.
    public void handleExpPickup(Player player, ExperienceOrb experienceOrb) {
    }

    // Handle resurrect logic for the armour.
    public void handleResurrect(Player player, EntityResurrectEvent event) {
    }

    // Handle respawn logic for the armour.
    public void handleRespawn(Player player) {
    }

    // Handle entity targeting logic for the armour.
    public void handleTarget(Player player, EntityTargetLivingEntityEvent event) {
    }

    // Handle entity death (mob kill) logic for the armour.
    public void handleMobKill(Player killer, EntityDeathEvent event) {
    }
}