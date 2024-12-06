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

import com.dre.brewery.BreweryPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.material.Cauldron;
import org.bukkit.material.MaterialData;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class MaterialUtil {

	private static final MinecraftVersion VERSION = BreweryPlugin.getMCVersion();

	// <= 1.12.2 methods
	// These will be rarely used
	private static final Method GET_BLOCK_TYPE_ID_AT = getMethod(World.class, "getBlockTypeIdAt", Location.class);
	private static final Method SET_DATA = getMethod(Bukkit.getServer().getClass().getPackage().getName() + ".block.CraftBlock", "setData", byte.class);

	// Removed in 1.13
	public static final Material STATIONARY_LAVA = getMaterialSafely("STATIONARY_LAVA");


	// Cauldron stuff
	public static final byte EMPTY = 0, SOME = 1, FULL = 2;
	public static final Material WATER_CAULDRON = getMaterialSafely("WATER_CAULDRON");
	public static final Material MAGMA_BLOCK = getV1_13MaterialSafely("MAGMA_BLOCK", "MAGMA");
	public static final Material CAMPFIRE = getMaterialSafely("CAMPFIRE");
	public static final Material SOUL_CAMPFIRE = getMaterialSafely("SOUL_CAMPFIRE");
	public static final Material SOUL_FIRE = getMaterialSafely("SOUL_FIRE");
	public static final Material CLOCK = getV1_13MaterialSafely("CLOCK", "WATCH");


	public static Method getMethod(String clazz, String name, Class<?>... parameterTypes) {
		try {
			return Class.forName(clazz).getDeclaredMethod(name, parameterTypes);
		} catch (NoSuchMethodException | SecurityException | ClassNotFoundException e) {
			return null;
		}
	}

	public static Method getMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
		try {
			return clazz.getDeclaredMethod(name, parameterTypes);
		} catch (NoSuchMethodException | SecurityException e) {
			return null;
		}
	}



	public static Material getMaterialSafely(String name) {
		try {
			if (name.equalsIgnoreCase("GRASS")) { // 1.20.6 -> renamed to short_grass
				return Material.GRASS;
			}
			return Material.matchMaterial(name);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static Material getV1_13MaterialSafely(String newName, String oldName) {
		try {
			return Material.valueOf(VERSION.isOrLater(MinecraftVersion.V1_13) ? newName : oldName);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}


	public static boolean isCauldronHeatSource(Block block) {
		Material type = block.getType();
		return type == Material.FIRE || type == SOUL_FIRE || type == MAGMA_BLOCK || litCampfire(block) || isLava(type);
	}

	// LAVA and STATIONARY_LAVA are merged as of 1.13
	public static boolean isLava(Material type) {
		return type == Material.LAVA || (VERSION.isOrEarlier(MinecraftVersion.V1_13) && type == STATIONARY_LAVA);
	}

	public static boolean litCampfire(Block block) {
		if (block.getType() == CAMPFIRE || block.getType() == SOUL_CAMPFIRE) {
			BlockData data = block.getBlockData();
			if (data instanceof org.bukkit.block.data.Lightable) {
				return ((org.bukkit.block.data.Lightable) data).isLit();
			}
		}
		return false;
	}

	public static boolean isBottle(Material type) {
		if (type == Material.POTION) return true;
		if (BreweryPlugin.getMCVersion().isOrEarlier(MinecraftVersion.V1_9)) return false;
		if (type == Material.LINGERING_POTION || type == Material.SPLASH_POTION) return true;
		if (VERSION.isOrEarlier(MinecraftVersion.V1_13)) return false;
		if (type == Material.EXPERIENCE_BOTTLE) return true;
		if (type.name().equals("DRAGON_BREATH")) return true;
		return type.name().equals("HONEY_BOTTLE");
	}

	public static boolean areStairsInverted(Block block) {
		if (VERSION.isOrEarlier(MinecraftVersion.V1_13)) {
			MaterialData data = block.getState().getData();
			return data instanceof org.bukkit.material.Stairs && (((org.bukkit.material.Stairs) data).isInverted());
		} else {
			BlockData data = block.getBlockData();
			return data instanceof org.bukkit.block.data.type.Stairs && ((org.bukkit.block.data.type.Stairs) data).getHalf() == org.bukkit.block.data.type.Stairs.Half.TOP;
		}
	}


	/**
	 * Test if this Material Type is a Cauldron filled with water, or any cauldron in 1.16 and lower
	 */
	public static boolean isWaterCauldron(Material type) {
		return WATER_CAULDRON != null ? type == WATER_CAULDRON : type == Material.CAULDRON;
	}

	/**
	 * Get The Fill Level of a Cauldron Block, 0 = empty, 1 = something in, 2 = full
	 *
	 * @return 0 = empty, 1 = something in, 2 = full
	 */
	public static byte getFillLevel(Block block) {
		if (!isWaterCauldron(block.getType())) {
			return EMPTY;
		}

		if (VERSION.isOrLater(MinecraftVersion.V1_13)) {
			Levelled cauldron = ((Levelled) block.getBlockData());
			if (cauldron.getLevel() == 0) {
				return EMPTY;
			} else if (cauldron.getLevel() == cauldron.getMaximumLevel()) {
				return FULL;
			} else {
				return SOME;
			}

		} else {
			Cauldron cauldron = (Cauldron) block.getState().getData();
			if (cauldron.isEmpty()) {
				return EMPTY;
			} else if (cauldron.isFull()) {
				return FULL;
			} else {
				return SOME;
			}
		}
	}

	// Only used for very old versions of LogBlock
	public static int getBlockTypeIdAt(Location location) {
		try {
			return GET_BLOCK_TYPE_ID_AT != null ? (int) GET_BLOCK_TYPE_ID_AT.invoke(location.getWorld(), location) : 0;
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			return 0;
		}
	}

	// Setting byte data to blocks for older versions
	public static void setData(Block block, byte data) {
		try {
			SET_DATA.invoke(block, data);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ignored) {
		}
	}


}
