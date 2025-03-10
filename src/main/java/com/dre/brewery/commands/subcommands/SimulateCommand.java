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
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimulateCommand implements SubCommand {

    private void sendUsage(Lang lang, CommandSender sender) {
        lang.sendEntry(sender, "Etc_Usage");
        lang.sendEntry(sender, "Help_Simulate");
        lang.sendEntry(sender, "Help_Simulate_Options");
        lang.sendEntry(sender, "Help_Simulate_Distill");
        lang.sendEntry(sender, "Help_Simulate_Age");
        lang.sendEntry(sender, "Help_Simulate_Brewer");
        lang.sendEntry(sender, "Help_Simulate_Player");
    }

    @Override
    public void execute(BreweryPlugin breweryPlugin, Lang lang, CommandSender sender, String label, String[] args) {
        SimulationParameters simulationParameters;
        SimulationParser parser = new SimulationParser();
        int currentArgIdx = 1;
        while (true) {
            // check one arg ahead since we need at least one ingredient arg
            if (currentArgIdx >= args.length) {
                sendUsage(lang, sender);
                return;
            }
            String arg = args[currentArgIdx];

            Status status = parser.parse(arg);
            if (status instanceof Status.Help) {
                sendUsage(lang, sender);
                return;
            } else if (status instanceof Status.Finished finished) {
                simulationParameters = finished.simulation();
                break;
            } else if (status instanceof Status.Error error) {
                lang.sendEntry(sender, error.errorType().getTranslationKey(), arg);
                return;
            } else {
                currentArgIdx++;
            }
        }

        String ingredientsStr = Arrays.stream(args, currentArgIdx, args.length)
            .collect(Collectors.joining(" "));
        List<String> ingredientsList = BUtil.splitStringKeepingQuotes(ingredientsStr);
        for (int i = 0; i < ingredientsList.size(); i++) {
            String ingredient = ingredientsList.get(i);
            if (BUtil.isInt(ingredient)) {
                String prevIngredient = i > 0 ? ingredientsList.get(i - 1) : lang.getEntry("CMD_Ingredient");
                lang.sendEntry(sender, "CMD_Invalid_Ingredient", ingredient, prevIngredient);
                return;
            }
        }

        BRecipe.IngredientsResult result = BRecipe.loadIngredientsVerbose(ingredientsList);
        if (result instanceof BRecipe.IngredientsResult.Error error) {
            lang.sendEntry(sender, error.error().getTranslationKey(), error.invalidPart());
            return;
        }
        List<RecipeItem> itemList = ((BRecipe.IngredientsResult.Success) result).ingredients();

        simulate(lang, sender, simulationParameters, itemList);
    }

    private static void simulate(Lang lang, CommandSender sender, SimulationParameters simulation, List<RecipeItem> itemList) {
        BIngredients ingredients = new BIngredients();
        for (RecipeItem item : itemList) {
            for (int i = 0; i < item.getAmount(); i++) {
                ingredients.addGeneric(item);
            }
        }

        ItemStack item = ingredients.cook(simulation.cookedTime(), simulation.brewer());
        Brew brew = new Brew(ingredients);
        Logging.debugLog(String.format("simulate: cooked for %d minutes: %s",
            simulation.cookedTime(), ChatColor.stripColor(brew.toString())));

        if (simulation.distillRuns().isPresent()) {
            if (!(item.getItemMeta() instanceof PotionMeta meta)) {
                lang.sendEntry(sender, "CMD_Cannot_Distill");
                return;
            }

            int distillRuns = simulation.distillRuns().getAsInt();
            for (int i = 0; i < distillRuns; i++) {
                brew.distillSlot(item, meta);
            }
            Logging.debugLog(String.format("simulate: distilled for %d runs: %s",
                distillRuns, ChatColor.stripColor(brew.toString())));

            if (!brew.hasRecipe()) {
                lang.sendEntry(sender, "CMD_Distill_Ruined");
                giveBrew(lang, sender, item, simulation.player());
                return;
            }
        }

        Age age = simulation.age();
        if (age != null) {
            brew.age(item, age.ageTime(), age.barrelType());
            Logging.debugLog(String.format("simulate: aged for %.3f years in %s barrel: %s",
                age.ageTime(), age.barrelType().getFormattedName(), ChatColor.stripColor(brew.toString())));

            if (!brew.hasRecipe()) {
                lang.sendEntry(sender, "CMD_Age_Ruined");
            }
        }

        giveBrew(lang, sender, item, simulation.player());
    }

    private static void giveBrew(Lang lang, CommandSender sender, ItemStack item, @Nullable Player player) {
        if (player != null) {
            player.getInventory().addItem(item);
        } else if (sender instanceof Player self) {
            self.getInventory().addItem(item);
        } else {
            Brew fromItem = Brew.get(item);
            if (fromItem == null) {
                // this message should never appear since simulation was successful
                sender.sendMessage("&cCould not get brew from item");
                return;
            }
            sender.sendMessage(fromItem.toString());
        }
        lang.sendEntry(sender, "CMD_Simulated");
    }

    @Override
    public List<String> tabComplete(BreweryPlugin breweryPlugin, CommandSender sender, String label, String[] args) {
        String lastArg = args[args.length - 1];

        SimulationParser parser = new SimulationParser();
        int currentArgIdx = 1;
        while (true) {
            if (currentArgIdx >= args.length - 1) {
                List<String> completions = parser.getTabCompletions();
                return completions == null ? null : StringUtil.copyPartialMatches(lastArg, completions, new ArrayList<>());
            }
            String arg = args[currentArgIdx];

            Status status = parser.parse(arg);
            if (status instanceof Status.Help) {
                return List.of();
            } else if (status instanceof Status.Finished) {
                break;
            } else if (status instanceof Status.Error) {
                return List.of();
            } else {
                currentArgIdx++;
            }
        }

        return StringUtil.copyPartialMatches(lastArg, getIngredientCompletions(), new ArrayList<>());
    }

    private static class SimulationParser {

        private int cookedTime = -1;
        private int distillRuns = -1;
        BarrelWoodType woodType = null;
        private float ageTime = Float.NaN;
        private Player brewer = null;
        private Player player = null;

        private State state = State.COOK_OR_HELP;

        @Nullable
        public Status parse(String arg) {
            if (arg.isBlank()) {
                return new Status.Updated();
            }

            switch (state) {

                case COOK_OR_HELP -> {
                    if (arg.equalsIgnoreCase("help")) {
                        return new Status.Help();
                    }
                    int cookedTime = BUtil.parseIntOrZero(arg);
                    if (cookedTime <= 0) {
                        return new Status.Error(ErrorType.COOK);
                    }
                    this.cookedTime = cookedTime;
                    state = State.OPTIONS;
                }

                case OPTIONS -> {
                    if (!arg.startsWith("-")) {
                        return new Status.Finished(createSimulation());

                    } else if (arg.equalsIgnoreCase("-d") || arg.equalsIgnoreCase("--distill")) {
                        if (distillRuns == -1) {
                            state = State.DISTILL;
                        } else {
                            return new Status.Error(ErrorType.DUPLICATE_OPTION);
                        }

                    } else if (arg.equalsIgnoreCase("-a") || arg.equalsIgnoreCase("--age")) {
                        if (Float.isNaN(ageTime)) {
                            state = State.WOOD;
                        } else {
                            return new Status.Error(ErrorType.DUPLICATE_OPTION);
                        }

                    } else if (arg.equalsIgnoreCase("-b") || arg.equalsIgnoreCase("--brewer")) {
                        if (brewer == null) {
                            state = State.BREWER;
                        } else {
                            return new Status.Error(ErrorType.DUPLICATE_OPTION);
                        }

                    } else if (arg.equalsIgnoreCase("-p") || arg.equalsIgnoreCase("--player")) {
                        if (player == null) {
                            state = State.PLAYER;
                        } else {
                            return new Status.Error(ErrorType.DUPLICATE_OPTION);
                        }

                    } else {
                        return new Status.Error(ErrorType.OPTION);
                    }
                }

                case DISTILL -> {
                    int distillRuns = BUtil.parseIntOrZero(arg);
                    if (distillRuns < 0) {
                        return new Status.Error(ErrorType.DISTILL_RUNS);
                    }
                    this.distillRuns = distillRuns;
                    state = State.OPTIONS;
                }

                case WOOD -> {
                    BarrelWoodType woodType = BarrelWoodType.fromName(arg);
                    if (woodType == null || !woodType.isSpecific()) {
                        return new Status.Error(ErrorType.WOOD_TYPE);
                    }
                    this.woodType = woodType;
                    state = State.AGE;
                }
                case AGE -> {
                    float ageTime = BUtil.parseFloatOrZero(arg);
                    if (ageTime < 0) {
                        return new Status.Error(ErrorType.AGE_TIME);
                    }
                    this.ageTime = ageTime;
                    state = State.OPTIONS;
                }

                case BREWER -> {
                    Player brewer = BUtil.getPlayerfromString(arg);
                    if (brewer == null) {
                        return new Status.Error(ErrorType.PLAYER);
                    }
                    this.brewer = brewer;
                    state = State.OPTIONS;
                }

                case PLAYER -> {
                    Player player = BUtil.getPlayerfromString(arg);
                    if (player == null) {
                        return new Status.Error(ErrorType.PLAYER);
                    }
                    this.player = player;
                    state = State.OPTIONS;
                }

            }
            return new Status.Updated();
        }

        // assumes cook time has been parsed
        private SimulationParameters createSimulation() {
            if (cookedTime <= 0) {
                throw new IllegalStateException("cookedTime <= 0");
            }
            OptionalInt distill = distillRuns == -1 ? OptionalInt.empty() : OptionalInt.of(distillRuns);
            Age age = woodType == null || Float.isNaN(ageTime) ? null : new Age(woodType, ageTime);
            return new SimulationParameters(cookedTime, distill, age, brewer, player);
        }

        @Nullable
        public List<String> getTabCompletions() {
            return switch (state) {
                case COOK_OR_HELP -> {
                    List<String> completions = new ArrayList<>(List.of("help"));
                    completions.addAll(BUtil.numberRange(1, 30));
                    yield completions;
                }
                case OPTIONS -> {
                    List<String> completions = getOptionCompletions();
                    completions.addAll(getIngredientCompletions());
                    yield completions;
                }
                case DISTILL -> BUtil.numberRange(1, 10);
                case WOOD -> Arrays.stream(BarrelWoodType.values())
                    .filter(BarrelWoodType::isSpecific)
                    .map(BarrelWoodType::getFormattedName)
                    .toList();
                case AGE -> BUtil.numberRange(1, 50);
                case BREWER -> null;
                case PLAYER -> null;
            };
        }

        private List<String> getOptionCompletions() {
            List<String> completions = new ArrayList<>();
            if (distillRuns == -1) {
                completions.add("-d");
                completions.add("--distill");
            }
            if (Float.isNaN(ageTime)) {
                completions.add("-a");
                completions.add("--age");
            }
            if (brewer == null) {
                completions.add("-b");
                completions.add("--brewer");
            }
            if (player == null) {
                completions.add("-p");
                completions.add("--player");
            }
            return completions;
        }

        private enum State {
            COOK_OR_HELP, OPTIONS, DISTILL, WOOD, AGE, BREWER, PLAYER
        }

    }

    private sealed interface Status {
        record Updated() implements Status {}
        record Help() implements Status {}
        record Finished(SimulationParameters simulation) implements Status {}
        record Error(ErrorType errorType) implements Status {}
    }

    private record SimulationParameters(
        int cookedTime,
        OptionalInt distillRuns,
        @Nullable Age age,
        @Nullable Player brewer,
        @Nullable Player player
    ) {}
    private record Age(BarrelWoodType barrelType, float ageTime) {}

    @AllArgsConstructor
    @Getter
    private enum ErrorType {
        COOK("CMD_Invalid_Cook_Time"),
        OPTION("CMD_Invalid_Option"),
        DUPLICATE_OPTION("CMD_Duplicate_Option"),
        DISTILL_RUNS("CMD_Invalid_Distill_Runs"),
        WOOD_TYPE("CMD_Invalid_Wood_Type"),
        AGE_TIME("CMD_Invalid_Age_Time"),
        PLAYER("Error_NoPlayer");

        private final String translationKey;
    }

    private static List<String> getIngredientCompletions() {
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
