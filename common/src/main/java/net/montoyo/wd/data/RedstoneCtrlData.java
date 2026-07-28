package net.montoyo.wd.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.client_bound.S2CMessageRedstoneCtrl;

public class RedstoneCtrlData {
    private final ResourceLocation dimension;
    private final BlockPos pos;
    private final String risingEdgeURL;
    private final String fallingEdgeURL;
    
    public RedstoneCtrlData(ResourceLocation dimension, BlockPos pos, String risingEdgeURL, String fallingEdgeURL) {
        this.dimension = dimension;
        this.pos = pos;
        this.risingEdgeURL = risingEdgeURL;
        this.fallingEdgeURL = fallingEdgeURL;
    }
    
    public void sendTo(ServerPlayer player) {
        WDNetworkRegistry.sendToPlayer(player, new S2CMessageRedstoneCtrl(dimension, pos, risingEdgeURL, fallingEdgeURL));
    }
}
