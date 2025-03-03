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

import org.bukkit.Material;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.jetbrains.annotations.NotNull;

public enum BarrelPart {
    BOTTOM_RIGHT(BarrelAsset.STAIRS, Bisected.Half.TOP, BarrelFacing.EAST),
    BOTTOM_LEFT(BarrelAsset.STAIRS, Bisected.Half.TOP, BarrelFacing.WEST),
    TOP_RIGHT(BarrelAsset.STAIRS, Bisected.Half.BOTTOM, BarrelFacing.EAST),
    TOP_LEFT(BarrelAsset.STAIRS, Bisected.Half.BOTTOM, BarrelFacing.WEST),
    BLOCK(BarrelAsset.PLANKS);

    final BarrelAsset barrelAsset;
    Bisected.Half half = null;
    BarrelFacing untransformedFacing = null;

    BarrelPart(BarrelAsset barrelAsset, Bisected.Half half, BarrelFacing untransformedFacing) {
        this.barrelAsset = barrelAsset;
        this.half = half;
        this.untransformedFacing = untransformedFacing;
    }

    BarrelPart(BarrelAsset barrelAsset) {
        this.barrelAsset = barrelAsset;
    }

    public boolean matches(BarrelWoodType type, @NotNull BlockData actual, BarrelFacing facing) {
        Material actualType = actual.getMaterial();
        if (!BarrelAsset.isBarrelAsset(barrelAsset, actualType) || BarrelWoodType.fromMaterial(actualType) != type) {
            return false;
        }
        if (half != null && (!(actual instanceof Bisected bisected) || bisected.getHalf() != half)) {
            return false;
        }
        if (untransformedFacing == null) {
            return true;
        }
        if (!(actual instanceof Directional directional)) {
            return false;
        }
        return switch (facing) {
            case SOUTH -> untransformedFacing.getFace().equals(directional.getFacing());
            case EAST -> untransformedFacing.rotate90degrees().getFace().equals(directional.getFacing());
            case NORTH -> untransformedFacing.rotate180degrees().getFace().equals(directional.getFacing());
            case WEST -> untransformedFacing.rotate270degrees().getFace().equals(directional.getFacing());
        };
    }
}
