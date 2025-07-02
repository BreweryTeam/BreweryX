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

package com.dre.brewery.integration.barrel;

import com.dre.brewery.Barrel;
import com.dre.brewery.BarrelAsset;
import com.dre.brewery.BarrelBody;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.api.events.barrel.BarrelAccessEvent;
import com.dre.brewery.integration.BlockLockerHook;
import com.dre.brewery.utility.Logging;
import nl.rutgerkok.blocklocker.BlockLockerAPIv2;
import nl.rutgerkok.blocklocker.ProtectableBlocksSettings;
import nl.rutgerkok.blocklocker.ProtectionType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.List;

public class BlockLockerBarrel implements ProtectableBlocksSettings {
    private static Block lastBarrelSign;

    @Override
    public boolean canProtect(Block block) {
        return isOrWillCreateBarrel(block);
    }

    @Override
    public boolean canProtect(ProtectionType protectionType, Block block) {
        if (protectionType != ProtectionType.CONTAINER) return false;

        return isOrWillCreateBarrel(block);
    }

    public boolean isOrWillCreateBarrel(Block block) {
        if (!BreweryPlugin.getInstance().isEnabled() || !BlockLockerHook.BLOCKLOCKER.isEnabled()) {
            return false;
        }
        if (!BarrelAsset.isBarrelAsset(BarrelAsset.PLANKS, block.getType()) && !BarrelAsset.isBarrelAsset(BarrelAsset.STAIRS, block.getType())) {
            // Can only be a barrel if it's a planks block
            return false;
        }
        if (Barrel.getByWood(block) != null) {
            // Barrel already exists
            return true;
        }
        if (lastBarrelSign == null) {
            // No player wants to create a Barrel
            return false;
        }
        for (BlockFace face : new BlockFace[]{ BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST }) {
            Block sign = block.getRelative(face);
            if (lastBarrelSign.equals(sign)) {
                Block spigot = BarrelBody.getSpigotOfSign(sign);
                byte signoffset = 0;
                if (!spigot.equals(sign)) {
                    signoffset = (byte) (sign.getY() - spigot.getY());
                }
                Barrel barrel = new Barrel(spigot, signoffset, true); // Barrel is temporary, size does not matter

                return barrel.getBrokenBlock(true) == null;
            }
        }
        return false;
    }

    public static boolean checkAccess(BarrelAccessEvent event) {
        Block sign = event.getBarrel().getSignOfSpigot();
        if (!BarrelAsset.isBarrelAsset(BarrelAsset.SIGN, sign.getType())) {
            return true;
        }
        return BlockLockerAPIv2.isAllowed(event.getPlayer(), sign, true);
    }

    public static void createdBarrelSign(Block sign) {
        // The Player created a sign with "Barrel" on it, he want's to create a barrel
        lastBarrelSign = sign;
    }

    public static void clearBarrelSign() {
        lastBarrelSign = null;
    }

    public static void registerBarrelAsProtectable() {
        try {
            List<ProtectableBlocksSettings> extraProtectables = BlockLockerAPIv2.getPlugin().getChestSettings().getExtraProtectables();
            if (extraProtectables.stream().noneMatch(blockSettings -> blockSettings instanceof BlockLockerBarrel)) {
                extraProtectables.add(new BlockLockerBarrel());
            }
        } catch (Exception e) {
            Logging.errorLog("Failed to register Barrel as protectable block", e);
        }
    }
}
