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

package com.dre.brewery.commands.subcommands;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.storage.DataManager;
import com.dre.brewery.storage.StorageInitException;
import com.dre.brewery.utility.Logging;
import org.bukkit.command.CommandSender;

import java.util.List;

public class DataManagerCommand implements SubCommand {
	@Override
	public void execute(BreweryPlugin breweryPlugin, Lang lang, CommandSender sender, String label, String[] args) {
		if (args.length < 2) {
			Logging.msg(sender, "Missing arguments.");
			return;
		}


		switch (args[1].toLowerCase()) {
			case "reload" -> BreweryPlugin.getDataManager().exit(true, true, () -> {
                try {
                    BreweryPlugin.setDataManager(DataManager.createDataManager(ConfigManager.getConfig(Config.class).getStorage()));
                    Logging.msg(sender, "Reloaded the DataManager!");
                } catch (StorageInitException e) {
					Logging.errorLog("Failed to initialize the DataManager! WARNING: This will cause issues and Brewery will NOT be able to save. Check your config and reload.", e);
                }
            });

			case "save" -> BreweryPlugin.getDataManager().saveAll(true, () -> Logging.msg(sender, "Saved all Brewery data!"));

			default -> lang.sendEntry(sender, "Error_UnknownCommand");
		}
	}

	@Override
	public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
		return List.of("reload", "save");
	}

	@Override
	public String permission() {
		return "brewery.cmd.datamanager";
	}

	@Override
	public boolean playerOnly() {
		return false;
	}
}
