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

@Getter
public class Metrics {

     private final FastStats fastStats;
     private final BStatsBrewery bstatsBrewery;
     private final BStatsBreweryX bStatsBreweryX;

     public Metrics() {
          this.fastStats = new FastStats();
          this.bstatsBrewery  = new BStatsBrewery();
          this.bStatsBreweryX = new BStatsBreweryX();
     }

     public void enable() {
          fastStats.enable();
          bstatsBrewery.setupBStats();
          bStatsBreweryX.setupBStats();
     }

     public void disable() {
         fastStats.disable();
     }
}
