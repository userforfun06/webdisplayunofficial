package net.montoyo.wd.net;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Abstract base class for packets that need class-based implementation.
 * For records, implement Packet directly. For classes, extend this.
 */
public abstract class AbstractPacket implements Packet {
    
    protected AbstractPacket() {
    }
    
    protected AbstractPacket(FriendlyByteBuf buf) {
    }
    
    @Override
    public abstract void write(FriendlyByteBuf buf);
    
    @Override
    public abstract void handle(NetworkEvent.Context ctx);
    
    @Override
    public boolean checkClient(NetworkEvent.Context ctx) {
        return ctx.getDirection().getReceptionSide().isClient();
    }
    
    @Override
    public boolean checkServer(NetworkEvent.Context ctx) {
        return ctx.getDirection().getReceptionSide().isServer();
    }
    
    @Override
    public void respond(NetworkEvent.Context ctx, Packet packet) {
        ctx.enqueueWork(() -> WDNetworkRegistry.INSTANCE.reply(packet, ctx));
    }
}
