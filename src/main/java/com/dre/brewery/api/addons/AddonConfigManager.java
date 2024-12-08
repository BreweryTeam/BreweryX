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

package com.dre.brewery.api.addons;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.configuration.AbstractConfigManager;
import com.dre.brewery.configuration.annotation.OkaeriConfigFileOptions;
import eu.okaeri.configs.configurer.Configurer;
import eu.okaeri.configs.serdes.OkaeriSerdesPack;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * Management of addon configuration files.
 * @see AddonConfigFile
 * @see OkaeriConfigFileOptions
 */
public class AddonConfigManager {

    private final AbstractConfigManager INSTANCE;


    public AddonConfigManager(BreweryAddon addon) {
        this.INSTANCE = new AbstractConfigManager(BreweryPlugin.getInstance().getDataFolder().toPath().resolve("addons").resolve(addon.getAddonInfo().name()));
    }

    public AddonConfigManager(Path addonDataFolder) {
        this.INSTANCE = new AbstractConfigManager(addonDataFolder);
    }

    /**
     * Get a config instance from the LOADED_CONFIGS map, or create a new instance if it doesn't exist
     * @param configClass The class of the config to get
     * @return The config instance
     * @param <T> The type of the config
     */
    public <T extends AddonConfigFile> T getConfig(Class<T> configClass) {
        return INSTANCE.getConfig(configClass);
    }

    /**
     * Replaces a config instance in the LOADED_CONFIGS map with a new instance of the same class
     * @param configClass The class of the config to replace
     * @param <T> The type of the config
     */
    public <T extends AddonConfigFile> void newInstance(Class<T> configClass, boolean overwrite) {
        INSTANCE.newInstance(configClass, overwrite);
    }


    /**
     * Get the file path of a config class
     * @param configClass The class of the config to get the file name of
     * @return The file name
     * @param <T> The type of the config
     */
    public <T extends AddonConfigFile> Path getFilePath(Class<T> configClass) {
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
    private <T extends AddonConfigFile> T createConfig(Class<T> configClass, Path file, Configurer configurer, OkaeriSerdesPack serdesPack, boolean update, boolean removeOrphans) {
        return INSTANCE.createConfig(configClass, file, configurer, serdesPack, update, removeOrphans);
    }

    /**
     * Create a new config instance using a config class' annotation
     * @param configClass The class of the config to create
     * @return The new config instance
     * @param <T> The type of the config
     */
    private <T extends AddonConfigFile> T createConfig(Class<T> configClass) {
        return INSTANCE.createConfig(configClass);
    }

    @Nullable
    private <T extends AddonConfigFile> T createBlankConfigInstance(Class<T> configClass) {
        return INSTANCE.createBlankConfigInstance(configClass);
    }


    // Util

    public void createFileFromResources(String resourcesPath, Path destination) {
        INSTANCE.createFileFromResources(resourcesPath, destination);
    }


    private OkaeriConfigFileOptions getOkaeriConfigFileOptions(Class<? extends AddonConfigFile> configClass) {
        return INSTANCE.getOkaeriConfigFileOptions(configClass);
    }
    
}
