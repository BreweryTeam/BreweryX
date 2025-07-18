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

package com.dre.brewery;

import com.dre.brewery.api.events.brew.BrewModifyEvent;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.lore.Base91DecoderStream;
import com.dre.brewery.lore.Base91EncoderStream;
import com.dre.brewery.lore.BrewLore;
import com.dre.brewery.lore.LoreLoadStream;
import com.dre.brewery.lore.LoreSaveStream;
import com.dre.brewery.lore.NBTLoadStream;
import com.dre.brewery.lore.NBTSaveStream;
import com.dre.brewery.lore.XORScrambleStream;
import com.dre.brewery.lore.XORUnscrambleStream;
import com.dre.brewery.recipe.BEffect;
import com.dre.brewery.recipe.BRecipe;
import com.dre.brewery.recipe.BestRecipeResult;
import com.dre.brewery.recipe.PotionColor;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.Logging;
import com.dre.brewery.utility.MinecraftVersion;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Represents the liquid in the brewed Potions
 */
@Getter
@Setter
public class Brew implements Cloneable {
    private static final MinecraftVersion VERSION = BreweryPlugin.getMCVersion();
    private static final Config config = ConfigManager.getConfig(Config.class);
    private static final Lang lang = ConfigManager.getConfig(Lang.class);

    public static final byte SAVE_VER = 1;
    private static long saveSeed;
    private static List<Long> prevSaveSeeds = new ArrayList<>(); // Save Seeds that have been used in the past, stored to decode brews made at that time
    public static Map<Integer, Brew> legacyPotions = new HashMap<>();
    public static long installTime = System.currentTimeMillis(); // plugin install time in millis after epoch

    private BIngredients ingredients;
    private int quality;
    private int alc;
    private byte distillRuns;
    private float ageTime;
    private BarrelWoodType wood = BarrelWoodType.ANY;
    // TODO: This should extend BRecipe, not hold a reference.
    private BRecipe currentRecipe; // Recipe this Brew is currently based off. May change between modifications and is often null when not modifying
    private boolean unlabeled;
    private boolean persistent; // Only for legacy
    private boolean immutable; // static/immutable potions should not be changed
    private boolean stripped; // Most Brewing information removed, only drinking and rough quality information available. Brew should not change anymore
    private int lastUpdate; // last update in hours after install time
    private boolean needsSave; // There was a change that has not yet been saved
    private boolean hasGlint; // The Brew has a glint effect

    /**
     * A new Brew with only ingredients
     */
    public Brew(BIngredients ingredients) {
        this.ingredients = ingredients;
        touch();
    }

    /**
     * A Brew with quality, alc and recipe already set
     */
    public Brew(int quality, int alc, BRecipe recipe, BIngredients ingredients) {
        this.ingredients = ingredients;
        this.quality = quality;
        this.alc = alc;
        this.currentRecipe = recipe;
        touch();
    }

    /**
     * Loading a Brew with all values set
     */
    public Brew(BIngredients ingredients, int quality, int alc, byte distillRuns, float ageTime, BarrelWoodType wood, String recipe, boolean unlabeled, boolean immutable, int lastUpdate) {
        this.ingredients = ingredients;
        this.quality = quality;
        this.alc = alc;
        this.distillRuns = distillRuns;
        this.ageTime = ageTime;
        this.wood = wood;
        this.unlabeled = unlabeled;
        this.immutable = immutable;
        this.lastUpdate = lastUpdate;
        setRecipeFromString(recipe);
    }

    // Loading from InputStream
    private Brew() {
    }

    /**
     * returns a Brew by ItemMeta
     *
     * @param meta The meta to get the brew from
     * @return The Brew if meta is a brew, null if not
     */
    @Nullable
    public static Brew get(ItemMeta meta) {
        if (!MinecraftVersion.isUseNBT() && !meta.hasLore()) return null;

        Brew brew = load(meta);

        if (brew == null && meta instanceof PotionMeta && ((PotionMeta) meta).hasCustomEffect(PotionEffectType.REGENERATION)) {
            // Load Legacy
            return getFromPotionEffect(((PotionMeta) meta), false);
        }
        return brew;
    }

    /**
     * returns a Brew by ItemStack
     *
     * @param item The Item to get the brew from
     * @return The Brew if item is a brew, null if not
     */
    @Nullable
    public static Brew get(ItemStack item) {
        if (item.getType() != Material.POTION) return null;
        if (!item.hasItemMeta()) return null;

        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        if (!MinecraftVersion.isUseNBT() && !meta.hasLore()) return null;

        Brew brew = load(meta);

        if (brew == null && meta instanceof PotionMeta && ((PotionMeta) meta).hasCustomEffect(PotionEffectType.REGENERATION)) {
            // Load Legacy and convert
            brew = getFromPotionEffect(((PotionMeta) meta), true);
            if (brew == null) return null;
            new BrewLore(brew, (PotionMeta) meta).removeLegacySpacing();
            brew.save(meta);
            item.setItemMeta(meta);
        } else if (brew != null && brew.needsSave) {
            // Brew needs saving from a previous format
            if (MinecraftVersion.isUseNBT()) {
                new BrewLore(brew, (PotionMeta) meta).removeLoreData();
                Logging.debugLog("removed Data from Lore");
            }
            brew.save(meta);
            item.setItemMeta(meta);
        }
        return brew;
    }

