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

import com.dre.brewery.Brew;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.integration.Hook;
import com.dre.brewery.utility.Logging;
import com.nisovin.shopkeepers.api.events.PlayerOpenUIEvent;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class ShopKeepersListener implements Listener {
	private final Set<HumanEntity> openedEditors = new HashSet<>();
	private final Lang lang = ConfigManager.getConfig(Lang.class);

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onShopkeeperOpen(PlayerOpenUIEvent event) {
		try {
			if (event.getUIType() == DefaultUITypes.EDITOR() || event.getUIType() == DefaultUITypes.TRADING()) {
				openedEditors.add(event.getPlayer());
			}
		} catch (Throwable e) {
			failed(e);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onInventoryClickShopKeeper(InventoryClickEvent event) {
		if (openedEditors.isEmpty() || !openedEditors.contains(event.getWhoClicked())) {
			return;
		}
		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}
		ItemStack item = event.getCursor();
		if (item != null && item.getType() == Material.POTION && event.getClickedInventory() == event.getView().getTopInventory()) {
			Brew brew = Brew.get(item);
			if (brew != null && !brew.isSealed()) {
				lang.sendEntry(event.getWhoClicked(), "Player_ShopSealBrew");
			}
		}
	}


	@EventHandler
	public void onCloseInventoryShopKeeper(InventoryCloseEvent event) {
		openedEditors.remove(event.getPlayer());
	}

	private void failed(Throwable e) {
		HandlerList.unregisterAll(this);
		Hook.SHOPKEEPERS.setEnabled(false);
		Logging.errorLog("Failed to notify Player using 'ShopKeepers'. Disabling 'ShopKeepers' support", e);
		openedEditors.clear();
	}

}
