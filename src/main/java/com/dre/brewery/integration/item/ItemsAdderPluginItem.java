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
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.inventory.ItemStack;

public class ItemsAdderPluginItem extends PluginItem {
    @Override
    public boolean matches(ItemStack itemStack) {
        if (!Hook.ITEMSADDER.isEnabled()) {
            return false;
        }

        CustomStack cs = CustomStack.byItemStack(itemStack);
        if (cs == null) {
            return false;
        }

        if (!this.getItemId().contains(":")) {
            return cs.getId().equals(this.getItemId());
        } else {
            return cs.getNamespacedID().equals(this.getItemId());
        }
    }
}
