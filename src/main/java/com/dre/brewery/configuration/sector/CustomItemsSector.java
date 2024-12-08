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

package com.dre.brewery.configuration.sector;

import com.dre.brewery.configuration.sector.capsule.ConfigCustomItem;
import eu.okaeri.configs.annotation.CustomKey;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CustomItemsSector extends AbstractOkaeriConfigSector<ConfigCustomItem> {

	@CustomKey("ex-item")
	ConfigCustomItem ex_item = ConfigCustomItem.builder()
			.material("Barrier")
			.name("Wall")
			.lore(List.of("&7Very well protected"))
			.build();

	@CustomKey("ex-item2")
	ConfigCustomItem ex_item2 = ConfigCustomItem.builder()
			.matchAny(true)
			.material(List.of("Acacia_Door", "Oak_Door", "Spruce_Door"))
			.name(List.of("Beechwood Door"))
			.lore(List.of("A door"))
			.build();

	ConfigCustomItem rasp = ConfigCustomItem.builder()
			.name("&cRaspberry")
			.build();

	ConfigCustomItem modelitem = ConfigCustomItem.builder()
			.material("paper")
			.customModelData(List.of(10234, 30334))
			.build();

	@CustomKey("blue-flowers")
	ConfigCustomItem blue_flowers = ConfigCustomItem.builder()
			.matchAny(true)
			.material(List.of("cornflower", "blue_orchid"))
			.build();
}
