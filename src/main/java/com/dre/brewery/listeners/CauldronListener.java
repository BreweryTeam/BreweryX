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

package com.dre.brewery.listeners;

import com.dre.brewery.BCauldron;
import com.dre.brewery.utility.MaterialUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;

public class CauldronListener implements Listener {

	/**
	 * Water in Cauldron gets filled up: remove BCauldron to disallow unlimited Brews
	 * Water in Cauldron gets removed: remove BCauldron to remove Brew data and stop particles
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onCauldronChange(CauldronLevelChangeEvent event) {
		if (MaterialUtil.WATER_CAULDRON == null) {
			// < 1.17
			oldCauldronChange(event);
			return;
		}

		Material currentType = event.getBlock().getType();
		BlockState newState = event.getNewState();
		Material newType = newState.getType();

		if (currentType == Material.WATER_CAULDRON) {
			if (newType != Material.WATER_CAULDRON) {
				// Change from water to anything else
				if (event.getReason() != CauldronLevelChangeEvent.ChangeReason.BOTTLE_FILL) {
					BCauldron.remove(event.getBlock());
				}
			} else { // newType == Material.WATER_CAULDRON
				// Water level change

				Levelled oldCauldron = (Levelled) event.getBlock().getBlockData();
				Levelled newCauldron = (Levelled) newState.getBlockData();

				// Water Level increased somehow, might be Bucket, Bottle, Rain, etc.
				if (newCauldron.getLevel() > oldCauldron.getLevel()) {
					BCauldron.remove(event.getBlock());
				}
			}
		}
	}

	/* PATCH - "My friend found a way to dupe brews #541" https://github.com/DieReicheErethons/Brewery/issues/541
	 * Check if piston is pushing a BreweryCauldron and remove it
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPistonExtend(BlockPistonExtendEvent event) {
		for (Block block : event.getBlocks()) {
			if (BCauldron.bcauldrons.containsKey(block)) {
				BCauldron.remove(block);
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void oldCauldronChange(CauldronLevelChangeEvent event) {
		if (event.getNewLevel() == 0 && event.getOldLevel() != 0) {
			if (event.getReason() == CauldronLevelChangeEvent.ChangeReason.BOTTLE_FILL) {
				return;
			}
			BCauldron.remove(event.getBlock());
		} else if (event.getNewLevel() == 3 && event.getOldLevel() != 3) {
			BCauldron.remove(event.getBlock());
		}
	}
}
