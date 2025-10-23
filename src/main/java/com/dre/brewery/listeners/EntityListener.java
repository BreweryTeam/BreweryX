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

import com.dre.brewery.BCauldron;
import com.dre.brewery.Barrel;
import com.dre.brewery.Brew;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.api.events.barrel.BarrelDestroyEvent;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.MinecraftVersion;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class EntityListener implements Listener {

    // Legacy Brew removal
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        if (Brew.noLegacy()) return;
        ItemStack item = event.getEntity().getItemStack();
        if (item.getType() == Material.POTION) {
            Brew.removeLegacy(item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityCombust(EntityCombustEvent event) {
        if (Brew.noLegacy()) return;
        Entity entity = event.getEntity();
        if (entity instanceof Item itemEntity) {
            ItemStack item = itemEntity.getItemStack();
            if (item.getType() == Material.POTION) {
                Brew.removeLegacy(item);
            }
        }
    }

    //  --- Barrel Breaking ---

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent event) {
        if (causedByWindCharge(event)) return; // Fixes barrels being destroyed when hit by a WindCharge
        ListIterator<Block> iter = event.blockList().listIterator();
        if (!iter.hasNext()) return;
        List<BarrelDestroyEvent> breakEvents = new ArrayList<>(6);
        Block block;
        blocks:
        while (iter.hasNext()) {
            block = iter.next();
            BCauldron cauldron = BCauldron.get(block);
            if (cauldron != null) {
                BUtil.blockDestroy(block, null, BarrelDestroyEvent.Reason.EXPLODED);
                continue;
            }
            if (!breakEvents.isEmpty()) {
                for (BarrelDestroyEvent breakEvent : breakEvents) {
                    if (breakEvent.getBarrel().hasBlock(block)) {
                        if (breakEvent.isCancelled()) {
                            iter.remove();
                        }
                        continue blocks;
                    }
                }
            }
            Barrel barrel = Barrel.get(block);
            if (barrel != null) {
                BarrelDestroyEvent breakEvent = new BarrelDestroyEvent(barrel, block, BarrelDestroyEvent.Reason.EXPLODED, null);
                // Listened to by LWCBarrel (IntegrationListener)
                BreweryPlugin.getInstance().getServer().getPluginManager().callEvent(breakEvent);
                breakEvents.add(breakEvent);
                if (breakEvent.isCancelled()) {
                    iter.remove();
                } else {
                    barrel.remove(block, null, true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockChange(EntityChangeBlockEvent event) {
        if (event.getBlock().getType().name().toUpperCase().contains("CUT_COPPER")) return;
        if (Barrel.get(event.getBlock()) == null) return;
        event.setCancelled(true);
    }

    /**
     * Utility method to determine if the given event was caused by a wind charge
     *
     * @param event An instance of EntityExplodeEvent that should be analyzed
     * @return A boolean representing if the given event was caused by a wind charge
     */
    private boolean causedByWindCharge(EntityExplodeEvent event) {

        // Wind charges don't exist in versions below 1.21
        if (!BreweryPlugin.getMCVersion().isOrLater(MinecraftVersion.V1_21)) return false;

        EntityType type = event.getEntityType();
        return type == EntityType.valueOf("BREEZE_WIND_CHARGE") || type == EntityType.valueOf("WIND_CHARGE");
        // Enum can't currently be used directly, because BX still uses Spigots 1.20.2 API

        /*
         * Note that, since WindCharges have the ability to modify BlockStates (e.g. flip trapdoors they hit), we sadly
         * can't just check for event.blockList(), as they provide one too and checking if those blocks are destroyed
         * could only be done after the event has been successfully executed. Life isn't that easy at times :/
         */

    }

}
