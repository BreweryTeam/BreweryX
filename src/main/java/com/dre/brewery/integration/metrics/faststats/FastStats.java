/*
 * BreweryX Bukkit-Plugin for an alternate brewing process
 * Copyright (C) 2024-2026 The Brewery Team
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

package com.dre.brewery.integration.metrics.faststats;

import com.dre.brewery.BCauldron;
import com.dre.brewery.BPlayer;
import com.dre.brewery.Barrel;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.integration.metrics.bstats.BStatsBreweryX;
import com.dre.brewery.recipe.BRecipe;
import dev.faststats.bukkit.BukkitMetrics;
import dev.faststats.core.data.Metric;

public class FastStats {

    private static final String TOKEN = "9e823f79476a54405051b485438be8e7";
    private final BukkitMetrics metrics;
    private final Config config = ConfigManager.getConfig(Config.class);

    public FastStats() {
        this.metrics = BukkitMetrics.factory()
            .token(TOKEN)
            .addMetric(Metric.number("recipe_count", () -> BRecipe.getAllRecipes().size()))
            .addMetric(Metric.number("addons_amount", () -> BreweryPlugin.getAddonManager().getAddons().size()))

            .addMetric(Metric.string("storage_type", () -> BreweryPlugin.getDataManager().getType().getFormattedName()))
            .addMetric(Metric.string("language", config::getLanguage))
            .addMetric(Metric.string("branch", BStatsBreweryX::getBranch))

            .addMetric(Metric.number("drunk_players", BPlayer::numDrunkPlayers))
            .addMetric(Metric.number("barrels_built", Barrel.getAllBarrels()::size))
            .addMetric(Metric.number("cauldrons_boiling", BCauldron.bcauldrons::size))

            .create(BreweryPlugin.getInstance());
    }

    public void enable() {
        metrics.ready();
    }

    public void disable() {
        metrics.shutdown();
    }
}
