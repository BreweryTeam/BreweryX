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

package com.dre.brewery.storage;

import com.dre.brewery.BCauldron;
import com.dre.brewery.BIngredients;
import com.dre.brewery.BPlayer;
import com.dre.brewery.Barrel;
import com.dre.brewery.BarrelWoodType;
import com.dre.brewery.Brew;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.MCBarrel;
import com.dre.brewery.Wakeup;
import com.dre.brewery.lore.Base91DecoderStream;
import com.dre.brewery.recipe.Ingredient;
import com.dre.brewery.recipe.SimpleItem;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.BoundingBox;
import com.dre.brewery.utility.FutureUtil;
import com.dre.brewery.utility.Logging;
import com.dre.brewery.utility.MinecraftVersion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Legacy storage class for loading data from worlddata.yml and data.yml
 * <p>
 * <p>
 * The new DataManager stores data differently than the old one which used world UUIDs and nested data within
 * those world UUIDs. The new DataManager now uses a UUID for Barrels, Cauldrons, Players, and Wakeups.
 * This class was written by the original authors and is only used for bringing the old data into cache.
 */
public class BData {

    private static final MinecraftVersion VERSION = BreweryPlugin.getMCVersion();
    private static final BreweryPlugin plugin = BreweryPlugin.getInstance();

    public static AtomicInteger dataMutex = new AtomicInteger(0); // WorldData: -1 = Saving, 0 = Free, >= 1 = Loading
    public static FileConfiguration worldData = null; // World Data Cache for consecutive loading of Worlds. Nulled after a data save


    public static boolean checkForLegacyData() {
        File file = new File(plugin.getDataFolder(), "data.yml");
        File worldDataFile = new File(plugin.getDataFolder(), "worlddata.yml");
        return file.exists() || worldDataFile.exists();
    }

    public static void finalizeLegacyDataMigration() {
        File file = new File(plugin.getDataFolder(), "data.yml");
        File worldDataFile = new File(plugin.getDataFolder(), "worlddata.yml");
        File worldDataFileBackup = new File(plugin.getDataFolder(), "worlddataBackup.yml");

        if (file.exists()) {
            file.renameTo(new File(plugin.getDataFolder(), "data.yml.old"));
        }
        if (worldDataFile.exists()) {
            worldDataFile.renameTo(new File(plugin.getDataFolder(), "worlddata.yml.old"));
        }
        if (worldDataFileBackup.exists()) {
            worldDataFileBackup.renameTo(new File(plugin.getDataFolder(), "worlddataBackup.yml.old"));
        }
    }


