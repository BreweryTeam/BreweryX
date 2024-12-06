/*
 * BreweryX Bukkit-Plugin for an alternate brewing process
 * Copyright (C) 2024 The Brewery Team
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

package com.dre.brewery.integration.barrel;


import com.sk89q.worldguard.bukkit.RegionQuery;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.bukkit.permission.RegionPermissionModel;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class WGBarrel6 implements WGBarrel {

	public boolean checkAccess(Player player, Block spigot, Plugin plugin) {
		WorldGuardPlugin wg = (WorldGuardPlugin) plugin;

		if (!wg.getGlobalStateManager().get(spigot.getWorld()).useRegions) return true; // Region support disabled
		if (new RegionPermissionModel(wg, player).mayIgnoreRegionProtection(spigot.getWorld())) return true; // Whitelisted cause

		RegionQuery query = wg.getRegionContainer().createQuery();

		return query.testBuild(spigot.getLocation(), player, DefaultFlag.USE, DefaultFlag.CHEST_ACCESS);

	}

}
