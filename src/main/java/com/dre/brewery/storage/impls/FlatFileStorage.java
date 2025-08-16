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

package com.dre.brewery.storage.impls;

import com.dre.brewery.BCauldron;
import com.dre.brewery.BIngredients;
import com.dre.brewery.BPlayer;
import com.dre.brewery.Barrel;
import com.dre.brewery.Wakeup;
import com.dre.brewery.configuration.sector.capsule.ConfiguredDataManager;
import com.dre.brewery.storage.DataManager;
import com.dre.brewery.storage.StorageInitException;
import com.dre.brewery.storage.interfaces.SerializableThing;
import com.dre.brewery.storage.records.BreweryMiscData;
import com.dre.brewery.storage.serialization.BukkitSerialization;
import com.dre.brewery.storage.serialization.SQLDataSerializer;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.BoundingBox;
import com.dre.brewery.utility.FutureUtil;
import com.dre.brewery.utility.Logging;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// TODO: Simplify methods
public class FlatFileStorage extends DataManager {

    private final File rawFile;
    private final YamlConfiguration dataFile;
    private SQLDataSerializer serializer;

    public FlatFileStorage(ConfiguredDataManager record) throws StorageInitException {
        super(record.getType());
        String fileName = record.getDatabase() + ".yml";
        this.rawFile = new File(plugin.getDataFolder(), fileName);

        if (!rawFile.exists()) {
            try {
                rawFile.createNewFile();
            } catch (IOException e) {
                throw new StorageInitException("Failed to create file! " + fileName, e);
            }
        }

        this.dataFile = YamlConfiguration.loadConfiguration(rawFile);
    }


    private void save() {
        try {
            dataFile.save(rawFile);
        } catch (IOException e) {
            Logging.errorLog("Failed to save to FlatFile!", e);
        }
    }

    private SQLDataSerializer getLazySerializerInstance() {
        if (serializer == null) {
            serializer = new SQLDataSerializer();
        }
        return serializer;
    }

    @Override
    public boolean createTable(String name, int maxIdLength) {
        if (dataFile.contains(name)) {
            return false;
        }
        dataFile.createSection(name);
        save();
        return true;
    }

    @Override
    public boolean dropTable(String name) {
        dataFile.set(name, null);
        save();
        return true;
    }


    @Override
    public <T extends SerializableThing> T getGeneric(String id, String table, Class<T> type) {
        String path = table + "." + id;

        ConfigurationSection section = dataFile.getConfigurationSection(path);
        if (section == null) {
            return null;
        }

        // Get all values at the path as a Map
        Map<String, Object> map = section.getValues(false);
        Gson gson = getLazySerializerInstance().getGson();

        // Gson writes ints as doubles sometimes, but they seem to serialize back to ints just fine.
        String json = gson.toJson(map);
        return gson.fromJson(json, type);
    }

    @Override
    public <T extends SerializableThing> List<T> getAllGeneric(String table, Class<T> type) {
        ConfigurationSection section = dataFile.getConfigurationSection(table);
        if (section == null) {
            return Collections.emptyList();
        }
        List<T> things = new ArrayList<>();
        for (String key : section.getKeys(false)) {
            things.add(getGeneric(key, table, type));
        }
        return things;
    }

    @Override
    public <T extends SerializableThing> void saveAllGeneric(List<T> serializableThings, String table, @Nullable Class<T> type) {
        ConfigurationSection section = dataFile.getConfigurationSection(table);
        if (section != null) {
            section.getKeys(false).forEach(key -> dataFile.set(table + "." + key, null));
        } else {
            dataFile.createSection(table);
        }

        for (T thing : serializableThings) {
            saveGeneric(thing, table);
        }
        save();
    }

