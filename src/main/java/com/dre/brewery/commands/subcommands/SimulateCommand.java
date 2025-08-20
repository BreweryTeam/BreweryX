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
import com.dre.brewery.Translatable;
import com.dre.brewery.commands.SubCommand;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.recipe.BCauldronRecipe;
import com.dre.brewery.recipe.BRecipe;
import com.dre.brewery.recipe.RecipeItem;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.Logging;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.stream.Stream;

public class SimulateCommand implements SubCommand {

    @Override
    public void execute(BreweryPlugin breweryPlugin, Lang lang, CommandSender sender, String label, String[] args) {
        List<String> arguments = BUtil.splitStringKeepingQuotes(String.join(" ", args));

        SimulationParser parser = new SimulationParser();
        for (int i = 1; i <= arguments.size(); i++) {
            Status status;
            if (i < arguments.size()) {
                String arg = arguments.get(i);
                status = parser.parse(arg);
            } else {
                status = parser.finish();
            }

            if (status instanceof Status.Help) {
                sendUsage(lang, sender);
                return;
            } else if (status instanceof Status.Finished finished) {
                simulate(lang, sender, finished.simulation());
                return;
            } else if (status instanceof Status.Error error) {
                lang.sendEntry(sender, error.error().getTranslationKey(), error.args());
                return;
            }
        }
        throw new AssertionError("parser.finish() must not return Status.Updated()");
    }

    private static void sendUsage(Lang lang, CommandSender sender) {
        lang.sendEntry(sender, "Etc_Usage");
        lang.sendEntry(sender, "Help_Simulate");
        lang.sendEntry(sender, "Help_Simulate_Options");
        lang.sendEntry(sender, "Help_Simulate_Recipe");
        lang.sendEntry(sender, "Help_Simulate_Cook");
        lang.sendEntry(sender, "Help_Simulate_Distill");
        lang.sendEntry(sender, "Help_Simulate_Age");
        lang.sendEntry(sender, "Help_Simulate_Brewer");
        lang.sendEntry(sender, "Help_Simulate_Player");
    }

    private static void simulate(Lang lang, CommandSender sender, SimulationParameters simulation) {
        BIngredients ingredients = new BIngredients();
        for (RecipeItem item : simulation.ingredients()) {
            for (int i = 0; i < item.getAmount(); i++) {
                ingredients.addGeneric(item);
            }
        }
        Logging.debugLog(String.format("simulate: ingredients=%s", ingredients));

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
            BarrelWoodType barrelType = age.barrelType();
            if (barrelType == null) {
                lang.sendEntry(sender, "Error_MissingBarrelType");
                return;
            }
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
        List<String> arguments = BUtil.splitStringKeepingQuotes(String.join(" ", args));

        SimulationParser parser = new SimulationParser();
        for (int i = 1; i <= arguments.size(); i++) {
            String arg = arguments.size() == 1 ? args[1] : arguments.get(i);

            if (i >= arguments.size() - 1) {
                String rawLastArg = args[args.length - 1];

                // supporting tab complete mid-quote is too complicated
                if (rawLastArg.equals("\"")) {
                    return List.of();
                }

                // If player inputs `/brew simulate --age ` (notice the trailing space), rawLastArg will be blank.
                // Since splitStringKeepingQuotes() will remove the trailing space,
                // we need to first parse `--age` then tab complete on `` (blank).
                if (rawLastArg.isBlank()) {
                    Status status = parser.parse(arg);
                    if (status instanceof Status.Help || status instanceof Status.Error) {
                        return List.of();
                    }
                    return tabComplete(parser, rawLastArg);
                }

                return tabComplete(parser, arg);
            }

            Status status = parser.parse(arg);
            if (status instanceof Status.Help || status instanceof Status.Error) {
                return List.of();
            }
        }
        throw new AssertionError("unreachable");
    }

    private static @Nullable List<String> tabComplete(SimulationParser parser, String arg) {
        List<String> completions = parser.getTabCompletions();
        return completions == null ? null : StringUtil.copyPartialMatches(arg, completions, new ArrayList<>());
    }

    @ToString
    private static class SimulationParser {

        private static final List<String> helpStrings = List.of("help", "-h", "--help");

