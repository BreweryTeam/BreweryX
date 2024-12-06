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

package com.dre.brewery;

import com.dre.brewery.utility.Logging;
import lombok.Getter;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;


@Getter
public enum BarrelWoodType {

    ANY("Any", 0, true),
    BIRCH("Birch", 1),
    OAK("Oak", 2),
    JUNGLE("Jungle", 3),
    SPRUCE("Spruce", 4),
    ACACIA("Acacia", 5),
    DARK_OAK("Dark Oak", 6),
    CRIMSON("Crimson", 7),
    WARPED("Warped", 8),
    MANGROVE("Mangrove", 9),
    CHERRY("Cherry", 10),
    BAMBOO("Bamboo", 11),
    CUT_COPPER("Cut Copper", 12, Material.CUT_COPPER, Material.CUT_COPPER_STAIRS, null, null),
    PALE_OAK("Pale Oak", 13),
    // If you're adding more wood types, add them above 'NONE'
    NONE("None", -1, true);


    private final String formattedName;
    private final int index;

	BarrelWoodType(String formattedName, int index) {
		this(formattedName, index, false);
	}

    BarrelWoodType(String formattedName, int index, boolean exclude) {
        this.formattedName = formattedName;
		this.index = index;
		if (!exclude) {
			BarrelAsset.addBarrelAsset(BarrelAsset.PLANKS, getStandardBarrelAssetMaterial(BarrelAsset.PLANKS));
			BarrelAsset.addBarrelAsset(BarrelAsset.STAIRS, getStandardBarrelAssetMaterial(BarrelAsset.STAIRS));
			BarrelAsset.addBarrelAsset(BarrelAsset.SIGN, getStandardBarrelAssetMaterial(BarrelAsset.SIGN));
			BarrelAsset.addBarrelAsset(BarrelAsset.FENCE, getStandardBarrelAssetMaterial(BarrelAsset.FENCE));
		}
    }


	BarrelWoodType(String formattedName, int index, Material planks, Material stairs, Material sign, Material fence) {
		this.formattedName = formattedName;
		this.index = index;

		BarrelAsset.addBarrelAsset(BarrelAsset.PLANKS, planks);
		BarrelAsset.addBarrelAsset(BarrelAsset.STAIRS, stairs);
		BarrelAsset.addBarrelAsset(BarrelAsset.SIGN, sign);
		BarrelAsset.addBarrelAsset(BarrelAsset.FENCE, fence);
	}

	@Nullable
	private Material[] getStandardBarrelAssetMaterial(BarrelAsset assetType) {
		try {
            // TODO: I dont like this... Change it later
            if (assetType == BarrelAsset.SIGN) {
                return new Material[]{Material.valueOf(this.name() + "_" + assetType.name()), Material.valueOf(this.name() + "_WALL_SIGN")};
            }
			return new Material[]{Material.valueOf(this.name() + "_" + assetType.name())};
		} catch (IllegalArgumentException e) {
			Logging.errorLog("Unable to find a standard asset for " + this.name() + " and " + assetType.name() + "!");
			Logging.errorLog("Developers should Manually specify the assets for the BarrelWoodType if they are not standard!");
			return null;
		}
	}


    public static BarrelWoodType fromName(String name) {
        for (BarrelWoodType type : values()) {
            if (type.name().equalsIgnoreCase(name) || type.formattedName.equalsIgnoreCase(name)) {
                return type;
            }
        }
        return ANY;
    }

    public static BarrelWoodType fromIndex(int index) {
        for (BarrelWoodType type : values()) {
            if (type.index == index) {
                return type;
            }
        }
        return ANY;
    }

    public static BarrelWoodType fromMaterial(Material material) {
        for (BarrelWoodType type : values()) {
            if (material.name().toUpperCase().startsWith(type.name().toUpperCase())) {
                return type;
            }
        }
        return ANY;
    }


    public static BarrelWoodType fromAny(Object intOrString) {
        if (intOrString instanceof Integer integer) {
            return fromIndex(integer);
        } else if (intOrString instanceof String s) {
            return fromName(s);
        } else if (intOrString instanceof Float || intOrString instanceof Double) {
            return fromIndex((int) (float) intOrString);
        } else if (intOrString instanceof Material m) {
            return fromMaterial(m);
        }
        return ANY;
    }
}
