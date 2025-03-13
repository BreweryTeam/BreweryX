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

package com.dre.brewery.utility;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class OptionalFloat {
    private static final OptionalFloat EMPTY = new OptionalFloat();
    private final float value;
    private final boolean isPresent;

    private OptionalFloat(float value) {
        this.value = value;
        isPresent = true;
    }
    private OptionalFloat() {
        this.value = 0.0f;
        isPresent = false;
    }

    public static OptionalFloat of(float value) {
        return new OptionalFloat(value);
    }
    public static OptionalFloat empty() {
        return EMPTY;
    }

    public float getAsFloat() {
        if (!isPresent) {
            throw new IllegalStateException("No value present");
        }
        return value;
    }

    public boolean isPresent() {
        return isPresent;
    }
    public boolean isEmpty() {
        return !isPresent;
    }

    public void ifPresent(Consumer<Float> action) {
        if (this.isPresent) {
            action.accept(this.value);
        }
    }

    public void ifPresentOrElse(Consumer<Float> action, Runnable emptyAction) {
        if (this.isPresent) {
            action.accept(this.value);
        } else {
            emptyAction.run();
        }
    }

    public float orElse(float other) {
        return isPresent ? value : other;
    }

    public float orElseGet(Supplier<Float> other) {
        return isPresent ? value : other.get();
    }

    public <X extends Throwable> float orElseThrow(Supplier<X> exceptionSupplier) throws X {
        if (isPresent) {
            return value;
        }
        throw exceptionSupplier.get();
    }

    public String toString() {
        return this.isPresent ? "OptionalFloat[" + this.value + "]" : "OptionalFloat.empty";
    }

}
