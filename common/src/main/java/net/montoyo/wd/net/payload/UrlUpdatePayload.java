package net.montoyo.wd.net.payload;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import net.montoyo.wd.utilities.data.BlockSide;

/**
 * Fabric 1.21 Payload for updating screen URL
 * This is sent from client to server when the player clicks OK in the URL box
 */
public record UrlUpdatePayload(BlockPos pos, BlockSide side, String url) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("webdisplays", "url_update");
    public static final Type<UrlUpdatePayload> TYPE = new Type<>(ID);

    // StreamCodec for serialization
    public static final StreamCodec<FriendlyByteBuf, UrlUpdatePayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            buf.writeBlockPos(payload.pos);
            buf.writeUtf(payload.side.name());
            buf.writeUtf(payload.url);
        },
        buf -> new UrlUpdatePayload(
            buf.readBlockPos(),
            BlockSide.valueOf(buf.readUtf()),
            buf.readUtf()
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
