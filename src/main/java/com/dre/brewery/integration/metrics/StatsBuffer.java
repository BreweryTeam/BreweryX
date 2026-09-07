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

import java.util.HashMap;
import java.util.Map;

public class StatsBuffer {
    public final Map<StatsType, Object> stats = new HashMap<>();

    public StatsBuffer() {
        for (StatsType statsType : StatsType.values()) {
            stats.put(statsType, statsType.getDefaultValue());
        }
    }

    public Object get(StatsType statsType) {
        return stats.get(statsType);
    }

    public void clear() {
        for (StatsType statsType : StatsType.values()) {
            stats.put(statsType, statsType.getDefaultValue());
        }
    }
}
