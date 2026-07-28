package net.montoyo.wd.net;

import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import java.io.DataOutput;
import java.io.DataInput;

public class NetworkEntry<T extends Packet> {
	Class<T> clazz;
	Function<FriendlyByteBuf, T> fabricator;
	BiConsumer<T, DataOutput> encoder;
	Function<DataInput, T> decoder;
	BiConsumer<T, NetworkEvent.Context> handler;
	
	public NetworkEntry(Class<T> clazz, Function<FriendlyByteBuf, T> fabricator) {
		this.clazz = clazz;
		this.fabricator = fabricator;
	}
	
	public void register(int indx, SimpleChannel channel) {
		// Registration is handled by PacketFactory
		// channel.registerMessage(indx, clazz, encoder, decoder, handler);
	}
}
