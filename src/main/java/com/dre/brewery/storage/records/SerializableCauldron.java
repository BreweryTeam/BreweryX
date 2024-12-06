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

package com.dre.brewery.storage.records;

import com.dre.brewery.BCauldron;
import com.dre.brewery.BIngredients;
import com.dre.brewery.storage.DataManager;
import com.dre.brewery.utility.BUtil;
import org.bukkit.Location;

/**
 * Represents a cauldron that can be serialized.
 * @param id The UUID of the cauldron
 * @param serializedLocation The Block/Location of the cauldron
 * @param serializedIngredients Serialized BIngredients 'BIngredients.deserialize(String)'
 * @param state The state
 */
public record SerializableCauldron(String id, String serializedLocation, String serializedIngredients, int state) implements SerializableThing {
    public SerializableCauldron(BCauldron cauldron) {
        this(cauldron.getId().toString(), DataManager.serializeLocation(cauldron.getBlock().getLocation()), cauldron.getIngredients().serializeIngredients(), cauldron.getState());
    }

    public BCauldron toCauldron() {
        Location loc = DataManager.deserializeLocation(serializedLocation);
        if (loc == null) {
            return null;
        }
        return new BCauldron(loc.getBlock(), BIngredients.deserializeIngredients(serializedIngredients), state, BUtil.uuidFromString(id));
    }

    @Override
    public String getId() {
        return id;
    }
}
