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

package com.dre.brewery.api.addons;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.commands.CommandManager;
import com.dre.brewery.utility.MinecraftVersion;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry and exit point for a BreweryX addon. Addon classes should be annotated with {@link AddonInfo}.
 * Addons should also include their own Spigot, Paper, or Folia API they intend to write against.
 * Avoid including unnecessary dependencies since they WILL slow the loading process of BreweryX.
 * Addons should not include any Kotlin library, BreweryX will load kotlin's lib through Spigot's library loader
 * automatically.
 * <p>
 * If an addon requires another plugin to function, it should check if the plugin is enabled using an extension of {@link com.dre.brewery.integration.Hook}
 * in methods that directly access the plugin's code, or after the server has finished starting up in the onAddonEnable method by using a runnable.
 * <p>
 * Addons should NOT use the scheduler of the server API they are writing against. They should use the scheduler provided by BreweryX because BreweryX may
 * run on Bukkit-like or Folia-like servers. The scheduler provided by BreweryX is a wrapper around the server API's scheduler and will work on Bukkit, Paper, and Folia server types.
 * <p>
 * If an addon needs a specific server software to function, it should determine the server software being used in it's onAddonEnable method,
 * if BreweryX is not being run on the required server software, the addon should unload itself with {@link AddonManager#unloadAddon(BreweryAddon)}.
 * <p>
 * Addons using Bukkit listeners can write Listeners the same way they would in a plugin, and register them with {@link #registerListener(Listener)},
 * and unregister them with {@link #unregisterListener(Listener)}.
 * <p>
 * Addons should use BreweryX's command interface to register commands. See {@link AddonCommand}. Commands should be registered with
 * {@link #registerCommand(String, AddonCommand)}, and unregistered with {@link #unregisterCommand(String)}.
 * <p>
 * Listeners and commands will be unregistered automatically when an addon is disabled.
 * <p>
 * Addons also have support for configuration files. See {@link AddonConfigFile} and {@link AddonConfigManager} for more information.
 *
 * @see AddonCommand
 * @see AddonInfo
 * @see AddonConfigFile
 * @see com.dre.brewery.configuration.annotation.OkaeriConfigFileOptions
 * @see AddonConfigManager
 * @see AddonFileManager
 * @see AddonLogger
 * @see AddonManager
 *
 * @author Jsinco
 */
@SuppressWarnings("unused")
public abstract class BreweryAddon {

	private static final boolean isPaper;
	private final List<Listener> listeners = new ArrayList<>();
	private final List<String> commands = new ArrayList<>();

	private AddonInfo addonInfo;

	private AddonLogger logger;
	private AddonFileManager addonFileManager;
	private AddonConfigManager addonConfigManager;


	public void onAddonPreEnable() {
		// Code for this addon which runs before the addon is enabled.
	}
	public void onAddonEnable() {
		// Code for this addon which runs after the addon is enabled.
	}
	public void onAddonDisable() {
		// Code for this addon which runs before the addon is disabled.
	}
	public void onBreweryReload() {
		// Code for this addon which runs after `/breweryx reload` is executed.
	}


	/**
	 * Get the AddonInfo for this addon.
	 * @return The addon info
	 */
	@NotNull
	public AddonInfo getAddonInfo() {
		return addonInfo;
	}

	/**
	 * Get the *DEPRECATED* file manager for this addon.
	 * @return The file manager
	 */
	@NotNull
	public AddonFileManager getAddonFileManager() {
		return addonFileManager;
	}

	/**
	 * Get the config manager for this addon.
	 * @return The config manager
	 */
	@NotNull
	public AddonConfigManager getAddonConfigManager() {
		return addonConfigManager;
	}

	/**
	 * Get the logger for this addon.
	 * @return The logger
	 */
	@NotNull
	public AddonLogger getAddonLogger() {
		return logger;
	}

	/**
	 * Register a listener with the server.
	 * @param listener The listener to register
	 */
	public void registerListener(Listener listener) {
		Bukkit.getPluginManager().registerEvents(listener, BreweryPlugin.getInstance());
		listeners.add(listener);
	}

	/**
	 * Unregister a listener registered by this addon.
	 * @param listener The listener to unregister
	 */
	public void unregisterListener(Listener listener) {
		HandlerList.unregisterAll(listener);
		listeners.remove(listener);
	}


	/**
	 * Register a command with BreweryX's command manager.
	 * @param name The name of the command
	 * @param command The command to register
	 */
	public void registerCommand(String name, AddonCommand command) {
		CommandManager.addSubCommand(name, command);
		commands.add(name);
	}

	/**
	 * Unregister a command with BreweryX's command manager.
	 * @param name The name of the command
	 */
	public void unregisterCommand(String name) {
		CommandManager.removeSubCommand(name);
		commands.remove(name);
	}

	/**
	 * Unregister all listeners and commands registered by this addon.
	 */
	public void unregisterListeners() {
		for (Listener listener : listeners) {
			HandlerList.unregisterAll(listener);
		}
		listeners.clear();
	}

	/**
	 * Unregister all commands registered by this addon.
	 */
	public void unregisterCommands() {
		for (String command : commands) {
			CommandManager.removeSubCommand(command);
		}
		commands.clear();
	}


	// Utility

	/**
	 * Get the BreweryX plugin instance.
	 * @return The BreweryX plugin instance
	 */
	@NotNull
	public BreweryPlugin getBreweryPlugin() {
		return BreweryPlugin.getInstance();
	}


	/**
	 * Get the scheduler for BreweryX.
	 * @return The scheduler
	 */
	@NotNull
	public TaskScheduler getScheduler() {
		return BreweryPlugin.getScheduler();
	}

	/**
	 * Get the addon manager for BreweryX.
	 * @return The addon manager
	 */
	@NotNull
	public AddonManager getAddonManager() {
		return BreweryPlugin.getAddonManager();
	}

	/**
	 * Get the Minecraft version of the server running BreweryX and this addon.
	 * @return The Minecraft version
	 */
	@NotNull
	public MinecraftVersion getMCVersion() {
		return BreweryPlugin.getMCVersion();
	}

	/**
	 * If BreweryX and this addon are running on Folia or a Folia-based server.
	 * @return true if running on Folia, false otherwise.
	 */
	public boolean isFolia() {
		return BreweryPlugin.isFolia();
	}

	/**
	 * If BreweryX and this addon are running on Paper or a Paper-based server.
	 * @return true if running on Paper, false otherwise.
	 */
	public boolean isPaper() {
		return isPaper;
	}


	static {
		boolean tempBool;
		try {
			Class.forName("com.destroystokyo.paper.ParticleBuilder");
			tempBool = true;
		} catch (ClassNotFoundException ignored) {
			tempBool = false;
		}
		isPaper = tempBool;
	}
}
