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

import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.BoundingBox;
import com.dre.brewery.utility.MinecraftVersion;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The Blocks that make up a Barrel in the World
 */
@Getter
@Setter
public abstract class BarrelBody {

    protected final Block spigot;
    protected final BoundingBox bounds;
    protected byte signoffset;

    public BarrelBody(Block spigot, byte signoffset) {
        this.spigot = spigot;
        this.signoffset = signoffset;
        this.bounds = new BoundingBox(0, 0, 0, 0, 0, 0);

        if (MinecraftVersion.isFolia()) { // Issues#70
            BreweryPlugin.getScheduler().runTask(spigot.getLocation(), () -> {
                Block broken = getBrokenBlock(true);
                if (broken != null) {
                    this.remove(broken, null, true);
                }
            });
        }
    }

    /**
     * Loading from file
     */
    public BarrelBody(Block spigot, byte signoffset, BoundingBox bounds) {
        this.spigot = spigot;
        this.signoffset = signoffset;
        this.bounds = bounds;
        if (this.bounds == null || this.bounds.isBad()) {
            // If loading from old data, or block locations are missing, or other error, regenerate BoundingBox
            // This will only be done in those extreme cases.

            // Barrels can be loaded async!
            if (Bukkit.isPrimaryThread()) {
                this.regenerateBounds();
            } else {
                BreweryPlugin.getScheduler().runTask(spigot.getLocation(), this::regenerateBounds);
            }
        }
    }


    /**
     * If the Sign of a Large Barrel gets destroyed, set signOffset to 0
     */
    public void destroySign() {
        signoffset = 0;
    }


    /**
     * direction of the barrel from the spigot
     */
    public static BarrelFacing getDirection(Block spigot) {
        BarrelFacing direction = null;// 1=x+ 2=x- 3=z+ 4=z-
        Material type = spigot.getRelative(0, 0, 1).getType();
        if (BarrelAsset.isBarrelAsset(BarrelAsset.PLANKS, type) || BarrelAsset.isBarrelAsset(BarrelAsset.STAIRS, type)) {
            direction = BarrelFacing.SOUTH;
        }
        type = spigot.getRelative(0, 0, -1).getType();
        if (BarrelAsset.isBarrelAsset(BarrelAsset.PLANKS, type) || BarrelAsset.isBarrelAsset(BarrelAsset.STAIRS, type)) {
            if (direction == null) {
                direction = BarrelFacing.NORTH;
            } else {
                return null;
            }
        }
        type = spigot.getRelative(1, 0, 0).getType();
        if (BarrelAsset.isBarrelAsset(BarrelAsset.PLANKS, type) || BarrelAsset.isBarrelAsset(BarrelAsset.STAIRS, type)) {
            if (direction == null) {
                direction = BarrelFacing.EAST;
            } else {
                return null;
            }
        }
        type = spigot.getRelative(-1, 0, 0).getType();
        if (BarrelAsset.isBarrelAsset(BarrelAsset.PLANKS, type) || BarrelAsset.isBarrelAsset(BarrelAsset.STAIRS, type)) {
            if (direction == null) {
                direction = BarrelFacing.WEST;
            } else {
                return null;
            }
        }
        return direction;
    }

    /**
     * woodtype of the block the spigot is attached to
     */
    public BarrelWoodType getWood() {
        BarrelFacing direction = getDirection(spigot);
        if (direction == null) {
            return BarrelWoodType.ANY;
        }
        // TODO: replace this with Block#getRelative(BlockFace)
        Block wood = switch (direction) {
            case WEST -> spigot.getRelative(1, 0, 0);
            case EAST -> spigot.getRelative(-1, 0, 0);
            case SOUTH -> spigot.getRelative(0, 0, 1);
            case NORTH -> spigot.getRelative(0, 0, -1);
        };
        return BarrelWoodType.fromMaterial(wood.getType());
    }

    /**
     * Returns true if this Block is part of this Barrel
     *
     * @param block the block to check
     * @return true if the given block is part of this Barrel
     */
    public boolean hasBlock(Block block) {
        if (block != null) {
            if (spigot.equals(block)) {
                return true;
            }
            if (spigot.getWorld().equals(block.getWorld())) {
                return bounds != null && bounds.contains(block.getX(), block.getY(), block.getZ());
            }
        }
        return false;
    }

    /**
     * Returns true if the Offset of the clicked Sign matches the Barrel.
     * <p>This prevents adding another sign to the barrel and clicking that.
     */
    public boolean isSignOfBarrel(byte offset) {
        return offset == 0 || signoffset == 0 || signoffset == offset;
    }

    /**
     * returns the Sign of a large barrel, the spigot if there is none
     */
    public Block getSignOfSpigot() {
        if (signoffset != 0) {
            if (BarrelAsset.isBarrelAsset(BarrelAsset.SIGN, spigot.getType())) {
                return spigot;
            }

            Block relative = spigot.getRelative(0, signoffset, 0);
            if (BarrelAsset.isBarrelAsset(BarrelAsset.SIGN, relative.getType())) {
                return relative;
            } else {
                signoffset = 0;
            }
        }
        return spigot;
    }

