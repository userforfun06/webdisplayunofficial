/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.data;

import net.minecraft.network.FriendlyByteBuf;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.client_bound.S2CMessageOpenGui;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;
import net.montoyo.wd.utilities.math.Vector3i;

/**
 * Data compound for Screen Configurator GUI
 * Based on 1.20 implementation adapted for Fabric 1.21
 * GUI creation is handled client-side via ClientProxy.displayGui(ScreenConfigData)
 */
public class ScreenConfigData {
    public boolean onlyUpdate;
    public Vector3i pos;
    public BlockSide side;
    public NameUUIDPair[] friends;
    public int friendRights;
    public int otherRights;
    public NameUUIDPair owner;

    public ScreenConfigData() {
    }

    public ScreenConfigData(Vector3i pos, BlockSide side, ScreenData scr) {
        this.pos = pos;
        this.side = side;
        friends = scr.friends != null ? scr.friends.toArray(new NameUUIDPair[0]) : new NameUUIDPair[0];
        friendRights = scr.friendRights;
        otherRights = scr.otherRights;
        owner = scr.owner;
        onlyUpdate = false;
    }

    public String getName() {
        return "ScreenConfig";
    }

    public ScreenConfigData updateOnly() {
        onlyUpdate = true;
        return this;
    }

    // Send to specific player
    public void sendTo(net.minecraft.server.level.ServerPlayer player) {
        WDNetworkRegistry.sendToPlayer(player, new S2CMessageOpenGui(this));
    }
    
    // Send to all players near a position (for updates)
    public void sendTo(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        WDNetworkRegistry.sendToNear(level, pos, new S2CMessageOpenGui(this));
    }

    // Serialization for packet
    public void serialize(FriendlyByteBuf buf) {
        buf.writeBoolean(onlyUpdate);
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeInt(side.ordinal());
        // Write friends array
        buf.writeInt(friends != null ? friends.length : 0);
        if (friends != null) {
            for (NameUUIDPair friend : friends) {
                friend.writeTo(buf);
            }
        }
        buf.writeInt(friendRights);
        buf.writeInt(otherRights);
        // Write owner
        buf.writeBoolean(owner != null);
        if (owner != null) {
            owner.writeTo(buf);
        }
    }

    // Deserialization from packet
    public static ScreenConfigData deserialize(FriendlyByteBuf buf) {
        ScreenConfigData data = new ScreenConfigData();
        data.onlyUpdate = buf.readBoolean();
        data.pos = new Vector3i(buf.readInt(), buf.readInt(), buf.readInt());
        data.side = BlockSide.values()[buf.readInt()];
        // Read friends array
        int friendCount = buf.readInt();
        data.friends = new NameUUIDPair[friendCount];
        for (int i = 0; i < friendCount; i++) {
            data.friends[i] = new NameUUIDPair(buf);
        }
        data.friendRights = buf.readInt();
        data.otherRights = buf.readInt();
        // Read owner
        if (buf.readBoolean()) {
            data.owner = new NameUUIDPair(buf);
        }
        return data;
    }
}
