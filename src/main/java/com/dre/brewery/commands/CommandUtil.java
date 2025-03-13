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

import com.dre.brewery.Brew;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.recipe.BRecipe;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.Logging;
import com.dre.brewery.utility.MinecraftVersion;
import com.dre.brewery.utility.PermissionUtil;
import com.dre.brewery.utility.Tuple;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dre.brewery.utility.PermissionUtil.BPermission.COPY;
import static com.dre.brewery.utility.PermissionUtil.BPermission.CREATE;
import static com.dre.brewery.utility.PermissionUtil.BPermission.DELETE;
import static com.dre.brewery.utility.PermissionUtil.BPermission.DRINK;
import static com.dre.brewery.utility.PermissionUtil.BPermission.DRINK_OTHER;
import static com.dre.brewery.utility.PermissionUtil.BPermission.INFO;
import static com.dre.brewery.utility.PermissionUtil.BPermission.INFO_OTHER;
import static com.dre.brewery.utility.PermissionUtil.BPermission.PUKE;
import static com.dre.brewery.utility.PermissionUtil.BPermission.PUKE_OTHER;
import static com.dre.brewery.utility.PermissionUtil.BPermission.RELOAD;
import static com.dre.brewery.utility.PermissionUtil.BPermission.SEAL;
import static com.dre.brewery.utility.PermissionUtil.BPermission.SET;
import static com.dre.brewery.utility.PermissionUtil.BPermission.STATIC;
import static com.dre.brewery.utility.PermissionUtil.BPermission.UNLABEL;
import static com.dre.brewery.utility.PermissionUtil.BPermission.WAKEUP;

public class CommandUtil {

    private static final BreweryPlugin plugin = BreweryPlugin.getInstance();
    private static final MinecraftVersion VERSION = BreweryPlugin.getMCVersion();
    private static final Lang lang = ConfigManager.getConfig(Lang.class);

    // Todo: Replace with a map
    private static Set<Tuple<String, String>> mainSet;
    private static Set<Tuple<String, String>> altSet;
    private static final String[] QUALITY = { "1", "10" };


    public static void cmdHelp(CommandSender sender, String[] args) {

        int page = 1;
        if (args.length > 1) {
            page = BUtil.parseInt(args[1]).orElse(1);
        }

        ArrayList<String> commands = getCommands(sender);

        if (page == 1) {
            Logging.msg(sender, "&6" + plugin.getDescription().getName() + " v" + plugin.getDescription().getVersion());
        }

        BUtil.list(sender, commands, page);

    }

