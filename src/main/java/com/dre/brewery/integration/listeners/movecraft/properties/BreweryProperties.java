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

import com.dre.brewery.BarrelAsset;
import com.dre.brewery.BarrelWoodType;
import com.dre.brewery.integration.listeners.movecraft.MovecraftUtil;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.craft.type.TypeData;
import net.countercraft.movecraft.craft.type.property.BooleanProperty;
import net.countercraft.movecraft.craft.type.property.ObjectPropertyImpl;
import net.countercraft.movecraft.craft.type.transform.MaterialSetTransform;
import net.countercraft.movecraft.util.Pair;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static net.countercraft.movecraft.craft.type.TypeData.NUMERIC_PREFIX;

public class BreweryProperties {
    public static final NamespacedKey ALL_BARRELS_ALLOWED = new NamespacedKey("brewery-x", "all_barrels_allowed");
    public static final NamespacedKey MAX_BARRELS = new NamespacedKey("brewery-x", "max_barrels");

    private static @NotNull Pair<Boolean, ? extends Number> parseLimit(@NotNull Object input) {
        if (!(input instanceof String str)) {
            return new Pair<>(false, (double) input);
        }

        if (!str.contains(NUMERIC_PREFIX)) {
            return new Pair<>(false, Double.valueOf(str));
        }

        String[] parts = str.split(NUMERIC_PREFIX);
        int val = Integer.parseInt(parts[1]);
        return new Pair<>(true, val);
    }

    public static void register() {
        CraftType.registerProperty(new BooleanProperty("allBarrelsAllowed", ALL_BARRELS_ALLOWED, (data) -> false));

        CraftType.registerProperty(new ObjectPropertyImpl("maxBarrels", MAX_BARRELS, (data, type, fileKey, namespacedKey) -> {
            Map<String, Object> map = data.getData(fileKey).getBackingData();
            if (map.isEmpty())
                throw new TypeData.InvalidValueException("Value for " + fileKey + " must not be an empty map");

            Set<MaxBarrelEntry> maxBarrels = new HashSet<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getKey() == null)
                    throw new TypeData.InvalidValueException("Keys for " + fileKey + " must be a string barrel type.");

                String name = entry.getKey().toUpperCase();
                try {
                    BarrelWoodType barrelType = BarrelWoodType.valueOf(name);
                    var limit = parseLimit(entry.getValue());
                    maxBarrels.add(new MaxBarrelEntry(barrelType, limit));
                } catch (IllegalArgumentException e) {
                    throw new TypeData.InvalidValueException("Type value of " + fileKey + " was an invalid barrel type.");
                }
            }
            return maxBarrels;
        }, (type -> new HashSet<>())));

        final Set<Material> signs = Tag.WALL_SIGNS.getValues();
        CraftType.registerTypeTransform((MaterialSetTransform) (data, type) -> {
            EnumSet<Material> set = data.get(CraftType.ALLOWED_BLOCKS);
            if (type.getBoolProperty(ALL_BARRELS_ALLOWED)) {
                Arrays.stream(BarrelWoodType.values())
                    .map(BarrelAsset::getMaterialsOf)
                    .forEach(set::addAll);
            }

            set.addAll(signs);
            return data;
        });

        CraftType.registerTypeTransform((MaterialSetTransform) (data, type) -> {
            EnumSet<Material> set = data.get(CraftType.ALLOWED_BLOCKS);
            MovecraftUtil.getBarrelsProperty(type).stream()
                .map(MaxBarrelEntry::type)
                .map(BarrelAsset::getMaterialsOf)
                .forEach(set::addAll);

            set.addAll(signs);
            return data;
        });
    }
}
