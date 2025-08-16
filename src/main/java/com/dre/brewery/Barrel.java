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
import com.github.Anon8281.universalScheduler.UniversalRunnable;
import com.google.common.base.Preconditions;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Multi Block Barrel with Inventory
 */
@Getter
@Setter
public class Barrel extends BarrelBody implements InventoryHolder {

    private static final Map<UUID, List<Barrel>> barrels = new ConcurrentHashMap<>();
    private static final Config config = ConfigManager.getConfig(Config.class);
    private static final Lang lang = ConfigManager.getConfig(Lang.class);
    private static Map<UUID, Integer> checkCounters = new ConcurrentHashMap<>(); // Which Barrel was last checked
    /**
     * -- GETTER --
     * Is this a small barrel?
     */
    private final boolean small;

    private boolean checked; // Checked by the random BarrelCheck routine
    private Inventory inventory;
    private float time;
    private final UUID id;

    /**
     * Create a new Barrel, has to be done in the primary thread
     */
    public Barrel(Block spigot, byte signoffset) {
        super(spigot, signoffset);
        this.small = computeSmall();
        this.inventory = Bukkit.createInventory(this, !small ? config.getBarrelInvSizeLarge() * 9 : config.getBarrelInvSizeSmall() * 9, lang.getEntry("Etc_Barrel"));
        this.id = UUID.randomUUID();
    }

    public Barrel(Block spigot, byte signoffset, boolean isSmall) {
        super(spigot, signoffset);
        this.small = isSmall;
        this.inventory = Bukkit.createInventory(this, !isSmall ? config.getBarrelInvSizeLarge() * 9 : config.getBarrelInvSizeSmall() * 9, lang.getEntry("Etc_Barrel"));
        this.id = UUID.randomUUID();
    }

