package org.unitedlands.items.managers;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.unitedlands.UnitedLib;
import org.unitedlands.items.UnitedItems;
import org.unitedlands.items.customitems.armours.*;
import org.unitedlands.items.util.ItemUpdater;
import org.unitedlands.items.util.MessageProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.bukkit.Bukkit.getScheduler;

public class ArmourManager implements Listener {

    private final Map<String, CustomArmour> armourSets = new HashMap<>();
    private final Map<UUID, CustomArmour> activeArmourCache = new HashMap<>();
    private final UnitedItems plugin;
    private static final int ONE_YEAR_TICKS = 630720000;

    public ArmourManager(UnitedItems plugin, FileConfiguration config) {
        this.plugin = plugin;
        armourSets.put("nutcracker", new NutcrackerArmour());
        armourSets.put("gamemaster", new GamemasterArmour(plugin, config));
        armourSets.put("kraken", new KrakenArmour(plugin, config));
        armourSets.put("necromancer", new NecromancerArmour(plugin, config));
    }

    // Detect if the player is wearing a full set of a registered armour.
    private CustomArmour detectArmourSet(Player player) {
        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();

        for (Map.Entry<String, CustomArmour> entry : armourSets.entrySet()) {
            String setId = entry.getKey();
            if (isFullSet(helmet, chestplate, leggings, boots, setId)) {
                return entry.getValue();
            }
        }

        // No matching set found
        return null;
    }

    // Check if all pieces of the set match the given setId.
    private boolean isFullSet(ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots,
                              String setId) {
        return isCustomArmourPiece(helmet, setId) &&
                isCustomArmourPiece(chestplate, setId) &&
                isCustomArmourPiece(leggings, setId) &&
                isCustomArmourPiece(boots, setId);
    }

    // Check if an individual armour piece matches the setId.
    private boolean isCustomArmourPiece(ItemStack item, String setId) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        if (UnitedLib.getInstance().getItemFactory().isCustomItem(item))
        {
            if (UnitedLib.getInstance().getItemFactory().getId(item).contains(setId))
                return true;
        }
        return false;
    }

    // Apply effects if full set is worn.
    private void applyEffectsIfWearingArmor(Player player) {
        CustomArmour armour = detectArmourSet(player);
        removeAllEffects(player);

        // Use cached sets instead of querying each individual piece every time.
        if (armour != null) {
            activeArmourCache.put(player.getUniqueId(), armour);
            armour.applyEffects(player);
        } else {
            activeArmourCache.remove(player.getUniqueId());
        }
    }

    // Remove effects if armour is removed.
    private void removeAllEffects(Player player) {
        for (CustomArmour armour : armourSets.values()) {
            for (PotionEffectType effectType : armour.getAppliedEffects()) {
                if (player.hasPotionEffect(effectType)) {
                    PotionEffect current = player.getPotionEffect(effectType);
                    if (current != null && current.getDuration() >= ONE_YEAR_TICKS) {
                        player.removePotionEffect(effectType);
                    }
                }
            }
        }
    }

    private boolean autoUpdateArmourToggle() {
        return plugin.getConfig().getBoolean("update.auto-update.armour");
    }

    private void autoUpdateArmour(Player player) {
        MessageProvider messageProvider = UnitedItems.getMessageProvider();
        var inv = player.getInventory();
        ItemStack helmet = inv.getHelmet();
        ItemStack chest = inv.getChestplate();
        ItemStack legs = inv.getLeggings();
        ItemStack boots = inv.getBoots();

        ItemStack newHelmet = ItemUpdater.updateItem(plugin, messageProvider, player, helmet, false);
        ItemStack newChest = ItemUpdater.updateItem(plugin, messageProvider, player, chest, false);
        ItemStack newLegs = ItemUpdater.updateItem(plugin, messageProvider, player, legs, false);
        ItemStack newBoots = ItemUpdater.updateItem(plugin, messageProvider, player, boots, false);

        if (newHelmet != helmet) inv.setHelmet(newHelmet);
        if (newChest != chest) inv.setChestplate(newChest);
        if (newLegs != legs) inv.setLeggings(newLegs);
        if (newBoots != boots) inv.setBoots(newBoots);
    }

    @EventHandler
    // Check player damage events for use of custom armour.
    public void handlePlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        CustomArmour armour = activeArmourCache.get(player.getUniqueId());
        if (armour != null) {
            armour.handlePlayerDamage(player, event);
        }
    }

    @EventHandler
    // Handle experience pickups.
    public void handleExpPickup(PlayerPickupExperienceEvent event) {
        Player player = event.getPlayer();
        CustomArmour armour = activeArmourCache.get(player.getUniqueId());
        if (armour != null) {
            armour.handleExpPickup(player, event.getExperienceOrb());
        }
    }
    @EventHandler
    // Handle armour changes.
    public void onPlayerArmorChange(PlayerArmorChangeEvent event) {
        Player player = event.getPlayer();
        getScheduler().runTask(plugin, () -> {
            if (autoUpdateArmourToggle()) autoUpdateArmour(player);
            applyEffectsIfWearingArmor(player);
        });
    }

    @EventHandler
    // Apply or remove effects when a player joins.
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        getScheduler().runTask(plugin, () -> {
            if (autoUpdateArmourToggle()) autoUpdateArmour(player);
            applyEffectsIfWearingArmor(player);
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        activeArmourCache.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    // Check if the armour has broken when taking damage.
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        getScheduler().runTask(plugin, () -> {
            if (autoUpdateArmourToggle()) autoUpdateArmour(player);
            applyEffectsIfWearingArmor(player);
        });
    }

    @EventHandler
    // Check resurrect events for use of custom armour.
    public void onEntityResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        CustomArmour armour = activeArmourCache.get(player.getUniqueId());
        if (armour != null) {
            armour.handleResurrect(player, event);
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player player)) return;
        CustomArmour armour = activeArmourCache.get(player.getUniqueId());
        if (armour != null) {
            armour.handleTarget(player, event);
        }
    }

    @EventHandler
    // Reset specific custom armour states on respawn.
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        for (CustomArmour armour : armourSets.values()) {
            armour.handleRespawn(player);
        }
    }

    @EventHandler
    // Removes effects on player death.
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        detectArmourSet(player);
        getScheduler().runTask(plugin, () -> removeAllEffects(player));
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        CustomArmour armour = activeArmourCache.get(killer.getUniqueId());
        if (armour != null) {
            armour.handleMobKill(killer, event);
        }
    }
}