package org.unitedlands.items;

import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.items.commands.UnitedItemsCommands;
import org.unitedlands.items.commands.UpdateItemCommand;

public class UnitedItems extends JavaPlugin {

    private ItemDetector itemDetector;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        itemDetector = new ItemDetector(this);
        getServer().getPluginManager().registerEvents(itemDetector, this);

        getCommand("uniteditems").setExecutor(new UnitedItemsCommands(this));
        getCommand("updateitem").setExecutor(new UpdateItemCommand(this));
    }

    @Override
    public void onDisable() {
        if (itemDetector != null) {
            itemDetector.saveData();
        }
    }
}
