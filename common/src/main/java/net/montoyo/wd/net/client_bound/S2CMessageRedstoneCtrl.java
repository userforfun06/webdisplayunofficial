package net.montoyo.wd.net.client_bound;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.net.NetworkEvent;
import net.montoyo.wd.net.AbstractPacket;

public class S2CMessageRedstoneCtrl extends AbstractPacket {
    private final ResourceLocation dimension;
    private final BlockPos pos;
    private final String risingEdgeURL;
    private final String fallingEdgeURL;

    public S2CMessageRedstoneCtrl(ResourceLocation dimension, BlockPos pos, String risingEdgeURL, String fallingEdgeURL) {
        this.dimension = dimension;
        this.pos = pos;
        this.risingEdgeURL = risingEdgeURL;
        this.fallingEdgeURL = fallingEdgeURL;
    }

    public S2CMessageRedstoneCtrl(FriendlyByteBuf buf) {
        super(buf);
        this.dimension = buf.readResourceLocation();
        this.pos = buf.readBlockPos();
        this.risingEdgeURL = buf.readUtf();
        this.fallingEdgeURL = buf.readUtf();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeResourceLocation(dimension);
        buf.writeBlockPos(pos);
        buf.writeUtf(risingEdgeURL);
        buf.writeUtf(fallingEdgeURL);
    }

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath("webdisplays", "redstonectrl");
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        if (checkClient(ctx)) {
            ctx.setPacketHandled(true);
        }
    }

    @Override
    public void handle(Player player) {
        if (player.level().isClientSide) {
            WebDisplays.PROXY.displayRedstoneCtrl(dimension, pos, risingEdgeURL, fallingEdgeURL);
        }
    }
}
