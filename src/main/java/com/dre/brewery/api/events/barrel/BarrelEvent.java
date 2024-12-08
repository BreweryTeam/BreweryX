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

package com.dre.brewery.api.events.barrel;

import com.dre.brewery.Barrel;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;

public abstract class BarrelEvent extends Event {
	protected final Barrel barrel;

	public BarrelEvent(Barrel barrel) {
		this.barrel = barrel;
	}

	public Barrel getBarrel() {
		return barrel;
	}

	public Inventory getInventory() {
		return barrel.getInventory();
	}

	/**
	 * @return The Spigot Block of the Barrel, usually Sign or a Fence
	 */
	public Block getSpigot() {
		return barrel.getSpigot();
	}
}
