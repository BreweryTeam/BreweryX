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

package com.dre.brewery.integration.item;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.integration.Hook;
import com.dre.brewery.recipe.PluginItem;
import com.dre.brewery.utility.Logging;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.inventory.ItemStack;

public class SlimefunPluginItem extends PluginItem {

// When implementing this, put Brewery as softdepend in your plugin.yml!
// We're calling this as server start:
// PluginItem.registerForConfig("slimefun", SlimefunPluginItem::new);
// PluginItem.registerForConfig("exoticgarden", SlimefunPluginItem::new);

	@Override
	public boolean matches(ItemStack item) {
		if (!Hook.SLIMEFUN.isEnabled()) return false;

		try {
			SlimefunItem sfItem = SlimefunItem.getByItem(item);
			if (sfItem != null) {
				return sfItem.getId().equalsIgnoreCase(getItemId());
			}
		} catch (Exception | LinkageError e) {
			Logging.errorLog("Could not check Slimefun for Item ID", e);
			Hook.SLIMEFUN.setEnabled(false);
		}
		return false;
	}
}
