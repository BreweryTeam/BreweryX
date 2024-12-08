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

package com.dre.brewery.api.events;

import com.dre.brewery.BPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * The player pukes (throws puke items to the ground).
 * <p>Those items can never be picked up and despawn after the time set in the config
 * <p>Number of items to drop can be changed with count
 */
public class PlayerPukeEvent extends PlayerEvent implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private int count;
	private boolean cancelled;
	private BPlayer bPlayer;


	public PlayerPukeEvent(Player who, int count) {
		super(who);
		this.count = count;
	}

	/**
	 * Get the Amount of items being dropped this time
	 */
	public int getCount() {
		return count;
	}

	/**
	 * Set the amount of items being dropped this time
	 */
	public void setCount(int count) {
		this.count = count;
	}

	public BPlayer getBPlayer() {
		if (bPlayer == null) {
			bPlayer = BPlayer.get(player);
		}
		return bPlayer;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	// Required by Bukkit
	public static HandlerList getHandlerList() {
		return handlers;
	}

}