    /**
     * returns the fence above/below a block, itself if there is none
     */
    public static Block getSpigotOfSign(Block block) {

        int y = -2;
        while (y <= 1) {
            // Fence and Netherfence
            Block relative = block.getRelative(0, y, 0);
            if (BarrelAsset.isBarrelAsset(BarrelAsset.FENCE, relative.getType())) {
                return relative;
            }
            y++;
        }
        return block;
    }

    public abstract void remove(@Nullable Block broken, @Nullable Player breaker, boolean dropItems);

    /**
     * Regenerate the Barrel Bounds.
     *
     * @return true if successful, false if Barrel was broken and should be removed.
     */
    public abstract boolean regenerateBounds();

    /**
     * returns null if Barrel is correctly placed; the block that is missing when not.
     * <p>the barrel needs to be formed correctly
     *
     * @param force to also check even if chunk is not loaded
     */
    public Block getBrokenBlock(boolean force) {
        if (force || BUtil.isChunkLoaded(spigot)) {
            //spigot = getSpigotOfSign(spigot);
            if (BarrelAsset.isBarrelAsset(BarrelAsset.SIGN, spigot.getType())) {
                return checkSBarrel();
            } else {
                return checkLBarrel();
            }
        }
        return null;
    }

    public Block checkSBarrel() {
        BarrelFacing direction = getDirection(spigot);// 1=x+ 2=x- 3=z+ 4=z-
        if (direction == null) {
            return spigot;
        }
        BarrelFacing orthogonal = direction.rotate90degrees();
        int dx1 = direction.getDx();
        int dx2 = orthogonal.getDx();
        int dz1 = direction.getDz();
        int dz2 = orthogonal.getDz();
        Map<BlockVector, BarrelPart> untransformedBarrelPartMap = Map.of(
            new BlockVector(1, 0, 0), BarrelPart.BOTTOM_RIGHT,
            new BlockVector(1, 0, 1), BarrelPart.BOTTOM_LEFT,
            new BlockVector(1, 1, 0), BarrelPart.TOP_RIGHT,
            new BlockVector(1, 1, 1), BarrelPart.TOP_LEFT,
            new BlockVector(2, 0, 0), BarrelPart.BOTTOM_RIGHT,
            new BlockVector(2, 0, 1), BarrelPart.BOTTOM_LEFT,
            new BlockVector(2, 1, 0), BarrelPart.TOP_RIGHT,
            new BlockVector(2, 1, 1), BarrelPart.TOP_LEFT
        );
        Block brokenBlock = validateStructure(direction, dx1, dx2, dz1, dz2, untransformedBarrelPartMap);
        if (brokenBlock != null) {
            return brokenBlock;
        }

        BlockVector spigotPos = spigot.getLocation().toVector().toBlockVector();
        BlockVector minBarrel = (BlockVector) new BlockVector(dx1, 0, dz1).add(spigotPos);
        BlockVector maxBarrel = (BlockVector) new BlockVector(2 * dx1 + dx2, 1, 2 * dz1 + dz2).add(spigotPos);this.bounds.resize(minBarrel.getBlockX(), minBarrel.getBlockY(), minBarrel.getBlockZ(), maxBarrel.getBlockX(), maxBarrel.getBlockY(), maxBarrel.getBlockZ());
        return null;
    }

