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
import com.dre.brewery.commands.CommandUtil;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.Logging;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class HelpCommand implements SubCommand {

    @Override
    public void execute(BreweryPlugin breweryPlugin, Lang lang, CommandSender sender, String label, String[] args) {
        int page = 1;
        if (args.length > 1) {
            page = BUtil.parseInt(args[1]);
        }

        ArrayList<String> commands = CommandUtil.getCommands(sender);

        if (page == 1) {
            Logging.msg(sender, "&6" + breweryPlugin.getDescription().getName() + " v" + breweryPlugin.getDescription().getVersion());
        }

        BUtil.list(sender, commands, page);
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return null;
    }

    @Override
    public boolean playerOnly() {
        return false;
    }
}


