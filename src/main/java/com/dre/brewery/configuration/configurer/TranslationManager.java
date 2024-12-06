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
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.Logging;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
				Translation trans = BUtil.getEnumByName(Translation.class, data.get("language"));

				if (trans != null) {
					this.activeTranslation = trans;
				} else {
					Logging.warningLog("Invalid language in config.yml: &6" + data.get("language"));
				}
			}
		} catch (IOException e) {
			Logging.debugLog("Error reading YAML file: " + e.getMessage());
		}

		this.translations = new ConfigTranslations(activeTranslation, yaml);
		this.fallbackTranslations = new ConfigTranslations(fallbackTranslation, yaml);

		// Create lang files from /resources/languages
		for (Translation translation : Translation.values()) {
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
		ConfigManager.createFileFromResources("languages/" + translation.getFilename(), dataFolder.toPath().resolve("languages").resolve(translation.getFilename()));
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
