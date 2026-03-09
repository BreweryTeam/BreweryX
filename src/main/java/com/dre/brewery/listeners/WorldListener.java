/*
 * BreweryX Bukkit-Plugin for an alternate brewing process
 * Copyright (C) 2024-2025 The Brewery Team
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

package com.dre.brewery.listeners;

import com.dre.brewery.Barrel;
import com.dre.brewery.storage.DataManager;
import com.dre.brewery.utility.Logging;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public record WorldListener(DataManager dataManager) implements Listener {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        Logging.debugLog("Loading barrels in world: " + event.getWorld().getUID());
        dataManager.getAllBarrels()
            .thenAcceptAsync(barrels -> barrels.stream()
                .filter(barrel -> barrel.getSpigot().getWorld().equals(event.getWorld()))
                .forEach(Barrel::registerBarrel)
            );
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldUnloadEvent event) {
        Logging.debugLog("Unloading barrels in world: " + event.getWorld().getUID());
        Barrel.onUnload(event.getWorld());
    }
}
