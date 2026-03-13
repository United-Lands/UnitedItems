package org.unitedlands.items.managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.event.Listener;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.unitedlands.items.UnitedItems;

public class BrewingManager implements Listener {
    private final UnitedItems plugin;

    private final Map<BrewerInventory, BukkitRunnable> activeBrews = new HashMap<>();
    //private Set<BrewingRecipe> recipes = new HashSet<>();

    public BrewingManager(UnitedItems plugin) {
        this.plugin = plugin;

        //loadRecipes();
    }
}
