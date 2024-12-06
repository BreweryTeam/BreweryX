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
import com.dre.brewery.utility.Logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;

/**
 * Yep, you guessed it. This is the class that manages all the addons. It loads them, unloads them, reloads them, and keeps track of them.
 * <p>
 * Basically just copies what Bukkit's plugin loader does, but on a much, much smaller scale.
 *
 * @see BreweryAddon
 * @see AddonInfo
 * @see AddonLogger
 * @see AddonFileManager
 * @see AddonConfigManager
 */
public class AddonManager {

	private final BreweryPlugin plugin;
	private final File addonsFolder;
	private final static List<BreweryAddon> addons = new ArrayList<>();

	public AddonManager(BreweryPlugin plugin) {
        this.plugin = plugin;
		this.addonsFolder = new File(plugin.getDataFolder(), "addons");
		if (!addonsFolder.exists()) {
			addonsFolder.mkdirs();
		}
	}

	public void unloadAddons() {
		for (BreweryAddon addon : addons) {
			try {
				addon.onAddonDisable();
				addon.unregisterListeners();
				addon.unregisterCommands();
			} catch (Throwable t) {
				Logging.errorLog("Failed to disable addon " + addon.getClass().getSimpleName(), t);
			}
		}
		int loaded = addons.size();
		if (loaded > 0) Logging.log("Disabled " + loaded + " addon(s)");
		addons.clear();
	}

	public void unloadAddon(BreweryAddon addon) {
		try {
			addon.onAddonDisable();
			addon.unregisterListeners();
			addon.unregisterCommands();
		} catch (Throwable t) {
			Logging.errorLog("Failed to disable addon " + addon.getClass().getSimpleName(), t);
		}
		addons.remove(addon);
	}


	public void reloadAddons() {
		for (BreweryAddon addon : addons) {
			try {
				addon.onBreweryReload();
			} catch (Throwable t) {
				Logging.errorLog("Failed to reload addon " + addon.getClass().getSimpleName(), t);
			}
		}
		int loaded = addons.size();
		if (loaded > 0) Logging.log("Reloaded " + loaded + " addon(s)");
	}

	public List<BreweryAddon> getAddons() {
		return addons;
	}


	// Get all classes that extend Addon and instantiates them
	public void loadAddons() {
		File[] files = addonsFolder.listFiles((dir, name) -> name.endsWith(".jar")); // Get all files in the addons folder that end with .jar
		if (files == null) {
			return;
		}

		for (File file : files) {
			loadAddon(file); // Go read the documentation below to understand what this does.
		}

		for (BreweryAddon addon : addons) {
			try {
				addon.onAddonEnable(); // All done, let the addon know it's been enabled.
			} catch (Throwable t) {
				Logging.errorLog("Failed to enable addon " + addon.getClass().getSimpleName(), t);
				unloadAddon(addon);
			}
		}
		int loaded = addons.size();
		if (loaded > 0) Logging.log("Loaded " + loaded + " addon(s)");
	}


	/**
	 * Load the addon from a jar file.
	 * Basically just scan the whole jar file for our BreweryAddon class, init that class first, init all other classes in the jar,
	 * set all the fields reflectively, and then call the onAddonPreEnable method.
	 * @param file The jar file to load the addon from
	 */
	public void loadAddon(File file) {
		try {
			List<Class<?>> classes = loadAllClassesFromJar(file); // Get all our loaded classes.

			// I actually don't like that I used a loop here, should replace with like findFirst or something.
			for (Class<?> clazz : classes) {
				if (!BreweryAddon.class.isAssignableFrom(clazz)) {
					continue;
				}
				Class<? extends BreweryAddon> addonClass = clazz.asSubclass(BreweryAddon.class);
				BreweryAddon addon;
				try {
					addon = addonClass.getConstructor().newInstance(); // Instantiate our main class, the class shouldn't have constructor args.
				}  catch (Exception e) {
					Logging.errorLog("Failed to load addon: " + file.getName(), e);
					continue;
				}
				try {

					Class<BreweryAddon> breweryAddonClass = BreweryAddon.class;
					// Set the logger and file manager
					Field infoField = breweryAddonClass.getDeclaredField("addonInfo"); infoField.setAccessible(true);
					infoField.set(addon, addonClass.getAnnotation(AddonInfo.class));

					if (addon.getAddonInfo() == null) { // This CAN be null for us. It's only annotated NotNull for addons.
						Logging.errorLog("Addon " + addonClass.getSimpleName() + " is missing the AddonInfo annotation. It will not be loaded.");
						continue;
					}

					// Set all the fields for our addon reflectively.

					Field loggerField = breweryAddonClass.getDeclaredField("logger"); loggerField.setAccessible(true);
					Field fileManagerField = breweryAddonClass.getDeclaredField("addonFileManager"); fileManagerField.setAccessible(true);
					Field addonConfigManagerField = breweryAddonClass.getDeclaredField("addonConfigManager"); addonConfigManagerField.setAccessible(true);

					loggerField.set(addon, new AddonLogger(addon.getAddonInfo()));
					fileManagerField.set(addon, new AddonFileManager(addon, file));
					addonConfigManagerField.set(addon, new AddonConfigManager(addon));


					addon.getAddonLogger().info("Loading &a" + addon.getAddonInfo().name() + " &f-&a v" + addon.getAddonInfo().version() + " &fby &a" + addon.getAddonInfo().author());
					addons.add(addon); // Add to our list of addons

					// let the addon know it has been loaded, it can do some pre-enable stuff here.
					addon.onAddonPreEnable();
				} catch (Exception e) {
					Logging.errorLog("Failed to load addon: " + file.getName(), e);
					unloadAddon(addon);
				}
			}
		} catch (Throwable ex) {
			Logging.errorLog("Failed to load addon classes from jar " + file.getName(), ex);
		}
	}

