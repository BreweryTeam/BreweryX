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

package com.dre.brewery.configuration;

import com.dre.brewery.DistortChat;
import com.dre.brewery.configuration.annotation.OkaeriConfigFileOptions;
import com.dre.brewery.configuration.files.CauldronFile;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.configuration.files.RecipesFile;
import com.dre.brewery.configuration.sector.capsule.ConfigDistortWord;
import com.dre.brewery.integration.item.BreweryPluginItem;
import com.dre.brewery.integration.item.ItemsAdderPluginItem;
import com.dre.brewery.integration.item.MMOItemsPluginItem;
import com.dre.brewery.integration.item.OraxenPluginItem;
import com.dre.brewery.integration.item.SlimefunPluginItem;
import com.dre.brewery.recipe.BCauldronRecipe;
import com.dre.brewery.recipe.BRecipe;
import com.dre.brewery.recipe.PluginItem;
import com.dre.brewery.utility.Logging;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private static final AbstractConfigManager INSTANCE = new AbstractConfigManager();

    public static final Map<Class<? extends AbstractOkaeriConfigFile>, AbstractOkaeriConfigFile> LOADED_CONFIGS = INSTANCE.LOADED_CONFIGS;

    /**
     * Get a config instance from the LOADED_CONFIGS map, or create a new instance if it doesn't exist
     * @param configClass The class of the config to get
     * @return The config instance
     * @param <T> The type of the config
     */
    public static <T extends AbstractOkaeriConfigFile> T getConfig(Class<T> configClass) {
        return INSTANCE.getConfig(configClass);
    }

    /**
     * Replaces a config instance in the LOADED_CONFIGS map with a new instance of the same class
     * @param configClass The class of the config to replace
     * @param <T> The type of the config
     */
    public static <T extends AbstractOkaeriConfigFile> void newInstance(Class<T> configClass, boolean overwrite) {
        INSTANCE.newInstance(configClass, overwrite);
    }


    /**
     * Get the file path of a config class
     * @param configClass The class of the config to get the file name of
     * @return The file name
     * @param <T> The type of the config
     */
    public static <T extends AbstractOkaeriConfigFile> Path getFilePath(Class<T> configClass) {
        return INSTANCE.getFilePath(configClass);
    }




    /**
     * Create a new config instance with a custom file name, configurer, serdes pack, and puts it in the LOADED_CONFIGS map
     * @param configClass The class of the config to create
     * @param file The file to use
     * @param configurer The configurer to use
     * @param serdesPack The serdes pack to use
     * @return The new config instance
     * @param <T> The type of the config
     */
    private static <T extends AbstractOkaeriConfigFile> T createConfig(Class<T> configClass, Path file, Configurer configurer, OkaeriSerdesPack serdesPack, boolean update, boolean removeOrphans) {
        return INSTANCE.createConfig(configClass, file, configurer, serdesPack, update, removeOrphans);
    }

    /**
     * Create a new config instance using a config class' annotation
     * @param configClass The class of the config to create
     * @return The new config instance
     * @param <T> The type of the config
     */
    private static <T extends AbstractOkaeriConfigFile> T createConfig(Class<T> configClass) {
        return INSTANCE.createConfig(configClass);
    }

	@Nullable
	private static <T extends AbstractOkaeriConfigFile> T createBlankConfigInstance(Class<T> configClass) {
        return INSTANCE.createBlankConfigInstance(configClass);
	}


    // Util

    public static void createFileFromResources(String resourcesPath, Path destination) {
        INSTANCE.createFileFromResources(resourcesPath, destination);
    }


    private static OkaeriConfigFileOptions getOkaeriConfigFileOptions(Class<? extends AbstractOkaeriConfigFile> configClass) {
        return INSTANCE.getOkaeriConfigFileOptions(configClass);
    }


    // Not really what I want to do, but I have to move these from BConfig right now

    public static void loadRecipes() {
        // loading recipes
        List<BRecipe> configRecipes = BRecipe.getConfigRecipes();
        configRecipes.clear();

        for (var recipeEntry : getConfig(RecipesFile.class).getRecipes().entrySet()) {
            BRecipe recipe = BRecipe.fromConfig(recipeEntry.getKey(), recipeEntry.getValue());
            if (recipe != null && recipe.isValid()) {
                configRecipes.add(recipe);
            } else {
                Logging.errorLog("Loading the Recipe with id: '" + recipeEntry.getKey() + "' failed!");
            }

            BRecipe.setNumConfigRecipes(configRecipes.size());
        }
    }


    public static void loadCauldronIngredients() {
        // Loading Cauldron Recipes

        List<BCauldronRecipe> configRecipes = BCauldronRecipe.getConfigRecipes();
        configRecipes.clear();

        for (var cauldronEntry : getConfig(CauldronFile.class).getCauldronIngredients().entrySet()) {
            BCauldronRecipe recipe = BCauldronRecipe.fromConfig(cauldronEntry.getKey(), cauldronEntry.getValue());
            if (recipe != null) {
                configRecipes.add(recipe);
            } else {
                Logging.errorLog("Loading the Cauldron-Recipe with id: '" + cauldronEntry.getKey() + "' failed!");
            }
        }
        BCauldronRecipe.setNumConfigRecipes(configRecipes.size());

        // Recalculating Cauldron-Accepted Items for non-config recipes
        for (BRecipe recipe : BRecipe.getAddedRecipes()) {
            recipe.updateAcceptedLists();
        }
        for (BCauldronRecipe recipe : BCauldronRecipe.getAddedRecipes()) {
            recipe.updateAcceptedLists();
        }
    }

    public static void loadDistortWords() {
        // Loading Words
        Config config = getConfig(Config.class);

        if (config.isEnableChatDistortion()) {
            for (ConfigDistortWord distortWord : config.getWords()) {
                new DistortChat(distortWord);
            }
            for (String bypass : config.getDistortBypass()) {
                DistortChat.getIgnoreText().add(bypass.split(","));
            }
            DistortChat.getCommands().addAll(config.getDistortCommands());
        }
    }

    public static void registerDefaultPluginItems() {
        PluginItem.registerForConfig("brewery", BreweryPluginItem::new);
        PluginItem.registerForConfig("mmoitems", MMOItemsPluginItem::new);
        PluginItem.registerForConfig("slimefun", SlimefunPluginItem::new);
        PluginItem.registerForConfig("exoticgarden", SlimefunPluginItem::new);
        PluginItem.registerForConfig("oraxen", OraxenPluginItem::new);
        PluginItem.registerForConfig("itemsadder", ItemsAdderPluginItem::new);
    }
}
