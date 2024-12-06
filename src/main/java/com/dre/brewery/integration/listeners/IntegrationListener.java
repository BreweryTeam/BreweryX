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

package com.dre.brewery.integration.listeners;

import com.dre.brewery.Barrel;
import com.dre.brewery.BarrelAsset;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.api.events.barrel.BarrelAccessEvent;
import com.dre.brewery.api.events.barrel.BarrelDestroyEvent;
import com.dre.brewery.api.events.barrel.BarrelRemoveEvent;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.integration.Hook;
import com.dre.brewery.integration.BlockLockerHook;
import com.dre.brewery.integration.WorldGuarkHook;
import com.dre.brewery.integration.barrel.BlockLockerBarrel;
import com.dre.brewery.integration.barrel.GriefPreventionBarrel;
import com.dre.brewery.integration.barrel.LWCBarrel;
import com.dre.brewery.integration.barrel.LogBlockBarrel;
import com.dre.brewery.integration.barrel.TownyBarrel;
import com.dre.brewery.integration.item.MMOItemsPluginItem;
import com.dre.brewery.listeners.PlayerListener;
import com.dre.brewery.recipe.BCauldronRecipe;
import com.dre.brewery.recipe.RecipeItem;
import com.dre.brewery.utility.MaterialUtil;
import com.dre.brewery.utility.Logging;
import com.dre.brewery.utility.MinecraftVersion;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.Plugin;

public class IntegrationListener implements Listener {