    /**
     * Load from File
     * <p>If async: true, The Barrel Bounds will not be recreated when missing/corrupt, getBody().getBounds() will be null if it needs recreating
     * Note from Jsinco, async is now checked using Bukkit.isPrimaryThread().^
     */
    public Barrel(Block spigot, byte sign, BoundingBox bounds, @Nullable Map<String, Object> items, float time, UUID id, boolean isSmall) {
        super(spigot, sign, bounds);
        this.small = isSmall;
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

    public Barrel(Block spigot, byte sign, BoundingBox bounds, ItemStack[] items, float time, UUID id, boolean isSmall) {
        super(spigot, sign, bounds);
        this.small = isSmall;
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
        barrels.values()
            .stream()
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .forEach(barrel -> barrel.time += (float) (1.0 / config.getAgingYearDuration()));
        for (UUID worldUuid : barrels.keySet()) {
            List<Barrel> worldBarrels = barrels.get(worldUuid);
            int numBarrels = worldBarrels.size();
            if (checkCounters.getOrDefault(worldUuid, 0) == 0 && numBarrels > 0) {
                Barrel random = worldBarrels.get((int) Math.floor(Math.random() * numBarrels));
                if (random != null) {
                    // You have been selected for a random search
                    // We want to check at least one barrel every time
                    random.checked = false;
                }
                if (numBarrels > 50) {
                    Barrel randomInTheBack = worldBarrels.get(numBarrels - 1 - (int) (Math.random() * (numBarrels >>> 2)));
                    if (randomInTheBack != null) {
                        // Prioritize checking one of the less recently used barrels as well
                        randomInTheBack.checked = false;
                    }
                }
                new BarrelCheck().runTaskTimer(BreweryPlugin.getInstance(), 1, 1);
            }
        }
    }

    public static @NotNull List<Barrel> getBarrels(UUID worldUuid) {
        List<Barrel> worldBarrels = barrels.get(worldUuid);
        return worldBarrels == null ? List.of() : worldBarrels;
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

    /**
     * Get the Barrel by Sign or Spigot (Fastest)
     */
    @Nullable
    public static Barrel getBySpigot(Block sign) {
        // convert spigot if neccessary
        Block spigot = BarrelBody.getSpigotOfSign(sign);

        byte signoffset = 0;
        if (!spigot.equals(sign)) {
            signoffset = (byte) (sign.getY() - spigot.getY());
        }
        List<Barrel> worldBarrels = barrels.get(sign.getWorld().getUID());
        if (worldBarrels == null) {
            return null;
        }
        int i = 0;
        for (Barrel barrel : worldBarrels) {
            if (barrel != null && barrel.isSignOfBarrel(signoffset)) {
                if (barrel.spigot.equals(spigot)) {
                    if (barrel.getSignoffset() == 0 && signoffset != 0) {
                        // Barrel has no signOffset even though we clicked a sign, may be old
                        barrel.setSignoffset(signoffset);
                    }
                    moveMRU(sign.getWorld().getUID(), i);
                    return barrel;
                }
            }
            i++;
        }
        return null;
    }

    /**
     * Get the barrel by its corpus (Wood Planks, Stairs)
     */
    @Nullable
    public static Barrel getByWood(Block wood) {
        if (!BarrelAsset.isBarrelAsset(BarrelAsset.PLANKS, wood.getType()) && !BarrelAsset.isBarrelAsset(BarrelAsset.STAIRS, wood.getType())) {
            return null;
        }
        List<Barrel> worldBarrels = barrels.get(wood.getWorld().getUID());
        if (worldBarrels == null) {
            return null;
        }
        for (int i = 0; i < worldBarrels.size(); i++) {
            Barrel barrel = worldBarrels.get(i);
            if (barrel.getSpigot().getWorld().equals(wood.getWorld()) && barrel.getBounds().contains(wood)) {
                moveMRU(wood.getWorld().getUID(), i);
                return barrel;
            }
        }
        return null;
    }

    // Move Barrel that was recently used more towards the front of the List
    // Optimizes retrieve by Block over time
    private static void moveMRU(UUID worldUuid, int index) {
        if (index < 0) {
            return;
        }
        List<Barrel> worldBarrels = barrels.get(worldUuid);
        if (index >= worldBarrels.size()) {
            return;
        }
        worldBarrels.set(index - 1, worldBarrels.set(index, worldBarrels.get(index - 1)));
    }

    /**
     * creates a new Barrel out of a sign
     */
    public static boolean create(Block sign, Player player) {
        Block spigot = BarrelBody.getSpigotOfSign(sign);

        // Check for already existing barrel at this location
        if (Barrel.get(spigot) != null) return false;

        byte signoffset = 0;
        if (!spigot.equals(sign)) {
            signoffset = (byte) (sign.getY() - spigot.getY());
        }

        Barrel barrel = getBySpigot(spigot);
        if (barrel == null) {
            barrel = new Barrel(spigot, signoffset);
            if (barrel.getBrokenBlock(true) == null) {
                if (BarrelAsset.isBarrelAsset(BarrelAsset.SIGN, spigot.getType())) {
                    if (!player.hasPermission("brewery.createbarrel.small")) {
                        lang.sendEntry(player, "Perms_NoBarrelCreate");
                        return false;
                    }
                } else {
                    if (!player.hasPermission("brewery.createbarrel.big")) {
                        lang.sendEntry(player, "Perms_NoBigBarrelCreate");
                        return false;
                    }
                }
                BarrelCreateEvent createEvent = new BarrelCreateEvent(barrel, player);
                BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(createEvent);
                if (!createEvent.isCancelled()) {
                    barrels.computeIfAbsent(sign.getWorld().getUID(), ignored -> new ArrayList<>()).addFirst(barrel);
                    return true;
                }
            }
        } else {
            if (barrel.getSignoffset() == 0 && signoffset != 0) {
                barrel.setSignoffset(signoffset);
                return true;
            }
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
                    barrels.getOrDefault(spigot.getWorld().getUID(), new ArrayList<>()).remove(this);
                    return;
                }

                BarrelWoodType wood = this.getWood();
                for (ItemStack item : items) {
                    try {
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
                    } catch (Throwable e) {
                        // Sensitive code, we do not want some items to drop, throw an error in the for loop and not
                        // deregister the barrel.
                        e.printStackTrace();
                    }
                }
            }
        }

        barrels.getOrDefault(spigot.getWorld().getUID(), new ArrayList<>()).remove(this);
    }

