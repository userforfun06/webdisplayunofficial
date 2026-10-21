package net.montoyo.wd.controls.builtin;

import net.montoyo.wd.net.NetworkEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.montoyo.wd.controls.ScreenControl;
import net.montoyo.wd.core.MissingPermissionException;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;

import java.util.function.Function;

public class ManageRightsAndUpgradesControl extends ScreenControl {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("webdisplays", "manage_rights_upgrades");
	
	private boolean adding;
	private ItemStack stack;
	private NameUUIDPair friend;
	private int friendRights;
	private int otherRights;
	
	public ManageRightsAndUpgradesControl(boolean adding, ItemStack stack) {
		super(ID);
		this.adding = adding;
		this.stack = stack;
	}
	
	public ManageRightsAndUpgradesControl(NameUUIDPair friend, boolean adding) {
		super(ID);
		this.friend = friend;
		this.adding = adding;
	}
	
	public ManageRightsAndUpgradesControl(int friendRights, int otherRights) {
		super(ID);
		this.friendRights = friendRights;
		this.otherRights = otherRights;
	}
	
	public ManageRightsAndUpgradesControl(FriendlyByteBuf buf) {
		super(ID);
		byte type = buf.readByte();
		switch (type) {
			case 0 -> {
				adding = buf.readBoolean();
				stack = ItemStack.STREAM_CODEC.decode((RegistryFriendlyByteBuf) buf);
			}
			case 1 -> {
				adding = buf.readBoolean();
				friend = new NameUUIDPair(buf);
			}
			case 2 -> {
				friendRights = buf.readInt();
				otherRights = buf.readInt();
			}
		}
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		if (stack != null) {
			buf.writeByte(0);
			buf.writeBoolean(adding);
			ItemStack.STREAM_CODEC.encode((RegistryFriendlyByteBuf) buf, stack);
		} else if (friend != null) {
			buf.writeByte(1);
			buf.writeBoolean(adding);
			friend.writeTo(buf);
		} else {
			buf.writeByte(2);
			buf.writeInt(friendRights);
			buf.writeInt(otherRights);
		}
	}
	
	@Override
	public void handleServer(BlockPos pos, BlockSide side, ScreenBlockEntity tes, NetworkEvent.Context ctx, Function<Integer, Boolean> permissionChecker) throws MissingPermissionException {
		ServerPlayer player = ctx.getSender();
		if (stack != null) {
			checkPerms(ScreenRights.MANAGE_UPGRADES, permissionChecker, player);
			if (adding)
				throw new RuntimeException("Cannot add an upgrade from the client");
			else
				tes.removeUpgrade(side, stack, player);
		} else if (friend != null) {
			checkPerms(ScreenRights.MANAGE_FRIEND_LIST, permissionChecker, player);
			if (adding)
				tes.addFriend(player, side, friend);
			else
				tes.removeFriend(player, side, friend);
		} else {
			ScreenData scr = tes.getScreen(side);
			if (scr == null) {
				net.montoyo.wd.WebDisplays.LOGGER.warn("ManageRights: no screen data for side {}", side);
				return;
			}
			if (scr.owner == null) {
				net.montoyo.wd.WebDisplays.LOGGER.warn("ManageRights: owner is NULL for screen at {}", tes.getBlockPos());
			}
			boolean isOwner = scr.owner != null && scr.owner.uuid.equals(player.getGameProfile().getId());
			net.montoyo.wd.WebDisplays.LOGGER.debug("ManageRights: player={} isOwner={} packet(fr={},or={}) current(fr={},or={})", player.getGameProfile().getName(), isOwner, friendRights, otherRights, scr.friendRights, scr.otherRights);
			int fr = isOwner ? friendRights : scr.friendRights;
			int hasOtherPerm = (scr.rightsFor(player) & ScreenRights.MANAGE_OTHER_RIGHTS);
			net.montoyo.wd.WebDisplays.LOGGER.debug("ManageRights: isOwner={} hasOtherPerm={} fr={} or_candidate={}", isOwner, hasOtherPerm, fr, otherRights);
			int or = hasOtherPerm == 0 ? scr.otherRights : otherRights;
			net.montoyo.wd.WebDisplays.LOGGER.debug("ManageRights: final fr={} or={} changed={}", fr, or, scr.friendRights != fr || scr.otherRights != or);
			if (scr.friendRights != fr || scr.otherRights != or)
				tes.setRights(player, side, fr, or);
		}
	}
	
	@Override
	public void handleClient(BlockPos pos, BlockSide side, ScreenBlockEntity tes, NetworkEvent.Context ctx) {
		ServerPlayer player = ctx != null ? ctx.getSender() : null;
		if (stack != null) {
			if (adding)
				tes.addUpgrade(side, stack, player, true);
			else
				tes.removeUpgrade(side, stack, player);
		} else if (friend != null) {
			if (adding)
				tes.addFriend(player, side, friend);
			else
				tes.removeFriend(player, side, friend);
		} else {
			net.montoyo.wd.WebDisplays.LOGGER.debug("ManageRights.handleClient: setting rights fr={} or={}", friendRights, otherRights);
			tes.setRights(player, side, friendRights, otherRights);
		}
	}
}
