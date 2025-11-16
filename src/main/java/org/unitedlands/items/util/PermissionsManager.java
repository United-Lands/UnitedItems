package org.unitedlands.items.util;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class PermissionsManager {

    public boolean canInteract(Player player, Block block) {
        return !playerHasPermissions(player, block);
    }

    // Checks if a player is not allowed to interact with a specific block location.
    public boolean playerHasPermissions(Player player, Block block) {
        if (block == null) return false;

        Location location = block.getLocation();

        // Towny permissions check.
        TownyAPI towny = TownyAPI.getInstance();
        if (towny != null && !towny.isWilderness(location)) {
            Town town = towny.getTown(location);
            Resident resident = towny.getResident(player);

            if (town != null && resident != null && !town.isRuined()) {
                boolean isOwnTown = resident.hasTown() && town.equals(resident.getTownOrNull());

                if (!isOwnTown) {
                    // Check if player is trusted in the town
                    if (town.getTrustedResidents().contains(resident)) {
                        return false;
                    }

                    // Check if player is trusted in the specific plot
                    TownBlock townBlock = towny.getTownBlock(location);
                    return townBlock == null || !townBlock.getTrustedResidents().contains(resident);
                }

                // It's the player's own town.
                return false;
            }
        }

        // WorldGuard permission check.
        LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

        // If the player has WorldGuard bypass permission in this world, allow interaction.
        if (WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(localPlayer, localPlayer.getWorld())) {
            return false;
        }

        // Check WorldGuard region flags. If build flag is present, allow interaction.
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();

        return !query.testState(BukkitAdapter.adapt(location), localPlayer, Flags.BUILD);
    }
}
