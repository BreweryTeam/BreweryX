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

package com.dre.brewery.configuration.sector.capsule;

import com.dre.brewery.configuration.annotation.CommentSpace;
import com.dre.brewery.configuration.annotation.LocalizedComment;
import com.dre.brewery.storage.DataManagerType;
import eu.okaeri.configs.OkaeriConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
@AllArgsConstructor
public class ConfiguredDataManager extends OkaeriConfig {

    @CommentSpace(0)
    @LocalizedComment("config.storage.type")
    private DataManagerType type;
    @CommentSpace(0)
    @LocalizedComment("config.storage.database")
    private String database;
    private String tablePrefix;
    private String address;
    private String username;
    private String password;
}
