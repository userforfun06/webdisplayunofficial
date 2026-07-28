package net.montoyo.wd.net;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Packet interface for Fabric 1.21 networking.
 * All packets must implement this interface.
 * For records, implement this directly. For classes, extend AbstractPacket.
 */
public interface Packet extends CustomPacketPayload {
    
    /**
     * Get the packet ID - must be unique per packet type
     */
    ResourceLocation getId();
    
    /**
     * Write packet data to buffer
     */
    void write(FriendlyByteBuf buf);
    
    /**
     * Handle packet on receiving side
     */
    void handle(NetworkEvent.Context ctx);
    
    /**
     * Handle packet with just a Player (client-side overload).
     * Creates a NetworkEvent.Context and delegates to handle(Context).
     */
    default void handle(Player player) {
        NetworkEvent.Context ctx = new NetworkEvent.Context(
            player instanceof ServerPlayer ? (ServerPlayer) player : null,
            player instanceof ServerPlayer
        );
        handle(ctx);
    }
    
    /**
     * Check if this is a client packet
     */
    default boolean checkClient(NetworkEvent.Context ctx) {
        return ctx.getDirection().getReceptionSide().isClient();
    }
    
    /**
     * Check if this is a server packet
     */
    default boolean checkServer(NetworkEvent.Context ctx) {
        return ctx.getDirection().getReceptionSide().isServer();
    }
    
    /**
     * Respond to this packet
     */
    default void respond(NetworkEvent.Context ctx, Packet packet) {
        ctx.enqueueWork(() -> WDNetworkRegistry.INSTANCE.reply(packet, ctx));
    }
    
    @Override
    default Type<? extends CustomPacketPayload> type() {
        return new Type<>(getId());
    }
}
