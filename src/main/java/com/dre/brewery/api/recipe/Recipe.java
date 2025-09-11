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

package com.dre.brewery.api.recipe;

import com.dre.brewery.BarrelWoodType;
import com.dre.brewery.recipe.PotionColor;
import com.dre.brewery.utility.Tuple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Recipe {

    @NotNull String getId();

    @NotNull List<RecipeIngredient> getRecipeIngredients();

    int getDifficulty();

    void setDifficulty(int difficulty);

    int getCookingTime();

    void setCookingTime(int cookingTime);

    byte getDistillRuns();

    void setDistillRuns(byte distillRuns);

    int getDistillTime();

    void setBarrelTypes(@NotNull List<BarrelWoodType> barrelTypes);

    @NotNull List<BarrelWoodType> getBarrelTypes();

    int getAge();

    void setAge(int age);

    @NotNull PotionColor getColor();

    void setColor(@NotNull PotionColor color);

    int getAlcohol();

    void setAlcohol(int alcohol);

    @NotNull List<Tuple<Integer, String>> getLore();

    void setLore(@NotNull List<Tuple<Integer, String>> lore);

    @NotNull List<RecipeEffect> getRecipeEffects();

    void setRecipeEffects(@NotNull List<RecipeEffect> recipeEffects);

    int[] getCustomModelData();

    void setCustomModelData(int[] customModelData);

    @Nullable List<Tuple<Integer, String>> getPlayerCommands();

    void setPlayerCommands(List<Tuple<Integer, String>> playerCommands);

    @Nullable List<Tuple<Integer, String>> getServerCommands();

    void setServerCommands(List<Tuple<Integer, String>> serverCommands);
}
