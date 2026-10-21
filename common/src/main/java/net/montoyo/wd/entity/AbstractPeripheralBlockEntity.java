/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.HolderLookup;
import net.montoyo.wd.core.IPeripheral;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.math.Vector3i;

public abstract class AbstractPeripheralBlockEntity extends BlockEntity implements IPeripheral {
    protected Vector3i screenPos;
    protected BlockSide screenSide;
    
    public AbstractPeripheralBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("ScreenX")) {
            screenPos = new Vector3i(
                tag.getInt("ScreenX"),
                tag.getInt("ScreenY"),
                tag.getInt("ScreenZ")
            );
        }
        if (tag.contains("ScreenSide")) {
            screenSide = BlockSide.values()[tag.getInt("ScreenSide")];
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (screenPos != null) {
            tag.putInt("ScreenX", screenPos.x);
            tag.putInt("ScreenY", screenPos.y);
            tag.putInt("ScreenZ", screenPos.z);
        }
        if (screenSide != null) {
            tag.putInt("ScreenSide", screenSide.ordinal());
        }
    }
    
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean isLinked() {
        return screenPos != null && screenSide != null;
    }

    // Server-side: verify the linked screen still exists on the server
    // (client trusts the cached data and relies on server sync)
    public boolean isActuallyLinked() {
        if (!isLinked() || level == null || level.isClientSide)
            return isLinked();
        return getConnectedScreenEx() != null;
    }
    
    public boolean isScreenChunkLoaded() {
        if (!isLinked() || level == null)
            return false;
        BlockPos bp = new BlockPos(screenPos.x, screenPos.y, screenPos.z);
        return level.hasChunk(bp.getX() >> 4, bp.getZ() >> 4);
    }
    
    public Vector3i getScreenPos() {
        return screenPos;
    }
    
    public void setScreenPos(Vector3i pos) {
        this.screenPos = pos;
        setChanged();
    }
    
    public BlockSide getScreenSide() {
        return screenSide;
    }
    
    public void setScreenSide(BlockSide side) {
        this.screenSide = side;
        setChanged();
    }
    
    public ScreenBlockEntity getConnectedScreenEx() {
        if (!isLinked() || level == null) {
            return null;
        }
        
        BlockEntity be = level.getBlockEntity(new BlockPos(screenPos.x, screenPos.y, screenPos.z));
        if (be instanceof ScreenBlockEntity) {
            return (ScreenBlockEntity) be;
        }
        return null;
    }
    
    public void type(BlockSide side, String text, BlockPos pos) {
        ScreenBlockEntity screen = getConnectedScreenEx();
        if (screen != null) {
            screen.type(side, text, pos);
        }
    }
    
    public void setResolution(BlockSide side, net.montoyo.wd.utilities.math.Vector2i res) {
        ScreenBlockEntity screen = getConnectedScreenEx();
        if (screen != null) {
            screen.setResolution(side, res);
        }
    }
    
    public void setRotation(BlockSide side, net.montoyo.wd.utilities.data.Rotation rot) {
        ScreenBlockEntity screen = getConnectedScreenEx();
        if (screen != null) {
            screen.setRotation(side, rot);
        }
    }
    
    public void setScreenURL(BlockSide side, String url) throws java.io.IOException {
        ScreenBlockEntity screen = getConnectedScreenEx();
        if (screen != null) {
            screen.setScreenURL(side, url);
        }
    }

    public abstract InteractionResult onRightClick(Player player, InteractionHand hand);

    public abstract void onNeighborChange(Block neighborType, BlockPos neighborPos);

    @Override
    public boolean connect(Level world, BlockPos blockPos, BlockState blockState, Vector3i screenPos, BlockSide screenSide) {
        this.screenPos = screenPos;
        this.screenSide = screenSide;
        setChanged();
        return true;
    }
}