	private final Config config = ConfigManager.getConfig(Config.class);
	private final Lang lang = ConfigManager.getConfig(Lang.class);

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBarrelAccessLowest(BarrelAccessEvent event) {
		WorldGuarkHook hook = WorldGuarkHook.WORLDGUARD;
		if (hook.isEnabled()) {
			Plugin plugin = hook.getPlugin();
			if (plugin != null) {
				try {
					if (!hook.getWgBarrel().checkAccess(event.getPlayer(), event.getSpigot(), plugin)) {
						event.setCancelled(true);
						lang.sendEntry(event.getPlayer(), "Error_NoBarrelAccess");
					}
				} catch (Throwable e) {
					event.setCancelled(true);
					Logging.errorLog("Failed to Check WorldGuard for Barrel Open Permissions!", e);
					Logging.errorLog("Brewery was tested with version 5.8, 6.1 to 7.0 of WorldGuard!");
					Logging.errorLog("Disable the WorldGuard support in the config and do /brew reload");
					Player player = event.getPlayer();
					if (player.hasPermission("brewery.admin") || player.hasPermission("brewery.mod")) {
						Logging.msg(player, "&cWorldGuard check Error, Brewery was tested with up to v7.0 of Worldguard");
						Logging.msg(player, "&cSet &7useWorldGuard: false &cin the config and /brew reload");
					} else {
						Logging.msg(player, "&cError opening Barrel, please report to an Admin!");
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBarrelAccess(BarrelAccessEvent event) {
		Hook hook = Hook.GAMEMODEINVENTORIES;
		if (hook.isEnabled()) {
			Plugin pl = hook.getPlugin();
			if (pl != null && pl.isEnabled()) {
				try {
					if (pl.getConfig().getBoolean("restrict_creative")) {
						Player player = event.getPlayer();
						if (player.getGameMode() == GameMode.CREATIVE) {
							if (!pl.getConfig().getBoolean("bypass.inventories") || (!player.hasPermission("gamemodeinventories.bypass") && !player.isOp())) {
								event.setCancelled(true);
								if (!pl.getConfig().getBoolean("dont_spam_chat")) {
									lang.sendEntry(event.getPlayer(), "Error_NoBarrelAccess");
								}
								return;
							}
						}
					}
				} catch (Throwable e) {
					Logging.errorLog("Failed to Check GameModeInventories for Barrel Open Permissions!", e);
					Logging.errorLog("Players will be able to open Barrel with GameMode Creative");
					hook.setEnabled(false);
				}
			} else {
				hook.setEnabled(false);
			}
		}
		if (Hook.GRIEFPREVENTION.isEnabled()) {
			try {
				if (!GriefPreventionBarrel.checkAccess(event)) {
					lang.sendEntry(event.getPlayer(), "Error_NoBarrelAccess");
					event.setCancelled(true);
					return;
				}
			} catch (Throwable e) {
				event.setCancelled(true);
				Logging.errorLog("Failed to Check GriefPrevention for Barrel Open Permissions!", e);
				Logging.errorLog("Brewery was tested with GriefPrevention v14.5 - v16.9");
				Logging.errorLog("Disable the GriefPrevention support in the config and do /brew reload");
				Player player = event.getPlayer();
				if (player.hasPermission("brewery.admin") || player.hasPermission("brewery.mod")) {
					Logging.msg(player, "&cGriefPrevention check Error, Brewery was tested with up to v16.9 of GriefPrevention");
					Logging.msg(player, "&cSet &7useGriefPrevention: false &cin the config and /brew reload");
				} else {
					Logging.msg(player, "&cError opening Barrel, please report to an Admin!");
				}
				return;
			}
		}

		if (Hook.LWC.isEnabled()) {
			Plugin plugin = BreweryPlugin.getInstance().getServer().getPluginManager().getPlugin("LWC");
			if (plugin != null) {

				// If the Clicked Block was the Sign, LWC already knows and we dont need to do anything here
				if (!BarrelAsset.isBarrelAsset(BarrelAsset.SIGN, event.getClickedBlock().getType())) {
					Block sign = event.getBarrel().getSignOfSpigot();
					// If the Barrel does not have a Sign, it cannot be locked
					if (!sign.equals(event.getClickedBlock())) {
						Player player = event.getPlayer();
						try {
							if (!LWCBarrel.checkAccess(player, sign, plugin)) {
								lang.sendEntry(event.getPlayer(), "Error_NoBarrelAccess");
								event.setCancelled(true);
								return;
							}
						} catch (Throwable e) {
							event.setCancelled(true);
							Logging.errorLog("Failed to Check LWC for Barrel Open Permissions!", e);
							Logging.errorLog("Brewery was tested with version 4.5.0 of LWC!");
							Logging.errorLog("Disable the LWC support in the config and do /brew reload");
							if (player.hasPermission("brewery.admin") || player.hasPermission("brewery.mod")) {
								Logging.msg(player, "&cLWC check Error, Brewery was tested with up to v4.5.0 of LWC");
								Logging.msg(player, "&cSet &7useLWC: false &cin the config and /brew reload");
							} else {
								Logging.msg(player, "&cError opening Barrel, please report to an Admin!");
							}
							return;
						}
					}
				}
			}
		}

		if (Hook.TOWNY.isEnabled()) {
			try {
				if (!TownyBarrel.checkAccess(event)) {
					lang.sendEntry(event.getPlayer(), "Error_NoBarrelAccess");
					event.setCancelled(true);
					return;
				}
			} catch (Throwable e) {
				event.setCancelled(true);
				Logging.errorLog("Failed to Check Towny for Barrel Open Permissions!", e);
				Logging.errorLog("Brewery was tested with Towny v0.96.3.0");
				Logging.errorLog("Disable the Towny support in the config and do /brew reload");
				Player player = event.getPlayer();
				if (player.hasPermission("brewery.admin") || player.hasPermission("brewery.mod")) {
					Logging.msg(player, "&cTowny check Error, Brewery was tested with up to v0.96.3.0 of Towny");
					Logging.msg(player, "&cSet &7useTowny: false &cin the config and /brew reload");
				} else {
					Logging.msg(player, "&cError opening Barrel, please report to an Admin!");
				}
				return;
			}
		}

		if (BlockLockerHook.BLOCKLOCKER.isEnabled()) {
			try {
				if (!BlockLockerBarrel.checkAccess(event)) {
					lang.sendEntry(event.getPlayer(), "Error_NoBarrelAccess");
					event.setCancelled(true);
					return;
				}
			} catch (Throwable e) {
				event.setCancelled(true);
				Logging.errorLog("Failed to Check BlockLocker for Barrel Open Permissions!", e);
				Logging.errorLog("Brewery was tested with BlockLocker v1.9");
				Logging.errorLog("Disable the BlockLocker support in the config and do /brew reload");
				Player player = event.getPlayer();
				if (player.hasPermission("brewery.admin") || player.hasPermission("brewery.mod")) {
					Logging.msg(player, "&cBlockLocker check Error, Brewery was tested with v1.9 of BlockLocker");
					Logging.msg(player, "&cSet &7useBlockLocker: false &cin the config and /brew reload");
				} else {
					Logging.msg(player, "&cError opening Barrel, please report to an Admin!");
				}
				return;
			}
		}

		if (config.isUseVirtualChestPerms()) {
			Player player = event.getPlayer();
			BlockState originalBlockState = event.getClickedBlock().getState();

			event.getClickedBlock().setType(Material.CHEST, false);
			PlayerInteractEvent simulatedEvent = new PlayerInteractEvent(
				player,
				Action.RIGHT_CLICK_BLOCK,
				player.getInventory().getItemInMainHand(),
				event.getClickedBlock(),
				event.getClickedBlockFace(),
				EquipmentSlot.HAND);

			try {
				BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(simulatedEvent);
			} catch (Throwable e) {
				Logging.errorLog("Failed to simulate a Chest for Barrel Open Permissions!", e);
				Logging.errorLog("Disable useVirtualChestPerms in the config and do /brew reload");

				if (player.hasPermission("brewery.admin") || player.hasPermission("brewery.mod")) {
					Logging.msg(player, "&cVirtual Chest Error");
					Logging.msg(player, "&cSet &7useVirtualChestPerms: false &cin the config and /brew reload");
				} else {
					Logging.msg(player, "&cError opening Barrel, please report to an Admin!");
				}
			} finally {
				event.getClickedBlock().setType(Material.AIR, false);
				originalBlockState.update(true);
			}

			if (simulatedEvent.useInteractedBlock() == Event.Result.DENY) {
				event.setCancelled(true);
				lang.sendEntry(event.getPlayer(), "Error_NoBarrelAccess");
				//return;
			}
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
	public void onBarrelDestroy(BarrelDestroyEvent event) {
		if (!Hook.LWC.isEnabled()) return;

		if (event.hasPlayer()) {
			Player player = event.getPlayerOptional();
			assert player != null;
			try {
				if (LWCBarrel.denyDestroy(player, event.getBarrel())) {
					event.setCancelled(true);
				}
			} catch (Throwable e) {
				event.setCancelled(true);
				Logging.errorLog("Failed to Check LWC for Barrel Break Permissions!", e);
				Logging.errorLog("Brewery was tested with version 4.5.0 of LWC!");
				Logging.errorLog("Disable the LWC support in the config and do /brew reload");

				if (player.hasPermission("brewery.admin") || player.hasPermission("brewery.mod")) {
					Logging.msg(player, "&cLWC check Error, Brewery was tested with up to v4.5.0 of LWC");
					Logging.msg(player, "&cSet &7useLWC: false &cin the config and /brew reload");
				} else {
					Logging.msg(player, "&cError breaking Barrel, please report to an Admin!");
				}
			}
		} else {
			try {
				if (event.getReason() == BarrelDestroyEvent.Reason.EXPLODED) {
					if (LWCBarrel.denyExplosion(event.getBarrel())) {
						event.setCancelled(true);
					}
				} else {
					if (LWCBarrel.denyDestroyOther(event.getBarrel())) {
						event.setCancelled(true);
					}
				}
			} catch (Throwable e) {
				event.setCancelled(true);
				Logging.errorLog("Failed to Check LWC on Barrel Destruction!", e);
				Logging.errorLog("Brewery was tested with version 4.5.0 of LWC!");
				Logging.errorLog("Disable the LWC support in the config and do /brew reload");
			}
		}
	}

	@EventHandler
	public void onBarrelRemove(BarrelRemoveEvent event) {
		if (!Hook.LWC.isEnabled()) return;

		try {
			LWCBarrel.remove(event.getBarrel());
		} catch (Throwable e) {
			Logging.errorLog("Failed to Remove LWC Lock from Barrel!", e);
			Logging.errorLog("Brewery was tested with version 4.5.0 of LWC!");
		}
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (Hook.LOGBLOCK.isEnabled()) {
			if (event.getInventory().getHolder() instanceof Barrel) {
				try {
					LogBlockBarrel.closeBarrel(event.getPlayer(), event.getInventory());
				} catch (Exception e) {
					Logging.errorLog("Failed to Log Barrel to LogBlock!", e);
					Logging.errorLog("Brewery was tested with version 1.94 of LogBlock!");
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent event) {
		// Catch the Interact Event early, so MMOItems does not act before us and cancel the event while we try to add it to the Cauldron
		if (BreweryPlugin.getMCVersion().isOrEarlier(MinecraftVersion.V1_9)) return;
		if (!Hook.MMOITEMS.isEnabled()) return;
		try {
			if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.hasItem() && event.getHand() == EquipmentSlot.HAND) {
				if (event.getClickedBlock() != null && MaterialUtil.isWaterCauldron(event.getClickedBlock().getType())) {
					NBTItem item = NBTItem.get(event.getItem());
					if (item.hasType()) {
						for (RecipeItem rItem : BCauldronRecipe.acceptedCustom) {
							if (rItem instanceof MMOItemsPluginItem mmo) {
                                if (mmo.matches(event.getItem())) {
									event.setCancelled(true);
									PlayerListener.handlePlayerInteract(event);
									return;
								}
							}
						}
					}
				}
			}
		} catch (Throwable e) {
			Logging.errorLog("Could not check MMOItems for Item" ,e);
			Hook.MMOITEMS.setEnabled(false);
		}
	}
}
