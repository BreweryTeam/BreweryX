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
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * The Players movement is hindered because of drunkenness.
 * <p>Called each time before pushing the Player with the Vector push 10 times
 * <p>The Push Vector can be changed or multiplied
 */
public class PlayerPushEvent extends PlayerEvent implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final BPlayer bPlayer;
	private Vector push;
	private boolean cancelled;

	public PlayerPushEvent(Player who, Vector push, BPlayer bPlayer) {
		super(who);
		this.push = push;
		this.bPlayer = bPlayer;
	}

	public BPlayer getBPlayer() {
		return bPlayer;
	}

	/**
	 * Get the Vector in which direction and magnitude the player is pushed.
	 * <p>Can be changed directly or through setPush
	 *
	 * @return The current push vector
	 */
	public Vector getPush() {
		return push;
	}

	/**
	 * Set the Push vector.
	 *
	 * @param push The new push vector, not null
	 */
	public void setPush(@NotNull Vector push) {
		if (push == null) {
			throw new NullPointerException("Push Vector is null");
		}
		this.push = push;
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
