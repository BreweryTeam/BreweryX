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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public enum BarrelAsset {
	PLANKS, // Base block
	STAIRS, // Alt 1 Block
	SIGN, // Alt 2 Block
	FENCE; // Optional: Alt 3 Block


	public static final Map<BarrelAsset, Set<Material>> BARREL_ASSET_LIST_MAP = new HashMap<>();
	static {
		for (BarrelAsset asset : values()) {
			BARREL_ASSET_LIST_MAP.put(asset, new HashSet<>());
		}
	}

	public static void addBarrelAsset(BarrelAsset asset, Material... materials) {
		if (materials == null || materials.length == 0) {
			return;
		}
		Collections.addAll(BARREL_ASSET_LIST_MAP.get(asset), Arrays.stream(materials).filter(Objects::nonNull).toArray(Material[]::new));
	}

	public static boolean isBarrelAsset(BarrelAsset assetType, Material material) {
		return BARREL_ASSET_LIST_MAP.get(assetType).contains(material);
	}
}