    @Override
    public boolean regenerateBounds() {
        Logging.debugLog("Regenerating Barrel BoundingBox: " + (bounds == null ? "was null" : "volume=" + bounds.volume()));
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
     * @return true if this is a small barrel
     */
    private boolean computeSmall() {
        Preconditions.checkState(BreweryPlugin.getScheduler().isRegionThread(spigot.getLocation()));
        return BarrelAsset.isBarrelAsset(BarrelAsset.SIGN, spigot.getType());
    }

    /**
     * @param spigotPosition Position of spigot
     * @return Future on whether this barrel is a small barrel, the future will be in an appropriate thread for modifying the world in that position
     */
    public static CompletableFuture<Boolean> computeSmall(Location spigotPosition) {
        if (!MinecraftVersion.isFolia()) {
            return CompletableFuture.completedFuture(BarrelAsset.isBarrelAsset(BarrelAsset.SIGN, spigotPosition.getBlock().getType()));
        }

        CompletableFuture<Boolean> output = new CompletableFuture<>();
        BreweryPlugin.getScheduler().runTask(spigotPosition,
            () -> output.complete(BarrelAsset.isBarrelAsset(BarrelAsset.SIGN, spigotPosition.getBlock().getType())));
        return output;
    }

    /**
     * returns the fence above/below a block, itself if there is none
     */
    public static Block getSpigotOfSign(Block block) {
        return BarrelBody.getSpigotOfSign(block);
    }

    /**
     * Are any Barrels in that World
     */
    public static boolean hasDataInWorld(World world) {
        return barrels.containsKey(world.getUID()) && !barrels.get(world.getUID()).isEmpty();
    }

    /**
     * unloads barrels that are in a unloading world
     */
    public static void onUnload(World world) {
        barrels.remove(world.getUID());
    }

    public static void registerBarrel(Barrel barrel) {
        barrels.computeIfAbsent(barrel.spigot.getWorld().getUID(), ignored -> new ArrayList<>())
            .add(barrel);
    }

    public static List<Barrel> getAllBarrels() {
        return barrels.values().stream()
            .flatMap(List::stream)
            .filter(Objects::nonNull)
            .toList();
    }

    public static class BarrelCheck extends UniversalRunnable {
        @Override
        public void run() {
            barrels.keySet()
                .forEach(worldUuid -> {
                    int counter = checkCounters.computeIfAbsent(worldUuid, ignored -> -1);
                    List<Barrel> worldBarrels = barrels.get(worldUuid);
                    counter = counter + 1 % worldBarrels.size();
                    while (counter < worldBarrels.size()) {
                        Barrel barrel = worldBarrels.get(counter++);
                        if (barrel.checked) {
                            continue;
                        }
                        BreweryPlugin.getScheduler().runTask(barrel.getSpigot().getLocation(), () -> {
                            Block broken = barrel.getBrokenBlock(false);
                            if (broken != null) {
                                Logging.debugLog("Barrel at "
                                    + broken.getWorld().getName() + "/" + broken.getX() + "/" + broken.getY() + "/" + broken.getZ()
                                    + " has been destroyed unexpectedly, contents will drop");
                                // remove the barrel if it was destroyed
                                barrel.remove(broken, null, true);
                            } else {
                                // Dont check this barrel again, its enough to check it once after every restart (and when randomly chosen)
                                // as now this is only the backup if we dont register the barrel breaking,
                                // for example when removing it with some world editor
                                barrel.checked = true;
                            }
                        });
                        return;
                    }
                    cancel();
                });
        }

    }

}
