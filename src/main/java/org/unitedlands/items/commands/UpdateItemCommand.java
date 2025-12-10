package org.unitedlands.items.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.items.UnitedItems;
import org.unitedlands.items.util.ItemUpdater;
import org.unitedlands.items.util.MessageProvider;

public class UpdateItemCommand implements CommandExecutor {

    @SuppressWarnings("unused")
    private final UnitedItems plugin;
    private final MessageProvider messageProvider;

    public UpdateItemCommand(UnitedItems plugin, MessageProvider messageProvider) {
        this.plugin = plugin;
        this.messageProvider = messageProvider;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
            @NotNull String @NotNull [] args) {

        if (args.length != 0)
            return false;

        Player player = (Player) sender;

        if (player == null)
            return false;

        var heldItem = player.getInventory().getItemInMainHand();
        if (heldItem == null || heldItem.getType() == Material.AIR) {
            org.unitedlands.utils.Messenger.sendMessage(player,
                    messageProvider.get("messages.update-no-item"), null,
                    messageProvider.get("messages.prefix"));
            return false;
        }

        var updated = ItemUpdater.updateItem(plugin, messageProvider, player, heldItem, true);

        if (updated == heldItem) {
            return true;
        }

        player.getInventory().setItemInMainHand(updated);
        return true;
    }
}
