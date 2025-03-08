/*
 * BreweryX Bukkit-Plugin for an alternate brewing process
 * Copyright (C) 2024-2025 The Brewery Team
 *
 * This file is part of BreweryX.
 *
 * BreweryX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BreweryX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BreweryX. If not, see <http://www.gnu.org/licenses/gpl-3.0.html>.
 */

package com.dre.brewery.integration;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.utility.BUtil;
import me.angeschossen.lands.api.LandsIntegration;
import me.angeschossen.lands.api.flags.enums.FlagTarget;
import me.angeschossen.lands.api.flags.enums.RoleFlagCategory;
import me.angeschossen.lands.api.flags.type.RoleFlag;
import me.angeschossen.lands.api.land.LandWorld;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class LandsHook extends Hook {

    public static LandsHook LANDS = new LandsHook("Lands", config.isUseLands());

    private LandsIntegration landsApi;
    private RoleFlag barrelAccessFlag;

    public LandsHook(String name, boolean enabled) {
        super(name, enabled);

        if (!this.isEnabled()) return;
        this.landsApi = LandsIntegration.of(BreweryPlugin.getInstance());
        this.barrelAccessFlag = RoleFlag.of(landsApi, FlagTarget.PLAYER, RoleFlagCategory.ACTION, "barrel_access")
            .setDisplayName("Barrel Access")
            .setDescription(BUtil.colorArray("&r&7Allows opening", "&r&7BreweryX barrels."))
            .setIcon(new ItemStack(Material.BARREL))
            .setDisplay(true);
    }

    public boolean hasBarrelAccess(Player player, Location location) {
        if (!this.isEnabled() || location.getWorld() == null) return true;
        LandWorld lWorld = landsApi.getWorld(location.getWorld());
        if (lWorld == null) return true;

        return lWorld.hasRoleFlag(player.getUniqueId(), location, this.barrelAccessFlag);
    }

}
