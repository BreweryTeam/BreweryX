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
import net.countercraft.movecraft.events.CraftTranslateEvent;
import net.countercraft.movecraft.util.MathUtils;
import net.countercraft.movecraft.util.hitboxes.HitBox;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class TranslationListener implements Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void translateListener(CraftTranslateEvent e) {
        Vector v = delta(e);
        if (v == null)
            return;

        HitBox hitBox = e.getCraft().getHitBox();
        for (Barrel barrel : Barrel.barrels) {
            Location location = barrel.getSpigot().getLocation().clone();
            MovecraftLocation mvLocation = MathUtils.bukkit2MovecraftLoc(location);

            if (hitBox.contains(mvLocation)) {
                BoundingBox box = barrel.getBounds();
                box.setMin(move(box.getMin(), v));
                box.setMax(move(box.getMax(), v));
                barrel.setSpigot( location.add(v.getX(), v.getY(), v.getZ()).getBlock() );
            }
        }
    }

    @Nullable
    private Vector delta(@NotNull CraftTranslateEvent e) {
        if (e.getOldHitBox().isEmpty() || e.getNewHitBox().isEmpty())
            return null;

        MovecraftLocation oldMid = e.getOldHitBox().getMidPoint();
        MovecraftLocation newMid = e.getNewHitBox().getMidPoint();

        int dx = newMid.getX() - oldMid.getX();
        int dy = newMid.getY() - oldMid.getY();
        int dz = newMid.getZ() - oldMid.getZ();

        return new Vector(dx, dy, dz);
    }

    @NotNull
    private BoundingBox.BlockPos move(@NotNull BoundingBox.BlockPos pos, @NotNull Vector vec) {
        return new BoundingBox.BlockPos(
            pos.getX() + vec.getBlockX(),
            pos.getY() + vec.getBlockY(),
            pos.getZ() + vec.getBlockZ()
        );
    }
}
