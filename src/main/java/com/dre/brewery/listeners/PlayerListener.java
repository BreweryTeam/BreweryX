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

package com.dre.brewery.listeners;

import com.dre.brewery.*;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.utility.*;
import com.dre.brewery.utility.MaterialUtil;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;


public class PlayerListener implements Listener {

	private static final MinecraftVersion VERSION = BreweryPlugin.getMCVersion();
	private static final Config config = ConfigManager.getConfig(Config.class);
	private static final Lang lang = ConfigManager.getConfig(Lang.class);

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		handlePlayerInteract(event);
	}

	public static void handlePlayerInteract(PlayerInteractEvent event) {
		Block clickedBlock = event.getClickedBlock();
		if (clickedBlock == null) return;

		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

		Player player = event.getPlayer();
		Material type = clickedBlock.getType();

		// -- Clicking an Hopper --
		if (type == Material.HOPPER) {
			if (config.isBrewHopperDump() && event.getPlayer().isSneaking()) {
				if (VERSION.isOrEarlier(MinecraftVersion.V1_9) || event.getHand() == EquipmentSlot.HAND) {
					ItemStack item = event.getItem();
					if (Brew.isBrew(item)) {
						event.setCancelled(true);
						BUtil.setItemInHand(event, Material.GLASS_BOTTLE, false);
						if (VERSION.isOrLater(MinecraftVersion.V1_11)) {
							clickedBlock.getWorld().playSound(clickedBlock.getLocation(), Sound.ITEM_BOTTLE_EMPTY, 1f, 1f);
						}
					}
				}
			}
			return;
		}

		// -- Opening a Sealing Table --
		if (VERSION.isOrLater(MinecraftVersion.V1_14) && BSealer.isBSealer(clickedBlock)) {
			if (player.isSneaking()) {
				event.setUseInteractedBlock(Event.Result.DENY);
				return;
			}
			event.setCancelled(true);
			if (config.isEnableSealingTable()) {
				BSealer sealer = new BSealer(player);
				event.getPlayer().openInventory(sealer.getInventory());
			} else {
				lang.sendEntry(player, "Error_SealingTableDisabled");
			}
			return;
		}

		if (player.isSneaking()) return;

		// -- Interacting with a Cauldron --
		if (MaterialUtil.isWaterCauldron(type)) {
			// Handle the Cauldron Interact
			// The Event might get cancelled in here
			BCauldron.clickCauldron(event);
			return;
		}

		// -- Opening a Minecraft Barrel --
		if (VERSION.isOrLater(MinecraftVersion.V1_14) && type == Material.BARREL) {
			if (!player.hasPermission("brewery.openbarrel.mc")) {
				event.setCancelled(true);
				lang.sendEntry(player, "Error_NoPermissions");
			}
			return;
		}

		// Do not process Off Hand for Barrel interaction
		if (VERSION.isOrLater(MinecraftVersion.V1_9) && event.getHand() != EquipmentSlot.HAND) {
			return;
		}

		// -- Access a Barrel --
		Barrel barrel = null;
		if (BarrelAsset.isBarrelAsset(BarrelAsset.PLANKS, type)) {
			if (config.isOpenLargeBarrelEverywhere()) {
				barrel = Barrel.getByWood(clickedBlock);
			}
		} else if (BarrelAsset.isBarrelAsset(BarrelAsset.STAIRS, type)) {
			barrel = Barrel.getByWood(clickedBlock);
			if (barrel != null) {
				if (!config.isOpenLargeBarrelEverywhere() && barrel.isLarge()) {
					barrel = null;
				}
			}
		} else if (BarrelAsset.isBarrelAsset(BarrelAsset.FENCE, type) || BarrelAsset.isBarrelAsset(BarrelAsset.SIGN, type)) {
			barrel = Barrel.getBySpigot(clickedBlock);
		}

		if (barrel != null) {
			event.setCancelled(true);

			if (!barrel.hasPermsOpen(player, event)) {
				return;
			}

			barrel.open(player);

			if (VERSION.isOrLater(MinecraftVersion.V1_14)) {

				// When right clicking a normal Block in 1.14 with a potion or any edible item in hand,
				// even when cancelled, the consume animation will continue playing while opening the Barrel inventory.
				// The Animation and sound will play endlessly while the inventory is open, though no item is consumed.
				// This seems to be a client bug.
				// This workaround switches the currently selected slot to another for a short time, it needs to be a slot with a different item in it.
				// This seems to make the client stop animating a consumption
				// If there is a better way to do this please let me know
				Material hand = event.getMaterial();
				if ((hand == Material.POTION || hand.isEdible()) && !BarrelAsset.isBarrelAsset(BarrelAsset.SIGN, type)) {
					PlayerInventory inv = player.getInventory();
					final int held = inv.getHeldItemSlot();
					int useSlot = -1;
					for (int i = 0; i < 9; i++) {
						ItemStack item = inv.getItem(i);
						if (item == null || item.getType() == Material.AIR) {
							useSlot = i;
							break;
						} else if (useSlot == -1 && item.getType() != hand) {
							useSlot = i;
						}
					}
					if (useSlot != -1) {
						inv.setHeldItemSlot(useSlot);
						BreweryPlugin.getScheduler().runTaskLater(() -> player.getInventory().setHeldItemSlot(held), 2);
					}
				}

				barrel.playOpeningSound();
			}
		}
	}

	@EventHandler
	public void onClickAir(PlayerInteractEvent event) {
		if (Wakeup.checkPlayer == null) return;

		if (event.getAction() == Action.LEFT_CLICK_AIR) {
			if (!event.hasItem()) {
				if (event.getPlayer() == Wakeup.checkPlayer) {
					Wakeup.tpNext();
				}
			}
		}
	}

	// player drinks a custom potion
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
		Player player = event.getPlayer();
		ItemStack item = event.getItem();
		if (item != null) {
			if (item.getType() == Material.POTION) {
				Brew brew = Brew.get(item);
				if (brew != null) {
					if (!BPlayer.drink(brew, item.getItemMeta(), player)) {
						event.setCancelled(true);
						return;
					}
					/*if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
						brew.remove(item);
					}*/
					if (VERSION.isOrLater(MinecraftVersion.V1_9)) {
						if (player.getGameMode() != GameMode.CREATIVE) {
							// replace the potion with an empty potion to avoid effects
							event.setItem(new ItemStack(Material.POTION));
						} else {
							// Don't replace the item when keeping the potion, just cancel the event
							event.setCancelled(true);
						}
					}
				}
			} else if (BUtil.getMaterialMap(config.getDrainItems()).containsKey(item.getType())) {
				BPlayer bplayer = BPlayer.get(player);
				if (bplayer != null) {
					bplayer.drainByItem(player, item.getType());
					if (config.isShowStatusOnDrink()) {
						bplayer.showDrunkeness(player);
					}
				}
			}
		}
	}

	// Player has died! Decrease Drunkeness by 20
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		BPlayer bPlayer = BPlayer.get(event.getPlayer());
		if (bPlayer != null) {
			if (bPlayer.getDrunkeness() > 20) {
				bPlayer.setData(bPlayer.getDrunkeness() - 20, 0);
			} else {
				BPlayer.remove(event.getPlayer());
			}
		}
	}

	// player walks while drunk, push him around!
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (BPlayer.hasPlayer(event.getPlayer())) {
			BPlayer.playerMove(event);
		}
	}

	// player talks while drunk, but he cant speak very well
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		DistortChat.playerChat(event);
	}

	// player commands while drunk, distort chat commands
	@EventHandler(priority = EventPriority.LOWEST)
	public void onCommandPreProcess(PlayerCommandPreprocessEvent event) {
		DistortChat.playerCommand(event);
	}

	// player joins while passed out
	@EventHandler
	public void onPlayerLogin(PlayerLoginEvent event) {
		if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
			return;
		}
		Player player = event.getPlayer();
		BPlayer bplayer = BPlayer.get(player);
		if (bplayer == null) {
			return;
		}
		if (player.hasPermission("brewery.bypass.logindeny")) {
			if (bplayer.getDrunkeness() > 100) {
				bplayer.setData(100, 0);
			}
			return;
		}
		switch (bplayer.canJoin()) {
			case 2 ->
					event.disallow(PlayerLoginEvent.Result.KICK_OTHER, lang.getEntry("Player_LoginDeny"));
			case 3 ->
					event.disallow(PlayerLoginEvent.Result.KICK_OTHER, lang.getEntry("Player_LoginDenyLong"));
		}
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		UpdateChecker.notify(event.getPlayer());
		BPlayer bplayer = BPlayer.get(event.getPlayer());
		if (bplayer != null) {
			bplayer.join(event.getPlayer());
		}
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		BPlayer bplayer = BPlayer.get(event.getPlayer());
		if (bplayer != null) {
			bplayer.disconnecting();
		}
		PermissionUtil.logout(event.getPlayer());
	}

	@EventHandler
	public void onPlayerKick(PlayerKickEvent event) {
		BPlayer bplayer = BPlayer.get(event.getPlayer());
		if (bplayer != null) {
			bplayer.disconnecting();
		}
		PermissionUtil.logout(event.getPlayer());
	}
}
