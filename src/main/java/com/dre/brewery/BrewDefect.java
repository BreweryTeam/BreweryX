/*
 * BreweryX Bukkit-Plugin for an alternate brewing process
 * Copyright (C) 2025 The Brewery Team
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

import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.recipe.DebuggableItem;
import com.dre.brewery.recipe.Ingredient;
import com.dre.brewery.utility.BUtil;

import java.util.List;
import java.util.Locale;

/**
 * Represents an imperfection with a brew given the recipe the player is attempting to make.
 */
public interface BrewDefect {

    List<String> getMessages(Lang lang);

    record WrongIngredient(DebuggableItem ingredient) implements BrewDefect {
        @Override
        public List<String> getMessages(Lang lang) {
            return lang.getEntries("Defect_WrongIngredient");
        }

        @Override
        public String toString() {
            return String.format("WrongIngredient{%s}", ingredient.getDebugID());
        }
    }

    record MissingIngredient(DebuggableItem ingredient, int amountNeeded) implements BrewDefect {
        @Override
        public List<String> getMessages(Lang lang) {
            return lang.getEntries("Defect_MissingIngredient");
        }

        @Override
        public String toString() {
            return String.format("MissingIngredient{%dx %s}", amountNeeded, ingredient.getDebugID());
        }
    }

    record WrongCount(Ingredient ingredient, int amountNeeded) implements BrewDefect {
        public WrongCount {
            if (ingredient.getAmount() == amountNeeded) {
                throw new IllegalArgumentException("WrongCount actual and needed counts were equal");
            }
        }

        @Override
        public List<String> getMessages(Lang lang) {
            if (ingredient.getAmount() < amountNeeded) {
                return lang.getEntries("Defect_LowCount");
            } else {
                return lang.getEntries("Defect_HighCount");
            }
        }

        @Override
        public String toString() {
            return String.format("WrongCount{%d/%d %s}", ingredient.getAmount(), amountNeeded, ingredient.getDebugID());
        }
    }

    record DistillMismatch(boolean actual, boolean needed) implements BrewDefect {
        public DistillMismatch {
            if (actual == needed) {
                throw new IllegalArgumentException("DistillMismatch actual and needed were equal");
            }
        }

        @Override
        public List<String> getMessages(Lang lang) {
            if (needed) {
                return lang.getEntries("Defect_NeedsDistill");
            } else {
                return lang.getEntries("Defect_BadDistill");
            }
        }

        @Override
        public String toString() {
            return needed ? "DistillNeeded" : "DistillUnnecessary";
        }
    }

    record CookTimeMismatch(int actual, int needed) implements BrewDefect {
        public CookTimeMismatch {
            if (actual == needed) {
                throw new IllegalArgumentException("CookTimeMismatch actual and needed were equal");
            }
        }

        @Override
        public List<String> getMessages(Lang lang) {
            if (actual < needed) {
                return lang.getEntries("Defect_Uncooked");
            } else {
                return lang.getEntries("Defect_Overcooked");
            }
        }

        @Override
        public String toString() {
            return String.format("CookTimeMismatch{%d/%d}", actual, needed);
        }
    }

    record AgeMismatch(float actual, float needed) implements BrewDefect {
        public AgeMismatch {
            if (BUtil.isClose(actual, needed)) {
                throw new IllegalArgumentException("AgeMismatch actual and needed were equal");
            }
        }

        @Override
        public List<String> getMessages(Lang lang) {
            if (needed == 0.0f) {
                return lang.getEntries("Defect_BadAged");
            } else if (actual < needed) {
                return lang.getEntries("Defect_UnderAged");
            } else {
                return lang.getEntries("Defect_OverAged");
            }
        }

        @Override
        public String toString() {
            return String.format("AgeMismatch{%.3f/%.3f}", actual, needed);
        }
    }

    record WrongWood(BarrelWoodType actual, BarrelWoodType needed) implements BrewDefect {
        public WrongWood {
            if (actual == needed) {
                throw new IllegalArgumentException("WrongWood actual and needed were equal");
            }
        }

        @Override
        public List<String> getMessages(Lang lang) {
            return lang.getEntries("Defect_WrongWood", actual.getFormattedName().toLowerCase(Locale.ROOT));
        }

        @Override
        public String toString() {
            return String.format("WrongWood{was %s, needs %s}", actual.getFormattedName(), needed.getFormattedName());
        }
    }

    record NoRecipesRegistered() implements BrewDefect {
        @Override
        public List<String> getMessages(Lang lang) {
            return lang.getEntries("Defect_NoRecipesRegistered");
        }

        @Override
        public String toString() {
            return "NoRecipesRegistered{}";
        }
    }

}
