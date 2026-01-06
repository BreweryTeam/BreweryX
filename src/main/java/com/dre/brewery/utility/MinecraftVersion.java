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

import com.google.common.base.Preconditions;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

/**
 * Enum for major Minecraft versions where Brewery needs
 * to handle things differently.
 */
@Getter
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
    // If we're being honest, probably no versions below this one will be used since we're compiling to Java 17.
    //  So they'll need to get some server software for Java 17 *if* they want to use BreweryX.
    V1_17("1.17"),
    V1_18("1.18"),
    V1_19("1.19"),
    V1_20("1.20"),
    V1_20_4("1.20.4", "1.20.3"), // 1.20.4 & 1.20.3 are one and the same
    V1_21("1.21", "1.20.5", "1.20.6"), // 1.20.5, 1.20.6, & 1.21 are being used the same way in BreweryX.
    V1_21_5("1.21.5"), // Int CustomModelData is deprecated since version 1.21.5
    V1_21_10("1.21.10", "1.21.9"), // 1.21.10 & 1.21.9 are one and the same
    V1_21_11("1.21.11"),
    UNKNOWN("Unknown");


    private @Getter static final boolean isFolia = MinecraftVersion.checkFolia();
    private @Getter static final boolean useNBT = NBTUtil.initNbt();

    private final String[] versions;

    MinecraftVersion(String... version) {
        this.versions = version;
    }

    public static MinecraftVersion get(String major, String minor, @Nullable String patch) {
        String withPatch = major + "." + minor + "." + patch;
        String withoutPatch = major + "." + minor;

        // We do two passes to prefer exact matches with a patch version.
        for (MinecraftVersion minecraftVersion : values()) {
            for (String versionString : minecraftVersion.versions) {
                if (versionString.equals(withPatch)) {
                    return minecraftVersion;
                }
            }
        }

        for (MinecraftVersion minecraftVersion : values()) {
            for (String versionString : minecraftVersion.versions) {
                if (versionString.equals(withoutPatch)) {
                    return minecraftVersion;
                }
            }
        }
        return UNKNOWN;
    }

    public static MinecraftVersion getIt() {
        String rawVersion = Bukkit.getVersion();
        String rawVersionParsed = rawVersion.substring(rawVersion.indexOf("(MC: ") + 5, rawVersion.indexOf(")"));
        String[] versionSplit = rawVersionParsed.split("\\.");

        Preconditions.checkState(versionSplit.length == 3 || versionSplit.length == 2, "Unexpected Minecraft version format: " + rawVersionParsed);


        return get(versionSplit[0], versionSplit[1], versionSplit[2]);
    }

    public boolean isOrLater(MinecraftVersion version) {
        return this.ordinal() >= version.ordinal();
    }

    public boolean isOrEarlier(MinecraftVersion version) {
        return this.ordinal() <= version.ordinal();
    }

    public String getVersion() {
        return versions[0];
    }

    private static boolean checkFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