    @Nullable
    public static Tuple<Brew, Player> getFromCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            return null;
        }

        int quality = 10;
        boolean hasQuality = false;
        String pName = null;
        if (args.length > 2) {
            quality = BUtil.getRandomIntInRange(args[args.length - 1]);

            if (quality <= 0 || quality > 10) {
                pName = args[args.length - 1];
                if (args.length > 3) {
                    quality = BUtil.getRandomIntInRange(args[args.length - 2]);
                }
            }
            if (quality > 0 && quality <= 10) {
                hasQuality = true;
            } else {
                quality = 10;
            }
        }
        Player player = null;
        if (pName != null) {
            player = plugin.getServer().getPlayer(pName);
        }

        if (!(sender instanceof Player) && player == null) {
            lang.sendEntry(sender, "Error_PlayerCommand");
            return null;
        }

        if (player == null) {
            player = ((Player) sender);
            pName = null;
        }
        int stringLength = args.length - 1;
        if (pName != null) {
            stringLength--;
        }
        if (hasQuality) {
            stringLength--;
        }

        String name;
        if (stringLength > 1) {
            StringBuilder builder = new StringBuilder(args[1]);

            for (int i = 2; i < stringLength + 1; i++) {
                builder.append(" ").append(args[i]);
            }
            name = builder.toString();
        } else {
            name = args[1];
        }
        name = name.replaceAll("\"", "");

        BRecipe recipe = BRecipe.getMatching(name);
        if (recipe != null) {
            return new Tuple<>(recipe.createBrew(quality), player);
        } else {
            lang.sendEntry(sender, "Error_NoBrewName", name);
        }
        return null;
    }

    public static ArrayList<String> getCommands(CommandSender sender) {

        ArrayList<String> cmds = new ArrayList<>();
        cmds.add(lang.getEntry("Help_Help"));
        PermissionUtil.evaluateExtendedPermissions(sender);

        if (INFO.checkCached(sender)) {
            cmds.add(lang.getEntry("Help_Info"));
        }

        if (VERSION.isOrLater(MinecraftVersion.V1_13) && SEAL.checkCached(sender)) {
            cmds.add(lang.getEntry("Help_Seal"));
        }

        if (UNLABEL.checkCached(sender)) {
            cmds.add(lang.getEntry("Help_UnLabel"));
        }

        if (PermissionUtil.noExtendedPermissions(sender)) {
            return cmds;
        }

        if (INFO_OTHER.checkCached(sender)) {
            cmds.add(lang.getEntry("Help_InfoOther"));
        }

        if (CREATE.checkCached(sender)) {
            cmds.add(lang.getEntry("Help_Create"));
            cmds.add(lang.getEntry("Help_Give"));
            cmds.add(lang.getEntry("Help_Distill"));
            cmds.add(lang.getEntry("Help_Age"));
            cmds.add(lang.getEntry("Help_Simulate"));
            cmds.add(lang.getEntry("Help_Simulate2"));
        }

        if (DRINK.checkCached(sender) || DRINK_OTHER.checkCached(sender)) {
            cmds.add(lang.getEntry("Help_Drink"));
        }

        if (RELOAD.checkCached(sender)) {
            cmds.add(lang.getEntry("Help_Configname"));
            cmds.add(lang.getEntry("Help_Reload"));
        }

        if (PUKE.checkCached(sender) || PUKE_OTHER.checkCached(sender)) {
            cmds.add(lang.getEntry("Help_Puke"));
        }

        if (WAKEUP.checkCached(sender)) {
            cmds.add(lang.getEntry("Help_Wakeup"));
            cmds.add(lang.getEntry("Help_WakeupList"));
            cmds.add(lang.getEntry("Help_WakeupCheck"));
            cmds.add(lang.getEntry("Help_WakeupCheckSpecific"));
            cmds.add(lang.getEntry("Help_WakeupAdd"));
            cmds.add(lang.getEntry("Help_WakeupRemove"));
        }

        if (STATIC.checkCached(sender)) {
            cmds.add(lang.getEntry("Help_Static"));
        }

        if (SET.checkCached(sender)) {
            cmds.add(lang.getEntry("Help_Set"));
        }

        if (COPY.checkCached(sender)) {
            cmds.add(lang.getEntry("Help_Copy"));
        }

        if (DELETE.checkCached(sender)) {
            cmds.add(lang.getEntry("Help_Delete"));
        }

        return cmds;
    }


    public static List<String> recipeNamesAndIds(String[] args) {
        if (args.length == 2) {
            return recipeNamesAndIds(args[1]);
        } else {
            if (args[args.length - 1].matches("10|[1-9]")) {
                return null; // automatically suggests player names
            } else {
                return filterWithInput(QUALITY, args[args.length - 1]);
            }
        }
    }

    public static List<String> recipeNamesAndIds(String arg) {
        if (mainSet == null) {
            mainSet = new HashSet<>();
            altSet = new HashSet<>();
            for (BRecipe recipe : BRecipe.getAllRecipes()) {
                mainSet.addAll(createLookupFromName(recipe.getName(5)));

                Set<String> altNames = new HashSet<>(3);
                altNames.add(recipe.getName(1));
                altNames.add(recipe.getName(10));
                if (recipe.getId() != null) { // Leaving a null check JUST in case. But ids are never null in the current implementation
                    altNames.add(recipe.getId());
                }

                for (String altName : altNames) {
                    altSet.addAll(createLookupFromName(altName));
                }

            }
        }

        final String input = arg.toLowerCase();

        List<String> options = mainSet.stream()
            .filter(s -> s.a().startsWith(input))
            .map(Tuple::second)
            .collect(Collectors.toList());
        if (options.isEmpty()) {
            options = altSet.stream()
                .filter(s -> s.a().startsWith(input))
                .map(Tuple::second)
                .collect(Collectors.toList());
        }
        return options;
    }

    private static List<Tuple<String, String>> createLookupFromName(final String name) {
        return Arrays.stream(name.split(" "))
            .map(word -> new Tuple<>(word.toLowerCase(), name))
            .collect(Collectors.toList());
    }

    public static List<String> filterWithInput(String[] options, String input) {
        return Arrays.stream(options)
            .filter(s -> s.startsWith(input))
            .collect(Collectors.toList());
    }

    public static void reloadTabCompleter() {
        mainSet = null;
        altSet = null;
    }
}
