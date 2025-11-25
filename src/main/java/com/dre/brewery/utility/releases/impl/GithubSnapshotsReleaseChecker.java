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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class GithubSnapshotsReleaseChecker extends ReleaseChecker {

    private static final String CONST_URL = "https://api.github.com/repos/%s/%s/releases";
    private static final String CONST_RELEASE_URL = "https://github.com/%s/%s/releases/tag/%s";
    private static final String CONST_JSON_FIELD = "tag_name";

    private final String link;
    private final String owner;
    private final String repo;

    public GithubSnapshotsReleaseChecker(String owner, String repo) {
        this.link = String.format(CONST_URL, owner, repo);
        this.owner = owner;
        this.repo = repo;
    }

    @Override
    public CompletableFuture<String> resolveLatest() {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(link))
            .GET()
            .build();

        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                JsonObject jsonResponse = JsonParser.parseString(response.body())
                    .getAsJsonArray().asList().stream()
                    .filter(JsonElement::isJsonObject)
                    .map(JsonElement::getAsJsonObject)
                    .toList().get(0);
                this.resolvedLatestVersion = jsonResponse.get(CONST_JSON_FIELD).getAsString();
                return this.resolvedLatestVersion;
            } catch (IOException | InterruptedException e) {
                Logging.warningLog("Failed to resolve latest BreweryX version from GitHub. (No connection?)");
                this.resolvedLatestVersion = CONST_UNRESOLVED;
                return CONST_UNRESOLVED;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> checkForUpdate() {
        return resolveLatest().thenApply(v -> {
            if (v.equals(CONST_UNRESOLVED)) {
                return false;
            }
            return isUpdateAvailable();
        });
    }

    @Override
    public String getDownloadURL() {
        return String.format(CONST_RELEASE_URL, owner, repo, resolvedLatestVersion);
    }
}
