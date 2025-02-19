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

import com.dre.brewery.utility.Logging;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class ConfigTranslations {

    private final Map<String, Object> translations;

    public ConfigTranslations(Translation activeTranslation) {
        this(activeTranslation, new Yaml());
    }

    public ConfigTranslations(Translation activeTranslation, Yaml yamlInstance) {
        try (InputStream inputStream = ConfigTranslations.class
            .getResourceAsStream("/config-langs/" + activeTranslation.fileName())) {
            if (inputStream != null) {
                translations = yamlInstance.load(inputStream);
                return;
            }
            try (InputStream defaultInputStream = ConfigTranslations.class.getResourceAsStream("/config-langs/" + Translation.EN.fileName())) {
                Logging.log("Could not find config translation, using default");
                if (defaultInputStream == null) {
                    throw new IOException("Couldn't find default translation file");
                }
                translations = yamlInstance.load(defaultInputStream);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Attempts to retrieve a translation for the given key.
     *
     * @param key the key to retrieve the translation for
     * @return the translation for the given key, or null if not found
     */
    @Nullable
    public String getTranslation(String key) {
        try {
            if (translations == null) {
                return null;
            } else if (!key.contains(".")) {
                return (String) translations.get(key);
            }


            String[] keys = key.split("\\.");
            Map<String, Object> current = translations;

            for (int i = 0; i < keys.length - 1; i++) {
                current = (Map<String, Object>) current.get(keys[i]);
                if (current == null) {
                    return null;
                }
            }

            return (String) current.get(keys[keys.length - 1]);
        } catch (Exception e) {
            Logging.errorLog("Error while getting translation for key: " + key, e);
            return null;
        }
    }
}
