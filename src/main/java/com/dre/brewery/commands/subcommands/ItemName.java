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
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.utility.MinecraftVersion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

public class ItemName implements SubCommand {
    @Override
    public void execute(BreweryPlugin breweryPlugin, Lang lang, CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;
        @SuppressWarnings("deprecation")
        ItemStack hand = BreweryPlugin.getMCVersion().isOrLater(MinecraftVersion.V1_9) ? player.getInventory().getItemInMainHand() : player.getItemInHand();
        if (hand != null) {
            lang.sendEntry(sender, "CMD_Configname", hand.getType().name().toLowerCase(Locale.ENGLISH));
        } else {
            lang.sendEntry(sender, "CMD_Configname_Error");
        }
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return "brewery.cmd.itemname";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }
}
