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

package com.dre.brewery;

import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.utility.Logging;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Barrel;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class MCBarrel {

	public static final String TAG = "Btime";

	public static long mcBarrelTime; // Globally stored Barrel time. Difference between this and the time stored on each mc-barrel will give the barrel age time
	public static List<MCBarrel> openBarrels = new ArrayList<>();
	private static final Config config = ConfigManager.getConfig(Config.class);
	private static final Lang lang = ConfigManager.getConfig(Lang.class);

	private byte brews = -1; // How many Brewery Brews are in this Barrel
	private final Inventory inv;
	private final int invSize;


	public MCBarrel(Inventory inv) {
		this.inv = inv;
		invSize = inv.getSize();
	}


	// Now Opening this Barrel for a player
	public void open() {
		// if nobody had the inventory opened
		if (inv.getViewers().size() == 1 && inv.getHolder() instanceof org.bukkit.block.Barrel) {
			Barrel barrel = (Barrel) inv.getHolder();
			PersistentDataContainer data = barrel.getPersistentDataContainer();
			NamespacedKey key = new NamespacedKey(BreweryPlugin.getInstance(), TAG);
			if (!data.has(key, PersistentDataType.LONG)) {
				key = new NamespacedKey("brewery", TAG.toLowerCase()); // Legacy key
			}

			// Check for legacy key
			if (!data.has(key, PersistentDataType.LONG)) {
				return;
			}

			// Get the difference between the time that is stored on the Barrel and the current stored global mcBarrelTime
			long time = mcBarrelTime - data.getOrDefault(key, PersistentDataType.LONG, mcBarrelTime);
			data.remove(key);
			barrel.update();
			Logging.debugLog("Barrel Time since last open: " + time);

			if (time > 0) {
				brews = 0;
				// if inventory contains potions
				if (inv.contains(Material.POTION)) {
					long loadTime = System.nanoTime();
					for (ItemStack item : inv.getContents()) {
						if (item != null) {
							Brew brew = Brew.get(item);
							if (brew != null && !brew.isStatic()) {
								if (brews < config.getMaxBrewsInMCBarrels() || config.getMaxBrewsInMCBarrels() < 0) {
									// The time is in minutes, but brew.age() expects time in mc-days
									brew.age(item, ((float) time) / 20f, BarrelWoodType.OAK);
								}
								brews++;
							}
						}
					}
					if (config.isDebug()) {
						loadTime = System.nanoTime() - loadTime;
						float ftime = (float) (loadTime / 1000000.0);
						Logging.debugLog("opening MC Barrel with potions (" + ftime + "ms)");
					}
				}
			}
		}
	}

	// Closing Inventory. Check if we need to set a time on the Barrel
	public void close() {
		if (inv.getViewers().size() == 1) {
			// This is the last viewer
			for (ItemStack item : inv.getContents()) {
				if (item != null) {
					if (Brew.isBrew(item)) {
						// We found a brew, so set time on this Barrel
						if (inv.getHolder() instanceof org.bukkit.block.Barrel) {
							Barrel barrel = (Barrel) inv.getHolder();
							PersistentDataContainer data = barrel.getPersistentDataContainer();
							data.set(new NamespacedKey(BreweryPlugin.getInstance(), TAG), PersistentDataType.LONG, mcBarrelTime);
							barrel.update();
						}
						return;
					}
				}
			}
			// No Brew found, ignore this Barrel
		}
	}

	public void countBrews() {
		brews = 0;
		for (ItemStack item : inv.getContents()) {
			if (item != null) {
				if (Brew.isBrew(item)) {
					brews++;
				}
			}
		}
	}

	public Inventory getInventory() {
		return inv;
	}


	public static void onUpdate() {
		if (config.isAgeInMCBarrels()) {
			mcBarrelTime++;
		}
	}

	// Used to visually stop Players from placing more than 6 (configurable) brews in the MC Barrels.
	// There are still methods to place more Brews in that would be too tedious to catch.
	// This is only for direct visual Notification, the age routine above will never age more than 6 brews in any case.
	public void clickInv(InventoryClickEvent event) {
		if (config.getMaxBrewsInMCBarrels() >= invSize || config.getMaxBrewsInMCBarrels() < 0) {
			// There are enough brews allowed to fill the inventory, we don't need to keep track
			return;
		}
		boolean adding = false;
		switch (event.getAction()) {
			case PLACE_ALL:
			case PLACE_ONE:
			case PLACE_SOME:
			case SWAP_WITH_CURSOR:
				// Placing Brew in MC Barrel
				if (event.getCursor() != null && event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.BARREL && event.getCursor().getType() == Material.POTION) {
					if (Brew.isBrew(event.getCursor())) {
						if (event.getAction() == InventoryAction.SWAP_WITH_CURSOR && event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.POTION) {
							if (Brew.isBrew(event.getCurrentItem())) {
								// The item we are swapping with is also a brew, dont change the count and allow
								break;
							}
						}
						adding = true;
					}
				}
				break;
			case MOVE_TO_OTHER_INVENTORY:
				if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.POTION && event.getClickedInventory() != null) {
					if (event.getClickedInventory().getType() == InventoryType.BARREL) {
						// Moving Brew out of MC Barrel
						if (Brew.isBrew(event.getCurrentItem())) {
							if (brews == -1) {
								countBrews();
							}
							brews--;
						}
						break;
					} else if (event.getClickedInventory().getType() == InventoryType.PLAYER) {
						// Moving Brew into MC Barrel
						if (Brew.isBrew(event.getCurrentItem())) {
							adding = true;
						}
					}
				}
				break;

			case PICKUP_ALL:
			case PICKUP_ONE:
			case PICKUP_HALF:
			case PICKUP_SOME:
			case COLLECT_TO_CURSOR:
				// Pickup Brew from MC Barrel
				if (event.getCurrentItem() != null && event.getClickedInventory() != null && event.getClickedInventory().getType() == InventoryType.BARREL && event.getCurrentItem().getType() == Material.POTION) {
					if (Brew.isBrew(event.getCurrentItem())) {
						if (brews == -1) {
							countBrews();
						}
						brews--;
					}
				}
				break;
			case HOTBAR_MOVE_AND_READD:
			case HOTBAR_SWAP:
				brews = -1;
				break;
			default:
				return;
		}
		if (adding) {
			if (brews == -1) {
				countBrews();
			}
			if (brews >= config.getMaxBrewsInMCBarrels()) {
				event.setCancelled(true);
				lang.sendEntry(event.getWhoClicked(), "Player_BarrelFull");
			} else {
				brews++;
			}
		}
	}

}
