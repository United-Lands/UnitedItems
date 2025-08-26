package org.unitedlands.items.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Attachable;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.BigDripleaf;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.Wall;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.text.Component;

public class ArchitectsWand extends CustomTool implements Listener {

    private final Plugin plugin;

    private Map<UUID, Location> playerEditLocationCache = new HashMap<>();
    private Map<UUID, String> playerEditStateCache = new HashMap<>();

    public ArchitectsWand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handleInteract(Player player, PlayerInteractEvent event) {

        if (event.getHand() == null)
            return;
        if (!event.getHand().equals(EquipmentSlot.HAND))
            return;
        if (event.getClickedBlock() == null)
            return;

        event.setCancelled(true);

        Block block = event.getClickedBlock();

        var oldEditLocation = playerEditLocationCache.get(player.getUniqueId());
        if (oldEditLocation != null) {
            if (!oldEditLocation.equals(block.getLocation()))
                playerEditStateCache.remove(player.getUniqueId());
        }
        playerEditLocationCache.put(player.getUniqueId(), block.getLocation());

        BlockData data = block.getBlockData();
        List<String> states = plugin.getConfig()
                .getStringList("items.architects-wand." + data.getClass().getSimpleName());

        if (states == null || states.isEmpty()) {
            player.sendActionBar(Component.text("§cThis block can't be edited with the Achitect's Wand."));
            return;
        }

        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {

            int oldStateIndex = states
                    .indexOf(playerEditStateCache.computeIfAbsent(player.getUniqueId(),
                            s -> states.get(states.size() - 1)));

            String newState = states.get((oldStateIndex + 1) % states.size());
            playerEditStateCache.put(player.getUniqueId(), newState);

            player.sendActionBar(Component.text("Now editing property §f§l" + newState));

        } else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {

            var state = playerEditStateCache.computeIfAbsent(player.getUniqueId(), s -> states.get(states.size() - 1));
            var direction = player.getFacing().getOppositeFace();

            switch (state) {
                case "direction":
                    BlockDataCycler.cycleEnum(
                            block,
                            Directional.class,
                            Directional::getFacing,
                            ((Directional) block.getBlockData()).getFaces().toArray(new BlockFace[0]),
                            Directional::setFacing);
                    break;
                case "attachment":
                    BlockDataCycler.cycleEnum(
                            block,
                            FaceAttachable.class,
                            FaceAttachable::getAttachedFace,
                            FaceAttachable.AttachedFace.values(),
                            FaceAttachable::setAttachedFace);
                    break;
                case "dripleaf_tilt":
                    BlockDataCycler.cycleEnum(
                            block,
                            BigDripleaf.class,
                            BigDripleaf::getTilt,
                            BigDripleaf.Tilt.values(),
                            BigDripleaf::setTilt);
                    break;
                case "stair_shape":
                    BlockDataCycler.cycleEnum(
                            block,
                            Stairs.class,
                            Stairs::getShape,
                            Stairs.Shape.values(),
                            Stairs::setShape);
                    break;
                case "half":
                    BlockDataCycler.cycleEnum(
                            block,
                            Bisected.class,
                            Bisected::getHalf,
                            Bisected.Half.values(),
                            Bisected::setHalf);
                    break;
                case "wall_height":
                    DirectionalBlockDataCycler.cycleEnum(
                            block,
                            Wall.class,
                            Wall::getHeight,
                            Wall.Height.values(),
                            Wall::setHeight,
                            direction);
                    break;
                case "bed_part":
                    BlockDataCycler.cycleEnum(
                            block,
                            Bed.class,
                            Bed::getPart,
                            Bed.Part.values(),
                            Bed::setPart);
                    break;
                case "center":
                    BoolBlockDataToggler.setData(
                            block,
                            Wall.class,
                            Wall::isUp,
                            Wall::setUp);
                    break;
                case "connection":
                    DirectionalBoolBlockDataToggler.setData(
                            block,
                            MultipleFacing.class,
                            MultipleFacing::hasFace,
                            MultipleFacing::setFace,
                            direction);
                    break;
                case "open":
                    BoolBlockDataToggler.setData(
                            block,
                            Openable.class,
                            Openable::isOpen,
                            Openable::setOpen);
                    break;
                case "powered":
                    BoolBlockDataToggler.setData(
                            block,
                            Powerable.class,
                            Powerable::isPowered,
                            Powerable::setPowered);
                    break;
                case "waterlogged":
                    BoolBlockDataToggler.setData(
                            block,
                            Waterlogged.class,
                            Waterlogged::isWaterlogged,
                            Waterlogged::setWaterlogged);
                    break;
                case "attached":
                    BoolBlockDataToggler.setData(
                            block,
                            Attachable.class,
                            Attachable::isAttached,
                            Attachable::setAttached);
                    break;
            }

        }
    }

    // #region Helper classes

    @FunctionalInterface
    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }

    public class BoolBlockDataToggler {
        public static <T extends BlockData> void setData(
                Block block,
                Class<T> typeClass,
                Function<T, Boolean> getter,
                BiConsumer<T, Boolean> setter) {
            T typedData = typeClass.cast(block.getBlockData());
            Boolean currentValue = getter.apply(typedData);
            setter.accept(typedData, !currentValue);
            block.setBlockData(typedData, false);
        }
    }

    public class DirectionalBoolBlockDataToggler {
        public static <T extends BlockData, A> void setData(
                Block block,
                Class<T> typeClass,
                BiFunction<T, A, Boolean> getter,
                TriConsumer<T, A, Boolean> setter,
                A face) {
            T typedData = typeClass.cast(block.getBlockData());
            Boolean currentValue = getter.apply(typedData, face);
            setter.accept(typedData, face, !currentValue);
            block.setBlockData(typedData, false);
        }
    }

    public class BlockDataCycler {

        public static <T extends BlockData, V> void cycleEnum(
                Block block,
                Class<T> typeClass,
                Function<T, V> getter,
                V[] validValues,
                BiConsumer<T, V> setter) {
            T typedData = typeClass.cast(block.getBlockData());

            V currentValue = getter.apply(typedData);
            int currentIndex = 0;

            for (int i = 0; i < validValues.length; i++) {
                if (validValues[i].equals(currentValue)) {
                    currentIndex = i;
                    break;
                }
            }

            int newIndex = (currentIndex + 1) % validValues.length;
            setter.accept(typedData, validValues[newIndex]);

            block.setBlockData(typedData, false);
        }
    }

    public class DirectionalBlockDataCycler {

        public static <T extends BlockData, V, U> void cycleEnum(
                Block block,
                Class<T> typeClass,
                BiFunction<T, U, V> getter,
                V[] validValues,
                TriConsumer<T, U, V> setter,
                U face) {
            T typedData = typeClass.cast(block.getBlockData());

            V currentValue = getter.apply(typedData, face);
            int currentIndex = 0;

            for (int i = 0; i < validValues.length; i++) {
                if (validValues[i].equals(currentValue)) {
                    currentIndex = i;
                    break;
                }
            }

            int newIndex = (currentIndex + 1) % validValues.length;
            setter.accept(typedData, face, validValues[newIndex]);

            block.setBlockData(typedData, false);
        }
    }

    // #endregion

}
