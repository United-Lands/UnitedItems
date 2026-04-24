package org.unitedlands.items.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.unitedlands.items.UnitedItems;
import org.unitedlands.utils.Logger;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

import java.util.*;

public class LootConfig {

    public record LootItem(String itemId, int minAmount, int maxAmount, double chance) {
    }

    public record LootEntry(String key, boolean enabled, Set<Biome> biomes, List<LootItem> items) {
    }

    private final Map<String, LootEntry> entries = new LinkedHashMap<>();
    private final UnitedItems plugin;

    public LootConfig(UnitedItems plugin, String configPath) {
        this.plugin = plugin;
        reload(configPath);
    }

    public void reload(String configPath) {
        entries.clear();
        ConfigurationSection section = plugin.getLootConfig().get().getConfigurationSection(configPath);
        if (section == null)
            return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection entrySection = section.getConfigurationSection(key);
            if (entrySection == null)
                continue;

            boolean enabled = entrySection.getBoolean("enabled", true);

            Set<Biome> biomes = new HashSet<>();
            Registry<Biome> biomeRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME);
            for (String biomeName : entrySection.getStringList("biomes")) {

                var biomeKey = biomeName.toLowerCase();
                Biome optionalBiome = biomeRegistry.get(new NamespacedKey("minecraft", biomeKey));
                if (optionalBiome != null) {
                    biomes.add(optionalBiome);
                } else {
                    Logger.logWarning("Unknown biome '" + biomeName + "' in " + configPath + "." + key, "UnitedItems");
                }

            }

            List<LootItem> items = new ArrayList<>();
            ConfigurationSection itemsSection = entrySection.getConfigurationSection("items");
            if (itemsSection != null) {
                for (String itemKey : itemsSection.getKeys(false)) {
                    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                    if (itemSection == null)
                        continue;
                    items.add(new LootItem(
                            itemSection.getString("item"),
                            itemSection.getInt("min_amount", 1),
                            itemSection.getInt("max_amount", 1),
                            itemSection.getDouble("chance", 0)));
                }
            }

            entries.put(key, new LootEntry(key, enabled, biomes, items));
        }
    }

    public Collection<LootEntry> getEntries() {
        return entries.values();
    }
}