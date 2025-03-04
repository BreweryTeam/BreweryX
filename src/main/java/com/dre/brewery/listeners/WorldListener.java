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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.Collection;

public class WorldListener implements Listener {

    private final DataManager dataManager;

    public WorldListener(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event) {
        Barrel.addBarrels(dataManager.getAllBarrels(event.getWorld()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldUnLoad(WorldLoadEvent event) {
        Collection<Barrel> barrels = Barrel.getBarrels(event.getWorld());
        barrels.forEach(dataManager::saveBarrel);
    }
}
