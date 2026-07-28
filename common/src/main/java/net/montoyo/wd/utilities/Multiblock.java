/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.utilities;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.core.BlockPos;
import net.montoyo.wd.registry.BlockRegistry;
import net.montoyo.wd.utilities.math.Vector2i;
import net.montoyo.wd.utilities.math.Vector3i;
import net.montoyo.wd.utilities.data.BlockSide;

public abstract class Multiblock {
    public enum OverrideAction {
        NONE,
        SIMULATE,
        IGNORE
    }

    public static class BlockOverride {
        final Vector3i pos;
        final OverrideAction action;

        public BlockOverride(Vector3i p, OverrideAction act) {
            pos = p;
            action = act;
        }

        public boolean apply(Vector3i bp, boolean originalResult) {
            if (action == OverrideAction.NONE || !bp.equals(pos))
                return originalResult;
            else if (action == OverrideAction.SIMULATE)
                return true;
            else //action == OverrideAction.IGNORE
                return false;
        }

    }

    public static final BlockOverride NULL_OVERRIDE = new BlockOverride(null, OverrideAction.NONE);

    // Multi-position override for multi-layer screen support
    public static class MultiBlockOverride extends BlockOverride {
        final Vector3i[] positions;
        final OverrideAction[] actions;

        public MultiBlockOverride(Vector3i[] positions, OverrideAction[] actions) {
            super(null, OverrideAction.NONE);
            this.positions = positions;
            this.actions = actions;
        }

        @Override
        public boolean apply(Vector3i bp, boolean originalResult) {
            for (int i = 0; i < positions.length; i++) {
                if (bp.equals(positions[i])) {
                    if (actions[i] == OverrideAction.SIMULATE)
                        return true;
                    else if (actions[i] == OverrideAction.IGNORE)
                        return false;
                }
            }
            return originalResult;
        }
    }

    //Modifies pos
    public static void findOrigin(LevelAccessor world, Vector3i pos, BlockSide side, BlockOverride override) {
        if (override == null)
            override = NULL_OVERRIDE;

        BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();

        //Go left
        do {
            pos.add(side.left);
            pos.toBlock(bp);
        } while (override.apply(pos, world.getBlockState(bp).getBlock() == BlockRegistry.SCREEN_BLOCK));

        pos.add(side.right);

        //Go down
        do {
            pos.add(side.down);
            pos.toBlock(bp);
        } while (override.apply(pos, world.getBlockState(bp).getBlock() == BlockRegistry.SCREEN_BLOCK));

        pos.add(side.up);
    }

    //Origin stays constant
    public static Vector2i measure(LevelAccessor world, Vector3i origin, BlockSide side) {
        Vector2i ret = new Vector2i();
        Vector3i pos = origin.clone();

        BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();
        pos.toBlock(bp);

        //Go up
        do {
            pos.add(side.up);
            pos.toBlock(bp);
            ret.y++;
        } while (world.getBlockState(bp).getBlock() == BlockRegistry.SCREEN_BLOCK);

        pos.add(side.down);

        //Go right
        do {
            pos.add(side.right);
            pos.toBlock(bp);
            ret.x++;
        } while (world.getBlockState(bp).getBlock() == BlockRegistry.SCREEN_BLOCK);

        return ret;
    }

    //Origin and size stays constant.
    //Returns null if structure is okay, otherwise the erroring block pos.
    public static Vector3i check(LevelAccessor world, Vector3i origin, Vector2i size, BlockSide side) {
        return check(world, origin, size, side, NULL_OVERRIDE);
    }

    //Origin and size stays constant - with BlockOverride support.
    //Returns null if structure is okay, otherwise the erroring block pos.
    public static Vector3i check(LevelAccessor world, Vector3i origin, Vector2i size, BlockSide side, BlockOverride override) {
        if (override == null)
            override = NULL_OVERRIDE;
            
        Vector3i pos = origin.clone();
        BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();

        //Check inner
        for (int y = 0; y < size.y; y++) {
            for (int x = 0; x < size.x; x++) {
                pos.toBlock(bp);
                if (!override.apply(pos, world.getBlockState(bp).getBlock() == BlockRegistry.SCREEN_BLOCK))
                    return pos; //Hole

                pos.add(side.forward);
                pos.toBlock(bp);
                if (override.apply(pos, world.getBlockState(bp).getBlock() == BlockRegistry.SCREEN_BLOCK))
                    return pos; //Back should be empty

                pos.addMul(side.backward, 2);
                pos.toBlock(bp);
                if (override.apply(pos, world.getBlockState(bp).getBlock() == BlockRegistry.SCREEN_BLOCK))
                    return pos; //Front should be empty

                pos.add(side.forward);
                pos.add(side.right);
            }

            pos.addMul(side.left, size.x);
            pos.add(side.up);
        }

        //Check left edge
        pos.set(origin);
        pos.add(side.left);

        for (int y = 0; y < size.y; y++) {
            pos.toBlock(bp);
            if (override.apply(pos, world.getBlockState(bp).getBlock() == BlockRegistry.SCREEN_BLOCK))
                return pos; //Left edge should be empty

            pos.add(side.up);
        }

        //Check right edge
        pos.set(origin);
        pos.addMul(side.right, size.x);

        for (int y = 0; y < size.y; y++) {
            pos.toBlock(bp);
            if (override.apply(pos, world.getBlockState(bp).getBlock() == BlockRegistry.SCREEN_BLOCK))
                return pos; //Right edge should be empty

            pos.add(side.up);
        }

        //Check bottom edge
        pos.set(origin);
        pos.add(side.down);

        for (int x = 0; x < size.x; x++) {
            pos.toBlock(bp);
            if (override.apply(pos, world.getBlockState(bp).getBlock() == BlockRegistry.SCREEN_BLOCK))
                return pos; //Bottom edge should be empty

            pos.add(side.right);
        }

        //Check top edge
        pos.set(origin);
        pos.addMul(side.up, size.y);

        for (int x = 0; x < size.x; x++) {
            pos.toBlock(bp);
            if (override.apply(pos, world.getBlockState(bp).getBlock() == BlockRegistry.SCREEN_BLOCK))
                return pos; //Top edge should be empty

            pos.add(side.right);
        }

        //All good.
        return null;
    }
}
