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

package com.dre.brewery.integration.listeners.movecraft.properties;

import com.dre.brewery.BarrelWoodType;
import net.countercraft.movecraft.util.Pair;
import org.jetbrains.annotations.NotNull;

public record MaxBarrelEntry(BarrelWoodType type, double max, boolean numericMax) {
    public MaxBarrelEntry(BarrelWoodType name, @NotNull Pair<Boolean, ? extends Number> max) {
        this(name, max.getRight().doubleValue(), max.getLeft());
    }

    public boolean check(int count, int size) {
        if (numericMax) {
            return count <= max;
        }

        double percent = 100D * count / size;
        return percent <= max;
    }

    /**
     * @return Empty if no error, otherwise return the error
     */
    public String detect(int count, int size) {
        if (numericMax) {
            if (count > max)
                return String.format("%d > %d", count, (int) max);
        } else {
            double blockPercent = 100D * count / size;
            if (blockPercent > max)
                return String.format("%.2f%% > %.2f%%", blockPercent, max);
        }

        return null;
    }
}
