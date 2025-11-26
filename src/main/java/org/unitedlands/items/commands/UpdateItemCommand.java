package org.unitedlands.items.commands;

import java.util.Set;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.Damageable;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.items.UnitedItems;
import org.unitedlands.items.util.MessageProvider;
import org.unitedlands.utils.Logger;
import org.unitedlands.utils.Messenger;

import dev.lone.itemsadder.api.CustomStack;

public class UpdateItemCommand implements CommandExecutor {

    @SuppressWarnings("unused")
    private final UnitedItems plugin;
    private final MessageProvider messageProvider;

    public UpdateItemCommand(UnitedItems plugin, MessageProvider messageProvider) {
        this.plugin = plugin;
        this.messageProvider = messageProvider;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
            @NotNull String @NotNull [] args) {

        if (args.length != 0)
            return false;

        Player player = (Player) sender;

        if (player == null)
            return false;

        var heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            Messenger.sendMessage(player, messageProvider.get("messages.update-no-item"), null,
                    messageProvider.get("messages.prefix"));
            return false;
        }

        var updateSection = plugin.getConfig().getConfigurationSection("update");
        Set<String> keys = updateSection.getKeys(false);

        CustomStack customStack = CustomStack.byItemStack(heldItem);

        if (customStack == null) {
            Messenger.sendMessage(player, messageProvider.get("messages.update-no-custom-item"), null,
                    messageProvider.get("messages.prefix"));
            return false;
        }

        if (keys.contains(customStack.getNamespacedID())) {
            var targetId = updateSection.getString(customStack.getNamespacedID());

            Logger.log("Updating " + customStack.getNamespacedID() + " to " + targetId + "...", "UnitedItems");
            Messenger.sendMessage(player, messageProvider.get("messages.update-updating"), null,
                    messageProvider.get("messages.prefix"));

            var newItem = CustomStack.getInstance(targetId).getItemStack();

            var oldItemMeta = heldItem.getItemMeta();
            var newItemMeta = newItem.getItemMeta();

            // Custom name
            newItemMeta.displayName(oldItemMeta.displayName());
            newItem.setItemMeta(newItemMeta);

            // Enchantments
            if (oldItemMeta.hasEnchants()) {
                Messenger.sendMessage(player, messageProvider.get("messages.update-updating"), null,
                        messageProvider.get("messages.prefix"));

                newItemMeta.removeEnchantments();
                var oldEnchants = oldItemMeta.getEnchants();
                for (var enchantSet : oldEnchants.entrySet()) {
                    newItemMeta.addEnchant(enchantSet.getKey(), enchantSet.getValue(), true);
                }
                newItem.setItemMeta(newItemMeta);
            }

            // Damage
            if (oldItemMeta instanceof Damageable oldDamageableMeta) {
                Messenger.sendMessage(player, messageProvider.get("messages.update-damage"), null, messageProvider.get("messages.prefix"));
                var damage = oldDamageableMeta.getDamage();

                var newDamageable = (Damageable) newItemMeta;
                newDamageable.setDamage(damage);
                newItem.setItemMeta(newDamageable);
            }

            // Armor trims
            if (oldItemMeta instanceof ArmorMeta oldArmorMeta) {
                Messenger.sendMessage(player, messageProvider.get("messages.update-trim"), null,
                        messageProvider.get("messages.prefix"));
                var trim = oldArmorMeta.getTrim();

                var newArmorMeta = (ArmorMeta) newItemMeta;
                newArmorMeta.setTrim(trim);
                newItem.setItemMeta(newArmorMeta);
            }

            var oldAmount = player.getInventory().getItemInMainHand().getAmount();
            newItem.setAmount(oldAmount);

            player.getInventory().setItemInMainHand(newItem);

            Messenger.sendMessage(player, messageProvider.get("messages.update-done"), null,
                    messageProvider.get("messages.prefix"));
        } else {
            Messenger.sendMessage(player, messageProvider.get("messages.update-error"), null,
                    messageProvider.get("messages.prefix"));
        }

        return true;
    }

}
