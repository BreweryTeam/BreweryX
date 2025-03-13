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
import com.dre.brewery.commands.CommandUtil;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimulateCommand implements SubCommand {

    @Override
    public void execute(BreweryPlugin breweryPlugin, Lang lang, CommandSender sender, String label, String[] args) {
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
            } else if (status instanceof Status.Recipe) {
                simulateRecipe(lang, sender, args);
                return;
            } else if (status instanceof Status.Finished finished) {
                simulateIngredients(lang, sender, args, currentArgIdx, finished.simulation());
                return;
            } else if (status instanceof Status.Error error) {
                lang.sendEntry(sender, error.errorType().getTranslationKey(), arg);
                return;
            } else {
                currentArgIdx++;
            }
        }

    }

    private static void sendUsage(Lang lang, CommandSender sender) {
        lang.sendEntry(sender, "Etc_Usage");
        lang.sendEntry(sender, "Help_Simulate");
        lang.sendEntry(sender, "Help_Simulate2");
        lang.sendEntry(sender, "Help_Simulate_Options");
        lang.sendEntry(sender, "Help_Simulate_Distill");
        lang.sendEntry(sender, "Help_Simulate_Age");
        lang.sendEntry(sender, "Help_Simulate_Brewer");
        lang.sendEntry(sender, "Help_Simulate_Player");
    }

    private static void simulateIngredients(Lang lang, CommandSender sender, String[] args, int currentArgIdx,
                                            SimulationParameters simulationParameters) {
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

    private static void simulateRecipe(Lang lang, CommandSender sender, String[] args) {
        String recipeArg = Arrays.stream(args)
            .skip(1)
            .collect(Collectors.joining(" "));
        BRecipe recipe = BRecipe.getMatching(recipeArg);
        if (recipe == null) {
            lang.sendEntry(sender, "Error_NoBrewName", recipeArg);
            return;
        }

        Logging.debugLog("simulate: simulating recipe " + recipe.getId());
        simulate(lang, sender, SimulationParameters.of(recipe), recipe.getIngredients());
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
        SimulationParser parser = new SimulationParser();
        int currentArgIdx = 1;
        while (true) {
            String arg = args[currentArgIdx];
            if (currentArgIdx >= args.length - 1) {
                List<String> completions = parser.getTabCompletions(arg);
                return completions == null ? null : StringUtil.copyPartialMatches(arg, completions, new ArrayList<>());
            }

            Status status = parser.parse(arg);
            if (status instanceof Status.Help || status instanceof Status.Error || status instanceof Status.Recipe) {
                return List.of();
            } else if (status instanceof Status.Finished) {
                break;
            } else {
                currentArgIdx++;
            }
        }

        String lastArg = args[args.length - 1];
        return StringUtil.copyPartialMatches(lastArg, getIngredientCompletions(), new ArrayList<>());
    }

    private static class SimulationParser {

        private int cookedTime = -1;
        private int distillRuns = -1;
        @Nullable
        BarrelWoodType woodType = null;
        private float ageTime = Float.NaN;
        @Nullable
        private Player brewer = null;
        @Nullable
        private Player player = null;

        private final EnumSet<Option> options = EnumSet.noneOf(Option.class);
        private State state = State.START;

        public Status parse(String arg) {
            if (arg.isBlank()) {
                return new Status.Updated();
            }

            switch (state) {

                case START -> {
                    if (arg.equalsIgnoreCase("help")) {
                        return new Status.Help();
                    }
                    if (BUtil.isInt(arg)) {
                        int cookedTime = BUtil.parseInt(arg).orElse(-1);
                        if (cookedTime < 0) {
                            return new Status.Error(ErrorType.COOK);
                        }
                        this.cookedTime = cookedTime;
                        state = State.OPTIONS;
                    } else {
                        return new Status.Recipe();
                    }
                }

                case OPTIONS -> {
                    if (!arg.startsWith("-")) {
                        return new Status.Finished(createSimulation());
                    }
                    Option option = Option.get(arg);
                    if (option == null) {
                        return new Status.Error(ErrorType.INVALID_OPTION);
                    }
                    if (!options.add(option)) {
                        return new Status.Error(ErrorType.DUPLICATE_OPTION);
                    }
                    state = option.getState();
                }

                case DISTILL -> {
                    int distillRuns = BUtil.parseInt(arg).orElse(-1);
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
                    float ageTime = BUtil.parseFloat(arg).orElse(-1);
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
            if (cookedTime < 0) {
                throw new IllegalStateException("cookedTime < 0");
            }
            OptionalInt distill = distillRuns <= 0 ? OptionalInt.empty() : OptionalInt.of(distillRuns);

            boolean hasAge = woodType != null && Float.isFinite(ageTime) && ageTime > 0;
            Age age = !hasAge ? null : new Age(woodType, ageTime);

            return new SimulationParameters(cookedTime, distill, age, brewer, player);
        }

        @Nullable
        public List<String> getTabCompletions(String arg) {
            return switch (state) {
                case START -> {
                    List<String> completions = new ArrayList<>(List.of("help"));
                    completions.addAll(BUtil.numberRange(0, 30));
                    completions.addAll(CommandUtil.recipeNamesAndIds(arg));
                    yield completions;
                }
                case OPTIONS -> {
                    List<String> completions = new ArrayList<>();
                    completions.addAll(getOptionCompletions());
                    completions.addAll(getIngredientCompletions());
                    yield completions;
                }
                case DISTILL -> BUtil.numberRange(1, 10);
                case WOOD -> BarrelWoodType.TAB_COMPLETIONS;
                case AGE -> BUtil.numberRange(1, 50);
                case BREWER, PLAYER -> null;
            };
        }

        private List<String> getOptionCompletions() {
            return EnumSet.complementOf(options).stream()
                .map(Option::getOptions)
                .flatMap(List::stream)
                .toList();
        }

        @Getter
        private enum Option {
            DISTILL(State.DISTILL, "-d", "--distill"),
            AGE(State.WOOD, "-a", "--age"),
            BREWER(State.BREWER, "-b", "--brewer"),
            PLAYER(State.PLAYER, "-p", "--player");

            private final State state;
            private final List<String> options;

            Option(State state, String... options) {
                this.state = state;
                this.options = List.of(options);
            }

            public boolean matches(String arg) {
                return options.contains(arg.toLowerCase(Locale.ROOT));
            }

            public static @Nullable Option get(String arg) {
                for (Option option : values()) {
                    if (option.matches(arg)) {
                        return option;
                    }
                }
                return null;
            }
        }

        private enum State {
            /** Either a number (cook time), string (recipe), or "help" */
            START,
            OPTIONS, DISTILL, WOOD, AGE, BREWER, PLAYER
        }

    }

    private sealed interface Status {
        /** The parser was updated with the latest argument, and parsing should continue */
        record Updated() implements Status {}
        /** Need to display command usage */
        record Help() implements Status {}
        /** Command is {@code /brew simulate <Recipe>} */
        record Recipe() implements Status {}
        /** Parsing finished, next arguments are ingredients */
        record Finished(SimulationParameters simulation) implements Status {}
        /** User error */
        record Error(ErrorType errorType) implements Status {}
    }

    private record SimulationParameters(
        int cookedTime,
        OptionalInt distillRuns,
        @Nullable Age age,
        @Nullable Player brewer,
        @Nullable Player player
    ) {
        public static SimulationParameters of(BRecipe recipe) {
            return new SimulationParameters(
                recipe.getCookingTime(),
                recipe.needsDistilling() ? OptionalInt.of(recipe.getDistillruns()) : OptionalInt.empty(),
                Age.of(recipe),
                null,
                null
            );
        }
    }

    private record Age(BarrelWoodType barrelType, float ageTime) {
        public static @Nullable Age of(BRecipe recipe) {
            if (recipe.needsToAge()) {
                BarrelWoodType barrelType = recipe.getWood();
                return new Age(barrelType.isSpecific() ? barrelType : BarrelWoodType.OAK, recipe.getAge());
            }
            return null;
        }
    }

    @AllArgsConstructor
    @Getter
    private enum ErrorType {
        COOK("CMD_Invalid_Cook_Time"),
        INVALID_OPTION("CMD_Invalid_Option"),
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
