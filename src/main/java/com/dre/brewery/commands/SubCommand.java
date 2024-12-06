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

package com.dre.brewery.commands;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.configuration.files.Lang;
import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {

    /**
     * Executes the subcommand's code
     * @param breweryPlugin Instance of the Brewery plugin
     * @param sender The CommandSender that executed the command
     * @param label The command label (alias)
     * @param args The command arguments
     */
    void execute(BreweryPlugin breweryPlugin, Lang lang, CommandSender sender, String label, String[] args);

    /**
     * Returns a list of possible tab completions for the subcommand
     * @param breweryPlugin Instance of the Brewery plugin
     * @param sender The CommandSender that executed the command
     * @param label The command label (alias)
     * @param args The command arguments
     * @return A list of possible tab completions for the subcommand
     */
    List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args);

    /**
     * @return the subcommand's required permission node
     */
    String permission();

    /**
     * @return if the command can only be executed by a player
     */
    boolean playerOnly();
}