    // load all Data
    public static void readData() {
        File file = new File(plugin.getDataFolder(), "data.yml");
        File worldDataFile = new File(plugin.getDataFolder(), "worlddata.yml");
        if (file.exists()) {

            FileConfiguration data = YamlConfiguration.loadConfiguration(file);


            Brew.installTime = data.getLong("installTime", System.currentTimeMillis());
            MCBarrel.mcBarrelTime = data.getLong("MCBarrelTime", 0);

            Brew.loadPrevSeeds(data);

            List<Integer> brewsCreated = data.getIntegerList("brewsCreated");
            if (brewsCreated.size() == 7) {
                int hash = data.getInt("brewsCreatedH");
                // Check the hash to prevent tampering with statistics
                if (brewsCreated.hashCode() == hash) {
                    plugin.getBreweryStats().brewsCreated = brewsCreated.get(0);
                    plugin.getBreweryStats().brewsCreatedCmd = brewsCreated.get(1);
                    plugin.getBreweryStats().exc = brewsCreated.get(2);
                    plugin.getBreweryStats().good = brewsCreated.get(3);
                    plugin.getBreweryStats().norm = brewsCreated.get(4);
                    plugin.getBreweryStats().bad = brewsCreated.get(5);
                    plugin.getBreweryStats().terr = brewsCreated.get(6);
                }
            }
        }

        if (worldDataFile.exists()) {
            FileConfiguration data = YamlConfiguration.loadConfiguration(worldDataFile);

            // loading Ingredients into ingMap
            // Only for Legacy Brews
            Map<String, BIngredients> ingMap = new HashMap<>();
            ConfigurationSection section = data.getConfigurationSection("Ingredients");
            if (section != null) {
                for (String id : section.getKeys(false)) {
                    if (section.isConfigurationSection(id + ".mats")) {
                        // Old way of saving
                        ConfigurationSection matSection = section.getConfigurationSection(id + ".mats");
                        if (matSection != null) {
                            // matSection has all the materials + amount as Integers
                            List<Ingredient> ingredients = oldDeserializeIngredients(matSection);
                            ingMap.put(id, new BIngredients(ingredients, section.getInt(id + ".cookedTime", 0), true));
                        } else {
                            Logging.errorLog("Ingredient id: '" + id + "' incomplete in data.yml");
                        }
                    } else {
                        // New way of saving ingredients
                        ingMap.put(id, deserializeIngredients(section.getString(id + ".mats")));
                    }
                }
            }

            // loading Brew legacy
            section = data.getConfigurationSection("Brew");
            if (section != null) {
                // All sections have the UID as name
                for (String uid : section.getKeys(false)) {
                    BIngredients ingredients = getIngredients(ingMap, section.getString(uid + ".ingId"));
                    int quality = section.getInt(uid + ".quality", 0);
                    int alc = section.getInt(uid + ".alc", 0);
                    byte distillRuns = (byte) section.getInt(uid + ".distillRuns", 0);
                    float ageTime = (float) section.getDouble(uid + ".ageTime", 0.0);
                    float wood = (float) section.getDouble(uid + ".wood", -1.0);
                    String recipe = section.getString(uid + ".recipe", null);
                    boolean unlabeled = section.getBoolean(uid + ".unlabeled", false);
                    boolean persistent = section.getBoolean(uid + ".persist", false);
                    boolean stat = section.getBoolean(uid + ".stat", false);
                    int lastUpdate = section.getInt(uid + ".lastUpdate", 0);

                    Brew.loadLegacy(ingredients, Integer.parseInt(uid), quality, alc, distillRuns, ageTime, BarrelWoodType.fromAny(wood), recipe, unlabeled, persistent, stat, lastUpdate);
                }
            }

            // Store how many legacy brews were created
            if (plugin.getBreweryStats().brewsCreated <= 0) {
                plugin.getBreweryStats().brewsCreated = 0;
                plugin.getBreweryStats().brewsCreatedCmd = 0;
                plugin.getBreweryStats().exc = 0;
                plugin.getBreweryStats().good = 0;
                plugin.getBreweryStats().norm = 0;
                plugin.getBreweryStats().bad = 0;
                plugin.getBreweryStats().terr = 0;
                if (!Brew.noLegacy()) {
                    for (int i = Brew.legacyPotions.size(); i > 0; i--) {
                        plugin.getBreweryStats().metricsForCreate(false);
                    }
                }
            }

            // Remove Legacy Potions that haven't been touched in a long time, these may have been lost
            if (!Brew.noLegacy()) {
                int currentHoursAfterInstall = (int) ((double) (System.currentTimeMillis() - Brew.installTime) / 3600000D);
                int purgeTime = currentHoursAfterInstall - (24 * 30 * 4); // Purge Time is 4 Months ago
                if (purgeTime > 0) {
                    int removed = 0;
                    for (Iterator<Brew> iterator = Brew.legacyPotions.values().iterator(); iterator.hasNext(); ) {
                        Brew brew = iterator.next();
                        if (brew.getLastUpdate() < purgeTime) {
                            iterator.remove();
                            removed++;
                        }
                    }
                    if (removed > 0) {
                        Logging.log("Removed " + removed + " Legacy Brews older than 3 months");
                    }
                }
            }

            // loading BPlayer
            List<BPlayer> players = new ArrayList<>();
            section = data.getConfigurationSection("Player");
            if (section != null) {
                // keys have players uuid
                for (String uuid : section.getKeys(false)) {


                    int quality = section.getInt(uuid + ".quality");
                    int drunk = section.getInt(uuid + ".drunk");
                    int offDrunk = section.getInt(uuid + ".offDrunk", 0);

                    players.add(new BPlayer(uuid, quality, drunk, offDrunk));
                }
            }
            BPlayer.getPlayers().putAll(players.stream().collect(Collectors.toMap(BPlayer::getUuid, Function.identity())));


            final List<World> worlds = plugin.getServer().getWorlds();
            for (World world : worlds) {
                loadWorldData(world.getUID().toString(), world);
            }
        }
    }

    public static BIngredients deserializeIngredients(String mat) {
        try (DataInputStream in = new DataInputStream(new Base91DecoderStream(new ByteArrayInputStream(mat.getBytes())))) {
            byte ver = in.readByte();
            return BIngredients.load(in, ver);
        } catch (IOException e) {
            Logging.errorLog("Failed to load Ingredients from data.yml", e);
            return new BIngredients();
        }
    }

    // Loading from the old way of saving ingredients
    public static List<Ingredient> oldDeserializeIngredients(ConfigurationSection matSection) {
        List<Ingredient> ingredients = new ArrayList<>();
        for (String mat : matSection.getKeys(false)) {
            String[] matSplit = mat.split(",");
            Material m = Material.getMaterial(matSplit[0]);
            if (m == null && VERSION.isOrLater(MinecraftVersion.V1_13)) {
                if (matSplit[0].equals("LONG_GRASS")) {
                    m = Material.GRASS;
                } else {
                    m = Material.matchMaterial(matSplit[0], true);
                }
                Logging.debugLog("converting Data Material from " + matSplit[0] + " to " + m);
            }
            if (m == null) continue;
            SimpleItem item;
            if (matSplit.length == 2) {
                item = new SimpleItem(m, (short) BUtil.parseIntOrZero(matSplit[1]));
            } else {
                item = new SimpleItem(m);
            }
            item.setAmount(matSection.getInt(mat));
            ingredients.add(item);
        }
        return ingredients;
    }

