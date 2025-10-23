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

import com.dre.brewery.BreweryPlugin;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BukkitConstants {

    private BukkitConstants() {}


    // More constants can be added as required, these are just the ones Brewery currently uses
    public static PotionEffectType HUNGER = potionEffectType("hunger");
    public static PotionEffectType NAUSEA = potionEffectType("nausea");
    public static PotionEffectType BLINDNESS = potionEffectType("blindness");
    public static PotionEffectType SLOWNESS = potionEffectType("slowness");
    public static PotionEffectType REGENERATION = potionEffectType("regeneration");
    public static PotionEffectType POISON = potionEffectType("poison");

    public static Particle INSTANT_EFFECT = particle("instant_effect");
    public static Particle SPLASH = particle("splash");
    public static Particle ENTITY_EFFECT = particle("entity_effect");
    public static Particle LARGE_SMOKE = particle("large_smoke");
    public static Particle DUST = particle("dust");


    public static Particle particle(String key) {
        return Registry.PARTICLE_TYPE.getOrThrow(NamespacedKey.minecraft(key));
    }

    public static PotionEffectType potionEffectType(String key) {
        return Registry.EFFECT.getOrThrow(NamespacedKey.minecraft(key));
    }

    @Nullable
    public static PotionEffectType nullablePotionEffectType(String key) {
        return Registry.EFFECT.get(NamespacedKey.minecraft(key));
    }


    @Getter
    @AllArgsConstructor
    public static abstract class BukkitConstantWrapper {
        protected static final MinecraftVersion SERVER_VERSION = BreweryPlugin.getMCVersion();
        protected final MinecraftVersion requiredVersion;
    }

    public static class ParticleSpellWrapper extends BukkitConstantWrapper {

        public ParticleSpellWrapper() {
            super(MinecraftVersion.V1_21_10);
        }

        @SuppressWarnings("UnstableApiUsage")
        @Nullable
        public Object toInstance(@NotNull Color color, float size) {
            if (!SERVER_VERSION.isOrLater(this.requiredVersion)) {
                return null;
            }

            return new Particle.Spell(color, size);
        }
    }
}
