/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.data;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.client_bound.S2CMessageOpenGui;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.math.Vector3i;

public class KeyboardData {
    public Vector3i pos;
    public BlockSide side;
    public int kbX;
    public int kbY;
    public int kbZ;

    public KeyboardData() {
    }

    public KeyboardData(ScreenBlockEntity tes, BlockSide side, BlockPos kbPos) {
        pos = new Vector3i(tes.getBlockPos());
        this.side = side;
        kbX = kbPos.getX();
        kbY = kbPos.getY();
        kbZ = kbPos.getZ();
    }

    public String getName() {
        return "Keyboard";
    }

    public void sendTo(ServerPlayer player) {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeInt(pos.x);
        buf.writeInt(pos.y);
        buf.writeInt(pos.z);
        buf.writeByte(side.ordinal());
        buf.writeInt(kbX);
        buf.writeInt(kbY);
        buf.writeInt(kbZ);
        WDNetworkRegistry.sendToPlayer(player, new S2CMessageOpenGui("keyboard", buf));
    }
}
