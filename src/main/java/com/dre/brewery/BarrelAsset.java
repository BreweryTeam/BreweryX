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

package com.dre.brewery;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public enum BarrelAsset {
    PLANKS, // Base block
    STAIRS, // Alt 1 Block
    SIGN, // Alt 2 Block
    FENCE; // Optional: Alt 3 Block


    private static final Map<BarrelWoodType, Map<BarrelAsset, Set<Material>>> BARREL_ASSET_LIST_MAP = new EnumMap<>(BarrelWoodType.class);

    static {
        for (BarrelWoodType type : BarrelWoodType.values()) {
            HashMap<BarrelAsset, Set<Material>> newMap = new HashMap<>();
            for (BarrelAsset asset : values()) {
                newMap.put(asset, EnumSet.noneOf(Material.class));
            }

            BARREL_ASSET_LIST_MAP.put(type, newMap);
        }
    }

    public static void addBarrelAsset(BarrelWoodType type, BarrelAsset asset, Material... materials) {
        if (materials == null || materials.length == 0) {
            return;
        }

        Collections.addAll(BARREL_ASSET_LIST_MAP.get(type).get(asset), Arrays.stream(materials).filter(Objects::nonNull).toArray(Material[]::new));
    }

    public static boolean isBarrelAsset(BarrelAsset assetType, Material material) {
        if (material == null) {
            return false;
        }

        return BARREL_ASSET_LIST_MAP.values().stream()
            .map((b) -> b.get(assetType))
            .anyMatch((materialSet) -> materialSet.contains(material));
    }

    public static Set<Material> getMaterialsOf(BarrelWoodType type) {
        var output = EnumSet.noneOf(Material.class);
        BARREL_ASSET_LIST_MAP.get(type).values().forEach(output::addAll);
        return output;
    }
}
