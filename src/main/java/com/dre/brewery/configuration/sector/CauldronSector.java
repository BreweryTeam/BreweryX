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

package com.dre.brewery.configuration.sector;

import com.dre.brewery.configuration.sector.capsule.ConfigCauldronIngredient;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class CauldronSector extends AbstractOkaeriConfigSector<ConfigCauldronIngredient> {


    @Comment("Example Cauldron Ingredient with every possible entry first:") // Comments not supported here anyway :(
    ConfigCauldronIngredient ex = ConfigCauldronIngredient.builder()
            .name("Example")
            .ingredients(List.of("Bedrock/2", "Diamond"))
            .color("BLACK")
            .cookParticles(List.of("RED/5", "WHITE/10", "800000/25"))
            .lore(List.of("An example for a Base Potion", "This is how it comes out of a Cauldron"))
            .customModelData(545)
            .build();

    @Comment("One Ingredient:")
    ConfigCauldronIngredient wheat = ConfigCauldronIngredient.builder()
            .name("Fermented wheat")
            .ingredients(List.of("Wheat"))
            .cookParticles(List.of("2d8686/8"))
            .build();

	ConfigCauldronIngredient sugarcane = ConfigCauldronIngredient.builder()
			.name("Sugar brew")
			.ingredients(List.of("Sugar_Cane"))
			.color("f1ffad")
			.cookParticles(List.of("f1ffad/4", "858547/10"))
			.build();

	ConfigCauldronIngredient sugar = ConfigCauldronIngredient.builder()
			.name("Sugarwater")
			.ingredients(List.of("Sugar"))
			.cookParticles(List.of("WHITE/4", "BRIGHT_GREY/25"))
			.build();

	ConfigCauldronIngredient apple = ConfigCauldronIngredient.builder()
			.name("Apple must")
			.ingredients(List.of("Apple"))
			.build();

	ConfigCauldronIngredient berries = ConfigCauldronIngredient.builder()
			.name("Grape must")
			.ingredients(List.of("Sweet_Berries"))
			.color("RED")
			.cookParticles(List.of("ff6666/2", "RED/7", "ac6553/13"))
			.build();

	ConfigCauldronIngredient potato = ConfigCauldronIngredient.builder()
			.name("Potatomash")
			.ingredients(List.of("Potato"))
			.build();

	ConfigCauldronIngredient grass = ConfigCauldronIngredient.builder()
			.name("Boiled herbs")
			.ingredients(List.of("Grass"))
			.color("99ff66")
			.cookParticles(List.of("GREEN/2", "99ff99/20"))
			.build();

	ConfigCauldronIngredient rmushroom = ConfigCauldronIngredient.builder()
			.name("Mushroom brew")
			.ingredients(List.of("Red_Mushroom"))
			.color("ff5c33")
			.cookParticles(List.of("fab09e/15"))
			.build();

	ConfigCauldronIngredient bmushroom = ConfigCauldronIngredient.builder()
			.name("Mushroom brew")
			.ingredients(List.of("Brown_Mushroom"))
			.color("c68c53")
			.cookParticles(List.of("c68c53/15"))
			.build();

	ConfigCauldronIngredient cocoa = ConfigCauldronIngredient.builder()
			.name("Chocolately brew")
			.ingredients(List.of("Cocoa_Beans"))
			.color("804600")
			.cookParticles(List.of("a26011/1", "5c370a/3", "4d4133/8"))
			.build();

	ConfigCauldronIngredient milk = ConfigCauldronIngredient.builder()
			.name("Milky water")
			.ingredients(List.of("Milk_Bucket"))
			.color("BRIGHT_GREY")
			.cookParticles(List.of("fbfbd0/1", "WHITE/6"))
			.build();

	ConfigCauldronIngredient bl_flow = ConfigCauldronIngredient.builder()
			.name("Blueish brew")
			.ingredients(List.of("blue-flowers"))
			.color("0099ff")
			.cookParticles(List.of("0099ff"))
			.build();

	ConfigCauldronIngredient cactus = ConfigCauldronIngredient.builder()
			.name("Agave brew")
			.ingredients(List.of("cactus"))
			.color("00b300")
			.cookParticles(List.of("00b300/16"))
			.build();

	ConfigCauldronIngredient poi_potato = ConfigCauldronIngredient.builder()
			.name("Poisonous Broth")
			.ingredients(List.of("Poisonous_Potato"))
			.build();

	ConfigCauldronIngredient egg = ConfigCauldronIngredient.builder()
			.name("Sticky brew")
			.ingredients(List.of("Egg"))
			.build();

	ConfigCauldronIngredient oak_sapling = ConfigCauldronIngredient.builder()
			.name("Stringy herb broth")
			.ingredients(List.of("Oak_Sapling"))
			.build();

	ConfigCauldronIngredient vine = ConfigCauldronIngredient.builder()
			.name("Boiled herbs")
			.ingredients(List.of("vine"))
			.color("99ff66")
			.cookParticles(List.of("GREEN/2", "99ff99/20"))
			.build();

	ConfigCauldronIngredient rot_flesh = ConfigCauldronIngredient.builder()
			.name("Foul pest")
			.ingredients(List.of("Rotten_Flesh"))
			.color("263300")
			.cookParticles(List.of("263300/8", "BLACK/20"))
			.build();

	ConfigCauldronIngredient melon = ConfigCauldronIngredient.builder()
			.name("Melon juice")
			.ingredients(List.of("melon_slice"))
			.build();

	ConfigCauldronIngredient wheat_seeds = ConfigCauldronIngredient.builder()
			.name("Bitter brew")
			.ingredients(List.of("Wheat_Seeds"))
			.build();

	ConfigCauldronIngredient melon_seeds = ConfigCauldronIngredient.builder()
			.name("Bitter brew")
			.ingredients(List.of("Melon_Seeds"))
			.build();

	ConfigCauldronIngredient pumpkin_seeds = ConfigCauldronIngredient.builder()
			.name("Bitter brew")
			.ingredients(List.of("Pumpkin_Seeds"))
			.build();

	ConfigCauldronIngredient bone_meal = ConfigCauldronIngredient.builder()
			.name("Bony Brew")
			.ingredients(List.of("bone_meal"))
			.color("BRIGHT_GREY")
			.build();

	ConfigCauldronIngredient cookie = ConfigCauldronIngredient.builder()
			.name("Chocolately sap")
			.ingredients(List.of("Cookie"))
			.color("804600")
			.cookParticles(List.of("a26011/1", "5c370a/3", "4d4133/8"))
			.build();

	ConfigCauldronIngredient fer_spid_eye = ConfigCauldronIngredient.builder()
			.name("Fermented Eye")
			.ingredients(List.of("Fermented_Spider_Eye"))
			.build();

	ConfigCauldronIngredient ghast_tear = ConfigCauldronIngredient.builder()
			.name("Sad brew")
			.ingredients(List.of("ghast_tear"))
			.build();

	ConfigCauldronIngredient snowball = ConfigCauldronIngredient.builder()
			.name("Icewater")
			.ingredients(List.of("Snowball"))
			.build();

	ConfigCauldronIngredient Gold_Nugget = ConfigCauldronIngredient.builder()
			.name("Glistering brew")
			.ingredients(List.of("Gold_Nugget"))
			.color("ffd11a")
			.cookParticles(List.of("ffd11a"))
			.build();

	ConfigCauldronIngredient glowstone_dust = ConfigCauldronIngredient.builder()
			.name("Glowing brew")
			.ingredients(List.of("Glowstone_Dust"))
			.color("ffff33")
			.cookParticles(List.of("ffff99/3", "d9d926/15"))
			.build();

	ConfigCauldronIngredient applemead_base = ConfigCauldronIngredient.builder()
			.name("Apple-Sugar brew")
			.ingredients(List.of("Sugar_Cane/3", "Apple"))
			.color("e1ff4d")
			.cookParticles(List.of("e1ff4d/4"))
			.build();

	ConfigCauldronIngredient poi_grass = ConfigCauldronIngredient.builder()
			.name("Boiled acidy herbs")
			.ingredients(List.of("Grass", "Poisonous_Potato"))
			.color("99ff66")
			.cookParticles(List.of("GREEN/2", "99ff99/20"))
			.build();

	ConfigCauldronIngredient juniper = ConfigCauldronIngredient.builder()
			.name("Juniper brew")
			.ingredients(List.of("blue-flowers", "wheat"))
			.color("00ccff")
			.cookParticles(List.of("00ccff/8"))
			.build();

	ConfigCauldronIngredient gin_base = ConfigCauldronIngredient.builder()
			.name("Fruity juniper brew")
			.ingredients(List.of("blue-flowers", "wheat", "apple"))
			.color("66e0ff")
			.cookParticles(List.of("00ccff/5"))
			.build();

	ConfigCauldronIngredient eggnog_base = ConfigCauldronIngredient.builder()
			.name("Smooth egg mixture")
			.ingredients(List.of("egg", "sugar", "milk_bucket"))
			.color("ffecb3")
			.cookParticles(List.of("ffecb3/2"))
			.build();
}