    // Legacy Brew Loading
    private static Brew getFromPotionEffect(PotionMeta potionMeta, boolean remove) {
        for (PotionEffect effect : potionMeta.getCustomEffects()) {
            if (effect.getType().equals(PotionEffectType.REGENERATION)) {
                if (effect.getDuration() < -1) {
                    if (remove) {
                        Brew b = legacyPotions.get(effect.getDuration());
                        if (b != null) {
                            potionMeta.removeCustomEffect(PotionEffectType.REGENERATION);
                            if (b.persistent) {
                                return b;
                            } else {
                                return legacyPotions.remove(effect.getDuration());
                            }
                        }
                        return null;
                    } else {
                        return legacyPotions.get(effect.getDuration());
                    }
                }
            }
        }
        return null;
    }

    /**
     * returns a Brew by its UID
     *
     * @deprecated Does not work anymore with new save system
     */
    @Deprecated
    public static Brew get(int uid) {
        if (uid < -1) {
            if (!legacyPotions.containsKey(uid)) {
                Logging.errorLog("Database failure! unable to find UID " + uid + " of a custom Potion!");
                return null;// throw some exception?
            }
        } else {
            return null;
        }
        return legacyPotions.get(uid);
    }

    /**
     * returns UID of custom Potion item
     *
     * @deprecated Does not work anymore with new save system
     */
    @Deprecated
    public static int getUID(ItemStack item) {
        return getUID((PotionMeta) item.getItemMeta());
    }

    // returns UID of custom Potion meta
    // Does not work anymore with new save system

    /**
     * returns UID of custom Potion meta
     *
     * @deprecated Does not work anymore with new save system
     */
    @Deprecated
    public static int getUID(PotionMeta potionMeta) {
        if (potionMeta.hasCustomEffect(PotionEffectType.REGENERATION)) {
            for (PotionEffect effect : potionMeta.getCustomEffects()) {
                if (effect.getType().equals(PotionEffectType.REGENERATION)) {
                    if (effect.getDuration() < -1) {
                        return effect.getDuration();
                    }
                }
            }
        }
        return 0;
    }

    // generate an UID
	/*public static int generateUID() {
		int uid = -2;
		while (potions.containsKey(uid)) {
			uid -= 1;
		}
		return uid;
	}*/

    /**
     * returns the recipe with the given name, recalculates if not found
     */
    public boolean setRecipeFromString(String name) {
        currentRecipe = null;
        if (name != null && !name.equals("")) {
            for (BRecipe recipe : BRecipe.getAllRecipes()) {
                if (recipe.getRecipeName().equalsIgnoreCase(name)) {
                    currentRecipe = recipe;
                    return true;
                }
            }

            if (quality > 0) {
                currentRecipe = ingredients.getBestRecipe(wood, ageTime, distillRuns > 0);
                if (currentRecipe != null) {
					/*if (!immutable) {
						this.quality = calcQuality();
					}*/
                    Logging.log("A Brew was made from Recipe: '" + name + "' which could not be found. '" + currentRecipe.getRecipeName() + "' used instead!");
                    return true;
                } else {
                    Logging.errorLog("A Brew was made from Recipe: '" + name + "' which could not be found!");
                }
            }
        }
        return false;
    }

    public boolean reloadRecipe() {
        return currentRecipe == null || setRecipeFromString(currentRecipe.getRecipeName());
    }

    // Copy a Brew with a new unique ID and return its item
    // Not needed anymore
	/*public ItemStack copy(ItemStack item) {
		ItemStack copy = item.clone();
		int uid = generateUID();
		clone(uid);
		PotionMeta meta = (PotionMeta) copy.getItemMeta();
		if (!P.use1_14) {
			// This is due to the Duration Modifier, that is removed in 1.14
			uid *= 4;
		}
		meta.addCustomEffect((PotionEffectType.REGENERATION).createEffect(uid, 0), true);
		copy.setItemMeta(meta);
		return copy;
	}*/

    public boolean isSimilar(Brew brew) {
        if (brew == null) return false;
        if (equals(brew)) return true;
        return quality == brew.quality &&
            alc == brew.alc &&
            distillRuns == brew.distillRuns &&
            Float.compare(brew.ageTime, ageTime) == 0 &&
            brew.wood == wood &&
            unlabeled == brew.unlabeled &&
            persistent == brew.persistent &&
            immutable == brew.immutable &&
            stripped == brew.stripped &&
            ingredients.equals(brew.ingredients) &&
            (Objects.equals(currentRecipe, brew.currentRecipe));
    }

