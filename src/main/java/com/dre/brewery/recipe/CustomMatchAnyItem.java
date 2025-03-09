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

package com.dre.brewery.recipe;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Custom Item that matches any one of the given info.
 * <p>Does not implement Ingredient, as it can not directly be added to an ingredient
 */
public class CustomMatchAnyItem extends RecipeItem {

    private List<Material> materials;
    private List<String> names;
    private List<String> lore;
    private List<Integer> customModelDatas;


    @Override
    public boolean hasMaterials() {
        return materials != null && !materials.isEmpty();
    }

    public boolean hasNames() {
        return names != null && !names.isEmpty();
    }

    public boolean hasLore() {
        return lore != null && !lore.isEmpty();
    }

    public boolean hasCustomModelDatas() {
        return customModelDatas != null && !customModelDatas.isEmpty();
    }

    @Override
    @Nullable
    public List<Material> getMaterials() {
        return materials;
    }

    protected void setMaterials(List<Material> materials) {
        this.materials = materials;
    }

    @Nullable
    public List<String> getNames() {
        return names;
    }

    protected void setNames(List<String> names) {
        this.names = names;
    }

    @Nullable
    public List<String> getLore() {
        return lore;
    }

    protected void setLore(List<String> lore) {
        this.lore = lore;
    }

    @Nullable
    public List<Integer> getCustomModelDatas() {
        return customModelDatas;
    }

    protected void setCustomModelDatas(List<Integer> customModelDatas) {
        this.customModelDatas = customModelDatas;
    }


    @NotNull
    @Override
    public Ingredient toIngredient(ItemStack forItem) {
        // We only use the one part of this item that actually matched the given item to add to ingredients
        Material mat = getMaterialMatch(forItem);
        if (mat != null) {
            return new CustomItem(mat);
        }
        String name = getNameMatch(forItem);
        if (name != null) {
            return new CustomItem(null, name, null);
        }
        String l = getLoreMatch(forItem);
        if (l != null) {
            List<String> lore = new ArrayList<>(1);
            lore.add(l);
            return new CustomItem(null, null, lore);
        }
        Integer cmData = getCustomModelDataMatch(forItem);
        if (cmData != null) {
            return new CustomItem(null, null, null, cmData);
        }

        // Shouldnt happen
        return new SimpleItem(Material.GOLDEN_HOE);
    }

    @NotNull
    @Override
    public Ingredient toIngredientGeneric() {
        if (hasMaterials()) {
            return new CustomItem(materials.get(0));
        }
        if (hasNames()) {
            return new CustomItem(null, names.get(0), null);
        }
        if (hasLore()) {
            return new CustomItem(null, null, new ArrayList<>(List.of(lore.get(0))));
        }
        if (hasCustomModelDatas()) {
            return new CustomItem(null, null, null, customModelDatas.get(0));
        }

        // Shouldnt happen
        return new SimpleItem(Material.GOLDEN_HOE);
    }

    public Material getMaterialMatch(ItemStack item) {
        if (!hasMaterials()) return null;

        Material usedMat = item.getType();
        for (Material mat : materials) {
            if (usedMat == mat) {
                return mat;
            }
        }
        return null;
    }

    public String getNameMatch(ItemStack item) {
        if (!item.hasItemMeta() || !hasNames()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        if (meta.hasDisplayName()) {
            return getNameMatch(meta.getDisplayName());
        }
        return null;
    }

    public String getNameMatch(String usedName) {
        if (!hasNames()) return null;

        for (String name : names) {
            if (name.equalsIgnoreCase(usedName)) {
                return name;
            }
        }
        return null;
    }

    public String getLoreMatch(ItemStack item) {
        if (!item.hasItemMeta() || !hasLore()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        if (meta.hasLore()) {
            return getLoreMatch(meta.getLore());
        }
        return null;
    }

    public String getLoreMatch(List<String> usedLore) {
        if (!hasLore()) return null;

        for (String line : this.lore) {
            for (String usedLine : usedLore) {
                if (line.equalsIgnoreCase(usedLine) || line.equalsIgnoreCase(ChatColor.stripColor(usedLine))) {
                    return line;
                }
            }
        }
        return null;
    }

    @Nullable
    public Integer getCustomModelDataMatch(ItemStack item) {
        if (!item.hasItemMeta() || !hasCustomModelDatas()) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        if (meta.hasCustomModelData()) {
            return getCustomModelDataMatch(meta.getCustomModelData());
        }
        return null;
    }

    @Nullable
    public Integer getCustomModelDataMatch(int usedCustomModelData) {
        if (!hasCustomModelDatas()) return null;

        for (int customModelData : this.customModelDatas) {
            if (customModelData == usedCustomModelData) {
                return customModelData;
            }
        }
        return null;
    }

    @Override
    public boolean matches(ItemStack item) {
        if (getMaterialMatch(item) != null) {
            return true;
        }
        if (getNameMatch(item) != null) {
            return true;
        }
        if (getLoreMatch(item) != null) {
            return true;
        }
        return getCustomModelDataMatch(item) != null;
    }

    @Override
    public boolean matches(Ingredient ingredient) {
        // Ingredient can not be CustomMatchAnyItem, so we don't need to/can't check for similarity.
        if (ingredient instanceof CustomItem) {
            // If the custom item has any of our data, we match
            CustomItem ci = ((CustomItem) ingredient);
            if (hasMaterials() && ci.hasMaterials()) {
                if (materials.contains(ci.getMaterial())) {
                    return true;
                }
            }
            if (hasNames() && ci.hasName()) {
                if (getNameMatch(ci.getName()) != null) {
                    return true;
                }
            }
            if (hasLore() && ci.hasLore()) {
                return getLoreMatch(ci.getLore()) != null;
            }
            if (hasCustomModelDatas() && ci.hasCustomModelData()) {
                return getCustomModelDataMatch(ci.getCustomModelData()) != null;
            }
        } else if (ingredient instanceof SimpleItem si) {
            // If we contain the Material of the Simple Item, we match
            return hasMaterials() && materials.contains(si.getMaterial());
        }
        return false;
    }

    @Override
    public String getDebugID() {
        String id = getConfigId();
        return id != null ? id : String.join("|", names);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        CustomMatchAnyItem that = (CustomMatchAnyItem) o;
        return Objects.equals(materials, that.materials) &&
            Objects.equals(names, that.names) &&
            Objects.equals(lore, that.lore) &&
            Objects.equals(customModelDatas, that.customModelDatas);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), materials, names, lore, customModelDatas);
    }

    @Override
    public String toString() {
        return "CustomMatchAnyItem{" +
            "id=" + getConfigId() +
            ", materials: " + (materials != null ? materials.size() : 0) +
            ", names:" + (names != null ? names.size() : 0) +
            ", loresize: " + (lore != null ? lore.size() : 0) +
            ", customModelDatas: " + (customModelDatas != null ? customModelDatas.size() : 0) +
            '}';
    }
}