    public Block checkLBarrel() {
        BarrelFacing direction = getDirection(spigot);
        if (direction == null) {
            return spigot;
        }
        BarrelFacing orthogonal = direction.rotate90degrees();
        int dx1 = direction.getDx();
        int dx2 = orthogonal.getDx();
        int dz1 = direction.getDz();
        int dz2 = orthogonal.getDz();
        Map<BlockVector, BarrelPart> untransformedBarrelPartMap = new HashMap<>();
        untransformedBarrelPartMap.put(new BlockVector(1, 0, -1), BarrelPart.BOTTOM_RIGHT);
        untransformedBarrelPartMap.put(new BlockVector(1, 0, 1), BarrelPart.BOTTOM_LEFT);
        untransformedBarrelPartMap.put(new BlockVector(1, 0, 0), BarrelPart.BLOCK);
        untransformedBarrelPartMap.put(new BlockVector(1, 1, 1), BarrelPart.BLOCK);
        untransformedBarrelPartMap.put(new BlockVector(1, 1, -1), BarrelPart.BLOCK);
        untransformedBarrelPartMap.put(new BlockVector(1, 2, -1), BarrelPart.TOP_RIGHT);
        untransformedBarrelPartMap.put(new BlockVector(1, 2, 1), BarrelPart.TOP_LEFT);
        untransformedBarrelPartMap.put(new BlockVector(1, 2, 0), BarrelPart.BLOCK);
        untransformedBarrelPartMap.put(new BlockVector(1, 1, 0), BarrelPart.BLOCK);
        untransformedBarrelPartMap.put(new BlockVector(2, 0, -1), BarrelPart.BOTTOM_RIGHT);
        untransformedBarrelPartMap.put(new BlockVector(2, 0, 1), BarrelPart.BOTTOM_LEFT);
        untransformedBarrelPartMap.put(new BlockVector(2, 0, 0), BarrelPart.BLOCK);
        untransformedBarrelPartMap.put(new BlockVector(2, 1, 1), BarrelPart.BLOCK);
        untransformedBarrelPartMap.put(new BlockVector(2, 1, -1), BarrelPart.BLOCK);
        untransformedBarrelPartMap.put(new BlockVector(2, 2, -1), BarrelPart.TOP_RIGHT);
        untransformedBarrelPartMap.put(new BlockVector(2, 2, 1), BarrelPart.TOP_LEFT);
        untransformedBarrelPartMap.put(new BlockVector(2, 2, 0), BarrelPart.BLOCK);
        untransformedBarrelPartMap.put(new BlockVector(3, 0, -1), BarrelPart.BOTTOM_RIGHT);
        untransformedBarrelPartMap.put(new BlockVector(3, 0, 1), BarrelPart.BOTTOM_LEFT);
        untransformedBarrelPartMap.put(new BlockVector(3, 0, 0), BarrelPart.BLOCK);
        untransformedBarrelPartMap.put(new BlockVector(3, 1, 1), BarrelPart.BLOCK);
        untransformedBarrelPartMap.put(new BlockVector(3, 1, -1), BarrelPart.BLOCK);
        untransformedBarrelPartMap.put(new BlockVector(3, 2, -1), BarrelPart.TOP_RIGHT);
        untransformedBarrelPartMap.put(new BlockVector(3, 2, 1), BarrelPart.TOP_LEFT);
        untransformedBarrelPartMap.put(new BlockVector(3, 2, 0), BarrelPart.BLOCK);
        untransformedBarrelPartMap.put(new BlockVector(4, 0, -1), BarrelPart.BOTTOM_RIGHT);
        untransformedBarrelPartMap.put(new BlockVector(4, 0, 1), BarrelPart.BOTTOM_LEFT);
        untransformedBarrelPartMap.put(new BlockVector(4, 0, 0), BarrelPart.BLOCK);
        untransformedBarrelPartMap.put(new BlockVector(4, 1, 1), BarrelPart.BLOCK);
        untransformedBarrelPartMap.put(new BlockVector(4, 1, -1), BarrelPart.BLOCK);
        untransformedBarrelPartMap.put(new BlockVector(4, 2, -1), BarrelPart.TOP_RIGHT);
        untransformedBarrelPartMap.put(new BlockVector(4, 2, 1), BarrelPart.TOP_LEFT);
        untransformedBarrelPartMap.put(new BlockVector(4, 2, 0), BarrelPart.BLOCK);
        untransformedBarrelPartMap.put(new BlockVector(4, 1, 0), BarrelPart.BLOCK);
        Block brokenBlock = validateStructure(direction, dx1, dx2, dz1, dz2, untransformedBarrelPartMap);
        if (brokenBlock != null) {
            return brokenBlock;
        }
        BlockVector spigotPos = spigot.getLocation().toVector().toBlockVector();
        BlockVector minBarrel = (BlockVector) new BlockVector(dx1 - dx2, 0, dz1 - dz2).add(spigotPos);
        BlockVector maxBarrel = (BlockVector) new BlockVector(4 * dx1 + dx2, 2, 4 * dz1 + dz2).add(spigotPos);
        this.bounds.resize(minBarrel.getBlockX(), minBarrel.getBlockY(), minBarrel.getBlockZ(), maxBarrel.getBlockX(), maxBarrel.getBlockY(), maxBarrel.getBlockZ());
        return null;
    }

    @Nullable
    private Block validateStructure(BarrelFacing direction, int dx1, int dx2, int dz1, int dz2, Map<BlockVector, BarrelPart> untransformedBarrelPartMap) {
        BarrelWoodType woodType = getWood();
        for (Map.Entry<BlockVector, BarrelPart> entry : untransformedBarrelPartMap.entrySet()) {
            int relativeX = dx1 * entry.getKey().getBlockX() + dx2 * entry.getKey().getBlockZ();
            int relativeZ = dz1 * entry.getKey().getBlockX() + dz2 * entry.getKey().getBlockZ();
            int relativeY = entry.getKey().getBlockY();
            Block block = spigot.getRelative(relativeX, relativeY, relativeZ);
            BlockData blockData = block.getBlockData();
            if (!entry.getValue().matches(woodType, blockData, direction)) {
                return block;
            }
        }
        return null;
    }
}
