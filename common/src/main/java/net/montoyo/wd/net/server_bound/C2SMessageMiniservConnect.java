/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.server_bound;

import net.montoyo.wd.net.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.montoyo.wd.miniserv.server.ClientManager;
import net.montoyo.wd.miniserv.server.Server;
import net.montoyo.wd.net.BufferUtils;
import net.montoyo.wd.net.AbstractPacket;
import net.montoyo.wd.net.client_bound.S2CMessageMiniservKey;

import java.util.Objects;

public class C2SMessageMiniservConnect extends AbstractPacket {
	private byte[] modulus;
	private byte[] exponent;
	
	public C2SMessageMiniservConnect(byte[] mod, byte[] exp) {
		modulus = mod;
		exponent = exp;
	}
	
	public C2SMessageMiniservConnect(FriendlyByteBuf buf) {
		super(buf);
		
		modulus = BufferUtils.readBytes(buf);
		exponent = BufferUtils.readBytes(buf);
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		BufferUtils.writeBytes(buf, modulus);
		BufferUtils.writeBytes(buf, exponent);
	}
	
	@Override
	public net.minecraft.resources.ResourceLocation getId() {
		return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("webdisplays", "miniservconnect");
	}
	
	@Override
	public void handle(NetworkEvent.Context ctx) {
		if (checkServer(ctx)) {
			try {
				ClientManager cliMgr = Server.getInstance().getClientManager();
				byte[] encKey = cliMgr.encryptClientKey(Objects.requireNonNull(ctx.getSender()).getGameProfile().getId(), modulus, exponent);
				
				if (encKey != null) {
					respond(ctx, new S2CMessageMiniservKey(encKey));
				}
				
				ctx.setPacketHandled(true);
			} catch (Throwable err) {
				err.printStackTrace();
				throw new RuntimeException(err);
			}
		}
	}
}
