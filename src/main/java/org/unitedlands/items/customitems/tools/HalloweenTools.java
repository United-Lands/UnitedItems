package org.unitedlands.items.customitems.tools;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.unitedlands.UnitedLib;

import java.util.*;

public class HalloweenTools extends CustomTool {

    private final Plugin plugin;
    private final boolean triggersOnBlock;
    private final boolean triggersOnAttack;
    private final Random random = new Random();

    public HalloweenTools(Plugin plugin, boolean triggersOnBlock, boolean triggersOnAttack) {
        this.plugin = plugin;
        this.triggersOnBlock = triggersOnBlock;
        this.triggersOnAttack = triggersOnAttack;
    }

    @Override
    public void handleBlockBreak(Player player, BlockBreakEvent event, EquipmentSlot hand) {
        if (!triggersOnBlock || event.isCancelled()) return;

        double activationChance = plugin.getConfig().getDouble("items.halloween.activation-chance");
        if (random.nextDouble() > activationChance) return;

        double treatChance = plugin.getConfig().getDouble("items.halloween.ability-balance");

        if (random.nextDouble() <= treatChance) {
            applyToolTreat(player, event);
        } else {
            applyToolTrick(player, event);
        }
    }

    @Override
    public void handleEntityDamage(Player player, EntityDamageByEntityEvent event, EquipmentSlot hand) {
        if (!triggersOnAttack || event.isCancelled() || !(event.getEntity() instanceof LivingEntity target)) return;

        double activationChance = plugin.getConfig().getDouble("items.halloween.activation-chance");
        if (random.nextDouble() > activationChance) return;

        double treatChance = plugin.getConfig().getDouble("items.halloween.ability-balance");

        if (random.nextDouble() <= treatChance) {
            applyWeaponTreat(player, target, event);
        } else {
            applyWeaponTrick(player, target);
        }
    }

    // +----------------------------------------+ #
    // |              Tool Treats               | #
    // +----------------------------------------+ #

    private void applyToolTreat(Player player, BlockBreakEvent event) {
        int treatType = random.nextInt(5);
        Block block = event.getBlock();
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);

