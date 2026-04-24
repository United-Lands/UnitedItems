package org.unitedlands.items;

import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.classes.ConfigFile;
import org.unitedlands.items.commands.RefreshItemCommand;
import org.unitedlands.items.commands.UnitedItemsCommands;
import org.unitedlands.items.commands.UpdateItemCommand;
import org.unitedlands.items.listeners.FishingListener;
import org.unitedlands.items.listeners.MobKillListener;
import org.unitedlands.items.managers.*;
import org.unitedlands.items.util.DataManager;
import org.unitedlands.items.util.MessageProvider;
import org.unitedlands.items.util.PermissionsManager;

import java.util.Objects;

public class UnitedItems extends JavaPlugin {

    private static MessageProvider messageProvider;

    private ConfigFile cropsConfig;
    private ConfigFile recipeConfig;
    private ConfigFile brewingConfig;
    private ConfigFile lootConfig;

    private PotionManager potionManager;
    private VoucherManager voucherManager;
    private DataManager dataManager;
    private CustomRecipeManager customRecipeManager;
    private BrewingManager brewingManager;

    private FishingListener fishingListener;
    private MobKillListener mobKillListener;

    @Override
    public void onEnable() {

        saveDefaultConfig();
        loadConfigs();

        messageProvider = new MessageProvider(getConfig());

        PermissionsManager permissionsManager = new PermissionsManager();
        dataManager = new DataManager();
        ArmourManager armourManager = new ArmourManager(this, getConfig());
        CropManager cropManager = new CropManager(permissionsManager, this, dataManager);
        potionManager = new PotionManager(this);
        ToolManager toolManager = new ToolManager(this, permissionsManager);
        TreeManager treeManager = new TreeManager(this, permissionsManager, dataManager);
        voucherManager = new VoucherManager(this);

        customRecipeManager = new CustomRecipeManager(this);
        brewingManager = new BrewingManager(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(armourManager, this);
        pm.registerEvents(cropManager, this);
        pm.registerEvents(potionManager, this);
        pm.registerEvents(toolManager, this);
        pm.registerEvents(treeManager, this);
        pm.registerEvents(voucherManager, this);
        pm.registerEvents(customRecipeManager, this);

        fishingListener = new FishingListener(this);
        pm.registerEvents(fishingListener, this);
        mobKillListener = new MobKillListener(this);
        pm.registerEvents(mobKillListener, this);

        // Disabled for the time being until Nexo potion issue is solved
        pm.registerEvents(brewingManager, this);

        Objects.requireNonNull(getCommand("uniteditems")).setExecutor(new UnitedItemsCommands(this, messageProvider));
        Objects.requireNonNull(getCommand("updateitem")).setExecutor(new UpdateItemCommand(this, messageProvider));
        Objects.requireNonNull(getCommand("itemrefresh")).setExecutor(new RefreshItemCommand(this, messageProvider));
    }

    public void loadConfigs() {
        cropsConfig = new ConfigFile(this, "crops.yml");
        recipeConfig = new ConfigFile(this, "recipes.yml");
        brewingConfig = new ConfigFile(this, "brewing.yml");
        lootConfig = new ConfigFile(this, "loottables.yml");
    }

    public ConfigFile getCropsConfig() {
        return cropsConfig;
    }

    public ConfigFile getRecipeConfig() {
        return recipeConfig;
    }

    public VoucherManager getVoucherManager() {
        return voucherManager;
    }

    public PotionManager getPotionManager() {
        return potionManager;
    }

    public CustomRecipeManager getCustomRecipeManager() {
        return customRecipeManager;
    }

    public ConfigFile getBrewingConfig() {
        return brewingConfig;
    }

    public ConfigFile getLootConfig() {
        return lootConfig;
    }

    public static MessageProvider getMessageProvider() {
        return messageProvider;
    }

    public BrewingManager getBrewingManager() {
        return brewingManager;
    }

    public FishingListener getFishingListener() {
        return fishingListener;
    }

    public MobKillListener getMobKillListener() {
        return mobKillListener;
    }

    @Override
    public void onDisable() {
        dataManager.saveSaplings();
        dataManager.saveCrops();
    }
}
