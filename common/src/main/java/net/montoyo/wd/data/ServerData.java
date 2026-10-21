/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.data;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.client_bound.S2CMessageOpenGui;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;

public class ServerData {
    private final BlockPos pos;
    private final NameUUIDPair owner;

    public ServerData(BlockPos pos, NameUUIDPair owner) {
        this.pos = pos;
        this.owner = owner;
    }

    public void sendTo(ServerPlayer player) {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeBlockPos(pos);
        buf.writeUtf(owner.name);
        buf.writeUUID(owner.uuid);
        WDNetworkRegistry.sendToPlayer(player, new S2CMessageOpenGui("server", buf));
    }
}
