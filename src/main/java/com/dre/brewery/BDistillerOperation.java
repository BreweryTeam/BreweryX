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

import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import org.bukkit.Material;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public enum BDistillerOperation {

    NOT_DISTILLABLE,
    DISTILLABLE_NO_OPERTION,
    DISTILLABLE,
    SPLASHABLE;

    private static final Config config = ConfigManager.getConfig(Config.class);

    public static BDistillerOperation isDistillable(@NotNull BrewerInventory brewer, Brew[] contents) {
        ItemStack item = brewer.getItem(3); // ingredient
        boolean glowStoneDust = (item != null && Material.GLOWSTONE_DUST == item.getType()); // need dust in the top slot.
        // TODO: Lingering potion support
        boolean splashingSerum = (item != null && (Material.GUNPOWDER == item.getType()));

        BDistillerOperation customFound = NOT_DISTILLABLE;
        if (contents == null) {
            return customFound;
        }
        for (Brew brew : contents) {
            if (brew == null) continue;

            if (!glowStoneDust && !splashingSerum) {
                return DISTILLABLE_NO_OPERTION;
            } else if (glowStoneDust && brew.canDistill()) {
                return DISTILLABLE;
            } else if (splashingSerum && !brew.isSplashPotion() && config.isEnableSplashableBrews()) {
                return SPLASHABLE;
            } else {
                customFound = DISTILLABLE_NO_OPERTION;
            }
        }
        return customFound;
    }
}
