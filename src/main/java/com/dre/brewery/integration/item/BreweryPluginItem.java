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
        if (brew == null) {
            return false;
        }
        return isMatchingBrew(brew) || isCauldronIngredient(item);
    }

    // Checks if an ItemStack is a Brewery Brew with the correct recipe by comparing the ids/names
    private boolean isMatchingBrew(Brew brew) {
        BRecipe recipe = brew.getCurrentRecipe();
        if (recipe != null) {
            // We *could* add support for names instead of just using the ids
            return this.getItemId().equalsIgnoreCase(recipe.getId()) ||
                this.getItemId().equalsIgnoreCase(recipe.getRecipeName()) || // From original Impl
                this.getItemId().equalsIgnoreCase(recipe.getName(10)); // From original Impl
        }
        return false;
    }

    // I truly hate doing it this way, but to have it be checked based on ids
    // I'd have to rewrite major parts of BRecipe and BCauldronRecipe - Jsinco
    private boolean isCauldronIngredient(ItemStack item) {
        if (!item.hasItemMeta()) return false;
        return ChatColor.stripColor(item.getItemMeta().getDisplayName()).equalsIgnoreCase(this.getItemId()); // From original Impl
    }
}
