package org.unitedlands.items.commands;

import java.util.Set;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ArmorMeta;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.items.UnitedItems;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.ItemsAdder;

public class UpdateItemCommand implements CommandExecutor {

    @SuppressWarnings("unused")
    private final UnitedItems plugin;

    public UpdateItemCommand(UnitedItems plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
            @NotNull String @NotNull [] args) {

        if (args.length != 0)
            return false;

        var logger = plugin.getLogger();

        Player player = (Player) sender;

        if (player == null)
            return false;

        var heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            player.sendMessage("§cYou must hold an item to update in your main hand.");
            return false;
        }

        var updateSection = plugin.getConfig().getConfigurationSection("update");
        Set<String> keys = updateSection.getKeys(false);

        CustomStack customStack = CustomStack.byItemStack(heldItem);

        if (customStack == null) {
            player.sendMessage("§cThis is not a custom item and can't be updated.");
            return false;
        }

        if (keys.contains(customStack.getNamespacedID())) {
            var targetId = updateSection.getString(customStack.getNamespacedID());

            logger.info("Updating " + customStack.getNamespacedID() + " to " + targetId + "...");
            player.sendMessage("§7Updating item...");

            var newItem = CustomStack.getInstance(targetId).getItemStack();

            var oldItemMeta = heldItem.getItemMeta();
            var newItemMeta = newItem.getItemMeta();

            // Custom name
            newItemMeta.displayName(oldItemMeta.displayName());
            newItem.setItemMeta(newItemMeta);

            // Enchantments
            if (oldItemMeta.hasEnchants()) {
                player.sendMessage("§7Adding enchants...");

                newItemMeta.removeEnchantments();
                var oldEnchants = oldItemMeta.getEnchants();
                for (var enchantSet : oldEnchants.entrySet()) {
                    newItemMeta.addEnchant(enchantSet.getKey(), enchantSet.getValue(), true);
                }
                newItem.setItemMeta(newItemMeta);
            }

            // Armor trims
            if (oldItemMeta instanceof ArmorMeta oldArmorMeta) {

                player.sendMessage("§7Applying trim...");
                var trim = oldArmorMeta.getTrim();

                var newArmorMeta = (ArmorMeta) newItemMeta;
                newArmorMeta.setTrim(trim);
                newItem.setItemMeta(newArmorMeta);
            }

            var oldAmount = player.getInventory().getItemInMainHand().getAmount();
            newItem.setAmount(oldAmount);

            player.getInventory().setItemInMainHand(newItem);

            player.sendMessage("§7Done.");
        } else {
            player.sendMessage("§cThis item can't be updated.");
        }

        return true;
    }

}
