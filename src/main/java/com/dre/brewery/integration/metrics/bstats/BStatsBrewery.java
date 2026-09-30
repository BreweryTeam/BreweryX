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

package com.dre.brewery.integration.metrics.bstats;

import com.dre.brewery.BCauldron;
import com.dre.brewery.BPlayer;
import com.dre.brewery.Barrel;
import com.dre.brewery.Brew;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.Wakeup;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.integration.metrics.BreweryMetrics;
import com.dre.brewery.integration.metrics.StatsBuffer;
import com.dre.brewery.recipe.BRecipe;
import com.dre.brewery.utility.Logging;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * General stats written by the original author of Brewery.
 */
public class BStatsBrewery implements BreweryMetrics {

    private static final int BSTATS_ID = 3494;

    private final Config config = ConfigManager.getConfig(Config.class);

    private Metrics bstats;
    public int brewsCreated;
    public int brewsCreatedCmd; // Created by command
    public int exc, good, norm, bad, terr; // Brews drunken with quality

    public void metricsForCreate(boolean byCmd) {
        if (brewsCreated == Integer.MAX_VALUE) return;
        brewsCreated++;
        if (byCmd) {
            if (brewsCreatedCmd == Integer.MAX_VALUE) return;
            brewsCreatedCmd++;
        }
    }

    public void forDrink(Brew brew) {
        int quality = brew.getQuality();

        if (quality >= 9) {
            exc++;
        } else if (quality >= 7) {
            good++;
        } else if (quality >= 5) {
            norm++;
        } else if (quality >= 3) {
            bad++;
        } else {
            terr++;
        }
    }

    @Override
    public void enable() {
        try {
            bstats = new Metrics(BreweryPlugin.getInstance(), BSTATS_ID);
            bstats.addCustomChart(new SingleLineChart("drunk_players", BPlayer::numDrunkPlayers));
            bstats.addCustomChart(new SingleLineChart("brews_in_existence", () -> brewsCreated));
            bstats.addCustomChart(new SingleLineChart("barrels_built", Barrel.getAllBarrels()::size));
            bstats.addCustomChart(new SingleLineChart("cauldrons_boiling", BCauldron.bcauldrons::size));
            bstats.addCustomChart(new AdvancedPie("brew_quality", () -> {
                Map<String, Integer> map = new HashMap<>(8);
                map.put("excellent", exc);
                map.put("good", good);
                map.put("normal", norm);
                map.put("bad", bad);
                map.put("terrible", terr);
                return map;
            }));
            bstats.addCustomChart(new AdvancedPie("brews_created", () -> {
                Map<String, Integer> map = new HashMap<>(4);
                map.put("by command", brewsCreatedCmd);
                map.put("brewing", brewsCreated - brewsCreatedCmd);
                return map;
            }));

            bstats.addCustomChart(new SimplePie("number_of_recipes", () -> {
                int recipes = BRecipe.getAllRecipes().size();

                if (recipes < 7) return "Less than 7";
                if (recipes < 11) return "7-10";
                if (recipes == 11) return "11"; // Default recipe count
                if (recipes == 20) return "20"; // Default recipe count

                // Group recipes 12-29 into pairs (e.g., 12-13, 14-15)
                if (recipes <= 29) {
                    int start = recipes - (recipes % 2);
                    return start + "-" + (start + 1);
                }

                if (recipes < 35) return "30-34";
                if (recipes < 40) return "35-39";
                if (recipes < 45) return "40-44";
                if (recipes <= 50) return "45-50";

                return "More than 50";
            }));
            bstats.addCustomChart(new SimplePie("cauldron_particles", () -> {
                if (!config.isEnableCauldronParticles()) {
                    return "disabled";
                }
                if (config.isMinimalParticles()) {
                    return "minimal";
                }
                return "enabled";
            }));
            bstats.addCustomChart(new SimplePie("wakeups", () -> {
                if (!config.isEnableWake()) {
                    return "disabled";
                }

                int wakeups = Wakeup.wakeups.size();
                if (wakeups == 0)  return "0";
                if (wakeups <= 5)  return "1-5";
                if (wakeups <= 10) return "6-10";
                if (wakeups <= 20) return "11-20";

                return "More than 20";
            }));
            bstats.addCustomChart(new SimplePie("v2_mc_version", () -> {
                String mcv = Bukkit.getBukkitVersion();
                mcv = mcv.substring(0, mcv.indexOf('.', 2));
                int index = mcv.indexOf('-');
                if (index > -1) {
                    mcv = mcv.substring(0, index);
                }
                if (mcv.matches("^\\d\\.\\d{1,2}$")) {
                    // Start, digit, dot, 1-2 digits, end
                    return mcv;
                } else {
                    return "undef";
                }
            }));
            bstats.addCustomChart(new DrilldownPie("plugin_mc_version", () -> {
                Map<String, Map<String, Integer>> map = new HashMap<>(3);
                String mcv = Bukkit.getBukkitVersion();
                mcv = mcv.substring(0, mcv.indexOf('.', 2));
                int index = mcv.indexOf('-');
                if (index > -1) {
                    mcv = mcv.substring(0, index);
                }
                if (mcv.matches("^\\d\\.\\d{1,2}$")) {
                    // Start, digit, dot, 1-2 digits, end
                    mcv = "MC " + mcv;
                } else {
                    mcv = "undef";
                }
                Map<String, Integer> innerMap = new HashMap<>(3);
                innerMap.put(mcv, 1);
                map.put(BreweryPlugin.getInstance().getDescription().getVersion(), innerMap);
                return map;
            }));
            bstats.addCustomChart(new SimplePie("language", config::getLanguage));
            bstats.addCustomChart(new SimplePie("config_scramble", () -> config.isEnableEncode() ? "enabled" : "disabled"));
            bstats.addCustomChart(new SimplePie("config_lore_color", () -> {
                if (config.isColorInBarrels()) {
                    return config.isColorInBrewer() ? "both" : "in barrels";
                }
                return config.isColorInBrewer() ? "in distiller" : "none";
            }));
            bstats.addCustomChart(new SimplePie("config_always_show", () -> {
                if (config.isAlwaysShowQuality()) {
                    return config.isAlwaysShowAlc() ? "both" : "quality stars";
                }
                return config.isAlwaysShowAlc() ? "alc content" : "none";
            }));
        } catch (Exception | LinkageError e) {
            Logging.errorLog("Failed to submit stats data to bStats.org", e);
        }
    }

    @Override
    public void disable() {
        bstats.shutdown();
    }

    @Override
    public @Nullable StatsBuffer getStatsCache() {
        return null;
    }
}
