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

package com.dre.brewery.configuration.files;

import com.dre.brewery.configuration.AbstractOkaeriConfigFile;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.annotation.Footer;
import com.dre.brewery.configuration.annotation.LocalizedComment;
import com.dre.brewery.configuration.annotation.OkaeriConfigFileOptions;
import com.dre.brewery.configuration.sector.RecipesSector;
import com.dre.brewery.configuration.sector.capsule.ConfigRecipe;
import com.dre.brewery.integration.PlaceholderAPIHook;
import eu.okaeri.configs.annotation.Header;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Header("!!! IMPORTANT: BreweryX configuration files do NOT support external comments! If you add any comments, they will be overwritten !!!")
@Footer({"",
	"More recipe ideas:",
	"Dandelion Liquor",
	"Beetroot Spirit",
	"Poppy Liquor: Macum/Grand Poppy",
	"Bamboo Liquor: Chu Yeh Ching",
	"Cachaca",
	"Cognac",
	"Sake",
	"Buorbon",
	"Moonshine",
	"Different Wines",
	"Brandy",
	"Amaretto",
	"etc. as well as variations like,",
	"Pumpkin Spice Beer",
	"Melon Vodka",
	"",
	"There are a lot of items in Minecraft like Vines, Milk and items added by plugins that would make great ingredients."
})
@OkaeriConfigFileOptions("recipes.yml")
@Setter
public class RecipesFile extends AbstractOkaeriConfigFile {


    @LocalizedComment("recipesFile.header")
    private Map<String, ConfigRecipe> recipes = new RecipesSector().getCapsules();

    // Backwards compatibility, merges recipes from the default config
    public Map<String, ConfigRecipe> getRecipes() {
        Map<String, ConfigRecipe> map = new HashMap<>(this.recipes);
        map.putAll(ConfigManager.getConfig(Config.class).getRecipes());
        return map;
    }
}
