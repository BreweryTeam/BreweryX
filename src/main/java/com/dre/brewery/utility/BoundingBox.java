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

package com.dre.brewery.utility;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.List;

@Getter
@Setter
public class BoundingBox {

    public record BlockPos(int x, int y, int z) { }

    private BlockPos min, max;

    public BoundingBox(BlockPos a, BlockPos b) {
        this(a.x, a.y, a.z, b.x, b.y, b.z);
    }

    public BoundingBox(int x1, int y1, int z1, int x2, int y2, int z2) {
        int minX, minY, minZ, maxX, maxY, maxZ;
        minX = Math.min(x1, x2);
        minY = Math.min(y1, y2);
        minZ = Math.min(z1, z2);
        min = new BlockPos(minX, minY, minZ);
        maxX = Math.max(x2, x1);
        maxY = Math.max(y2, y1);
        maxZ = Math.max(z2, z1);
        max = new BlockPos(maxX, maxY, maxZ);
    }

    public boolean contains(int x, int y, int z) {
        return (x >= min.x && x <= max.x) && (y >= min.y && y <= max.y) && (z >= min.z && z <= max.z);
    }

    public boolean contains(Location loc) {
        return contains(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public boolean contains(Block block) {
        return contains(block.getX(), block.getY(), block.getZ());
    }

    public long area() {
        return ((long) (max.z - min.z + 1)) * ((long) (max.y - min.y + 1)) * ((long) (max.z - min.z + 1));
    }

    // Quick check if the bounds are valid or seem corrupt
    public boolean isBad() {
        long area = this.area();
        return area > 64 || area < 4;
    }

    public void resize(int x1, int y1, int z1, int x2, int y2, int z2) {
        BoundingBox box = new BoundingBox(x1, y1, z1, x2, y2, z2);
        this.min = box.min;
        this.max = box.max;
    }

    public String serialize() {
        return min.x + "," + min.y + "," + min.z + "," + max.x + "," + max.y + "," + max.z;
    }

    public List<Integer> serializeToIntList() {
        return List.of(min.x, min.y, min.z, max.x, max.y, max.z);
    }

    public static BoundingBox fromPoints(int[] locations) {
        if (locations.length % 3 != 0) throw new IllegalArgumentException("Locations has to be pairs of three");

        int length = locations.length - 2;

        int minx = Integer.MAX_VALUE,
            miny = Integer.MAX_VALUE,
            minz = Integer.MAX_VALUE,
            maxx = Integer.MIN_VALUE,
            maxy = Integer.MIN_VALUE,
            maxz = Integer.MIN_VALUE;
        for (int i = 0; i < length; i += 3) {
            minx = Math.min(locations[i], minx);
            miny = Math.min(locations[i + 1], miny);
            minz = Math.min(locations[i + 2], minz);
            maxx = Math.max(locations[i], maxx);
            maxy = Math.max(locations[i + 1], maxy);
            maxz = Math.max(locations[i + 2], maxz);
        }
        return new BoundingBox(minx, miny, minz, maxx, maxy, maxz);
    }

    public static BoundingBox fromPoints(List<Integer> locations) {
        if (locations.size() % 3 != 0) throw new IllegalArgumentException("Locations has to be pairs of three");

        int length = locations.size() - 2;

        int minx = Integer.MAX_VALUE,
            miny = Integer.MAX_VALUE,
            minz = Integer.MAX_VALUE,
            maxx = Integer.MIN_VALUE,
            maxy = Integer.MIN_VALUE,
            maxz = Integer.MIN_VALUE;
        for (int i = 0; i < length; i += 3) {
            minx = Math.min(locations.get(i), minx);
            miny = Math.min(locations.get(i + 1), miny);
            minz = Math.min(locations.get(i + 2), minz);
            maxx = Math.max(locations.get(i), maxx);
            maxy = Math.max(locations.get(i + 1), maxy);
            maxz = Math.max(locations.get(i + 2), maxz);
        }
        return new BoundingBox(minx, miny, minz, maxx, maxy, maxz);
    }
}
