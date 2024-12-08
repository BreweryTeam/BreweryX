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
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

// Nexo is written using Java 21, to not sacrifice current compatability with lower MC versions, we will not be using Nexo's API.
// We'll be using Bukkit's PDC to check it the ItemStack is a Nexo item (which Nexo uses anyway)
public class NexoPluginItem extends PluginItem {
    @Override
    public boolean matches(ItemStack itemStack) {
        if (!Hook.NEXO.isEnabled()) {
            return false;
        }

        NamespacedKey ITEM_ID = new NamespacedKey(Hook.NEXO.getPlugin(), "id");
        if (itemStack == null || itemStack.getItemMeta() == null) {
            return false;
        }

        String itemId = itemStack.getItemMeta().getPersistentDataContainer().get(ITEM_ID, PersistentDataType.STRING);
        if (itemId == null) {
            return false;
        }
        return itemId.equals(this.getItemId());
    }

    // Nexo's internal API code for getting an item's ID:
    //val ITEM_ID = NamespacedKey(NexoPlugin.instance(), "id")
    //@JvmStatic
    //    fun idFromItem(item: ItemStack?) = item?.itemMeta?.persistentDataContainer?.get(ITEM_ID, PersistentDataType.STRING)

    // API Usage if we were to use Nexo's API:
    // String itemId = NexoItems.idFromItem(itemStack);
}
