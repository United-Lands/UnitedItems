package org.unitedlands.items;

import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.items.commands.UnitedItemsCommands;
import org.unitedlands.items.commands.UpdateItemCommand;
import org.unitedlands.items.util.VoucherManager;

public class UnitedItems extends JavaPlugin {

    private ItemDetector itemDetector;
    private VoucherManager voucherManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.voucherManager = new VoucherManager(this);
        itemDetector = new ItemDetector(this, voucherManager);
        getServer().getPluginManager().registerEvents(itemDetector, this);

        getCommand("uniteditems").setExecutor(new UnitedItemsCommands(this));
        getCommand("updateitem").setExecutor(new UpdateItemCommand(this));
    }

    public VoucherManager getVoucherManager() {
        return voucherManager;
    }

    public ItemDetector getItemDetector() {
        return itemDetector;
    }

    @Override
    public void onDisable() {
        if (itemDetector != null) {
            itemDetector.saveData();
        }
    }
}
