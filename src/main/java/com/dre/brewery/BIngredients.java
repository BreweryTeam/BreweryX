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

package com.dre.brewery;

import com.dre.brewery.api.events.brew.BrewModifyEvent;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.lore.Base91DecoderStream;
import com.dre.brewery.lore.Base91EncoderStream;
import com.dre.brewery.lore.BrewLore;
import com.dre.brewery.recipe.BCauldronRecipe;
import com.dre.brewery.recipe.BRecipe;
import com.dre.brewery.recipe.BestRecipeResult;
import com.dre.brewery.recipe.DebuggableItem;
import com.dre.brewery.recipe.Ingredient;
import com.dre.brewery.recipe.ItemLoader;
import com.dre.brewery.recipe.PotionColor;
import com.dre.brewery.recipe.RecipeEvaluation;
import com.dre.brewery.recipe.RecipeItem;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.Logging;
import com.dre.brewery.utility.MinecraftVersion;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Represents ingredients in Cauldron, Brew
 */
@Getter
public class BIngredients {

    private static final MinecraftVersion VERSION = BreweryPlugin.getMCVersion();
    private static final BreweryPlugin plugin = BreweryPlugin.getInstance();
    private static final Config config = ConfigManager.getConfig(Config.class);
    private static final Lang lang = ConfigManager.getConfig(Lang.class);
    private static int lastId = 0; // Legacy

    private int id; // Legacy
    private List<Ingredient> ingredients = new ArrayList<>();
    private int cookedTime;

    /**
     * Init a new BIngredients
     */
    public BIngredients() {
        //this.id = lastId;
        //lastId++;
    }

    /**
     * Load from File
     */
    public BIngredients(List<Ingredient> ingredients, int cookedTime) {
        this.ingredients = ingredients;
        this.cookedTime = cookedTime;
        //this.id = lastId;
        //lastId++;
    }

    /**
     * Load from legacy Brew section
     */
    public BIngredients(List<Ingredient> ingredients, int cookedTime, boolean legacy) {
        this(ingredients, cookedTime);
        if (legacy) {
            this.id = lastId;
            lastId++;
        }
    }

    /**
     * Force add an ingredient to this.
     * <p>Will not check if item is acceptable
     *
     * @param ingredient the item to add
     */
    public void add(ItemStack ingredient) {
        for (Ingredient existing : ingredients) {
            if (existing.matches(ingredient)) {
                existing.setAmount(existing.getAmount() + 1);
                return;
            }
        }

        Ingredient ing = RecipeItem.getMatchingRecipeItem(ingredient, true).toIngredient(ingredient);
        ing.setAmount(1);
        ingredients.add(ing);
    }

    /**
     * Add an ingredient to this with corresponding RecipeItem
     *
     * @param ingredient the item to add
     * @param rItem      the RecipeItem that matches the ingredient
     */
    public void add(ItemStack ingredient, RecipeItem rItem) {
        add(rItem.toIngredient(ingredient));
    }

    /**
     * Add an ingredient to this based on the given RecipeItem
     *
     * @param rItem      the RecipeItem that matches the ingredient
     */
    public void addGeneric(RecipeItem rItem) {
        add(rItem.toIngredientGeneric());
    }

    private void add(Ingredient ingredient) {
        for (Ingredient existing : ingredients) {
            if (existing.isSimilar(ingredient)) {
                existing.setAmount(existing.getAmount() + 1);
                return;
            }
        }
        ingredient.setAmount(1);
        ingredients.add(ingredient);
    }

