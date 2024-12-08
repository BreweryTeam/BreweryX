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
import com.dre.brewery.api.addons.AddonManager;
import com.dre.brewery.api.addons.BreweryAddon;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.utility.Logging;
import com.dre.brewery.utility.UpdateChecker;
import org.bukkit.command.CommandSender;

import java.util.List;

public class VersionCommand implements SubCommand {
    @Override
    public void execute(BreweryPlugin breweryPlugin, Lang lang, CommandSender sender, String label, String[] args) {
        List<BreweryAddon> addons = new AddonManager(breweryPlugin).getAddons();
        StringBuilder addonString = new StringBuilder();

        for (BreweryAddon addon : addons) {
            addonString.append(addon.getClass().getSimpleName());
            if (addons.indexOf(addon) < addons.size() - 1) {
                addonString.append("&f, &a");
            }
        }

        Logging.msg(sender, "&2BreweryX version&7: &av" + breweryPlugin.getDescription().getVersion() + " &7(Latest: v" + UpdateChecker.getLatestVersion() + ")");
        Logging.msg(sender, "&2Original authors&7: &aGrafe, TTTheKing, Sn0wStorm");
        Logging.msg(sender, "&dBreweryX authors&7: &aJsinco, Mitality, Nadwey, Szarkans, Vutka1");
        Logging.msg(sender, "&2Loaded addons&7: &a" + addonString);
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return "brewery.cmd.version";
    }

    @Override
    public boolean playerOnly() {
        return false;
    }
}