	/**
	 * It's about time I document this...
	 * <p>
	 * What we're doing here is trying to recreate what Bukkit does to load plugins. Obviously the code could be cleaned up and spread out to
	 * multiple functions but I honestly can't be bothered and this is fine I guess.
	 * <p>
	 * We have to load each class file, but first, we must load the class which extends BreweryAddon (our main class) before all else. If we
	 * don't load this class first we can get some nasty race conditions. Also, plugin developers expect their main class to be loaded first
	 * (as with all java programs) so we must first figure out which class extends BreweryAddon, load that one, then load the rest of the classes
	 * packaged in the addon.
	 * @param jarFile The jar file to load classes from
	 * @return A list of classes loaded from the jar
	 */
	private List<Class<?>> loadAllClassesFromJar(File jarFile) {
		List<Class<?>> classes = new ArrayList<>();

		// We have to use the same class loader used to load this class AKA, the 'PluginLoader' class provided by Bukkit.
		// ClassLoaders in Java are pretty interesting, and only classes loaded by the same ClassLoader can access each other.
		// So to prevent any issues, we're using the same ClassLoader that loaded this class to load the classes from the jar.
		try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, getClass().getClassLoader())) {

			try (JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile))) {
				JarEntry jarEntry;
				String mainDir = "";
				while ((jarEntry = jarInputStream.getNextJarEntry()) != null) { // Just iterate through every file in the jar file and check if it's a compiled java class.
					if (jarEntry.getName().endsWith(".class")) {

						// We have to replace the '/' with '.' and remove the '.class' extension to get the canonical name of the class. (org.example.Whatever)
						String className = jarEntry.getName().replaceAll("/", ".").replace(".class", "");
						try {
							Class<?> clazz;
							try {
								// It's important that we don't initialize any other classes before our main class.
								clazz = Class.forName(className, false, classLoader);
							} catch (ClassNotFoundException | NoClassDefFoundError e) {
								continue;
							}
                            if (BreweryAddon.class.isAssignableFrom(clazz)) {
								// Found our main class, we're going to load it now.
								classLoader.loadClass(className);
								mainDir = className.substring(0, className.lastIndexOf('.'));
							}
							// Prevents loading classes that aren't in the same package. Addons that have dependencies better shade em in I guess. (TODO: remove this?)
							if (!clazz.getName().contains(mainDir)) {
								continue;
							}
							classes.add(clazz); // And finally... add the class to our list of classes.

						} catch (ClassNotFoundException e) {
							plugin.getLogger().log(Level.SEVERE, "Failed to load class " + className, e);
						}
					}
				}

				// Now that we're done loading our main class, we can go ahead and load the rest of our classes. We don't care about the order of these classes.
				for (Class<?> clazz : classes) {
					if (!BreweryAddon.class.isAssignableFrom(clazz)) { // Just make sure it's not our main class we're trying to load again.
						try {
							classLoader.loadClass(clazz.getName());
						} catch (ClassNotFoundException e) {
							plugin.getLogger().log(Level.SEVERE, "Failed to load class " + clazz.getName(), e);
						}
					}
				}
			}
		} catch (IOException e) {
			plugin.getLogger().log(Level.SEVERE, "Error loading classes from JAR", e);
		}
		return classes;
	}

}