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

import java.io.ByteArrayInputStream;

public class NBTLoadStream extends ByteArrayInputStream {
	private static final String TAG = "brewdata";
	private static final NamespacedKey KEY = new NamespacedKey(BreweryPlugin.getInstance(), TAG);
	private static final NamespacedKey LEGACY_KEY = new NamespacedKey("brewery", TAG.toLowerCase());

	public NBTLoadStream(ItemMeta meta) {
		super(getNBTBytes(meta));
	}

	private static byte[] getNBTBytes(ItemMeta meta) {
		byte[] bytes = NBTUtil.readBytesItem(meta, KEY);
		if (bytes == null) {
			bytes = NBTUtil.readBytesItem(meta, LEGACY_KEY);
		}
		if (bytes == null) {
			return new byte[0];
		}
		return bytes;
	}

	public boolean hasData() {
		return count > 0;
	}

	public static boolean hasDataInMeta(ItemMeta meta) {
		return NBTUtil.hasBytesItem(meta, KEY) || NBTUtil.hasBytesItem(meta, LEGACY_KEY);
	}
}
