package net.montoyo.wd.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record WDPacketPayload(ResourceLocation id, byte[] data) implements CustomPacketPayload {
    public static final Type<WDPacketPayload> TYPE = new Type<>(WDNetworkRegistry.CHANNEL_ID);

    // Custom StreamCodec for byte array
    public static final StreamCodec<ByteBuf, byte[]> BYTE_ARRAY_CODEC = new StreamCodec<>() {
        @Override
        public byte[] decode(ByteBuf buffer) {
            int length = buffer.readInt();
            if (length < 0 || length > 65536) {
                return new byte[0];
            }
            byte[] bytes = new byte[length];
            buffer.readBytes(bytes);
            return bytes;
        }

        @Override
        public void encode(ByteBuf buffer, byte[] bytes) {
            if (bytes == null) {
                buffer.writeInt(0);
            } else {
                buffer.writeInt(bytes.length);
                buffer.writeBytes(bytes);
            }
        }
    };

    // Use ResourceLocation.STREAM_CODEC and custom byte array codec
    public static final StreamCodec<RegistryFriendlyByteBuf, WDPacketPayload> STREAM_CODEC = StreamCodec.of(
        (buf, payload) -> {
            ResourceLocation.STREAM_CODEC.encode(buf, payload.id());
            BYTE_ARRAY_CODEC.encode(buf, payload.data());
        },
        buf -> new WDPacketPayload(
            ResourceLocation.STREAM_CODEC.decode(buf),
            BYTE_ARRAY_CODEC.decode(buf)
        )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public RegistryFriendlyByteBuf getData(net.minecraft.core.RegistryAccess registryAccess) {
        if (data == null) {
            return new RegistryFriendlyByteBuf(Unpooled.EMPTY_BUFFER, registryAccess);
        }
        return new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), registryAccess);
    }

    /**
     * Safe factory method that handles null values
     */
    public static WDPacketPayload create(ResourceLocation id, byte[] data) {
        if (id == null) {
            net.montoyo.wd.WebDisplays.LOGGER.warn("Creating payload with null id");
            id = ResourceLocation.fromNamespaceAndPath("webdisplays", "null_id");
        }
        if (data == null) {
            net.montoyo.wd.WebDisplays.LOGGER.warn("Creating payload with null data");
            data = new byte[0];
        }
        return new WDPacketPayload(id, data);
    }
}
