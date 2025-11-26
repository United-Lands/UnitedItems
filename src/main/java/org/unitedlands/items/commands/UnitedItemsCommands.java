package org.unitedlands.items.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.items.UnitedItems;
import org.unitedlands.items.util.MessageProvider;
import org.unitedlands.utils.Messenger;

public class UnitedItemsCommands implements CommandExecutor {

    private final UnitedItems plugin;
    private final MessageProvider messageProvider;

    public UnitedItemsCommands(UnitedItems plugin, MessageProvider messageProvider) {
        this.plugin = plugin;
        this.messageProvider = messageProvider;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label,
            @NotNull String @NotNull [] args) {

        if (args.length == 0)
            return false;

        if (!sender.hasPermission("united.items.admin"))
            return false;

        if (args[0].equalsIgnoreCase("reload"))
        {
            plugin.reloadConfig();
            UnitedItems.getMessageProvider().reload(plugin.getConfig());

            plugin.getPotionManager().reloadPotions();
            plugin.getVoucherManager().reload();

            Messenger.sendMessage(sender, messageProvider.get("messages.reload"), null, messageProvider.get("messages.prefix"));
        }

        return true;
    }

}
