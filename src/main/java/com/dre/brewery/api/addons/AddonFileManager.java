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
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * A class to manage files for an addon.
 * Deprecated, use {@link AddonConfigFile} and {@link AddonConfigManager} instead.
 */
@Deprecated(since = "3.4.3-SNAPSHOT")
public class AddonFileManager {
	private final static BreweryPlugin plugin = BreweryPlugin.getInstance();

    private final File addonFolder;
	private final AddonLogger logger;
	private final File configFile;
	private YamlConfiguration addonConfig;
	private final File jarFile;

	public AddonFileManager(BreweryAddon addon, File jarFile) {
        this.jarFile = jarFile;
        String addonName = addon.getClass().getSimpleName();
		this.addonFolder = new File(plugin.getDataFolder().getAbsolutePath() + File.separator + "addons" + File.separator + addonName);
		this.logger = addon.getAddonLogger();
		this.configFile = new File(addonFolder, addonName + ".yml");
		this.addonConfig = configFile.exists() ? YamlConfiguration.loadConfiguration(configFile) : null;
	}


	public void generateFile(String fileName) {
		generateFile(new File(addonFolder, fileName));
	}
	public void generateFileAbsPath(String absolutePath) {
		generateFile(new File(absolutePath));
	}
	public void generateFile(File parent, String fileName) {
		generateFile(new File(parent, fileName));
	}
	public void generateFile(File file) {
		createAddonFolder();
		try {
			if (!file.exists()) {
				file.createNewFile();
				try (JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jarFile))) {
					JarEntry jarEntry;
					while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
						if (jarEntry.isDirectory() || !jarEntry.getName().equals(file.getName())) {
							continue;
						}
						OutputStream outputStream = Files.newOutputStream(file.toPath());
						byte[] buffer = new byte[1024];
						int bytesRead;
						while ((bytesRead = jarInputStream.read(buffer)) != -1) {
							outputStream.write(buffer, 0, bytesRead);
						}
						outputStream.flush();
						outputStream.close();
						break;
					}
				}
			}
		} catch (IOException ex) {
			logger.severe("Failed to generate file " + file.getName(), ex);
		}
	}

	public File getFile(String fileName) {
		createAddonFolder();
		return new File(addonFolder, fileName);
	}
	public YamlConfiguration getYamlConfiguration(String fileName) {
		createAddonFolder();
		return YamlConfiguration.loadConfiguration(new File(addonFolder, fileName));
	}
	public File getAddonFolder() {
		return addonFolder;
	}


	public YamlConfiguration getAddonConfig() {
		generateAddonConfig();
		return addonConfig;
	}
	public void saveAddonConfig() {
		generateAddonConfig();
		try {
			addonConfig.save(configFile);
		} catch (IOException ex) {
			logger.severe("Failed to save addon config", ex);
		}
	}
	private void generateAddonConfig() {
		if (addonConfig == null) {
			generateFile(configFile);
			addonConfig = YamlConfiguration.loadConfiguration(configFile);
		}
	}

	private void createAddonFolder() {
		if (!addonFolder.exists()) {
			addonFolder.mkdirs();
		}
	}
}
