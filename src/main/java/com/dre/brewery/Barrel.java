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

package com.dre.brewery;

import com.dre.brewery.api.events.barrel.BarrelAccessEvent;
import com.dre.brewery.api.events.barrel.BarrelCreateEvent;
import com.dre.brewery.api.events.barrel.BarrelDestroyEvent;
import com.dre.brewery.api.events.barrel.BarrelRemoveEvent;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.integration.Hook;
import com.dre.brewery.integration.barrel.LogBlockBarrel;
import com.dre.brewery.lore.BrewLore;
import com.dre.brewery.utility.BoundingBox;
import com.dre.brewery.utility.Logging;
import com.dre.brewery.utility.MinecraftVersion;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A Multi Block Barrel with Inventory
 */
@Getter
@Setter
public class Barrel extends BarrelBody implements InventoryHolder {

    private static final Map<UUID, Map<BlockVector, Barrel>> barrels = new HashMap<>();
    private static final Map<UUID, Map<BlockVector, Barrel>> barrelSpigots = new HashMap<>();
    private static final Config config = ConfigManager.getConfig(Config.class);
    private static final Lang lang = ConfigManager.getConfig(Lang.class);

    private boolean checked; // Checked by the random BarrelCheck routine
    private Inventory inventory;
    private float time;
    private final UUID id;

    /**
     * Create a new Barrel
     */
    public Barrel(Block spigot) {
        super(spigot);
        this.inventory = Bukkit.createInventory(this, isLarge() ? config.getBarrelInvSizeLarge() * 9 : config.getBarrelInvSizeSmall() * 9, lang.getEntry("Etc_Barrel"));
        this.id = UUID.randomUUID();
    }

    /**
     * Load from File
     * <p>If async: true, The Barrel Bounds will not be recreated when missing/corrupt, getBody().getBounds() will be null if it needs recreating
     * Note from Jsinco, async is now checked using Bukkit.isPrimaryThread().^
     */
    public Barrel(Block spigot, BoundingBox bounds, @Nullable Map<String, Object> items, float time, UUID id) {
        super(spigot, bounds);
        this.inventory = Bukkit.createInventory(this, isLarge() ? config.getBarrelInvSizeLarge() * 9 : config.getBarrelInvSizeSmall() * 9, lang.getEntry("Etc_Barrel"));
        if (items != null) {
            for (String slot : items.keySet()) {
                if (items.get(slot) instanceof ItemStack) {
                    this.inventory.setItem(Integer.parseInt(slot), (ItemStack) items.get(slot));
                }
            }
        }
        this.time = time;
        this.id = id;
    }

    public Barrel(Block spigot, BoundingBox bounds, ItemStack[] items, float time, UUID id) {
        super(spigot, bounds);
        this.inventory = Bukkit.createInventory(this, isLarge() ? config.getBarrelInvSizeLarge() * 9 : config.getBarrelInvSizeSmall() * 9, lang.getEntry("Etc_Barrel"));
        if (items != null) {
            for (int slot = 0; slot < items.length; slot++) {
                if (items[slot] != null) {
                    this.inventory.setItem(slot, items[slot]);
                }
            }
        }
        this.time = time;
        this.id = id;
    }

    public static void onUpdate() {
        for (Map<BlockVector, Barrel> barrelMap : barrelSpigots.values()) {
            // A Minecraft day is 20 min, so add 1/20 to the time every minute
            for (Barrel barrel : barrelMap.values()) {
                if (barrel != null) {
                    barrel.time += (float) (1.0 / config.getAgingYearDuration());
                }
            }
        }
    }

    public static void addBarrels(Collection<Barrel> list) {
        list.forEach(Barrel::addBarrel);
    }

    public static void addBarrel(Barrel barrel) {
        Location spigot = barrel.getSpigot().getLocation();
        UUID worldUUID = spigot.getWorld().getUID();
        BlockVector position = spigot.toVector().toBlockVector();
        barrelSpigots.computeIfAbsent(worldUUID, ignored -> new HashMap<>()).put(position, barrel);
        Map<BlockVector, Barrel> barrelMap = barrels.computeIfAbsent(worldUUID, ignored -> new HashMap<>());
        barrel.getBounds().getPositions()
            .forEach(block -> barrelMap.put(block, barrel));
    }

