package org.unitedlands.items.customitems.tools;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GingerbreadTools extends CustomTool implements Listener {

    @Override
    public void handleEntityDamage(Player player, EntityDamageByEntityEvent event, EquipmentSlot hand) {
        // When a hit is landed, give the player speed and jump boosts.
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 60, 0));
    }
}
