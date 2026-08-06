package org.unitedlands.items.customitems.tools;

import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.Particle.DustTransition;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.unitedlands.UnitedLib;
import org.unitedlands.utils.Logger;
import org.unitedlands.utils.Messenger;

@SuppressWarnings("unused")
public class Musket extends CustomTool {

    private final Plugin plugin;

    private static final double MAX_DISTANCE = 64.0;
    private static final double STEP = 0.5;

    private static final double HAND_FORWARD = 0.4; // forward along look direction
    private static final double HAND_SIDE = 0.35; // sideways, mirrored for left/right hand
    private static final double HAND_DOWN = 0.3; // down from eye height

    public Musket(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handleInteract(Player player, PlayerInteractEvent event, EquipmentSlot hand) {

        Logger.log("Used musket!");

        event.setCancelled(true);

        var gunPowder = new ItemStack(Material.GUNPOWDER);
        if (removeFromInventory(player.getInventory(), gunPowder, 1) == -1) {
            Messenger.sendMessage(player, "<yellow>You are out of gunpowder.</yellow>");
            return;
        }

        World world = player.getWorld();
        Location handLocation = getMainHandLocation(player);
        Vector direction = handLocation.getDirection().normalize();

        RayTraceResult rayTraceResult = world.rayTrace(handLocation, direction, MAX_DISTANCE, FluidCollisionMode.NEVER,
                true, 0.25, e -> !e.equals(player));

        double hitDistance = (rayTraceResult != null)
                ? handLocation.toVector().distance(rayTraceResult.getHitPosition())
                : MAX_DISTANCE;

        player.setVelocity(direction.multiply(-0.5));
        player.playSound(player, "entity.generic.explode", 1f, 1f);
        player.setCooldown(player.getInventory().getItemInMainHand(), 100);
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, handLocation, 3, 0.1, 0.1, 0.1, 0);

        // Spawn a particle every 1 block along the ray, stopping at the hit point (or
        // max range)

        var dustOptions = new DustTransition(Color.BLACK, Color.WHITE, 1);

        for (double d = STEP; d <= hitDistance; d += STEP) {
            Location particleLoc = handLocation.clone().add(direction.clone().multiply(-d));
            world.spawnParticle(Particle.DUST_COLOR_TRANSITION, particleLoc, 1, 0, 0, 0, dustOptions);
        }

        if (rayTraceResult != null) {

            var hitLocation = new Location(world, rayTraceResult.getHitPosition().getX(),
                    rayTraceResult.getHitPosition().getY(), rayTraceResult.getHitPosition().getZ());

            world.spawnParticle(Particle.ELECTRIC_SPARK, hitLocation, 12, 0.15, 0.15, 0.15, 0);

            if (rayTraceResult.getHitEntity() != null) {
                if (rayTraceResult.getHitEntity() instanceof Damageable entity) {
                    entity.damage(25d, DamageSource.builder(DamageType.ARROW).withCausingEntity(player)
                            .withDirectEntity(player).withDamageLocation(hitLocation).build());
                    entity.setVelocity(direction.multiply(-1));
                }
            }
        } 

    }

    private Location getMainHandLocation(Player player) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection().normalize();

        // "Right" vector relative to look direction (world up = 0,1,0)
        Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();

        boolean isRightHanded = player.getMainHand() == MainHand.RIGHT;
        double sideSign = isRightHanded ? 1.0 : -1.0;

        return eyeLocation.clone()
                .add(direction.clone().multiply(HAND_FORWARD))
                .add(right.clone().multiply(HAND_SIDE * sideSign))
                .add(0, -HAND_DOWN, 0);
    }

    // TODO: Put in UnitedLib

    public int removeFromInventory(PlayerInventory inventory, ItemStack item, int amount) {

        var itemFactory = UnitedLib.getInstance().getItemFactory();
        var orderItemId = itemFactory.getFilterName(item);

        // First pass: count total available
        int total = 0;
        for (ItemStack stack : inventory.getStorageContents()) {
            if (stack != null && stack.getType() != Material.AIR) {
                var itemId = itemFactory.getFilterName(stack);
                if (itemId.equals(orderItemId)) {
                    total += stack.getAmount();
                }
            }
        }

        if (total < amount)
            return -1;

        // Second pass: remove up to amount
        int remaining = Math.min(total, amount);
        int removed = remaining;
        ItemStack[] contents = inventory.getStorageContents();

        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack stack = contents[i];

            if (stack == null || stack.getType() == Material.AIR)
                continue;

            var itemId = itemFactory.getFilterName(stack);

            if (!itemId.equals(orderItemId))
                continue;

            if (stack.getAmount() <= remaining) {
                remaining -= stack.getAmount();
                contents[i] = null;
            } else {
                stack.setAmount(stack.getAmount() - remaining);
                remaining = 0;
            }
        }

        inventory.setStorageContents(contents);
        return removed;
    }
}