    // returns Ingredients by id from the specified ingMap
    public static BIngredients getIngredients(Map<String, BIngredients> ingMap, String id) {
        if (!ingMap.isEmpty()) {
            if (ingMap.containsKey(id)) {
                return ingMap.get(id);
            }
        }
        Logging.errorLog("Ingredient id: '" + id + "' not found in data.yml");
        return new BIngredients();
    }

    // loads BIngredients from an ingredient section
    public static BIngredients loadCauldronIng(ConfigurationSection section, String path) {
        if (section.isConfigurationSection(path)) {
            // Old way of saving
            ConfigurationSection matSection = section.getConfigurationSection(path);
            if (matSection != null) {
                // matSection has all the materials + amount as Integers
                return new BIngredients(oldDeserializeIngredients(section), 0);
            } else {
                Logging.errorLog("Cauldron is missing Ingredient Section");
                return new BIngredients();
            }
        } else {
            // New way of saving ingredients
            return deserializeIngredients(section.getString(path));
        }
    }

    public static void lwDataTask(List<World> worlds) {
        if (!acquireDataLoadMutex()) return; // Tries for 60 sec

        try {
            for (World world : worlds) {
                if (world.getName().startsWith("DXL_")) {
                    loadWorldData(BUtil.getDxlName(world.getName()), world);
                } else {
                    loadWorldData(world.getUID().toString(), world);
                }
            }
        } catch (Exception e) {
            Logging.errorLog("Error loading World Data", e);
        } finally {
            releaseDataLoadMutex();
            if (BData.dataMutex.get() == 0) {
                Logging.log("Background data loading complete.");
            }
        }
    }

