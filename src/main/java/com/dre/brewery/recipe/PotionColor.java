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

package com.dre.brewery.recipe;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.utility.MinecraftVersion;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

@Getter
public class PotionColor {

    private static final String HEX_STRING = "#%02x%02x%02x";
    private static final MinecraftVersion VERSION = BreweryPlugin.getMCVersion();

    public static final PotionColor PINK = new PotionColor(1, PotionType.REGENERATION, Color.FUCHSIA);
    public static final PotionColor CYAN = new PotionColor(2, PotionType.SWIFTNESS, Color.AQUA);
    public static final PotionColor ORANGE = new PotionColor(3, PotionType.FIRE_RESISTANCE, Color.ORANGE);
    public static final PotionColor GREEN = new PotionColor(4, PotionType.POISON, Color.GREEN);
    public static final PotionColor BRIGHT_RED = new PotionColor(5, PotionType.HEALING, Color.fromRGB(255, 0, 0));
    public static final PotionColor BLUE = new PotionColor(6, PotionType.NIGHT_VISION, Color.NAVY);
    public static final PotionColor BLACK = new PotionColor(8, PotionType.WEAKNESS, Color.BLACK);
    public static final PotionColor RED = new PotionColor(9, PotionType.STRENGTH, Color.fromRGB(196, 0, 0));
    public static final PotionColor GREY = new PotionColor(10, PotionType.SLOWNESS, Color.GRAY);
    public static final PotionColor WATER = new PotionColor(11, VERSION.isOrLater(MinecraftVersion.V1_9) ? PotionType.WATER_BREATHING : null, Color.BLUE);
    public static final PotionColor DARK_RED = new PotionColor(12, PotionType.HARMING, Color.fromRGB(128, 0, 0));
    public static final PotionColor BRIGHT_GREY = new PotionColor(14, PotionType.INVISIBILITY, Color.SILVER);
    public static final PotionColor WHITE = new PotionColor(Color.WHITE);
    public static final PotionColor LIME = new PotionColor(Color.LIME);
    public static final PotionColor OLIVE = new PotionColor(Color.OLIVE);
    public static final PotionColor PURPLE = new PotionColor(Color.PURPLE);
    public static final PotionColor TEAL = new PotionColor(Color.TEAL);
    public static final PotionColor YELLOW = new PotionColor(Color.YELLOW);

    private final int colorId;
    private final PotionType type;
    private final Color color;

    PotionColor(int colorId, PotionType type, Color color) {
        this.colorId = colorId;
        this.type = type;
        this.color = color;
    }

    public PotionColor(Color color) {
        colorId = WATER.colorId;
        type = WATER.getType();
        this.color = color;
    }

    // gets the Damage Value, that sets a color on the potion
    // offset +32 is not accepted by brewer, so not further destillable
    // Only for minecraft pre 1.9
    public short getColorId(boolean destillable) {
        if (destillable) {
            return (short) (colorId + 64);
        }
        return (short) (colorId + 32);
    }

    @SuppressWarnings("deprecation")
    public void colorBrew(PotionMeta meta, ItemStack potion, boolean destillable) {
        if (VERSION.isOrLater(MinecraftVersion.V1_9)) {
            // We need to Hide Potion Effects even in 1.12, as it would otherwise show "No Effects"
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            if (VERSION.isOrLater(MinecraftVersion.V1_11)) {
                // BasePotionData was only used for the Color, so starting with 1.12 we can use setColor instead
                meta.setColor(getColor());
            } else {
                meta.setBasePotionType(getType());
            }
        } else {
            potion.setDurability(getColorId(destillable));
            // To stop 1.8 from showing the potioneffect for the color id, add a dummy Effect
            meta.addCustomEffect(PotionEffectType.REGENERATION.createEffect(0, 0), true);
        }
    }

    public static PotionColor fromString(String string) {
        return switch (string.toUpperCase()) {
            case "PINK" -> PINK;
            case "CYAN" -> CYAN;
            case "ORANGE" -> ORANGE;
            case "GREEN" -> GREEN;
            case "BRIGHT_RED" -> BRIGHT_RED;
            case "BLUE" -> BLUE;
            case "BLACK" -> BLACK;
            case "RED" -> RED;
            case "GREY" -> GREY;
            case "WATER" -> WATER;
            case "DARK_RED" -> DARK_RED;
            case "BRIGHT_GREY" -> BRIGHT_GREY;
            case "WHITE" -> WHITE;
            case "LIME" -> LIME;
            case "OLIVE" -> OLIVE;
            case "PURPLE" -> PURPLE;
            case "TEAL" -> TEAL;
            case "YELLOW" -> YELLOW;
            default -> {
                try {
                    if (string.length() >= 7) {
                        string = string.substring(1);
                    }
                    yield new PotionColor(Color.fromRGB(
                        Integer.parseInt(string.substring(0, 2), 16),
                        Integer.parseInt(string.substring(2, 4), 16),
                        Integer.parseInt(string.substring(4, 6), 16)
                    ));
                } catch (NumberFormatException e) {
                    yield WATER;
                }
            }

        };
    }

    public static PotionColor fromColor(Color color) {
        return new PotionColor(color);
    }

    @Override
    public String toString() {
        return String.format(HEX_STRING, color.getRed(), color.getGreen(), color.getBlue());
    }
}
