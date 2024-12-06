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

package com.dre.brewery.api.addons;

import com.dre.brewery.utility.Logging;

/**
 * Logger for addons. Logs messages with a prefix that includes the addon's name.
 *
 * @see BreweryAddon#getAddonLogger()
 */
public class AddonLogger {

    private final String prefix;

	public AddonLogger(Class<? extends BreweryAddon> addonUninstantiated) {
		this.prefix = "&2[" + addonUninstantiated.getSimpleName() + "] &r";
	}

	public AddonLogger(AddonInfo addonInfo) {
		this.prefix = "&2[" + addonInfo.name() + "] &r";
	}

	public void info(String message) {
		Logging.log(prefix + message);
	}

	public void warning(String message) {
		Logging.warningLog(prefix + message);
	}

	public void severe(String message) {
		Logging.errorLog(prefix + message);
	}

	public void severe(String message, Throwable throwable) {
		Logging.errorLog(prefix + message, throwable);
	}
}