        @Nullable
        private BRecipe recipe = null;
        private int cookedTime = -1;
        private int distillRuns = -1;
        @Nullable
        BarrelWoodType woodType = null;
        private float ageTime = Float.NaN;
        private final List<RecipeItem> ingredients = new ArrayList<>();
        @Nullable
        private Player brewer = null;
        @Nullable
        private Player player = null;

        private final EnumSet<Option> options = EnumSet.noneOf(Option.class);
        private State state = State.OPTIONS;

        @Nullable
        private String prevArg = null;

        public Status parse(String arg) {
            if (arg.isBlank()) {
                return new Status.Updated();
            }

            switch (state) {

                case OPTIONS -> {
                    if (prevArg == null && helpStrings.contains(arg.toLowerCase(Locale.ROOT))) {
                        return new Status.Help();
                    }
                    if (!arg.startsWith("-")) {
                        if (prevArg == null) {
                            return new Status.Error(ErrorType.INVALID_OPTION, arg);
                        } else {
                            state = State.INGREDIENTS;
                            return parseIngredient(arg);
                        }
                    }

                    Option option = Option.get(arg);
                    if (option == null) {
                        return new Status.Error(ErrorType.INVALID_OPTION, arg);
                    }
                    if (!options.add(option)) {
                        return new Status.Error(ErrorType.DUPLICATE_OPTION, arg);
                    }
                    state = option.getState();
                }

                case RECIPE -> {
                    BRecipe recipe = BRecipe.getMatching(arg);
                    if (recipe == null) {
                        return new Status.Error(ErrorType.RECIPE, arg);
                    }
                    this.recipe = recipe;
                    state = State.OPTIONS;
                }

                case COOK -> {
                    int cookedTime = BUtil.parseInt(arg).orElse(-1);
                    if (cookedTime <= 0) {
                        return new Status.Error(ErrorType.COOK, arg);
                    }
                    this.cookedTime = cookedTime;
                    state = State.OPTIONS;
                }

                case DISTILL -> {
                    int distillRuns = BUtil.parseInt(arg).orElse(-1);
                    if (distillRuns <= 0) {
                        return new Status.Error(ErrorType.DISTILL_RUNS, arg);
                    }
                    this.distillRuns = distillRuns;
                    state = State.OPTIONS;
                }

                case WOOD -> {
                    BarrelWoodType woodType = BarrelWoodType.fromName(arg);
                    if (woodType == null || !woodType.isSpecific()) {
                        return new Status.Error(ErrorType.WOOD_TYPE, arg);
                    }
                    this.woodType = woodType;
                    state = State.AGE;
                }
                case AGE -> {
                    float ageTime = BUtil.parseFloat(arg).orElse(-1);
                    if (ageTime <= 0) {
                        return new Status.Error(ErrorType.AGE_TIME, arg);
                    }
                    this.ageTime = ageTime;
                    state = State.OPTIONS;
                }

                case BREWER -> {
                    Player brewer = BUtil.getPlayerfromString(arg);
                    if (brewer == null) {
                        return new Status.Error(ErrorType.PLAYER, arg);
                    }
                    this.brewer = brewer;
                    state = State.OPTIONS;
                }

                case PLAYER -> {
                    Player player = BUtil.getPlayerfromString(arg);
                    if (player == null) {
                        return new Status.Error(ErrorType.PLAYER, arg);
                    }
                    this.player = player;
                    state = State.OPTIONS;
                }

                case INGREDIENTS -> {
                    return parseIngredient(arg);
                }

            }
            return update(arg);
        }

        private Status parseIngredient(String arg) {
            // user probably meant "ingredient/#" instead of "ingredient #"
            if (BUtil.isInt(arg)) {
                String prevIngredient = prevArg != null ? prevArg : ConfigManager.getConfig(Lang.class).getEntry("CMD_Ingredient");
                return new Status.Error(ErrorType.INVALID_INGREDIENT, arg, prevIngredient);
            }

            BRecipe.IngredientResult result = BRecipe.loadIngredientVerbose(arg);
            if (result instanceof BRecipe.IngredientResult.Error error) {
                return new Status.Error(error.error(), error.invalidPart());
            }
            ingredients.add(((BRecipe.IngredientResult.Success) result).ingredient());

            return update(arg);
        }

        private Status update(String arg) {
            prevArg = arg;
            return new Status.Updated();
        }

