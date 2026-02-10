package org.unitedlands.items.customitems.tools;

import java.util.ArrayList;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.unitedlands.UnitedLib;

import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class CreeperRocket extends CustomTool {

    private final Plugin plugin;
    private final String KEY = "creeper-rocket-usages";

    public CreeperRocket(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handleElytraBoost(Player player, PlayerElytraBoostEvent event, EquipmentSlot hand) {

        var rocket = event.getItemStack();
        if (!UnitedLib.getInstance().getItemFactory().isCustomItem(rocket))
            return;

        event.setShouldConsume(false);

        var usages = getRocketUsages(rocket);

        if (usages == 0) {
            player.sendActionBar(Component.text("§cThe rocket has run out of usages, please refill."));
            event.setCancelled(true);
        } else {

            usages--;
            setRocketUsages(rocket, usages);

            var meta = rocket.getItemMeta();

            var itemLore = new ArrayList<Component>(meta.lore());
            itemLore.remove(itemLore.size() - 1);
            itemLore.add(itemLore.size(), Component.text("Usages: " + usages)
                    .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(itemLore);

            rocket.setItemMeta(meta);

            if (event.getHand() == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(rocket);
            } else {
                player.getInventory().setItemInMainHand(rocket);
            }

            var maxUsages = plugin.getConfig().getInt("items.creeper_rocket.max-usages", 256);
            NamedTextColor infoColor = NamedTextColor.GRAY;
            if (usages == 0) {
                infoColor = NamedTextColor.RED;
            } else if (usages <= 5) {
                infoColor = NamedTextColor.YELLOW;
            }

            player.sendActionBar(
                    Component.text("Creeper Rocket usages: " + usages + "/" + maxUsages).color(infoColor));
        }
    }

    @Override
    public void handleInteract(Player player, PlayerInteractEvent event, EquipmentSlot hand) {
        if (player.isGliding())
            return;

        event.setCancelled(true);

        var refillItemId = plugin.getConfig().getString("items.creeper_rocket.refill-item");
        if (refillItemId == null)
            return;

        var inv = player.getInventory();
        Integer index = null;

        var itemFactory = UnitedLib.getInstance().getItemFactory();
        for (int i = 0; i < inv.getSize(); i++) {
            var item = inv.getItem(i);
            var itemId = itemFactory.getId(item);
            if (itemId.equals(refillItemId)) {
                index = i;
                break;
            }
        }

        if (index != null) {
            var rocket = event.getItem();

            var refillRate = plugin.getConfig().getInt("items.creeper_rocket.usages-per-refill", 1);
            var maxUsages = plugin.getConfig().getInt("items.creeper_rocket.max-usages", 256);
            var currentUsages = getRocketUsages(rocket);

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

            setRocketUsages(rocket, newUsages);
            var meta = rocket.getItemMeta();

            var itemLore = new ArrayList<Component>(meta.lore());
            itemLore.remove(itemLore.size() - 1);
            itemLore.add(itemLore.size(), Component.text("Usages: " + newUsages)
                    .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            meta.lore(itemLore);

            rocket.setItemMeta(meta);

            if (event.getHand() == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(rocket);
            } else {
                player.getInventory().setItemInMainHand(rocket);
            }
            player.sendActionBar(Component.text("§aRocket refilled (" + newUsages + "/" + maxUsages + " usages).")
                    .color(NamedTextColor.GREEN));

        } else {
            player.sendActionBar(
                    Component.text("You need charged gunpowder in your inventory to refill the Creeper Rocket.")
                            .color(NamedTextColor.YELLOW));
        }
    }

    private int getRocketUsages(ItemStack item) {
        var pdc = item.getItemMeta().getPersistentDataContainer();
        var key = new NamespacedKey(plugin, KEY);
        return pdc.getOrDefault(key, PersistentDataType.INTEGER, 0);
    }

    private void setRocketUsages(ItemStack item, int usages) {
        var pdc = item.getItemMeta().getPersistentDataContainer();
        var key = new NamespacedKey(plugin, KEY);
        pdc.set(key, PersistentDataType.INTEGER, usages);
    }

}