    // load Block locations of given world
    // can be run async
    public static void loadWorldData(String uuid, World world) {
        if (BData.worldData == null) {
            File file = new File(plugin.getDataFolder(), "worlddata.yml");
            if (file.exists()) {
                long t1 = System.currentTimeMillis();
                BData.worldData = YamlConfiguration.loadConfiguration(file);
                long t2 = System.currentTimeMillis();
                if (t2 - t1 > 15000) {
                    // Spigot is _very_ slow at loading inventories from yml. Paper is way faster.
                    // Notify Admin that loading Data took long (its async so not much of a problem)
                    Logging.log("Bukkit took " + (t2 - t1) / 1000.0 + "s to load Inventories from the World-Data File (in the Background),");
                    Logging.log("consider switching to Paper, or have less items in Barrels if it takes a long time for Barrels to become available");
                } else {
                    Logging.debugLog("Loading worlddata.yml: " + (t2 - t1) + "ms");
                }
            } else {
                return;
            }
        }

        // loading BCauldron
        final Map<Block, BCauldron> initCauldrons = new HashMap<>();
        if (BData.worldData.contains("BCauldron." + uuid)) {
            ConfigurationSection section = BData.worldData.getConfigurationSection("BCauldron." + uuid);
            for (String cauldron : section.getKeys(false)) {
                // block is splitted into x/y/z
                String block = section.getString(cauldron + ".block");
                if (block != null) {
                    String[] splitted = block.split("/");
                    if (splitted.length == 3) {
                        Block worldBlock = world.getBlockAt(Integer.parseInt(splitted[0]), Integer.parseInt(splitted[1]), Integer.parseInt(splitted[2]));
                        BIngredients ingredients = loadCauldronIng(section, cauldron + ".ingredients");
                        int state = section.getInt(cauldron + ".state", 0);

                        initCauldrons.put(worldBlock, new BCauldron(worldBlock, ingredients, state, UUID.randomUUID()));
                    } else {
                        Logging.errorLog("Incomplete Block-Data in data.yml: " + section.getCurrentPath() + "." + cauldron);
                    }
                } else {
                    Logging.errorLog("Missing Block-Data in data.yml: " + section.getCurrentPath() + "." + cauldron);
                }
            }
        }

        // loading Barrel
        final List<CompletableFuture<Barrel>> initBarrelFutures = new ArrayList<>();
        if (BData.worldData.contains("Barrel." + uuid)) {
            ConfigurationSection section = BData.worldData.getConfigurationSection("Barrel." + uuid);
            for (String barrel : section.getKeys(false)) {
                // block spigot is splitted into x/y/z
                String spigot = section.getString(barrel + ".spigot");
                if (spigot != null) {
                    String[] splitted = spigot.split("/");
                    if (splitted.length == 3) {

                        // load itemStacks from invSection
                        ConfigurationSection invSection = section.getConfigurationSection(barrel + ".inv");
                        Location spigotLocation = new Location(world, Integer.parseInt(splitted[0]), Integer.parseInt(splitted[1]), Integer.parseInt(splitted[2]));
                        float time = (float) section.getDouble(barrel + ".time", 0.0);
                        byte sign = (byte) section.getInt(barrel + ".sign", 0);

                        BoundingBox box = null;
                        if (section.contains(barrel + ".bounds")) {
                            String[] bds = section.getString(barrel + ".bounds", "").split(",");
                            if (bds.length == 6) {
                                box = BoundingBox.fromPoints(
                                    Arrays.stream(bds).mapToInt(Integer::parseInt).toArray()
                                );
                            }
                        } else if (section.contains(barrel + ".st")) {
                            // Convert from Stair and Wood Locations to BoundingBox
                            String[] st = section.getString(barrel + ".st", "").split(",");
                            String[] wo = section.getString(barrel + ".wo", "").split(",");
                            int woLength = wo.length;
                            if (woLength <= 1) {
                                woLength = 0;
                            }
                            String[] points = new String[st.length + woLength];
                            System.arraycopy(st, 0, points, 0, st.length);
                            if (woLength > 1) {
                                System.arraycopy(wo, 0, points, st.length, woLength);
                            }
                            int[] locs = Arrays.stream(points).mapToInt(Integer::parseInt).toArray();
                            try {
                                box = BoundingBox.fromPoints(locs);
                            } catch (Exception e) {
                                Logging.errorLog("Failed to load BoundingBox from Stair and Wood Locations", e);
                            }
                        }

                        final BoundingBox bbox = box;
                        CompletableFuture<Barrel> barrelFuture = Barrel.computeSmall(spigotLocation)
                            .thenApply(small -> {
                                if (invSection != null) {
                                    return new Barrel(spigotLocation.getBlock(), sign, bbox, invSection.getValues(true), time, UUID.randomUUID(), small);
                                } else {
                                    // Barrel has no inventory
                                    return new Barrel(spigotLocation.getBlock(), sign, bbox, Collections.emptyMap(), time, UUID.randomUUID(), small);
                                }
                            });
                        initBarrelFutures.add(barrelFuture);

                    } else {
                        Logging.errorLog("Incomplete Block-Data in data.yml: " + section.getCurrentPath() + "." + barrel);
                    }
                } else {
                    Logging.errorLog("Missing Block-Data in data.yml: " + section.getCurrentPath() + "." + barrel);
                }
            }
        }

        // loading Wakeup
        final List<Wakeup> initWakeups = new ArrayList<>();
        if (BData.worldData.contains("Wakeup." + uuid)) {
            ConfigurationSection section = BData.worldData.getConfigurationSection("Wakeup." + uuid);
            for (String wakeup : section.getKeys(false)) {
                // loc of wakeup is splitted into x/y/z/pitch/yaw
                String loc = section.getString(wakeup);
                if (loc != null) {
                    String[] splitted = loc.split("/");
                    if (splitted.length == 5) {
                        double x = Double.parseDouble(splitted[0]);
                        double y = Double.parseDouble(splitted[1]);
                        double z = Double.parseDouble(splitted[2]);
                        float pitch = Float.parseFloat(splitted[3]);
                        float yaw = Float.parseFloat(splitted[4]);

                        Location location = new Location(world, x, y, z, yaw, pitch);

                        initWakeups.add(new Wakeup(location));

                    } else {
                        Logging.errorLog("Incomplete Location-Data in data.yml: " + section.getCurrentPath() + "." + wakeup);
                    }
                }
            }
        }

        // Merge Loaded Data in Main Thread

        if (plugin.getServer().getWorld(world.getUID()) == null) {
            return;
        }
        if (!initCauldrons.isEmpty()) {
            BCauldron.bcauldrons.putAll(initCauldrons);
        }
        if (!initBarrelFutures.isEmpty()) {
            FutureUtil.mergeFutures(initBarrelFutures)
                .thenAcceptAsync(Barrel.barrels::addAll);
        }
        if (!initWakeups.isEmpty()) {
            Wakeup.wakeups.addAll(initWakeups);
        }

    }

    public static boolean acquireDataLoadMutex() {
        int wait = 0;
        // Increment the Data Mutex if it is not -1
        while (BData.dataMutex.updateAndGet(i -> i >= 0 ? i + 1 : i) <= 0) {
            wait++;
            if (wait > 60) {
                Logging.errorLog("Could not load World Data, Mutex: " + BData.dataMutex.get());
                return false;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return false;
            }
        }
        return true;
    }

    public static void releaseDataLoadMutex() {
        dataMutex.decrementAndGet();
    }
}
