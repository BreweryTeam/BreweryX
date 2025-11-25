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

package com.dre.brewery.utility.releases.impl;

import com.dre.brewery.utility.Logging;
import com.dre.brewery.utility.releases.ReleaseChecker;

import java.io.IOException;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class SpigotReleaseChecker extends ReleaseChecker {

    private static final String CONST_URL = "https://api.spigotmc.org/legacy/update.php?resource=%s/~";
    private static final String CONST_RELEASE_URL = "https://www.spigotmc.org/resources/%s";

    private final String link;
    private final int resourceId;

    public SpigotReleaseChecker(int resourceId) {
        this.link = String.format(CONST_URL, resourceId);
        this.resourceId = resourceId;
    }

    @Override
    public CompletableFuture<String> resolveLatest() {
        return CompletableFuture.supplyAsync(() -> {
            try (Scanner scanner = new Scanner(new URL(link).openStream())) {
                if (scanner.hasNext()) {
                    this.resolvedLatestVersion = scanner.next();
                    return this.resolvedLatestVersion;
                }
            } catch (IOException ignored) {

            }
            return this.failedToResolve();
        });
    }

    @Override
    public CompletableFuture<Boolean> checkForUpdate() {
        return resolveLatest().thenApply(ignored -> isUpdateAvailable());
    }

    public String failedToResolve() {
        Logging.warningLog("Failed to resolve latest BreweryX version from SpigotMC. (No connection?)");
        this.resolvedLatestVersion = CONST_UNRESOLVED;
        return CONST_UNRESOLVED;
    }

    @Override
    public String getDownloadURL() {
        return String.format(CONST_RELEASE_URL, resourceId);
    }
}
