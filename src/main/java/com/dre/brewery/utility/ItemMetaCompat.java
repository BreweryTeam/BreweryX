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

import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Compatibility wrapper around CustomModelData access for different 1.21.x APIs.
 */
public final class ItemMetaCompat {

    private static final Method HAS_COMPONENT_METHOD = findMethod(ItemMeta.class, "hasCustomModelDataComponent");
    private static final Method GET_COMPONENT_METHOD = findMethod(ItemMeta.class, "getCustomModelDataComponent");
    private static final Method HAS_LEGACY_METHOD = findMethod(ItemMeta.class, "hasCustomModelData");
    private static final Method GET_LEGACY_METHOD = findMethod(ItemMeta.class, "getCustomModelData");

    private ItemMetaCompat() {
    }

    public static boolean hasCustomModelData(@NotNull ItemMeta meta) {
        if (HAS_COMPONENT_METHOD != null && GET_COMPONENT_METHOD != null) {
            return getCustomModelData(meta) != null;
        }
        if (HAS_LEGACY_METHOD != null) {
            try {
                return (boolean) HAS_LEGACY_METHOD.invoke(meta);
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                return false;
            }
        }
        return false;
    }

    public static Integer getCustomModelData(@NotNull ItemMeta meta) {
        if (HAS_COMPONENT_METHOD != null && GET_COMPONENT_METHOD != null) {
            Integer componentValue = readComponentValue(meta);
            if (componentValue != null) {
                return componentValue;
            }
        }
        if (HAS_LEGACY_METHOD != null && GET_LEGACY_METHOD != null) {
            try {
                if ((boolean) HAS_LEGACY_METHOD.invoke(meta)) {
                    return (Integer) GET_LEGACY_METHOD.invoke(meta);
                }
            } catch (IllegalAccessException | InvocationTargetException ignored) {
                return null;
            }
        }
        return null;
    }

    private static Integer readComponentValue(@NotNull ItemMeta meta) {
        try {
            Object component = GET_COMPONENT_METHOD.invoke(meta);
            if (component == null) {
                return null;
            }

            Method getFloats = findMethod(component.getClass(), "getFloats");
            if (getFloats == null) {
                return null;
            }

            Object rawFloats = getFloats.invoke(component);
            if (!(rawFloats instanceof List<?> floats) || floats.isEmpty()) {
                return null;
            }

            Object first = floats.get(0);
            if (first instanceof Number number) {
                return number.intValue();
            }
        } catch (IllegalAccessException | InvocationTargetException ignored) {
            return null;
        }
        return null;
    }

    private static Method findMethod(Class<?> type, String name) {
        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }
}
