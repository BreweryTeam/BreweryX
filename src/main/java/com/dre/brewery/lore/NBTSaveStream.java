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

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.utility.NBTUtil;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class NBTSaveStream extends ByteArrayOutputStream {
	private static final String TAG = "brewdata";
	private static final NamespacedKey KEY = new NamespacedKey(BreweryPlugin.getInstance(), TAG);

	private final ItemMeta meta;

	public NBTSaveStream(ItemMeta meta) {
		super(128);
		this.meta = meta;
	}

	@Override
	public void flush() throws IOException {
		super.flush();
		if (size() <= 0) return;
		NBTUtil.writeBytesItem(toByteArray(), meta, KEY);
	}
}
