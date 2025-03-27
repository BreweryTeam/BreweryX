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

import com.dre.brewery.api.events.IngedientAddEvent;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.recipe.BCauldronRecipe;
import com.dre.brewery.recipe.RecipeItem;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.MaterialUtil;
import com.dre.brewery.utility.MinecraftVersion;
import com.dre.brewery.utility.Tuple;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class BCauldron {

    private static final MinecraftVersion VERSION = BreweryPlugin.getMCVersion();
    private static final Config config = ConfigManager.getConfig(Config.class);
    private static final Lang lang = ConfigManager.getConfig(Lang.class);
    public static final int PARTICLEPAUSE = 15;
    public static Random particleRandom = new Random();
    private static final Set<UUID> plInteracted = new HashSet<>(); // Interact Event helper
    @Getter
    public static Map<Block, BCauldron> bcauldrons = new ConcurrentHashMap<>(); // All active cauldrons. Mapped to their block for fast retrieve

    private BIngredients ingredients = new BIngredients();
    private final Block block;
    private int state = 0;
    private boolean changed = false; // Not really needed anymore
    private BCauldronRecipe particleRecipe; // null if we haven't checked, empty if there is none
    private Color particleColor;
    private final Location particleLocation;
    private final UUID id;

    public BCauldron(Block block) {
        this.block = block;
        this.particleLocation = block.getLocation().add(0.5, 0.9, 0.5);
        this.id = UUID.randomUUID();
    }

    // loading from file
    public BCauldron(Block block, BIngredients ingredients, int state, UUID id) {
        this.block = block;
        this.state = state;
        this.ingredients = ingredients;
        particleLocation = block.getLocation().add(0.5, 0.9, 0.5);
        this.id = id;
    }

    /**
     * Updates this Cauldron, increasing the cook time and checking for Heatsource
     *
     * @return false if Cauldron needs to be removed
     */
    public boolean onUpdate() {
        // add a minute to cooking time
        if (!BUtil.isChunkLoaded(block)) {
            increaseState();
        } else {
            if (!MaterialUtil.isWaterCauldron(block.getType())) {
                // Catch any WorldEdit etc. removal
                return false;
            }
            // Check if fire still alive
            if (MaterialUtil.isCauldronHeatSource(block.getRelative(BlockFace.DOWN))) {
                increaseState();
            }
        }
        return true;
    }

    /**
     * Will add a minute to the cooking time
     */
    public void increaseState() {
        state++;
        if (changed) {
            ingredients = ingredients.copy();
            changed = false;
        }
        particleColor = null;
    }

    // add an ingredient to the cauldron
    public void add(ItemStack ingredient, RecipeItem rItem) {
        if (ingredient == null || ingredient.getType() == Material.AIR) return;
        if (changed) {
            ingredients = ingredients.copy();
            changed = false;
        }

        particleRecipe = null;
        particleColor = null;
        ingredients.add(ingredient, rItem);
        block.getWorld().playEffect(block.getLocation(), Effect.EXTINGUISH, 0);
        if (state > 0) {
            state--;
        }
        if (config.isEnableCauldronParticles() && !config.isMinimalParticles()) {
            // Few little sparks and lots of water splashes. Offset by 0.2 in x and z
            block.getWorld().spawnParticle(Particle.SPELL_INSTANT, particleLocation, 2, 0.2, 0, 0.2);
            block.getWorld().spawnParticle(Particle.WATER_SPLASH, particleLocation, 10, 0.2, 0, 0.2);
        }
    }


    // get cauldron by Block
    @Nullable
    public static BCauldron get(Block block) {
        return bcauldrons.get(block);
    }

    // get cauldron from block and add given ingredient
    // Calls the IngredientAddEvent and may be cancelled or changed
    public static boolean ingredientAdd(Block block, ItemStack ingredient, Player player) {
        // if not empty
        if (MaterialUtil.getFillLevel(block) != MaterialUtil.EMPTY) {

            if (!BCauldronRecipe.acceptedMaterials.contains(ingredient.getType()) && !ingredient.hasItemMeta()) {
                // Extremely fast way to check for most items
                return false;
            }
            // If the Item is on the list, or customized, we have to do more checks
            RecipeItem rItem = RecipeItem.getMatchingRecipeItem(ingredient, false);
            if (rItem == null) {
                return false;
            }

            BCauldron bcauldron = get(block);
            if (bcauldron == null) {
                bcauldron = new BCauldron(block);
                BCauldron.bcauldrons.put(block, bcauldron);
            }

            IngedientAddEvent event = new IngedientAddEvent(player, block, bcauldron, ingredient.clone(), rItem);
            BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                bcauldron.add(event.getIngredient(), event.getRecipeItem());
                //P.p.debugLog("Cauldron add: t2 " + ((t2 - t1) / 1000) + " t3: " + ((t3 - t2) / 1000) + " t4: " + ((t4 - t3) / 1000) + " t5: " + ((t5 - t4) / 1000) + "µs");
                return event.willTakeItem();
            } else {
                return false;
            }
        }
        return false;
    }

    // fills players bottle with cooked brew
    public boolean fill(Player player, Block block) {
        if (!player.hasPermission("brewery.cauldron.fill")) {
            lang.sendEntry(player, "Perms_NoCauldronFill");
            return true;
        }
        ItemStack potion = ingredients.cook(state, player);
        if (potion == null) return false;

        if (VERSION.isOrLater(MinecraftVersion.V1_13)) {
            BlockData data = block.getBlockData();
            if (!(data instanceof Levelled)) {
                bcauldrons.remove(block);
                return false;
            }
            Levelled cauldron = ((Levelled) data);
            if (cauldron.getLevel() <= 0) {
                bcauldrons.remove(block);
                return false;
            }

            // If the Water_Cauldron type exists and the cauldron is on last level
            if (MaterialUtil.WATER_CAULDRON != null && cauldron.getLevel() == 1) {
                // Empty Cauldron
                block.setType(Material.CAULDRON);
                bcauldrons.remove(block);
            } else {
                cauldron.setLevel(cauldron.getLevel() - 1);

                // Update the new Level to the Block
                // We have to use the BlockData variable "data" here instead of the casted "cauldron"
                // otherwise < 1.13 crashes on plugin load for not finding the BlockData Class
                block.setBlockData(data);

                if (cauldron.getLevel() <= 0) {
                    bcauldrons.remove(block);
                } else {
                    changed = true;
                }
            }

        } else {
            @SuppressWarnings("deprecation")
            byte data = block.getData();
            if (data > 3) {
                data = 3;
            } else if (data <= 0) {
                bcauldrons.remove(block);
                return false;
            }
            data -= 1;
            MaterialUtil.setData(block, data);

            if (data == 0) {
                bcauldrons.remove(block);
            } else {
                changed = true;
            }
        }
        if (VERSION.isOrLater(MinecraftVersion.V1_9)) {
            block.getWorld().playSound(block.getLocation(), Sound.ITEM_BOTTLE_FILL, 1f, 1f);
        }
        // Bukkit Bug, inventory not updating while in event so this
        // will delay the give
        // but could also just use deprecated updateInventory()
        giveItem(player, potion);
        // player.getInventory().addItem(potion);
        // player.getInventory().updateInventory();
        return true;
    }

    // prints the current cooking time to the player
    public static void printTime(Player player, Block block) {
        if (!player.hasPermission("brewery.cauldron.time")) {
            lang.sendEntry(player, "Error_NoPermissions");
            return;
        }
        BCauldron bcauldron = get(block);
        if (bcauldron != null) {
            if (bcauldron.state > 1) {
                lang.sendEntry(player, "Player_CauldronInfo1", "" + bcauldron.state);
            } else {
                lang.sendEntry(player, "Player_CauldronInfo2");
            }
        }
    }

    public void cookEffect() {
        if (BUtil.isChunkLoaded(block) && MaterialUtil.isCauldronHeatSource(block.getRelative(BlockFace.DOWN))) {
            Color color = getParticleColor();
            // Colorable spirally spell, 0 count enables color instead of the offset variables
            // Configurable RGB color. The last parameter seems to control the hue and motion, but I couldn't find
            // how exactly in the client code. 1025 seems to be the best for color brightness and upwards motion

            if (VERSION.isOrLater(MinecraftVersion.V1_21)) {
                block.getWorld().spawnParticle(Particle.SPELL_MOB, getRandParticleLoc(), 0, color);
            } else {
                block.getWorld().spawnParticle(Particle.SPELL_MOB, getRandParticleLoc(), 0,
                    ((double) color.getRed()) / 255.0,
                    ((double) color.getGreen()) / 255.0,
                    ((double) color.getBlue()) / 255.0,
                    1025.0);
            }

            if (config.isMinimalParticles()) {
                return;
            }

            if (particleRandom.nextFloat() > 0.85) {
                // Dark pixely smoke cloud at 0.4 random in x and z
                // 0 count enables direction, send to y = 1 with speed 0.09
                block.getWorld().spawnParticle(Particle.SMOKE_LARGE, getRandParticleLoc(), 0, 0, 1, 0, 0.09);
            }
            if (particleRandom.nextFloat() > 0.2) {
                // A Water Splash with 0.2 offset in x and z
                block.getWorld().spawnParticle(Particle.WATER_SPLASH, particleLocation, 1, 0.2, 0, 0.2);
            }

            if (VERSION.isOrLater(MinecraftVersion.V1_13) && particleRandom.nextFloat() > 0.4) {
                // Two hovering pixely dust clouds, a bit of offset and with DustOptions to give some color and size
                block.getWorld().spawnParticle(Particle.REDSTONE, particleLocation, 2, 0.15, 0.2, 0.15, new Particle.DustOptions(color, 1.5f));
            }
        }
    }

    private Location getRandParticleLoc() {
        return new Location(particleLocation.getWorld(),
            particleLocation.getX() + (particleRandom.nextDouble() * 0.8) - 0.4,
            particleLocation.getY(),
            particleLocation.getZ() + (particleRandom.nextDouble() * 0.8) - 0.4);
    }

    /**
     * Get or calculate the particle color from the current best Cauldron Recipe
     * Also calculates the best Cauldron Recipe if not yet done
     *
     * @return the Particle Color, after potentially calculating it
     */
    @NotNull
    public Color getParticleColor() {
        if (state < 1) {
            return Color.fromRGB(153, 221, 255); // Bright Blue
        }
        if (particleColor != null) {
            return particleColor;
        }
        if (particleRecipe == null) {
            // Check for Cauldron Recipe
            particleRecipe = ingredients.getCauldronRecipe();
        }

        List<Tuple<Integer, Color>> colorList = null;
        if (particleRecipe != null) {
            colorList = particleRecipe.getParticleColor();
        }

        if (colorList == null || colorList.isEmpty()) {
            // No color List configured, or no recipe found
            colorList = new ArrayList<>(1);
            colorList.add(new Tuple<>(10, Color.fromRGB(77, 166, 255))); // Dark Aqua kind of Blue
        }
        int index = 0;
        while (index < colorList.size() - 1 && colorList.get(index).a() < state) {
            // Find the first index where the colorList Minute is higher than the state
            index++;
        }

        int minute = colorList.get(index).a();
        if (minute > state) {
            // going towards the minute
            int prevPos;
            Color prevColor;
            if (index > 0) {
                // has previous colours
                prevPos = colorList.get(index - 1).a();
                prevColor = colorList.get(index - 1).b();
            } else {
                prevPos = 0;
                prevColor = Color.fromRGB(153, 221, 255); // Bright Blue
            }

            particleColor = BUtil.weightedMixColor(prevColor, prevPos, state, colorList.get(index).b(), minute);
        } else if (minute == state) {
            // reached the minute
            particleColor = colorList.get(index).b();
        } else {
            // passed the last minute configured
            if (index > 0) {
                // We have more than one color, just use the last one
                particleColor = colorList.get(index).b();
            } else {
                // Only have one color, go towards a Gray
                Color nextColor = Color.fromRGB(138, 153, 168); // Dark Teal, Gray
                int nextPos = (int) (minute * 2.6f);

                if (nextPos <= state) {
                    // We are past the next color (Gray) as well, keep using it
                    particleColor = nextColor;
                } else {
                    particleColor = BUtil.weightedMixColor(colorList.get(index).b(), minute, state, nextColor, nextPos);
                }
            }
        }
        //P.p.log("RGB: " + particleColor.getRed() + "|" + particleColor.getGreen() + "|" + particleColor.getBlue());
        return particleColor;
    }

    public static void processCookEffects() {
        if (!config.isEnableCauldronParticles()) return;
        if (bcauldrons.isEmpty()) {
            return;
        }
        final float chance = 1f / PARTICLEPAUSE;

        for (BCauldron cauldron : bcauldrons.values()) {
            if (particleRandom.nextFloat() < chance) {
                BreweryPlugin.getScheduler().runTask(cauldron.block.getLocation(), cauldron::cookEffect);
            }
        }
    }

    public static void clickCauldron(PlayerInteractEvent event) {
        Material materialInHand = event.getMaterial();
        ItemStack item = event.getItem();
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();
        assert clickedBlock != null;

        if (materialInHand == Material.AIR || materialInHand == Material.BUCKET) {
            return;

        } else if (materialInHand == MaterialUtil.CLOCK) {
            printTime(player, clickedBlock);
            return;

            // fill a glass bottle with potion
        } else if (materialInHand == Material.GLASS_BOTTLE) {
            assert item != null;
            if (player.getInventory().firstEmpty() != -1 || item.getAmount() == 1) {
                BCauldron bcauldron = get(clickedBlock);
                if (bcauldron != null) {
                    if (bcauldron.fill(player, clickedBlock)) {
                        event.setCancelled(true);
                        if (player.hasPermission("brewery.cauldron.fill")) {
                            if (item.getAmount() > 1) {
                                item.setAmount(item.getAmount() - 1);
                            } else {
                                BUtil.setItemInHand(event, Material.AIR, false);
                            }
                        }
                    }
                }
            } else {
                event.setCancelled(true);
            }
            return;

            // Ignore Water Buckets
        } else if (materialInHand == Material.WATER_BUCKET) {
            if (VERSION.isOrEarlier(MinecraftVersion.V1_9)) {
                // reset < 1.9 cauldron when refilling to prevent unlimited source of potions
                // We catch >=1.9 cases in the Cauldron Listener
                if (MaterialUtil.getFillLevel(clickedBlock) == 1) {
                    // will only remove when existing
                    BCauldron.remove(clickedBlock);
                }
            }
            return;
        }

        // Check if fire alive below cauldron when adding ingredients
        Block down = clickedBlock.getRelative(BlockFace.DOWN);
        if (MaterialUtil.isCauldronHeatSource(down)) {

            event.setCancelled(true);
            boolean handSwap = false;

            // Interact event is called twice!!!?? in 1.9, once for each hand.
            // Certain Items in Hand cause one of them to be cancelled or not called at all sometimes.
            // We mark if a player had the event for the main hand
            // If not, we handle the main hand in the event for the offhand
            if (VERSION.isOrLater(MinecraftVersion.V1_9)) {
                if (event.getHand() == EquipmentSlot.HAND) {
                    final UUID id = player.getUniqueId();
                    plInteracted.add(id);
                    BreweryPlugin.getScheduler().runTask(() -> plInteracted.remove(id));
                } else if (event.getHand() == EquipmentSlot.OFF_HAND) {
                    if (!plInteracted.remove(player.getUniqueId())) {
                        item = player.getInventory().getItemInMainHand();
                        if (item.getType() != Material.AIR) {
                            materialInHand = item.getType();
                            handSwap = true;
                        } else {
                            item = config.isUseOffhandForCauldron() ? event.getItem() : null;
                        }
                    }
                }
            }
            if (item == null) return;

            if (!player.hasPermission("brewery.cauldron.insert")) {
                lang.sendEntry(player, "Perms_NoCauldronInsert");
                return;
            }
            if (ingredientAdd(clickedBlock, item, player)) {
                boolean isBucket = item.getType().name().endsWith("_BUCKET");
                boolean isBottle = MaterialUtil.isBottle(item.getType());
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);

                    if (isBucket) {
                        giveItem(player, new ItemStack(Material.BUCKET));
                    } else if (isBottle) {
                        giveItem(player, new ItemStack(Material.GLASS_BOTTLE));
                    }
                } else {
                    if (isBucket) {
                        BUtil.setItemInHand(event, Material.BUCKET, handSwap);
                    } else if (isBottle) {
                        BUtil.setItemInHand(event, Material.GLASS_BOTTLE, handSwap);
                    } else {
                        item.setAmount(0);
                    }
                }
            }
        }
    }

    /**
     * Recalculate the Cauldron Particle Recipe
     */
    public static void reload() {
        var scheduler = BreweryPlugin.getScheduler();
        for (BCauldron cauldron : bcauldrons.values()) {
            cauldron.particleRecipe = null;
            cauldron.particleColor = null;

            scheduler.execute(cauldron.block.getLocation(), () -> {
                if (config.isEnableCauldronParticles()) {
                    if (BUtil.isChunkLoaded(cauldron.block) && MaterialUtil.isCauldronHeatSource(cauldron.block.getRelative(BlockFace.DOWN))) {
                        cauldron.getParticleColor();
                    }
                }
            });
        }
    }

    /**
     * reset to normal cauldron
     */
    public static boolean remove(Block block) {
        return bcauldrons.remove(block) != null;
    }

    /**
     * Are any Cauldrons in that World
     */
    public static boolean hasDataInWorld(World world) {
        return bcauldrons.keySet().stream().anyMatch(block -> block.getWorld().equals(world));
    }

    // unloads cauldrons that are in a unloading world
    // as they were written to file just before, this is safe to do
    public static void onUnload(World world) {
        bcauldrons.keySet().removeIf(block -> block.getWorld().equals(world));
    }

    /**
     * Unload all Cauldrons that have are in a unloaded World
     */
    public static void unloadWorlds() {
        List<World> worlds = BreweryPlugin.getInstance().getServer().getWorlds();
        bcauldrons.keySet().removeIf(block -> !worlds.contains(block.getWorld()));
    }

    public static void save(ConfigurationSection config, ConfigurationSection oldData) {
        BUtil.createWorldSections(config);

        if (!bcauldrons.isEmpty()) {
            int id = 0;
            for (BCauldron cauldron : bcauldrons.values()) {
                String worldName = cauldron.block.getWorld().getName();
                String prefix;

                if (worldName.startsWith("DXL_")) {
                    prefix = BUtil.getDxlName(worldName) + "." + id;
                } else {
                    prefix = cauldron.block.getWorld().getUID().toString() + "." + id;
                }

                config.set(prefix + ".block", cauldron.block.getX() + "/" + cauldron.block.getY() + "/" + cauldron.block.getZ());
                if (cauldron.state != 0) {
                    config.set(prefix + ".state", cauldron.state);
                }
                config.set(prefix + ".ingredients", cauldron.ingredients.serializeIngredients());
                id++;
            }
        }
        // copy cauldrons that are not loaded
        if (oldData != null) {
            for (String uuid : oldData.getKeys(false)) {
                if (!config.contains(uuid)) {
                    config.set(uuid, oldData.get(uuid));
                }
            }
        }
    }

    // bukkit bug not updating the inventory while executing event, have to
    // schedule the give
    public static void giveItem(final Player player, final ItemStack item) {
        BreweryPlugin.getScheduler().runTaskLater(() -> player.getInventory().addItem(item), 1L);
    }

}
