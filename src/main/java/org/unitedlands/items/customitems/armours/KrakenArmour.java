package org.unitedlands.items.customitems.armours;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.unitedlands.utils.Logger;

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
        var commands = plugin.getConfig().getStringList("items.kraken-armour.assistant-commands");
        if (commands == null)
            return;

        Logger.log("Calling assistant...");

        var rnd = new Random();
        var randomCommand = commands.get(rnd.nextInt(0, commands.size()));

        Bukkit.dispatchCommand(player, randomCommand);

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
