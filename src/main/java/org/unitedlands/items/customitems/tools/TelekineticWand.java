package org.unitedlands.items.customitems.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.text.Component;

public class TelekineticWand extends CustomTool implements Listener {

    private final Plugin plugin;

    private Map<UUID, Block> playerCopyCache = new HashMap<>();

    public TelekineticWand(Plugin plugin) {
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

        Block sourceBlock = event.getClickedBlock();

        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            if (isBlacklisted(sourceBlock)) {
                player.sendActionBar(Component.text("§cThis block type can't be moved."));
                return;
            }
            playerCopyCache.put(player.getUniqueId(), sourceBlock);
            player.sendActionBar(Component.text("Block selected, ready to move with right click."));

        } else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {

            var copiedBlock = playerCopyCache.get(player.getUniqueId());
            if (copiedBlock == null) {
                player.sendActionBar(
                        Component.text("§cNo source found, select something to copy by left-clicking."));
                return;
            }

            if (isBlacklisted(copiedBlock)) {
                player.sendActionBar(Component.text("§cThis block type can't be moved."));
                return;
            }

            var targetBlock = sourceBlock.getRelative(event.getBlockFace());
            if (!targetBlock.getType().equals(Material.AIR)) {
                player.sendActionBar(Component.text("§cThe target space must be empty."));
                return;
            }

            if (!copiedBlock.getChunk().equals(targetBlock.getChunk())) {
                player.sendActionBar(
                        Component.text("§cThe target space must be in the same chunk as the source block."));
                return;
            }

            Bukkit.getScheduler().runTaskLater(plugin, () -> {

                targetBlock.setType(copiedBlock.getType(), false);
                targetBlock.setBlockData(copiedBlock.getBlockData().clone(), false);

                if (copiedBlock.getState() instanceof InventoryHolder holder) {
                    var inventory = holder.getInventory();
                    var dropLocation = copiedBlock.getLocation();

                    for (ItemStack invItem : inventory.getContents()) {
                        if (invItem != null && !invItem.getType().isAir()) {
                            dropLocation.getWorld().dropItemNaturally(dropLocation, invItem);
                        }
                    }
                }
                copiedBlock.setType(Material.AIR, false);

                player.sendActionBar(Component.text("§2Block successfully moved."));
                playerCopyCache.remove(player.getUniqueId());

            }, 1);

        }
    }

    private boolean isBlacklisted(Block block) {
        var blacklist = plugin.getConfig().getStringList("items.telekinetic-wand.blacklist");
        if (blacklist == null || blacklist.isEmpty())
            return false;

        var copiedBlockType = block.getType().toString();
        for (String blEntry : blacklist) {
            if (copiedBlockType.contains(blEntry)) {
                return true;
            }
        }
        return false;
    }

}
