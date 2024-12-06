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

package com.dre.brewery.recipe;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.utility.Logging;
import com.dre.brewery.utility.MinecraftVersion;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Simple Minecraft Item with just Material
 */
public class SimpleItem extends RecipeItem implements Ingredient {

	private static final MinecraftVersion VERSION = BreweryPlugin.getMCVersion();

	private Material mat;
	private short dur; // Old Mc


	public SimpleItem(Material mat) {
		this(mat, (short) 0);
	}

	public SimpleItem(Material mat, short dur) {
		this.mat = mat;
		this.dur = dur;
	}

	@Override
	public boolean hasMaterials() {
		return mat != null;
	}

	public Material getMaterial() {
		return mat;
	}

	@Override
	public List<Material> getMaterials() {
		List<Material> l = new ArrayList<>(1);
		l.add(mat);
		return l;
	}

	@NotNull
	@Override
	public Ingredient toIngredient(ItemStack forItem) {
		return ((SimpleItem) getMutableCopy());
	}

	@NotNull
	@Override
	public Ingredient toIngredientGeneric() {
		return ((SimpleItem) getMutableCopy());
	}

	@Override
	public boolean matches(ItemStack item) {
		if (!mat.equals(item.getType())) {
			return false;
		}
		//noinspection deprecation
		return VERSION.isOrLater(MinecraftVersion.V1_13) || dur == item.getDurability();
	}

	@Override
	public boolean matches(Ingredient ingredient) {
		if (isSimilar(ingredient)) {
			return true;
		}
		if (ingredient instanceof RecipeItem) {
			if (!((RecipeItem) ingredient).hasMaterials()) {
				return false;
			}
			if (ingredient instanceof CustomItem) {
				// Only match if the Custom Item also only defines material
				// If the custom item has more info like name and lore, it is not supposed to match a simple item
				CustomItem ci = (CustomItem) ingredient;
				return !ci.hasLore() && !ci.hasName() && mat == ci.getMaterial();
			}
		}
		return false;
	}

	@Override
	public boolean isSimilar(Ingredient item) {
		if (this == item) {
			return true;
		}
		if (item instanceof SimpleItem) {
			SimpleItem si = ((SimpleItem) item);
			return si.mat == mat && si.dur == dur;
		}
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		SimpleItem item = (SimpleItem) o;
		return dur == item.dur &&
			mat == item.mat;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), mat, dur);
	}

	@Override
	public String toString() {
		return "SimpleItem{" +
			"mat=" + mat.name().toLowerCase() +
			" amount=" + getAmount() +
			'}';
	}

	@Override
	public void saveTo(DataOutputStream out) throws IOException {
		out.writeUTF("SI");
		out.writeUTF(mat.name());
		out.writeShort(dur);
	}

	public static SimpleItem loadFrom(ItemLoader loader) {
		try {
			DataInputStream in = loader.getInputStream();
			Material mat = Material.getMaterial(in.readUTF());
			short dur = in.readShort();
			if (mat != null) {
				SimpleItem item = new SimpleItem(mat, dur);
				return item;
			}
		} catch (IOException e) {
			Logging.errorLog("Failed to load SimpleItem", e);
		}
		return null;
	}

	// Needs to be called at Server start
	public static void registerItemLoader(BreweryPlugin breweryPlugin) {
		breweryPlugin.registerForItemLoader("SI", SimpleItem::loadFrom);
	}

}

