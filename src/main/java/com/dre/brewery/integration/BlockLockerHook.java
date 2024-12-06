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

package com.dre.brewery.integration;

import com.dre.brewery.integration.barrel.BlockLockerBarrel;
import com.dre.brewery.utility.Logging;

public class BlockLockerHook extends Hook {

    public static final BlockLockerHook BLOCKLOCKER = new BlockLockerHook("BlockLocker", config.isUseBlockLocker());

    public BlockLockerHook(String name, boolean enabled) {
        super(name, enabled);

        if (!isEnabled()) {
            return;
        }

        try {
            Class.forName("nl.rutgerkok.blocklocker.BlockLockerAPIv2");
            Class.forName("nl.rutgerkok.blocklocker.ProtectableBlocksSettings");
            BlockLockerBarrel.registerBarrelAsProtectable();
        } catch (ClassNotFoundException e) {
            this.enabled = false;
            Logging.log("Unsupported Version of 'BlockLocker', locking Brewery Barrels with LogBlock disabled.");
        }
    }
}
