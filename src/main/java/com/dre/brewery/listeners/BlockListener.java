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

import com.dre.brewery.BPlayer;
import com.dre.brewery.BSealer;
import com.dre.brewery.Barrel;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.DistortChat;
import com.dre.brewery.api.events.barrel.BarrelDestroyEvent;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.integration.BlockLockerHook;
import com.dre.brewery.integration.Hook;
import com.dre.brewery.integration.barrel.BlockLockerBarrel;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.MinecraftVersion;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;

public class BlockListener implements Listener {

	private static final MinecraftVersion VERSION = BreweryPlugin.getMCVersion();
	private final Config config = ConfigManager.getConfig(Config.class);
	private final Lang lang = ConfigManager.getConfig(Lang.class);

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onSignChange(SignChangeEvent event) {
		String[] lines = event.getLines();

		if (hasBarrelLine(lines) || !config.isRequireKeywordOnSigns()) {
			Player player = event.getPlayer();
			if (!player.hasPermission("brewery.createbarrel.small") && !player.hasPermission("brewery.createbarrel.big")) {
				lang.sendEntry(player, "Perms_NoBarrelCreate");
				return;
			}
			if (Barrel.create(event.getBlock(), player)) {
				lang.sendEntry(player, "Player_BarrelCreated");
			}
		}
	}

	public boolean hasBarrelLine(String[] lines) {
		for (String line : lines) {
			if (line.equalsIgnoreCase("Barrel") || line.equalsIgnoreCase(lang.getEntry("Etc_Barrel"))) {
				return true;
			}
		}
		return false;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onSignChangeLow(SignChangeEvent event) {
		if (config.isDistortSignText()) {
			if (BPlayer.hasPlayer(event.getPlayer())) {
				DistortChat.signWrite(event);
			}
		}
		if (BlockLockerHook.BLOCKLOCKER.isEnabled()) {
			String[] lines = event.getLines();
			if (hasBarrelLine(lines) || !config.isRequireKeywordOnSigns()) {
				BlockLockerBarrel.createdBarrelSign(event.getBlock());
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		if (VERSION.isOrEarlier(MinecraftVersion.V1_14) || event.getBlock().getType() != config.getSealingTableBlock()) return;
		BSealer.blockPlace(event.getItemInHand(), event.getBlock());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (!BUtil.blockDestroy(event.getBlock(), event.getPlayer(), BarrelDestroyEvent.Reason.PLAYER)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		if (!BUtil.blockDestroy(event.getBlock(), null, BarrelDestroyEvent.Reason.BURNED)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPistonRetract(BlockPistonRetractEvent event) {
		if (event.isSticky()) {
			for (Block block : event.getBlocks()) {
				if (Barrel.get(block) != null) {
					event.setCancelled(true);
					return;
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPistonExtend(BlockPistonExtendEvent event) {
		for (Block block : event.getBlocks()) {
			if (Barrel.get(block) != null) {
				event.setCancelled(true);
				return;
			}
		}
	}
}
