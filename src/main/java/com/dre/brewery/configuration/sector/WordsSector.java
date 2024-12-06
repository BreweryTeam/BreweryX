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

import com.dre.brewery.configuration.sector.capsule.ConfigDistortWord;

@SuppressWarnings("unused")
public class WordsSector extends AbstractOkaeriConfigSector<ConfigDistortWord> {

    // Doesn't matter what the fields are named in this sector

    ConfigDistortWord a = ConfigDistortWord.builder()
            .replace("s")
            .to("sh")
            .percentage(90)
            .alcohol(30)
            .build();

    ConfigDistortWord b = ConfigDistortWord.builder()
            .replace("ch")
            .to("sh")
            .pre("u,s,o,a")
            .match(false)
            .alcohol(10)
            .percentage(70)
            .build();

    ConfigDistortWord c = ConfigDistortWord.builder()
            .replace("h")
            .to("hh")
            .pre("sch,h,t")
            .match(false)
            .percentage(60)
            .alcohol(20)
            .build();

	ConfigDistortWord d = ConfigDistortWord.builder()
			.replace("th")
			.to("thl")
			.percentage(40)
			.alcohol(30)
			.build();

	ConfigDistortWord e = ConfigDistortWord.builder()
			.replace("sch")
			.to("shk")
			.percentage(60)
			.alcohol(40)
			.build();

	ConfigDistortWord f = ConfigDistortWord.builder()
			.replace("u")
			.to("uuh")
			.percentage(20)
			.build();

	ConfigDistortWord g = ConfigDistortWord.builder()
			.replace("y")
			.to("yy")
			.percentage(60)
			.alcohol(15)
			.build();

	ConfigDistortWord h = ConfigDistortWord.builder()
			.replace("e")
			.to("ee")
			.percentage(40)
			.alcohol(15)
			.build();

	ConfigDistortWord i = ConfigDistortWord.builder()
			.replace("you")
			.to("u")
			.percentage(40)
			.build();

	ConfigDistortWord j = ConfigDistortWord.builder()
			.replace("u")
			.to("uo")
			.pre("u")
			.match(false)
			.percentage(60)
			.build();

	ConfigDistortWord k = ConfigDistortWord.builder()
			.replace("that")
			.to("taht")
			.percentage(20)
			.alcohol(40)
			.build();

	ConfigDistortWord l = ConfigDistortWord.builder()
			.replace("p")
			.to("b")
			.percentage(30)
			.build();

	ConfigDistortWord m = ConfigDistortWord.builder()
			.replace("p")
			.to("b")
			.percentage(70)
			.alcohol(60)
			.build();

	ConfigDistortWord n = ConfigDistortWord.builder()
			.replace("up")
			.to("ubb")
			.percentage(80)
			.alcohol(25)
			.build();

	ConfigDistortWord o = ConfigDistortWord.builder()
			.replace("o")
			.to("oh")
			.percentage(20)
			.build();

	ConfigDistortWord p = ConfigDistortWord.builder()
			.replace("ei")
			.to("i")
			.percentage(30)
			.alcohol(15)
			.build();

	ConfigDistortWord q = ConfigDistortWord.builder()
			.replace("b")
			.to("bb")
			.percentage(80)
			.alcohol(40)
			.build();

	ConfigDistortWord r = ConfigDistortWord.builder()
			.replace("!!!")
			.to("!!!111!!!eleven!1!")
			.pre("!")
			.match(false)
			.percentage(20)
			.alcohol(70)
			.build();

	ConfigDistortWord s = ConfigDistortWord.builder()
			.replace("!")
			.to("!!")
			.pre("!")
			.match(false)
			.percentage(90)
			.build();

	ConfigDistortWord t = ConfigDistortWord.builder()
			.replace("drunk")
			.to("dhrkunn")
			.pre("are")
			.match(false)
			.percentage(70)
			.alcohol(65)
			.build();

	ConfigDistortWord u = ConfigDistortWord.builder()
			.replace("walk")
			.to("whhealhk")
			.pre("you can, you can still, you can not")
			.match(false)
			.percentage(80)
			.alcohol(30)
			.build();

	ConfigDistortWord v = ConfigDistortWord.builder()
			.replace("wtf")
			.to("wft")
			.percentage(20)
			.alcohol(40)
			.build();

