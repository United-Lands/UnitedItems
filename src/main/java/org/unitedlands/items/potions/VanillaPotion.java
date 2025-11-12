package org.unitedlands.items.potions;

import org.bukkit.Material;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

public class VanillaPotion extends CustomPotion {

    private final VanillaPotionBuilder.PotionForm form;
    private final PotionEffectType effect;
    private final int amplifier;
    private final int durationTicks;


    public VanillaPotionBuilder.PotionForm getForm() {
        return form;
    }

    public VanillaPotion(VanillaPotionBuilder.PotionForm form,
                         PotionEffectType effect,
                         int amplifier,
                         int durationTicks)
    {
        this.form = form;
        this.effect = effect;
        this.amplifier = amplifier;
        this.durationTicks = durationTicks;
    }

    @Override
    public void onDrink(Player player, ItemStack potionItem, PlayerItemConsumeEvent event) {
        if (form != VanillaPotionBuilder.PotionForm.DRINK) return;

        // Apply the effect
        player.addPotionEffect(buildEffect());

        // Return glass bottle.
        event.setCancelled(true);
        player.getInventory().setItemInMainHand(new ItemStack(Material.GLASS_BOTTLE));
    }

    @Override
    public void onSplash(Player thrower, PotionSplashEvent event) {
        if (form != VanillaPotionBuilder.PotionForm.SPLASH) return;

        var thrown = event.getPotion();
        var loc = thrown.getLocation();
        var world = loc.getWorld();
        if (world == null) return;

        // Detect if the thrown item has any effects, assume none then override if applicable.
        boolean noVanillaEffects = true;
        ItemStack stack = thrown.getItem();
        ItemMeta im = stack.getItemMeta();
        if (im instanceof PotionMeta pm) {
            boolean hasCustom = pm.hasCustomEffects();
            PotionType baseType = pm.getBasePotionType();
            boolean hasBase = (baseType != null && baseType != PotionType.WATER);
            noVanillaEffects = !(hasCustom || hasBase);
        }

        int applied = 0;
        double totalIntensity = 0.0;

        for (LivingEntity target : event.getAffectedEntities()) {
            double intensity = event.getIntensity(target);
            // If it's a water potion (ItemsAdder style) game might set intensity to 0 automatically, override.
            if (noVanillaEffects) intensity = 1.0;
            totalIntensity += intensity;
            if (intensity <= 0) continue;
            int scaledDuration = (int) Math.max(1, durationTicks * intensity);

            target.addPotionEffect(new PotionEffect(effect, scaledDuration, amplifier, false, true, true));
            applied++;
        }

        // Manual fallback if game didn't deliver anything.
        if (applied == 0 || totalIntensity == 0.0) {
            double radius = 4.0;
            double r2 = radius * radius;

            for (var entity : world.getNearbyEntities(loc, radius, radius, radius)) {
                if (!(entity instanceof LivingEntity target)) continue;
                if (target.isDead()) continue;

                double d2 = target.getLocation().distanceSquared(loc);
                if (d2 > r2) continue;

                double dist = Math.sqrt(d2);
                double intensity = Math.max(0.0, 1.0 - (dist / radius));

                int scaledDuration = (int) Math.max(1, durationTicks * intensity);
                if (scaledDuration <= 0) continue;

                target.addPotionEffect(new PotionEffect(effect, scaledDuration, amplifier, false, true, true));
            }
        }
    }

    @Override
    public void onLingeringCloud(Player thrower, AreaEffectCloud cloud, AreaEffectCloudApplyEvent event) {
        if (form != VanillaPotionBuilder.PotionForm.LINGERING) return;

        for (LivingEntity target : event.getAffectedEntities()) {
            target.addPotionEffect(buildEffect());
        }
    }

    private PotionEffect buildEffect() {
        return new PotionEffect(effect, durationTicks, amplifier, false, true, true);
    }
}
