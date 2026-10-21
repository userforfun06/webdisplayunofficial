package net.montoyo.wd.controls.builtin;

import net.montoyo.wd.WebDisplaysMod;
import net.montoyo.wd.net.NetworkEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.montoyo.wd.controls.ScreenControl;
import net.montoyo.wd.core.MissingPermissionException;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;

import java.util.function.Function;

public class TurnOffControl extends ScreenControl {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("webdisplays", "turn_off");
	
	public static final TurnOffControl INSTANCE = new TurnOffControl();
	
	private TurnOffControl() {
		super(ID);
	}
	
	public TurnOffControl(FriendlyByteBuf buf) {
		super(ID);
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		// No data to write
	}
	
	@Override
	public void handleServer(BlockPos pos, BlockSide side, ScreenBlockEntity tes, NetworkEvent.Context ctx, Function<Integer, Boolean> permissionChecker) throws MissingPermissionException {
		tes.turnOff(side);
	}

	@Override
	public void handleClient(BlockPos pos, BlockSide side, ScreenBlockEntity tes, NetworkEvent.Context ctx) {
		if (side != null) {
			WebDisplaysMod.PROXY.closeGui(pos, side);
			tes.turnOff(side);
		} else {
			for (BlockSide value : BlockSide.values()) {
				WebDisplaysMod.PROXY.closeGui(pos, value);
				tes.turnOff(value);
			}
		}
	}
}
