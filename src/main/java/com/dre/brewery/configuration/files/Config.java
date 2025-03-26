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

package com.dre.brewery.configuration.files;

import com.dre.brewery.configuration.AbstractOkaeriConfigFile;
import com.dre.brewery.configuration.annotation.DefaultCommentSpace;
import com.dre.brewery.configuration.annotation.Footer;
import com.dre.brewery.configuration.annotation.LocalizedComment;
import com.dre.brewery.configuration.annotation.OkaeriConfigFileOptions;
import com.dre.brewery.configuration.sector.WordsSector;
import com.dre.brewery.configuration.sector.capsule.ConfigCauldronIngredient;
import com.dre.brewery.configuration.sector.capsule.ConfigCustomItem;
import com.dre.brewery.configuration.sector.capsule.ConfigDistortWord;
import com.dre.brewery.configuration.sector.capsule.ConfigRecipe;
import com.dre.brewery.configuration.sector.capsule.ConfiguredDataManager;
import com.dre.brewery.storage.DataManagerType;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.Logging;
import com.dre.brewery.utility.releases.ReleaseChecker;
import eu.okaeri.configs.annotation.Header;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;
import java.util.Random;


@OkaeriConfigFileOptions(value = "config.yml", removeOrphans = true)
@Header({ "!!! IMPORTANT: BreweryX configuration files do NOT support external comments! If you add any comments, they will be overwritten !!!",
    "Our proper config guide can be found at: https://brewery.lumamc.net/en/guide/edit_config/",
    "Still have questions? Join our Discord: https://discord.gg/ZTGCzeKg45" })
@Footer({ "", "Yep, that's it! The end of config.yml! I had so much fun! And you?..." })
@DefaultCommentSpace(1)
@Getter
@Setter
public class Config extends AbstractOkaeriConfigFile {

    @Override
    public void onFirstCreation() {
        Logging.log("&9Creating a new &6config.yml&9!");
        Logging.log("&9If this is your first time using BreweryX, change config.yml#language to your language and run &6/brewery reload");
    }

    // This doesn't need to be an enumerator, we're reading this value back to an enum from TranslationManager which doesn't rely on this class.
    @LocalizedComment("config.language")
    private String language = "en";

    @LocalizedComment("config.updateCheck")
    private ReleaseChecker.ReleaseCheckerType resolveUpdatesFrom = ReleaseChecker.ReleaseCheckerType.GITHUB;

    @LocalizedComment("config.autosave")
    private int autosave = 10;

    @LocalizedComment("config.pluginPrefix")
    private String pluginPrefix = "&2[BreweryX]&f ";

    @LocalizedComment("config.debug")
    private boolean debug = false;


    @LocalizedComment("config.storage.header")
    private ConfiguredDataManager storage = ConfiguredDataManager.builder()
        .type(DataManagerType.SQLITE)
        .database("brewery-data")
        .tablePrefix("brewery_")
        .address("localhost")
        .username("root")
        .password("password")
        .build();


    @LocalizedComment("config.enableHome")
    private boolean enableHome = true;

    @LocalizedComment("config.homeType")
    private String homeType = "cmd: home";

    @LocalizedComment("config.enableWake")
    private boolean enableWake = true;

    @LocalizedComment("config.enableLoginDisallow")
    private boolean enableLoginDisallow = true;

    @LocalizedComment("config.enableKickOnOverdrink")
    private boolean enableKickOnOverdrink = false;

    @LocalizedComment("config.enablePuke")
    private boolean enablePuke = true;

    @LocalizedComment("config.pukeItem")
    private Object pukeItem = List.of(Material.SOUL_SAND);

    public List<Material> getPukeItem() {
        return BUtil.getListSafely(pukeItem, Material.class);
    }

    @LocalizedComment("config.pukeDespawntime")
    private int pukeDespawntime = 60;

    @LocalizedComment("config.stumblePercent")
    private int stumblePercent = 100;

    public float getStumblePercent() {
        return stumblePercent / 100f;
    }

    @LocalizedComment("config.showStatusOnDrink")
    private boolean showStatusOnDrink = true;

    @LocalizedComment("config.drainItems")
    private List<String> drainItems = List.of("Bread/4", "Milk_Bucket/2");

    @LocalizedComment("config.enableCauldronParticles")
    private boolean enableCauldronParticles = true;

