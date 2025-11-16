package org.unitedlands.items.customitems.potions;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Objects;

public class BlastingRegular extends CustomPotion {

    @Override
    public void onDrink(Player player, ItemStack potionItem, PlayerItemConsumeEvent event) {

        Vector launch = new Vector(0, 1.65, 0);
        player.setVelocity(launch);
        player.getWorld().createExplosion(player.getLocation(), 2.0F, false, false, player);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        // Damage the player when they drink the potion, delay by 1 tick to avoid duping potion if they die.
        Bukkit.getScheduler().runTaskLater(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("UnitedItems")), () -> {
            double damage = 2.0;
            player.damage(damage);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
        }, 1L);
    }
}
