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
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class BukkitConstants {

    private BukkitConstants() {}


    // More constants can be added as required, these are just the ones Brewery currently uses
    public static PotionEffectType HUNGER = potionEffectType("hunger");
    public static PotionEffectType NAUSEA = potionEffectType("nausea");
    public static PotionEffectType BLINDNESS = potionEffectType("blindness");
    public static PotionEffectType SLOWNESS = potionEffectType("slowness");
    public static PotionEffectType REGENERATION = potionEffectType("regeneration");
    public static PotionEffectType POISON = potionEffectType("poison");
    public static PotionEffectType WEAKNESS = potionEffectType("weakness");
    public static PotionEffectType FIRE_RESISTANCE = potionEffectType("fire_resistance");
    public static PotionEffectType INSTANT_HEALTH = potionEffectType("instant_health");
    public static PotionEffectType INSTANT_DAMAGE = potionEffectType("instant_damage");
    public static PotionEffectType WATER_BREATHING = potionEffectType("water_breathing");
    public static PotionEffectType NIGHT_VISION = potionEffectType("night_vision");
    public static PotionEffectType SPEED = potionEffectType("speed");
    public static PotionEffectType HASTE = potionEffectType("haste");

    public static Particle INSTANT_EFFECT = particle("instant_effect");
    public static Particle SPLASH = particle("splash");
    public static Particle ENTITY_EFFECT = particle("entity_effect");
    public static Particle LARGE_SMOKE = particle("large_smoke");
    public static Particle DUST = particle("dust");

    public static PotionType POTION_REGENERATION = potionType("regeneration");
    public static PotionType POTION_SWIFTNESS = potionType("swiftness");
    public static PotionType POTION_FIRE_RESISTANCE = potionType("fire_resistance");
    public static PotionType POTION_POISON = potionType("poison");
    public static PotionType POTION_HEALING = potionType("healing");
    public static PotionType POTION_NIGHT_VISION = potionType("night_vision");
    public static PotionType POTION_WEAKNESS = potionType("weakness");
    public static PotionType POTION_STRENGTH = potionType("strength");
    public static PotionType POTION_SLOWNESS = potionType("slowness");
    public static PotionType POTION_WATER_BREATHING = potionType("water_breathing");
    public static PotionType POTION_HARMING = potionType("harming");
    public static PotionType POTION_INVISIBILITY = potionType("invisibility");

    public static Material SHORT_GRASS = renamedMaterial("grass", new Tuple<>(MinecraftVersion.V1_20_4, "short_grass"));


    public static Particle particle(String key) {
        return Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft(key));
    }

    public static PotionEffectType potionEffectType(String key) {
        if (BreweryPlugin.getMCVersion().isOrLater(MinecraftVersion.V1_21)) {
            return getOrThrow(Registry.EFFECT, key);
        }
        return throwIfNull(key, k -> PotionEffectType.getByKey(NamespacedKey.minecraft(k)));
    }

    public static PotionType potionType(String key) {
        return getOrThrow(Registry.POTION, key);
    }

    @Nullable
    public static PotionEffectType nullablePotionEffectType(String key) {
        if (BreweryPlugin.getMCVersion().isOrLater(MinecraftVersion.V1_21)) {
            return Registry.EFFECT.get(NamespacedKey.minecraft(key));
        }
        return PotionEffectType.getByKey(NamespacedKey.minecraft(key));
    }


    public static Material renamedMaterial(String defaultValue, Tuple<MinecraftVersion, String>... names) {
        MinecraftVersion activeVersion = BreweryPlugin.getMCVersion();
        for (Tuple<MinecraftVersion, String> pair : names) {
            MinecraftVersion version = pair.a();
            String name = pair.b();
            if (activeVersion.isOrLater(version)) {
                return Material.getMaterial(name.toUpperCase());
            }
        }
        return Material.getMaterial(defaultValue.toUpperCase());
    }

    private static <T extends Keyed> T getOrThrow(Registry<T> registry, String key) {
        T value = registry.get(NamespacedKey.minecraft(key));
        if (value == null) {
            throw new IllegalArgumentException("No value found in registry for key: " + key);
        }
        return value;
    }

    private static <T> T throwIfNull(String key, KeyLookup<T> function) {
        T value = function.throwIfNull(key);
        if (value == null) {
            throw new IllegalArgumentException("No value found for key: " + key);
        }
        return value;
    }

    private interface KeyLookup<T> {
        T throwIfNull(String key);
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

    // I don't really like this but whatever

    private static final Map<String, Keyed> MAPPED_VALUES = new HashMap<>();

    static {
        try {
            for (Field field : BukkitConstants.class.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers()) || !Keyed.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                Keyed obj = (Keyed) field.get(null);
                MAPPED_VALUES.put(obj.getKey().getKey(), obj);
            }
        } catch (IllegalAccessException e) {
            Logging.errorLog("BukkitConstants failed to initialize mapped values", e);
        }
    }

    @Nullable
    public static <T extends Keyed> T getMappedValue(String key, Class<T> type) {
        Keyed keyed = MAPPED_VALUES.get(key);
        if (keyed == null) {
            return null;
        }
        if (!type.isAssignableFrom(keyed.getClass())) {
            return null;
        }
        return type.cast(keyed);
    }


    public static <T extends Keyed> Collection<T> getMappedValues(Class<T> type) {
        List<T> list = new ArrayList<>();
        for (Keyed keyed : MAPPED_VALUES.values()) {
            if (type.isAssignableFrom(keyed.getClass())) {
                list.add(type.cast(keyed));
            }
        }
        return list;
    }

    @Nullable
    public static Keyed getMappedValue(String key) {
        return MAPPED_VALUES.get(key);
    }

    public static Collection<Keyed> getMappedValues() {
        return MAPPED_VALUES.values();
    }
}
