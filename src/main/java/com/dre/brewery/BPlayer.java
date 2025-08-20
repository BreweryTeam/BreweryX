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

import com.dre.brewery.api.events.PlayerEffectEvent;
import com.dre.brewery.api.events.PlayerPukeEvent;
import com.dre.brewery.api.events.PlayerPushEvent;
import com.dre.brewery.api.events.brew.BrewDrinkEvent;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.lore.BrewLore;
import com.dre.brewery.recipe.BEffect;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.Logging;
import com.dre.brewery.utility.MinecraftVersion;
import com.dre.brewery.utility.PermissionUtil;
import com.github.Anon8281.universalScheduler.scheduling.tasks.MyScheduledTask;
import io.papermc.lib.PaperLib;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ToString
@Getter
@Setter
public class BPlayer {

    private static final MinecraftVersion VERSION = BreweryPlugin.getMCVersion();
    private static final Config config = ConfigManager.getConfig(Config.class);
    private static final Lang lang = ConfigManager.getConfig(Lang.class);

    private static final ConcurrentHashMap<String, BPlayer> players = new ConcurrentHashMap<>();// Players uuid and BPlayer
    private static final ConcurrentHashMap<Player, Integer> pTasks = new ConcurrentHashMap<>();// Player and count
    private static MyScheduledTask task;
    private static Random pukeRand;

    private final String uuid;
    private int quality = 0;// = quality of drunkenness * drunkenness
    private int drunkenness = 0;// = amount of drunkenness
    private int offlineDrunk = 0;// drunkenness when gone offline
    private int alcRecovery = -1; // Drunkeness reduce per minute
    private Vector push = new Vector(0, 0, 0);
    private int time = 20;

    public BPlayer(String uuid) {
        this.uuid = uuid;
    }

    // reading from file
    public BPlayer(String uuid, int quality, int drunkenness, int offlineDrunk) {
        this.quality = quality;
        this.drunkenness = drunkenness;
        this.offlineDrunk = offlineDrunk;
        this.uuid = uuid;
    }

    public BPlayer(UUID uuid, int quality, int drunkenness, int offlineDrunk) {
        this(uuid.toString(), quality, drunkenness, offlineDrunk);
    }

    public BPlayer(UUID uuid) {
        this(uuid.toString());
    }

    @Nullable
    public static BPlayer get(OfflinePlayer player) {
        if (!players.isEmpty()) {
            return players.get(player.getUniqueId().toString());
        }
        return null;
    }