    public static Collection<Barrel> getBarrels() {
        return barrelSpigots.values().stream()
            .map(Map::values)
            .flatMap(Collection::stream)
            .toList();
    }

    public static Collection<Barrel> getBarrels(World world) {
        return barrelSpigots.computeIfAbsent(world.getUID(), ignored -> new HashMap<>())
            .values();
    }

    public boolean hasPermsOpen(Player player, PlayerInteractEvent event) {
        if (isLarge()) {
            if (!player.hasPermission("brewery.openbarrel.big")) {
                lang.sendEntry(player, "Error_NoBarrelAccess");
                return false;
            }
        } else {
            if (!player.hasPermission("brewery.openbarrel.small")) {
                lang.sendEntry(player, "Error_NoBarrelAccess");
                return false;
            }
        }

        // Call event
        BarrelAccessEvent accessEvent = new BarrelAccessEvent(this, player, event.getClickedBlock(), event.getBlockFace());
        // Listened to by IntegrationListener
        BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(accessEvent);
        return !accessEvent.isCancelled();
    }

    /**
     * Ask for permission to destroy barrel
     */
    public boolean hasPermsDestroy(Player player, Block block, BarrelDestroyEvent.Reason reason) {
        // Listened to by LWCBarrel (IntegrationListener)
        BarrelDestroyEvent destroyEvent = new BarrelDestroyEvent(this, block, reason, player);
        BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(destroyEvent);
        return !destroyEvent.isCancelled();
    }

    /**
     * player opens the barrel
     */
    public void open(Player player) {
        if (inventory == null) {
            this.inventory = Bukkit.createInventory(this, isLarge() ? config.getBarrelInvSizeLarge() * 9 : config.getBarrelInvSizeSmall() * 9, lang.getEntry("Etc_Barrel"));
        } else {
            if (time > 0) {
                // if nobody has the inventory opened
                if (inventory.getViewers().isEmpty()) {
                    // if inventory contains potions
                    if (inventory.contains(Material.POTION)) {
                        BarrelWoodType wood = this.getWood();
                        long loadTime = System.nanoTime();
                        for (ItemStack item : inventory.getContents()) {
                            if (item != null) {
                                Brew brew = Brew.get(item);
                                if (brew != null) {
                                    brew.age(item, time, wood);
                                }
                            }
                        }
                        loadTime = System.nanoTime() - loadTime;
                        float ftime = (float) (loadTime / 1000000.0);
                        Logging.debugLog("opening Barrel with potions (" + ftime + "ms)");
                    }
                }
            }
        }
        // reset barreltime, potions have new age
        time = 0;

        if (Hook.LOGBLOCK.isEnabled()) {
            try {
                LogBlockBarrel.openBarrel(player, inventory, spigot.getLocation());
            } catch (Throwable e) {
                Logging.errorLog("Failed to Log Barrel to LogBlock!", e);
            }
        }
        player.openInventory(inventory);
    }

    public void playOpeningSound() {
        float randPitch = (float) (Math.random() * 0.1);
        Location location = getSpigot().getLocation();
        if (location.getWorld() == null) return;
        if (isLarge()) {
            location.getWorld().playSound(location, Sound.BLOCK_CHEST_OPEN, SoundCategory.BLOCKS, 0.4f, 0.55f + randPitch);
            //getSpigot().getWorld().playSound(getSpigot().getLocation(), Sound.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 0.5f, 0.6f + randPitch);
            location.getWorld().playSound(location, Sound.BLOCK_BREWING_STAND_BREW, SoundCategory.BLOCKS, 0.4f, 0.45f + randPitch);
        } else {
            location.getWorld().playSound(location, Sound.BLOCK_BARREL_OPEN, SoundCategory.BLOCKS, 0.5f, 0.8f + randPitch);
        }
    }

