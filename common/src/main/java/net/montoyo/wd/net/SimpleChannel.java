package net.montoyo.wd.net;

import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Forge-compatible SimpleChannel implementation for Fabric 1.21
 * Wraps Fabric networking to provide Forge-like API
 */
public class SimpleChannel {
    
    public SimpleChannel(String channelName) {
    }
    
    public static SimpleChannel create(String channelName) {
        return new SimpleChannel(channelName);
    }
    
    /**
     * Send a packet to the server (client-side)
     */
    public void sendToServer(Packet packet) {
        WDNetworkRegistry.sendToServer(packet);
    }
    
    /**
     * Send a packet to a specific player
     */
    public void sendToPlayer(ServerPlayer player, Packet packet) {
        WDNetworkRegistry.sendToPlayer(player, packet);
    }
    
    /**
     * Send a packet using a player supplier (Forge-compatible API)
     */
    public void sendToPlayer(Supplier<ServerPlayer> playerSupplier, Packet packet) {
        ServerPlayer player = playerSupplier.get();
        if (player != null) {
            WDNetworkRegistry.sendToPlayer(player, packet);
        }
    }
    
    /**
     * Send a packet to all players
     */
    public void sendToAllPlayers(Packet packet) {
        WDNetworkRegistry.sendToAllPlayers(packet);
    }
    
    /**
     * Send packet using PacketDistributor (Forge-compatible API)
     */
    public void send(PacketDistributor distributor, Packet packet) {
        ServerPlayer player = distributor.getPlayer();
        
        if (distributor.getType() == PacketDistributor.Type.PLAYER && player != null) {
            WDNetworkRegistry.sendToPlayer(player, packet);
        } else if (distributor.getType() == PacketDistributor.Type.ALL) {
            WDNetworkRegistry.sendToAllPlayers(packet);
        } else {
            WDNetworkRegistry.sendToAllPlayers(packet);
        }
    }
    
    /**
     * Reply to a packet (server to client)
     */
    public void reply(Packet packet, NetworkEvent.Context ctx) {
        if (ctx.getSender() != null) {
            WDNetworkRegistry.sendToPlayer(ctx.getSender(), packet);
        }
    }
    
    /**
     * Register a message type (Forge-compatible API stub)
     * Actual registration is handled by PacketFactory
     */
    public <T extends Packet> void registerMessage(int id, Class<T> clazz, 
                                java.util.function.BiConsumer<T, java.io.DataOutput> encoder,
                                java.util.function.Function<java.io.DataInput, T> decoder,
                                java.util.function.BiConsumer<T, NetworkEvent.Context> handler) {
        // Registration is handled by PacketFactory during static initialization
    }
    
}
