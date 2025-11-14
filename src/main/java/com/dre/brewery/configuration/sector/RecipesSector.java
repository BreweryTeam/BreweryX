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
import com.dre.brewery.utility.BukkitConstants;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

import java.util.List;

@Getter
@Setter
public class RecipesSector extends AbstractOkaeriConfigSector<ConfigRecipe> {

    // Comments not supported here

    ConfigRecipe ex = ConfigRecipe.builder()
        .enabled(false)
        .name("Bad Example/Example/Good Example")
        .ingredients(List.of(Material.BEDROCK.name() + "/2", Material.SPRUCE_PLANKS.name() + "/8", Material.BEDROCK.name() + "/1", "Brewery:Wheatbeer/2", "ExoticGarden:Grape/3", "ex-item/4"))
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
        .effects(List.of(BukkitConstants.FIRE_RESISTANCE.getKey().getKey().toUpperCase() + "/20", BukkitConstants.INSTANT_HEALTH.getKey().getKey().toUpperCase() + "/1", BukkitConstants.WEAKNESS.getKey().getKey().toUpperCase() + "/2-3/50-60", BukkitConstants.POISON.getKey().getKey().toUpperCase() + "/1-0/20-0"))
        .build();

    ConfigRecipe wheatbeer = ConfigRecipe.builder()
        .name("Skunky Wheatbeer/Wheatbeer/Fine Wheatbeer")
        .ingredients(List.of(Material.WHEAT.name() + "/3"))
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
        .ingredients(List.of(Material.WHEAT.name() + "/6"))
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
        .ingredients(List.of(Material.WHEAT.name() + "/6"))
        .cookingTime(8)
        .distillRuns(0)
        .wood(6)
        .age(8)
        .color("650013")
        .difficulty(2)
        .alcohol(7)
        .lore(List.of("+++ &8Roasted taste"))
        .build();

    ConfigRecipe wine = ConfigRecipe.builder()
        .name("Red Wine")
        .ingredients(List.of(Material.SWEET_BERRIES.name() + "/5"))
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
        .ingredients(List.of(Material.SUGAR_CANE.name() + "/6"))
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
        .ingredients(List.of(Material.SUGAR_CANE.name() + "/6", Material.APPLE.name() + "/2"))
        .cookingTime(4)
        .distillRuns(0)
        .wood(2)
        .age(4)
        .color("ORANGE")
        .difficulty(4)
        .alcohol(11)
        .lore(List.of("Is there any Apple in this?", "Refreshing taste of Apple", "Sweetest hint of Apple"))
        .effects(List.of(BukkitConstants.WATER_BREATHING.getKey().getKey().toUpperCase() + "/1-2/150"))
        .build();

    ConfigRecipe cidre = ConfigRecipe.builder()
        .name("Poor Cidre/Apple Cider/Great Apple Cider")
        .ingredients(List.of(Material.APPLE.name() + "/14"))
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
        .ingredients(List.of(Material.APPLE.name() + "/12"))
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
        .ingredients(List.of(Material.WHEAT.name() + "/10"))
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
        .ingredients(List.of(Material.SUGAR_CANE.name() + "/18"))
        .cookingTime(6)
        .distillRuns(2)
        .distillTime(30)
        .wood(2)
        .age(14)
        .color("DARK_RED")
        .difficulty(6)
        .alcohol(30)
        .lore(List.of("+ &8Too bitter to drink", "++ &8Spiced by the barrel", "+++ &eSpiced Gold"))
        .effects(List.of(BukkitConstants.FIRE_RESISTANCE.getKey().getKey().toUpperCase() + "/1/20-100", BukkitConstants.POISON.getKey().getKey().toUpperCase() + "/1-0/30-0"))
        .build();

    ConfigRecipe vodka = ConfigRecipe.builder()
        .name("Lousy Vodka/Vodka/Russian Vodka")
        .ingredients(List.of(Material.POTATO.name() + "/10"))
        .cookingTime(15)
        .distillRuns(3)
        .age(0)
        .color("WHITE")
        .difficulty(4)
        .alcohol(20)
        .lore(List.of("+ &8Almost undrinkable"))
        .effects(List.of(BukkitConstants.WEAKNESS.getKey().getKey().toUpperCase() + "/15", BukkitConstants.POISON.getKey().getKey().toUpperCase() + "/10"))
        .build();

    ConfigRecipe shroom_vodka = ConfigRecipe.builder()
        .name("Mushroom Vodka/Mushroom Vodka/Glowing Mushroom Vodka")
        .ingredients(List.of(Material.POTATO.name() + "/10", Material.RED_MUSHROOM.name() + "/3", Material.BROWN_MUSHROOM.name() + "/3"))
        .cookingTime(18)
        .distillRuns(5)
        .age(0)
        .color("ff9999")
        .difficulty(7)
        .alcohol(18)
        .lore(List.of("+++&aGlows in the dark"))
        .effects(List.of(BukkitConstants.WEAKNESS.getKey().getKey().toUpperCase() + "/80", BukkitConstants.NAUSEA.getKey().getKey().toUpperCase() + "/27", BukkitConstants.NIGHT_VISION.getKey().getKey().toUpperCase() + "/50-80", BukkitConstants.BLINDNESS.getKey().getKey().toUpperCase() + "/12-2", BukkitConstants.SLOWNESS.getKey().getKey().toUpperCase() + "/10-3"))
        .build();

