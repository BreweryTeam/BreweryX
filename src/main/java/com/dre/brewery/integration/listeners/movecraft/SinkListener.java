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

package com.dre.brewery.integration.listeners.movecraft;

import com.dre.brewery.Barrel;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.events.CraftSinkEvent;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;

public class SinkListener implements Listener {
    @EventHandler
    public void onSink(CraftSinkEvent event) {
        HitBox hitBox = event.getCraft().getHitBox();
        ArrayList<Barrel> list = new ArrayList<>(Barrel.getBarrels(event.getCraft().getWorld().getUID()));

        for (Barrel barrel : list) {
            Location location = barrel.getSpigot().getLocation().clone();
            MovecraftLocation mvLocation = MathUtils.bukkit2MovecraftLoc(location);

            if (hitBox.contains(mvLocation)) {
                barrel.remove(null, null, true);
            }
        }
    }
}
