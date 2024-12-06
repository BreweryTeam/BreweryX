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

import com.dre.brewery.integration.item.SlimefunPluginItem;
import com.dre.brewery.listeners.PlayerListener;
import com.dre.brewery.recipe.BCauldronRecipe;
import com.dre.brewery.recipe.RecipeItem;
import com.dre.brewery.utility.MaterialUtil;
import com.dre.brewery.utility.Logging;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Optional;

public class SlimefunListener implements Listener {

	/**
	 * Catch the Slimefun Right Click event, to cancel it if the right click was on a Cauldron.
	 * This prevents item consumption while adding to the cauldron
	 */
	@EventHandler
	public void onCauldronClickSlimefun(PlayerRightClickEvent event) {
		try {
			if (event.getClickedBlock().isPresent() && event.getHand() == EquipmentSlot.HAND) {
				if (MaterialUtil.isWaterCauldron(event.getClickedBlock().get().getType())) {
					Optional<SlimefunItem> slimefunItem = event.getSlimefunItem();
					if (slimefunItem.isPresent()) {
						for (RecipeItem rItem : BCauldronRecipe.acceptedCustom) {
							if (rItem instanceof SlimefunPluginItem) {
								if (slimefunItem.get().getId().equalsIgnoreCase(((SlimefunPluginItem) rItem).getItemId())) {
									event.cancel();
									PlayerListener.handlePlayerInteract(event.getInteractEvent());
									return;
								}
							}
						}
					}
				}
			}
		} catch (Throwable e) {
			HandlerList.unregisterAll(this);
			Logging.errorLog("Slimefun check failed!", e);
		}
	}
}
