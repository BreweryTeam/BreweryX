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

import com.dre.brewery.integration.Hook;
import com.dre.brewery.recipe.PluginItem;
import com.dre.brewery.utility.Logging;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.inventory.ItemStack;

public class MMOItemsPluginItem extends PluginItem {

// When implementing this, put Brewery as softdepend in your plugin.yml!
// We're calling this as server start:
// PluginItem.registerForConfig("mmoitems", MMOItemsPluginItem::new);

	@Override
	public boolean matches(ItemStack item) {
		if (!Hook.MMOITEMS.isEnabled()) return false;

		try {
			NBTItem nbtItem = NBTItem.get(item);
			return nbtItem.hasType() && nbtItem.getString("MMOITEMS_ITEM_ID").equalsIgnoreCase(getItemId());
		} catch (Throwable e) {
			Logging.errorLog("Could not check MMOItems for Item ID", e);
			Hook.MMOITEMS.setEnabled(false);
			return false;
		}
	}
}
