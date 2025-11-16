package org.unitedlands.items.customitems.potions;

import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public abstract class CustomPotion {

    // Called when a player drinks a potion.
    public void onDrink(Player player, ItemStack potionItem, PlayerItemConsumeEvent event) { }

    // Called when a splash potion hits an entity.
    public void onSplash(Player thrower, PotionSplashEvent event) { }

    // Called when an entity is effected by a lingering potion cloud.
    public void onLingeringCloud(Player thrower, AreaEffectCloud cloud, AreaEffectCloudApplyEvent event) { }

}
