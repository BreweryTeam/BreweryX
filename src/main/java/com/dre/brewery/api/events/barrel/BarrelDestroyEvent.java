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

package com.dre.brewery.api.events.barrel;

import com.dre.brewery.Barrel;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A Barrel is being destroyed by something, may not be by a Player.
 * <p>A BarrelRemoveEvent will be called after this, if this is not cancelled
 * <p>Use the BarrelRemoveEvent to monitor any and all barrels being removed in a non cancellable way
 * <p>Cancelling the Event will stop the barrel from being destroyed
*/
public class BarrelDestroyEvent extends BarrelEvent implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final Block broken;
	private final Reason reason;
	private final Player player;
	private boolean cancelled;

	public BarrelDestroyEvent(Barrel barrel, Block broken, Reason reason, Player player) {
		super(barrel);
		this.broken = broken;
		this.player = player;
		this.reason = reason;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * Cancelling the Event will stop the barrel from being destroyed.
	 * Any Blocks that are part of the barrel will not be destroyed
	 */
	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	/**
	 * @return The Block of the Barrel that was broken
	 */
	public Block getBroken() {
		return broken;
	}

	/**
	 * @return The Reason of destruction of this barrel, see Reason
	 */
	public Reason getReason() {
		return reason;
	}

	/**
	 * If a Player was recorded destroying the barrel
	 */
	public boolean hasPlayer() {
		return player != null;
	}

	/**
	 * @return The Player, Null if no Player is involved
	 */
	@Nullable
	public Player getPlayerOptional() {
		return player;
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

	/**
	 * The Reason why the Barrel is being destroyed.
	 */
	public enum Reason {
		/**
		 * A Player Broke the Barrel
		 */
		PLAYER,

		/**
		 * A Block was broken by something
		 */
		BROKEN,

		/**
		 * A Block burned away
		 */
		BURNED,

		/**
		 * The Barrel exploded somehow
		 */
		EXPLODED,

		/**
		 * The Barrel was broken somehow else
		 */
		UNKNOWN
	}
}
