/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.client_bound;

import net.montoyo.wd.net.NetworkEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.montoyo.wd.WebDisplaysMod;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.net.AbstractPacket;
import net.montoyo.wd.utilities.math.Vector2i;
import net.montoyo.wd.utilities.math.Vector3i;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.data.Rotation;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;

import java.util.ArrayList;

public class S2CMessageAddScreen extends AbstractPacket {
	private boolean clear;
	private Vector3i pos;
	private ScreenData[] screens;
	
	public S2CMessageAddScreen(ScreenBlockEntity tes) {
		clear = true;
		pos = new Vector3i(tes.getBlockPos());
		screens = new ScreenData[tes.screenCount()];
		
		for (int i = 0; i < tes.screenCount(); i++)
			screens[i] = tes.getScreen(i);
	}
	
	public S2CMessageAddScreen(ScreenBlockEntity tes, ScreenData... toSend) {
		clear = false;
		pos = new Vector3i(tes.getBlockPos());
		screens = toSend;
	}
	
	public S2CMessageAddScreen(boolean clear, Vector3i pos, ScreenData[] screens) {
		this.clear = clear;
		this.pos = pos;
		this.screens = screens;
	}
	
	public S2CMessageAddScreen(FriendlyByteBuf buf) {
		super(buf);
		
		clear = buf.readBoolean();
		pos = new Vector3i(buf);
		
		int cnt = buf.readByte() & 7;
		
		screens = new ScreenData[cnt];
		for (int i = 0; i < cnt; i++) {
			screens[i] = new ScreenData();
			screens[i].side = BlockSide.values()[buf.readByte()];
			screens[i].size = new Vector2i(buf);
			screens[i].url = buf.readUtf();
			screens[i].resolution = new Vector2i(buf);
			screens[i].rotation = Rotation.values()[buf.readByte() & 3];
			screens[i].owner = new NameUUIDPair(buf);
			screens[i].upgrades = new ArrayList<>();
			
			int numUpgrades = buf.readByte();
			for (int j = 0; j < numUpgrades; j++) {
				// MC 1.21: Use RegistryFriendlyByteBuf for proper ItemStack decoding
				RegistryFriendlyByteBuf regBuf = (RegistryFriendlyByteBuf) buf;
				screens[i].upgrades.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(regBuf));
			}
		}
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeBoolean(clear);
		pos.writeTo(buf);
		buf.writeByte(screens.length);
		
		for (ScreenData scr : screens) {
			buf.writeByte(scr.side.ordinal());
			scr.size.writeTo(buf);
			buf.writeUtf(scr.url);
			scr.resolution.writeTo(buf);
			buf.writeByte(scr.rotation.ordinal());
			scr.owner.writeTo(buf);
			buf.writeByte(scr.upgrades.size());
			
			for (ItemStack is : scr.upgrades) {
				// MC 1.21: Use RegistryFriendlyByteBuf for proper ItemStack encoding
				RegistryFriendlyByteBuf regBuf = (RegistryFriendlyByteBuf) buf;
				ItemStack.OPTIONAL_STREAM_CODEC.encode(regBuf, is);
			}
		}
	}
	
	@Override
	public net.minecraft.resources.ResourceLocation getId() {
		return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("webdisplays", "addscreen");
	}
	
	@Override
	public void handle(Player player) {
		if (player != null && player.level().isClientSide) {
			WebDisplaysMod.PROXY.handleAddScreen(pos, clear, screens);
		}
	}
	
	@Override
	public void handle(NetworkEvent.Context ctx) {
		if (checkClient(ctx)) {
			ctx.enqueueWork(() -> {
				// Delegate to client proxy - handles all client-side logic
				WebDisplaysMod.PROXY.handleAddScreen(pos, clear, screens);
			});
			
			ctx.setPacketHandled(true);
		}
	}
}
