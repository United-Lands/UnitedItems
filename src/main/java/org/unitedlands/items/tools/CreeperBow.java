package org.unitedlands.items.tools;

import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.plugin.Plugin;

public class CreeperBow extends CustomTool implements Listener {

    private final Plugin plugin;

    public CreeperBow(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handleProjectileLaunch(Player player, ProjectileLaunchEvent event) {

        double tntChance = plugin.getConfig().getDouble("items.creeper_bow.tnt_chance", 0.5);
        double offset = plugin.getConfig().getDouble("items.creeper_bow.offset", 1);
        double velocity_multiplier = plugin.getConfig().getDouble("items.creeper_bow.velocity_multiplier", 1.0);
        int fuse = plugin.getConfig().getInt("items.creeper_bow.fuse-time", 60);

        var chance = Math.random();
        if (chance <= tntChance) {
            var arrow = event.getEntity();
            var velocity = arrow.getVelocity().clone();
            var arrowDirection = velocity.normalize();
            TNTPrimed tnt = arrow.getLocation().getWorld()
                    .spawn(arrow.getLocation().clone().add(arrowDirection.multiply(offset)), TNTPrimed.class);
            tnt.setVelocity(velocity.multiply(velocity_multiplier));
            tnt.setFuseTicks(fuse);
            arrow.remove();
        }

    }
}
