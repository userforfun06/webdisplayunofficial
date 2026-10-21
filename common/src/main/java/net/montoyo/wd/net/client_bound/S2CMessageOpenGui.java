/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.client_bound;

import net.montoyo.wd.net.NetworkEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.data.ScreenConfigData;
import net.montoyo.wd.net.AbstractPacket;
public class S2CMessageOpenGui extends AbstractPacket {
	private String guiName;
	private FriendlyByteBuf dataBuf;
	private ScreenConfigData screenConfigData;
	
	public S2CMessageOpenGui(String guiName, FriendlyByteBuf data) {
		this.guiName = guiName;
		this.dataBuf = data;
	}
	
	// Constructor for Screen Configurator GUI
	public S2CMessageOpenGui(ScreenConfigData data) {
		this.guiName = "ScreenConfig";
		this.screenConfigData = data;
	}
	
	public S2CMessageOpenGui(FriendlyByteBuf buf) {
		super(buf);
		guiName = buf.readUtf();
		if ("ScreenConfig".equals(guiName)) {
			screenConfigData = ScreenConfigData.deserialize(buf);
		} else {
			int len = buf.readableBytes();
			if (len > 0) {
				dataBuf = new FriendlyByteBuf(buf.readBytes(len));
			}
		}
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(guiName);
		if ("ScreenConfig".equals(guiName) && screenConfigData != null) {
			screenConfigData.serialize(buf);
		} else if (dataBuf != null) {
			buf.writeBytes(dataBuf);
		}
	}
	
	@Override
	public net.minecraft.resources.ResourceLocation getId() {
		return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("webdisplays", "opengui");
	}
	
	@Override
	public void handle(Player player) {
		if (player.level().isClientSide) {
			if ("ScreenConfig".equals(guiName) && screenConfigData != null) {
				WebDisplays.PROXY.displayGui(screenConfigData);
			} else {
				WebDisplays.PROXY.displayGui(guiName, dataBuf);
			}
		}
	}

	public void handle(NetworkEvent.Context context) {
		if (checkClient(context)) {
			context.enqueueWork(() -> {
				if ("ScreenConfig".equals(guiName) && screenConfigData != null) {
					WebDisplays.PROXY.displayGui(screenConfigData);
				} else {
					WebDisplays.PROXY.displayGui(guiName, dataBuf);
				}
			});
			context.setPacketHandled(true);
		}
	}
	
	public ScreenConfigData getScreenConfigData() {
		return screenConfigData;
	}
}
