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

package com.dre.brewery.utility;

import com.dre.brewery.commands.subcommands.ReloadCommand;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public final class Logging {

    private static final Config config = ConfigManager.getConfig(Config.class);

    public static void msg(CommandSender sender, String msg) {
        sender.sendMessage(BUtil.color(config.getPluginPrefix() + msg));
    }

    public static void log(String msg) {
        Bukkit.getConsoleSender().sendMessage(BUtil.color(config.getPluginPrefix() + msg));
    }

    public static void log(LogLevel level, String msg) {
        log(level, msg, null);
    }

    public static void log(LogLevel level, String msg, @Nullable Throwable throwable) {
        switch (level) {
            case INFO -> log(msg);
            case WARNING -> warningLog(msg);
            case ERROR -> {
                if (throwable != null) {
                    errorLog(msg, throwable);
                } else {
                    errorLog(msg);
                }
            }
            case DEBUG -> debugLog(msg);
        }
    }

    public static void debugLog(String msg) {
        if (ConfigManager.getConfig(Config.class).isDebug()) {
            msg(Bukkit.getConsoleSender(), "&2[Debug] &f" + msg);
        }
    }

    public static void warningLog(String msg) {
        Bukkit.getConsoleSender().sendMessage(BUtil.color("&e[BreweryX] WARNING: " + msg));
    }

    public static void errorLog(String msg) {
        String str = BUtil.color("&c[BreweryX] ERROR: " + msg);
        Bukkit.getConsoleSender().sendMessage(str);
        if (ReloadCommand.getReloader() != null) { // I hate this, but I'm too lazy to go change all of it - Jsinco
            ReloadCommand.getReloader().sendMessage(str);
        }
    }

    // TODO: cleanup
    public static void errorLog(String msg, Throwable throwable) {
        errorLog(msg);
        errorLog("&6" + throwable.toString());
        for (StackTraceElement ste : throwable.getStackTrace()) {
            String str = ste.toString();
            if (str.contains(".jar//")) {
                str = str.substring(str.indexOf(".jar//") + 6);
            }
            errorLog(str);
        }
        Throwable cause = throwable.getCause();
        while (cause != null) {
            Bukkit.getConsoleSender().sendMessage(BUtil.color("&c[BreweryX]&6 Caused by: " + cause));
            for (StackTraceElement ste : cause.getStackTrace()) {
                String str = ste.toString();
                if (str.contains(".jar//")) {
                    str = str.substring(str.indexOf(".jar//") + 6);
                }
                Bukkit.getConsoleSender().sendMessage(BUtil.color("&c[BreweryX]&6      " + str));
            }
            cause = cause.getCause();
        }
    }


    public enum LogLevel {
        INFO,
        WARNING,
        ERROR,
        DEBUG
    }
}
