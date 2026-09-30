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

package com.dre.brewery.integration.metrics;

import com.dre.brewery.integration.metrics.bstats.BStatsBrewery;
import com.dre.brewery.integration.metrics.bstats.BStatsBreweryX;
import com.dre.brewery.integration.metrics.faststats.FastStats;
import lombok.Getter;

import java.util.List;
import java.util.function.Function;

@Getter
public class MetricsManager {
    private final FastStats fastStats = new FastStats();
    private final BStatsBrewery bstatsBrewery = new BStatsBrewery();
    private final BStatsBreweryX bStatsBreweryX = new BStatsBreweryX();

    public void enable() {
        getMetricsList().forEach(BreweryMetrics::enable);
    }

    public void disable() {
        getMetricsList().forEach(BreweryMetrics::disable);
    }

    private List<BreweryMetrics> getMetricsList() {
        return List.of(fastStats, bstatsBrewery, bStatsBreweryX);
    }

    /**
     * Updates the specified statistic across all metrics implementations.
     * <p>
     *     Takes a function for flexibility.
     * </p>
     *
     * @param type      the type of statistic to be updated
     * @param collector a function that takes the current cached value and returns the new value to store
     */
    public void handleStat(StatsType type, Function<Object, Object> collector) {
        getMetricsList().forEach(metrics -> {
            StatsBuffer cache = metrics.getStatsCache();
            if (cache != null) {
                Object cached = cache.get(type);
                cache.stats.put(type, collector.apply(cached));
            }
        });
    }
}
