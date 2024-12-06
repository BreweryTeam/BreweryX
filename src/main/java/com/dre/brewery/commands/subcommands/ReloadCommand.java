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

import com.dre.brewery.BCauldron;
import com.dre.brewery.BSealer;
import com.dre.brewery.Brew;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.commands.CommandUtil;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.configuration.AbstractOkaeriConfigFile;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.configurer.TranslationManager;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.utility.Logging;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ReloadCommand implements SubCommand {

	@Getter
	private static CommandSender reloader;

    @Override
    public void execute(BreweryPlugin breweryPlugin, Lang lang, CommandSender sender, String label, String[] args) {
		if (!sender.equals(Bukkit.getConsoleSender())) {
			reloader = sender;
		}


		try {
			// Reload translation manager
			TranslationManager.newInstance(breweryPlugin.getDataFolder());

			// Reload each config
			for (var entry : ConfigManager.LOADED_CONFIGS.entrySet()) {
				AbstractOkaeriConfigFile file = entry.getValue();
				try {
					file.reload();
				} catch (Throwable e) {
					Logging.errorLog("Something went wrong trying to load " + file.getBindFile().getFileName() + "!", e);
				}
			}

			// Reload Cauldron Ingredients
			ConfigManager.loadCauldronIngredients();
			// Reload Recipes
			ConfigManager.loadRecipes();

			// Reload Cauldron Particle Recipes
			BCauldron.reload();

			// Clear Recipe completions
			CommandUtil.reloadTabCompleter();

			// Sealing table recipe
			BSealer.registerRecipe();

			// Let addons know this command was executed
			BreweryPlugin.getAddonManager().reloadAddons();

			// Reload Recipes <-- TODO: Not really sure what this is doing...? - Jsinco
			boolean successful = true;
			for (Brew brew : Brew.legacyPotions.values()) {
				if (!brew.reloadRecipe()) {
					successful = false;
				}
			}

			if (!successful) {
				lang.sendEntry(sender, "Error_Recipeload");
			} else {
				lang.sendEntry(sender, "CMD_Reload");
			}


		} catch (Throwable e) {
			Logging.errorLog("Something went wrong trying to reload Brewery!", e);
		}
		// Make sure this reloader is set to null after
		reloader = null;
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return "brewery.cmd.reload";
    }

    @Override
    public boolean playerOnly() {
        return false;
    }
}