    /**
     * Clones this instance
     */
    @Override
    public Brew clone() {
        try {
            Brew brew = (Brew) super.clone();
            brew.ingredients = ingredients.copy();
            return brew;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    @Override
    public String toString() {
        return "Brew{" +
            "ingredients=" + ingredients +
            ", quality=" + quality +
            ", alc=" + alc +
            ", distillRuns=" + distillRuns +
            ", ageTime=" + ageTime +
            ", wood=" + wood +
            ", currentRecipe=" + currentRecipe +
            ", unlabeled=" + unlabeled +
            ", immutable=" + immutable +
            ", stripped=" + stripped +
            '}';
    }

    // remove potion from file (drinking, despawning, combusting, cmdDeleting, should be more!)
    // Not needed anymore
	/*public void remove(ItemStack item) {
		if (!persistent) {
			potions.remove(getUID(item));
		}
	}*/

    /**
     * calculate alcohol from recipe
     */
    @Contract(pure = true)
    public int calcAlcohol() {
        if (quality == 0) {
            // Give bad potions some alc
            int badAlc = 0;
            if (distillRuns > 1) {
                badAlc = distillRuns;
            }
            if (ageTime > 10) {
                badAlc += 5;
            } else if (ageTime > 2) {
                badAlc += 3;
            }
            if (currentRecipe != null) {
                return badAlc;
            } else {
                return badAlc / 2;
            }
        }

        if (currentRecipe != null) {
            int alc = currentRecipe.getAlcohol();
            if (currentRecipe.needsDistilling()) {
                if (distillRuns == 0) {
                    return 0;
                }
                // bad quality can decrease alc by up to 40%
                alc *= 1 - ((float) (10 - quality) * 0.04f);
                // distillable Potions should have half alc after one and full alc after all needed distills
                alc /= 2;
                alc *= 1.0F + ((float) distillRuns / currentRecipe.getDistillruns());
            } else {
                // quality decides 10% - 100%
                alc *= ((float) quality / 10.0f);
            }
            return alc;
        }
        return 0;
    }

    /**
     * calculating quality
     */
    @Contract(pure = true)
    public int calcQuality() {
        // calculate quality from all of the factors
        float quality = ingredients.getIngredientQuality(currentRecipe) + ingredients.getCookingQuality(currentRecipe, distillRuns > 0);
        if (currentRecipe.needsToAge() || ageTime > 0.5) {
            quality += ingredients.getWoodQuality(currentRecipe, wood) + ingredients.getAgeQuality(currentRecipe, ageTime);
            quality /= 4;
        } else {
            quality /= 2;
        }
        return Math.round(quality);
    }


    public boolean canDistill() {
        if (immutable) return false;
        if (currentRecipe != null) {
            return currentRecipe.getDistillruns() > distillRuns;
        } else {
            return distillRuns < 6;
        }
    }

    public void updateCustomModelData(ItemMeta meta) {
        if (VERSION.isOrEarlier(MinecraftVersion.V1_14)) return;
        if (currentRecipe != null && currentRecipe.getCmData() != null) {
            int cm;
            if (quality > 7) {
                cm = currentRecipe.getCmData()[2];
            } else if (quality > 3) {
                cm = currentRecipe.getCmData()[1];
            } else {
                cm = currentRecipe.getCmData()[0];
            }
            if (cm == 0) {
                meta.setCustomModelData(null);
            } else {
                meta.setCustomModelData(cm);
            }
        } else {
            meta.setCustomModelData(null);
        }
    }

    /**
     * Get Special Drink Effects
     */
    public List<BEffect> getEffects() {
        if (currentRecipe != null && quality > 0) {
            return currentRecipe.getEffects();
        }
        return null;
    }

    /**
     * Set unlabeled to true to hide the numbers in Lore
     *
     * @param item The Item this Brew is on
     */
    public void unLabel(ItemStack item) {
        if (unlabeled) return;
        unlabeled = true;
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof PotionMeta && meta.hasLore()) {
            BrewLore lore = new BrewLore(this, ((PotionMeta) meta));
            if (distillRuns > 0) {
                lore.updateDistillLore(false);
            }
            if (ageTime >= 1) {
                lore.updateAgeLore(false);
            }
            lore.updateIngredientLore(false);
            lore.updateCookLore(false);
            lore.updateDistillLore(false);
            lore.updateAgeLore(false);
            lore.updateWoodLore(false);
            lore.updateQualityStars(false);
            lore.updateAlc(false);
            lore.write();
            item.setItemMeta(meta);
        }
    }

    /**
     * Sealing the Brew to make it Immutable, Unlabeled and Stripped
     * <p>This makes it easier to sell in shops as Brews that are mostly the same will be equal after
     *
     * @param potion The Item this Brew is on
     */
    public void seal(ItemStack potion, @Nullable Player player) {
        if (stripped) return;
        ItemMeta origMeta = potion.getItemMeta();
        if (!(origMeta instanceof PotionMeta)) return;

        if (quality == 1) {
            quality = 2;
        } else if (quality % 2 == 1) {
            quality--;
        }
        alc = calcAlcohol();

        setStatic(true, potion);
        unLabel(potion);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        BrewLore lore = new BrewLore(this, meta);
        lore.updateQualityStars(false, true);
        lore.write();

        stripped = true;
        ingredients = new BIngredients();
        ageTime = 0;
        wood = BarrelWoodType.NONE;
        touch();

        BrewModifyEvent modifyEvent = new BrewModifyEvent(this, meta, BrewModifyEvent.Type.SEAL, player);
        BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(modifyEvent);

        if (modifyEvent.isCancelled()) {
            // As the brew and everything connected to it is only saved on the meta from now on,
            // restoring the origMeta is enough in this case
            potion.setItemMeta(origMeta);
            return;
        }
        save(meta);
        potion.setItemMeta(meta);
    }

    /**
     * Do some regular updates.
     * <p>Not really used, apart from legacy potion timed purge
     */
    public void touch() {
        lastUpdate = (int) ((double) (System.currentTimeMillis() - installTime) / 3600000D);
    }

    public int getOrCalcAlc() {
        if (alc == 0) {
            alc = calcAlcohol();
        }
        return alc;
    }

    public boolean hasRecipe() {
        return currentRecipe != null;
    }


    public boolean isSealed() {
        return stripped && immutable;
    }

    public boolean isStatic() {
        return immutable;
    }

    /**
     * Set the Static flag, so potion is unchangeable
     */
    public void setStatic(boolean immutable, ItemStack potion) {
        if (!immutable && isStripped()) {
            throw new IllegalStateException("Cannot make stripped Brews non-static");
        }
        if (BreweryPlugin.getMCVersion().isOrEarlier(MinecraftVersion.V1_9) && currentRecipe != null && canDistill()) {
            currentRecipe.getColor().colorBrew(((PotionMeta) potion.getItemMeta()), potion, !immutable);
        }
        this.immutable = immutable;
    }

    // Distilling section ---------------

    /**
     * distill all custom potions in the brewer
     *
     * @param inv      The Inventory of the Distiller
     * @param contents The Brews in the 3 slots of the Inventory
     */
    public static void distillAll(BrewerInventory inv, Brew[] contents) {
        for (int slot = 0; slot < 3; slot++) {
            if (contents[slot] != null) {
                ItemStack slotItem = inv.getItem(slot);
                PotionMeta potionMeta = (PotionMeta) slotItem.getItemMeta();
                contents[slot].distillSlot(slotItem, potionMeta);
            }
        }
    }

    /**
     * distill custom potion in a distiller slot
     *
     * @param slotItem   The item in the slot
     * @param potionMeta The meta of the item
     */
    public void distillSlot(ItemStack slotItem, PotionMeta potionMeta) {
        if (immutable) return;

        distillRuns += 1;
        BrewLore lore = new BrewLore(this, potionMeta);
        BestRecipeResult result = ingredients.getDistillRecipeFull(wood, ageTime);
        if (result instanceof BestRecipeResult.Found found) {
            // distillRuns will have an effect on the amount of alcohol, not the quality
            currentRecipe = found.recipe();
            quality = calcQuality();

            lore.addOrReplaceEffects(getEffects(), quality);
            potionMeta.setDisplayName(BUtil.color("&f" + currentRecipe.getName(quality)));
            currentRecipe.getColor().colorBrew(potionMeta, slotItem, canDistill());

        } else {
            quality = 0;
            lore.removeEffects();
            Logging.debugLog("Distill brew ruined! " + result);
            if (config.isShowRuinedBrewHints()) {
                BrewDefect defect = result.getWorstDefect();
                assert defect != null; // since no recipe was found, there must be a defect
                lore.updateDefect(BUtil.choose(defect.getMessages(lang)));
            }
            potionMeta.setDisplayName(BUtil.color("&f" + lang.getEntry("Brew_DistillUndefined")));
            PotionColor.GREY.colorBrew(potionMeta, slotItem, canDistill());
        }
        alc = calcAlcohol();
        updateCustomModelData(potionMeta);

        // Distill Lore
        if (currentRecipe != null && config.isColorInBrewer() != BrewLore.hasColorLore(potionMeta)) {
            lore.convertLore(config.isColorInBrewer());
        } else {
            lore.updateQualityStars(config.isColorInBrewer());
            lore.updateCustomLore();
            lore.updateDistillLore(config.isColorInBrewer());
        }
        lore.updateAlc(true);
        lore.write();
        touch();
        BrewModifyEvent modifyEvent = new BrewModifyEvent(this, potionMeta, BrewModifyEvent.Type.DISTILL);
        BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(modifyEvent);
        if (modifyEvent.isCancelled()) {
            // As the brew and everything connected to it is only saved on the meta from now on,
            // not saving the brew into potionMeta is enough to not change anything in case of cancel
            return;
        }
        save(potionMeta);

        slotItem.setItemMeta(potionMeta);
    }

    public int getDistillTimeNextRun() {
        if (!canDistill()) {
            return -1;
        }

        if (currentRecipe != null) {
            return currentRecipe.getDistillTime();
        }

        BRecipe recipe = ingredients.getDistillRecipe(wood, ageTime);
        if (recipe != null) {
            return recipe.getDistillTime();
        }
        return 0;
    }

    // Ageing Section ------------------

    public void age(ItemStack item, float time, BarrelWoodType woodType) {
        if (immutable) return;
        PotionMeta potionMeta = (PotionMeta) item.getItemMeta();

        BrewLore lore = new BrewLore(this, potionMeta);
        ageTime += time;

        // if younger than half a day, it shouldnt get aged form
        if (ageTime > 0.5) {
            woodShift(time, woodType);
            BestRecipeResult result = ingredients.getAgeRecipeFull(wood, ageTime, distillRuns > 0);
            if (result instanceof BestRecipeResult.Found found) {
                currentRecipe = found.recipe();
                quality = calcQuality();

                lore.addOrReplaceEffects(getEffects(), quality);
                potionMeta.setDisplayName(BUtil.color("&f" + currentRecipe.getName(quality)));
                currentRecipe.getColor().colorBrew(potionMeta, item, canDistill());

                if (currentRecipe.isGlint()) {
                    potionMeta.addEnchant(Enchantment.MENDING, 1, true);
                    potionMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }
            } else {
                quality = 0;
                lore.convertLore(false);
                lore.removeEffects();
                Logging.debugLog("Aging brew ruined! " + result);
                if (config.isShowRuinedBrewHints()) {
                    BrewDefect defect = result.getWorstDefect();
                    assert defect != null; // since no recipe was found, there must be a defect
                    lore.updateDefect(BUtil.choose(defect.getMessages(lang)));
                }
                currentRecipe = null;
                potionMeta.setDisplayName(BUtil.color("&f" + lang.getEntry("Brew_BadPotion")));
                PotionColor.GREY.colorBrew(potionMeta, item, canDistill());
            }
        }
        alc = calcAlcohol();
        updateCustomModelData(potionMeta);

        // Lore
        if (currentRecipe != null && config.isColorInBarrels() != BrewLore.hasColorLore(potionMeta)) {
            lore.convertLore(config.isColorInBarrels());
        } else {
            if (ageTime >= 1) {
                lore.updateAgeLore(config.isColorInBarrels());
            }
            if (ageTime > 0.5) {
                if (config.isColorInBarrels()) {
                    lore.updateWoodLore(true);
                    lore.updateIngredientLore(true);
                    lore.updateCookLore(true);
                }
                lore.updateQualityStars(config.isColorInBarrels());
                lore.updateCustomLore();
                lore.updateAlc(false);
            }
        }
        lore.write();
        touch();
        BrewModifyEvent modifyEvent = new BrewModifyEvent(this, potionMeta, BrewModifyEvent.Type.AGE);
        BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(modifyEvent);
        if (modifyEvent.isCancelled()) {
            // As the brew and everything connected to it is only saved on the meta from now on,
            // not saving the brew into potionMeta is enough to not change anything in case of cancel
            return;
        }
        save(potionMeta);
        item.setItemMeta(potionMeta);
    }

    /**
     * Slowly shift the wood of the Brew to the new Type
     */
    public void woodShift(float time, BarrelWoodType to) {
        if (immutable || wood == to) {
            return;
        }
        if (wood == BarrelWoodType.ANY) {
            wood = to;
            return;
        }
        if (config.isNewBarrelTypeAlgorithm()) {
            woodShiftNew(time, to);
            return;
        }

        int fromIndex = wood.getIndex();
        int toIndex = to.getIndex();

        float factor = woodShiftFactor();
        if (fromIndex > toIndex) {
            fromIndex -= (int) (time / factor);
            if (fromIndex < toIndex) {
                wood = to;
            }
        } else {
            fromIndex += (int) (time / factor);
            if (fromIndex > toIndex) {
                wood = to;
            }
        }
    }
    private void woodShiftNew(float time, BarrelWoodType to) {
        BarrelWoodType old = wood;
        float factor = woodShiftFactor();
        float shift = time / factor;
        // If the old algorithm would shift by 2 indexes,
        // shift the barrel type by 1 group (or 1 barrel type within a group)
        int steps = (int) (2.0f * shift);
        wood = wood.stepTowards(to, steps);
        Logging.debugLog(String.format("Shifted wood from %s to %s by %s steps", old, wood, steps));
        Logging.debugLog(String.format("time=%.3f, factor=%.3f, shift=%.3f", time, factor, shift));
    }
    private float woodShiftFactor() {
        float factor = 1;
        if (ageTime > 5) {
            factor = 2;
        }
        if (ageTime > 10) {
            factor += ageTime / 10F;
        }
        return factor;
    }

    public ItemStack createItem() {
        return createItem(null, true, null);
    }

    /**
     * Create a new Item of this Brew. A BrewModifyEvent type CREATE will be called.
     *
     * @param recipe Recipe is required if the brew doesn't have a currentRecipe
     * @return The created Item, null if the Event is cancelled
     */
    public ItemStack createItem(@Nullable BRecipe recipe) {
        return createItem(recipe, true, null);
    }

    public ItemStack createItem(@Nullable BRecipe recipe, @Nullable Player player) {
        return createItem(recipe, true, player);
    }

    public ItemStack createItem(@Nullable BRecipe recipe, boolean event) {
        return createItem(recipe, true, null);
    }

    /**
     * Create a new Item of this Brew.
     *
     * @param recipe Recipe is required if the brew doesn't have a currentRecipe
     * @param event  Set event to true if a BrewModifyEvent type CREATE should be called and may be cancelled. Only then may this method return null
     * @return The created Item, null if the Event is cancelled
     */
    @Contract("_, false -> !null")
    public ItemStack createItem(@Nullable BRecipe recipe, boolean event, @Nullable Player player) {
        if (recipe == null) {
            recipe = getCurrentRecipe();
        }
        if (recipe == null) {
            throw new IllegalArgumentException("Argument recipe can't be null if the brew doesn't have a currentRecipe");
        }
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();

        recipe.getColor().colorBrew(potionMeta, potion, false);
        updateCustomModelData(potionMeta);


        if (recipe.isGlint()) {
            potionMeta.addEnchant(Enchantment.MENDING, 1, true);
            potionMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        potionMeta.setDisplayName(BUtil.color("&f" + recipe.getName(quality)));
        //if (!P.use1_14) {
        // Before 1.14 the effects duration would strangely be only a quarter of what we tell it to be
        // This is due to the Duration Modifier, that is removed in 1.14
        //	uid *= 4;
        //}
        // This effect stores the UID in its Duration
        //potionMeta.addCustomEffect((PotionEffectType.REGENERATION).createEffect((uid * 4), 0), true);

        BrewLore lore = new BrewLore(this, potionMeta);
        lore.convertLore(false);
        lore.addOrReplaceEffects(recipe.getEffects(), quality);
        lore.write();
        touch();
        if (event) {
            BrewModifyEvent modifyEvent = new BrewModifyEvent(this, potionMeta, BrewModifyEvent.Type.CREATE, player);
            BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(modifyEvent);
            if (modifyEvent.isCancelled()) {
                return null;
            }
        }
        save(potionMeta);
        potion.setItemMeta(potionMeta);
        BreweryPlugin.getInstance().getBreweryStats().metricsForCreate(true);
        return potion;
    }

    /**
     * Performant way of checking if this item is a Brew.
     * <p>Does not give any guarantees that get() will return notnull for this item, i.e. if it is a brew but the data is corrupt
     *
     * @param item The Item to check
     * @return True if the item is a brew
     */
    public static boolean isBrew(ItemStack item) {
        if (item == null || item.getType() != Material.POTION) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        if (!MinecraftVersion.isUseNBT() && !meta.hasLore()) return false;

        if (MinecraftVersion.isUseNBT()) {
            // Check for Data on PersistentDataContainer
            if (NBTLoadStream.hasDataInMeta(meta)) {
                return true;
            }
        }
        // If either NBT is not supported or no data was found in NBT, try finding data in lore
        if (meta.hasLore()) {
            // Find the Data Identifier in Lore
            return BUtil.indexOfStart(meta.getLore(), LoreLoadStream.IDENTIFIER) > -1;
        }
        return false;
    }

    private static Brew load(ItemMeta meta) {
        InputStream itemLoadStream = null;
        if (MinecraftVersion.isUseNBT()) {
            // Try loading the Item Data from PersistentDataContainer
            NBTLoadStream nbtStream = new NBTLoadStream(meta);
            if (nbtStream.hasData()) {
                itemLoadStream = nbtStream;
            }
        }
        if (itemLoadStream == null) {
            // If either NBT is not supported or no data was found in NBT, try loading from Lore
            try {
                itemLoadStream = new Base91DecoderStream(new LoreLoadStream(meta, 0));
            } catch (IllegalArgumentException ignored) {
                // No Brew data found in Meta
                return null;
            }
        }

        XORUnscrambleStream unscrambler = new XORUnscrambleStream(itemLoadStream, saveSeed, prevSaveSeeds);
        try (DataInputStream in = new DataInputStream(unscrambler)) {
            boolean parityFailed = false;
            if (in.readByte() != 86) {
                Logging.errorLog("Parity check failed on Brew while loading, trying to load anyways!");
                parityFailed = true;
            }
            Brew brew = new Brew();
            byte ver = in.readByte();
            switch (ver) {
                case 1:

                    unscrambler.start();
                    brew.loadFromStream(in, ver);

                    break;
                default:
                    if (parityFailed) {
                        Logging.errorLog("Failed to load Brew. Maybe something corrupted the Lore of the Item?");
                    } else {
                        Logging.errorLog("Brew has data stored in v" + ver + " this Plugin version supports up to v" + SAVE_VER);
                    }
                    return null;
            }

            XORUnscrambleStream.SuccessType successType = unscrambler.getSuccessType();
            if (successType == XORUnscrambleStream.SuccessType.PREV_SEED) {
                Logging.debugLog("Converting Brew from previous Seed");
                brew.setNeedsSave(true);
            } else if ((config.isEnableEncode() && !brew.isStripped()) != (successType == XORUnscrambleStream.SuccessType.MAIN_SEED)) {
                // We have either enabled encode and the data was not encoded or the other way round
                Logging.debugLog("Converting Brew to new encode setting");
                brew.setNeedsSave(true);
            } else if (MinecraftVersion.isUseNBT() && itemLoadStream instanceof Base91DecoderStream) {
                // We are on a version that supports nbt but the data is still in the lore of the item
                // Just save it again so that it gets saved to nbt
                Logging.debugLog("Converting Brew to NBT");
                brew.setNeedsSave(true);
            }
            return brew;
        } catch (IOException e) {
            Logging.errorLog("IO Error while loading Brew", e);
        } catch (InvalidKeyException e) {
            Logging.errorLog("Failed to load Brew, has the data key 'encodeKey' in the config.yml been changed?", e);
        }
        return null;
    }

    private void loadFromStream(DataInputStream in, byte dataVersion) throws IOException {
        quality = in.readByte();
        int bools = in.readUnsignedByte();
        if ((bools & 64) != 0) {
            alc = in.readShort();
        }
        if ((bools & 1) != 0) {
            distillRuns = in.readByte();
        }
        if ((bools & 2) != 0) {
            ageTime = in.readFloat();
        }
        if ((bools & 4) != 0) {
            wood = BarrelWoodType.fromAny(in.readFloat());
        }
        String recipe = null;
        if ((bools & 8) != 0) {
            recipe = in.readUTF();
        }
        unlabeled = (bools & 16) != 0;
        immutable = (bools & 32) != 0;
        stripped = (bools & 128) != 0;
        ingredients = BIngredients.load(in, dataVersion);
        setRecipeFromString(recipe);
    }

    /**
     * Save brew data into meta: lore/nbt.
     * <p>Should be called after any changes made to the brew
     */
    public void save(ItemMeta meta) {
        OutputStream itemSaveStream;
        if (MinecraftVersion.isUseNBT()) {
            itemSaveStream = new NBTSaveStream(meta);
        } else {
            itemSaveStream = new Base91EncoderStream(new LoreSaveStream(meta, 0));
        }
        XORScrambleStream scrambler = new XORScrambleStream(itemSaveStream, saveSeed);
        try (DataOutputStream out = new DataOutputStream(scrambler)) {
            out.writeByte(86); // Parity/sanity
            out.writeByte(SAVE_VER); // Version
            // If Stripped of data, we can save everything unscrambled
            if (config.isEnableEncode() && !isStripped()) {
                scrambler.start();
            } else {
                scrambler.startUnscrambled();
            }
            saveToStream(out);
        } catch (IOException e) {
            Logging.errorLog("IO Error while saving Brew", e);
        }
    }

    /**
     * Save brew data into the meta/lore of the specified item.
     * <p>The meta on the item changes, so to make further changes to the meta, item.getItemMeta() has to be called again after this
     *
     * @param item The item to save this brew into
     */
    public void save(ItemStack item) {
        ItemMeta meta;
        if (!item.hasItemMeta()) {
            meta = BreweryPlugin.getInstance().getServer().getItemFactory().getItemMeta(item.getType());
        } else {
            meta = item.getItemMeta();
        }
        save(meta);
        item.setItemMeta(meta);
    }

    public void saveToStream(DataOutputStream out) throws IOException {
        if (quality > 10) {
            quality = 10;
        }
        alc = Math.min(alc, Short.MAX_VALUE);
        alc = Math.max(alc, Short.MIN_VALUE);

        out.writeByte((byte) quality);
        int bools = 0;
        bools |= ((distillRuns != 0) ? 1 : 0);
        bools |= (ageTime > 0 ? 2 : 0);
        bools |= (wood != BarrelWoodType.NONE ? 4 : 0);
        bools |= (currentRecipe != null ? 8 : 0);
        bools |= (unlabeled ? 16 : 0);
        bools |= (immutable ? 32 : 0);
        bools |= (alc != 0 ? 64 : 0);
        bools |= (stripped ? 128 : 0);
        out.writeByte(bools);
        if (alc != 0) {
            out.writeShort(alc);
        }
        if (distillRuns != 0) {
            out.writeByte(distillRuns);
        }
        if (ageTime > 0) {
            out.writeFloat(ageTime);
        }
        if (wood != BarrelWoodType.NONE) {
            out.writeFloat(wood != null ? wood.getIndex() : 0);
        }
        if (currentRecipe != null) {
            out.writeUTF(currentRecipe.getRecipeName());
        }
        ingredients.save(out);
    }

    public static void loadSeed(long seed) {
        saveSeed = seed;
        updatePrevSeeds();
    }

    public static void loadPrevSeeds(ConfigurationSection section) {
        if (section.contains("prevSaveSeeds")) {
            prevSaveSeeds = section.getLongList("prevSaveSeeds");
            updatePrevSeeds();
        }
    }

    public static void loadPrevSeeds(List<Long> list) {
        prevSaveSeeds = list;
        updatePrevSeeds();
    }

    private static void updatePrevSeeds() {
        if (!prevSaveSeeds.contains(saveSeed)) {
            prevSaveSeeds.add(saveSeed);
        }
    }

    public static List<Long> getPrevSeeds() {
        return prevSaveSeeds;
    }


    public static boolean noLegacy() {
        return legacyPotions.isEmpty();
    }

    /**
     * Load potion data from data file for backwards compatibility
     */
    public static void loadLegacy(BIngredients ingredients, int uid, int quality, int alc, byte distillRuns, float ageTime, BarrelWoodType wood, String recipe, boolean unlabeled, boolean persistent, boolean stat, int lastUpdate) {
        Brew brew = new Brew(ingredients, quality, alc, distillRuns, ageTime, wood, recipe, unlabeled, stat, lastUpdate);
        brew.persistent = persistent;
        if (brew.lastUpdate <= 0) {
            // We failed to save the lastUpdate, restart the countdown
            brew.touch();
        }
        legacyPotions.put(uid, brew);
    }

    /**
     * remove legacy potiondata for an item
     */
    public static void removeLegacy(ItemStack item) {
        if (legacyPotions.isEmpty()) return;
        if (!item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof PotionMeta)) return;
        getFromPotionEffect(((PotionMeta) meta), true);
    }

    public void convertPre1_9(ItemStack item) {
        removeLegacy(item);
        PotionMeta potionMeta = ((PotionMeta) item.getItemMeta());
        assert potionMeta != null;

        BrewLore lore = new BrewLore(this, potionMeta);
        lore.removeEffects();

        if (hasRecipe()) {
            currentRecipe.getColor().colorBrew(potionMeta, item, canDistill());
        } else {
            PotionColor.GREY.colorBrew(potionMeta, item, canDistill());
        }
        lore.removeLegacySpacing();
        save(potionMeta);
        item.setItemMeta(potionMeta);
    }

    public void convertPre1_11(ItemStack item) {
        removeLegacy(item);
        PotionMeta potionMeta = ((PotionMeta) item.getItemMeta());
        assert potionMeta != null;

        potionMeta.setBasePotionType(PotionType.THICK); // UNCRAFTABLE
        BrewLore lore = new BrewLore(this, potionMeta);
        lore.removeEffects();

        if (hasRecipe()) {
            lore.addOrReplaceEffects(currentRecipe.getEffects(), getQuality());
            currentRecipe.getColor().colorBrew(potionMeta, item, canDistill());
        } else {
            PotionColor.GREY.colorBrew(potionMeta, item, canDistill());
        }
        lore.removeLegacySpacing();
        save(potionMeta);
        item.setItemMeta(potionMeta);
    }

    /**
     * Saves all data,
     * Legacy method to save to data file.
     */
    public static void saveLegacy(ConfigurationSection config) {
        for (Map.Entry<Integer, Brew> entry : legacyPotions.entrySet()) {
            int uid = entry.getKey();
            Brew brew = entry.getValue();
            ConfigurationSection idConfig = config.createSection("" + uid);
            // not saving unneccessary data
            if (brew.quality != 0) {
                idConfig.set("quality", brew.quality);
            }
            if (brew.alc != 0) {
                idConfig.set("alc", brew.alc);
            }
            if (brew.distillRuns != 0) {
                idConfig.set("distillRuns", brew.distillRuns);
            }
            if (brew.ageTime != 0) {
                idConfig.set("ageTime", brew.ageTime);
            }
            if (brew.wood != BarrelWoodType.NONE) {
                idConfig.set("wood", brew.wood);
            }
            if (brew.currentRecipe != null) {
                idConfig.set("recipe", brew.currentRecipe.getRecipeName());
            }
            if (brew.unlabeled) {
                idConfig.set("unlabeled", true);
            }
            if (brew.persistent) {
                idConfig.set("persist", true);
            }
            if (brew.immutable) {
                idConfig.set("stat", true);
            }
            if (brew.lastUpdate > 0) {
                idConfig.set("lastUpdate", brew.lastUpdate);
            }
            // save the ingredients
            idConfig.set("ingId", brew.ingredients.saveLegacy(config.getParent()));
        }
    }

}
