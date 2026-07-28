package net.montoyo.wd.controls.builtin;

import net.montoyo.wd.net.NetworkEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.montoyo.wd.controls.ScreenControl;
import net.montoyo.wd.core.MissingPermissionException;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;

import java.util.function.Function;

public class OwnerControl extends ScreenControl {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("webdisplays", "owner");
	
	public NameUUIDPair owner;
	
	public OwnerControl(NameUUIDPair owner) {
		super(ID);
		this.owner = owner;
	}
	
	public OwnerControl(FriendlyByteBuf buf) {
		super(ID);
		owner = new NameUUIDPair(buf);
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		owner.writeTo(buf);
	}
	
	@Override
	public void handleServer(BlockPos pos, BlockSide side, ScreenBlockEntity tes, NetworkEvent.Context ctx, Function<Integer, Boolean> permissionChecker) throws MissingPermissionException {
		tes.setOwner(side, owner);
	}
	
	@Override
	public void handleClient(BlockPos pos, BlockSide side, ScreenBlockEntity tes, NetworkEvent.Context ctx) {
		ScreenData scr = tes.getScreen(side);
		if (scr != null)
			scr.owner = owner;
	}
}