    /**
     * returns an Potion item with cooked ingredients
     */
    public ItemStack cook(int state, Player brewer) {

        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
        assert potionMeta != null;

        // cookedTime is always time in minutes, state may differ with number of ticks
        cookedTime = state;
        String cookedName = null;
        BRecipe cookRecipe = getCookRecipe();
        Brew brew;

        //int uid = Brew.generateUID();

        if (cookRecipe != null) {
            // Potion is best with cooking only
            int quality = (int) Math.round((getIngredientQuality(cookRecipe) + getCookingQuality(cookRecipe, false)) / 2.0);
            int alc = Math.round(cookRecipe.getAlcohol() * ((float) quality / 10.0f));
            Logging.debugLog("cooked potion has Quality: " + quality + ", Alc: " + alc);
            brew = new Brew(quality, alc, cookRecipe, this);
            BrewLore lore = new BrewLore(brew, potionMeta);
            lore.updateQualityStars(false);
            lore.updateCustomLore();
            lore.updateAlc(false);
            lore.updateBrewer(brewer == null ? null : brewer.getDisplayName());
            lore.addOrReplaceEffects(brew.getEffects(), brew.getQuality());
            lore.write();

            cookedName = cookRecipe.getName(quality);
            cookRecipe.getColor().colorBrew(potionMeta, potion, false);
            brew.updateCustomModelData(potionMeta);

            if (cookRecipe.isGlint()) {
                potionMeta.addEnchant(Enchantment.MENDING, 1, true);
                potionMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        } else {
            // new base potion
            brew = new Brew(this);

            if (state <= 0) {
                cookedName = lang.getEntry("Brew_ThickBrew");
                PotionColor.BLUE.colorBrew(potionMeta, potion, false);
            } else {
                BCauldronRecipe cauldronRecipe = getCauldronRecipe();
                if (cauldronRecipe != null) {
                    Logging.debugLog("Found Cauldron Recipe: " + cauldronRecipe.getName());
                    cookedName = cauldronRecipe.getName();
                    if (cauldronRecipe.getLore() != null) {
                        BrewLore lore = new BrewLore(brew, potionMeta);
                        lore.addCauldronLore(cauldronRecipe.getLore());
                        lore.write();
                    }
                    cauldronRecipe.getColor().colorBrew(potionMeta, potion, true);
                    if (VERSION.isOrLater(MinecraftVersion.V1_14) && cauldronRecipe.getCmData() != 0) {
                        potionMeta.setCustomModelData(cauldronRecipe.getCmData());
                    }
                }
            }
        }
        if (cookedName == null) {
            // if no name could be found
            cookedName = lang.getEntry("Brew_Undefined");
            PotionColor.CYAN.colorBrew(potionMeta, potion, true);
        }

        potionMeta.setDisplayName(BUtil.color("&f" + cookedName));
        //if (!P.use1_14) {
        // Before 1.14 the effects duration would strangely be only a quarter of what we tell it to be
        // This is due to the Duration Modifier, that is removed in 1.14
        //	uid *= 4;
        //}
        // This effect stores the UID in its Duration
        //potionMeta.addCustomEffect((PotionEffectType.REGENERATION).createEffect((uid * 4), 0), true);

        brew.touch();
        BrewModifyEvent modifyEvent = new BrewModifyEvent(brew, potionMeta, BrewModifyEvent.Type.FILL, brewer);
        plugin.getServer().getPluginManager().callEvent(modifyEvent);
        if (modifyEvent.isCancelled()) {
            return null;
        }
        brew.save(potionMeta);
        potion.setItemMeta(potionMeta);
        plugin.getBreweryStats().metricsForCreate(false);

        return potion;
    }

    /**
     * returns amount of ingredients
     */
    public int getIngredientsCount() {
        int count = 0;
        for (Ingredient ing : ingredients) {
            count += ing.getAmount();
        }
        return count;
    }

    public List<Ingredient> getIngredientList() {
        return ingredients;
    }

    /**
     * best recipe for current state of potion, STILL not always returns the correct one...
     */
    public @Nullable BRecipe getBestRecipe(BarrelWoodType wood, float time, boolean distilled) {
        return getBestRecipeFull(wood, time, distilled).getSuccessRecipe();
    }
    /**
     * best recipe for current state of potion, STILL not always returns the correct one...
     */
    public BestRecipeResult getBestRecipeFull(BarrelWoodType wood, float time, boolean distilled) {
        if (BRecipe.getAllRecipes().isEmpty()) {
            return new BestRecipeResult.NoRecipesRegistered();
        }

        // tracks the highest quality recipe using exact numbers, no rounding or clamping
        // if no legacy recipe can be found, this is the plugin's best guess at what the player is trying to make
        BRecipe bestRecipe = null;
        RecipeEvaluation bestEval = null;
        // the original Brewery plugin uses a different algorithm that rounds and clamps ingredient/cook/age/wood
        // qualities before adding them, so we have to do the same here to avoid breaking backward compatibility
        float quality = 0;
        BRecipe bestRecipeLegacy = null;
        RecipeEvaluation bestEvalLegacy = null;

        // FIXME: This should include BCauldronRecipes too. (Proper parent class needed!)
        for (BRecipe recipe : BRecipe.getAllRecipes()) {
            RecipeEvaluation completeRecipeEval;

            RecipeEvaluation ingredientEval = getIngredientQualityFull(recipe);
            int ingredientQuality = ingredientEval.getQuality();

            RecipeEvaluation cookingEval = getCookingQualityFull(recipe, distilled);
            int cookingQuality = cookingEval.getQuality();

            // age and wood quality cannot be fatal, only need to check ingredient and cooking
            boolean isFatal = ingredientEval.hasFatalDefect() || cookingEval.hasFatalDefect();

            if (recipe.needsToAge() || time > 0.5) {
                // needs riping in barrel
                RecipeEvaluation ageEval = getAgeQualityFull(recipe, time);
                int ageQuality = ageEval.getQuality();

                RecipeEvaluation woodEval = getWoodQualityFull(recipe, wood);
                int woodQuality = woodEval.getQuality();

                // is this recipe better than the previous best?
                Logging.debugLog("Ingredient Quality: " + ingredientQuality + " Cooking Quality: " + cookingQuality +
                    " Wood Quality: " + woodQuality + " age Quality: " + ageQuality + " for " + recipe.getName(5));
                completeRecipeEval = RecipeEvaluation.combine(ingredientEval, cookingEval, ageEval, woodEval);

                float averageQuality = ((float) ingredientQuality + cookingQuality + woodQuality + ageQuality) / 4;
                if (!isFatal && averageQuality > quality) {
                    quality = ((float) ingredientQuality + cookingQuality + woodQuality + ageQuality) / 4;
                    bestRecipeLegacy = recipe;
                    bestEvalLegacy = completeRecipeEval;
                }

            } else {
                // calculate quality without age and barrel
                Logging.debugLog("Ingredient Quality: " + ingredientQuality + " Cooking Quality: " + cookingQuality + " for " + recipe.getName(5));
                completeRecipeEval = RecipeEvaluation.combine(ingredientEval, cookingEval);

                float averageQuality = ((float) ingredientQuality + cookingQuality) / 2;
                if (!isFatal && averageQuality > quality) {
                    quality = averageQuality;
                    bestRecipeLegacy = recipe;
                    bestEvalLegacy = completeRecipeEval;
                }
            }

            if (bestEval == null || completeRecipeEval.compareMostToLeastComplexity(bestEval) > 0) {
                bestRecipe = recipe;
                bestEval = completeRecipeEval;
            }
        }

        if (bestRecipeLegacy != null) {
            Logging.debugLog("best recipe: " + bestRecipeLegacy.getName(5) + " has Quality= " + quality);
            return new BestRecipeResult.Found(bestRecipeLegacy, bestEvalLegacy);
        } else {
            // quality is guaranteed to be 0 or less, so constructor will never throw an exception
            return new BestRecipeResult.Error(bestRecipe, bestEval);
        }
    }

    /**
     * returns recipe that is cooking only and matches the ingredients and cooking time
     */
    public @Nullable BRecipe getCookRecipe() {
        return getCookRecipeFull().getSuccessRecipe();
    }
    public BestRecipeResult getCookRecipeFull() {
        BestRecipeResult result = getBestRecipeFull(BarrelWoodType.ANY, 0, false);

        // Check if best recipe is cooking only
        if (result instanceof BestRecipeResult.Found found) {
            if (found.recipe().isCookingOnly()) {
                return result;
            } else {
                RecipeEvaluation eval = found.eval();
                eval.fatal(new BrewDefect.CookTimeMismatch(cookedTime, 0));
                return new BestRecipeResult.Error(found.recipe(), eval);
            }
        }
        return result;
    }

    /**
     * Get Cauldron Recipe that matches the contents of the cauldron
     */
    @Nullable
    public BCauldronRecipe getCauldronRecipe() {
        BCauldronRecipe best = null;
        float bestMatch = 0;
        float match;
        for (BCauldronRecipe recipe : BCauldronRecipe.getAllRecipes()) {
            match = recipe.getIngredientMatch(ingredients);
            if (match >= 10) {
                return recipe;
            }
            if (match > bestMatch) {
                best = recipe;
                bestMatch = match;
            }
        }
        return best;
    }

    /**
     * returns the currently best matching recipe for distilling for the ingredients and cooking time
     */
    public @Nullable BRecipe getDistillRecipe(BarrelWoodType wood, float time) {
        return getDistillRecipeFull(wood, time).getSuccessRecipe();
    }
    public BestRecipeResult getDistillRecipeFull(BarrelWoodType wood, float time) {
        BestRecipeResult result = getBestRecipeFull(wood, time, true);

        // Check if best recipe needs to be distilled
        if (result instanceof BestRecipeResult.Found found) {
            if (found.recipe().needsDistilling()) {
                return result;
            } else {
                RecipeEvaluation eval = found.eval();
                eval.fatal(new BrewDefect.DistillMismatch(true, false));
                return new BestRecipeResult.Error(found.recipe(), eval);
            }
        }
        return result;
    }

    /**
     * returns currently best matching recipe for ingredients, cooking- and ageingtime
     */
    public @Nullable BRecipe getAgeRecipe(BarrelWoodType wood, float time, boolean distilled) {
        return getAgeRecipeFull(wood, time, distilled).getSuccessRecipe();
    }
    public BestRecipeResult getAgeRecipeFull(BarrelWoodType wood, float time, boolean distilled) {
        BestRecipeResult result = getBestRecipeFull(wood, time, distilled);

        if (result instanceof BestRecipeResult.Found found) {
            if (found.recipe().needsToAge()) {
                return result;
            } else {
                RecipeEvaluation eval = found.eval();
                eval.fatal(new BrewDefect.AgeMismatch(time, found.recipe().getAge()));
                return new BestRecipeResult.Error(found.recipe(), eval);
            }
        }
        return result;
    }

    /**
     * returns the quality of the ingredients conditioning given recipe, -1 if no recipe is near them
     */
    public int getIngredientQuality(BRecipe recipe) {
        return getIngredientQualityFull(recipe).getQuality();
    }
    public RecipeEvaluation getIngredientQualityFull(BRecipe recipe) {
        RecipeEvaluation eval = new RecipeEvaluation();

        List<RecipeItem> missingIngredients = recipe.getMissingIngredients(ingredients);
        if (!missingIngredients.isEmpty()) {
            // when ingredients are not complete
            for (RecipeItem missing : missingIngredients) {
                eval.fatal(new BrewDefect.MissingIngredient(missing, missing.getAmount()));
            }
        }

        int badStuff = 0;
        for (Ingredient ingredient : ingredients) {
            int amountInRecipe = recipe.amountOf(ingredient);
            int count = ingredient.getAmount();
            if (amountInRecipe == 0) {
                // this ingredient doesn't belong into the recipe
                badStuff++;
                if (count > (getIngredientsCount() / 2)) {
                    // when more than half of the ingredients don't fit into the recipe
                    eval.fatal(new BrewDefect.WrongIngredient(ingredient));
                } else if (badStuff < ingredients.size()) {
                    // when there are other ingredients
                    float badIngredientDeduction = count * (recipe.getDifficulty() / 2.0f);
                    eval.deduct(new BrewDefect.WrongIngredient(ingredient), badIngredientDeduction);
                } else {
                    // ingredients don't fit at all
                    eval.fatal(new BrewDefect.WrongIngredient(ingredient));
                }
            } else if (count != amountInRecipe) {
                // calculate the quality
                float ingredientCountDeduction = ((float) Math.abs(count - amountInRecipe) / recipe.allowedCountDiff(amountInRecipe)) * 10.0f;
                eval.deduct(new BrewDefect.WrongCount(ingredient, amountInRecipe), ingredientCountDeduction);
            }
        }
        return eval;
    }

    /**
     * returns the quality regarding the cooking-time conditioning given Recipe
     */
    public int getCookingQuality(BRecipe recipe, boolean distilled) {
        return getCookingQualityFull(recipe, distilled).getQuality();
    }
    public RecipeEvaluation getCookingQualityFull(BRecipe recipe, boolean distilled) {
        RecipeEvaluation eval = new RecipeEvaluation();
        if (recipe.needsDistilling() != distilled) {
            eval.fatal(new BrewDefect.DistillMismatch(distilled, recipe.needsDistilling()));
        }

        if (cookedTime < 1) {
            eval.deduct(new BrewDefect.CookTimeMismatch(0, recipe.getCookingTime()), 10);
        } else if (cookedTime != recipe.getCookingTime()) {
            float cookTimeDeduction = ((float) Math.abs(cookedTime - recipe.getCookingTime()) / recipe.allowedTimeDiff(recipe.getCookingTime())) * 10.0f;
            eval.deduct(new BrewDefect.CookTimeMismatch(cookedTime, recipe.getCookingTime()), cookTimeDeduction);
        }
        return eval;
    }

    /**
     * returns pseudo quality of distilling. 0 if doesn't match the need of the recipes distilling
     */
    public int getDistillQuality(BRecipe recipe, byte distillRuns) {
        if (recipe.needsDistilling() != distillRuns > 0) {
            return 0;
        }
        return 10 - Math.abs(recipe.getDistillruns() - distillRuns);
    }

    /**
     * returns the quality regarding the barrel wood conditioning given Recipe
     */
    public int getWoodQuality(BRecipe recipe, BarrelWoodType wood) {
        return getWoodQualityFull(recipe, wood).getQuality();
    }

    public RecipeEvaluation getWoodQualityFull(BRecipe recipe, BarrelWoodType wood) {
        RecipeEvaluation eval = new RecipeEvaluation();
        if (recipe.usesAnyWood()) {
            // type of wood doesnt matter
            return eval;
        }

        if (wood != recipe.getWood()) {
            float woodDeduction;
            if (config.isNewBarrelTypeAlgorithm()) {
                woodDeduction = getWoodQualityNew(recipe, wood);
            } else {
                woodDeduction = recipe.getWoodDiff(wood.getIndex()) * recipe.getDifficulty();
            }
            eval.deduct(new BrewDefect.WrongWood(wood, recipe.getWood()), woodDeduction);
        }
        return eval;
    }
    // At difficulty 1, distances 0-5 have quality 10, 10, 9, 8, 7, 6
    // At difficulty 5, distances 0-5 have quality 10, 8, 4, 1, 0, 0
    // At difficulty 10, distances 0-5 have quality 10, 5, 0, 0, 0, 0
    // See: https://www.desmos.com/calculator/aaoixs2qo7
    private int getWoodQualityNew(BRecipe recipe, BarrelWoodType wood) {
        BarrelWoodType recipeWood = recipe.getWood();
        float baseQuality = switch (recipeWood.getDistance(wood)) {
            case 0 -> 10.0f;
            case 1 -> 9.0f;
            case 2 -> 7.75f;
            case 3 -> 6.25f;
            case 4 -> 4.5f;
            case 5 -> 2.5f;
            default -> 0.0f;
        };
        if (baseQuality == 0.0f) {
            return 0;
        }
        float quality = 10f - (10f - baseQuality) * 0.5f * recipe.getDifficulty();
        return Math.max(Math.round(quality), 0);
    }

    /**
     * returns the quality regarding the ageing time conditioning given Recipe
     */
    public int getAgeQuality(BRecipe recipe, float time) {
        return getAgeQualityFull(recipe, time).getQuality();
    }
    public RecipeEvaluation getAgeQualityFull(BRecipe recipe, float time) {
        RecipeEvaluation eval = new RecipeEvaluation();
        if (!BUtil.isClose(time, recipe.getAge())) {
            float ageDeduction = Math.abs(time - recipe.getAge()) * ((float) recipe.getDifficulty() / 2);
            eval.deduct(new BrewDefect.AgeMismatch(time, recipe.getAge()), ageDeduction);
        }
        return eval;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BIngredients)) return false;
        BIngredients other = ((BIngredients) obj);
        return cookedTime == other.cookedTime &&
            ingredients.equals(other.ingredients);
    }

    // Creates a copy ingredients
    public BIngredients copy() {
        BIngredients copy = new BIngredients();
        copy.ingredients.addAll(ingredients);
        copy.cookedTime = cookedTime;
        return copy;
    }

    @Override
    public String toString() {
        String ingredientsStr = ingredients.stream()
            .map(DebuggableItem::debug)
            .collect(Collectors.joining(", ", "[", "]"));
        return new StringJoiner(", ", "BIngredients{", "}")
            .add("cookedTime=" + cookedTime)
            .add("ingredients=" + ingredientsStr)
            .toString();
    }

	/*public void testStore(DataOutputStream out) throws IOException {
		out.writeInt(cookedTime);
		out.writeByte(ingredients.size());
		for (ItemStack item : ingredients) {
			out.writeUTF(item.getType().name());
			out.writeShort(item.getDurability());
			out.writeShort(item.getAmount());
		}
	}

	public void testLoad(DataInputStream in) throws IOException {
		if (in.readInt() != cookedTime) {
			P.p.log("cookedtime wrong");
		}
		if (in.readUnsignedByte() != ingredients.size()) {
			P.p.log("size wrong");
			return;
		}
		for (ItemStack item : ingredients) {
			if (!in.readUTF().equals(item.getType().name())) {
				P.p.log("name wrong");
			}
			if (in.readShort() != item.getDurability()) {
				P.p.log("dur wrong");
			}
			if (in.readShort() != item.getAmount()) {
				P.p.log("amount wrong");
			}
		}
	}*/

    public void save(DataOutputStream out) throws IOException {
        out.writeInt(cookedTime);
        out.writeByte(ingredients.size());
        for (Ingredient ing : ingredients) {
            ing.saveTo(out);
            out.writeShort(Math.min(ing.getAmount(), Short.MAX_VALUE));
        }
    }

    public static BIngredients load(DataInputStream in, short dataVersion) throws IOException {
        int cookedTime = in.readInt();
        byte size = in.readByte();
        List<Ingredient> ing = new ArrayList<>(size);
        for (; size > 0; size--) {
            ItemLoader itemLoader = new ItemLoader(dataVersion, in, in.readUTF());
            if (!plugin.getIngredientLoaders().containsKey(itemLoader.getSaveID())) {
                Logging.errorLog("Ingredient Loader not found: " + itemLoader.getSaveID());
                break;
            }
            Ingredient loaded = plugin.getIngredientLoaders().get(itemLoader.getSaveID()).apply(itemLoader);
            int amount = in.readShort();
            if (loaded != null) {
                loaded.setAmount(amount);
                ing.add(loaded);
            }
        }
        return new BIngredients(ing, cookedTime);
    }

    // saves data into main Ingredient section. Returns the save id
    // Only needed for legacy potions
    public int saveLegacy(ConfigurationSection config) {
        String path = "Ingredients." + id;
        if (cookedTime != 0) {
            config.set(path + ".cookedTime", cookedTime);
        }
        config.set(path + ".mats", serializeIngredients());
        return id;
    }


    // Serialize Ingredients to String for storing in yml, ie for Cauldrons
    public String serializeIngredients() {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(new Base91EncoderStream(byteStream))) {
            out.writeByte(Brew.SAVE_VER);
            save(out);
        } catch (IOException e) {
            Logging.errorLog("Failed to serialize Ingredients", e);
            return "";
        }
        return byteStream.toString();
    }


    public static BIngredients deserializeIngredients(String mat) {
        try (DataInputStream in = new DataInputStream(new Base91DecoderStream(new ByteArrayInputStream(mat.getBytes())))) {
            byte ver = in.readByte();
            return BIngredients.load(in, ver);
        } catch (IOException e) {
            Logging.errorLog("Failed to deserialize Ingredients", e);
            return new BIngredients();
        }
    }

}
