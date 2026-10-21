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
import net.montoyo.wd.utilities.math.Vector2i;

import java.util.function.Function;

public class ClickControl extends ScreenControl {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("webdisplays", "click");
	
	public enum ControlType {
		CLICK, MOVE, DOWN, UP
	}
	
	public ControlType type;
	public Vector2i coord;
	private int button = -1;
	
	public ClickControl(ControlType type, Vector2i coord) {
		this(type, coord, -1);
	}
	
	public ClickControl(ControlType type, Vector2i coord, int button) {
		super(ID);
		this.type = type;
		this.coord = coord;
		this.button = button;
	}
	
	public ClickControl(FriendlyByteBuf buf) {
		super(ID);
		type = ControlType.values()[buf.readByte()];
		if (!type.equals(ControlType.UP))
			coord = new Vector2i(buf);
		if (!type.equals(ControlType.MOVE))
			button = buf.readInt();
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeByte(type.ordinal());
		if (coord != null) coord.writeTo(buf);
		if (type != ControlType.MOVE) buf.writeInt(button);
	}
	
	@Override
	public void handleServer(BlockPos pos, BlockSide side, ScreenBlockEntity tes, NetworkEvent.Context ctx, Function<Integer, Boolean> permissionChecker) throws MissingPermissionException {
		if (coord == null || type != ClickControl.ControlType.CLICK) return;
		if (!permissionChecker.apply(ScreenRights.INTERACT)) return;
		tes.click(side, coord);
	}
	
	@Override
	public void handleClient(BlockPos pos, BlockSide side, ScreenBlockEntity tes, NetworkEvent.Context ctx) {
		if (tes.getScreen(side) == null) return;
		if (coord != null)
			tes.handleMouseEvent(side, ClickControl.ControlType.MOVE, coord, -1);
		
		// Skip duplicate non-MOVE if local processing just handled it (prevents double-click from echo)
		if (type != ClickControl.ControlType.MOVE && coord != null) {
			var scr = tes.getScreen(side);
			if (scr != null && scr.lastClickTime > 0 && System.currentTimeMillis() - scr.lastClickTime < 500) {
				if (coord.x == scr.lastMousePos.x && coord.y == scr.lastMousePos.y) {
					scr.lastClickTime = 0;
					return;
				}
			}
		}
		
		int btn = button;
		if (btn == -1) btn = (type == ClickControl.ControlType.MOVE) ? 0 : 1;
		tes.handleMouseEvent(side, type, coord, btn);
	}
}

