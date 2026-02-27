package org.unitedlands.items.customitems.tools;

import java.util.Random;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class BananaSabre extends CustomTool {

    private final Plugin plugin;

    public BananaSabre(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    // Handle damage logic for the tool.
    public void handleEntityDamage(Player player, EntityDamageByEntityEvent event, EquipmentSlot hand) {

        var rnd = new Random();
        var slipChance = plugin.getConfig().getDouble("items.banana-sabre.slip-chance");
        if (rnd.nextDouble() <= slipChance) {

            var strength = plugin.getConfig().getInt("items.banana-sabre.slip-strength");
            var slipper = event.getEntity();

            Vector viewDirection = player.getLocation().getDirection();
            Vector randomSlipDirection = new Vector();

            // Randomly slip the target either left or right along the orthogonal vector to the view direction
            if (rnd.nextDouble() > 0.5) {
                randomSlipDirection = new Vector(-1 * viewDirection.getZ(), 0, viewDirection.getX());
            } else {
                randomSlipDirection = new Vector(viewDirection.getZ(), 0, -1 * viewDirection.getX());
            }

            slipper.setVelocity(randomSlipDirection.normalize().multiply(strength));

        }

    }
}
