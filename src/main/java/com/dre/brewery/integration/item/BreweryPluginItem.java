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

import com.dre.brewery.Brew;
import com.dre.brewery.recipe.BRecipe;
import com.dre.brewery.recipe.PluginItem;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

/**
 * For recipes that use Brewery Items as input
 */
public class BreweryPluginItem extends PluginItem {

// When implementing this, put Brewery as softdepend in your plugin.yml!
// We're calling this as server start:
// PluginItem.registerForConfig("brewery", BreweryPluginItem::new);

	@Override
	public boolean matches(ItemStack item) {
		Brew brew = Brew.get(item);
		if (brew != null) {
			BRecipe recipe = brew.getCurrentRecipe();
			if (recipe != null) {
				return recipe.getRecipeName().equalsIgnoreCase(getItemId()) || recipe.getName(10).equalsIgnoreCase(getItemId());
			}
			return ChatColor.stripColor(item.getItemMeta().getDisplayName()).equalsIgnoreCase(getItemId());
		}
		return false;
	}
}
