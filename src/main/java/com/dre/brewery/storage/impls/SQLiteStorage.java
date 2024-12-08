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
import com.dre.brewery.BPlayer;
import com.dre.brewery.Barrel;
import com.dre.brewery.Wakeup;
import com.dre.brewery.configuration.sector.capsule.ConfiguredDataManager;
import com.dre.brewery.storage.DataManager;
import com.dre.brewery.storage.StorageInitException;
import com.dre.brewery.storage.records.BreweryMiscData;
import com.dre.brewery.storage.records.SerializableBPlayer;
import com.dre.brewery.storage.records.SerializableBarrel;
import com.dre.brewery.storage.records.SerializableCauldron;
import com.dre.brewery.storage.records.SerializableThing;
import com.dre.brewery.storage.records.SerializableWakeup;
import com.dre.brewery.storage.serialization.SQLDataSerializer;
import com.dre.brewery.utility.Logging;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("Duplicates") // Dupe code from MySQLStorage
public class SQLiteStorage extends DataManager {

    private static final String URL = "jdbc:sqlite:";
    private static final String[] TABLES = {
            "misc (id VARCHAR(4) PRIMARY KEY, data LONGTEXT);",
            "barrels (id VARCHAR(36) PRIMARY KEY, data LONGTEXT);",
            "cauldrons (id VARCHAR(36) PRIMARY KEY, data LONGTEXT);",
            "players (id VARCHAR(36) PRIMARY KEY, data LONGTEXT);",
            "wakeups (id VARCHAR(36) PRIMARY KEY, data LONGTEXT);"
    };

    private final Connection connection;
    private final String tablePrefix;
    private final SQLDataSerializer serializer;

    public SQLiteStorage(ConfiguredDataManager record) throws StorageInitException {
        super(record.getType());
        String fileName = record.getDatabase() + ".db";
        File rawFile = new File(plugin.getDataFolder(), fileName);

        if (!rawFile.exists()) {
            try {
                rawFile.createNewFile();
            } catch (IOException e) {
                throw new StorageInitException("Failed to create db file! " + fileName, e);
            }
        }

        try {
            this.connection = DriverManager.getConnection(URL + rawFile.getAbsolutePath());
            this.tablePrefix = record.getTablePrefix();
            this.serializer = new SQLDataSerializer();

            for (String table : TABLES) {
                try (PreparedStatement statement = connection.prepareStatement("CREATE TABLE IF NOT EXISTS " + tablePrefix + table)) {
                    statement.execute();
                }
            }
        } catch (SQLException e) {
            throw new StorageInitException("Failed to connect or create tables!", e);
        }
    }

