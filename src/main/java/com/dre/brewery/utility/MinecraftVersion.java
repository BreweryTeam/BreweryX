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

import org.bukkit.Bukkit;

/**
 * Enum for major Minecraft versions where Brewery needs
 * to handle things differently.
 */
public enum MinecraftVersion {

    //V1_7("1.7"), Remove 1.7 support. We only support versions that use UUIDs.
    V1_8("1.8"),
    V1_9("1.9"),
    V1_10("1.10"),
    V1_11("1.11"),
    V1_12("1.12"),
    V1_13("1.13"),
    V1_14("1.14"),
    V1_15("1.15"),
    V1_16("1.16"),
    V1_17("1.17"), // If we're being honest, probably no versions below this one will be used since we're compiling to Java 17. So they'll need to get some server software for Java 17 *if* they want to use BreweryX.
    V1_18("1.18"),
    V1_19("1.19"),
    V1_20("1.20"),
    V1_21("1.21"),
    UNKNOWN("Unknown");

    private final String version;

    MinecraftVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public static MinecraftVersion get(String version) {
        for (MinecraftVersion v : values()) {
            if (v.version.equals(version)) {
                return v;
            }
        }
        return UNKNOWN;
    }

    public static MinecraftVersion getIt() {
        String rawVersion = Bukkit.getVersion();
        String rawVersionParsed = rawVersion.substring(rawVersion.indexOf("(MC: ") + 5, rawVersion.indexOf(")"));

        // 1.20.5/6 is the same as 1.21 API
        if (rawVersionParsed.equals("1.20.5") || rawVersionParsed.equals("1.20.6")) {
            return V1_21;
        }

        String[] versionSplit = rawVersionParsed.split("\\.");
        return get(versionSplit[0] + "." + versionSplit[1]);
    }

    public boolean isOrLater(MinecraftVersion version) {
        return this.ordinal() >= version.ordinal();
    }

    public boolean isOrEarlier(MinecraftVersion version) {
        return this.ordinal() <= version.ordinal();
    }

}
