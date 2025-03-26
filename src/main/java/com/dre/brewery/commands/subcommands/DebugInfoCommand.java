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

import com.dre.brewery.BIngredients;
import com.dre.brewery.Brew;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.recipe.BRecipe;
import com.dre.brewery.recipe.BestRecipeResult;
import com.dre.brewery.recipe.Ingredient;
import com.dre.brewery.recipe.RecipeEvaluation;
import com.dre.brewery.recipe.RecipeItem;
import com.dre.brewery.utility.Logging;
import com.dre.brewery.utility.MinecraftVersion;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class DebugInfoCommand implements SubCommand {


    @Override
    public void execute(BreweryPlugin breweryPlugin, Lang lang, CommandSender sender, String label, String[] args) {
        debugInfo(sender, args.length > 1 ? args[1] : null);
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        return null;
    }

    @Override
    public String permission() {
        return "brewery.cmd.debuginfo";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }


    public void debugInfo(CommandSender sender, String recipeName) {
        if (BreweryPlugin.getMCVersion().isOrEarlier(MinecraftVersion.V1_9) || !sender.isOp()) return;
        Player player = (Player) sender;
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand != null) {
            Brew brew = Brew.get(hand);
            if (brew == null) return;
            Logging.log(brew.toString());
            BIngredients ingredients = brew.getIngredients();
            if (recipeName == null) {
                Logging.log("&lIngredients:");
                for (Ingredient ing : ingredients.getIngredientList()) {
                    Logging.log(ing.toString());
                }
                Logging.log("&lTesting Recipes");
                for (BRecipe recipe : BRecipe.getAllRecipes()) {
                    logRecipe(recipe, brew);
                }
                BestRecipeResult distill = ingredients.getBestRecipeFull(brew.getWood(), brew.getAgeTime(), true);
                Logging.log("&lDistill-Recipe: &r" + ChatColor.stripColor(distill.toString()));
                BestRecipeResult nonDistill = ingredients.getBestRecipeFull(brew.getWood(), brew.getAgeTime(), false);
                Logging.log("&lRecipe: &r" + ChatColor.stripColor(nonDistill.toString()));
            } else {
                BRecipe recipe = BRecipe.getMatching(recipeName);
                if (recipe == null) {
                    Logging.msg(player, "Could not find Recipe " + recipeName);
                    return;
                }
                Logging.log("&lIngredients in Recipe " + recipe.getRecipeName() + "&r&l:&r");
                for (RecipeItem ri : recipe.getIngredients()) {
                    Logging.log(ri.toString());
                }
                Logging.log("&lIngredients in Brew:");
                for (Ingredient ingredient : ingredients.getIngredientList()) {
                    int amountInRecipe = recipe.amountOf(ingredient);
                    Logging.log(ingredient.toString() + ": " + amountInRecipe + " of this are in the Recipe");
                }
                logRecipe(recipe, brew);
            }

            Logging.msg(player, "Debug Info for item written into Log");
        }
    }

    private static void logRecipe(BRecipe recipe, Brew brew) {
        BIngredients ingredients = brew.getIngredients();
        RecipeEvaluation ingQ = ingredients.getIngredientQualityFull(recipe);
        Logging.log(String.format("%s&r ingQlty: %s", recipe.getRecipeName(), ingQ));
        RecipeEvaluation cookQ = ingredients.getCookingQualityFull(recipe, false);
        Logging.log(String.format("%s&r cookQlty: %s", recipe.getRecipeName(), cookQ));
        RecipeEvaluation cookDistQ = ingredients.getCookingQualityFull(recipe, true);
        Logging.log(String.format("%s&r cook+DistQlty: %s", recipe.getRecipeName(), cookDistQ));
        RecipeEvaluation ageQ = ingredients.getAgeQualityFull(recipe, brew.getAgeTime());
        Logging.log(String.format("%s&r ageQlty: %s", recipe.getRecipeName(), ageQ));
        RecipeEvaluation woodQ = ingredients.getWoodQualityFull(recipe, brew.getWood());
        Logging.log(String.format("%s&r woodQlty: %s", recipe.getRecipeName(), woodQ));
    }

}
