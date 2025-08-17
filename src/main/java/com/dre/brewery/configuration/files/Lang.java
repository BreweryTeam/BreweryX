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

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.configuration.AbstractOkaeriConfigFile;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.annotation.DefaultCommentSpace;
import com.dre.brewery.configuration.annotation.OkaeriConfigFileOptions;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.Logging;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import eu.okaeri.configs.annotation.Exclude;
import eu.okaeri.configs.annotation.Header;
import lombok.SneakyThrows;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Our bind file for this class should vary based on what language the user has set in the config.
@OkaeriConfigFileOptions(useLangFileName = true, removeOrphans = true)
@Header({"!!! IMPORTANT: BreweryX configuration files do NOT support external comments! If you add any comments, they will be overwritten !!!",
    "Translations for BreweryX"})
@DefaultCommentSpace(1)
@SuppressWarnings("unused")
public class Lang extends AbstractOkaeriConfigFile {

    @Exclude
    private transient final Config config = ConfigManager.getConfig(Config.class);
    @Exclude
    private transient Map<String, Object> mappedEntries; // String or List<String> only

    @SneakyThrows
    @Override // Should override because we need to remap our strings after a reload of this file.
    public void reload() {
        if (this.blankInstance) {
            super.reload();
            return;
        }
        Path bind = ConfigManager.getFilePath(Lang.class);
        File file = bind.toFile();
        if (!file.exists()) {
            file.createNewFile();
        }
        this.setBindFile(bind);
        this.load(this.update);
        this.mapStrings();
    }

    public void updateMissingValuesFrom(@Nullable Lang other) {
        if (other == null) {
            return;
        }

        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.getType() != String.class && field.getType() != List.class) {
                continue;
            }

