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
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * A Barrel is being removed.
 * <p>There may have been a BarrelDestroyEvent before this.
 * If not, Worldedit, other Plugins etc may be the cause for unexpected removal
 */
public class BarrelRemoveEvent extends BarrelEvent {
	private static final HandlerList handlers = new HandlerList();
	private boolean dropItems;

	public BarrelRemoveEvent(Barrel barrel, boolean dropItems) {
		super(barrel);
		this.dropItems = dropItems;
	}

	public boolean willDropItems() {
		return dropItems;
	}

	/**
	 * @param dropItems Should the Items contained in this Barrel drop to the ground?
	 */
	public void setShouldDropItems(boolean dropItems) {
		this.dropItems = dropItems;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	// Required by Bukkit
	public static HandlerList getHandlerList() {
		return handlers;
	}
}
