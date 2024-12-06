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
import com.dre.brewery.Wakeup;
import com.dre.brewery.commands.CommandUtil;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.utility.BUtil;
import org.bukkit.command.CommandSender;

import java.util.List;

public class WakeupCommand implements SubCommand {
    @Override
    public void execute(BreweryPlugin breweryPlugin, Lang lang, CommandSender sender, String label, String[] args) {
        if (args.length == 1) {
            CommandUtil.cmdHelp(sender, args);
            return;
        }

        if (args[1].equalsIgnoreCase("add")) {

            Wakeup.set(sender);

        } else if (args[1].equalsIgnoreCase("list")){

            int page = 1;
            String world = null;
            if (args.length > 2) {
                page = BUtil.parseInt(args[2]);
            }
            if (args.length > 3) {
                world = args[3];
            }
            Wakeup.list(sender, page, world);

        } else if (args[1].equalsIgnoreCase("remove")){

            if (args.length > 2) {
                int id = BUtil.parseInt(args[2]);
                Wakeup.remove(sender, id);
            } else {
                lang.sendEntry(sender, "Etc_Usage");
                lang.sendEntry(sender, "Help_WakeupRemove");
            }

        } else if (args[1].equalsIgnoreCase("check")){

            int id = -1;
            if (args.length > 2) {
                id = BUtil.parseInt(args[2]);
                if (id < 0) {
                    id = 0;
                }
            }
            Wakeup.check(sender, id, id == -1);

        } else if (args[1].equalsIgnoreCase("cancel")){

            Wakeup.cancel(sender);

        } else {

            lang.sendEntry(sender, "Error_UnknownCommand");
            lang.sendEntry(sender, "Error_ShowHelp");

        }
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return "brewery.cmd.wakeup";
    }

    @Override
    public boolean playerOnly() {
        return false;
    }
}
