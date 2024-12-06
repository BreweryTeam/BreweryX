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
import com.dre.brewery.api.events.barrel.BarrelAccessEvent;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.ClaimPermission;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

public class GriefPreventionBarrel {

	private static BreweryPlugin brewery = BreweryPlugin.getInstance();

	public static boolean checkAccess(BarrelAccessEvent event) {
		GriefPrevention griefPrevention = GriefPrevention.instance;
		Player player = event.getPlayer();
		PlayerData playerData = griefPrevention.dataStore.getPlayerData(player.getUniqueId());

		if (!griefPrevention.claimsEnabledForWorld(player.getWorld()) || playerData.ignoreClaims || !griefPrevention.config_claims_preventTheft) {
			return true;
		}

		// block container use during pvp combat
		if (playerData.inPvpCombat()) {
			return false;
		}

		// check permissions for the claim the Barrel is in
		Claim claim = griefPrevention.dataStore.getClaimAt(event.getSpigot().getLocation(), false, playerData.lastClaim);
		if (claim != null) {
			playerData.lastClaim = claim;
			Supplier<String> supplier = claim.checkPermission(player, ClaimPermission.Inventory, null);
			String noContainersReason = supplier != null ? supplier.get() : null;
			if (noContainersReason != null) {
				return false;
			}
		}

		// drop any pvp protection, as the player opens a barrel
		if (playerData.pvpImmune) {
			playerData.pvpImmune = false;
		}

		return true;
	}

}
