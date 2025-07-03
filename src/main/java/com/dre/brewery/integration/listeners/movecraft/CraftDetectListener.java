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
import com.dre.brewery.BarrelWoodType;
import com.dre.brewery.integration.listeners.movecraft.properties.MaxBarrelEntry;
import com.dre.brewery.integration.listeners.movecraft.properties.BreweryProperties;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.events.CraftDetectEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public class CraftDetectListener implements Listener {

    @EventHandler
    public void onCraftDetect(CraftDetectEvent event) {
        Craft craft = event.getCraft();
        CraftType type = craft.getType();

        boolean allAllowed = type.getBoolProperty(BreweryProperties.ALL_BARRELS_ALLOWED);
        if (allAllowed) {
            return;
        }

        Set<Barrel> barrels = MovecraftUtil.barrelsOnCraft(craft.getHitBox(), craft.getWorld());
        Set<?> maxBarrels = MovecraftUtil.getBarrelsProperty(type);
        if (maxBarrels.isEmpty() && !barrels.isEmpty()) {
            event.setCancelled(true);
            event.setFailMessage("Detection Failed! Barrels aren't allowed on this craft!");
            return;
        }

        Map<BarrelWoodType, Integer> barrelCount = new EnumMap<>(BarrelWoodType.class);
        for (var barrel : barrels) {
            BarrelWoodType woodType = barrel.getWood();
            barrelCount.compute(woodType, (key, value) -> (value == null) ? 1 : value + 1);
        }

        // Check designs against maxBarrels
        int size = craft.getOrigBlockCount();
        for (var entry : maxBarrels) {
            if (!(entry instanceof MaxBarrelEntry max))
                throw new IllegalStateException("maxBarrels must be a set of MaxBarrelEntry.");

            BarrelWoodType barrelType = max.type();
            var count = barrelCount.get(barrelType);
            if (count == null)
                continue;

            var result = max.detect(count, size);

            if (result != null) {
                event.setCancelled(true);
                event.setFailMessage("Detection Failed! You have too many barrels of the following type on this craft: "
                    + barrelType + ": " + result);
            }
        }
    }
}
