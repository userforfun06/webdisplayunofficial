package net.montoyo.wd.controls.builtin;

import net.montoyo.wd.net.NetworkEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.montoyo.wd.controls.ScreenControl;
import net.montoyo.wd.core.MissingPermissionException;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.data.Rotation;
import net.montoyo.wd.utilities.math.Vector2i;

import java.util.function.Function;

public class ScreenModifyControl extends ScreenControl {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("webdisplays", "screen_modify");
	
	public Vector2i resolution;
	public Rotation rotation;
	
	public ScreenModifyControl(Vector2i resolution) {
		super(ID);
		this.resolution = resolution;
	}
	
	public ScreenModifyControl(Rotation rotation) {
		super(ID);
		this.rotation = rotation;
	}
	
	public ScreenModifyControl(FriendlyByteBuf buf) {
		super(ID);
		// Read based on what was written
		boolean hasResolution = buf.readBoolean();
		if (hasResolution) {
			resolution = new Vector2i(buf);
		}
		boolean hasRotation = buf.readBoolean();
		if (hasRotation) {
			rotation = Rotation.values()[buf.readByte()];
		}
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeBoolean(resolution != null);
		if (resolution != null) resolution.writeTo(buf);
		buf.writeBoolean(rotation != null);
		if (rotation != null) buf.writeByte(rotation.ordinal());
	}
	
	@Override
	public void handleServer(BlockPos pos, BlockSide side, ScreenBlockEntity tes, NetworkEvent.Context ctx, Function<Integer, Boolean> permissionChecker) throws MissingPermissionException {
		if (!permissionChecker.apply(ScreenRights.MODIFY_SCREEN)) {
			throw new MissingPermissionException(ScreenRights.MODIFY_SCREEN, ctx.getSender());
		}
		if (resolution != null) {
			tes.setResolution(side, resolution);
		}
		if (rotation != null) {
			tes.setRotation(side, rotation);
		}
	}
	
	@Override
	public void handleClient(BlockPos pos, BlockSide side, ScreenBlockEntity tes, NetworkEvent.Context ctx) {
		if (resolution != null) {
			tes.setResolution(side, resolution);
		}
		if (rotation != null) {
			tes.setRotation(side, rotation);
		}
	}
}
