package org.unitedlands.items.managers;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.unitedlands.items.customitems.potions.*;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class PotionManager implements Listener {

    private final Map<String, CustomPotion> potionSets = new HashMap<>();
    private final NamespacedKey POTION_KEY;
    private final Plugin plugin;

    public PotionManager(Plugin plugin) {
        FileConfiguration config = plugin.getConfig();
        this.POTION_KEY = new NamespacedKey(plugin, "custom_potion_id");
        this.plugin = plugin;

        potionSets.put("blasting1", new BlastingRegular());
        potionSets.put("blasting2", new BlastingSplash());
        potionSets.put("blasting3", new BlastingLingering(plugin));
        var potionBuilder = new VanillaPotionBuilder(plugin);
        potionSets.putAll(potionBuilder.loadFrom(config));
    }

    @Nullable
    private CustomPotion detectPotion(ItemStack item) {
        String key = findPotionKey(item);
        if (key == null) {
            return null;
        }
        return potionSets.get(key);
    }

    // Player drinks custom potion.
    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        // Try the stack from the player's hand.
        CustomPotion potion = detectPotion(event.getPlayer().getInventory().getItemInMainHand());

        // Fallback to the event item.
        if (potion == null) {
            potion = detectPotion(event.getItem());
        }

        if (potion == null) {
            return;
        }

        potion.onDrink(event.getPlayer(), event.getItem(), event);
    }

    // Player splashed by custom potion.
    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {

        String potionKey = event.getPotion()
                .getPersistentDataContainer()
                .get(POTION_KEY, PersistentDataType.STRING);

        CustomPotion potion = null;

        if (potionKey != null) {
            potion = potionSets.get(potionKey);
        }

        // Fallback: resolve from the item itself.
        if (potion == null) {
            potion = detectPotion(event.getPotion().getItem());
        }

        if (potion == null) {
            return;
        }

        // Shooter might not be a player (ItemsAdder being weird?)
        Player thrower = null;
        if (event.getPotion().getShooter() instanceof Player p) {
            thrower = p;
        }

        // I think ItemsAdder doesn't actually make lingering potions, so we artificially create a cloud.
        if (potion instanceof VanillaPotion vp
                && vp.getForm() == VanillaPotionBuilder.PotionForm.LINGERING) {

            // Spawn cloud at splash location.
            var loc = event.getPotion().getLocation();
            var world = loc.getWorld();
            if (world != null) {
                AreaEffectCloud cloud = (AreaEffectCloud)
                        world.spawnEntity(loc, EntityType.AREA_EFFECT_CLOUD);

                // Tag cloud so onCloudApply can resolve it.
                String resolvedKey = (potionKey != null)
                        ? potionKey : findPotionKey(event.getPotion().getItem());

                if (resolvedKey == null) {
                    // Can't associate this cloud with any custom potion, skip.
                    cloud.remove();
                    return;
                }

                cloud.getPersistentDataContainer().set(POTION_KEY,
                        PersistentDataType.STRING,
                        resolvedKey);

                // Set colour from the bottle's custom colour.
                var itemMeta = event.getPotion().getItem().getItemMeta();
                if (itemMeta instanceof PotionMeta pm && pm.hasColor()) {
                    var color = pm.getColor();
                    if (color != null) {
                        cloud.setColor(color);
                    }
                }

                // Replicate vanilla cloud behaviour.
                cloud.setRadius(3.0f);
                cloud.setRadiusPerTick(-0.005f);
                cloud.setDuration(200);
                cloud.setWaitTime(0);
                cloud.setReapplicationDelay(10);

                // Dummy effect to force plugin to fire real behaviours.
                cloud.addCustomEffect(new PotionEffect(PotionEffectType.LUCK, 1, 0, false, false, false), true);

                cloud.setSource(thrower);
            }
        } else {
            potion.onSplash(thrower, event);
        }
    }

    @EventHandler
    public void onLingeringSplash(LingeringPotionSplashEvent event) {
        var thrown = event.getEntity();

        // Try to read the key stored on throw
        String key = thrown.getPersistentDataContainer().get(POTION_KEY,
                PersistentDataType.STRING);

        // Fallback: resolve from the item itself.
        if (key == null) {
            key = findPotionKey(thrown.getItem());
        }
        if (key == null) {
            return;
        }

        // Tag the cloud with the same key
        var cloud = event.getAreaEffectCloud();
        cloud.getPersistentDataContainer().set(
                POTION_KEY,
                PersistentDataType.STRING,
                key
        );
    }


    // Player effected by lingering cloud.
    @EventHandler
    public void onCloudApply(AreaEffectCloudApplyEvent event) {
        var pdc = event.getEntity().getPersistentDataContainer();
        String key = pdc.get(POTION_KEY, PersistentDataType.STRING);
        if (key == null) {
            return;
        }

        CustomPotion potion = potionSets.get(key);
        if (potion == null) {
            return;
        }

        potion.onLingeringCloud(
                event.getEntity().getSource() instanceof Player p ? p : null,
                event.getEntity(),
                event
        );
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void handleProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }

        // Tag thrown potions.
        if (event.getEntity() instanceof ThrownPotion thrown) {
            // Check main hand first.
            String key = findPotionKey(player.getInventory().getItemInMainHand());

            // Then offhand.
            if (key == null) {
                key = findPotionKey(player.getInventory().getItemInOffHand());
            }

            // If a custom potion ID is found, store it on the projectile.
            if (key != null) {
                thrown.getPersistentDataContainer().set(
                        POTION_KEY,
                        PersistentDataType.STRING,
                        key
                );
            }
        }
    }


    // Helper to find registry key that matches an ItemStack
    @Nullable
    private String findPotionKey(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;

        var cs = CustomStack.byItemStack(item);
        if (cs == null) {
            return null;
        }

        String iaId = cs.getId().toLowerCase();

        for (String key : potionSets.keySet()) {
            String reg = key.toLowerCase();

            if (reg.equals(iaId)) {
                return key;
            }

            if (reg.endsWith(":" + iaId)) {
                return key;
            }
        }

        return null;
    }

    // Reload config of vanilla style potions.
    private void loadVanillaPotions(FileConfiguration config) {
        VanillaPotionBuilder builder = new VanillaPotionBuilder(plugin);
        Map<String, CustomPotion> loaded = builder.loadFrom(config);

        // Remove only previously loaded config potions, ignore hard-coded ones.
        potionSets.entrySet().removeIf(entry -> entry.getValue() instanceof VanillaPotion);

        potionSets.putAll(loaded);
        plugin.getLogger().info("Reloaded " + loaded.size() + " vanilla style potions.");
    }

    public void reloadPotions() {
        loadVanillaPotions(plugin.getConfig());
    }

}