    @Override
    public <T extends SerializableThing> void saveGeneric(T serializableThing, String table) {
        String path = table + "." + serializableThing.getId();

        Gson gson = getLazySerializerInstance().getGson();
        JsonObject jsonObject = gson.toJsonTree(serializableThing).getAsJsonObject();
        Type mapType = new TypeToken<Map<String, Object>>() {
        }.getType();
        Map<String, Object> map = gson.fromJson(jsonObject, mapType);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            dataFile.set(path + "." + entry.getKey(), entry.getValue());
        }
        save();
    }

    @Override
    public void deleteGeneric(String id, String table) {
        dataFile.set(table + "." + id, null);
        save();
    }

    @Override
    public CompletableFuture<Barrel> getBarrel(UUID id) {
        String path = "barrels." + id;

        Location spigotLoc = deserializeLocation(dataFile.getString(path + ".spigot"));
        if (spigotLoc == null) {
            return CompletableFuture.completedFuture(null);
        }

        int[] bounds = Arrays.stream(
                dataFile.getString(path + ".bounds").split(",")
            )
            .mapToInt(Integer::parseInt).toArray();

        BoundingBox boundingBox = BoundingBox.fromPoints(bounds);
        float time = (float) dataFile.getDouble(path + ".time", 0.0);
        byte sign = (byte) dataFile.getInt(path + ".sign", 0);
        ItemStack[] items = BukkitSerialization.itemStackArrayFromBase64(dataFile.getString(path + ".items", null));


        return Barrel.computeSmall(spigotLoc).thenApplyAsync(small ->
            new Barrel(spigotLoc.getBlock(), sign, boundingBox, items, time, id, small)
        );
    }

    @Override
    public CompletableFuture<List<Barrel>> getAllBarrels() {
        ConfigurationSection section = dataFile.getConfigurationSection("barrels");
        if (section == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        List<CompletableFuture<Barrel>> barrels = new ArrayList<>();

        for (String key : section.getKeys(false)) {
            CompletableFuture<Barrel> barrel = getBarrel(BUtil.uuidFromString(key));
            if (barrel != null) {
                barrels.add(barrel);
            }
        }
        return FutureUtil.mergeFutures(barrels);
    }

    @Override
    public void saveAllBarrels(Collection<Barrel> barrels) {
        dataFile.set("barrels", null);
        for (Barrel barrel : barrels) {
            saveBarrel(barrel);
        }
    }

    @Override
    public void saveBarrel(Barrel barrel) {
        if (barrel.getBounds() == null) {
            return;
        }
        String path = "barrels." + barrel.getId();

        dataFile.set(path + ".spigot", serializeLocation(barrel.getSpigot().getLocation()));
        dataFile.set(path + ".bounds", barrel.getBounds().serialize());
        dataFile.set(path + ".time", barrel.getTime());
        dataFile.set(path + ".sign", barrel.getSignoffset());
        dataFile.set(path + ".items", BukkitSerialization.itemStackArrayToBase64(barrel.getInventory().getContents()));
        save();
    }

    @Override
    public void deleteBarrel(UUID id) {
        dataFile.set("barrels." + id, null);
        save();
    }

    @Override
    public BCauldron getCauldron(UUID id) {
        String path = "cauldrons." + id;

        Location loc = deserializeLocation(dataFile.getString(path + ".block"));
        if (loc == null) {
            return null;
        }
        BIngredients ingredients = BIngredients.deserializeIngredients(dataFile.getString(path + ".ingredients"));
        int state = dataFile.getInt(path + ".state", 0);

        return new BCauldron(loc.getBlock(), ingredients, state, id);
    }

    @Override
    public Collection<BCauldron> getAllCauldrons() {
        ConfigurationSection section = dataFile.getConfigurationSection("cauldrons");

        if (section == null) {
            return Collections.emptyList();
        }

        List<BCauldron> cauldrons = new ArrayList<>();

        for (String key : section.getKeys(false)) {
            BCauldron cauldron = getCauldron(BUtil.uuidFromString(key));
            if (cauldron != null) {
                cauldrons.add(cauldron);
            }
        }
        return cauldrons;
    }

    @Override
    public void saveAllCauldrons(Collection<BCauldron> cauldrons) {
        dataFile.set("cauldrons", null);
        for (BCauldron cauldron : cauldrons) {
            saveCauldron(cauldron);
        }
    }

    @Override
    public void saveCauldron(BCauldron cauldron) {
        String path = "cauldrons." + cauldron.getId();

        dataFile.set(path + ".block", serializeLocation(cauldron.getBlock().getLocation()));
        dataFile.set(path + ".ingredients", cauldron.getIngredients().serializeIngredients());
        dataFile.set(path + ".state", cauldron.getState());
        save();
    }


    @Override
    public void deleteCauldron(UUID id) {
        dataFile.set("cauldrons." + id, null);
        save();
    }


    @Override
    public BPlayer getPlayer(UUID playerUUID) {
        String path = "players." + playerUUID;

        int quality = dataFile.getInt(path + ".quality", 0);
        int drunkenness = dataFile.getInt(path + ".drunkenness", 0);
        int offlineDrunkenness = dataFile.getInt(path + ".offlineDrunkenness", 0);
        return new BPlayer(playerUUID, quality, drunkenness, offlineDrunkenness);
    }

    @Override
    public Collection<BPlayer> getAllPlayers() {
        ConfigurationSection section = dataFile.getConfigurationSection("players");

        if (section == null) {
            return Collections.emptyList();
        }

        List<BPlayer> players = new ArrayList<>();

        for (String key : section.getKeys(false)) {
            BPlayer player = getPlayer(BUtil.uuidFromString(key));
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    @Override
    public void saveAllPlayers(Collection<BPlayer> players) {
        dataFile.set("players", null);
        for (BPlayer player : players) {
            savePlayer(player);
        }
    }

    @Override
    public void savePlayer(BPlayer player) {
        String path = "players." + player.getUuid();

        dataFile.set(path + ".quality", player.getQuality());
        dataFile.set(path + ".drunkenness", player.getDrunkeness());
        dataFile.set(path + ".offlineDrunkenness", player.getOfflineDrunkeness());
        save();
    }

    @Override
    public void deletePlayer(UUID playerUUID) {
        dataFile.set("players." + playerUUID, null);
        save();
    }

    @Override
    public Wakeup getWakeup(UUID id) {
        String path = "wakeups." + id;
        Location wakeupLocation = deserializeLocation(dataFile.getString(path + ".location"), true);
        if (wakeupLocation == null) {
            return null;
        }
        return new Wakeup(wakeupLocation, id);
    }

    @Override
    public Collection<Wakeup> getAllWakeups() {
        ConfigurationSection section = dataFile.getConfigurationSection("wakeups");

        if (section == null) {
            return Collections.emptyList();
        }

        List<Wakeup> wakeups = new ArrayList<>();

        for (String key : section.getKeys(false)) {
            Wakeup wakeup = getWakeup(BUtil.uuidFromString(key));
            if (wakeup != null) {
                wakeups.add(wakeup);
            }
        }
        return wakeups;
    }

    @Override
    public void saveAllWakeups(Collection<Wakeup> wakeups) {
        dataFile.set("wakeups", null);
        for (Wakeup wakeup : wakeups) {
            saveWakeup(wakeup);
        }
    }

    @Override
    public void saveWakeup(Wakeup wakeup) {
        String path = "wakeups." + wakeup.getId();
        dataFile.set(path + ".location", serializeLocation(wakeup.getLoc(), true));
        save();
    }

    @Override
    public void deleteWakeup(UUID id) {
        dataFile.set("wakeups." + id, null);
        save();
    }

    @Override
    public BreweryMiscData getBreweryMiscData() {
        return new BreweryMiscData(
            dataFile.getLong("misc.installTime", System.currentTimeMillis()),
            dataFile.getLong("misc.mcBarrelTime", 0),
            dataFile.getLongList("misc.previousSaveSeeds"),
            dataFile.getIntegerList("misc.brewsCreated"),
            dataFile.getInt("misc.brewsCreatedHash", 0)
        );
    }

    @Override
    public void saveBreweryMiscData(BreweryMiscData data) {
        dataFile.set("misc.installTime", data.installTime());
        dataFile.set("misc.mcBarrelTime", data.mcBarrelTime());
        dataFile.set("misc.previousSaveSeeds", data.prevSaveSeeds());
        dataFile.set("misc.brewsCreated", data.brewsCreated());
        dataFile.set("misc.brewsCreatedHash", data.brewsCreatedHash());
        save();
    }
}
