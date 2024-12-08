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
import com.dre.brewery.BPlayer;
import com.dre.brewery.Barrel;
import com.dre.brewery.Brew;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.MCBarrel;
import com.dre.brewery.Wakeup;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.configuration.sector.capsule.ConfiguredDataManager;
import com.dre.brewery.integration.bstats.BreweryStats;
import com.dre.brewery.storage.impls.FlatFileStorage;
import com.dre.brewery.storage.impls.MongoDBStorage;
import com.dre.brewery.storage.impls.MySQLStorage;
import com.dre.brewery.storage.impls.SQLiteStorage;
import com.dre.brewery.storage.records.BreweryMiscData;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.Logging;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public abstract class DataManager {

    // TODO: Instead of using UUIDs for Barrels, Cauldrons, and Wakeups. We should figure out some hashing algorithm to generate a unique ID for each of them.

    protected static BreweryPlugin plugin = BreweryPlugin.getInstance();
    protected static long lastAutoSave = System.currentTimeMillis();

    @Getter
    private final DataManagerType type;

    protected DataManager(DataManagerType type) throws StorageInitException {
        this.type = type;
    }

    public abstract Barrel getBarrel(UUID id);
    public abstract Collection<Barrel> getAllBarrels();
    public abstract void saveAllBarrels(Collection<Barrel> barrels, boolean overwrite);
    public abstract void saveBarrel(Barrel barrel);
    public abstract void deleteBarrel(UUID id);


    public abstract BCauldron getCauldron(UUID id);
    public abstract Collection<BCauldron> getAllCauldrons();
    public abstract void saveAllCauldrons(Collection<BCauldron> cauldrons, boolean overwrite);
    public abstract void saveCauldron(BCauldron cauldron);
    public abstract void deleteCauldron(UUID id);


    public abstract BPlayer getPlayer(UUID playerUUID);
    public abstract Collection<BPlayer> getAllPlayers();
    public abstract void saveAllPlayers(Collection<BPlayer> players, boolean overwrite);
    public abstract void savePlayer(BPlayer player);
    public abstract void deletePlayer(UUID playerUUID);


    public abstract Wakeup getWakeup(UUID id);
    public abstract Collection<Wakeup> getAllWakeups();
    public abstract void saveAllWakeups(Collection<Wakeup> wakeups, boolean overwrite);
    public abstract void saveWakeup(Wakeup wakeup);
    public abstract void deleteWakeup(UUID id);


    public abstract BreweryMiscData getBreweryMiscData();
    public abstract void saveBreweryMiscData(BreweryMiscData data);


    public void tryAutoSave() {
        long interval = ConfigManager.getConfig(Config.class).getAutosave() * 60000L;

        if (System.currentTimeMillis() - lastAutoSave > interval) {
            saveAll(true);
            lastAutoSave = System.currentTimeMillis();
            Logging.debugLog("Auto saved all data!");
        }
    }

    public void exit(boolean save, boolean async) {
        this.exit(save, async, null);
    }

    public void exit(boolean save, boolean async, Runnable callback) {
        if (save) {
            saveAll(async, () -> {
                this.closeConnection();
                Logging.log("Closed connection from&7:&a " + this.getType().getFormattedName());
                if (callback != null) {
                    callback.run();
                }
            });
        } else {
            this.closeConnection(); // let databases close their connections
            Logging.log("Closed connection from&7:&a " + this.getType().getFormattedName());
            if (callback != null) {
                callback.run();
            }
        }
    }

    public void saveAll(boolean async) {
        saveAll(async, null);
    }

    public void saveAll(boolean async, Runnable callback) {
        Collection<Barrel> barrels = Barrel.getBarrels();
        Collection<BCauldron> cauldrons = BCauldron.getBcauldrons().values();
        Collection<BPlayer> bPlayers = BPlayer.getPlayers().values();
        Collection<Wakeup> wakeups = Wakeup.getWakeups();

        if (async) {
            BreweryPlugin.getScheduler().runTaskAsynchronously(() -> {
                doSave(barrels, cauldrons, bPlayers, wakeups);
                if (callback != null) {
                    callback.run();
                }
            });
        } else {
            doSave(barrels, cauldrons, bPlayers, wakeups);
            if (callback != null) {
                callback.run();
            }
        }
    }

    private void doSave(Collection<Barrel> barrels, Collection<BCauldron> cauldrons, Collection<BPlayer> players, Collection<Wakeup> wakeups) {
        saveBreweryMiscData(getLoadedMiscData());
        saveAllBarrels(barrels, true);
        saveAllCauldrons(cauldrons, true);
        saveAllPlayers(players, true);
        saveAllWakeups(wakeups, true);
        Logging.debugLog("Saved all data!");
    }

    protected void closeConnection() {
        // Implemented in subclasses that use database connections
    }

    public static DataManager createDataManager(ConfiguredDataManager record) throws StorageInitException {
        DataManager dataManager = switch (record.getType()) {
            case FLATFILE -> new FlatFileStorage(record);
            case MYSQL -> new MySQLStorage(record);
            case SQLITE -> new SQLiteStorage(record);
            case MONGODB -> new MongoDBStorage(record);
        };

        // Legacy data migration
        if (BData.checkForLegacyData()) {
            long start = System.currentTimeMillis();
            Logging.log("&5Brewery is loading data from a legacy format!");

            BData.readData();
            BData.finalizeLegacyDataMigration();

            dataManager.saveAll(false);

            Logging.log("&5Finished migrating legacy data! Took&7: &a" + (System.currentTimeMillis() - start) + "ms&5! Join our discord if you need assistance: &ahttps://discord.gg/3FkNaNDnta");
			Logging.warningLog("BreweryX can only load legacy data from worlds that exist. If you're trying to migrate old cauldrons, barrels, etc. And the worlds they're in don't exist, you'll need to migrate manually.");
        }


        Logging.log("DataManager created&7:&a " + record.getType().getFormattedName());
        return dataManager;
    }



    // Utility

    public static void loadMiscData(BreweryMiscData miscData) {
        Brew.installTime = miscData.installTime();
        MCBarrel.mcBarrelTime = miscData.mcBarrelTime();
        Brew.loadPrevSeeds(miscData.prevSaveSeeds());


        BreweryStats breweryStats = plugin.getBreweryStats();
        // Check the hash to prevent tampering with statistics - Note by original author
        if (miscData.brewsCreated().size() == 7 && miscData.brewsCreatedHash() == miscData.brewsCreated().hashCode()) {
            breweryStats.brewsCreated = miscData.brewsCreated().get(0);
            breweryStats.brewsCreatedCmd = miscData.brewsCreated().get(1);
            breweryStats.exc = miscData.brewsCreated().get(2);
            breweryStats.good = miscData.brewsCreated().get(3);
            breweryStats.norm = miscData.brewsCreated().get(4);
            breweryStats.bad = miscData.brewsCreated().get(5);
            breweryStats.terr = miscData.brewsCreated().get(6);
        }
    }

    public static BreweryMiscData getLoadedMiscData() {
        List<Integer> brewsCreated = new ArrayList<>(7);
        BreweryStats breweryStats = plugin.getBreweryStats();
        brewsCreated.addAll(List.of(breweryStats.brewsCreated, breweryStats.brewsCreatedCmd, breweryStats.exc, breweryStats.good, breweryStats.norm, breweryStats.bad, breweryStats.terr));


        return new BreweryMiscData(
                Brew.installTime,
                MCBarrel.mcBarrelTime,
                Brew.getPrevSeeds(),
                brewsCreated,
                brewsCreated.hashCode()
        );
    }


    public static Location deserializeLocation(String locationString) {
        return deserializeLocation(locationString, false);
    }

    public static String serializeLocation(Location location) {
        return serializeLocation(location, false);
    }

    public static Location deserializeLocation(String string, boolean yawPitch) {
        if (string == null) {
            Logging.warningLog("Location is null!");
            return null;
        }

        String locationString = string;
        String worldName = null;
        if (locationString.contains("?=")) {
            String[] split = locationString.split("\\?=");
            locationString = split[0];
            worldName = split[1];
        }

        String[] loc = locationString.split(",");
        UUID worldUUID = null;
        try {
            worldUUID = UUID.fromString(loc[0]);
        } catch (IllegalArgumentException ignored) {
        }


        World world = null;


        if (worldUUID != null) {
            world = Bukkit.getWorld(worldUUID);
        }

        if (world == null && worldName != null) {
            world = Bukkit.getWorld(worldName);
        }


        if (world == null) {
            Logging.warningLog("World not found! " + loc[0]); // TODO: add command to purge stuff in non-existent worlds
            return null;
        }

        if (yawPitch && loc.length == 6) {
            return new Location(world, BUtil.parseInt(loc[1]), BUtil.parseInt(loc[2]), BUtil.parseInt(loc[3]), BUtil.parseFloat(loc[4]), BUtil.parseFloat(loc[5]));
        } else {
            return new Location(world, BUtil.parseInt(loc[1]), BUtil.parseInt(loc[2]), BUtil.parseInt(loc[3]));
        }
    }

    public static String serializeLocation(Location location, boolean yawPitch) {
        if (location.getWorld() == null) {
            Logging.errorLog("Location must have a world! " + location);
            return null;
        }
        String locationString;
        if (yawPitch) {
            locationString = location.getWorld().getUID() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + "," + location.getYaw() + "," + location.getPitch();
        } else {
            locationString = location.getWorld().getUID() + "," + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
        }

        // added this extra char separator so brewery can now parse locations via the world uuid or name
        locationString = locationString + "?=" + location.getWorld().getName();
        return locationString;
    }
}
