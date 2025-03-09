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

import com.dre.brewery.BIngredients;
import com.dre.brewery.BarrelWoodType;
import com.dre.brewery.Brew;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.recipe.BCauldronRecipe;
import com.dre.brewery.recipe.BRecipe;
import com.dre.brewery.recipe.RecipeItem;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.Logging;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimulateCommand implements SubCommand {

    @Override
    public void execute(BreweryPlugin breweryPlugin, Lang lang, CommandSender sender, String label, String[] args) {
        if (args.length < 6) {
            lang.sendEntry(sender, "Etc_Usage");
            lang.sendEntry(sender, "Help_Simulate");
            return;
        }

        int cookedTime = BUtil.parseIntOrZero(args[1]);
        if (cookedTime <= 0) {
            lang.sendEntry(sender, "CMD_Invalid_Cook_Time", args[1]);
            return;
        }
        int distillRuns = BUtil.parseIntOr(args[2], -1);
        if (distillRuns < 0) {
            lang.sendEntry(sender, "CMD_Invalid_Distill_Runs", args[2]);
            return;
        }
        BarrelWoodType barrelType = BarrelWoodType.parse(args[3]);
        float ageTime = BUtil.parseFloatOrNaN(args[4]);
        if (!Float.isFinite(ageTime) || ageTime < 0) {
            lang.sendEntry(sender, "CMD_Invalid_Age_Time", args[4]);
            return;
        }
        if (ageTime > 0 && !barrelType.isSpecific()) {
            lang.sendEntry(sender, "CMD_Invalid_Wood_Type", args[3]);
            return;
        }

        String ingredientsStr = Arrays.stream(args, 5, args.length)
            .collect(Collectors.joining(" "));
        List<String> ingredientsList = BUtil.splitStringKeepingQuotes(ingredientsStr);

        BRecipe.IngredientsResult result = BRecipe.loadIngredientsVerbose(ingredientsList);
        if (result instanceof BRecipe.IngredientsResult.Error error) {
            lang.sendEntry(sender, error.error().getTranslationKey(), error.invalidPart());
            return;
        }
        List<RecipeItem> itemList = ((BRecipe.IngredientsResult.Success) result).ingredients();

        BIngredients ingredients = new BIngredients();
        for (RecipeItem item : itemList) {
            for (int i = 0; i < item.getAmount(); i++) {
                ingredients.addGeneric(item);
            }
        }

        Player player = sender instanceof Player ? (Player) sender : null;
        ItemStack item = ingredients.cook(cookedTime, player);
        Brew brew = new Brew(ingredients);
        Logging.debugLog(String.format("simulate: cooked for %d minutes: %s",
            cookedTime, ChatColor.stripColor(brew.toString())));

        if (distillRuns > 0) {
            if (!(item.getItemMeta() instanceof PotionMeta meta)) {
                lang.sendEntry(sender, "CMD_Cannot_Distill");
                return;
            }
            for (int i = 0; i < distillRuns; i++) {
                brew.distillSlot(item, meta);
            }
            Logging.debugLog(String.format("simulate: distilled for %d runs: %s",
                distillRuns, ChatColor.stripColor(brew.toString())));
            if (!brew.hasRecipe()) {
                lang.sendEntry(sender, "CMD_Distill_Ruined");
                giveBrew(lang, sender, item);
                return;
            }
        }

        if (ageTime > 0) {
            brew.age(item, ageTime, barrelType);
            Logging.debugLog(String.format("simulate: aged for %.3f years in %s barrel: %s",
                ageTime, barrelType.getFormattedName(), ChatColor.stripColor(brew.toString())));
            if (!brew.hasRecipe()) {
                lang.sendEntry(sender, "CMD_Age_Ruined");
            }
        }

        giveBrew(lang, sender, item);
    }

    private static void giveBrew(Lang lang, CommandSender sender, ItemStack item) {
        if (sender instanceof Player player) {
            player.getInventory().addItem(item);
        } else {
            Brew fromItem = Brew.get(item);
            if (fromItem == null) {
                sender.sendMessage("&cCould not get brew from item");
                return;
            }
            sender.sendMessage(fromItem.toString());
        }
        lang.sendEntry(sender, "CMD_Simulated");
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return switch (args.length) {
            case 2, 3 -> List.of();
            case 4 -> {
                List<String> names = Arrays.stream(BarrelWoodType.values())
                    .filter(t -> t != BarrelWoodType.ANY)
                    .map(BarrelWoodType::getFormattedName)
                    .toList();
                yield StringUtil.copyPartialMatches(args[3], names, new ArrayList<>());
            }
            case 5 -> {
                BarrelWoodType barrelType = BarrelWoodType.parse(args[3]);
                if (!barrelType.isSpecific()) {
                    yield List.of("0");
                }
                yield List.of();
            }
            default -> StringUtil.copyPartialMatches(args[args.length - 1], getIngredients(), new ArrayList<>());
        };
    }

    private static List<String> getIngredients() {
        return Stream.concat(
            BCauldronRecipe.getAllRecipes().stream()
                .map(BCauldronRecipe::getIngredients),
            BRecipe.getAllRecipes().stream()
                .map(BRecipe::getIngredients)
        ).flatMap(List::stream)
            .map(RecipeItem::toConfigStringNoAmount)
            .sorted()
            .distinct()
            .map(BUtil::quote)
            .toList();
    }

    @Override
    public String permission() {
        return "brewery.cmd.create";
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

}
