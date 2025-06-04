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
import com.dre.brewery.utility.BoundingBox;
import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.MovecraftRotation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftRotateEvent;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class RotationListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void craftRotate(CraftRotateEvent event) {
        Craft craft = event.getCraft();
        MovecraftRotation rotation = event.getRotation();
        MovecraftLocation originPoint = event.getOriginPoint();

        HitBox hitBox = craft.getHitBox();
        for (Barrel barrel : Barrel.barrels) {
            Location location = barrel.getSpigot().getLocation().clone();
            MovecraftLocation mvLocation = MathUtils.bukkit2MovecraftLoc(location);

            if (hitBox.contains(mvLocation)) {
                rotate(barrel, rotation, originPoint);
            }
        }
    }

    public void rotate(Barrel barrel, MovecraftRotation rotation, MovecraftLocation origin) {
        BoundingBox box = barrel.getBounds();
        World world = barrel.getSpigot().getWorld();

        Location spigot = barrel.getSpigot().getLocation();
        MovecraftLocation mvSpigot = MathUtils.bukkit2MovecraftLoc(spigot);
        MovecraftLocation rtSpigot = rotateCentered(rotation, mvSpigot, origin);
        barrel.setSpigot( rtSpigot.toBukkit(world).getBlock() );

        BoundingBox.BlockPos min = box.getMin();
        MovecraftLocation mvMin = new MovecraftLocation(min.getX(), min.getY(), min.getZ());
        MovecraftLocation rtMin = rotateCentered(rotation, mvMin, origin);
        BoundingBox.BlockPos bpMin = adapt(rtMin);

        BoundingBox.BlockPos max = box.getMax();
        MovecraftLocation mvMax = new MovecraftLocation(max.getX(), max.getY(), max.getZ());
        MovecraftLocation rtMax = rotateCentered(rotation, mvMax, origin);
        BoundingBox.BlockPos bpMax = adapt(rtMax);

        BoundingBox rotatedBox = new BoundingBox(bpMin, bpMax);
        box.setMin(rotatedBox.getMin());
        box.setMax(rotatedBox.getMax());
    }

    public MovecraftLocation rotateCentered(MovecraftRotation rotation, MovecraftLocation subject, MovecraftLocation origin) {
        return MathUtils.rotateVec(rotation, subject.subtract(origin)).add(origin);
    }

    public BoundingBox.BlockPos adapt(MovecraftLocation ml) {
        return new BoundingBox.BlockPos(
            ml.getX(),
            ml.getY(),
            ml.getZ()
        );
    }
}
