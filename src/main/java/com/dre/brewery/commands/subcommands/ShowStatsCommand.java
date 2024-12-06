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
import com.dre.brewery.BPlayer;
import com.dre.brewery.Barrel;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.Wakeup;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.recipe.BRecipe;
import com.dre.brewery.utility.Logging;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ShowStatsCommand implements SubCommand {
    @Override
    public void execute(BreweryPlugin breweryPlugin, Lang lang, CommandSender sender, String label, String[] args) {
        //if (sender instanceof ConsoleCommandSender && !sender.isOp()) return;

        Logging.msg(sender, "Drunk Players: " + BPlayer.numDrunkPlayers());
        Logging.msg(sender, "Brews created: " + BreweryPlugin.getInstance().getBreweryStats().brewsCreated);
        Logging.msg(sender, "Barrels built: " + Barrel.barrels.size());
        Logging.msg(sender, "Cauldrons boiling: " + BCauldron.bcauldrons.size());
        Logging.msg(sender, "Number of Recipes: " + BRecipe.getAllRecipes().size());
        Logging.msg(sender, "Wakeups: " + Wakeup.wakeups.size());
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return "brewery.cmd.showstats";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }
}
