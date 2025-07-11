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
import com.dre.brewery.integration.listeners.movecraft.properties.BreweryProperties;
import com.dre.brewery.integration.listeners.movecraft.properties.MaxBarrelEntry;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class MovecraftUtil {

    public static Set<Barrel> barrelsOnCraft(HitBox hitBox, World craftWorld) {
        Set<Barrel> output = new HashSet<>();

        for (Barrel barrel : Barrel.barrels) {
            Location location = barrel.getSpigot().getLocation();

            if (!Objects.equals(location.getWorld(), craftWorld)) {
                continue;
            }

            MovecraftLocation mvLocation = MathUtils.bukkit2MovecraftLoc(location);

            if (hitBox.contains(mvLocation)) {
                output.add(barrel);
            }
        }

        return output;
    }

    public static Set<MaxBarrelEntry> getBarrelsProperty(CraftType type) {
        try {
            Object objectProperty = type.getObjectProperty(BreweryProperties.MAX_BARRELS);
            if (objectProperty instanceof Set<?> property) {
                return (Set<MaxBarrelEntry>) property;
            } else {
                throw new IllegalStateException("maxBarrels must be a set.");
            }
        } catch (Exception exception) {
            return Set.of();
        }
    }
}
