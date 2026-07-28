package net.montoyo.wd.net.server_bound;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.montoyo.wd.item.ItemMinePad2;
import net.montoyo.wd.net.NetworkEvent;
import net.montoyo.wd.net.Packet;

import java.util.UUID;

public class C2SMessageMinepadUrl implements Packet {
	UUID id;
	String url;

	public C2SMessageMinepadUrl(UUID id, String url) {
		this.id = id;
		this.url = url;
	}

	public C2SMessageMinepadUrl(FriendlyByteBuf buf) {
		this.id = buf.readUUID();
		this.url = buf.readUtf();
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeUUID(id);
		buf.writeUtf(url);
	}

	protected void merge(ItemStack stack) {
		if (url.isEmpty()) {
			CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
			if (customData != null) {
				CompoundTag tag = customData.copyTag();
				tag.remove("PadID");
				stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
			}
		} else {
			CompoundTag tag = new CompoundTag();
			CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
			if (existing != null) {
				tag = existing.copyTag();
			}
			tag.putUUID("PadID", id);
			tag.putString("PadURL", url);
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		}
	}

	@Override
	public void handle(NetworkEvent.Context ctx) {
		if (!ctx.getDirection().getReceptionSide().isServer()) return;

		for (InteractionHand value : InteractionHand.values()) {
			ItemStack stack = ctx.getSender().getItemInHand(value);
			if (stack.getItem() instanceof ItemMinePad2) {
				CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
				if (customData != null && customData.copyTag().contains("PadID")) {
					UUID padId = customData.copyTag().getUUID("PadID");
					if (padId.equals(id)) {
						merge(stack);
						return;
					}
				}
			}
		}

		for (InteractionHand value : InteractionHand.values()) {
			ItemStack stack = ctx.getSender().getItemInHand(value);
			if (stack.getItem() instanceof ItemMinePad2) {
				CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
				if (customData == null || !customData.copyTag().contains("PadID")) {
					merge(stack);
					return;
				}
			}
		}
	}

	@Override
	public net.minecraft.resources.ResourceLocation getId() {
		return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("webdisplays", "minepadurl");
	}
}
