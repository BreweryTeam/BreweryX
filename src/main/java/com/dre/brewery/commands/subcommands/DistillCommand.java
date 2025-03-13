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
import org.bukkit.inventory.meta.PotionMeta;

import java.util.List;

public class DistillCommand implements SubCommand {

    @Override
    public void execute(BreweryPlugin breweryPlugin, Lang lang, CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            cmdDistill(lang, (Player) sender, 1);
        } else {
            int distillRuns = BUtil.parseInt(args[1]).orElse(0);
            if (distillRuns <= 0) {
                lang.sendEntry(sender, "CMD_Invalid_Distill_Runs", args[1]);
                return;
            }
            cmdDistill(lang, (Player) sender, distillRuns);
        }
    }

    private static void cmdDistill(Lang lang, Player player, int distillRuns) {
        ItemStack item = player.getInventory().getItemInMainHand();
        Brew brew = Brew.get(item);
        if (brew == null) {
            lang.sendEntry(player, "Error_ItemNotPotion");
            return;
        }
        PotionMeta meta = (PotionMeta) item.getItemMeta();

        for (int i = 0; i < distillRuns; i++) {
            brew.distillSlot(item, meta);
        }
        Logging.debugLog(String.format("distill: distilled for %d runs: %s",
            distillRuns, ChatColor.stripColor(brew.toString())));
        player.getInventory().setItemInMainHand(item);
        lang.sendEntry(player, "CMD_Distilled", distillRuns);
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
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
