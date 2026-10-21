/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.math.Vector3i;

/**
 * Interface for peripheral blocks that can be linked to screens.
 */
public interface IPeripheral {
    /**
     * Connect this peripheral to a screen.
     * @param level The world
     * @param pos The position of this peripheral
     * @param state The block state
     * @param screenPos The position of the screen to connect to
     * @param screenSide The side of the screen
     * @return true if connection was successful
     */
    boolean connect(Level level, BlockPos pos, BlockState state, Vector3i screenPos, BlockSide screenSide);
}
