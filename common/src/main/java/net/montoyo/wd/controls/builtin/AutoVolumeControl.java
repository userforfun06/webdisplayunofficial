package net.montoyo.wd.controls.builtin;

import net.montoyo.wd.net.NetworkEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.montoyo.wd.controls.ScreenControl;
import net.montoyo.wd.core.MissingPermissionException;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;

import java.util.function.Function;

public class AutoVolumeControl extends ScreenControl {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("webdisplays", "auto_volume");
	
	public boolean autoVolume;
	
	public AutoVolumeControl(boolean autoVolume) {
		super(ID);
		this.autoVolume = autoVolume;
	}
	
	public AutoVolumeControl(FriendlyByteBuf buf) {
		super(ID);
		autoVolume = buf.readBoolean();
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeBoolean(autoVolume);
	}
	
	@Override
	public void handleServer(BlockPos pos, BlockSide side, ScreenBlockEntity tes, NetworkEvent.Context ctx, Function<Integer, Boolean> permissionChecker) throws MissingPermissionException {
		tes.setAutoVolume(side, autoVolume);
	}
	
	@Override
	public void handleClient(BlockPos pos, BlockSide side, ScreenBlockEntity tes, NetworkEvent.Context ctx) {
		tes.setAutoVolume(side, autoVolume);
	}
}