    @LocalizedComment("config.minimalParticles")
    private boolean minimalParticles = false;

    @LocalizedComment("config.craft-enableSealingTable")
    private boolean craftSealingTable = true;
    private boolean enableSealingTable = true;

    @LocalizedComment("config.sealingTableBlock")
    private Material sealingTableBlock = Material.SMOKER;

    @LocalizedComment("config.alwaysShowQuality")
    private boolean alwaysShowQuality = true;

    @LocalizedComment("config.alwaysShowAlc")
    private boolean alwaysShowAlc = false;

    @LocalizedComment("config.alwaysShowAlcIndicator")
    private boolean alwaysShowAlcIndicator = true;

    @LocalizedComment("config.showRuinedBrewHints")
    private boolean showRuinedBrewHints = false;

    @LocalizedComment("config.showBrewer")
    private boolean showBrewer = false;

    @LocalizedComment("config.requireKeywordOnSigns")
    private boolean requireKeywordOnSigns = true;

    @LocalizedComment("config.ageInMCBarrels")
    private boolean ageInMCBarrels = true;
    private int maxBrewsInMCBarrels = 6;

    @LocalizedComment("config.agingYearDuration")
    private int agingYearDuration = 20;

    @LocalizedComment("config.newBarrelTypeAlgorithm")
    private boolean newBarrelTypeAlgorithm = false;

    @LocalizedComment("config.commandAliases")
    private List<String> commandAliases = List.of("brewery", "brew");

    @LocalizedComment("config.enableEncode")
    private boolean enableEncode = false;
    private long encodeKey = new Random().nextLong(); // Generate a random key


    @LocalizedComment("config.useOtherPlugins")
    private boolean useWorldGuard = true;
    private boolean useLWC = true;
    private boolean useGriefPrevention = true;
    private boolean useTowny = true;
    private boolean useLands = true;
    private boolean useBlockLocker = true;
    private boolean useGMInventories = true;

    @LocalizedComment("config.useVirtualChestPerms")
    private boolean useVirtualChestPerms = false;

    @LocalizedComment("config.useLogBlock")
    private boolean useLogBlock = true;

    @LocalizedComment("config.useOffhandForCauldron")
    private boolean useOffhandForCauldron = false;

    @LocalizedComment("config.loadDataAsync") // Unused, see config-langs/en.yml#config.loadDataAsync comment
    private boolean loadDataAsync = true;

    @LocalizedComment("config.hangoverDays")
    private int hangoverDays = 7;

    @LocalizedComment("config.onlyAllowBrewsInBarrels")
    private boolean onlyAllowBrewsInBarrels = false;

    @LocalizedComment("config.colorInBarrels-Brewer")
    private boolean colorInBarrels = true;
    private boolean colorInBrewer = true;

    @LocalizedComment("config.openLargeBarrelEverywhere")
    private boolean openLargeBarrelEverywhere = true;

    @LocalizedComment("config.barrelInvSizes")
    private int barrelInvSizeLarge = 3;
    private int barrelInvSizeSmall = 1;

    @LocalizedComment("config.brewHopperDump")
    private boolean brewHopperDump = true;

    @LocalizedComment("config.enableChatDistortion")
    private boolean enableChatDistortion = true;

    @LocalizedComment("config.logRealChat")
    private boolean logRealChat = false;

    @LocalizedComment("config.distortCommands")
    private List<String> distortCommands = List.of("/gl", "/global", "/fl", "/s", "/letter", "/g", "/l", "/lokal",
        "/local", "/mail send", "/m", "/msg", "/w", "/whisper", "/reply", "/r", "/t", "/tell");

    @LocalizedComment("config.distortSignText")
    private boolean distortSignText = false;

    @LocalizedComment("config.distortBypass")
    private List<String> distortBypass = List.of("*,*", "[,]");


    @LocalizedComment("config.words")
    private List<ConfigDistortWord> words = new WordsSector().getCapsules().values().stream().toList();


    @LocalizedComment("config.useOtherFiles")
    private Map<String, ConfigCustomItem> customItems = Map.of();
    @LocalizedComment("config.useOtherFiles")
    private Map<String, ConfigCauldronIngredient> cauldron = Map.of();
    @LocalizedComment("config.useOtherFiles")
    private Map<String, ConfigRecipe> recipes = Map.of();

}
