package net.montoyo.wd.data;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.client_bound.S2CMessageOpenGui;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.math.Vector3i;

public class SetURLData {
    private final Vector3i screenPos;
    private final BlockSide screenSide;
    private final String url;
    private final BlockPos peripheralPos;
    
    public SetURLData(Vector3i screenPos, BlockSide screenSide, String url, BlockPos peripheralPos) {
        this.screenPos = screenPos;
        this.screenSide = screenSide;
        this.url = url;
        this.peripheralPos = peripheralPos;
    }
    
    public SetURLData(Vector3i screenPos, BlockSide screenSide, String url, boolean isRemote, Vector3i remoteLocation) {
        this.screenPos = screenPos;
        this.screenSide = screenSide;
        this.url = url;
        this.peripheralPos = null;
    }
    
    public void sendTo(ServerPlayer player) {
        WebDisplays.LOGGER.debug("SetURLData.sendTo: screenPos={}, side={}, url={}", screenPos, screenSide, url);
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeInt(screenPos.x);
        buf.writeInt(screenPos.y);
        buf.writeInt(screenPos.z);
        buf.writeUtf(screenSide.name());
        buf.writeUtf(url);
        buf.writeBoolean(peripheralPos != null);
        if (peripheralPos != null) {
            buf.writeInt(peripheralPos.getX());
            buf.writeInt(peripheralPos.getY());
            buf.writeInt(peripheralPos.getZ());
        }
        WDNetworkRegistry.sendToPlayer(player, new S2CMessageOpenGui("seturl", buf));
        WebDisplays.LOGGER.debug("SetURLData.sendTo: Packet sent!");
    }
}
