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

package com.dre.brewery.configuration.configurer;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.configuration.ConfigHead;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.Logging;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Getter
public class TranslationManager {

    @Getter
    private static final Translation fallbackTranslation = Translation.EN;
    private static TranslationManager singleton;


    private Translation activeTranslation;
    private final File dataFolder;
    private final ConfigTranslations translations;
    private final ConfigTranslations fallbackTranslations;

    private TranslationManager(File dataFolder) {
        Yaml yaml = new Yaml();

        // Ok so,
        // Our config depends on our TranslationManager in order to load properly.
        // The problem is, the TranslationManager needs to be provided a translation in order to load,
        // in order to preserve the configurability of the language using the 'config.yml' instead of a separate file,
        // we have to grab the language from the config.yml before Okaeri loads it, so that's what we're doing right here.
        // Annoying race condition, but so be it.
        this.dataFolder = dataFolder;
        this.activeTranslation = Translation.EN;

        try (InputStream inputStream = Files.newInputStream(ConfigManager.getFilePath(Config.class))) {
            Map<String, String> data = yaml.loadAs(inputStream, Map.class);
            if (data != null) {
                this.activeTranslation = Translation.getTranslation(data.get("language"));
            }
        } catch (IOException e) {
            Logging.debugLog("Error reading YAML file: " + e.getMessage());
        }

        this.translations = new ConfigTranslations(activeTranslation, yaml);
        this.fallbackTranslations = new ConfigTranslations(fallbackTranslation, yaml);

        // Create lang files from /resources/languages
        for (Translation translation : Translation.getDefaultTranslations()) {
            createLanguageFile(translation);
        }
    }

    /**
     * Attempts to retrieve a translation for the given key with fallback logic.
     */
    @Nullable
    public String getTranslationWithFallback(String key) {
        String activeTranslationString = translations.getTranslation(key);
        if (activeTranslationString != null) {
            return activeTranslationString;
        }

        // Fallback to the english translation
        String fallbackTranslationString = fallbackTranslations.getTranslation(key);
        if (fallbackTranslationString == null) {
            Logging.warningLog("No translation found for key: " + key);
        }

        return fallbackTranslationString;
    }

    public void createLanguageFile(Translation translation) {
        ConfigManager.createFileFromResources("languages/" + translation.fileName(), dataFolder.toPath().resolve("languages").resolve(translation.fileName()));
    }

    // Okaeri would do this normally, but since default values in Lang changes based on language,
    // we have to manually go through each file.

    /**
     * Updates all translation files by adding all missing keys.
     * If a key hasn't been translated yet, english is used as a fallback.
     */
    public void updateTranslationFiles() {
        ConfigHead tempHead = new ConfigHead(); // Prevent polluting ConfigManager global state

        Lang fallback = loadFromResources(tempHead, Translation.EN);
        for (Translation trans : Translation.getDefaultTranslations()) {
            String langFilePathStr = "languages/" + trans.fileName();
            Path langFilePath = dataFolder.toPath().resolve(langFilePathStr);

            Lang langFromFile = tempHead.createConfig(Lang.class, langFilePath);
            Lang langFromResources = loadFromResources(tempHead, trans);

            langFromFile.updateMissingValuesFrom(langFromResources);
            if (trans != Translation.EN) {
                langFromFile.updateMissingValuesFrom(fallback);
            }
            langFromFile.save();
        }
    }

    // Loads a lang from resources... by loading from file then overwriting with resources InputStream
    // A bit of a hack, but avoids having to modify Okaeri
    @Nullable
    private Lang loadFromResources(ConfigHead tempHead, Translation translation) {
        String langFilePathStr = "languages/" + translation.fileName();
        Path langFilePath = dataFolder.toPath().resolve(langFilePathStr);

        Lang langFromResources = tempHead.createConfig(Lang.class, langFilePath);
        try (InputStream inputStream = BreweryPlugin.class.getClassLoader().getResourceAsStream(langFilePathStr)) {
            langFromResources.load(inputStream);
            return langFromResources;
        } catch (IOException e) {
            Logging.errorLog("Failed to load " + langFilePathStr + " from resources", e);
            return null;
        }
    }


    public static void newInstance(File dataFolder) {
        newInstance(dataFolder, false);
    }

    public static void newInstance(File dataFolder, boolean respectAlreadyExisting) {
        if (singleton != null && respectAlreadyExisting) {
            return;
        }
        singleton = new TranslationManager(dataFolder);
    }

    public static TranslationManager getInstance() {
        if (singleton == null) {
            singleton = new TranslationManager(BreweryPlugin.getInstance().getDataFolder());
        }
        return singleton;
    }
}
