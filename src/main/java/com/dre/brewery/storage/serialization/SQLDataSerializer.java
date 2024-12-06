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

package com.dre.brewery.storage.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Modifier;
import java.util.Base64;

/**
 * Serializes the given object to JSON and then to a Base64 encoded string.
 */
public class SQLDataSerializer {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithModifiers(Modifier.STATIC).create();

    public String serialize(Object object) {
        return Base64.getEncoder().encodeToString(gson.toJson(object).getBytes());
    }

    public <T> T deserialize(String data, Class<T> type) {
        return gson.fromJson(new String(Base64.getDecoder().decode(data)), type);
    }

    public <T> T deserialize(String data, Class<T> type, T defaultValue) {
        try {
            return deserialize(data, type);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
