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

import com.dre.brewery.BPlayer;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.utility.BUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class PukeCommand implements SubCommand {
    @Override
    public void execute(BreweryPlugin breweryPlugin, Lang lang, CommandSender sender, String label, String[] args) {
        Player player = null;
        if (args.length > 1) {
            player = breweryPlugin.getServer().getPlayer(args[1]);
            if (player == null) {
                lang.sendEntry(sender, "Error_NoPlayer", args[1]);
                return;
            }
        }

        if (!(sender instanceof Player) && player == null) {
            lang.sendEntry(sender, "Error_PlayerCommand");
            return;
        }
        if (player == null) {
            player = ((Player) sender);
        } else {
            if (!sender.hasPermission("brewery.cmd.pukeOther") && !player.equals(sender)) {
                lang.sendEntry(sender, "Error_NoPermissions");
                return;
            }
        }
        int count = 0;
        if (args.length > 2) {
            count = BUtil.parseInt(args[2]);
        }
        if (count <= 0) {
            count = 20 + (int) (Math.random() * 40);
        }
        BPlayer.addPuke(player, count);
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return "brewery.cmd.puke";
    }

    @Override
    public boolean playerOnly() {
        return false;
    }
}