    ConfigRecipe gin = ConfigRecipe.builder()
        .name("Pale Gin/Gin/Old Tom Gin")
        .ingredients(List.of(Material.WHEAT.name() + "/9", "blue-flowers/6", Material.APPLE.name() + "/1"))
        .cookingTime(6)
        .distillRuns(2)
        .color("99ddff")
        .difficulty(6)
        .alcohol(20)
        .lore(List.of("++ With the", "++ taste of juniper", "+++ Perfectly finished off", "+++ with juniper"))
        .build();

    ConfigRecipe tequila = ConfigRecipe.builder()
        .name("Mezcal/Tequila/Tequila anejo")
        .ingredients(List.of(Material.CACTUS.name() + "/8"))
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
        .ingredients(List.of(Material.SHORT_GRASS.name() + "/15"))
        .cookingTime(3)
        .distillRuns(6)
        .distillTime(80)
        .color("GREEN")
        .difficulty(8)
        .alcohol(42)
        .lore(List.of("+++&8High proof liquor"))
        .effects(List.of(BukkitConstants.POISON.getKey().getKey().toUpperCase() + "/15-25"))
        .build();

    ConfigRecipe gr_absinthe = ConfigRecipe.builder()
        .name("Poor Absinthe/Green Absinthe/Bright Green Absinthe")
        .ingredients(List.of(Material.SHORT_GRASS.name() + "/17", Material.POISONOUS_POTATO.name() + "/2"))
        .cookingTime(5)
        .distillRuns(6)
        .distillTime(85)
        .color("LIME")
        .difficulty(9)
        .alcohol(46)
        .lore(List.of("&aLooks poisonous"))
        .effects(List.of(BukkitConstants.POISON.getKey().getKey().toUpperCase() + "/25-40", BukkitConstants.INSTANT_DAMAGE.getKey().getKey().toUpperCase() + "/2", BukkitConstants.NIGHT_VISION.getKey().getKey().toUpperCase() + "/40-60"))
        .build();

    ConfigRecipe potato_soup = ConfigRecipe.builder()
        .name("Potato soup")
        .ingredients(List.of(Material.POTATO.name() + "/5", Material.SHORT_GRASS.name() + "/3"))
        .cookingTime(3)
        .color("ORANGE")
        .difficulty(1)
        .effects(List.of(BukkitConstants.INSTANT_HEALTH.getKey().getKey().toUpperCase() + "/0-1"))
        .build();

    ConfigRecipe coffee = ConfigRecipe.builder()
        .name("Stale Coffee/Coffee/Strong Coffee")
        .ingredients(List.of(Material.COCOA_BEANS.name() + "/12", Material.MILK_BUCKET.name() + "/2"))
        .cookingTime(2)
        .color("BLACK")
        .difficulty(3)
        .alcohol(-6)
        .lore(List.of("+ &8Probably a week old"))
        .effects(List.of(BukkitConstants.REGENERATION.getKey().getKey().toUpperCase() + "/1/2-5", BukkitConstants.SPEED.getKey().getKey().toUpperCase() + "/1/30-140"))
        .build();

    ConfigRecipe eggnog = ConfigRecipe.builder()
        .name("Egg Liquor/Eggnog/Advocaat")
        .ingredients(List.of(Material.EGG.name() + "/5", Material.SUGAR.name() + "/2", Material.MILK_BUCKET.name() + "/1"))
        .cookingTime(2)
        .color("ffe680")
        .difficulty(4)
        .alcohol(10)
        .age(3)
        .lore(List.of("Made with raw egg"))
        .build();

    ConfigRecipe g_vodka = ConfigRecipe.builder()
        .name("Rancid Vodka/&6Golden Vodka/&6Shimmering Golden Vodka")
        .ingredients(List.of(Material.POTATO.name() + "/10", Material.GOLD_NUGGET.name() + "/2"))
        .cookingTime(18)
        .distillRuns(3)
        .age(0)
        .color("ORANGE")
        .difficulty(6)
        .alcohol(20)
        .effects(List.of(BukkitConstants.WEAKNESS.getKey().getKey().toUpperCase() + "/28", BukkitConstants.POISON.getKey().getKey().toUpperCase() + "/4"))
        .build();

    ConfigRecipe fire_whiskey = ConfigRecipe.builder()
        .name("Powdery Whiskey/Burning Whiskey/Blazing Whiskey")
        .ingredients(List.of(Material.WHEAT.name() + "/10", Material.BLAZE_POWDER.name() + "/2"))
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
        .ingredients(List.of(Material.COOKIE.name() + "/3"))
        .cookingTime(2)
        .color("DARK_RED")
        .difficulty(2)
        .effects(List.of(BukkitConstants.HASTE.getKey().getKey().toUpperCase() + "/40"))
        .build();

    ConfigRecipe iced_coffee = ConfigRecipe.builder()
        .name("Watery Coffee/Iced Coffee/Strong Iced Coffee")
        .ingredients(List.of(Material.COOKIE.name() + "/8", Material.SNOWBALL.name() + "/4", Material.MILK_BUCKET.name() + "/1"))
        .cookingTime(1)
        .color("BLACK")
        .difficulty(4)
        .alcohol(-8)
        .effects(List.of(BukkitConstants.REGENERATION.getKey().getKey().toUpperCase() + "/30", BukkitConstants.SPEED.getKey().getKey().toUpperCase() + "/10"))
        .build();
}