    public void playClosingSound() {
        float randPitch = (float) (Math.random() * 0.1);
        Location location = getSpigot().getLocation();
        if (location.getWorld() == null) return;
        if (isLarge()) {
            location.getWorld().playSound(location, Sound.BLOCK_BARREL_CLOSE, SoundCategory.BLOCKS, 0.5f, 0.5f + randPitch);
            location.getWorld().playSound(location, Sound.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 0.2f, 0.6f + randPitch);
        } else {
            location.getWorld().playSound(location, Sound.BLOCK_BARREL_CLOSE, SoundCategory.BLOCKS, 0.5f, 0.8f + randPitch);
        }
    }

    @Override
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }


    /**
     * @deprecated just use hasBlock
     */
    @Deprecated
    public boolean hasWoodBlock(Block block) {
        return hasBlock(block);
    }

    /**
     * @deprecated just use hasBlock
     */
    @Deprecated
    public boolean hasStairsBlock(Block block) {
        return hasBlock(block);
    }

    /**
     * Get the Barrel by Block, null if that block is not part of a barrel
     */
    @Nullable
    public static Barrel get(Block block) {
        if (block == null) {
            return null;
        }
        Material type = block.getType();
        if (BarrelAsset.isBarrelAsset(BarrelAsset.FENCE, type) || BarrelAsset.isBarrelAsset(BarrelAsset.SIGN, type)) {
            return getBySpigot(block);
        } else {
            return getByWood(block);
        }
    }

    private static int getYOffset(Block block1, Block block2) {
        if (!block2.equals(block1)) {
            return (byte) (block1.getY() - block2.getY());
        }
        return 0;
    }

    /**
     * Get the Barrel by Sign or Spigot (Fastest)
     */
    @Nullable
    public static Barrel getBySpigot(Block spigot) {
        // convert spigot if neccessary
        Set<Block> spigots = BarrelBody.getSpigotOfSign(spigot);
        for (Block possibleSpigot : spigots) {
            Location location = possibleSpigot.getLocation();
            if (location.getWorld() == null) {
                continue;
            }
            UUID worldUuid = location.getWorld().getUID();
            Barrel barrel = barrelSpigots.computeIfAbsent(worldUuid, ignored -> new HashMap<>()).get(location.toVector().toBlockVector());
            if (barrel != null) {
                return barrel;
            }
        }
        return null;
    }

    /**
     * Get the barrel by its corpus (Wood Planks, Stairs)
     */
    @Nullable
    public static Barrel getByWood(Block wood) {
        if (BarrelAsset.isBarrelAsset(BarrelAsset.PLANKS, wood.getType()) || BarrelAsset.isBarrelAsset(BarrelAsset.STAIRS, wood.getType())) {
            UUID worldUuid = wood.getWorld().getUID();
            BlockVector position = wood.getLocation().toVector().toBlockVector();
            return barrels.computeIfAbsent(worldUuid, ignored -> new HashMap<>()).get(position);
        }
        return null;
    }

    /**
     * @param sign   Initiating sign position
     * @param player The player that initiated the creation
     * @return True, if barrel was created
     */
    public static boolean create(Block sign, Player player) {
        Set<Block> spigots = BarrelBody.getSpigotOfSign(sign);

        for (Block spigot : spigots) {
            // Assume if any of the possible spigots are already registered as a barrel no valid barrel can be found.
            if (Barrel.get(spigot) != null) {
                return false;
            }
            if (tryBarrelCreation(spigot, player)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param spigot The barrel spigot block (a sign or fence)
     * @return True, if successfully created a barrel
     */
    private static boolean tryBarrelCreation(Block spigot, Player player) {
        Barrel barrel = new Barrel(spigot);
        if (barrel.getBrokenBlock(true) != null) {
            return false;
        }
        if (BarrelAsset.isBarrelAsset(BarrelAsset.SIGN, spigot.getType())) {
            if (!player.hasPermission("brewery.createbarrel.small")) {
                lang.sendEntry(player, "Perms_NoBarrelCreate");
                return false;
            }
        } else if (!player.hasPermission("brewery.createbarrel.big")) {
            lang.sendEntry(player, "Perms_NoBigBarrelCreate");
            return false;
        }

        BarrelCreateEvent createEvent = new BarrelCreateEvent(barrel, player);
        BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(createEvent);
        if (!createEvent.isCancelled()) {
            addBarrel(barrel);
            return true;
        }
        return false;
    }

    /**
     * Removes a barrel, throwing included potions to the ground
     *
     * @param broken    The Block that was broken
     * @param breaker   The Player that broke it, or null if not known
     * @param dropItems If the items in the barrels inventory should drop to the ground
     */
    @Override
    public void remove(@Nullable Block broken, @Nullable Player breaker, boolean dropItems) {
        BarrelRemoveEvent event = new BarrelRemoveEvent(this, dropItems);
        // Listened to by LWCBarrel (IntegrationListener)
        BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(event);

        if (inventory != null) {
            List<HumanEntity> viewers = new ArrayList<>(inventory.getViewers());
            // Copy List to fix ConcModExc
            for (HumanEntity viewer : viewers) {
                viewer.closeInventory();
            }
            ItemStack[] items = inventory.getContents();
            inventory.clear();
            if (Hook.LOGBLOCK.isEnabled() && breaker != null) {
                try {
                    LogBlockBarrel.breakBarrel(breaker, items, spigot.getLocation());
                } catch (Throwable e) {
                    Logging.errorLog("Failed to Log Barrel-break to LogBlock!", e);
                }
            }
            if (event.willDropItems()) {
                if (getBounds() == null) {
                    Logging.debugLog("Barrel Body is null, can't drop items: " + this.id);
                    clearBarrel(this);
                    return;
                }

                BarrelWoodType wood = this.getWood();
                for (ItemStack item : items) {
                    if (item != null) {
                        Brew brew = Brew.get(item);
                        if (brew != null) {
                            // Brew before throwing
                            brew.age(item, time, wood);
                            PotionMeta meta = (PotionMeta) item.getItemMeta();
                            if (BrewLore.hasColorLore(meta)) {
                                BrewLore lore = new BrewLore(brew, meta);
                                lore.convertLore(false);
                                lore.write();
                                item.setItemMeta(meta);
                            }
                        }
                        // "broken" is the block that destroyed, throw them there!
                        if (broken != null) {
                            broken.getWorld().dropItem(broken.getLocation(), item);
                        } else {
                            spigot.getWorld().dropItem(spigot.getLocation(), item);
                        }
                    }
                }
            }
        }

        clearBarrel(this);
    }

    private static void clearBarrel(Barrel barrel) {
        Block spigot = barrel.getSpigot();
        UUID worldUuid = spigot.getWorld().getUID();
        barrelSpigots.computeIfAbsent(worldUuid, ignored -> new HashMap<>()).remove(spigot.getLocation().toVector().toBlockVector());
        Map<BlockVector, Barrel> barrelMap = barrels.computeIfAbsent(worldUuid, ignored -> new HashMap<>());
        barrel.getBounds().getPositions().forEach(barrelMap::remove);
    }

    @Override
    public boolean regenerateBounds() {
        Logging.debugLog("Regenerating Barrel BoundingBox: " + (bounds == null ? "was null" : "area=" + bounds.area()));
        Block broken = getBrokenBlock(true);
        if (broken != null) {
            this.remove(broken, null, true);
            return false;
        }
        return true;
    }

    /**
     * is this a Large barrel?
     */
    public boolean isLarge() {
        return !isSmall();
    }

    /**
     * is this a Small barrel?
     */
    public boolean isSmall() {
        if (!MinecraftVersion.isFolia()) {
            return BarrelAsset.isBarrelAsset(BarrelAsset.SIGN, spigot.getType());
        }

        // TODO: Invalid scheduling, will always return false if it's Folia
        AtomicBoolean type = new AtomicBoolean(false);
        BreweryPlugin.getScheduler().runTask(spigot.getLocation(),
            () -> type.set(BarrelAsset.isBarrelAsset(BarrelAsset.SIGN, spigot.getType())));
        return type.get();
    }

    /**
     * Are any Barrels in that World
     */
    public static boolean hasDataInWorld(World world) {
        Map<BlockVector, Barrel> barrelMap = barrelSpigots.get(world.getUID());
        return barrelMap != null && !barrelMap.isEmpty();
    }

    /**
     * Clears barrels that are in  world
     */
    public static void clear(World world) {
        barrelSpigots.remove(world.getUID());
        barrels.remove(world.getUID());
    }
}
