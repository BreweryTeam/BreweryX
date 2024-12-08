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

import com.dre.brewery.Brew;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.utility.BUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CopyCommand implements SubCommand {

    @Override
    public void execute(BreweryPlugin breweryPlugin, Lang lang, CommandSender sender, String label, String[] args) {
        if (args.length > 1) {
            cmdCopy(sender, BUtil.parseInt(args[1]), lang);
        } else {
            cmdCopy(sender, 1, lang);
        }
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return "brewery.cmd.copy";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }

    //@Deprecated but still used?
    public void cmdCopy(CommandSender sender, int count, Lang lang) {
        if (count < 1 || count > 36) {
            lang.sendEntry(sender, "Etc_Usage");
            lang.sendEntry(sender, "Help_Copy");
            return;
        }
        Player player = (Player) sender;
        ItemStack hand = player.getItemInHand();
        if (hand != null) {
            if (Brew.isBrew(hand)) {
                while (count > 0) {
                    ItemStack item = hand.clone();
                    if (!(player.getInventory().addItem(item)).isEmpty()) {
                        lang.sendEntry(sender, "CMD_Copy_Error", "" + count);
                        return;
                    }
                    count--;
                }
                return;
            }
        }

        lang.sendEntry(sender, "Error_ItemNotPotion");

    }
}
