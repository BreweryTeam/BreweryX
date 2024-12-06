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

import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.utility.MinecraftVersion;
import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * The Sealing Inventory that is being checked for Brews and seals them after a second.
 * <p>Class doesn't load in mc 1.12 and lower (Can't find RecipeChoice, BlockData and NamespacedKey)
 */
public class BSealer implements InventoryHolder {
	public static final NamespacedKey TAG_KEY = new NamespacedKey(BreweryPlugin.getInstance(), "SealingTable");
	public static final NamespacedKey LEGACY_TAG_KEY = new NamespacedKey("brewery", "sealingtable");
	public static boolean inventoryHolderWorking = true;

	private static final Config config = ConfigManager.getConfig(Config.class);
	private static final Lang lang = ConfigManager.getConfig(Lang.class);

	private final Inventory inventory;
	private final Player player;
	private final short[] slotTime = new short[9];
	private ItemStack[] contents = null;
	private MyScheduledTask task;

	public BSealer(Player player) {
		this.player = player;
		if (inventoryHolderWorking) {
			Inventory inv = BreweryPlugin.getInstance().getServer().createInventory(this, InventoryType.DISPENSER, lang.getEntry("Etc_SealingTable"));
			// Inventory Holder (for DISPENSER, ...) is only passed in Paper, not in Spigot. Doing inventory.getHolder() will return null in spigot :/
			if (inv.getHolder() == this) {
				inventory = inv;
				return;
			} else {
				inventoryHolderWorking = false;
			}
		}
		inventory = BreweryPlugin.getInstance().getServer().createInventory(this, 9, lang.getEntry("Etc_SealingTable"));
	}

	@Override
	public @NotNull Inventory getInventory() {
		return inventory;
	}


	public void clickInv() {
		contents = null;
		if (task == null) {
			task = BreweryPlugin.getScheduler().runTaskTimer(BreweryPlugin.getInstance(), this::itemChecking, 1, 1);
		}
	}

	public void closeInv() {
		if (task != null) {
			task.cancel();
			task = null;
		}
		contents = inventory.getContents();
		for (ItemStack item : contents) {
			if (item != null && item.getType() != Material.AIR) {
				player.getWorld().dropItemNaturally(player.getLocation(), item);
			}
		}
		contents = null;
		inventory.clear();
	}

	private void itemChecking() {
		if (contents == null) {
			contents = inventory.getContents();
			for (int i = 0; i < slotTime.length; i++) {
				if (contents[i] == null || contents[i].getType() != Material.POTION) {
					slotTime[i] = -1;
				} else if (slotTime[i] < 0) {
					slotTime[i] = 0;
				}
			}
		}
		boolean playerValid = player.isValid() && !player.isDead();
		for (int i = 0; i < slotTime.length; i++) {
			if (slotTime[i] > 20) {
				slotTime[i] = -1;
				Brew brew = Brew.get(contents[i]);
				if (brew != null && !brew.isStripped()) {
					brew.seal(contents[i]);
					if (playerValid && BreweryPlugin.getMCVersion().isOrLater(MinecraftVersion.V1_9)) {
						player.playSound(player.getLocation(), Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, 1, 1.5f + (float) (Math.random() * 0.2));
					}
				}
			} else if (slotTime[i] >= 0) {
				slotTime[i]++;
			}
		}
	}

	public static boolean isBSealer(Block block) {
		if (BreweryPlugin.getMCVersion().isOrLater(MinecraftVersion.V1_14) && block.getType() == config.getSealingTableBlock()) {
			Container container = (Container) block.getState();
			if (container.getCustomName() != null) {
				if (container.getCustomName().equals("§e" + lang.getEntry("Etc_SealingTable"))) {
					return true;
				} else {
					return container.getPersistentDataContainer().has(TAG_KEY, PersistentDataType.BYTE) || container.getPersistentDataContainer().has(LEGACY_TAG_KEY, PersistentDataType.BYTE);
				}
			}
		}
		return false;
	}

	public static void blockPlace(ItemStack item, Block block) {
		if (item.getType() == config.getSealingTableBlock() && item.hasItemMeta()) {
			ItemMeta itemMeta = item.getItemMeta();
			assert itemMeta != null;
			if ((itemMeta.hasDisplayName() && itemMeta.getDisplayName().equals("§e" + lang.getEntry("Etc_SealingTable"))) ||
				itemMeta.getPersistentDataContainer().has(BSealer.TAG_KEY, PersistentDataType.BYTE)) {
				Container container = (Container) block.getState();
				// Rotate the Block 180° so it looks different
				if (container.getBlockData() instanceof Directional dir) {
					dir.setFacing(dir.getFacing().getOppositeFace());
					container.setBlockData(dir);
				}				
				container.getPersistentDataContainer().set(BSealer.TAG_KEY, PersistentDataType.BYTE, (byte)1);
				container.update();
			}
		}
	}

	public static void registerRecipe() {
		// Register Sealing Table Recipe
		if (!config.isCraftSealingTable() && recipeExists()) {
			unregisterRecipe();
			return;
		} else if (!config.isCraftSealingTable() || recipeExists() || BreweryPlugin.getMCVersion().isOrEarlier(MinecraftVersion.V1_13)) {
			return;
		}

		ItemStack sealingTableItem = new ItemStack(config.getSealingTableBlock());
		ItemMeta meta = BreweryPlugin.getInstance().getServer().getItemFactory().getItemMeta(config.getSealingTableBlock());
		if (meta == null) return;
		meta.setDisplayName("§e" + lang.getEntry("Etc_SealingTable"));
		meta.getPersistentDataContainer().set(TAG_KEY, PersistentDataType.BYTE, (byte)1);
		sealingTableItem.setItemMeta(meta);

		ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(BreweryPlugin.getInstance(), "SealingTable"), sealingTableItem);
		recipe.shape("bb ",
					 "ww ",
					 "ww ");
		recipe.setIngredient('b', Material.GLASS_BOTTLE);
		recipe.setIngredient('w', new RecipeChoice.MaterialChoice(Tag.PLANKS));

		Bukkit.getServer().addRecipe(recipe);
	}

	public static boolean recipeExists() {
		Recipe recipe = Bukkit.getRecipe(TAG_KEY);
		return recipe != null;
	}

	public static void unregisterRecipe() {
		Recipe recipe = Bukkit.getRecipe(TAG_KEY);
		if (recipe != null) {
			Bukkit.removeRecipe(TAG_KEY);
		}
	}
}