            try {
                Object thisValue = field.get(this);
                Object otherValue = field.get(other);
                if (thisValue == null && otherValue != null) {
                    field.set(this, otherValue);
                }
            } catch (IllegalAccessException e) {
                Logging.errorLog("Lang failed to get a field value! &6(" + field.getName() + ")", e);
            }
        }
    }

    public void mapStrings() {
        BreweryPlugin plugin = BreweryPlugin.getInstance();
        Logging.log("Using language&7: &a" + this.getBindFile().getFileName());

        this.mappedEntries = new HashMap<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.getType() != String.class && field.getType() != List.class) {
                continue;
            }

            try {
                CustomKey customKey = field.getAnnotation(CustomKey.class);
                if (customKey != null) {
                    this.mappedEntries.put(customKey.value(), field.get(this));
                } else {
                    this.mappedEntries.put(field.getName(), field.get(this));
                }
            } catch (IllegalAccessException e) {
                Logging.errorLog("Lang failed to get a field value! &6(" + field.getName() + ")", e);
            }
        }
    }

    public void sendEntry(CommandSender recipient, String key, Object... args) {
        recipient.sendMessage(BUtil.color(config.getPluginPrefix() + this.getEntry(key, false, args)));
    }

    public void logEntry(Logging.LogLevel level, String key, Object... args) {
        Logging.log(level, this.getEntry(key, false, args));
    }

    public String getEntry(String key, Object... args) {
        return this.getEntry(key, true, args);
    }

    public String getEntry(String key, boolean color, Object... args) {
        if (mappedEntries == null) {
            mapStrings();
        }

        String msg;
        Object entry = mappedEntries.get(key);
        if (entry instanceof String) {
            msg = format((String) entry, args);
        } else if (entry instanceof List) {
            msg = "&c[LanguageReader] Config entry for key '" + key + "' is a list!";
        } else {
            msg = "&c[LanguageReader] Failed to retrieve a config entry for key '" + key + "'!";
        }
        return color ? BUtil.color(msg) : msg;
    }

    public List<String> getEntries(String key, Object... args) {
        return this.getEntries(key, true, args);
    }

    @SuppressWarnings("unchecked") // All lists in this class are List<String>
    public List<String> getEntries(String key, boolean color, Object... args) {
        if (mappedEntries == null) {
            mapStrings();
        }

        List<String> msgs;
        Object entry = mappedEntries.get(key);
        if (entry instanceof String) {
            msgs = List.of((String) entry);
        } else if (entry instanceof List) {
            msgs = (List<String>) entry;
        } else {
            msgs = List.of("&c[LanguageReader] Failed to retrieve a config entry for key '" + key + "'!");
        }
        return msgs.stream()
            .map(s -> format(s, args))
            .map(s -> color ? BUtil.color(s) : s)
            .toList();
    }

    private static String format(String entry, Object... args) {
        int i = 0;
        for (Object arg : args) {
            if (arg != null) {
                i++;
                entry = entry.replace("&v" + i, arg.toString());
            }
        }
        return entry;
    }


    // I shouldn't have to set any declarations here since they'll all be pulled from the bound translation files.

    @Comment("Brew")
    @CustomKey("Brew_-times")
    private String brewTimes;
    @CustomKey("Brew_BadPotion")
    private String brewBadPotion;
    @CustomKey("Brew_BarrelRiped")
    private String brewBarrelRiped;
    @CustomKey("Brew_DistillUndefined")
    private String brewDistillUndefined;
    @CustomKey("Brew_Distilled")
    private String brewDistilled;
    @CustomKey("Brew_LessDistilled")
    private String brewLessDistilled;
    @CustomKey("Brew_HundredsOfYears")
    private String brewHundredsOfYears;
    @CustomKey("Brew_Ingredients")
    private String brewIngredients;
    @CustomKey("Brew_MinutePluralPostfix")
    private String brewMinutePluralPostfix;
    @CustomKey("Brew_OneYear")
    private String brewOneYear;
    @CustomKey("Brew_ThickBrew")
    private String brewThickBrew;
    @CustomKey("Brew_Undefined")
    private String brewUndefined;
    @CustomKey("Brew_Woodtype")
    private String brewWoodtype;
    @CustomKey("Brew_Years")
    private String brewYears;
    @CustomKey("Brew_fermented")
    private String brewFermented;
    @CustomKey("Brew_minute")
    private String brewMinute;
    @CustomKey("Brew_Alc")
    private String brewAlc;
    @CustomKey("Brew_Alcoholic")
    private String brewAlcoholic;
    @CustomKey("Brew_Brewer")
    private String brewBrewer;


    @Comment("CMD")
    @CustomKey("CMD_Copy_Error")
    private String cmdCopyError;
    @CustomKey("CMD_Info_Drunk")
    private String cmdInfoDrunk;
    @CustomKey("CMD_Info_NotDrunk")
    private String cmdInfoNotDrunk;
    @CustomKey("CMD_NonStatic")
    private String cmdNonStatic;
    @CustomKey("CMD_Player")
    private String cmdPlayer;
    @CustomKey("CMD_Player_Error")
    private String cmdPlayerError;
    @CustomKey("CMD_Reload")
    private String cmdReload;
    @CustomKey("CMD_Created")
    private String cmdCreated;
    @CustomKey("CMD_Configname")
    private String cmdConfigname;
    @CustomKey("CMD_Configname_Error")
    private String cmdConfignameError;
    @CustomKey("CMD_Static")
    private String cmdStatic;
    @CustomKey("CMD_UnLabel")
    private String cmdUnLabel;
    @CustomKey("CMD_Drink")
    private String cmdDrink;
    @CustomKey("CMD_DrinkOther")
    private String cmdDrinkOther;
    @CustomKey("CMD_Set")
    private String cmdSet;
    @CustomKey("CMD_Distilled")
    private String cmdDistilled;
    @CustomKey("CMD_Aged")
    private String cmdAged;
    @CustomKey("CMD_Simulated")
    private String cmdSimulated;
    @CustomKey("CMD_Invalid_Option")
    private String cmdInvalidOption;
    @CustomKey("CMD_Duplicate_Option")
    private String cmdDuplicateOption;
    @CustomKey("CMD_Invalid_Cook_Time")
    private String cmdInvalidCookTime;
    @CustomKey("CMD_Invalid_Distill_Runs")
    private String cmdInvalidDistillRuns;
    @CustomKey("CMD_Invalid_Wood_Type")
    private String cmdInvalidWoodType;
    @CustomKey("CMD_Invalid_Age_Time")
    private String cmdInvalidAgeTime;
    @CustomKey("CMD_Ingredient")
    private String cmdIngredient;
    @CustomKey("CMD_Invalid_Ingredient")
    private String cmdInvalidIngredient;
    @CustomKey("CMD_Missing_Cook_Time")
    private String cmdMissingCookTime;
    @CustomKey("CMD_Missing_Ingredients")
    private String cmdMissingIngredients;
    @CustomKey("CMD_Cannot_Distill")
    private String cmdCannotDistill;
    @CustomKey("CMD_Distill_Ruined")
    private String cmdDistillRuined;
    @CustomKey("CMD_Age_Ruined")
    private String cmdAgeRuined;


    @Comment("Brew Defects")
    @CustomKey("Defect_WrongIngredient")
    private List<String> defectWrongIngredient;
    @CustomKey("Defect_MissingIngredient")
    private List<String> defectMissingIngredient;
    @CustomKey("Defect_LowCount")
    private List<String> defectLowCount;
    @CustomKey("Defect_HighCount")
    private List<String> defectHighCount;
    @CustomKey("Defect_NeedsDistill")
    private List<String> defectNeedsDistill;
    @CustomKey("Defect_NeedsDistillAlc")
    private List<String> defectNeedsDistillAlc;
    @CustomKey("Defect_BadDistill")
    private List<String> defectBadDistill;
    @CustomKey("Defect_Uncooked")
    private List<String> defectUncooked;
    @CustomKey("Defect_Overcooked")
    private List<String> defectOvercooked;
    @CustomKey("Defect_UnderAged")
    private List<String> defectUnderAged;
    @CustomKey("Defect_OverAged")
    private List<String> defectOverAged;
    @CustomKey("Defect_OverAgedAlc")
    private List<String> defectOverAgedAlc;
    @CustomKey("Defect_BadAged")
    private List<String> defectBadAged;
    @CustomKey("Defect_WrongWood")
    private List<String> defectWrongWood;
    @CustomKey("Defect_NoRecipesRegistered")
    private List<String> defectNoRecipesRegistered;


    @Comment("Error")
    @CustomKey("Error_ConfigUpdate")
    private String errorConfigUpdate;
    @CustomKey("Error_ItemNotPotion")
    private String errorItemNotPotion;
    @CustomKey("Error_SealingTableDisabled")
    private String errorSealingTableDisabled;
    @CustomKey("Error_SealedAlwaysStatic")
    private String errorSealedAlwaysStatic;
    @CustomKey("Error_AlreadyUnlabeled")
    private String errorAlreadyUnlabeled;
    @CustomKey("Error_NoBarrelAccess")
    private String errorNoBarrelAccess;
    @CustomKey("Error_NoBrewName")
    private String errorNoBrewName;
    @CustomKey("Error_NoPermissions")
    private String errorNoPermissions;
    @CustomKey("Error_PlayerCommand")
    private String errorPlayerCommand;
    @CustomKey("Error_Recipeload")
    private String errorRecipeload;
    @CustomKey("Error_ShowHelp")
    private String errorShowHelp;
    @CustomKey("Error_UnknownCommand")
    private String errorUnknownCommand;
    @CustomKey("Error_YmlRead")
    private String errorYmlRead;
    @CustomKey("Error_NoPlayer")
    private String errorNoPlayer;
    @CustomKey("Error_InvalidAmount")
    private String errorInvalidAmount;
    @CustomKey("Error_InvalidPluginItem")
    private String errorInvalidPluginItem;
    @CustomKey("Error_InvalidMaterial")
    private String errorInvalidMaterial;


    @Comment("Etc")
    @CustomKey("Etc_Barrel")
    private String etcBarrel;
    @CustomKey("Etc_Page")
    private String etcPage;
    @CustomKey("Etc_Usage")
    private String etcUsage;
    @CustomKey("Etc_SealingTable")
    private String etcSealingTable;
    @CustomKey("Etc_NewRelease")
    private String etcNewRelease;
    @CustomKey("Etc_LandsFlag_Title")
    private String etcLandsFlagTitle;
    @CustomKey("Etc_LandsFlag_Description")
    private String etcLandsFlagDescription;


    @Comment("Help")
    @CustomKey("Help_Copy")
    private String helpCopy;
    @CustomKey("Help_Create")
    private String helpCreate;
    @CustomKey("Help_Give")
    private String helpGive;
    @CustomKey("Help_Simulate")
    private String helpSimulate;
    @CustomKey("Help_Simulate_Options")
    private String helpSimulateOptions;
    @CustomKey("Help_Simulate_Recipe")
    private String helpSimulateRecipe;
    @CustomKey("Help_Simulate_Cook")
    private String helpSimulateCook;
    @CustomKey("Help_Simulate_Distill")
    private String helpSimulateDistill;
    @CustomKey("Help_Simulate_Age")
    private String helpSimulateAge;
    @CustomKey("Help_Simulate_Brewer")
    private String helpSimulateBrewer;
    @CustomKey("Help_Simulate_Player")
    private String helpSimulatePlayer;
    @CustomKey("Help_Distill")
    private String helpDistill;
    @CustomKey("Help_Age")
    private String helpAge;
    @CustomKey("Help_Delete")
    private String helpDelete;
    @CustomKey("Help_Help")
    private String helpHelp;
    @CustomKey("Help_Info")
    private String helpInfo;
    @CustomKey("Help_InfoOther")
    private String helpInfoOther;
    @CustomKey("Help_Reload")
    private String helpReload;
    @CustomKey("Help_Configname")
    private String helpConfigname;
    @CustomKey("Help_Static")
    private String helpStatic;
    @CustomKey("Help_Set")
    private String helpSet;
    @CustomKey("Help_UnLabel")
    private String helpUnLabel;
    @CustomKey("Help_Seal")
    private String helpSeal;
    @CustomKey("Help_Wakeup")
    private String helpWakeup;
    @CustomKey("Help_WakeupAdd")
    private String helpWakeupAdd;
    @CustomKey("Help_WakeupCheck")
    private String helpWakeupCheck;
    @CustomKey("Help_WakeupCheckSpecific")
    private String helpWakeupCheckSpecific;
    @CustomKey("Help_WakeupList")
    private String helpWakeupList;
    @CustomKey("Help_WakeupRemove")
    private String helpWakeupRemove;
    @CustomKey("Help_Puke")
    private String helpPuke;
    @CustomKey("Help_Drink")
    private String helpDrink;


    @Comment("Perms")
    @CustomKey("Perms_NoBarrelCreate")
    private String permsNoBarrelCreate;
    @CustomKey("Perms_NoBigBarrelCreate")
    private String permsNoBigBarrelCreate;
    @CustomKey("Perms_NoCauldronFill")
    private String permsNoCauldronFill;
    @CustomKey("Perms_NoCauldronInsert")
    private String permsNoCauldronInsert;
    @CustomKey("Perms_NoSmallBarrelCreate")
    private String permsNoSmallBarrelCreate;


    @Comment("Player")
    @CustomKey("Player_BarrelCreated")
    private String playerBarrelCreated;
    @CustomKey("Player_BarrelFull")
    private String playerBarrelFull;
    @CustomKey("Player_CantDrink")
    private String playerCantDrink;
    @CustomKey("Player_CauldronInfo1")
    private String playerCauldronInfo1;
    @CustomKey("Player_CauldronInfo2")
    private String playerCauldronInfo2;
    @CustomKey("Player_DrunkPassOut")
    private String playerDrunkPassOut;
    @CustomKey("Player_LoginDeny")
    private String playerLoginDeny;
    @CustomKey("Player_LoginDenyLong")
    private String playerLoginDenyLong;
    @CustomKey("Player_TriedToSay")
    private String playerTriedToSay;
    @CustomKey("Player_Wake")
    private String playerWake;
    @CustomKey("Player_WakeAlreadyDeleted")
    private String playerWakeAlreadyDeleted;
    @CustomKey("Player_WakeCancel")
    private String playerWakeCancel;
    @CustomKey("Player_WakeCreated")
    private String playerWakeCreated;
    @CustomKey("Player_WakeDeleted")
    private String playerWakeDeleted;
    @CustomKey("Player_WakeFilled")
    private String playerWakeFilled;
    @CustomKey("Player_WakeHint1")
    private String playerWakeHint1;
    @CustomKey("Player_WakeHint2")
    private String playerWakeHint2;
    @CustomKey("Player_WakeLast")
    private String playerWakeLast;
    @CustomKey("Player_WakeNoCheck")
    private String playerWakeNoCheck;
    @CustomKey("Player_WakeNoPoints")
    private String playerWakeNoPoints;
    @CustomKey("Player_WakeNotExist")
    private String playerWakeNotExist;
    @CustomKey("Player_WakeTeleport")
    private String playerWakeTeleport;
    @CustomKey("Player_ShopSealBrew")
    private String playerShopSealBrew;
    @CustomKey("Player_Drunkeness")
    private String playerDrunkeness;
    @CustomKey("Player_Hangover")
    private String playerHangover;

}
