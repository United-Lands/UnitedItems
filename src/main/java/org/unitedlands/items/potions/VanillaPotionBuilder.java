package org.unitedlands.items.potions;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// Helper class to build vanilla potion sets from configuration file.

public class VanillaPotionBuilder {

    private static final String ROOT_PATH = "potions";
    private final Plugin plugin;

    public VanillaPotionBuilder(Plugin plugin) {
        this.plugin = plugin;
    }

    public Map<String, CustomPotion> loadFrom(FileConfiguration config) {
        ConfigurationSection potionsRoot = config.getConfigurationSection(ROOT_PATH);
        if (potionsRoot == null) {
            plugin.getLogger().info("No potionsRoot section found at '" + ROOT_PATH + "'. Skipping vanilla effects potion load.");
            return Collections.emptyMap();
        }

        Map<String, CustomPotion> result = new HashMap<>();

        for (String namespace : potionsRoot.getKeys(false)) {
            ConfigurationSection namespaceSection = potionsRoot.getConfigurationSection(namespace);
            if (namespaceSection == null) {
                plugin.getLogger().warning("Namespace '" + namespace + "' is not a section. Skipping.");
                continue;
            }

            for (String potionId : namespaceSection.getKeys(false)) {
                ConfigurationSection section = namespaceSection.getConfigurationSection(potionId);
                if (section == null) {
                    plugin.getLogger().warning("Potion '" + namespace + "." + potionId + "' is not a section. Skipping.");
                    continue;
                }

                String formStr = section.getString("form");
                String effectStr = section.getString("effect");
                int amplifier = section.getInt("amplifier");
                int duration = section.getInt("duration");

                if (formStr == null) {
                    plugin.getLogger().warning("Potion config '" + namespace + "' is missing 'form'. Skipping.");
                    continue;
                }

                PotionForm form;
                try {
                    form = PotionForm.valueOf(formStr);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Potion config '" + namespace + "' has an invalid form '" + formStr + "'. Skipping.");
                    continue;
                }

                if (effectStr == null) {
                    plugin.getLogger().warning("Potion config '" + namespace + "' is missing 'effect'. Skipping.");
                    continue;
                }

                NamespacedKey effectKey = toEffectKey(effectStr);
                PotionEffectType effect = org.bukkit.Registry.EFFECT.get(effectKey);

                if (effect == null) {
                    plugin.getLogger().warning("Potion config '" + namespace + "' has unknown effect '" + effectStr + "'. Skipping.");
                    continue;
                }

                if (duration <= 0) {
                    plugin.getLogger().warning("Potion config '" + namespace + "' has non-positive duration " + duration + ". Skipping.");
                    continue;
                }

                String fullKey = namespace + ":" + potionId;

                result.put(fullKey, new VanillaPotion(form, effect, amplifier, duration));
                plugin.getLogger().info("Loaded potion config '" + namespace + "' (" + form + ", " +
                        effect.getKey().getKey() + ", amp=" + amplifier + ", dur=" + duration + "t)");
            }
        }

            if (result.isEmpty()) {
                plugin.getLogger().info("No valid potions were loaded from '" + ROOT_PATH + "'.");
            }

            return result;
    }

    private static NamespacedKey toEffectKey(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (!s.contains(":")) s = "minecraft:" + s;
        return NamespacedKey.fromString(s);
    }

    public enum PotionForm {
        DRINK,
        SPLASH,
        LINGERING
    }
}
