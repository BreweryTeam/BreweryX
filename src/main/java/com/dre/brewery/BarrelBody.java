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
import com.dre.brewery.utility.MaterialUtil;
import com.dre.brewery.utility.MinecraftVersion;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * The Blocks that make up a Barrel in the World
 */
@Getter
@Setter
public abstract class BarrelBody {

    protected Block spigot;
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
    public static int getDirection(Block spigot) {
        int direction = 0;// 1=x+ 2=x- 3=z+ 4=z-
        Material type = spigot.getRelative(0, 0, 1).getType();
        if (BarrelAsset.isBarrelAsset(BarrelAsset.PLANKS, type) || BarrelAsset.isBarrelAsset(BarrelAsset.STAIRS, type)) {
            direction = 3;
        }
        type = spigot.getRelative(0, 0, -1).getType();
        if (BarrelAsset.isBarrelAsset(BarrelAsset.PLANKS, type) || BarrelAsset.isBarrelAsset(BarrelAsset.STAIRS, type)) {
            if (direction == 0) {
                direction = 4;
            } else {
                return 0;
            }
        }
        type = spigot.getRelative(1, 0, 0).getType();
        if (BarrelAsset.isBarrelAsset(BarrelAsset.PLANKS, type) || BarrelAsset.isBarrelAsset(BarrelAsset.STAIRS, type)) {
            if (direction == 0) {
                direction = 1;
            } else {
                return 0;
            }
        }
        type = spigot.getRelative(-1, 0, 0).getType();
        if (BarrelAsset.isBarrelAsset(BarrelAsset.PLANKS, type) || BarrelAsset.isBarrelAsset(BarrelAsset.STAIRS, type)) {
            if (direction == 0) {
                direction = 2;
            } else {
                return 0;
            }
        }
        return direction;
    }

    /**
     * woodtype of the block the spigot is attached to
     */
    public BarrelWoodType getWood() {
        Block wood;
        switch (getDirection(spigot)) { // 1=x+ 2=x- 3=z+ 4=z-
            case 0 -> {
                return BarrelWoodType.ANY;
            }

            case 1 -> wood = spigot.getRelative(1, 0, 0);
            case 2 -> wood = spigot.getRelative(-1, 0, 0);
            case 3 -> wood = spigot.getRelative(0, 0, 1);
            default -> wood = spigot.getRelative(0, 0, -1);
        }
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
        int direction = getDirection(spigot);// 1=x+ 2=x- 3=z+ 4=z-
        if (direction == 0) {
            return spigot;
        }
        int startX;
        int startZ;
        int endX;
        int endZ;

        if (direction == 1) {
            startX = 1;
            startZ = -1;
        } else if (direction == 2) {
            startX = -2;
            startZ = 0;
        } else if (direction == 3) {
            startX = 0;
            startZ = 1;
        } else {
            startX = -1;
            startZ = -2;
        }
        endX = startX + 1;
        endZ = startZ + 1;

        Material type;
        int x = startX;
        int y = 0;
        int z = startZ;
        while (y <= 1) {
            while (x <= endX) {
                while (z <= endZ) {
                    Block block = spigot.getRelative(x, y, z);
                    type = block.getType();

                    if (BarrelAsset.isBarrelAsset(BarrelAsset.STAIRS, type)) {
                        if (y == 0) {
                            // stairs have to be upside down
                            if (!MaterialUtil.areStairsInverted(block)) {
                                return block;
                            }
                        }
                        z++;
                    } else {
                        return spigot.getRelative(x, y, z);
                    }
                }
                z = startZ;
                x++;
            }
            z = startZ;
            x = startX;
            y++;
        }
        bounds.resize(
            spigot.getX() + startX,
            spigot.getY(),
            spigot.getZ() + startZ,
            spigot.getX() + endX,
            spigot.getY() + 1,
            spigot.getZ() + endZ
        );
        return null;
    }

    public Block checkLBarrel() {
        int direction = getDirection(spigot);// 1=x+ 2=x- 3=z+ 4=z-
        if (direction == 0) {
            return spigot;
        }
        int startX;
        int startZ;
        int endX;
        int endZ;

        if (direction == 1) {
            startX = 1;
            startZ = -1;
        } else if (direction == 2) {
            startX = -4;
            startZ = -1;
        } else if (direction == 3) {
            startX = -1;
            startZ = 1;
        } else {
            startX = -1;
            startZ = -4;
        }
        if (direction == 1 || direction == 2) {
            endX = startX + 3;
            endZ = startZ + 2;
        } else {
            endX = startX + 2;
            endZ = startZ + 3;
        }

        Material type;
        int x = startX;
        int y = 0;
        int z = startZ;
        while (y <= 2) {
            while (x <= endX) {
                while (z <= endZ) {
                    Block block = spigot.getRelative(x, y, z);
                    type = block.getType();
                    if (direction == 1 || direction == 2) {
                        if (y == 1 && z == 0) {
                            z++;
                            continue;
                        }
                    } else {
                        if (y == 1 && x == 0) {
                            z++;
                            continue;
                        }
                    }
                    if (BarrelAsset.isBarrelAsset(BarrelAsset.PLANKS, type) || BarrelAsset.isBarrelAsset(BarrelAsset.STAIRS, type)) {
                        z++;
                    } else {
                        return block;
                    }
                }
                z = startZ;
                x++;
            }
            z = startZ;
            x = startX;
            y++;
        }

        bounds.resize(
            spigot.getX() + startX,
            spigot.getY(),
            spigot.getZ() + startZ,
            spigot.getX() + endX,
            spigot.getY() + 2,
            spigot.getZ() + endZ
        );
        return null;
    }
}
