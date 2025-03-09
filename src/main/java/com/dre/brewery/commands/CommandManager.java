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
import com.dre.brewery.commands.subcommands.CopyCommand;
import com.dre.brewery.commands.subcommands.CreateCommand;
import com.dre.brewery.commands.subcommands.DataManagerCommand;
import com.dre.brewery.commands.subcommands.DebugInfoCommand;
import com.dre.brewery.commands.subcommands.DeleteCommand;
import com.dre.brewery.commands.subcommands.DrinkCommand;
import com.dre.brewery.commands.subcommands.HelpCommand;
import com.dre.brewery.commands.subcommands.InfoCommand;
import com.dre.brewery.commands.subcommands.ItemName;
import com.dre.brewery.commands.subcommands.PukeCommand;
import com.dre.brewery.commands.subcommands.ReloadAddonsCommand;
import com.dre.brewery.commands.subcommands.ReloadCommand;
import com.dre.brewery.commands.subcommands.SealCommand;
import com.dre.brewery.commands.subcommands.SetCommand;
import com.dre.brewery.commands.subcommands.ShowStatsCommand;
import com.dre.brewery.commands.subcommands.SimulateCommand;
import com.dre.brewery.commands.subcommands.StaticCommand;
import com.dre.brewery.commands.subcommands.UnLabelCommand;
import com.dre.brewery.commands.subcommands.VersionCommand;
import com.dre.brewery.commands.subcommands.WakeupCommand;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.utility.Logging;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandManager implements TabExecutor {

    private static final BreweryPlugin plugin = BreweryPlugin.getInstance();
    private static final Lang lang = ConfigManager.getConfig(Lang.class);

    private static final Map<String, SubCommand> subCommands = new HashMap<>();

    public CommandManager() {
        addSubCommand("help", new HelpCommand());
        addSubCommand("reload", new ReloadCommand());
        addSubCommand("wakeup", new WakeupCommand());
        addSubCommand("itemName", new ItemName());
        addSubCommand("info", new InfoCommand());
        addSubCommand("seal", new SealCommand());
        addSubCommand("copy", new CopyCommand());
        addSubCommand("delete", new DeleteCommand());
        addSubCommand("static", new StaticCommand());
        addSubCommand("set", new SetCommand());
        addSubCommand("unLabel", new UnLabelCommand());
        addSubCommand("debuginfo", new DebugInfoCommand());
        addSubCommand("showstats", new ShowStatsCommand());
        addSubCommand("puke", new PukeCommand());
        addSubCommand("drink", new DrinkCommand());
        addSubCommand("reloadaddons", new ReloadAddonsCommand());
        addSubCommand("version", new VersionCommand());
        addSubCommand("data", new DataManagerCommand());
        addSubCommand("simulate", new SimulateCommand());

        addSubCommand(new CreateCommand(), "create", "give");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length < 1) {
            CommandUtil.cmdHelp(sender, args);
            return true;
        }

        SubCommand subCommand = subCommands.get(args[0]);
        if (subCommand == null) {
            CommandUtil.cmdHelp(sender, args);
            return true;
        }
        boolean playerOnly = subCommand.playerOnly();
        String permission = subCommand.permission();

        if (playerOnly && !(sender instanceof Player)) {
            lang.sendEntry(sender, "Error_NotPlayer");
            return true;
        } else if (permission != null && !sender.hasPermission(permission)) {
            lang.sendEntry(sender, "Error_NoPermissions");
            return true;
        }

        subCommand.execute(plugin, lang, sender, s, args);
        return false;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if (strings.length == 1) {
            List<String> commands = new ArrayList<>();
            for (Map.Entry<String, SubCommand> entry : subCommands.entrySet()) {
                String perm = entry.getValue().permission();
                if (perm != null && commandSender.hasPermission(perm)) {
                    commands.add(entry.getKey());
                }
            }
            return commands;
        }

        SubCommand subCommand = subCommands.get(strings[0].toLowerCase());
        if (subCommand != null) {
            return subCommand.tabComplete(plugin, commandSender, s, strings);
        }
        return null;
    }

    public static void addSubCommand(String name, SubCommand subCommand) {
        if (subCommands.containsKey(name)) {
            Logging.warningLog("SubCommand with name: &6" + name + " &ealready exists! It's being overwritten!");
        }
        subCommands.put(name, subCommand);
    }

    public static void addSubCommand(SubCommand subCommand, String... names) {
        for (String name : names) {
            addSubCommand(name, subCommand);
        }
    }

    public static void removeSubCommand(String name) {
        subCommands.remove(name);
    }

    public static void removeSubCommand(String... names) {
        for (String name : names) {
            subCommands.remove(name);
        }
    }

    public static void removeSubCommand(SubCommand subCommand) {
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, SubCommand> entry : subCommands.entrySet()) {
            if (entry.getValue() == subCommand) {
                keys.add(entry.getKey());
            }
        }
        for (String key : keys) {
            subCommands.remove(key);
        }
    }

    public static void execute(Class<? extends SubCommand> clazz, CommandSender sender, String label, String[] args) {
        subCommands.values().stream()
            .filter(subCommand -> subCommand.getClass().equals(clazz))
            .forEach(subCommand -> subCommand.execute(plugin, lang, sender, label, args));
    }
}
