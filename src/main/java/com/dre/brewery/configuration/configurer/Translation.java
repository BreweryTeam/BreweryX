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

package com.dre.brewery.configuration.configurer;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * List of the available translation<br />
 * Should we even use an enum here?<br />
 * <br />
 * The comments can be found under {@code langs/LANGUAGE.yml}
 */
@Getter
@AllArgsConstructor
public enum Translation {

	// Languages added should have a config and a lang translation (resources/config-langs/, resources/languages/)

	EN("en.yml"),
	DE("de.yml"),
	ES("es.yml"),
	FR("fr.yml"),
	IT("it.yml"),
	RU("ru.yml"),
	ZH("zh.yml");

	private final String filename;
}
