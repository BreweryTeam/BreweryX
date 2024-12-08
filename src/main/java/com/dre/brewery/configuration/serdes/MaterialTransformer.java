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

package com.dre.brewery.configuration.serdes;

import com.dre.brewery.utility.MaterialUtil;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import org.bukkit.Material;

public class MaterialTransformer extends BidirectionalTransformer<String, Material> {
	@Override
	public GenericsPair<String, Material> getPair() {
		return this.genericsPair(String.class, Material.class);
	}

	@Override
	public Material leftToRight(@NonNull String data, @NonNull SerdesContext serdesContext) {
		return MaterialUtil.getMaterialSafely(data); // handles grass -> short_grass
	}

	@Override
	public String rightToLeft(@NonNull Material data, @NonNull SerdesContext serdesContext) {
		return data.toString();
	}
}
