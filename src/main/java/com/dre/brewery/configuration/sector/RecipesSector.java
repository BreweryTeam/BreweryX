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

import com.dre.brewery.configuration.sector.capsule.ConfigRecipe;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class RecipesSector extends AbstractOkaeriConfigSector<ConfigRecipe> {

    // Comments not supported here

    ConfigRecipe ex = ConfigRecipe.builder()
            .name("Diamond/1/Example/Good Example")
            .ingredients(List.of("Bedrock/2", "Spruce_Planks/8", "Bedrock/1", "Brewery:Wheatbeer/2", "ExoticGarden:Grape/3", "ex-item/4"))
            .cookingTime(3)
            .distillRuns(2)
            .distillTime(60)
            .wood(4)
            .age(11)
            .color("DARK_RED")
            .difficulty(3)
            .alcohol(14)
            .lore(List.of("This is an example brew", "++Just a normal Example", "This text would be on the brew", "+ Smells disgusting", "++ Smells alright", "+++ Smells really good"))
            .serverCommands(List.of("+++ weather clear", "+ weather rain"))
            .playerCommands(List.of("homes"))
            .drinkMessage("Tastes good")
            .drinkTitle("Warms you from inside")
            .glint(true)
            .customModelData("556/557/557")
            .effects(List.of("FIRE_RESISTANCE/20", "HEAL/1", "WEAKNESS/2-3/50-60", "POISON/1-0/20-0"))
            .build();

    ConfigRecipe wheatbeer = ConfigRecipe.builder()
            .name("Skunky Wheatbeer/Wheatbeer/Fine Wheatbeer")
            .ingredients(List.of("Wheat/3"))
            .cookingTime(8)
            .distillRuns(0)
            .wood(1)
            .age(2)
            .color("ffb84d")
            .difficulty(1)
            .alcohol(5)
            .lore(List.of("+++ &8Refreshing"))
            .build();

	ConfigRecipe beer = ConfigRecipe.builder()
			.name("Skunky Beer/Beer/Fine Beer")
			.ingredients(List.of("Wheat/6"))
			.cookingTime(8)
			.distillRuns(0)
			.wood(0)
			.age(3)
			.color("ffd333")
			.difficulty(1)
			.alcohol(6)
			.lore(List.of("+++ &8Crisp taste"))
			.build();

	ConfigRecipe darkbeer = ConfigRecipe.builder()
			.name("Skunky Darkbeer/Darkbeer/Fine Darkbeer")
			.ingredients(List.of("Wheat/6"))
			.cookingTime(8)
			.distillRuns(3)
			.wood(6)
			.age(0)
			.color("650013")
			.difficulty(2)
			.alcohol(7)
			.lore(List.of("+++ &8Roasted taste"))
			.build();

	ConfigRecipe wine = ConfigRecipe.builder()
			.name("Red Wine")
			.ingredients(List.of("Sweet_Berries/5"))
			.cookingTime(5)
			.distillRuns(0)
			.wood(0)
			.age(20)
			.color("RED")
			.difficulty(4)
			.alcohol(8)
			.lore(List.of("+ &8Harsh", "+ &8Corked", "++ &8Mellow", "+++ &8Full-Bodied"))
			.build();

	ConfigRecipe mead = ConfigRecipe.builder()
			.name("Awkward Mead/Mead/&6Golden Mead")
			.ingredients(List.of("Sugar_Cane/6"))
			.cookingTime(3)
			.distillRuns(0)
			.wood(2)
			.age(4)
			.color("ORANGE")
			.difficulty(2)
			.alcohol(9)
			.lore(List.of("+++ Has a golden shine"))
			.build();

	ConfigRecipe ap_mead = ConfigRecipe.builder()
			.name("Apple Mead/Sweet Apple Mead/&6Sweet Golden Apple Mead")
			.ingredients(List.of("Sugar_Cane/6", "Apple/2"))
			.cookingTime(4)
			.distillRuns(0)
			.wood(2)
			.age(4)
			.color("ORANGE")
			.difficulty(4)
			.alcohol(11)
			.lore(List.of("Is there any Apple in this?", "Refreshing taste of Apple", "Sweetest hint of Apple"))
			.effects(List.of("WATER_BREATHING/1-2/150"))
			.build();

	ConfigRecipe cidre = ConfigRecipe.builder()
			.name("Poor Cidre/Apple Cider/Great Apple Cider")
			.ingredients(List.of("Apple/14"))
			.cookingTime(7)
			.distillRuns(0)
			.wood(0)
			.age(3)
			.color("f86820")
			.difficulty(4)
			.alcohol(7)
			.build();

	ConfigRecipe apple_liquor = ConfigRecipe.builder()
			.name("Sour Apple Liquor/Apple Liquor/Calvados")
			.ingredients(List.of("Apple/12", "Diamond/300", "Barrel/20", "bedrock/2", "egg/3"))
			.cookingTime(16)
			.distillRuns(3)
			.wood(5)
			.age(6)
			.color("BRIGHT_RED")
			.difficulty(5)
			.alcohol(14)
			.lore(List.of("+Sour like Acid", "+++ Good Apple Liquor"))
			.build();

	ConfigRecipe whiskey = ConfigRecipe.builder()
			.name("Unsightly Whiskey/Whiskey/Scotch Whiskey")
			.ingredients(List.of("Wheat/10"))
			.cookingTime(10)
			.distillRuns(2)
			.distillTime(50)
			.wood(4)
			.age(18)
			.color("ORANGE")
			.difficulty(7)
			.alcohol(26)
			.lore(List.of("&7Single Malt"))
			.build();

	ConfigRecipe rum = ConfigRecipe.builder()
			.name("Bitter Rum/Spicy Rum/&6Golden Rum")
			.ingredients(List.of("Sugar_Cane/18"))
			.cookingTime(6)
			.distillRuns(2)
			.distillTime(30)
			.wood(2)
			.age(14)
			.color("DARK_RED")
			.difficulty(6)
			.alcohol(30)
			.lore(List.of("+ &8Too bitter to drink", "++ &8Spiced by the barrel", "+++ &eSpiced Gold"))
			.effects(List.of("FIRE_RESISTANCE/1/20-100", "POISON/1-0/30-0"))
			.build();

	ConfigRecipe vodka = ConfigRecipe.builder()
			.name("Lousy Vodka/Vodka/Russian Vodka")
			.ingredients(List.of("Potato/10"))
			.cookingTime(15)
			.distillRuns(3)
			.age(0)
			.color("WHITE")
			.difficulty(4)
			.alcohol(20)
			.lore(List.of("+ &8Almost undrinkable"))
			.effects(List.of("WEAKNESS/15", "POISON/10"))
			.build();

	ConfigRecipe shroom_vodka = ConfigRecipe.builder()
			.name("Mushroom Vodka/Mushroom Vodka/Glowing Mushroom Vodka")
			.ingredients(List.of("Potato/10", "Red_Mushroom/3", "Brown_Mushroom/3"))
			.cookingTime(18)
			.distillRuns(5)
			.age(0)
			.color("ff9999")
			.difficulty(7)
			.alcohol(18)
			.lore(List.of("+++&aGlows in the dark"))
			.effects(List.of("WEAKNESS/80", "CONFUSION/27", "NIGHT_VISION/50-80", "BLINDNESS/12-2", "SLOW/10-3"))
			.build();

	ConfigRecipe gin = ConfigRecipe.builder()
			.name("Pale Gin/Gin/Old Tom Gin")
			.ingredients(List.of("Wheat/9", "blue-flowers/6", "Apple/1"))
			.cookingTime(6)
			.distillRuns(2)
			.color("99ddff")
			.difficulty(6)
			.alcohol(20)
			.lore(List.of("++ With the", "++ taste of juniper", "+++ Perfectly finished off", "+++ with juniper"))
			.build();

	ConfigRecipe tequila = ConfigRecipe.builder()
			.name("Mezcal/Tequila/Tequila anejo")
			.ingredients(List.of("cactus/8"))
			.cookingTime(15)
			.distillRuns(2)
			.color("f5f07e")
			.difficulty(5)
			.wood(1)
			.age(12)
			.alcohol(20)
			.lore(List.of("Desert spirit"))
			.build();

	ConfigRecipe absinthe = ConfigRecipe.builder()
			.name("Poor Absinthe/Absinthe/Strong Absinthe")
			.ingredients(List.of("Grass/15"))
			.cookingTime(3)
			.distillRuns(6)
			.distillTime(80)
			.color("GREEN")
			.difficulty(8)
			.alcohol(42)
			.lore(List.of("+++&8High proof liquor"))
			.effects(List.of("POISON/15-25"))
			.build();

	ConfigRecipe gr_absinthe = ConfigRecipe.builder()
			.name("Poor Absinthe/Green Absinthe/Bright Green Absinthe")
			.ingredients(List.of("Grass/17", "Poisonous_Potato/2"))
			.cookingTime(5)
			.distillRuns(6)
			.distillTime(85)
			.color("LIME")
			.difficulty(9)
			.alcohol(46)
			.lore(List.of("&aLooks poisonous"))
			.effects(List.of("POISON/25-40", "HARM/2", "NIGHT_VISION/40-60"))
			.build();

	ConfigRecipe potato_soup = ConfigRecipe.builder()
			.name("Potato soup")
			.ingredients(List.of("Potato/5", "Grass/3"))
			.cookingTime(3)
			.color("ORANGE")
			.difficulty(1)
			.effects(List.of("HEAL/0-1"))
			.build();

	ConfigRecipe coffee = ConfigRecipe.builder()
			.name("Stale Coffee/Coffee/Strong Coffee")
			.ingredients(List.of("Cocoa_Beans/12", "Milk_Bucket/2"))
			.cookingTime(2)
			.color("BLACK")
			.difficulty(3)
			.alcohol(-6)
			.lore(List.of("+ &8Probably a week old"))
			.effects(List.of("REGENERATION/1/2-5", "SPEED/1/30-140"))
			.build();

	ConfigRecipe eggnog = ConfigRecipe.builder()
			.name("Egg Liquor/Eggnog/Advocaat")
			.ingredients(List.of("Egg/5", "Sugar/2", "Milk_Bucket/1"))
			.cookingTime(2)
			.color("ffe680")
			.difficulty(4)
			.alcohol(10)
			.age(3)
			.lore(List.of("Made with raw egg"))
			.build();

	ConfigRecipe g_vodka = ConfigRecipe.builder()
			.name("Rancid Vodka/&6Golden Vodka/&6Shimmering Golden Vodka")
			.ingredients(List.of("Potato/10", "Gold_Nugget/2"))
			.cookingTime(18)
			.distillRuns(3)
			.age(0)
			.color("ORANGE")
			.difficulty(6)
			.alcohol(20)
			.effects(List.of("WEAKNESS/28", "POISON/4"))
			.build();

	ConfigRecipe fire_whiskey = ConfigRecipe.builder()
			.name("Powdery Whiskey/Burning Whiskey/Blazing Whiskey")
			.ingredients(List.of("Wheat/10", "Blaze_Powder/2"))
			.cookingTime(12)
			.distillRuns(3)
			.distillTime(55)
			.wood(4)
			.age(18)
			.color("ORANGE")
			.difficulty(7)
			.alcohol(28)
			.drinkMessage("You get a burning feeling in your mouth")
			.build();

	ConfigRecipe hot_choc = ConfigRecipe.builder()
			.name("Hot Chocolate")
			.ingredients(List.of("cookie/3"))
			.cookingTime(2)
			.color("DARK_RED")
			.difficulty(2)
			.effects(List.of("FAST_DIGGING/40"))
			.build();

	ConfigRecipe iced_coffee = ConfigRecipe.builder()
			.name("Watery Coffee/Iced Coffee/Strong Iced Coffee")
			.ingredients(List.of("cookie/8", "snowball/4", "milk_bucket/1"))
			.cookingTime(1)
			.color("BLACK")
			.difficulty(4)
			.alcohol(-8)
			.effects(List.of("REGENERATION/30", "SPEED/10"))
			.build();
}