    // This method may be slow and should not be used if not needed
    @Nullable
    public static BPlayer getByName(String playerName) {
        for (Map.Entry<String, BPlayer> entry : players.entrySet()) {
            OfflinePlayer p = BreweryPlugin.getInstance().getServer().getOfflinePlayer(UUID.fromString(entry.getKey()));
            String name = p.getName();
            if (name != null) {
                if (name.equalsIgnoreCase(playerName)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    // This method may be slow and should not be used if not needed
    public static boolean hasPlayerbyName(String playerName) {
        for (Map.Entry<String, BPlayer> entry : players.entrySet()) {
            OfflinePlayer p = BreweryPlugin.getInstance().getServer().getOfflinePlayer(UUID.fromString(entry.getKey()));
            if (p != null) {
                String name = p.getName();
                if (name != null) {
                    if (name.equalsIgnoreCase(playerName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static ConcurrentHashMap<String, BPlayer> getPlayers() {
        return players;
    }

    public static boolean isEmpty() {
        return players.isEmpty();
    }

    public static boolean hasPlayer(OfflinePlayer player) {
        return players.containsKey(player.getUniqueId().toString());
    }

    // Create a new BPlayer and add it to the list
    public static BPlayer addPlayer(OfflinePlayer player) {
        BPlayer bPlayer = new BPlayer(player.getUniqueId());
        players.put(player.getUniqueId().toString(), bPlayer);
        return bPlayer;
    }

    public static void remove(OfflinePlayer player) {
        players.remove(player.getUniqueId().toString());
    }


    public static int numDrunkPlayers() {
        return players.size();
    }

    public void remove() {
        for (Iterator<Map.Entry<String, BPlayer>> iterator = players.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, BPlayer> entry = iterator.next();
            if (entry.getValue() == this) {
                iterator.remove();
                return;
            }
        }
    }

    public static void clear() {
        players.clear();
    }

    // Drink a brew and apply effects, etc.
    public static boolean drink(Brew brew, Player player, @Nullable ItemMeta meta, @Nullable PlayerItemConsumeEvent event) {
        BPlayer bPlayer = get(player);
        if (bPlayer == null) {
            bPlayer = addPlayer(player);
        }
        // In this event the added alcohol amount is calculated, based on the sensitivity permission
        BrewDrinkEvent drinkEvent = new BrewDrinkEvent(brew, meta, player, bPlayer, event);
        if (meta != null) {
            BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(drinkEvent);
            if (brew != drinkEvent.getBrew()) brew = drinkEvent.getBrew();
            if (drinkEvent.isCancelled()) {
                if (bPlayer.drunkenness <= 0) {
                    bPlayer.remove();
                }
                return false;
            }
        }

        if (brew.hasRecipe()) {
            brew.getCurrentRecipe().applyDrinkFeatures(player, brew.getQuality());
        }
        BreweryPlugin.getInstance().getBreweryStats().forDrink(brew);

        int brewAlc = drinkEvent.getAddedAlcohol();
        int quality = drinkEvent.getQuality();
        List<PotionEffect> effects = getBrewEffects(brew.getEffects(), quality);

        applyEffects(effects, player, PlayerEffectEvent.EffectType.DRINK);
        if (brewAlc < 0) {
            // If the Drink has negative alcohol, drain some alcohol
            bPlayer.drain(player, -brewAlc);
        } else if (brewAlc > 0) {
            bPlayer.drunkenness += brewAlc;
            if (quality > 0) {
                bPlayer.quality += quality * brewAlc;
            } else {
                bPlayer.quality += brewAlc;
            }

            applyEffects(getQualityEffects(quality, brewAlc), player, PlayerEffectEvent.EffectType.QUALITY);
        }

        if (bPlayer.drunkenness > 100) {
            bPlayer.drinkCap(player);
        }

        if (config.isShowStatusOnDrink()) {
            // Only show the Player his drunkenness if he is already drunk, or this drink changed his drunkenness
            if (brewAlc != 0 || bPlayer.drunkenness > 0) {
                bPlayer.showDrunkeness(player);
            }
        }

        if (bPlayer.drunkenness <= 0) {
            bPlayer.remove();
        }
        return true;
    }

    /**
     * Show the Player his current drunkenness and quality as an Actionbar graphic or when unsupported, in chat
     */
    public void showDrunkeness(Player player) {
        try {
            // It this returns false, then the Action Bar is not supported. Do not repeat the message as it was sent into chat
            if (sendDrunkenessMessage(player)) {
                BreweryPlugin.getScheduler().runTaskLater(() -> sendDrunkenessMessage(player), 40);
                BreweryPlugin.getScheduler().runTaskLater(() -> sendDrunkenessMessage(player), 80);
            }
        } catch (Exception e) {
            Logging.errorLog("Failed to show drunkenness to " + player.getName(), e);
        }
    }

    /**
     * Send one Message to the player, showing his drunkenness or hangover
     *
     * @param player The Player to send the message to
     * @return false if the message should not be repeated.
     */
    public boolean sendDrunkenessMessage(Player player) {
        StringBuilder b = new StringBuilder(100);

        int strength = drunkenness;
        boolean hangover = false;
        if (offlineDrunk > 0) {
            strength = offlineDrunk;
            hangover = true;
        }

        b.append(lang.getEntry(hangover ? "Player_Hangover" : "Player_Drunkeness"));

        // Drunkenness or Hangover Strength Bars
        b.append("§7[");
        b.append(generateBars(strength, hangover));
        b.append("§7] ");

        int quality;
        if (hangover) {
            quality = 11 - getHangoverQuality();
        } else {
            quality = strength > 0 ? getQuality() : 0;
        }

        // Quality Stars
        b.append("§7[");
        b.append(generateStars(quality));
        b.append("§7]");

        final String text = b.toString();
        if (hangover && VERSION.isOrLater(MinecraftVersion.V1_11)) {
            BreweryPlugin.getScheduler().runTaskLater(() -> player.sendTitle("", text, 30, 100, 90), 160);
            return false;
        }
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(text));
            return true;
        } catch (UnsupportedOperationException | NoSuchMethodError e) {
            player.sendMessage(text);
            return false;
        }
    }

    private String generateBars(int strength, boolean hangover) {
        // Generate 25 Bars, color one per 4 drunkenness
        StringBuilder b = new StringBuilder();
        int bars;
        if (strength <= 0) {
            bars = 0;
        } else if (strength == 1) {
            bars = 1;
        } else {
            bars = Math.round(strength / 4.0f);
        }
        int noBars = 25 - bars;
        if (bars > 0) {
            b.append(hangover ? "§c" : "§6");
        }
        for (int addedBars = 0; addedBars < bars; addedBars++) {
            b.append("|");
            if (addedBars == 20) {
                // color the last 4 bars red
                b.append("§c");
            }
        }
        if (noBars > 0) {
            b.append("§0");
            for (; noBars > 0; noBars--) {
                b.append("|");
            }
        }
        return b.toString();
    }

    public String generateBars() {
        return generateBars(offlineDrunk > 0 ? offlineDrunk : drunkenness, offlineDrunk > 0);
    }

    private String generateStars(int quality) {
        // Generate stars representing the quality
        StringBuilder b = new StringBuilder();
        int stars = quality / 2;
        boolean half = quality % 2 > 0;
        int noStars = 5 - stars - (half ? 1 : 0);

        b.append(BrewLore.getQualityColor(quality));
        for (; stars > 0; stars--) {
            b.append("⭑");
        }
        if (half) {
            b.append("⭒");
        }
        if (noStars > 0) {
            b.append("§0");
            for (; noStars > 0; noStars--) {
                b.append("⭑");
            }
        }

        return b.toString();
    }

    public String generateStars() {
        return generateStars(offlineDrunk > 0 ? 11 - getHangoverQuality() : drunkenness > 0 ? getQuality() : 0);
    }

    // Player has drunken too much
    public void drinkCap(Player player) {
        quality = getQuality() * 100;
        drunkenness = 100;
        if (config.isEnableKickOnOverdrink() && !player.hasPermission("brewery.bypass.overdrink")) {
            BreweryPlugin.getScheduler().runTaskLater(() -> passOut(player), 1);
        } else {
            addPuke(player, 60 + (int) (Math.random() * 60.0));
            lang.sendEntry(player, "Player_CantDrink");
        }
    }

    // push the player around if he moves
    public static void playerMove(PlayerMoveEvent event) {
        BPlayer bPlayer = get(event.getPlayer());
        if (bPlayer != null) {
            bPlayer.move(event);
        }
    }

    // Eat something to drain the drunkenness
    public void drainByItem(Player player, Material mat) {
        int strength = BUtil.getMaterialMap(config.getDrainItems()).get(mat);
        if (drain(player, strength)) {
            remove(player);
        }
    }

    // drain the drunkenness by amount, returns true when player has to be removed
    public boolean drain(@Nullable Player player, int amount) {
        if (drunkenness > 0) {
            quality -= getQuality() * amount;
        }
        drunkenness -= amount;
        if (drunkenness > 0) {
            if (offlineDrunk == 0) {
                if (player == null) {
                    offlineDrunk = drunkenness;
                }
            }
        } else {
            if (offlineDrunk == 0) {
                return true;
            }
            if (drunkenness == 0) {
                drunkenness--;
            }
            quality = getQuality();
            if (drunkenness <= -offlineDrunk) {
                return drunkenness <= -config.getHangoverDays();
            }
        }
        return false;
    }

    // player is drunk
    public void move(PlayerMoveEvent event) {
        // has player more alc than 10
        if (drunkenness >= 10 && config.getStumblePercent() > 0.001f) {
            if (drunkenness <= 100) {
                if (time > 1) {
                    time--;
                } else {
                    // Is he moving
                    if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {
                        Player player = event.getPlayer();
                        // We have to cast here because it had issues otherwise on previous versions of Minecraft
                        // Don't know if that's still the case, but we better leave it
                        // not in midair
                        if (((Entity) player).isOnGround()) {
                            time--;
                            if (time == 0) {
                                // push him only to the side? or any direction
                                // like now
                                if (VERSION.isOrLater(MinecraftVersion.V1_9)) { // Pushing is way stronger in 1.9
                                    push.setX((Math.random() - 0.5) / 2.0);
                                    push.setZ((Math.random() - 0.5) / 2.0);
                                } else {
                                    push.setX(Math.random() - 0.5);
                                    push.setZ(Math.random() - 0.5);
                                }
                                push.multiply(config.getStumblePercent());
                                PlayerPushEvent pushEvent = new PlayerPushEvent(player, push, this);
                                BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(pushEvent);
                                push = pushEvent.getPush();
                                if (pushEvent.isCancelled() || push.lengthSquared() <= 0) {
                                    time = -10;
                                    return;
                                }
                                player.setVelocity(push);
                            } else if (time < 0 && time > -10) {
                                // push him some more in the same direction
                                player.setVelocity(push);
                            } else {
                                // when more alc, push him more often
                                time = (int) (Math.random() * (201.0 - (drunkenness * 2)));
                            }
                        }
                    }
                }
            }
        }
    }

    public void passOut(Player player) {
        player.kickPlayer(lang.getEntry("Player_DrunkPassOut"));
        offlineDrunk = drunkenness;
    }


    // #### Login ####

    // can the player login or is he too drunk
    public int canJoin() {
        if (drunkenness <= 70) {
            return 0;
        }
        if (!config.isEnableLoginDisallow()) {
            if (drunkenness <= 100) {
                return 0;
            } else {
                return 3;
            }
        }
        if (drunkenness <= 90) {
            if (Math.random() > 0.4) {
                return 0;
            } else {
                return 2;
            }
        }
        if (drunkenness <= 100) {
            if (Math.random() > 0.6) {
                return 0;
            } else {
                return 2;
            }
        }
        return 3;
    }

    // he may be having a hangover
    public void join(final Player player) {
        // TODO: Rewrite part of this class to not use offlinedrunk, a bunch of this overhead boilerplate is completely unnecessary and overcomplicates our code
        // Modified this method a bit to just patch wakeups not working but this *REALLY* needs a rewrite

        if (drunkenness < 10) {
            if (offlineDrunk > 60) {
                if (config.isEnableHome() && !player.hasPermission("brewery.bypass.teleport")) {
                    goHome(player);
                }
            }
            if (offlineDrunk > 20) {
                hangoverEffects(player);
                showDrunkeness(player);
            }
            if (drunkenness <= 0) {
                remove(player);
            }

        } else if (offlineDrunk >= 30 || drunkenness >= 30) {
            if (config.isEnableWake() && !player.hasPermission("brewery.bypass.teleport")) {
                Location randomLoc = Wakeup.getRandom(player.getLocation());
                if (randomLoc != null) {
                    PaperLib.teleportAsync(player, randomLoc);
                    lang.sendEntry(player, "Player_Wake");
                }
            }
        }

        offlineDrunk = 0;
    }

    public void disconnecting() {
        offlineDrunk = drunkenness;
    }

    public void goHome(final Player player) {
        String homeType = config.getHomeType();
        if (homeType == null) {
            return;
        }
        if (homeType.equalsIgnoreCase("bed")) {
            PaperLib.getBedSpawnLocationAsync(player, true).thenAcceptAsync(it -> {
                if (it != null) {
                    PaperLib.teleportAsync(player, it);
                }
            });
        } else if (homeType.startsWith("cmd: ")) {
            player.performCommand(homeType.substring(5));
        } else if (homeType.startsWith("cmd:")) {
            player.performCommand(homeType.substring(4));
        } else {
            Logging.errorLog("Config.yml 'homeType: " + homeType + "' unknown!");
        }
    }

    public void recalculateAlcRecovery(@Nullable Player player) {
        setAlcRecovery(2);
        if (player != null) {
            int rec = PermissionUtil.getAlcRecovery(player);
            if (rec > -1) {
                setAlcRecovery(rec);
            }
        }
    }


    // #### Puking ####

    // Chance that players puke on big drunkenness
    // runs every 6 sec, average chance is 15%, so should puke about every 40 sec
    // good quality can decrease the chance by up to 15%
    public void drunkPuke(Player player) {
        if (drunkenness >= 90) {
            // chance between 20% and 10%
            if (Math.random() < 0.20f - (getQuality() / 100f)) {
                addPuke(player, 20 + (int) (Math.random() * 40));
            }
        } else if (drunkenness >= 80) {
            // chance between 15% and 0%
            if (Math.random() < 0.15f - (getQuality() / 66f)) {
                addPuke(player, 10 + (int) (Math.random() * 30));
            }
        } else if (drunkenness >= 70) {
            // chance between 10% at 1 quality and 0% at 6 quality
            if (Math.random() < 0.10f - (getQuality() / 60f)) {
                addPuke(player, 10 + (int) (Math.random() * 20));
            }
        }
    }

    // make a Player puke "count" items
    public static void addPuke(Player player, int count) {
        if (!config.isEnablePuke()) {
            return;
        }

        PlayerPukeEvent event = new PlayerPukeEvent(player, count);
        BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getCount() < 1) {
            return;
        }
        BUtil.reapplyPotionEffect(player, PotionEffectType.HUNGER.createEffect(80, 4), true);

        if (pTasks.isEmpty()) {
            task = BreweryPlugin.getScheduler().runTaskTimer(player, BPlayer::pukeTask, 1L, 1L);
        }
        pTasks.put(player, event.getCount());
    }

    public static void pukeTask() {
        for (Iterator<Map.Entry<Player, Integer>> iter = pTasks.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry<Player, Integer> entry = iter.next();
            Player player = entry.getKey();
            int count = entry.getValue();
            if (!player.isValid() || !player.isOnline()) {
                iter.remove();
                continue;
            }
            puke(player);
            if (count <= 1) {
                iter.remove();
            } else {
                entry.setValue(count - 1);
            }
        }
        if (pTasks.isEmpty()) {
            task.cancel();
        }
    }

    public static void puke(Player player) {
        if (pukeRand == null) {
            pukeRand = new Random();
        }
        if (config.getPukeItem() == null || config.getPukeItem().isEmpty()) {
            config.setPukeItem(List.of(Material.SOUL_SAND));
        }
        Location loc = player.getLocation();
        loc.setY(loc.getY() + 1.1);
        loc.setPitch(loc.getPitch() - 10 + pukeRand.nextInt(20));
        loc.setYaw(loc.getYaw() - 10 + pukeRand.nextInt(20));
        Vector direction = loc.getDirection();
        direction.multiply(0.5);
        loc.add(direction);

        Item item = player.getWorld().dropItem(loc, new ItemStack(config.getPukeItem().get(new Random().nextInt(config.getPukeItem().size()))));
        item.setVelocity(direction);
        item.setPickupDelay(32767); // Item can never be picked up when pickup delay is 32767
        item.setMetadata("brewery_puke", new FixedMetadataValue(BreweryPlugin.getInstance(), true));
        if (VERSION.isOrLater(MinecraftVersion.V1_14)) item.setPersistent(false); // No need to save Puke items

        int pukeDespawntime = config.getPukeDespawntime();
        int despawnRate = BUtil.getItemDespawnRate(player.getWorld());
        if (pukeDespawntime >= (despawnRate - 200)) {
            return;
        }

        // Setting the age determines when an item is despawned. At age 6000 it is removed.
        if (pukeDespawntime <= 0) {
            // Just show the item for a few ticks
            item.setTicksLived(despawnRate - 4);
        } else if (pukeDespawntime <= 120) {
            // it should despawn in less than 6 sec. Add up to half of that randomly
            item.setTicksLived(despawnRate - pukeDespawntime + pukeRand.nextInt((int) (pukeDespawntime / 2F)));
        } else {
            // Add up to 5 sec randomly
            item.setTicksLived(despawnRate - pukeDespawntime + pukeRand.nextInt(100));
        }
    }


    // #### Effects ####

    public static void applyEffects(List<PotionEffect> effects, Player player, PlayerEffectEvent.EffectType effectType) {
        PlayerEffectEvent event = new PlayerEffectEvent(player, effectType, effects);
        BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(event);
        effects = event.getEffects();
        if (event.isCancelled() || effects == null) {
            return;
        }
        for (PotionEffect effect : effects) {
            BUtil.reapplyPotionEffect(player, effect, true);
        }
    }

    public void drunkEffects(Player player) {
        int duration = 10 - getQuality();
        duration += drunkenness / 2;
        duration *= 5;
        if (duration > 240) {
            duration *= 5;
        } else if (duration < 115) {
            duration = 115;
        }
        if (VERSION.isOrEarlier(MinecraftVersion.V1_14)) {
            duration *= 4;
        }
        List<PotionEffect> l = new ArrayList<>(1);
        l.add(PotionEffectType.CONFUSION.createEffect(duration, 0));

        PlayerEffectEvent event = new PlayerEffectEvent(player, PlayerEffectEvent.EffectType.ALCOHOL, l);
        BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(event);
        l = event.getEffects();
        if (event.isCancelled() || l == null) {
            return;
        }
        for (PotionEffect effect : l) {
            BreweryPlugin.getScheduler().runTask(player, () -> effect.apply(player)); // Fix can't add effect to entities Async
        }
    }

    public static List<PotionEffect> getQualityEffects(int quality, int brewAlc) {
        List<PotionEffect> out = new ArrayList<>(2);
        int duration = 7 - quality;
        if (quality == 0) {
            duration *= 125;
        } else if (quality <= 5) {
            duration *= 62;
        } else {
            duration = 25;
            if (brewAlc <= 10) {
                duration = 0;
            }
        }
        if (VERSION.isOrEarlier(MinecraftVersion.V1_14)) {
            duration *= 4;
        }
        if (duration > 0) {
            out.add(PotionEffectType.POISON.createEffect(duration, 0));
        }

        if (brewAlc > 10) {
            if (quality <= 5) {
                duration = 10 - quality;
                duration += brewAlc;
                duration *= 15;
            } else {
                duration = 30;
            }
            if (VERSION.isOrEarlier(MinecraftVersion.V1_14)) {
                duration *= 4;
            }
            out.add(PotionEffectType.BLINDNESS.createEffect(duration, 0));
        }
        return out;
    }

    public static void addQualityEffects(int quality, int brewAlc, Player player) {
        List<PotionEffect> list = getQualityEffects(quality, brewAlc);
        PlayerEffectEvent event = new PlayerEffectEvent(player, PlayerEffectEvent.EffectType.QUALITY, list);
        BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(event);
        list = event.getEffects();
        if (event.isCancelled() || list == null) {
            return;
        }
        for (PotionEffect effect : list) {
            BUtil.reapplyPotionEffect(player, effect, true);
        }
    }

    public static List<PotionEffect> getBrewEffects(List<BEffect> effects, int quality) {
        List<PotionEffect> out = new ArrayList<>();
        if (effects != null) {
            for (BEffect effect : effects) {
                PotionEffect e = effect.generateEffect(quality);
                if (e != null) {
                    out.add(e);
                }
            }
        }
        return out;
    }

    public static void addBrewEffects(Brew brew, Player player) {
        List<BEffect> effects = brew.getEffects();
        if (effects != null) {
            for (BEffect effect : effects) {
                effect.apply(brew.getQuality(), player);
            }
        }
    }

    public void hangoverEffects(final Player player) {
        int duration = offlineDrunk * 25 * getHangoverQuality();
        if (VERSION.isOrEarlier(MinecraftVersion.V1_14)) {
            duration *= 2;
        }
        int amplifier = getHangoverQuality() / 3;

        List<PotionEffect> list = new ArrayList<>(2);
        list.add(PotionEffectType.SLOW.createEffect(duration, amplifier));
        list.add(PotionEffectType.HUNGER.createEffect(duration, amplifier));

        PlayerEffectEvent event = new PlayerEffectEvent(player, PlayerEffectEvent.EffectType.HANGOVER, list);
        BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(event);
        list = event.getEffects();
        if (event.isCancelled() || list == null) {
            return;
        }
        for (PotionEffect effect : list) {
            BUtil.reapplyPotionEffect(player, effect, true);
        }
    }


    // #### Scheduled ####

    public static void drunkenness() {
        for (Map.Entry<String, BPlayer> entry : players.entrySet()) {
            String name = entry.getKey();
            BPlayer bplayer = entry.getValue();

            if (bplayer.drunkenness > 30) {
                if (bplayer.offlineDrunk == 0) {
                    Player player = BUtil.getPlayerfromString(name);
                    if (player != null) {

                        bplayer.drunkEffects(player);

                        if (config.isEnablePuke()) {
                            bplayer.drunkPuke(player);
                        }

                    }
                }
            }
        }
    }

    // decreasing drunkenness over time
    public static void onUpdate() {
        if (!players.isEmpty()) {
            Iterator<Map.Entry<String, BPlayer>> iter = players.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, BPlayer> entry = iter.next();
                String uuid = entry.getKey();
                BPlayer bplayer = entry.getValue();
                Player playerIfOnline = BUtil.getPlayerfromString(uuid);

                if (bplayer.getAlcRecovery() == -1) {
                    bplayer.recalculateAlcRecovery(playerIfOnline);
                }

                if (bplayer.drain(playerIfOnline, bplayer.getAlcRecovery())) {
                    iter.remove();
                }
            }
        }
    }

    // save all data
    public static void save(ConfigurationSection config) {
        for (Map.Entry<String, BPlayer> entry : players.entrySet()) {
            ConfigurationSection section = config.createSection(entry.getKey());
            BPlayer bPlayer = entry.getValue();
            section.set("quality", bPlayer.quality);
            section.set("drunk", bPlayer.drunkenness);
            if (bPlayer.offlineDrunk != 0) {
                section.set("offDrunk", bPlayer.offlineDrunk);
            }
        }
    }


    // #### getter/setter ####


    public String getUuid() {
        return uuid;
    }

    public int getDrunkeness() {
        return drunkenness;
    }

    public void setDrunkeness(int value) {
        drunkenness = value;
    }

    public void setData(int drunkenness, int quality) {
        if (quality > 0) {
            this.quality = quality * drunkenness;
        } else {
            if (this.quality == 0) {
                this.quality = 5 * drunkenness;
            } else {
                this.quality = getQuality() * drunkenness;
            }
        }
        this.drunkenness = drunkenness;
    }

    public int getQuality() {
        if (drunkenness == 0) {
            // PAPI Placeholder %breweryx_quality% may be used on players that aren't drunk!
            // Logging.errorLog("drunkenness should not be 0!");
            return quality;
        }
        if (drunkenness < 0) {
            return quality;
        }
        return Math.round((float) quality / (float) drunkenness);
    }

    public int getQualityData() {
        return quality;
    }

    public void setQuality(int value) {
        quality = value;
    }

    // opposite of quality
    public int getHangoverQuality() {
        if (drunkenness < 0) {
            return quality + 11;
        }
        return -getQuality() + 11;
    }

    /**
     * Drunkeness at the time he went offline
     */
    public int getOfflineDrunkeness() {
        return offlineDrunk;
    }

    public int getAlcRecovery() {
        return alcRecovery;
    }

    public void setAlcRecovery(int alcRecovery) {
        this.alcRecovery = alcRecovery;
    }


    public String getName() {
        Player player = BUtil.getPlayerfromString(uuid);
        OfflinePlayer offlinePlayer;

        if (player != null) {
            return player.getName();
        } else {
            offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        }
        return offlinePlayer.getName();
    }
}