        switch (treatType) {

            // Jack o'Pot:
            // Drop a random valuable.
            case 0:
                List<String> jackpotDrops = plugin.getConfig().getStringList("items.halloween.jack-o-pot-drops");
                if (!jackpotDrops.isEmpty()) {
                    String randomDrop = jackpotDrops.get(random.nextInt(jackpotDrops.size()));
                    Material mat = Material.matchMaterial(randomDrop);

                    if (mat != null) {
                        int amount = 1;
                        loc.getWorld().dropItemNaturally(loc, new ItemStack(mat, amount));
                    }
                }
                playTrickOrTreatEffect(player, loc, Color.ORANGE, Sound.ENTITY_PLAYER_LEVELUP, "Treat: Jack o'Pot!");
                break;

            // Ooh! A piece of candy:
            // Drop a configurable item on the ground.
            case 1:
                List<String> treatDrops = plugin.getConfig().getStringList("items.halloween.treat-drops");
                if (!treatDrops.isEmpty()) {
                    String randomDrop = treatDrops.get(random.nextInt(treatDrops.size()));
                    var itemFactory = UnitedLib.getInstance().getItemFactory();
                    if (itemFactory != null) {
                        ItemStack item = itemFactory.getItemStack(randomDrop, 1);
                        if (item != null) {
                            loc.getWorld().dropItemNaturally(loc, item);
                        }
                    }
                }
                playTrickOrTreatEffect(player, loc, Color.YELLOW, Sound.ENTITY_ITEM_PICKUP, "Treat: Ooh! A piece of candy!");
                break;

            // Poltergeist's Pull:
            // Pull nearby items towards player.
            case 2:
                new org.bukkit.scheduler.BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 60) {
                            this.cancel();
                            return;
                        }
                        for (Entity entity : player.getNearbyEntities(8, 8, 8)) {
                            if (entity instanceof Item item) {
                                Vector direction = player.getLocation().toVector().subtract(item.getLocation().toVector()).normalize().multiply(0.5);
                                item.setVelocity(direction);
                            }
                        }
                        ticks++;
                    }
                }.runTaskTimer(plugin, 0L, 1L);

                playTrickOrTreatEffect(player, player.getLocation(), Color.PURPLE, Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, "Treat: Poltergeist's Pull!");
                break;

            // Spirit Orbs:
            // Spawn some bonus EXP.
            case 3:
                int expAmount = plugin.getConfig().getInt("items.halloween.spirit-orbs-exp");
                loc.getWorld().spawn(loc, org.bukkit.entity.ExperienceOrb.class, orb -> orb.setExperience(expAmount));
                playTrickOrTreatEffect(player, loc, Color.LIME, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, "Treat: Spirit Orbs!");
                break;

            // Long Limbed:
            // Increase player reach.
            case 4: {
                double reachBonus = plugin.getConfig().getDouble("items.halloween.reach-amount");

                var blockRangeAttr = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);

                NamespacedKey blockKey = new NamespacedKey(plugin, "halloween_long_limbed_block");

                if (blockRangeAttr != null) {
                    blockRangeAttr.removeModifier(blockKey);
                    blockRangeAttr.addTransientModifier(new org.bukkit.attribute.AttributeModifier(
                            blockKey,
                            reachBonus,
                            org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER
                    ));
                }

                // Automatically remove after 10 seconds
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        var currentBlockAttr = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);
                        if (currentBlockAttr != null) currentBlockAttr.removeModifier(blockKey);
                    }
                }, 200L);

                playTrickOrTreatEffect(player, loc, Color.PURPLE, Sound.ENTITY_ENDERMAN_TELEPORT, "Treat: Long Limbed!");
                break;
            }
        }
    }

    // +----------------------------------------+ #
    // |              Tool Tricks               | #
    // +----------------------------------------+ #
    private void applyToolTrick(Player player, BlockBreakEvent event) {
        int trickType = random.nextInt(5);
        Block block = event.getBlock();
        Location loc = block.getLocation().add(0.5, 0.5, 0.5);

        switch (trickType) {

            // Scary Sounds:
            // Play a scary noise to the player, with no indication.
            case 0:
                List<String> soundKeys = plugin.getConfig().getStringList("items.halloween.scary-sounds");
                if (!soundKeys.isEmpty()) {
                    String randomKeyString = soundKeys.get(random.nextInt(soundKeys.size()));
                    NamespacedKey key = NamespacedKey.fromString(randomKeyString);

                    if (key != null) {
                        Sound spookySound = Registry.SOUNDS.get(key);
                        if (spookySound != null) {
                            player.playSound(loc, spookySound, 1.0f, 1.0f);
                        }
                    }
                }
                break;

            // Bat Swarm
            // Spawns a bunch of bats in front of the player.
            case 1:
                int batCount = plugin.getConfig().getInt("items.halloween.bat-swarm-amount");
                for (int i = 0; i < batCount; i++) {
                    Bat bat = (Bat) loc.getWorld().spawnEntity(loc, EntityType.BAT);
                    Bukkit.getScheduler().runTaskLater(plugin, bat::remove, 100L);
                }
                playTrickOrTreatEffect(player, loc, Color.BLACK, Sound.ENTITY_BAT_TAKEOFF, "Trick: Bat Swarm!");
                break;

            // Ghostly Grab (and Toss)
            // Throws the dropped item away.
            case 2:
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (Entity entity : loc.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                        if (entity instanceof Item item) {
                            item.setVelocity(new Vector(random.nextDouble() * 2 - 1, 0.4, random.nextDouble() * 2 - 1).normalize().multiply(0.5));                        }
                    }
                }, 1L);
                playTrickOrTreatEffect(player, loc, Color.GRAY, Sound.ENTITY_GHAST_SHOOT, "Trick: Ghostly Grab!");
                break;

            // Fool's Gourd:
            // Broken block is replaced with Jack-o'-Lantern
            case 3:
                if (!(block.getState() instanceof Container)) {
                    event.setDropItems(false);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> block.setType(Material.JACK_O_LANTERN), 1L);
                    playTrickOrTreatEffect(player, loc, Color.ORANGE, Sound.BLOCK_WOOD_PLACE, "Trick: Fool's Gourd!");
                }
                break;

            // Grave Grasp
            // Apply negative potion effects to the player.
            case 4: {
                List<Map<?, ?>> effectMaps = plugin.getConfig().getMapList("items.halloween.negative-potion-effects");

                for (Map<?, ?> effectMap : effectMaps) {
                    String typeName = (String) effectMap.get("type");
                    int duration = (effectMap.get("duration") instanceof Number num) ? num.intValue() : 100;
                    int amplifier = (effectMap.get("amplifier") instanceof Number num) ? num.intValue() : 0;

                    if (typeName != null) {
                        NamespacedKey key = NamespacedKey.minecraft(typeName);
                        PotionEffectType effectType = Registry.EFFECT.get(key);

                        if (effectType != null) {
                            player.addPotionEffect(new PotionEffect(effectType, duration, amplifier, true, true));
                        }
                    }
                }

                playTrickOrTreatEffect(player, loc, Color.PURPLE, Sound.ENTITY_ENDERMAN_TELEPORT, "Trick: Grave Grasp!");
                break;
            }
        }
    }

    // +----------------------------------------+ #
    // |             Weapon Treats              | #
    // +----------------------------------------+ #
    private void applyWeaponTreat(Player player, LivingEntity target, EntityDamageByEntityEvent event) {
        int treatType = random.nextInt(5);
        Location loc = player.getLocation().add(0.5, 0.5, 0.5);


        switch (treatType) {

            // Vampiric Bite:
            // Heal player for amount of damage dealt.
            case 0:
                double healAmount = Math.min(player.getHealth() + event.getFinalDamage(), Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue());
                player.setHealth(healAmount);
                playTrickOrTreatEffect(player, player.getLocation(), Color.RED, Sound.ENTITY_PLAYER_BURP, "Treat: Vampiric Bite!");
                break;

            // Gourd Guard:
            // Apply positive potion effects to the player.
            case 1:
                List<Map<?, ?>> effectMaps = plugin.getConfig().getMapList("items.halloween.positive-potion-effects");

                for (Map<?, ?> effectMap : effectMaps) {
                    String typeName = (String) effectMap.get("type");
                    int duration = (effectMap.get("duration") instanceof Number num) ? num.intValue() : 100;
                    int amplifier = (effectMap.get("amplifier") instanceof Number num) ? num.intValue() : 0;

                    if (typeName != null) {
                        NamespacedKey key = NamespacedKey.minecraft(typeName);
                        PotionEffectType effectType = Registry.EFFECT.get(key);

                        if (effectType != null) {
                            player.addPotionEffect(new PotionEffect(effectType, duration, amplifier, true, true));
                        }
                    }
                }

                playTrickOrTreatEffect(player, loc, Color.YELLOW, Sound.BLOCK_PUMPKIN_CARVE, "Treat: Gourd Guard!");
                break;

            // Pinata
            // Spawns candy on hit.
            case 2:
                List<String> treatDrops = plugin.getConfig().getStringList("items.halloween.treat-drops");

                if (!treatDrops.isEmpty()) {
                    var itemFactory = org.unitedlands.UnitedLib.getInstance().getItemFactory();

                    if (itemFactory != null) {
                        // Grab 5 random items from the treat list.
                        for (int i = 0; i < 5; i++) {
                            String randomDrop = treatDrops.get(random.nextInt(treatDrops.size()));
                            org.bukkit.inventory.ItemStack item = itemFactory.getItemStack(randomDrop, 1);

                            if (item != null) {
                                target.getWorld().dropItemNaturally(target.getLocation(), item);
                            }
                        }
                    }
                }

                playTrickOrTreatEffect(player, loc, org.bukkit.Color.FUCHSIA, Sound.ENTITY_LLAMA_HURT, "Treat: Piñata!");
                break;

            // Midnight Snack:
            // Immediately feed some hunger points.
            case 3:
                int foodBonus = plugin.getConfig().getInt("items.halloween.midnight-snack-food");
                float saturationBonus = (float) plugin.getConfig().getDouble("items.halloween.midnight-snack-saturation");

                int newFoodLevel = Math.min(20, player.getFoodLevel() + foodBonus);
                player.setFoodLevel(newFoodLevel);

                // Saturation cannot exceed the current food level.
                float newSaturation = Math.min(newFoodLevel, player.getSaturation() + saturationBonus);
                player.setSaturation(newSaturation);

                playTrickOrTreatEffect(player, loc, Color.ORANGE, Sound.ENTITY_PLAYER_BURP, "Treat: Midnight Snack!");
                break;

            // Shocking:
            // Simulate lightning strikes on hit entity.
            case 4:
                target.getWorld().strikeLightningEffect(target.getLocation());
                event.setDamage(event.getDamage() + 5.0);
                target.setFireTicks(100);
                playTrickOrTreatEffect(player, loc, Color.YELLOW, null, "Treat: Shocking!");
                break;
        }
    }

    // +----------------------------------------+ #
    // |             Weapon Tricks              | #
    // +----------------------------------------+ #
    private void applyWeaponTrick(Player player, LivingEntity target) {
        int trickType = random.nextInt(5);

        switch (trickType) {

            // Ghostly Blast:
            // Spawn a no damage explosion that knocks everyone back.
            case 0:
                target.getWorld().createExplosion(target.getLocation(), 0.0F, false, false);
                for (Entity entity : target.getWorld().getNearbyEntities(target.getLocation(), 4, 4, 4)) {
                    if (entity instanceof LivingEntity le) {
                        if (le.equals(target)) continue;
                        Vector knockback = le.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(1.5).setY(0.6);
                        le.setVelocity(knockback);
                    }
                }
                playTrickOrTreatEffect(player, target.getLocation(), Color.GRAY, null, "Trick: Ghostly Blast!");
                break;

            // Night Terror:
            // Spawns smoke that blinds players.
            case 1:
                target.getWorld().spawnParticle(Particle.WHITE_SMOKE, target.getLocation(), 40, 3, 1, 3, 0.05);
                for (Entity entity : target.getWorld().getNearbyEntities(target.getLocation(), 5, 5, 5)) {
                    if (entity instanceof LivingEntity le) {
                        le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                    }
                }
                playTrickOrTreatEffect(player, target.getLocation(), Color.BLACK, null, "Trick: Night Terror!");
                break;

            // Tangled Fates:
            // Swap places with hit entity.
            case 2:
                Location pLoc = player.getLocation();
                Location tLoc = target.getLocation();
                player.teleport(tLoc);
                target.teleport(pLoc);
                playTrickOrTreatEffect(player, pLoc, Color.PURPLE, Sound.ENTITY_ENDERMAN_TELEPORT, "Trick: Tangled Fates!");
                playTrickOrTreatEffect(null, tLoc, Color.PURPLE, Sound.ENTITY_ENDERMAN_TELEPORT, null);
                break;

            // Masquerade:
            // Fakes pumpkin overlay on all effected entities.
            case 3:
                player.sendEquipmentChange(player, org.bukkit.inventory.EquipmentSlot.HEAD, new org.bukkit.inventory.ItemStack(org.bukkit.Material.CARVED_PUMPKIN));

                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline()) {
                        org.bukkit.inventory.ItemStack realHelmet = player.getInventory().getHelmet();
                        player.sendEquipmentChange(player, org.bukkit.inventory.EquipmentSlot.HEAD, realHelmet);
                    }
                }, 100L);
                playTrickOrTreatEffect(player, player.getLocation(), Color.ORANGE, null, "Trick: Masquerade!");
                break;

            // Phantom Ascent:
            // Give all effected entities' levitation.
            case 4:
                player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 2));
                target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 60, 2));
                playTrickOrTreatEffect(player, player.getLocation(), Color.WHITE, Sound.ENTITY_GHAST_WARN, "Trick: Phantom Ascent!");
                break;
        }
    }

    private void playTrickOrTreatEffect(Player player, Location loc, Color color, Sound sound, String abilityName) {
        if (loc != null && loc.getWorld() != null) {
            if (sound != null) {
                loc.getWorld().playSound(loc, sound, 1.0f, 1.0f);
            }
            if (color != null) {
                loc.getWorld().spawnParticle(Particle.DUST, loc, 15, 0.5, 0.5, 0.5, new Particle.DustOptions(color, 1.5f));
            }
        }

        // Send the action bar if a name is provided.
        if (player != null && abilityName != null && !abilityName.isEmpty()) {
            int hexColor = (color != null) ? color.asRGB() : 0xFFAA00;

            player.sendActionBar(net.kyori.adventure.text.Component.text(
                    abilityName,
                    net.kyori.adventure.text.format.TextColor.color(hexColor)
            ));
        }
    }
}