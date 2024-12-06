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

import com.dre.brewery.BPlayer;

/**
 * Represents a player that can be serialized.
 * @param id The UUID of the player
 * @param quality The quality of the player
 * @param drunkenness The drunkenness of the player
 * @param offlineDrunkenness The offline drunkenness of the player
 */
public record SerializableBPlayer(String id, int quality, int drunkenness, int offlineDrunkenness) implements SerializableThing {
    public SerializableBPlayer(BPlayer player) {
        this(player.getUuid(), player.getQuality(), player.getDrunkeness(), player.getOfflineDrunkeness());
    }

    public BPlayer toBPlayer() {
        return new BPlayer(id, quality, drunkenness, offlineDrunkenness);
    }

    @Override
    public String getId() {
        return id;
    }
}
