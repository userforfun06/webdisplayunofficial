package net.montoyo.wd.net;

import net.minecraft.server.level.ServerPlayer;

/**
 * Forge-compatible NetworkEvent.Context for Fabric
 */
public class NetworkEvent {
    
    public static class Context {
        private ServerPlayer sender;
        private boolean isServer;
        
        public Context() {
            this(null, false);
        }
        
        public Context(ServerPlayer sender, boolean isServer) {
            this.sender = sender;
            this.isServer = isServer;
        }
        
        public ServerPlayer getSender() {
            return sender;
        }
        
        public void setSender(ServerPlayer sender) {
            this.sender = sender;
        }
        
        public boolean isServer() {
            return isServer;
        }
        
        public Direction getDirection() {
            return isServer ? Direction.SERVERBOUND : Direction.CLIENTBOUND;
        }
        
        public void setPacketHandled(boolean handled) {
            // No-op for compatibility
        }
        
        public void enqueueWork(Runnable runnable) {
            runnable.run();
        }
    }
    
    public enum Direction {
        SERVERBOUND,
        CLIENTBOUND;
        
        public boolean isClient() {
            return this == CLIENTBOUND;
        }
        
        public boolean isServer() {
            return this == SERVERBOUND;
        }
        
        public Direction getReceptionSide() {
            return this;
        }
    }
}
