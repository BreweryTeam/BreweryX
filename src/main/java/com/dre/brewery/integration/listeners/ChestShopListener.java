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

package com.dre.brewery.integration.listeners;

import com.Acrobot.ChestShop.Events.ShopCreatedEvent;
import com.dre.brewery.Brew;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.integration.Hook;
import com.dre.brewery.utility.Logging;
import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public class ChestShopListener implements Listener {

	private final Lang lang = ConfigManager.getConfig(Lang.class);

	@EventHandler
	public void onShopCreated(ShopCreatedEvent event) {
		try {
			Container container = event.getContainer();
			if (container != null) {
				for (ItemStack item : container.getInventory().getContents()) {
					if (item != null && item.getType() == Material.POTION) {
						Brew brew = Brew.get(item);
						if (brew != null && !brew.isSealed()) {
							event.getPlayer().sendTitle("", lang.getEntry("Player_ShopSealBrew"), 10, 70, 20);
							return;
						}
					}
				}
			}
		} catch (Throwable e) {
			HandlerList.unregisterAll(this);
			Hook.CHESTSHOP.setEnabled(false);
			Logging.errorLog("Failed to notify Player using ChestShop. Disabling ChestShop support", e);
		}
	}
}
