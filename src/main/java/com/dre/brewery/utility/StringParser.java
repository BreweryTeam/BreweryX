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

package com.dre.brewery.utility;

public class StringParser {

	public static Tuple<Integer, String> parseQuality(String line, ParseType type) {
		line = BUtil.color(line);
		int plus = 0;
		if (line.startsWith("+++")) {
			plus = 3;
			line = line.substring(3);
		} else if (line.startsWith("++")) {
			plus = 2;
			line = line.substring(2);
		} else if (line.startsWith("+")) {
			plus = 1;
			line = line.substring(1);
		}
		if (line.startsWith(" ")) {
			line = line.substring(1);
		}

		if (type == ParseType.CMD && line.startsWith("/")) {
			line = line.substring(1);
		}

		if (type == ParseType.LORE && !line.startsWith("§")) {
			line = "§9" + line;
		}
		return new Tuple<>(plus, line);
	}

	public enum ParseType {
		LORE,
		CMD,
		OTHER
	}
}
