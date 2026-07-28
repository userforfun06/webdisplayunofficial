package net.montoyo.wd.net;

import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Forge-compatible PacketDistributor for Fabric
 */
public class PacketDistributor {
    
    public enum Type {
        SERVER,
        ALL,
        PLAYER,
        TRACKING_ENTITY,
        TRACKING_CHUNK
    }
    
    private final Type type;
    private Supplier<ServerPlayer> playerSupplier;
    
    public static final PacketDistributor SERVER = new PacketDistributor(Type.SERVER);
    public static final PacketDistributor ALL = new PacketDistributor(Type.ALL);
    public static final PacketDistributor PLAYER = new PacketDistributor(Type.PLAYER);
    public static final PacketDistributor TRACKING_ENTITY = new PacketDistributor(Type.TRACKING_ENTITY);
    public static final PacketDistributor TRACKING_CHUNK = new PacketDistributor(Type.TRACKING_CHUNK);
    
    private PacketDistributor(Type type) {
        this.type = type;
    }
    
    public Type getType() {
        return type;
    }
    
    /**
     * Creates a PLAYER distributor with a specific player supplier
     */
    public PacketDistributor with(Supplier<ServerPlayer> playerSupplier) {
        if (this.type != Type.PLAYER) {
            throw new IllegalStateException("with() can only be used with PLAYER distributor");
        }
        PacketDistributor result = new PacketDistributor(Type.PLAYER);
        result.playerSupplier = playerSupplier;
        return result;
    }
    
    public Supplier<ServerPlayer> getPlayerSupplier() {
        return playerSupplier;
    }
    
    /**
     * Get the player from this distributor (if PLAYER type with supplier)
     */
    public ServerPlayer getPlayer() {
        if (playerSupplier != null) {
            return playerSupplier.get();
        }
        return null;
    }
}