        public Status finish() {
            int cookedTime;
            if (options.contains(Option.COOK)) {
                cookedTime = this.cookedTime;
            } else if (recipe != null) {
                cookedTime = recipe.getCookingTime();
            } else {
                return new Status.Error(ErrorType.MISSING_COOK);
            }

            OptionalInt distill;
            if (options.contains(Option.DISTILL)) {
                distill = OptionalInt.of(distillRuns);
            } else if (recipe != null && recipe.needsDistilling()) {
                distill = OptionalInt.of(recipe.getDistillruns());
            } else {
                distill = OptionalInt.empty();
            }

            Age age;
            if (options.contains(Option.AGE)) {
                age = new Age(woodType, ageTime);
            } else if (recipe != null) {
                age = Age.of(recipe);
            } else {
                age = null;
            }

            List<RecipeItem> ingredients = new ArrayList<>();
            if (recipe != null && this.ingredients.isEmpty()) {
                ingredients.addAll(recipe.getIngredients());
            } else if (!this.ingredients.isEmpty()) {
                ingredients.addAll(this.ingredients);
            } else {
                return new Status.Error(ErrorType.MISSING_INGREDIENTS);
            }

            return new Status.Finished(new SimulationParameters(cookedTime, distill, age, ingredients, brewer, player));
        }

        @Nullable
        public List<String> getTabCompletions() {
            return switch (state) {

                case OPTIONS -> {
                    List<String> completions = new ArrayList<>();
                    if (prevArg == null) {
                        completions.addAll(helpStrings);
                    }
                    if (options.contains(Option.RECIPE) || options.contains(Option.COOK)) {
                        completions.addAll(getIngredientCompletions());
                    }
                    completions.addAll(getOptionCompletions());
                    yield completions;
                }

                case RECIPE -> getRecipeCompletions();
                case COOK -> BUtil.numberRange(1, 30);
                case DISTILL -> BUtil.numberRange(1, 10);
                case WOOD -> BarrelWoodType.TAB_COMPLETIONS;
                case AGE -> BUtil.numberRange(1, 50);
                case BREWER, PLAYER -> null;
                case INGREDIENTS -> getIngredientCompletions();

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
            RECIPE(State.RECIPE, "-r", "--recipe"),
            COOK(State.COOK, "-c", "--cook"),
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
            OPTIONS, RECIPE, COOK, DISTILL, WOOD, AGE, BREWER, PLAYER, INGREDIENTS
        }

    }

    private sealed interface Status {
        /** The parser was updated with the latest argument, and parsing should continue */
        record Updated() implements Status {}
        /** Need to display command usage */
        record Help() implements Status {}
        /** Parsing finished, next arguments are ingredients */
        record Finished(SimulationParameters simulation) implements Status {}
        /** User error */
        record Error(Translatable error, Object... args) implements Status {}
    }

    private record SimulationParameters(
        int cookedTime,
        OptionalInt distillRuns,
        @Nullable Age age,
        List<RecipeItem> ingredients,
        @Nullable Player brewer,
        @Nullable Player player
    ) {}
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
    private enum ErrorType implements Translatable {
        INVALID_OPTION("CMD_Invalid_Option"),
        DUPLICATE_OPTION("CMD_Duplicate_Option"),
        RECIPE("Error_NoBrewName"),
        COOK("CMD_Invalid_Cook_Time"),
        DISTILL_RUNS("CMD_Invalid_Distill_Runs"),
        WOOD_TYPE("CMD_Invalid_Wood_Type"),
        AGE_TIME("CMD_Invalid_Age_Time"),
        PLAYER("Error_NoPlayer"),
        /** Takes 2 parameters, [arg, prevArg] */
        INVALID_INGREDIENT("CMD_Invalid_Ingredient"),
        /** Takes 0 parameters */
        MISSING_COOK("CMD_Missing_Cook_Time"),
        /** Takes 0 parameters */
        MISSING_INGREDIENTS("CMD_Missing_Ingredients");

        private final String translationKey;
    }

    private static List<String> getRecipeCompletions() {
        return Stream.concat(
            BCauldronRecipe.getAllRecipes().stream()
                .map(BCauldronRecipe::getName),
            BRecipe.getAllRecipes().stream()
                .mapMulti((recipe, consumer) -> {
                    consumer.accept(recipe.getRecipeName());
                    consumer.accept(recipe.getId());
                })
        ).sorted()
            .distinct()
            .map(BUtil::quote)
            .toList();
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
