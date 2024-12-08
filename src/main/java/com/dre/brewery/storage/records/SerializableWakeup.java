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

import com.dre.brewery.Wakeup;
import com.dre.brewery.storage.DataManager;
import com.dre.brewery.utility.BUtil;
import org.bukkit.Location;

/**
 * Represents a wakeup that can be serialized.
 * @param id The UUID of the wakeup
 * @param serializedLocation The Location of the wakeup
 */
public record SerializableWakeup(String id, String serializedLocation) implements SerializableThing {
    public SerializableWakeup(Wakeup wakeup) {
        this(wakeup.getId().toString(), DataManager.serializeLocation(wakeup.getLoc(), true));
    }

    public Wakeup toWakeup() {
        Location loc = DataManager.deserializeLocation(serializedLocation, true);
        if (loc == null) {
            return null;
        }
        return new Wakeup(loc, BUtil.uuidFromString(id));
    }

    @Override
    public String getId() {
        return id;
    }
}
