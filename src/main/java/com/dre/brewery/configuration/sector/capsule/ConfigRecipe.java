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

package com.dre.brewery.configuration.sector.capsule;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.CustomKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
public class ConfigRecipe extends OkaeriConfig {

    // Added because our configs don't support external comments yet
    private Boolean enabled;

    private String name;
    private List<String> ingredients;

    @CustomKey("cookingtime")
    private Integer cookingTime;
    @CustomKey("distillruns")
    private Integer distillRuns;
    @CustomKey("distilltime")
    private Integer distillTime;
    private Object wood; // int or String(Enum<BarrelWoodType>) or List<int or String(Enum<BarrelWoodType>)>
    private Integer age;
    private String color;
    private Integer difficulty;
    private Integer alcohol;
    private Object lore; // List<String> or String
    @CustomKey("servercommands")
    private List<String> serverCommands;
    @CustomKey("playercommands")
    private List<String> playerCommands;
    @CustomKey("drinkmessage")
    private String drinkMessage;
    @CustomKey("drinktitle")
    private String drinkTitle;
    private Boolean glint;
    private String customModelData;
    private List<String> effects;
}
