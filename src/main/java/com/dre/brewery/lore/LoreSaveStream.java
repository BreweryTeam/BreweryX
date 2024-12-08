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

import com.dre.brewery.utility.BUtil;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LoreSaveStream extends ByteArrayOutputStream {

	public static final String IDENTIFIER = "§%";

	private ItemMeta meta;
	private int line;
	private boolean flushed = false;

	public LoreSaveStream(ItemMeta meta) {
		this(meta, -1);
	}

	public LoreSaveStream(ItemMeta meta, int line) {
		super(128);
		this.meta = meta;
		this.line = line;
	}

	// Writes to the Lore
	// Without calling this, the ItemMeta remains unchanged
	@Override
	public void flush() throws IOException {
		super.flush();
		if (size() <= 0) return;
		if (flushed || meta == null) {
			// Dont write twice
			return;
		}
		flushed = true;
		String s = toString();

		StringBuilder loreLineBuilder = new StringBuilder((s.length() * 2) + 6);
		loreLineBuilder.append(IDENTIFIER);
		for (char c : s.toCharArray()) {
			loreLineBuilder.append('§').append(c);
		}
		List<String> lore;
		if (meta.hasLore()) {
			lore = meta.getLore();
		} else {
			lore = new ArrayList<>();
		}
		int prev = 0;
		for (Iterator<String> iterator = lore.iterator(); iterator.hasNext(); ) {
			if (iterator.next().startsWith(IDENTIFIER)) {
				iterator.remove();
				break;
			}
			prev++;
		}
		if (line < 0) {
			if (prev >= 0) {
				line = prev;
			} else {
				line = lore.size();
			}
		}
		while (lore.size() < line) {
			lore.add("");
		}
		lore.add(line, loreLineBuilder.toString());
		meta.setLore(lore);
	}

	@Override
	public void close() throws IOException {
		super.close();
		meta = null;
	}
}
