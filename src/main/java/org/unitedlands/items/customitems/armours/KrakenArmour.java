package org.unitedlands.items.customitems.armours;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.unitedlands.UnitedLib;

public class KrakenArmour extends CustomArmour {

    private final Plugin plugin;
    private final Map<UUID, Long> commandCooldowns;

    public KrakenArmour(Plugin plugin, FileConfiguration config) {
        this.plugin = plugin;
        this.commandCooldowns = new HashMap<>();

    }

    @Override
    public void handlePlayerDamage(Player player, EntityDamageEvent event) {
        
        if (!commandCooldowns.containsKey(player.getUniqueId()))
        {
            callAssistant(player);
        } else {
            var cooldownEnd = commandCooldowns.get(player.getUniqueId());
            if (System.currentTimeMillis() >= cooldownEnd)
            {
                callAssistant(player);
            }
        }
    }

    private void callAssistant(Player player) {
        var assistants = new ArrayList<>(plugin.getConfig().getConfigurationSection("items.kraken-armour.assistants").getKeys(false));
        if (assistants == null || assistants.isEmpty())
            return;

        var rnd = new Random();

        String randomAssistant = assistants.get(rnd.nextInt(0, assistants.size()));
        var amount = plugin.getConfig().getInt("items.kraken-armour.assistants." + randomAssistant + ".amount", 1);
        var radius = plugin.getConfig().getInt("items.kraken-armour.assistants." + randomAssistant + ".radius", 8);

        for (int i = 0; i < amount; i++) {
            var rndX = rnd.nextDouble(radius * -1, radius);
            var rndZ = rnd.nextDouble(radius * -1, radius);
            var rndLoc = player.getLocation().clone().add(rndX, 0, rndZ);
            var y = rndLoc.getWorld().getHighestBlockYAt((int)rndLoc.getX(), (int)rndLoc.getZ());
            rndLoc.setY(y);

            UnitedLib.getInstance().getMobFactory().createMobAtLocation(randomAssistant, rndLoc, player, 1);
        }

        int cooldown = plugin.getConfig().getInt("items.kraken-armour.cooldown", 60);

        var chestplate = player.getInventory().getChestplate();
        if (chestplate != null) {
            player.setCooldown(chestplate, cooldown * 20);
        }
        var leggings = player.getInventory().getLeggings();
        if (leggings != null) {
            player.setCooldown(leggings, cooldown * 20);
        }
        var helmet = player.getInventory().getHelmet();
        if (helmet != null) {
            player.setCooldown(helmet, cooldown * 20);
        }
        var boots = player.getInventory().getBoots();
        if (boots != null) {
            player.setCooldown(boots, cooldown * 20);
        }

        commandCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldown * 1000));
    }

}
