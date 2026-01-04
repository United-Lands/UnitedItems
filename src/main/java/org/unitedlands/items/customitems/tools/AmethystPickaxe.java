package org.unitedlands.items.customitems.tools;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class AmethystPickaxe extends CustomTool implements Listener {

    @Override
    public void handleBlockBreak(Player player, BlockBreakEvent event, EquipmentSlot hand) {
        Block block = event.getBlock();
        if (block.getType() == Material.BUDDING_AMETHYST) {
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.BUDDING_AMETHYST));
        }
    }
}