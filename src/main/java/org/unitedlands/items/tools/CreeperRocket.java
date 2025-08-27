package org.unitedlands.items.tools;

import java.util.ArrayList;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import dev.lone.itemsadder.api.CustomStack;

public class CreeperRocket extends CustomTool {

    private final Plugin plugin;

    public CreeperRocket(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handleElytraBoost(Player player, PlayerElytraBoostEvent event) {
        var customRocket = CustomStack.byItemStack(event.getItemStack());
        if (customRocket == null)
            return;

        event.setShouldConsume(false);

        var usages = customRocket.getUsages();

        if (usages == 0) {
            player.sendActionBar(Component.text("§cThe rocket has run out of usages, please refill."));
            event.setCancelled(true);
        } else {
            customRocket.reduceUsages(1);

            ItemStack updatedStack = customRocket.getItemStack();
            var meta = updatedStack.getItemMeta();

            var itemLore = new ArrayList<Component>(meta.lore());
            itemLore.remove(itemLore.size() - 1);
            itemLore.add(itemLore.size(), Component.text("Usages: " + customRocket.getUsages())
                    .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(itemLore);

            updatedStack.setItemMeta(meta);

            if (event.getHand() == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(updatedStack);
            } else {
                player.getInventory().setItemInMainHand(updatedStack);
            }

            var newUsages = customRocket.getUsages();
            var maxUsages = plugin.getConfig().getInt("items.creeper_rocket.max-usages", 256);
            NamedTextColor infoColor = NamedTextColor.GRAY;
            if (newUsages == 0) {
                infoColor = NamedTextColor.RED;
            } else if (newUsages <= 5) {
                infoColor = NamedTextColor.YELLOW;
            }

            player.sendActionBar(
                    Component.text("Creeper Rocket usages: " + newUsages + "/" + maxUsages).color(infoColor));
        }
    }

    @Override
    public void handleInteract(Player player, PlayerInteractEvent event) {
        if (player.isGliding())
            return;

        event.setCancelled(true);

        var refillItemId = plugin.getConfig().getString("items.creeper_rocket.refill-item");
        if (refillItemId == null)
            return;

        var inv = player.getInventory();
        Integer index = null;

        for (int i = 0; i < inv.getSize(); i++) {
            var item = inv.getItem(i);
            var customStack = CustomStack.byItemStack(item);
            if (customStack != null) {
                if (customStack.getNamespacedID().equals(refillItemId)) {
                    index = i;
                    break;
                }
            }
        }

        if (index != null) {
            var customRocket = CustomStack.byItemStack(event.getItem());
            if (customRocket == null)
                return;

            var refillRate = plugin.getConfig().getInt("items.creeper_rocket.usages-per-refill", 1);
            var maxUsages = plugin.getConfig().getInt("items.creeper_rocket.max-usages", 256);
            var currentUsages = customRocket.getUsages();

            if (currentUsages == maxUsages) {
                player.sendActionBar(
                        Component.text("The rocket is already completely filled up!").color(NamedTextColor.GREEN));
                return;
            }

            var item = inv.getItem(index);
            item.setAmount(item.getAmount() - 1);
            if (item.getAmount() == 0)
                inv.setItem((int) index, new ItemStack(Material.AIR));

            var newUsages = Math.min(maxUsages, currentUsages + refillRate);

            customRocket.setUsages(newUsages);
            ItemStack updatedStack = customRocket.getItemStack();
            var meta = updatedStack.getItemMeta();

            var itemLore = new ArrayList<Component>(meta.lore());
            itemLore.remove(itemLore.size() - 1);
            itemLore.add(itemLore.size(), Component.text("Usages: " + customRocket.getUsages())
                    .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(itemLore);

            updatedStack.setItemMeta(meta);

            if (event.getHand() == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(updatedStack);
            } else {
                player.getInventory().setItemInMainHand(updatedStack);
            }
            player.sendActionBar(Component.text("§aRocket refilled (" + newUsages + "/" + maxUsages + " usages).")
                    .color(NamedTextColor.GREEN));

        } else {
            player.sendActionBar(
                    Component.text("You need charged gunpowder in your inventory to refill the Creeper Rocket.")
                            .color(NamedTextColor.YELLOW));
        }
    }

}
