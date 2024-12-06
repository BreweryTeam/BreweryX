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

package com.dre.brewery.utility;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Lang;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.logging.Level;


/**
 * Update Checker modified for BreweryX
 */

public class UpdateChecker {

	private static final BreweryPlugin plugin = BreweryPlugin.getInstance();
	private static final Lang lang = ConfigManager.getConfig(Lang.class);

	@Getter @Setter
	private static String latestVersion = "Unknown";
	@Getter @Setter
	private static boolean updateAvailable = false;

	private final int resourceID;

	public UpdateChecker(int resourceID) {
		this.resourceID = resourceID;
	}

	public static void notify(final Player player) {
		if (!updateAvailable || !player.hasPermission("brewery.update")) {
			return;
		}

		lang.sendEntry(player, "Etc_UpdateAvailable", "v" + plugin.getDescription().getVersion(), "v" + latestVersion);
	}

	/**
	 * Query the API to find the latest approved file's details.
	 */
	public void query(final Consumer<String> consumer) {
		BreweryPlugin.getScheduler().runTaskAsynchronously(() -> {
			try (InputStream is = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceID + "/~").openStream(); Scanner scann = new Scanner(is)) {
				if (scann.hasNext()) {
					consumer.accept(scann.next());
				}
			} catch (IOException e) {
				plugin.getLogger().log(Level.WARNING, "Cannot look for updates: " + e);
			}
		});
	}

	public static int parseVersion(String version) {
		StringBuilder sb = new StringBuilder();
		for (char c : version.toCharArray()) {
			if (Character.isDigit(c)) {
				sb.append(c);
			}
		}
		return Integer.parseInt(sb.toString());
	}

	public static void run(int resourceID) {
		new UpdateChecker(resourceID).query(latestVersion -> {
			String currentVersion = plugin.getDescription().getVersion();

			if (UpdateChecker.parseVersion(latestVersion) > UpdateChecker.parseVersion(currentVersion)) {
				UpdateChecker.setUpdateAvailable(true);
				Logging.log(lang.getEntry("Etc_UpdateAvailable", "v" + currentVersion, "v" + latestVersion));
			}
			UpdateChecker.setLatestVersion(latestVersion);
		});
	}
}
