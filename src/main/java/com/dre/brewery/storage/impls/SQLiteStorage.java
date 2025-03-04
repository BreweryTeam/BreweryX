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
import com.dre.brewery.storage.interfaces.SerializableThing;
import com.dre.brewery.storage.records.BreweryMiscData;
import com.dre.brewery.storage.records.SerializableBPlayer;
import com.dre.brewery.storage.records.SerializableBarrel;
import com.dre.brewery.storage.records.SerializableCauldron;
import com.dre.brewery.storage.records.SerializableWakeup;
import com.dre.brewery.storage.serialization.SQLDataSerializer;
import com.dre.brewery.utility.Logging;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

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

// Dupe code from MySQLStorage
@SuppressWarnings({ "SqlSourceToSinkFlow", "Duplicates" })
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

    @Override
    public boolean createTable(String name, int maxIdLength) {
        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + name + " (id VARCHAR(" + maxIdLength + ") PRIMARY KEY, data LONGTEXT);";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
            return true;
        } catch (SQLException e) {
            Logging.errorLog("Failed to create table: " + name + " due to MySQL exception!", e);
        }
        return false;
    }

    @Override
    public boolean dropTable(String name) {
        String sql = "DROP TABLE IF EXISTS " + tablePrefix + name;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
            return true;
        } catch (SQLException e) {
            Logging.errorLog("Failed to drop table: " + name + " due to MySQL exception!", e);
        }
        return false;
    }

    @Override
    public <T extends SerializableThing> T getGeneric(String id, String table, Class<T> type) {
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

    @Override
    public <T extends SerializableThing> List<T> getAllGeneric(String table, Class<T> type) {
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

    public <T extends SerializableThing> void saveAllGeneric(List<T> serializableThings, String table) {
        saveAllGeneric(serializableThings, table, null);
    }

    @Override
    public <T extends SerializableThing> void saveAllGeneric(List<T> serializableThings, String table, @Nullable Class<T> type) {

        String createTempTableSql = "CREATE TEMPORARY TABLE temp_" + table + " (id TEXT PRIMARY KEY, data TEXT)";
        String insertTempTableSql = "INSERT INTO temp_" + table + " (id, data) VALUES (?, ?) ON CONFLICT(id) DO UPDATE SET data = excluded.data";
        String deleteOldRecordsSql = "DELETE FROM " + tablePrefix + table + " WHERE id NOT IN (SELECT id FROM temp_" + table + ")";

        try {
            connection.setAutoCommit(false);

            // create the table before inserting data
            try (PreparedStatement createTempTableStmt = connection.prepareStatement(createTempTableSql)) {
                createTempTableStmt.execute();
            } catch (SQLException e) {
                Logging.errorLog("Failed to create temporary table for saving objects to: " + table + " due to SQLite exception!", e);
            }

            try (PreparedStatement insertTempTableStmt = connection.prepareStatement(insertTempTableSql)) {

                for (SerializableThing serializableThing : serializableThings) {
                    insertTempTableStmt.setString(1, serializableThing.getId());
                    insertTempTableStmt.setString(2, serializer.serialize(serializableThing));
                    insertTempTableStmt.addBatch();
                }
                insertTempTableStmt.executeBatch();

                try (PreparedStatement deleteStmt = connection.prepareStatement(deleteOldRecordsSql)) {
                    deleteStmt.executeUpdate();
                }

                try (PreparedStatement replaceTableStmt = connection.prepareStatement("INSERT OR REPLACE INTO " + tablePrefix + table + " (id, data) SELECT id, data FROM temp_" + table)) {
                    replaceTableStmt.executeUpdate();
                }

                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                Logging.errorLog("Failed to save objects to: " + table + " due to SQLite exception!", e);
            } finally {
                // Drop the temporary table to ensure it is removed
                try (PreparedStatement dropTempTableStmt = connection.prepareStatement("DROP TABLE IF EXISTS temp_" + table)) {
                    dropTempTableStmt.execute();
                } catch (SQLException e) {
                    Logging.errorLog("Failed to drop temporary table for saving objects to: " + table + " due to SQLite exception!", e);
                } finally {
                    connection.setAutoCommit(true);
                }
            }
        } catch (SQLException e) {
            Logging.errorLog("Failed to manage transaction for saving objects to: " + table + " due to SQLite exception!", e);
        }
    }


    @Override
    public <T extends SerializableThing> void saveGeneric(T serializableThing, String table) {
        String sql = "INSERT INTO " + tablePrefix + table + " (id, data) VALUES (?, ?) ON CONFLICT(id) DO UPDATE SET data = excluded.data";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serializableThing.getId());
            statement.setString(2, serializer.serialize(serializableThing));
            statement.execute();
        } catch (SQLException e) {
            Logging.errorLog("Failed to save object to:" + table + ", to: SQLite!", e);
        }
    }

    @Override
    public void deleteGeneric(String id, String table) {
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
        SerializableBarrel serializableBarrel = getGeneric(id.toString(), "barrels", SerializableBarrel.class);
        if (serializableBarrel != null) {
            return serializableBarrel.toBarrel();
        }
        return null;
    }

    @Override
    public Collection<Barrel> getAllBarrels(World world) {
        return getAllGeneric("barrels", SerializableBarrel.class).stream()
            .map(SerializableBarrel::toBarrel)
            .filter(barrel -> barrel.getSpigot().getWorld().equals(world))
            .toList();
    }

    @Override
    public void saveAllBarrels(Collection<Barrel> barrels) {
        List<SerializableBarrel> serializableBarrels = barrels.stream()
            .filter(it -> it.getBounds() != null)
            .map(SerializableBarrel::new)
            .toList();
        saveAllGeneric(serializableBarrels, "barrels");
    }

    @Override
    public void saveBarrel(Barrel barrel) {
        saveGeneric(new SerializableBarrel(barrel), "barrels");
    }

    @Override
    public void deleteBarrel(UUID id) {
        deleteGeneric(id.toString(), "barrels");
    }

    @Override
    public BCauldron getCauldron(UUID id) {
        SerializableCauldron serializableCauldron = getGeneric(id.toString(), "cauldrons", SerializableCauldron.class);
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
    public void saveAllCauldrons(Collection<BCauldron> cauldrons) {
        List<SerializableCauldron> serializableCauldrons = cauldrons.stream()
            .map(SerializableCauldron::new)
            .toList();
        saveAllGeneric(serializableCauldrons, "cauldrons");
    }

    @Override
    public void saveCauldron(BCauldron cauldron) {
        saveGeneric(new SerializableCauldron(cauldron), "cauldrons");
    }

    @Override
    public void deleteCauldron(UUID id) {
        deleteGeneric(id.toString(), "cauldrons");
    }

    @Override
    public BPlayer getPlayer(UUID playerUUID) {
        SerializableBPlayer serializableBPlayer = getGeneric(playerUUID.toString(), "players", SerializableBPlayer.class);
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
    public void saveAllPlayers(Collection<BPlayer> players) {
        List<SerializableBPlayer> serializableBPlayers = players.stream()
            .map(SerializableBPlayer::new)
            .toList();
        saveAllGeneric(serializableBPlayers, "players");
    }

    @Override
    public void savePlayer(BPlayer player) {
        saveGeneric(new SerializableBPlayer(player), "players");
    }

    @Override
    public void deletePlayer(UUID playerUUID) {
        deleteGeneric(playerUUID.toString(), "players");
    }

    @Override
    public Wakeup getWakeup(UUID id) {
        SerializableWakeup serializableWakeup = getGeneric(id.toString(), "wakeups", SerializableWakeup.class);
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
    public void saveAllWakeups(Collection<Wakeup> wakeups) {
        List<SerializableWakeup> serializableWakeups = wakeups.stream()
            .map(SerializableWakeup::new)
            .toList();
        saveAllGeneric(serializableWakeups, "wakeups");
    }

    @Override
    public void saveWakeup(Wakeup wakeup) {
        saveGeneric(new SerializableWakeup(wakeup), "wakeups");
    }

    @Override
    public void deleteWakeup(UUID id) {
        deleteGeneric(id.toString(), "wakeups");
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
