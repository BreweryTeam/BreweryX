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

package com.dre.brewery.lore;

import org.bukkit.inventory.meta.ItemMeta;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

public class LoreLoadStream extends ByteArrayInputStream {

	public static final String IDENTIFIER = "§%";

	public LoreLoadStream(ItemMeta meta) throws IllegalArgumentException {
		this(meta, -1);
	}

	public LoreLoadStream(ItemMeta meta, int line) throws IllegalArgumentException {
		super(loreToBytes(meta, line));
	}

	private static byte[] loreToBytes(ItemMeta meta, int lineNum) throws IllegalArgumentException {
		if (meta.hasLore()) {
			List<String> lore = meta.getLore();
			if (lineNum >= 0) {
				String line = lore.get(lineNum);
				if (line.startsWith(IDENTIFIER)) {
					return loreLineToBytes(line);
				}
			}
			for (String line : lore) {
				if (line.startsWith(IDENTIFIER)) {
					return loreLineToBytes(line);
				}
			}
		}
		throw new IllegalArgumentException("Meta has no data in lore");
	}

	private static byte[] loreLineToBytes(String line) {
		StringBuilder build = new StringBuilder((int) (line.length() / 2F));
		byte skip = 2;
		for (char c : line.toCharArray()) {
			if (skip > 0) {
				skip--;
				continue;
			}
			if (c == '§') continue;
			build.append(c);
		}
		return build.toString().getBytes();
	}
}
