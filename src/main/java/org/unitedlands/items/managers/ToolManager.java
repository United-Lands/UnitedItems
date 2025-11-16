package org.unitedlands.items.managers;

import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.unitedlands.items.customitems.tools.*;
import org.unitedlands.items.util.PermissionsManager;

import java.util.HashMap;
import java.util.Map;

import static org.bukkit.Bukkit.getScheduler;

public class ToolManager implements Listener {

    private final Map<String, CustomTool> toolSets = new HashMap<>();
    private final PermissionsManager permissionsManager;
    private final Plugin plugin;

    public ToolManager(Plugin plugin, PermissionsManager permissionsManager) {
        this.plugin = plugin;
        this.permissionsManager = permissionsManager;

        FileConfiguration config = plugin.getConfig();

        toolSets.put("gamemaster", new GamemasterTools(plugin, config));
        toolSets.put("amethyst", new AmethystPickaxe());
        toolSets.put("barkbinder", new BarkbinderAxe(plugin));
        toolSets.put("gingerbread", new GingerbreadTools());
        toolSets.put("architects_wand", new ArchitectsWand(plugin));
        toolSets.put("telekinetic_wand", new TelekineticWand(plugin));
        toolSets.put("creeper_bow", new CreeperBow(plugin));
        toolSets.put("creeper_rocket", new CreeperRocket(plugin));
    }

    // Detect if the player is holding a registered tool.
    public CustomTool detectTool(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        for (Map.Entry<String, CustomTool> entry : toolSets.entrySet()) {
            String toolId = entry.getKey();
            if (isCustomTool(itemInHand, toolId)) {
                return entry.getValue();
            }
        }

        // No matching tool found
        return null;
    }

    // Check if the item in hand matches the toolId.
    private boolean isCustomTool(ItemStack item, String toolId) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        CustomStack customStack = CustomStack.byItemStack(item);
        return customStack != null && customStack.getId().contains(toolId);
    }

    // Apply effects for the detected tool.
    private void applyEffectsIfHoldingTool(Player player) {
        CustomTool tool = detectTool(player);
        if (tool != null) {
            tool.applyEffects(player);
        } else {
            removeToolEffects(player);
        }
    }

    // Remove any effects if a player is no longer holding the tool.
    private void removeToolEffects(Player player) {
        CustomTool detectedTool = detectTool(player); // Detect currently held tool
        if (detectedTool != null) {
            // Remove only the effects applied by the detected tool
            for (PotionEffectType effectType : detectedTool.getAppliedEffects()) {
                if (player.hasPotionEffect(effectType)) {
                    player.removePotionEffect(effectType);
                }
            }
        } else {
            // Fallback: Remove effects that could belong to any tool in the registry
            toolSets.values().forEach(tool -> {
                for (PotionEffectType effectType : tool.getAppliedEffects()) {
                    if (player.hasPotionEffect(effectType)) {
                        player.removePotionEffect(effectType);
                    }
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteractTool(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        CustomTool tool = detectTool(player);
        if (tool == null) return;

        if (!permissionsManager.canInteract(player, event.getClickedBlock()))
            return;

        tool.handleInteract(player, event);
    }

    @EventHandler
    // Check block breaks for use of custom tools.
    public void onNormalBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        CustomTool tool = detectTool(player);
        // Delegate the block breaking logic to the specific tool.
        if (tool != null) {
            tool.handleBlockBreak(player, event);
        }
    }

    @EventHandler
    // Check entity damage for use of custom tools.
    public void handleEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            CustomTool tool = detectTool(player);
            if (tool != null) {
                tool.handleEntityDamage(player, event);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void handleProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }

        // Tool logic
        CustomTool tool = detectTool(player);
        if (tool != null) {
            tool.handleProjectileLaunch(player, event);
        }
    }


    @EventHandler
    // Check projectile launch for use of custom tools.
    public void handleElytraBoost(PlayerElytraBoostEvent event) {
        CustomTool tool = detectTool(event.getPlayer());
        if (tool != null) {
            tool.handleElytraBoost(event.getPlayer(), event);
        }
    }

    @EventHandler
    // Check if tools has been moved when interacting with the inventory.
    public void onInventoryClick(InventoryClickEvent event) {
        getScheduler().runTask(plugin, () -> {
            if (event.getWhoClicked() instanceof Player player) {
                applyEffectsIfHoldingTool(player);
            }
        });
    }

    @EventHandler
    // Checks when a player switches their held item.
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        getScheduler().runTask(plugin, () -> applyEffectsIfHoldingTool(player));
    }

    @EventHandler
    // Checks if the dropped item is a registered tool.
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();
        for (Map.Entry<String, CustomTool> entry : toolSets.entrySet()) {
            String toolId = entry.getKey();
            if (isCustomTool(droppedItem, toolId)) {
                // If the dropped item is a registered tool, remove its effects.
                getScheduler().runTask(plugin, () -> removeToolEffects(player));
                break; // Stop checking further since the tool has been identified.
            }
        }
    }

    @EventHandler
    // Checks if the player picks up a registered tool.
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        // Check if the entity picking up the item is a player.
        if (event.getEntity() instanceof Player player) {
            ItemStack pickedUpItem = event.getItem().getItemStack();
            // Check if the picked up item is a registered tool.
            for (Map.Entry<String, CustomTool> entry : toolSets.entrySet()) {
                String toolId = entry.getKey();
                if (isCustomTool(pickedUpItem, toolId)) {
                    // If the picked up item is a registered tool, apply its effects.
                    getScheduler().runTask(plugin, () -> applyEffectsIfHoldingTool(player));
                    break; // Stop checking further since the tool has been identified.
                }
            }
        }
    }
}
