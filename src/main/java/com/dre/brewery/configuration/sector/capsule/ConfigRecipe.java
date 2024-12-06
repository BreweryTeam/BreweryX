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

    private String name;
    private List<String> ingredients;

    @CustomKey("cookingtime")
    private int cookingTime;
    @CustomKey("distillruns")
    private int distillRuns;
    @CustomKey("distilltime")
    private int distillTime;
    private Object wood; // int or String(Enum<BarrelWoodType>)
    private int age;
    private String color;
    private int difficulty;
    private int alcohol;
    private Object lore; // List<String> or String
    @CustomKey("servercommands")
    private List<String> serverCommands;
    @CustomKey("playercommands")
    private List<String> playerCommands;
    @CustomKey("drinkmessage")
    private String drinkMessage;
    @CustomKey("drinktitle")
    private String drinkTitle;
    private boolean glint;
    private String customModelData;
    private List<String> effects;
}
