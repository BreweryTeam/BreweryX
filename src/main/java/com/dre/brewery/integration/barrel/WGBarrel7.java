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


import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.utility.Logging;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.internal.permission.RegionPermissionModel;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WGBarrel7 implements WGBarrel {

	private Method getWorldByName;

	public WGBarrel7() {
		try {
			getWorldByName = WorldGuardPlatform.class.getDeclaredMethod("getWorldByName", String.class);
		} catch (NoSuchMethodException e) {
			getWorldByName = null;
		}
	}

	@SuppressWarnings("deprecation")
	public boolean checkAccess(Player player, Block spigot, Plugin plugin) {
		WorldGuardPlugin wg = (WorldGuardPlugin) plugin;
		WorldGuardPlatform platform = WorldGuard.getInstance().getPlatform();

		World world;
		if (getWorldByName == null) {
			world = platform.getMatcher().getWorldByName(spigot.getWorld().getName());
		} else {
			// Workaround for an older Beta Version of WorldGuard 7.0
			try {
				world = ((World) getWorldByName.invoke(platform, spigot.getWorld().getName()));
			} catch (IllegalAccessException | InvocationTargetException e) {
				Logging.errorLog("Error in WorldGuard", e);
				Logging.msg(player, "Error in WorldGuard");
				return false;
			}
		}
		if (!platform.getGlobalStateManager().get(world).useRegions) return true; // Region support disabled
		WorldEditPlugin we = JavaPlugin.getPlugin(WorldEditPlugin.class);
		if (new RegionPermissionModel(we.wrapPlayer(player)).mayIgnoreRegionProtection(world)) return true; // Whitelisted cause

		RegionQuery query = platform.getRegionContainer().createQuery();

		return query.testBuild(new Location(world, spigot.getX(), spigot.getY(), spigot.getZ()), wg.wrapPlayer(player), Flags.USE, Flags.CHEST_ACCESS);
	}

}
