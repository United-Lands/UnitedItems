package org.unitedlands.items.util;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.unitedlands.items.UnitedItems;
import org.unitedlands.utils.Logger;
import org.unitedlands.utils.Messenger;

import java.util.Set;

public final class ItemUpdater {

    private ItemUpdater() { }

    public static ItemStack updateItem(
            UnitedItems plugin,
            MessageProvider messageProvider,
            Player player,
            ItemStack original,
            boolean sendMessages
    ) {
        if (original == null || original.getType() == Material.AIR) {
            if (sendMessages && messageProvider != null) {
                Messenger.sendMessage(player, messageProvider.get("messages.update-no-item"), null,
                        messageProvider.get("messages.prefix"));
            }
            return original;
        }

        ConfigurationSection updateSection = plugin.getConfig().getConfigurationSection("update");
        if (updateSection == null) {
            return original;
        }
        Set<String> keys = updateSection.getKeys(false);

        CustomStack customStack = CustomStack.byItemStack(original);
        if (customStack == null) {
            if (sendMessages && messageProvider != null) {
                Messenger.sendMessage(player, messageProvider.get("messages.update-no-custom-item"), null,
                        messageProvider.get("messages.prefix"));
            }
            return original;
        }

        String fromId = customStack.getNamespacedID();
        if (!keys.contains(fromId)) {
            if (sendMessages && messageProvider != null) {
                Messenger.sendMessage(player, messageProvider.get("messages.update-error"), null,
                        messageProvider.get("messages.prefix"));
            }
            return original;
        }

        String targetId = updateSection.getString(fromId);
        if (targetId == null) {
            if (sendMessages && messageProvider != null) {
                Messenger.sendMessage(player, messageProvider.get("messages.update-error"), null,
                        messageProvider.get("messages.prefix"));
            }
            return original;
        }

        if (sendMessages && messageProvider != null) {
            Logger.log("Updating " + fromId + " to " + targetId + "...");
            Messenger.sendMessage(player, messageProvider.get("messages.update-updating"), null,
                    messageProvider.get("messages.prefix"));
        }

        ItemStack newItem = CustomStack.getInstance(targetId).getItemStack();

        ItemMeta oldMeta = original.getItemMeta();
        ItemMeta newMeta = newItem.getItemMeta();

        if (oldMeta != null && newMeta != null) {
            // Custom name
            newMeta.displayName(oldMeta.displayName());
            newItem.setItemMeta(newMeta);

            // Enchantments
            if (oldMeta.hasEnchants()) {
                if (sendMessages && messageProvider != null) {
                    Messenger.sendMessage(player, messageProvider.get("messages.update-enchants"), null,
                            messageProvider.get("messages.prefix"));
                }
                newMeta.removeEnchantments(); // Paper API
                var oldEnchants = oldMeta.getEnchants();
                for (var ench : oldEnchants.entrySet()) {
                    newMeta.addEnchant(ench.getKey(), ench.getValue(), true);
                }
                newItem.setItemMeta(newMeta);
            }

            // Damage
            if (oldMeta instanceof Damageable oldDamageable && newMeta instanceof Damageable newDamageable) {
                if (sendMessages && messageProvider != null) {
                    Messenger.sendMessage(player, messageProvider.get("messages.update-damage"), null,
                            messageProvider.get("messages.prefix"));
                }
                int damage = oldDamageable.getDamage();
                newDamageable.setDamage(damage);
                newItem.setItemMeta((ItemMeta) newDamageable);
            }

            // Armor trims
            if (oldMeta instanceof ArmorMeta oldArmor && newMeta instanceof ArmorMeta newArmor) {
                if (sendMessages && messageProvider != null) {
                    Messenger.sendMessage(player, messageProvider.get("messages.update-trim"), null,
                            messageProvider.get("messages.prefix"));
                }
                newArmor.setTrim(oldArmor.getTrim());
                newItem.setItemMeta(newArmor);
            }
        }

        newItem.setAmount(original.getAmount());

        if (sendMessages && messageProvider != null) {
            Messenger.sendMessage(player, messageProvider.get("messages.update-done"), null,
                    messageProvider.get("messages.prefix"));
        }

        return newItem;
    }
}