    @Override
    protected void closeConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            Logging.errorLog("Failed to close SQLite connection!", e);
        }
    }

    private <T> T getGeneric(UUID id, String table, Class<T> type) {
        String sql = "SELECT data FROM " + tablePrefix + table + " WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return serializer.deserialize(resultSet.getString("data"), type);
                }
            }
        } catch (SQLException e) {
            Logging.errorLog("Failed to retrieve object from table: " + table + ", from: SQLite!", e);
        }
        return null;
    }

    private <T> List<T> getAllGeneric(String table, Class<T> type) {
        String sql = "SELECT id, data FROM " + tablePrefix + table;
        List<T> objects = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                String data = resultSet.getString("data");
                objects.add(serializer.deserialize(data, type));
            }
        } catch (SQLException e) {
            Logging.errorLog("Failed to retrieve objects from table: " + table + ", from: SQLite!", e);
        }
        return objects;
    }

    private void saveAllGeneric(List<? extends SerializableThing> serializableThings, String table, boolean overwrite) {
        String sql;
        if (overwrite) {
            sql = "INSERT INTO " + tablePrefix + table + " (id, data) VALUES (?, ?) ON CONFLICT(id) DO UPDATE SET data = excluded.data";
        } else {
            sql = "INSERT INTO " + tablePrefix + table + " (id, data) VALUES (?, ?) ON CONFLICT(id) DO NOTHING";
        }

        try (PreparedStatement insertStatement = connection.prepareStatement(sql)) {
            for (SerializableThing serializableThing : serializableThings) {
                insertStatement.setString(1, serializableThing.getId());
                insertStatement.setString(2, serializer.serialize(serializableThing));
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        } catch (SQLException e) {
            Logging.errorLog("Failed to save objects to SQLite!", e);
        }
    }


    private <T extends SerializableThing> void saveGeneric(T serializableThing, String table) {
        String sql = "INSERT INTO " + tablePrefix + table + " (id, data) VALUES (?, ?) ON CONFLICT(id) DO UPDATE SET data = excluded.data";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serializableThing.getId());
            statement.setString(2, serializer.serialize(serializableThing));
            statement.execute();
        } catch (SQLException e) {
            Logging.errorLog("Failed to save object to:" + table + ", to: SQLite!", e);
        }
    }

    private void deleteGeneric(UUID id, String table) {
        String sql = "DELETE FROM " + tablePrefix + table + " WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id.toString());
            statement.execute();
        } catch (SQLException e) {
            Logging.errorLog("Failed to delete object from: " + table + ", from: SQLite!", e);
        }
    }

    @Override
    public Barrel getBarrel(UUID id) {
        SerializableBarrel serializableBarrel = getGeneric(id, "barrels", SerializableBarrel.class);
        if (serializableBarrel != null) {
            return serializableBarrel.toBarrel();
        }
        return null;
    }

    @Override
    public Collection<Barrel> getAllBarrels() {
        return getAllGeneric("barrels", SerializableBarrel.class).stream()
                .map(SerializableBarrel::toBarrel)
                .toList();
    }

    @Override
    public void saveAllBarrels(Collection<Barrel> barrels, boolean overwrite) {
        List<SerializableBarrel> serializableBarrels = barrels.stream()
                .map(SerializableBarrel::new)
                .toList();
        saveAllGeneric(serializableBarrels, "barrels", overwrite);
    }

    @Override
    public void saveBarrel(Barrel barrel) {
        saveGeneric(new SerializableBarrel(barrel), "barrels");
    }

    @Override
    public void deleteBarrel(UUID id) {
        deleteGeneric(id, "barrels");
    }

    @Override
    public BCauldron getCauldron(UUID id) {
        SerializableCauldron serializableCauldron = getGeneric(id, "cauldrons", SerializableCauldron.class);
        if (serializableCauldron != null) {
            return serializableCauldron.toCauldron();
        }
        return null;
    }

    @Override
    public Collection<BCauldron> getAllCauldrons() {
        return getAllGeneric("cauldrons", SerializableCauldron.class).stream()
                .map(SerializableCauldron::toCauldron)
                .toList();
    }

    @Override
    public void saveAllCauldrons(Collection<BCauldron> cauldrons, boolean overwrite) {
        List<SerializableCauldron> serializableCauldrons = cauldrons.stream()
                .map(SerializableCauldron::new)
                .toList();
        saveAllGeneric(serializableCauldrons, "cauldrons", overwrite);
    }

    @Override
    public void saveCauldron(BCauldron cauldron) {
        saveGeneric(new SerializableCauldron(cauldron), "cauldrons");
    }

    @Override
    public void deleteCauldron(UUID id) {
        deleteGeneric(id, "cauldrons");
    }

    @Override
    public BPlayer getPlayer(UUID playerUUID) {
        SerializableBPlayer serializableBPlayer = getGeneric(playerUUID, "players", SerializableBPlayer.class);
        if (serializableBPlayer != null) {
            return serializableBPlayer.toBPlayer();
        }
        return null;
    }

    @Override
    public Collection<BPlayer> getAllPlayers() {
        return getAllGeneric("players", SerializableBPlayer.class).stream()
                .map(SerializableBPlayer::toBPlayer)
                .toList();
    }

    @Override
    public void saveAllPlayers(Collection<BPlayer> players, boolean overwrite) {
        List<SerializableBPlayer> serializableBPlayers = players.stream()
                .map(SerializableBPlayer::new)
                .toList();
        saveAllGeneric(serializableBPlayers, "players", overwrite);
    }

    @Override
    public void savePlayer(BPlayer player) {
        saveGeneric(new SerializableBPlayer(player), "players");
    }

    @Override
    public void deletePlayer(UUID playerUUID) {
        deleteGeneric(playerUUID, "players");
    }

    @Override
    public Wakeup getWakeup(UUID id) {
        SerializableWakeup serializableWakeup = getGeneric(id, "wakeups", SerializableWakeup.class);
        if (serializableWakeup != null) {
            return serializableWakeup.toWakeup();
        }
        return null;
    }

    @Override
    public Collection<Wakeup> getAllWakeups() {
        return getAllGeneric("wakeups", SerializableWakeup.class).stream()
                .map(SerializableWakeup::toWakeup)
                .toList();
    }

    @Override
    public void saveAllWakeups(Collection<Wakeup> wakeups, boolean overwrite) {
        List<SerializableWakeup> serializableWakeups = wakeups.stream()
                .map(SerializableWakeup::new)
                .toList();
        saveAllGeneric(serializableWakeups, "wakeups", overwrite);
    }

    @Override
    public void saveWakeup(Wakeup wakeup) {
        saveGeneric(new SerializableWakeup(wakeup), "wakeups");
    }

    @Override
    public void deleteWakeup(UUID id) {
        deleteGeneric(id, "wakeups");
    }

    @Override
    public BreweryMiscData getBreweryMiscData() {
        String sql = "SELECT CASE WHEN EXISTS (SELECT 1 FROM " + tablePrefix + "misc WHERE id = 'misc') THEN (SELECT data FROM " + tablePrefix + "misc WHERE id = 'misc') ELSE NULL END AS data";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            if (resultSet.next() && resultSet.getString("data") != null) {
                return serializer.deserialize(resultSet.getString("data"), BreweryMiscData.class);
            }
        } catch (SQLException e) {
            Logging.errorLog("Failed to retrieve misc data from SQLite!", e);
        }
        return new BreweryMiscData(System.currentTimeMillis(), 0, new ArrayList<>(), new ArrayList<>(), 0);
    }

    @Override
    public void saveBreweryMiscData(BreweryMiscData data) {
        String sql = "INSERT INTO " + tablePrefix + "misc (id, data) VALUES ('misc', ?) ON CONFLICT(id) DO UPDATE SET data = excluded.data";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serializer.serialize(data));
            statement.execute();
        } catch (SQLException e) {
            Logging.errorLog("Failed to save misc data to SQLite!", e);
        }
    }
}
