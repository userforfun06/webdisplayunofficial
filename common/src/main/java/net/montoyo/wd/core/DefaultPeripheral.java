/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.montoyo.wd.entity.RedstoneControlBlockEntity;
import net.montoyo.wd.entity.RemoteControlBlockEntity;
import net.montoyo.wd.entity.ServerBlockEntity;

public enum DefaultPeripheral {
    REDSTONE_CONTROLLER(RedstoneControlBlockEntity::new),
    REMOTE_CONTROLLER(RemoteControlBlockEntity::new),
    SERVER(ServerBlockEntity::new);

    private final BlockEntityFactory factory;

    DefaultPeripheral(BlockEntityFactory factory) {
        this.factory = factory;
    }

    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return factory.create(pos, state);
    }

    @FunctionalInterface
    public interface BlockEntityFactory {
        BlockEntity create(BlockPos pos, BlockState state);
    }
}
