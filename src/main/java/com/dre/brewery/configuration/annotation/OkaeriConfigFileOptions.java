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

package com.dre.brewery.configuration.annotation;

import com.dre.brewery.configuration.configurer.BreweryXConfigurer;
import eu.okaeri.configs.configurer.Configurer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface OkaeriConfigFileOptions {

    /**
     * The name of the file to be created
     * @return the name of the file
     */
    String value() default "";

    /**
     * Uses the name of our lang file for this class' bind file.
     * @see com.dre.brewery.configuration.configurer.Translation
     * @see com.dre.brewery.configuration.configurer.TranslationManager
     * @return true if the lang file name should be used
     */
    boolean useLangFileName() default false;

    /**
     * Should the file be updated by Okaeri or read-only
     * @return true if the file should be updated
     */
    boolean update() default true;

    /**
     * Remove orphan keys?
     * @return true if orphan keys should be removed
     */
    boolean removeOrphans() default false;

    /**
     * The configurer to be used for the file
     * @return the configurer class
     */
    Class<? extends Configurer> configurer() default BreweryXConfigurer.class;
}
