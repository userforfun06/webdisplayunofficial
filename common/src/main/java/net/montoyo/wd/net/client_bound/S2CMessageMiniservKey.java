/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.client_bound;

import net.montoyo.wd.net.NetworkEvent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.net.BufferUtils;
import net.montoyo.wd.net.AbstractPacket;
import net.montoyo.wd.utilities.Log;

public class S2CMessageMiniservKey extends AbstractPacket {
	private byte[] encryptedKey;
	
	public S2CMessageMiniservKey(byte[] key) {
		encryptedKey = key;
	}
	
	public S2CMessageMiniservKey(FriendlyByteBuf buf) {
		super(buf);
		encryptedKey = BufferUtils.readBytes(buf);
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		BufferUtils.writeBytes(buf, encryptedKey);
	}
	
	@Override
	public ResourceLocation getId() {
		return ResourceLocation.fromNamespaceAndPath("webdisplays", "miniservkey");
	}
	
	@Override
	public void handle(NetworkEvent.Context ctx) {
		if (checkClient(ctx)) {
			if (WebDisplays.PROXY.decryptKey(encryptedKey)) {
				Log.info("Successfully received and decrypted key, starting miniserv client...");
				WebDisplays.PROXY.startMiniservClient();
			}
			
			ctx.setPacketHandled(true);
		}
	}
}
