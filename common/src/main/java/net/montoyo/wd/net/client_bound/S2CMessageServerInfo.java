/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.client_bound;

import net.montoyo.wd.net.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.net.AbstractPacket;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.server_bound.C2SMessageMiniservConnect;

public class S2CMessageServerInfo extends AbstractPacket {
	
	private int miniservPort;
	
	public S2CMessageServerInfo(int msPort) {
		miniservPort = msPort;
	}
	
	public S2CMessageServerInfo(FriendlyByteBuf buf) {
		super(buf);
		miniservPort = buf.readShort();
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeShort(miniservPort);
	}
	
	@Override
	public net.minecraft.resources.ResourceLocation getId() {
		return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("webdisplays", "serverinfo");
	}
	
	@Override
	public void handle(NetworkEvent.Context ctx) {
		if (checkClient(ctx)) {
			try {
				WebDisplays.PROXY.setMiniservClientPort(miniservPort);
				C2SMessageMiniservConnect message = WebDisplays.PROXY.beginMiniservConnection();
				WDNetworkRegistry.sendToServer(message);
				ctx.setPacketHandled(true);
			} catch (Throwable err) {
				err.printStackTrace();
				throw new RuntimeException(err);
			}
		}
	}
}
