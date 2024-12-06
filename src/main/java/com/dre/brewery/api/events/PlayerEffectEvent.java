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

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A List of effects is applied to the player.
 * <p>This happens for various reasons like Alcohol level, Brew quality, Brew effects, etc.
 *
 * <p>Can be changed or cancelled
 */
public class PlayerEffectEvent extends PlayerEvent implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final EffectType effectType;
	private List<PotionEffect> effects;
	private boolean cancelled;

	public PlayerEffectEvent(Player who, EffectType effectType, List<PotionEffect> effects) {
		super(who);
		this.effectType = effectType;
		this.effects = effects;
	}

	/**
	 * @return The effects being applied. Effects can be added or removed from this list.
	 */
	public List<PotionEffect> getEffects() {
		return effects;
	}

	public void setEffects(List<PotionEffect> effects) {
		this.effects = effects;
	}

	/**
	 * @return What type of effects are applied, see EffectType
	 */
	public EffectType getEffectType() {
		return effectType;
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


	/**
	 * The Type of Effect, or why an effect is being added to the player.
	 */
	public enum EffectType {
		/**
		 * The Alcohol level demands its toll.
		 * <p>Regularly applied depending on the players alcohol level
		 * <p>By default it is just one Confusion effect
		 */
		ALCOHOL,

		/**
		 *  Effects of a Brew are applied to the player (drinking the Brew).
		 *  <p>These depend on alcohol and quality of the brew
		 */
		DRINK,

		/**
		 * When drinking a Brew with low Quality, these effects are applied.
		 */
		QUALITY,

		/**
		 * When logging in after drinking, Hangover Effects are applied.
		 */
		HANGOVER

	}
}
