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

package com.dre.brewery.listeners;

import com.dre.brewery.BDistiller;
import com.dre.brewery.BSealer;
import com.dre.brewery.Barrel;
import com.dre.brewery.Brew;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.MCBarrel;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.lore.BrewLore;
import com.dre.brewery.utility.Logging;
import com.dre.brewery.utility.MinecraftVersion;
import io.papermc.lib.PaperLib;
import org.bukkit.Material;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public class InventoryListener implements Listener {

    private static final MinecraftVersion VERSION = BreweryPlugin.getMCVersion();

    private final Config config = ConfigManager.getConfig(Config.class);
    private static final Set<InventoryAction> CLICKED_INVENTORY_ITEM_MOVE = Set.of(InventoryAction.PLACE_SOME,
        InventoryAction.PLACE_ONE, InventoryAction.PLACE_ALL, InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_HALF,
        InventoryAction.PICKUP_SOME, InventoryAction.PICKUP_ONE);
    private static final Set<String> BANNED_ACTIONS = Set.of("PICKUP_ALL_INTO_BUNDLE", "PICKUP_FROM_BUNDLE",
        "PICKUP_SOME_INTO_BUNDLE", "PLACE_ALL_INTO_BUNDLE", "PLACE_SOME_INTO_BUNDLE");

    /* === Recreating manually the prior BrewEvent behavior. === */
    private HashSet<UUID> trackedBrewmen = new HashSet<>();

    /**
     * Start tracking distillation for a person when they open the brewer window.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBrewerOpen(InventoryOpenEvent event) {
        if (VERSION.isOrEarlier(MinecraftVersion.V1_9)) return;
        HumanEntity player = event.getPlayer();
        Inventory inv = event.getInventory();
        if (player == null || !(inv instanceof BrewerInventory)) return;

        Logging.debugLog("Starting brew inventory tracking");
        trackedBrewmen.add(player.getUniqueId());
    }

    /**
     * Stop tracking distillation for a person when they close the brewer window.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBrewerClose(InventoryCloseEvent event) {
        if (VERSION.isOrEarlier(MinecraftVersion.V1_9)) return;
        HumanEntity player = event.getPlayer();
        Inventory inv = event.getInventory();
        if (player == null || !(inv instanceof BrewerInventory)) return;

        Logging.debugLog("Stopping brew inventory tracking");
        trackedBrewmen.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBrewerDrag(InventoryDragEvent event) {
        if (VERSION.isOrEarlier(MinecraftVersion.V1_9)) return;
        // Workaround the Drag event when only clicking a slot
        if (event.getInventory() instanceof BrewerInventory) {
            onBrewerClick(new InventoryClickEvent(event.getView(), InventoryType.SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.PLACE_ALL));
        }
    }

    /**
     * Clicking can either start or stop the new brew distillation tracking.
     * <p>Note that server restart will halt any ongoing brewing processes and
     * they will _not_ restart until a new click event.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBrewerClick(InventoryClickEvent event) {
        if (VERSION.isOrEarlier(MinecraftVersion.V1_9)) return;

        HumanEntity player = event.getWhoClicked();
        Inventory inv = event.getInventory();
        if (!(inv instanceof BrewerInventory)) return;

        UUID puid = player.getUniqueId();
        if (!trackedBrewmen.contains(puid)) return;

        if (InventoryType.BREWING != inv.getType()) return;
        if (event.getAction() == InventoryAction.NOTHING) return; // Ignore clicks that do nothing

        BDistiller.distillerClick(event);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {
        if (VERSION.isOrLater(MinecraftVersion.V1_9)) {
            if (BDistiller.hasBrew(event.getContents(), BDistiller.getDistillContents(event.getContents())) != 0) {
                event.setCancelled(true);
            }
            return;
        }
        if (BDistiller.runDistill(event.getContents(), BDistiller.getDistillContents(event.getContents()))) {
            event.setCancelled(true);
        }
    }

    // Clicked a Brew somewhere, do some updating
    // TODO: Remove this? This was for legacy potion conversion - Jsinco
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onInventoryClickLow(InventoryClickEvent event) {
        if (event.getCurrentItem() != null && event.getCurrentItem().getType().equals(Material.POTION)) {
            ItemStack item = event.getCurrentItem();
            if (item.hasItemMeta()) {
                PotionMeta potion = ((PotionMeta) item.getItemMeta());
                assert potion != null;
                if (VERSION.isOrLater(MinecraftVersion.V1_11)) {
                    // Convert potions from 1.10 to 1.11 for new color
                    if (potion.getColor() == null) {
                        Brew brew = Brew.get(potion);
                        if (brew != null) {
                            brew.convertPre1_11(item);
                        }
                    }
                } else {
                    // convert potions from 1.8 to 1.9 for color and to remove effect descriptions
                    if (VERSION.isOrLater(MinecraftVersion.V1_9) && !potion.hasItemFlag(ItemFlag.HIDE_ATTRIBUTES)) {
                        Brew brew = Brew.get(potion);
                        if (brew != null) {
                            brew.convertPre1_9(item);
                        }
                    }
                }
				/*Brew brew = Brew.get(item);
				if (brew != null) {
					brew.touch();
				}*/
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = PaperLib.getHolder(event.getInventory(), true).getHolder();
        if (!(holder instanceof Barrel) && !(VERSION.isOrLater(MinecraftVersion.V1_14) && holder instanceof org.bukkit.block.Barrel)) {
            return;
        }
        InventoryAction action = event.getAction();
        if (action == InventoryAction.NOTHING) {
            return;
        }
        boolean upperInventoryIsClicked = event.getClickedInventory() == event.getInventory();
        if (!upperInventoryIsClicked && CLICKED_INVENTORY_ITEM_MOVE.contains(action)) {
            return;
        }
        ItemStack hoveredItem = event.getCurrentItem();
        Stream<ItemStack> relatedItems;
        if (upperInventoryIsClicked && hoveredItem != null) {
            if (hoveredItem.getItemMeta() instanceof PotionMeta potionMeta) {
                Brew brew = Brew.get(potionMeta);
                if (brew != null) {
                    BrewLore lore = new BrewLore(brew, potionMeta);
                    if (BrewLore.hasColorLore(potionMeta)) {
                        lore.convertLore(false);
                        lore.write();
                    } else if (!config.isAlwaysShowAlc() && event.getInventory().getType() == InventoryType.BREWING) {
                        lore.updateAlc(false);
                        lore.write();
                    }
                }
            }
        }
        if (!config.isOnlyAllowBrewsInBarrels()) {
            return;
        }
        if (BANNED_ACTIONS.contains(action.name())) {
            event.setResult(Event.Result.DENY);
            return;
        }
        InventoryView view = event.getView();
        // getHotbarButton also returns -1 for offhand clicks
        ItemStack hotbarItem = event.getHotbarButton() == -1 ?
            (event.getClick() == ClickType.SWAP_OFFHAND
                ? event.getWhoClicked().getInventory().getItemInOffHand()
                : null)
            : view.getBottomInventory().getItem(event.getHotbarButton());
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            // player takes something out
            if (upperInventoryIsClicked && hotbarItem == null) {
                return;
            }
            relatedItems = Stream.of(hotbarItem, hoveredItem);
        } else if (action == InventoryAction.HOTBAR_SWAP) {
            // barrel not involved
            if (!upperInventoryIsClicked) {
                return;
            }
            relatedItems = Stream.of(hotbarItem, hoveredItem);
        } else {
            ItemStack cursor = event.getCursor();
            relatedItems = Stream.of(cursor);
        }
        Stream<ItemStack> itemsToCheck = relatedItems
            .filter(Objects::nonNull)
            .filter(item -> !item.getType().isAir());
        if (itemsToCheck.anyMatch(item -> !(item.getItemMeta() instanceof PotionMeta potionMeta && Brew.get(potionMeta) != null))) {
            event.setResult(Event.Result.DENY);
        }
    }

    // Check if the player tries to add more than the allowed amount of brews into an mc-barrel
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClickMCBarrel(InventoryClickEvent event) {
        if (VERSION.isOrEarlier(MinecraftVersion.V1_14)) return;
        if (event.getInventory().getType() != InventoryType.BARREL) return;
        if (!config.isAgeInMCBarrels()) return;

        Inventory inv = event.getInventory();
        for (MCBarrel barrel : MCBarrel.openBarrels) {
            if (barrel.getInventory().equals(inv)) {
                barrel.clickInv(event);
                return;
            }
        }
        MCBarrel barrel = new MCBarrel(inv);
        MCBarrel.openBarrels.add(barrel);
        barrel.clickInv(event);
    }

    // Handle the Brew Sealer Inventory
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClickBSealer(InventoryClickEvent event) {
        if (VERSION.isOrEarlier(MinecraftVersion.V1_13)) return;
        InventoryHolder holder = PaperLib.getHolder(event.getInventory(), true).getHolder();
        if (!(holder instanceof BSealer)) {
            return;
        }
        ((BSealer) holder).clickInv();
    }

    //public static boolean opening = false;

    @SuppressWarnings("deprecation")
    @EventHandler(ignoreCancelled = false)
    public void onInventoryOpenLegacyConvert(InventoryOpenEvent event) {
        if (Brew.noLegacy()) {
            return;
        }
        if (event.getInventory().getType() == InventoryType.PLAYER) {
            return;
        }
        for (ItemStack item : event.getInventory().getContents()) {
            if (item != null && item.getType() == Material.POTION) {
                int uid = Brew.getUID(item);
                // Check if the uid exists first, otherwise it will log that it can't find the id
                if (uid < 0 && Brew.legacyPotions.containsKey(uid)) {
                    // This will convert the Brew
                    Brew.get(item);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (VERSION.isOrEarlier(MinecraftVersion.V1_14)) return;
        if (!config.isAgeInMCBarrels()) return;

        // Check for MC Barrel
        if (event.getInventory().getType() == InventoryType.BARREL) {
            Inventory inv = event.getInventory();
            for (MCBarrel barrel : MCBarrel.openBarrels) {
                if (barrel.getInventory().equals(inv)) {
                    barrel.open();
                    return;
                }
            }
            MCBarrel barrel = new MCBarrel(inv);
            MCBarrel.openBarrels.add(barrel);
            barrel.open();
        }
    }

    // block the pickup of items where getPickupDelay is > 1000 (puke)
    @EventHandler(ignoreCancelled = true)
    public void onHopperPickupPuke(InventoryPickupItemEvent event) {
        if (event.getItem().getPickupDelay() > 1000 && config.getPukeItem().contains(event.getItem().getItemStack().getType())) {
            event.setCancelled(true);
        }
    }

    // Block taking out items from running distillers,
    // Convert Color Lore from MC Barrels back into normal color on taking out
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onHopperMove(InventoryMoveItemEvent event) {
        if (event.getSource() instanceof BrewerInventory inv && PaperLib.getHolder(inv, true).getHolder() instanceof BrewingStand holder) {
            if (BDistiller.isTrackingDistiller(holder.getBlock())) {
                event.setCancelled(true);
            }
            return;
        }

        if (VERSION.isOrEarlier(MinecraftVersion.V1_14)) return;

        if (event.getSource().getType() == InventoryType.BARREL) {
            ItemStack item = event.getItem();
            if (item.getType() == Material.POTION && Brew.isBrew(item)) {
                PotionMeta meta = (PotionMeta) item.getItemMeta();
                assert meta != null;
                if (BrewLore.hasColorLore(meta)) {
                    // has color lore, convert lore back to normal
                    Brew brew = Brew.get(meta);
                    if (brew != null) {
                        BrewLore lore = new BrewLore(brew, meta);
                        lore.convertLore(false);
                        lore.write();
                        item.setItemMeta(meta);
                        event.setItem(item);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (VERSION.isOrEarlier(MinecraftVersion.V1_13)) return;
        if (PaperLib.getHolder(event.getInventory(), true).getHolder() instanceof BSealer holder) {
            holder.closeInv();
        }

        if (VERSION.isOrEarlier(MinecraftVersion.V1_14)) return;

        // Barrel Closing Sound
        if (PaperLib.getHolder(event.getInventory(), true).getHolder() instanceof Barrel barrel) {
            barrel.playClosingSound();
        }

        // Check for MC Barrel
        if (config.isAgeInMCBarrels() && event.getInventory().getType() == InventoryType.BARREL) {
            Inventory inv = event.getInventory();
            for (Iterator<MCBarrel> iter = MCBarrel.openBarrels.iterator(); iter.hasNext(); ) {
                MCBarrel barrel = iter.next();
                if (barrel.getInventory().equals(inv)) {
                    barrel.close();
                    if (inv.getViewers().size() == 1) {
                        // Last viewer, remove Barrel from List of open Barrels
                        iter.remove();
                    }
                    return;
                }
            }
            new MCBarrel(inv).close();
        }
    }
}
