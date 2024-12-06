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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.utility.Logging;
import org.bukkit.block.Block;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;


public class WGBarrel5 implements WGBarrel {

	private Method allows;
	private Method canBuild;
	private Method getApplicableRegions;

	public WGBarrel5() {
		try {
			allows = ApplicableRegionSet.class.getMethod("allows", StateFlag.class, LocalPlayer.class);
			canBuild = ApplicableRegionSet.class.getMethod("canBuild", LocalPlayer.class);
			getApplicableRegions = RegionManager.class.getMethod("getApplicableRegions", Location.class);
		} catch (NoSuchMethodException e) {
			Logging.errorLog("Failed to Hook WorldGuard for Barrel Open Permissions! Opening Barrels will NOT work!", e);
			Logging.errorLog("Brewery was tested with version 5.8, 6.1 to 7.0 of WorldGuard!");
			Logging.errorLog("Disable the WorldGuard support in the config and do /brew reload");
		}
	}

	@SuppressWarnings("deprecation")
	public boolean checkAccess(Player player, Block spigot, Plugin plugin) {
		WorldGuardPlugin wg = (WorldGuardPlugin) plugin;
		if (!wg.getGlobalRegionManager().hasBypass(player, player.getWorld())) {
			try {
				Object region = getApplicableRegions.invoke(wg.getRegionManager(player.getWorld()), spigot.getLocation());

				if (region != null) {
					LocalPlayer localPlayer = wg.wrapPlayer(player);

					if (!(Boolean) allows.invoke(region, DefaultFlag.CHEST_ACCESS, localPlayer)) {
						if (!(Boolean) canBuild.invoke(region, localPlayer)) {
							return false;
						}
					}
				}

			} catch (IllegalAccessException | InvocationTargetException e) {
				Logging.errorLog("Failed to check WorldGuard Barrel Access!", e);
				return false;
			}
		}
		return true;
	}
}
