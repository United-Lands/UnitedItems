package org.unitedlands.items;

import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.items.commands.UnitedItemsCommands;
import org.unitedlands.items.commands.UpdateItemCommand;
import org.unitedlands.items.managers.*;
import org.unitedlands.items.util.DataManager;
import org.unitedlands.items.util.MessageProvider;
import org.unitedlands.items.util.PermissionsManager;

import java.util.Objects;

public class UnitedItems extends JavaPlugin {

    private static MessageProvider messageProvider;

    private PotionManager potionManager;
    private VoucherManager voucherManager;
    private DataManager dataManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        messageProvider = new MessageProvider(getConfig());

        PermissionsManager permissionsManager = new PermissionsManager();
        dataManager = new DataManager();
        ArmourManager armourManager = new ArmourManager(this, getConfig());
        CropManager cropManager = new CropManager(permissionsManager, this, dataManager);
        potionManager = new PotionManager(this);
        ToolManager toolManager = new ToolManager(this, permissionsManager);
        TreeManager treeManager = new TreeManager(this, permissionsManager, dataManager);
        voucherManager = new VoucherManager(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(armourManager, this);
        pm.registerEvents(cropManager, this);
        pm.registerEvents(potionManager, this);
        pm.registerEvents(toolManager, this);
        pm.registerEvents(treeManager, this);
        pm.registerEvents(voucherManager, this);

        Objects.requireNonNull(getCommand("uniteditems")).setExecutor(new UnitedItemsCommands(this, messageProvider));
        Objects.requireNonNull(getCommand("updateitem")).setExecutor(new UpdateItemCommand(this, messageProvider));
    }

    public VoucherManager getVoucherManager() {
        return voucherManager;
    }

    public PotionManager getPotionManager() {
        return potionManager;
    }

    public static MessageProvider getMessageProvider() {
        return messageProvider;
    }

    @Override
    public void onDisable() {
        dataManager.saveSaplings();
        dataManager.saveCrops();
    }
}
