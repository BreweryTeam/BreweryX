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

package com.dre.brewery;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.block.BlockFace;

@Getter
@AllArgsConstructor
public enum BarrelFacing {
    EAST(BlockFace.EAST, 1, 0), WEST(BlockFace.WEST, -1, 0), SOUTH(BlockFace.SOUTH, 0, 1), NORTH(BlockFace.NORTH, 0, -1);

    private final BlockFace face;
    private final int dx;
    private final int dz;

    /**
     * Rotate in against the positive y-axis, counterclockwise according to mathematical definition of rotations
     *
     * @return A rotated facing
     */
    public BarrelFacing rotate90degrees() {
        return switch (this) {
            case EAST -> NORTH;
            case WEST -> SOUTH;
            case NORTH -> WEST;
            case SOUTH -> EAST;
        };
    }

    /**
     * Rotate in against the positive y-axis, counterclockwise according to mathematical definition of rotations
     *
     * @return A rotated facing
     */
    public BarrelFacing rotate180degrees() {
        return switch (this) {
            case EAST -> WEST;
            case WEST -> EAST;
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
        };
    }

    /**
     * Rotate in against the positive y-axis, counterclockwise according to mathematical definition of rotations
     *
     * @return A rotated facing
     */
    public BarrelFacing rotate270degrees() {
        return switch (this) {
            case EAST -> SOUTH;
            case WEST -> NORTH;
            case NORTH -> EAST;
            case SOUTH -> WEST;
        };
    }
}
