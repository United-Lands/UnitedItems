package org.unitedlands.items.customitems.tools;

import java.util.Random;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

public class KrakenSabre extends CustomTool {

    private final Plugin plugin;

    public KrakenSabre(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    // Handle damage logic for the tool.
    public void handleEntityDamage(Player player, EntityDamageByEntityEvent event, EquipmentSlot hand) {

        var rnd = new Random();
        var lightningChance = plugin.getConfig().getDouble("items.kraken-sabre.lightning-chance");
        if (rnd.nextDouble() <= lightningChance)
        {
            var originalDamage = event.getDamage();
            var lightningDamage = plugin.getConfig().getInt("items.kraken-sabre.lightning-damage");
            event.setDamage(originalDamage + lightningDamage);

            var location = event.getEntity().getLocation();
            location.getWorld().strikeLightningEffect(location);
        }


    }
}
