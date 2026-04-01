package org.unitedlands.items.customitems.potions;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;

public class MilkRegular extends CustomPotion {

    @Override
    public void onDrink(Player player, ItemStack potionItem, PlayerItemConsumeEvent event) {
        player.clearActivePotionEffects();       
    }
}
