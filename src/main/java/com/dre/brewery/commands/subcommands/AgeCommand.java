/*
 * BreweryX Bukkit-Plugin for an alternate brewing process
 * Copyright (C) 2024-2025 The Brewery Team
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

import com.dre.brewery.BarrelWoodType;
import com.dre.brewery.Brew;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.Logging;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class AgeCommand implements SubCommand {

    @Override
    public void execute(BreweryPlugin breweryPlugin, Lang lang, CommandSender sender, String label, String[] args) {
        if (args.length < 3) {
            lang.sendEntry(sender, "Etc_Usage");
            lang.sendEntry(sender, "Help_Age");
            return;
        }

        BarrelWoodType woodType = BarrelWoodType.fromName(args[1]);
        if (woodType == null || !woodType.isSpecific()) {
            lang.sendEntry(sender, "CMD_Invalid_Wood_Type", args[1]);
            return;
        }

        float ageTime = BUtil.parseFloat(args[2]).orElse(0);
        if (ageTime < 1) {
            lang.sendEntry(sender, "CMD_Invalid_Age_Time", args[2]);
            return;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();
        Brew brew = Brew.get(item);
        if (brew == null) {
            lang.sendEntry(player, "Error_ItemNotPotion");
            return;
        }

        brew.age(item, ageTime, BarrelWoodType.ANY);
        Logging.debugLog(String.format("age: aged for %s years: %s",
            args[2], ChatColor.stripColor(brew.toString())));
        lang.sendEntry(sender, "CMD_Aged", args[2]);
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        if (args.length == 2) {
            return StringUtil.copyPartialMatches(args[1], BarrelWoodType.TAB_COMPLETIONS, new ArrayList<>());
        }
        return List.of();
    }

    @Override
    public String permission() {
        return "brewery.cmd.create";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }

}
