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
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class MongoDBStorage extends DataManager {

    private static final String URL = "mongodb+srv://%s:%s@%s/?retryWrites=true&w=majority&appName=BreweryX#%d";
    private static final String[] COLLECTIONS = { "misc", "barrels", "cauldrons", "players", "wakeups" };
    private static final String MONGO_ID = "_id";

    private final MongoClient mongoClient;
    private final MongoDatabase mongoDatabase;
    private final String collectionPrefix;

    public MongoDBStorage(ConfiguredDataManager record) throws StorageInitException {
        super(record.getType());
        try {
            // suppress mongo client's logs
            Logger clientLogger = (Logger) LogManager.getLogger("org.mongodb.driver.client");
            Logger clusterLogger = (Logger) LogManager.getLogger("org.mongodb.driver.cluster");
            if (clientLogger != null) clientLogger.setLevel(Level.ERROR);
            if (clusterLogger != null) clusterLogger.setLevel(Level.ERROR);

            String fullURL = String.format(URL, record.getUsername(), record.getPassword(), record.getAddress(), this.getClass().hashCode());

            this.mongoClient = MongoClients.create(fullURL);
            this.mongoClient.startSession();
            this.mongoDatabase = mongoClient.getDatabase(record.getDatabase());
            this.collectionPrefix = record.getTablePrefix();

            for (String collection : COLLECTIONS) {
                mongoDatabase.createCollection(collectionPrefix + collection); // Create the collection if it doesn't exist
            }
        } catch (Exception e) {
            throw new StorageInitException("Failed to start MongoDB client or get database!", e);
        }
    }

    @Override
    protected void closeConnection() {
        mongoClient.close();
    }

    private boolean collectionExists(String name) {
        return mongoDatabase.listCollectionNames().into(new ArrayList<>()).contains(collectionPrefix + name);
    }

    @Override
    public boolean createTable(String name, int maxIdLength) {
        if (collectionExists(name)) {
            return false;
        }
        mongoDatabase.createCollection(collectionPrefix + name);
        return true;
    }

    @Override
    public boolean dropTable(String name) {
        if (collectionExists(name)) {
            return false;
        }
        mongoDatabase.getCollection(collectionPrefix + name).drop();
        return true;
    }

    @Override
    public <T extends SerializableThing> T getGeneric(String id, String collection, Class<T> type) {
        MongoCollection<T> mongoCollection = mongoDatabase.getCollection(collectionPrefix + collection, type);
        return mongoCollection.find(Filters.eq(MONGO_ID, id)).first();
    }

    private <T extends SerializableThing> T getGeneric(UUID id, String collection, Class<T> type) {
        return getGeneric(id.toString(), collection, type);
    }

    @Override
    public <T extends SerializableThing> List<T> getAllGeneric(String collection, Class<T> type) {
        MongoCollection<T> mongoCollection = mongoDatabase.getCollection(collectionPrefix + collection, type);
        return mongoCollection.find().into(new ArrayList<>());
    }

    @Override
    public <T extends SerializableThing> void saveGeneric(T thing, String collection) {
        MongoCollection<T> mongoCollection = (MongoCollection<T>) mongoDatabase.getCollection(collectionPrefix + collection, thing.getClass());
        mongoCollection.replaceOne(Filters.eq(MONGO_ID, thing.getId()), thing, new ReplaceOptions().upsert(true));
    }

    @Override
    public <T extends SerializableThing> void saveAllGeneric(List<T> things, String collection, @Nullable Class<T> type) {
        assert type != null : "'type' cannot be null when using mongo storage!";
        MongoCollection<T> mongoCollection = mongoDatabase.getCollection(collectionPrefix + collection, type);

        Set<String> thingsIds = things.stream().map(T::getId).collect(Collectors.toSet());
        // Delete objects from the collection that are no longer in the list
        mongoCollection.deleteMany(Filters.not(Filters.in(MONGO_ID, thingsIds)));

        for (T thing : things) {
            mongoCollection.replaceOne(Filters.eq(MONGO_ID, thing.getId()), thing, new ReplaceOptions().upsert(true)); // Upsert to handle both insert and update
        }
    }

    @Override
    public void deleteGeneric(String id, String collection) {
        MongoCollection<SerializableThing> mongoCollection = mongoDatabase.getCollection(collectionPrefix + collection, SerializableThing.class);
        mongoCollection.deleteOne(Filters.eq(MONGO_ID, id));
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
    public void saveAllBarrels(Collection<Barrel> barrels) {
        List<SerializableBarrel> serializableBarrels = barrels.stream()
            .filter(it -> it.getBounds() != null)
            .map(SerializableBarrel::new)
            .toList();
        saveAllGeneric(serializableBarrels, "barrels", SerializableBarrel.class);
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
    public void saveAllCauldrons(Collection<BCauldron> cauldrons) {
        List<SerializableCauldron> serializableCauldrons = cauldrons.stream()
            .map(SerializableCauldron::new)
            .toList();
        saveAllGeneric(serializableCauldrons, "cauldrons", SerializableCauldron.class);
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
    public void saveAllPlayers(Collection<BPlayer> players) {
        List<SerializableBPlayer> serializableBPlayers = players.stream()
            .map(SerializableBPlayer::new)
            .toList();
        saveAllGeneric(serializableBPlayers, "players", SerializableBPlayer.class);
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
    public void saveAllWakeups(Collection<Wakeup> wakeups) {
        List<SerializableWakeup> serializableWakeups = wakeups.stream()
            .map(SerializableWakeup::new)
            .toList();
        saveAllGeneric(serializableWakeups, "wakeups", SerializableWakeup.class);
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
        BreweryMiscData data = getGeneric("misc", "misc", BreweryMiscData.class);
        if (data != null) {
            return data;
        }
        return new BreweryMiscData(System.currentTimeMillis(), 0, new ArrayList<>(), new ArrayList<>(), 0);
    }

    @Override
    public void saveBreweryMiscData(BreweryMiscData data) {
        saveGeneric(data, "misc");
    }
}
