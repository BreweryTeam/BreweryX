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

package com.dre.brewery.utility;

import com.dre.brewery.BCauldron;
import com.dre.brewery.Barrel;
import com.dre.brewery.BarrelAsset;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.api.events.barrel.BarrelDestroyEvent;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Lang;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class BUtil {

    /* **************************************** */
    /* *********                      ********* */
    /* *********     Bukkit Utils     ********* */
    /* *********                      ********* */
    /* **************************************** */

    private static final String WITH_DELIMITER = "((?<=%1$s)|(?=%1$s))";
    private static final MinecraftVersion VERSION = BreweryPlugin.getMCVersion();
    private static final Pattern RANGE_PATTERN = Pattern.compile("([-+]?\\d+)\\.\\.([-+]?\\d+)");

    /**
     * Check if the Chunk of a Block is loaded !without loading it in the process!
     */
    public static boolean isChunkLoaded(Block block) {
        return block.getWorld().isChunkLoaded(block.getX() >> 4, block.getZ() >> 4);
    }

    /**
     * Color code a message. Supports HEX colors and default minecraft colors!
     *
     * @param msg The message to color
     * @return The colored message, or null if msg was null
     */
    public static String color(String msg) {
        if (msg == null) return null;
        String[] texts = msg.split(String.format(WITH_DELIMITER, "&"));

        StringBuilder finalText = new StringBuilder();

        for (int i = 0; i < texts.length; i++) {
            if (texts[i].equalsIgnoreCase("&")) {
                //get the next string
                i++;
                if (texts[i].charAt(0) == '#') {
                    finalText.append(net.md_5.bungee.api.ChatColor.of(texts[i].substring(0, 7))).append(texts[i].substring(7));
                } else {
                    finalText.append(ChatColor.translateAlternateColorCodes('&', "&" + texts[i]));
                }
            } else {
                finalText.append(texts[i]);
            }
        }
        return finalText.toString();
    }

    public static List<String> colorArrayList(List<String> list) {
        if (list == null) return null;
        return list.stream().map(BUtil::color).toList();
    }

    public static List<String> colorArray(String... list) {
        if (list == null) return null;
        return Stream.of(list).map(BUtil::color).toList();
    }

    /**
     * Creates a weighted mix between the two given colours
     * <p>where the weight is calculated from the distance of the currentPos to the prev and next
     *
     * @param prevColor  Previous Color
     * @param prevPos    Position of the Previous Color
     * @param currentPos Current Position
     * @param nextColor  Next Color
     * @param nextPos    Position of the Next Color
     * @return Mixed Color
     */
    public static Color weightedMixColor(Color prevColor, int prevPos, int currentPos, Color nextColor, int nextPos) {
        float diffPrev = currentPos - prevPos;
        float diffNext = nextPos - currentPos;
        float total = diffNext + diffPrev;
        float percentNext = diffPrev / total;
        float percentPrev = diffNext / total;

			/*5 #8# 15
			8-5 = 3 -> 3/10
			15-8 = 7 -> 7/10*/

        return Color.fromRGB(
            Math.min(255, (int) ((nextColor.getRed() * percentNext) + (prevColor.getRed() * percentPrev))),
            Math.min(255, (int) ((nextColor.getGreen() * percentNext) + (prevColor.getGreen() * percentPrev))),
            Math.min(255, (int) ((nextColor.getBlue() * percentNext) + (prevColor.getBlue() * percentPrev)))
        );
    }

    /**
     * Sets the Item in the Players hand, depending on which hand he used and if the hand should be swapped
     *
     * @param event   Interact Event to tell which hand the player used
     * @param mat     The Material of the new item
     * @param swapped If true, will set the opposite Hand instead of the one he used
     */
    @SuppressWarnings("deprecation")
    public static void setItemInHand(PlayerInteractEvent event, Material mat, boolean swapped) {
        if (BreweryPlugin.getMCVersion().isOrLater(MinecraftVersion.V1_9)) {
            if ((event.getHand() == EquipmentSlot.OFF_HAND) != swapped) {
                event.getPlayer().getInventory().setItemInOffHand(new ItemStack(mat));
            } else {
                event.getPlayer().getInventory().setItemInMainHand(new ItemStack(mat));
            }
        } else {
            event.getPlayer().setItemInHand(new ItemStack(mat));
        }
    }


    /**
     * returns the Player if online
     */
    public static Player getPlayerfromString(String nameOrUUID) {
        try {
            return Bukkit.getPlayer(UUID.fromString(nameOrUUID));
        } catch (IllegalArgumentException e) {
            return Bukkit.getPlayerExact(nameOrUUID);
        }
    }

    /**
     * Apply a Potion Effect, if player already has this effect, overwrite the existing effect.
     *
     * @param onlyIfStronger Optionally only overwrite if the new one is stronger, i.e. has higher level or longer duration
     */
    public static void reapplyPotionEffect(Player player, PotionEffect effect, boolean onlyIfStronger) {
        BreweryPlugin.getScheduler().execute(player, () -> {
            final PotionEffectType type = effect.getType();
            if (player.hasPotionEffect(type)) {
                PotionEffect plEffect;
                if (VERSION.isOrLater(MinecraftVersion.V1_11)) {
                    plEffect = player.getPotionEffect(type);
                } else {
                    plEffect = player.getActivePotionEffects().stream().filter(e -> e.getType().equals(type)).findAny().get();
                }

                if (!onlyIfStronger ||
                    plEffect.getAmplifier() < effect.getAmplifier() ||
                    (plEffect.getAmplifier() == effect.getAmplifier() && plEffect.getDuration() < effect.getDuration())) {
                    player.removePotionEffect(type);
                } else {
                    return;
                }
            }
            effect.apply(player);
        });
    }

    /**
     * Load A List of Strings from config, if found a single String, will convert to List
     */
    @Nullable
    public static List<String> loadCfgStringList(ConfigurationSection cfg, String path) {
        if (cfg.isString(path)) {
            List<String> list = new ArrayList<>(1);
            list.add(cfg.getString(path));
            return list;
        } else if (cfg.isList(path)) {
            return cfg.getStringList(path);
        }
        return null;
    }


    public static <T> List<T> getListSafely(Object object) {
        if (object == null) {
            return new ArrayList<>();
        }
        if (object instanceof List) {
            return (List<T>) object;
        } else if (object != null) {
            List<T> list = new ArrayList<>(1);
            list.add((T) object);
            return list;
        }
        return null;

    }


    public static <E extends Enum<E>> List<E> getListSafely(Object object, Class<E> mapToEnum) {
        var list = getListSafely(object);
        if (list == null) return null;
        return list.stream().map(it -> getEnumByName(mapToEnum, it.toString())).toList();
    }

    /* **************************************** */
    /* *********                      ********* */
    /* *********     String Utils     ********* */
    /* *********                      ********* */
    /* **************************************** */

    /**
     * Returns the Index of a String from the list that contains this substring
     *
     * @param list      The List in which to search for a substring
     * @param substring Part of the String to search for in each of <tt>list</tt>
     */
    public static int indexOfSubstring(List<String> list, String substring) {
        if (list.isEmpty()) return -1;
        for (int index = 0, size = list.size(); index < size; index++) {
            String string = list.get(index);
            if (string.contains(substring)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Returns the index of a String from the list that starts with 'lineStart', returns -1 if not found;
     */
    public static int indexOfStart(List<String> list, String lineStart) {
        for (int i = 0, size = list.size(); i < size; i++) {
            if (list.get(i).startsWith(lineStart)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Escapes any backslashes or double quotes, and surrounds the string in double quotes if it contains spaces.
     *
     * @param input The input string
     * @return The quoted string
     */
    public static String quote(String input) {
        String escaped = input.replace("\\", "\\\\").replace("\"", "\\\"");
        if (input.contains(" ")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    /**
     * Splits a string by spaces, unless enclosed in double quotes.
     * Uses backslash as escape character for quotes and other backslashes.
     * Multiple spaces will be treated as one space.
     *
     * @param input The input string
     * @return List of strings in the input, quoted strings will have their start and end quotes removed
     */
    public static List<String> splitStringKeepingQuotes(String input) {
        return splitStringKeepingQuotesVerbose(input).strings;
    }

    /**
     * Splits a string by spaces, unless enclosed in double quotes.
     * Uses backslash as escape character for quotes and other backslashes.
     * Multiple spaces will be treated as one space.
     *
     * @param input The input string
     * @return List of strings in the input, quoted strings will have their start and end quotes removed
     */
    public static SplitResult splitStringKeepingQuotesVerbose(String input) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escape = false;

        for (char c : input.toCharArray()) {
            if (escape) {
                current.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (!current.isEmpty()) {
                    result.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            result.add(current.toString());
        }

        return new SplitResult(result, inQuotes);
    }

    public record SplitResult(List<String> strings, boolean inQuotes) {
    }

    /**
     * Replaces the Placeholders %player_name% and %quality% in the given input string
     *
     * @param input   The String to replace the placeholders in
     * @param player  Player Name to replace %player_name%
     * @param quality Quality to replace %quality%
     * @return The String with all placeholders replaced
     */
    public static String applyPlaceholders(String input, String player, int quality) {
        return input.replaceAll("%player_name%", player).replaceAll("%quality%", String.valueOf(quality));
    }

    /* **************************************** */
    /* *********                      ********* */
    /* *********     Brewery Utils    ********* */
    /* *********                      ********* */
    /* **************************************** */

    /**
     * create empty World save Sections
     */
    public static void createWorldSections(ConfigurationSection section) {
        for (World world : BreweryPlugin.getInstance().getServer().getWorlds()) {
            String worldName = world.getName();
            if (worldName.startsWith("DXL_")) {
                worldName = getDxlName(worldName);
            } else {
                worldName = world.getUID().toString();
            }
            section.createSection(worldName);
        }
    }

    /**
     * Returns true if the Block can be destroyed by the Player or something else (null)
     *
     * @param player The Player that destroyed a Block, Null if no Player involved
     * @return True if the Block can be destroyed
     */
    public static boolean blockDestroy(Block block, Player player, BarrelDestroyEvent.Reason reason) {
        if (block == null || block.getType() == null) {
            return true;
        }
        Material type = block.getType();
        if (type == Material.CAULDRON || type == MaterialUtil.WATER_CAULDRON) {
            // will only remove when existing
            BCauldron.remove(block);
            return true;

        } else if (BarrelAsset.isBarrelAsset(BarrelAsset.FENCE, type)) {
            // remove barrel and throw potions on the ground
            Barrel barrel = Barrel.getBySpigot(block);
            if (barrel != null) {
                if (barrel.hasPermsDestroy(player, block, reason)) {
                    barrel.remove(null, player, true);
                    return true;
                } else {
                    return false;
                }
            }
            return true;

        } else if (BarrelAsset.isBarrelAsset(BarrelAsset.SIGN, type)) {
            // remove small Barrels
            Barrel barrel2 = Barrel.getBySpigot(block);
            if (barrel2 != null) {
                if (!barrel2.isLarge()) {
                    if (barrel2.hasPermsDestroy(player, block, reason)) {
                        barrel2.remove(null, player, true);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    barrel2.destroySign();
                }
            }
            return true;

        } else if (BarrelAsset.isBarrelAsset(BarrelAsset.PLANKS, type) || BarrelAsset.isBarrelAsset(BarrelAsset.STAIRS, type)) {
            Barrel barrel3 = Barrel.getByWood(block);
            if (barrel3 != null) {
                if (barrel3.hasPermsDestroy(player, block, reason)) {
                    barrel3.remove(block, player, true);
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    /* **************************************** */
    /* *********                      ********* */
    /* *********     Other Utils      ********* */
    /* *********                      ********* */
    /* **************************************** */

    /**
     * prints a list of Strings at the specified page
     *
     * @param sender The CommandSender to send the Page to
     */
    public static void list(CommandSender sender, ArrayList<String> strings, int page) {
        int pages = (int) Math.ceil(strings.size() / 7F);
        if (page > pages || page < 1) {
            page = 1;
        }

        sender.sendMessage(color("&7-------------- &f" + ConfigManager.getConfig(Lang.class).getEntry("Etc_Page") + " &6" + page + "&f/&6" + pages + " &7--------------"));

        ListIterator<String> iter = strings.listIterator((page - 1) * 7);

        for (int i = 0; i < 7; i++) {
            if (iter.hasNext()) {
                sender.sendMessage(color(iter.next()));
            } else {
                break;
            }
        }
    }

    public static Map<Material, Integer> getMaterialMap(List<String> stringList) {
        Map<Material, Integer> map = new HashMap<>();
        for (String materialString : stringList) {
            String[] drainSplit = materialString.split("/");
            if (drainSplit.length > 1) {
                Material mat = MaterialUtil.getMaterialSafely(drainSplit[0]);
                int strength = BUtil.parseIntOrZero(drainSplit[1]);
//                if (mat == null && hasVault && strength > 0) {
//                    try {
//                        net.milkbowl.vault.item.ItemInfo vaultItem = net.milkbowl.vault.item.Items.itemByString(drainSplit[0]);
//                        if (vaultItem != null) {
//                            mat = vaultItem.getType();
//                        }
//                    } catch (Exception e) {
//                        Logging.errorLog("Could not check vault for Item Name");
//                        e.printStackTrace();
//                    }
//                }
                if (mat != null && strength > 0) {
                    map.put(mat, strength);
                }
            }
        }
        return map;
    }


    public static UUID uuidFromString(String uuid) {
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            Logging.errorLog("UUID is invalid! " + uuid, e);
            return null;
        }
    }

    @Nullable
    public static <E extends Enum<E>> E getEnumByName(Class<E> enumClass, String name) {
        try {
            return Enum.valueOf(enumClass, name.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }


    public static int getItemDespawnRate(World world) {
        YamlConfiguration spigotConfig = Bukkit.spigot().getConfig();

        int worldDespawnRate = spigotConfig.getInt("world-settings." + world.getName() + ".item-despawn-rate", -1);
        if (worldDespawnRate < 0) {
            return spigotConfig.getInt("world-settings.default.item-despawn-rate", 6000);
        }
        return worldDespawnRate;
    }

    /**
     * gets the Name of a DXL World
     */
    public static String getDxlName(String worldName) {
        File dungeonFolder = new File(worldName);
        if (dungeonFolder.isDirectory()) {
            for (File file : dungeonFolder.listFiles()) {
                if (!file.isDirectory()) {
                    if (file.getName().startsWith(".id_")) {
                        return file.getName().substring(1).toLowerCase();
                    }
                }
            }
        }
        return worldName;
    }

    public static List<String> numberRange(int startInclusive, int stopInclusive) {
        return IntStream.range(startInclusive, stopInclusive + 1)
            .mapToObj(String::valueOf)
            .toList();
    }

    public static int getRandomIntInRange(String string) {
        if (string == null) {
            return 0;
        }

        try {
            Matcher matcher = RANGE_PATTERN.matcher(string);

            if (!matcher.matches()) {
                return parseIntOrZero(string);
            }

            int lowerBound = Integer.parseInt(matcher.group(1));
            int upperBound = Integer.parseInt(matcher.group(2));

            Random rand = new Random();
            return rand.nextInt(upperBound - lowerBound + 1) + lowerBound;
        } catch (NumberFormatException ignored) {
            Logging.debugLog("Could not parse integer range: " + string);
        }

        return 0;
    }

    /**
     * Chooses a random element from a list.
     *
     * @param list the list to choose from
     * @param <T>  type of element in list
     * @return a random element, or null if the list is empty
     */
    public static <T> @Nullable T choose(List<T> list) {
        if (list.isEmpty()) {
            return null;
        }
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    /**
     * Finds the minimum element in a collection.
     * If multiple elements have the same minimum value, they will all be returned.
     *
     * @param <T>        the type of elements in the collection
     * @param collection the collection to search for minimum elements
     * @return a list of all minimum elements in the collection, will be empty if the collection is empty
     */
    public static <T extends Comparable<T>> List<T> multiMin(Collection<T> collection) {
        List<T> minValues = new ArrayList<>();
        T min = null;
        for (T t : collection) {
            if (min == null) {
                min = t;
                minValues.add(t);
            } else {
                int compare = t.compareTo(min);
                if (compare < 0) {
                    min = t;
                    minValues.clear();
                    minValues.add(t);
                } else if (compare == 0) {
                    minValues.add(t);
                }
            }
        }
        return minValues;
    }

    /**
     * Determines if two floats are close enough to be considered "equal" (difference < 1e-6)
     *
     * @param a first float
     * @param b second float
     * @return true if the floats are close
     */
    public static boolean isClose(float a, float b) {
        return Math.abs(a - b) < 1e-6;
    }

    public static boolean isInt(String string) {
        try {
            Integer.parseInt(string);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public static OptionalInt parseInt(@Nullable String string) {
        if (string == null) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(string));
        } catch (NumberFormatException ignored) {
            return OptionalInt.empty();
        }
    }

    /**
     * Parses a non-infinite, non-NaN floating-point double.
     *
     * @param string the input string
     * @return the optional
     */
    public static OptionalDouble parseDouble(@Nullable String string) {
        if (string == null) {
            return OptionalDouble.empty();
        }
        try {
            double d = Double.parseDouble(string);
            if (Double.isFinite(d)) {
                return OptionalDouble.of(d);
            }
            return OptionalDouble.empty();
        } catch (NumberFormatException ignored) {
            return OptionalDouble.empty();
        }
    }

    /**
     * Parses a non-infinite, non-NaN floating-point float.
     *
     * @param string the input string
     * @return the optional
     */
    public static OptionalFloat parseFloat(@Nullable String string) {
        if (string == null) {
            return OptionalFloat.empty();
        }
        try {
            float f = Float.parseFloat(string);
            if (Float.isFinite(f)) {
                return OptionalFloat.of(f);
            }
            return OptionalFloat.empty();
        } catch (NumberFormatException ignored) {
            return OptionalFloat.empty();
        }
    }

    public static int parseIntOrZero(@Nullable String string) {
        if (string == null) {
            return 0;
        }

        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public static double parseDoubleOrZero(@Nullable String string) {
        if (string == null) {
            return 0;
        }
        try {
            return Double.parseDouble(string);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public static float parseFloatOrZero(@Nullable String string) {
        if (string == null) {
            return 0;
        }
        try {
            return Float.parseFloat(string);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

}
