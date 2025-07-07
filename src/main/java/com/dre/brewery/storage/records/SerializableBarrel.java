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

import com.dre.brewery.Barrel;
import com.dre.brewery.storage.DataManager;
import com.dre.brewery.storage.interfaces.SerializableThing;
import com.dre.brewery.storage.serialization.BukkitSerialization;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.BoundingBox;
import org.bukkit.Location;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a barrel that can be serialized.
 *
 * @param id                 The UUID of the barrel
 * @param serializedLocation The Block/Location of the Spigot of the barrel
 * @param bounds             The bounds of the barrel
 * @param time               no idea
 * @param sign               The sign byte offset the barrel
 * @param serializedItems    Serialized ItemStacks 'BukkitSerialization.itemStackArrayToBase64(ItemStack[])'
 */
public record SerializableBarrel(String id, String serializedLocation, List<Integer> bounds, float time, byte sign,
                                 String serializedItems) implements SerializableThing {
    public SerializableBarrel(Barrel barrel) {
        this(barrel.getId().toString(), DataManager.serializeLocation(barrel.getSpigot().getLocation()), barrel.getBounds().serializeToIntList(), barrel.getTime(), barrel.getSignoffset(), BukkitSerialization.itemStackArrayToBase64(barrel.getInventory().getContents()));
    }

    public CompletableFuture<Barrel> toBarrel() {
        Location loc = DataManager.deserializeLocation(serializedLocation);
        if (loc == null) {
            return null;
        }
        return Barrel.computeSmall(loc).thenApplyAsync(small ->
            new Barrel(loc.getBlock(), sign, BoundingBox.fromPoints(bounds), BukkitSerialization.itemStackArrayFromBase64(serializedItems), time, BUtil.uuidFromString(id), small)
        );
    }

    @Override
    public String getId() {
        return id;
    }
}
