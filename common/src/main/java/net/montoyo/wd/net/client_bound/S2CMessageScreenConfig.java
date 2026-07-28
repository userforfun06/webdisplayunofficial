package net.montoyo.wd.net.client_bound;

import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.net.AbstractPacket;
import net.montoyo.wd.net.NetworkEvent;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.math.Vector3i;

public class S2CMessageScreenConfig extends AbstractPacket {
    private Vector3i pos;
    private BlockSide side;
    private ScreenData screen;
    
    public S2CMessageScreenConfig(Vector3i pos, BlockSide side, ScreenData screen) {
        this.pos = pos;
        this.side = side;
        this.screen = screen;
    }
    
    public S2CMessageScreenConfig(RegistryFriendlyByteBuf buf) {
        super(buf);
        pos = new Vector3i(buf.readInt(), buf.readInt(), buf.readInt());
        side = BlockSide.values()[buf.readInt()];
        // Read ScreenData from NBT
        CompoundTag tag = buf.readNbt();
        if (tag != null) {
            screen = ScreenData.deserialize(tag);
            screen.side = side; // Ensure side is set correctly
        }
    }
    
    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeInt(side.ordinal());
        // Write ScreenData as NBT
        if (screen != null) {
            buf.writeNbt(screen.serialize());
        } else {
            buf.writeNbt(null);
        }
    }
    
    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath("webdisplays", "screen_config");
    }
    
    @Override
    public void handle(Player player) {
        WebDisplays.LOGGER.debug("S2CMessageScreenConfig.handle() called, clientSide={}", player.level().isClientSide);
        if (player.level().isClientSide && screen != null) {
            WebDisplays.LOGGER.debug("Opening screen config GUI");
            WebDisplays.PROXY.openScreenConfigGui(pos, side, screen);
        }
    }
    
    @Override
    public void handle(NetworkEvent.Context ctx) {
        // Client-side handling is done via handle(Player player) method
        // which is called by WebDisplaysClient when the packet is received
    }
    
    public Vector3i getPos() {
        return pos;
    }
    
    public BlockSide getSide() {
        return side;
    }
    
    public ScreenData getScreen() {
        return screen;
    }
}