	ConfigDistortWord w = ConfigDistortWord.builder()
			.replace("lol")
			.to("loool")
			.percentage(80)
			.alcohol(10)
			.build();

	ConfigDistortWord x = ConfigDistortWord.builder()
			.replace("afk")
			.to("aafkayyy")
			.percentage(30)
			.alcohol(30)
			.build();

	ConfigDistortWord y = ConfigDistortWord.builder()
			.replace("write")
			.to("wreitt")
			.pre("you can,you can still,you can not")
			.match(false)
			.percentage(80)
			.alcohol(50)
			.build();

	ConfigDistortWord z = ConfigDistortWord.builder()
			.replace("drink")
			.to("booze")
			.percentage(80)
			.alcohol(70)
			.build();

	ConfigDistortWord aA = ConfigDistortWord.builder()
			.replace("?")
			.to("????")
			.pre("?")
			.match(false)
			.percentage(80)
			.alcohol(40)
			.build();

	ConfigDistortWord bB = ConfigDistortWord.builder()
			.replace("-space")
			.to("")
			.pre("h,g,w")
			.match(true)
			.alcohol(10)
			.build();

	ConfigDistortWord cC = ConfigDistortWord.builder()
			.replace("-space")
			.to("")
			.percentage(30)
			.alcohol(35)
			.build();

	ConfigDistortWord dD = ConfigDistortWord.builder()
			.replace("-space")
			.to("")
			.percentage(10)
			.build();

	ConfigDistortWord eE = ConfigDistortWord.builder()
			.replace("-start")
			.to("dho")
			.percentage(15)
			.alcohol(50)
			.build();

	ConfigDistortWord fF = ConfigDistortWord.builder()
			.replace("-start")
			.to("hhn")
			.percentage(10)
			.alcohol(50)
			.build();

	ConfigDistortWord gG = ConfigDistortWord.builder()
			.replace("-random")
			.to("lu")
			.percentage(10)
			.build();

	ConfigDistortWord hH = ConfigDistortWord.builder()
			.replace("-random")
			.to("lug")
			.percentage(10)
			.alcohol(50)
			.build();

	ConfigDistortWord iI = ConfigDistortWord.builder()
			.replace("-random")
			.to("blub")
			.percentage(20)
			.alcohol(80)
			.build();

	ConfigDistortWord jJ = ConfigDistortWord.builder()
			.replace("-random")
			.to("lerg")
			.percentage(40)
			.alcohol(85)
			.build();

	ConfigDistortWord kK = ConfigDistortWord.builder()
			.replace("-random")
			.to("gul")
			.percentage(40)
			.alcohol(80)
			.build();

	ConfigDistortWord lL = ConfigDistortWord.builder()
			.replace("-random")
			.to(" ")
			.percentage(100)
			.alcohol(70)
			.build();

	ConfigDistortWord mM = ConfigDistortWord.builder()
			.replace("-random")
			.to(" ")
			.percentage(60)
			.alcohol(40)
			.build();

	ConfigDistortWord nN = ConfigDistortWord.builder()
			.replace("-random")
			.to(" ")
			.percentage(50)
			.alcohol(30)
			.build();

	ConfigDistortWord oO = ConfigDistortWord.builder()
			.replace("-end")
			.to("!")
			.percentage(40)
			.alcohol(30)
			.build();

	ConfigDistortWord pP = ConfigDistortWord.builder()
			.replace("-random")
			.to(" *hic* ")
			.percentage(80)
			.alcohol(70)
			.build();

	ConfigDistortWord qQ = ConfigDistortWord.builder()
			.replace("-random")
			.to(" *hic* ")
			.percentage(15)
			.alcohol(40)
			.build();

	ConfigDistortWord rR = ConfigDistortWord.builder()
			.replace("-space")
			.to(" *hic* ")
			.percentage(5)
			.alcohol(20)
			.build();

	ConfigDistortWord sS = ConfigDistortWord.builder()
			.replace("-end")
			.to(" *hic*")
			.percentage(70)
			.alcohol(50)
			.build();

	ConfigDistortWord tT = ConfigDistortWord.builder()
			.replace("-all")
			.to("*burp*")
			.percentage(3)
			.alcohol(60)
			.build();

	ConfigDistortWord uU = ConfigDistortWord.builder()
			.replace("-all")
			.to("*burp*")
			.percentage(6)
			.alcohol(80)
			.build();

}
