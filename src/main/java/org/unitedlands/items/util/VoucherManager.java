package org.unitedlands.items.util;

import dev.lone.itemsadder.api.CustomStack;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VoucherManager {

    private String cfgNamespace = "vouchers";
    private String cfgPathPrefix = "prefix";
    private String cfgPermFormat = "chatprefix.%key%";

    private final Plugin plugin;
    private final LuckPerms luckPerms;

    private String msgAlreadyOwned;
    private String msgUnlocked;
    private String msgFailed;
    private Sound fanfareSound;
    private Sound failSound;
    private Particle fanfareParticle;


    // Tiny guard to prevent immediate double-clicks breaking things.
    private final ConcurrentHashMap<UUID, Long> lastUseMs = new ConcurrentHashMap<>();

    public VoucherManager(Plugin plugin) {
        this.plugin = plugin;
        this.luckPerms = LuckPermsProvider.get();
        reload();
    }

    public void reload() {
        loadMessages();
        loadFanfare();
        loadDerivation();
    }

    private void loadMessages() {
        this.msgAlreadyOwned = (plugin.getConfig().getString("messages.voucher-owned"));
        this.msgUnlocked     = (plugin.getConfig().getString("messages.voucher-unlocked"));
        this.msgFailed       = (plugin.getConfig().getString("messages.voucher-failed"));
    }

    private void loadDerivation() {
        var c = plugin.getConfig();
        String ns  = c.getString("vouchers.namespace", cfgNamespace);
        String pp  = c.getString("vouchers.path-prefix", cfgPathPrefix);
        String fmt = c.getString("vouchers.permission-format", cfgPermFormat);

        if (ns != null && !ns.isBlank())  cfgNamespace = ns.trim();
        if (pp != null && !pp.isBlank())  cfgPathPrefix = pp.trim();
        if (fmt != null && !fmt.isBlank()) cfgPermFormat = fmt.trim();
    }

    public boolean tryRedeem(PlayerInteractEvent event) {
        // Only handle right-clicks with the MAIN_HAND to avoid duplicate firing from off-hand.
        if (event.getHand() != EquipmentSlot.HAND) return false;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return false;

        final Player player = event.getPlayer();
        final ItemStack item = event.getItem();
        if (item == null) return false;

        // Detect voucher and derive permission.
        String permission = derivePermission(item);
        // If it's not a voucher, pass the behaviour back to ItemDetector.
        if (permission == null) return false;

        // Slight delay to stop weird behaviour if spam-redeeming.
        final long now = System.currentTimeMillis();
        final Long last = lastUseMs.put(player.getUniqueId(), now);
        if (last != null && (now - last) < 200L) {
            event.setCancelled(true);
            return true;
        }

        // Block redeeming if they already own.
        if (player.hasPermission(permission)) {
            player.sendMessage(msgAlreadyOwned);
            playFailSound(player);
            event.setCancelled(true);
            return true;
        }

        // Grant permissions via LuckPerms.
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());

        assert user != null;
        DataMutateResult result = user.data().add(Node.builder(permission).value(true).build());
        luckPerms.getUserManager().saveUser(user);

        // Feedback & consume on success
        if (result.wasSuccessful()) {
        safeDecrement(event, item);
        player.sendMessage(msgUnlocked);
        playFanfare(player);
        } else {
            player.sendMessage(msgFailed);
            playFailSound(player);
        }

        // Prevent any other handlers from running for this click by mistake.
        event.setCancelled(true);
        return true;
    }

    // Derive the permission name dynamically from the item namespace.
    // For example, prefixes:prefixbear will return the permission chatprefix.bear
    public String derivePermission(ItemStack item) {
        CustomStack cs = CustomStack.byItemStack(item);
        if (cs == null) return null;

        String id = cs.getNamespacedID();
        if (id == null) return null;

        int colon = id.indexOf(':');
        if (colon < 0) return null;

        String namespace = id.substring(0, colon);
        String path = id.substring(colon + 1);

        if (!namespace.equalsIgnoreCase(cfgNamespace)) return null;
        if (!path.regionMatches(true, 0, cfgPathPrefix, 0, cfgPathPrefix.length())) return null;

        String rawKey = path.substring(cfgPathPrefix.length());
        String key = normaliseKey(rawKey);
        if (key.isEmpty()) return null;

        return cfgPermFormat.replace("%key%", key);
    }

    private static String normaliseKey(String s) {
        return s.toLowerCase(Locale.ROOT)
                .replaceAll("^[^a-z0-9]+", "")
                .replaceAll("[^a-z0-9]+", "");
    }

    // Decrease item count.
    private void safeDecrement(PlayerInteractEvent event, ItemStack item) {
        int amount = item.getAmount();
        if (amount <= 1) {
            // Clear the slot that fired the event.
            event.getPlayer().getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(amount - 1);
            // Ensure the mutated stack is written back.
            event.getPlayer().getInventory().setItemInMainHand(item);
        }
    }

    private void loadFanfare() {
        FileConfiguration cfg = plugin.getConfig();

        String soundName = trimOrNull(cfg.getString("vouchers.success-sound", ""));
        this.fanfareSound = resolveSound(soundName);

        String particleName = trimOrNull(cfg.getString("vouchers.play-particle", ""));
        this.fanfareParticle = resolveParticle(particleName);

        String failName = trimOrNull(cfg.getString("vouchers.fail-sound", ""));
        this.failSound = resolveSound(failName);
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private Sound resolveSound(String name) {
        if (name == null) return null;
        String s = name.trim();
        if (s.isEmpty()) return null;

        // Accept "entity.experience_orb.pickup" or "minecraft:entity.experience_orb.pickup"
        String lower = s.toLowerCase(Locale.ROOT).replace(' ', '_');
        NamespacedKey key = lower.contains(":")
                ? NamespacedKey.fromString(lower)
                : NamespacedKey.minecraft(lower);

        if (key == null) return null;

        Sound sound = Registry.SOUNDS.get(key);
        if (sound == null) {
            plugin.getLogger().warning("[Vouchers] Unknown sound '" + name + "'. Sound disabled.");
        }
        return sound;
    }

    private Particle resolveParticle(String name) {
        if (name == null) return null;
        String s = name.trim();
        if (s.isEmpty()) return null;

        try {
            return Particle.valueOf(s.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            plugin.getLogger().warning("[Vouchers] Unknown particle '" + name + "'. Particle disabled.");
            return null;
        }
    }

    private void playFanfare(Player player) {
        if (fanfareSound != null) {
            player.playSound(player.getLocation(), fanfareSound, 1.0f, 1.0f);
        }
        if (fanfareParticle != null) {
            player.getWorld().spawnParticle(
                    fanfareParticle,
                    player.getLocation().add(0, 2.0, 0),
                    20, 0, 0, 0, 0
            );
        }
    }

    private void playFailSound(Player player) {
        if (failSound != null) {
            player.playSound(player.getLocation(), failSound, 1.0f, 1.0f);
        }
    }

}
