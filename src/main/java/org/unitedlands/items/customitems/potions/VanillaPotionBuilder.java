package org.unitedlands.items.customitems.potions;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.unitedlands.utils.Logger;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

// Helper class to build vanilla potion sets from configuration file.

public class VanillaPotionBuilder {

    private static final String ROOT_PATH = "potions";
    @SuppressWarnings("unused")
    private final Plugin plugin;

    public VanillaPotionBuilder(Plugin plugin) {
        this.plugin = plugin;
    }

    public Map<String, CustomPotion> loadFrom(FileConfiguration config) {
        ConfigurationSection potionsRoot = config.getConfigurationSection(ROOT_PATH);
        if (potionsRoot == null) {
            Logger.log("No potionsRoot section found at '" + ROOT_PATH + "'. Skipping vanilla effects potion load.", "UnitedItems");
            return Collections.emptyMap();
        }

        Map<String, CustomPotion> result = new HashMap<>();

        for (String namespace : potionsRoot.getKeys(false)) {
            ConfigurationSection namespaceSection = potionsRoot.getConfigurationSection(namespace);
            if (namespaceSection == null) {
                Logger.logWarning("Namespace '" + namespace + "' is not a section. Skipping.", "UnitedItems");
                continue;
            }

            for (String potionId : namespaceSection.getKeys(false)) {
                ConfigurationSection section = namespaceSection.getConfigurationSection(potionId);
                if (section == null) {
                    Logger.logWarning("Potion '" + namespace + "." + potionId + "' is not a section. Skipping.", "UnitedItems");
                    continue;
                }

                String formStr = section.getString("form");
                String effectStr = section.getString("effect");
                int amplifier = section.getInt("amplifier");
                int duration = section.getInt("duration");

                if (formStr == null) {
                    Logger.logWarning("Potion config '" + namespace + "' is missing 'form'. Skipping.", "UnitedItems");
                    continue;
                }

                PotionForm form;
                try {
                    form = PotionForm.valueOf(formStr);
                } catch (IllegalArgumentException ex) {
                    Logger.logWarning("Potion config '" + namespace + "' has an invalid form '" + formStr + "'. Skipping.", "UnitedItems");
                    continue;
                }

                if (effectStr == null) {
                    Logger.logWarning("Potion config '" + namespace + "' is missing 'effect'. Skipping.", "UnitedItems");
                    continue;
                }

                NamespacedKey effectKey = toEffectKey(effectStr);
                PotionEffectType effect = org.bukkit.Registry.EFFECT.get(effectKey);

                if (effect == null) {
                    Logger.logWarning("Potion config '" + namespace + "' has unknown effect '" + effectStr + "'. Skipping.", "UnitedItems");
                    continue;
                }

                if (duration <= 0) {
                    Logger.logWarning("Potion config '" + namespace + "' has non-positive duration " + duration + ". Skipping.", "UnitedItems");
                    continue;
                }

                String fullKey = namespace + ":" + potionId;

                result.put(fullKey, new VanillaPotion(form, effect, amplifier, duration));
                Logger.log("Loaded potion config '" + namespace + "' (" + form + ", " +
                        effect.getKey().getKey() + ", amp=" + amplifier + ", dur=" + duration + "t)", "UnitedItems");
            }
        }

            if (result.isEmpty()) {
                Logger.log("No valid potions were loaded from '" + ROOT_PATH + "'.", "UnitedItems");
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
