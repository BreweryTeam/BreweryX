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

package com.dre.brewery.api.events.brew;

import com.dre.brewery.Brew;
import org.bukkit.event.Event;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public abstract class BrewEvent extends Event {
	protected final Brew brew;
	protected final ItemMeta meta;

	public BrewEvent(@NotNull Brew brew, @NotNull ItemMeta meta) {
		this.brew = brew;
		this.meta = meta;
	}

	@NotNull
	public Brew getBrew() {
		return brew;
	}

	/**
	 * Gets the Meta of the Item this Brew is attached to
	 */
	@NotNull
	public ItemMeta getItemMeta() {
		return meta;
	}
}
