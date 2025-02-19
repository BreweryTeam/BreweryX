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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public record Translation(String fileName) {

    // Languages added should have a language translation, and may have a config translation

    public static final Translation EN = new Translation("en.yml");
    private static final List<Translation> DEFAULT_TRANSLATIONS = compileTranslations();

    public static Translation getTranslation(String language) {
        return new Translation(language.toLowerCase(Locale.ROOT) + ".yml");
    }

    public static List<Translation> getDefaultTranslations() {
        return DEFAULT_TRANSLATIONS;
    }

    private static List<Translation> compileTranslations() {
        try {
            URI uri = Translation.class.getResource("/languages").toURI();
            Path myPath;
            if (uri.getScheme().equals("jar")) {
                try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                    myPath = fileSystem.getPath("/languages");
                    return fromPaths(Files.walk(myPath, 1));
                }
            } else {
                myPath = Paths.get(uri);
                return fromPaths(Files.walk(myPath, 1));

            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static List<Translation> fromPaths(Stream<Path> pathStream) {
        return pathStream
            .map(Path::getFileName)
            .map(Path::toString)
            .filter(filename -> filename.endsWith(".yml"))
            .map(Translation::new)
            .toList();
    }
}
