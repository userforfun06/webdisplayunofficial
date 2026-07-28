package net.montoyo.wd.controls.builtin;

import net.montoyo.wd.net.NetworkEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.montoyo.wd.controls.ScreenControl;
import net.montoyo.wd.core.JSServerRequest;
import net.montoyo.wd.core.MissingPermissionException;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;

import java.util.function.Function;

public class JSRequestControl extends ScreenControl {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("webdisplays", "js_request");
	
	public enum JSRequestType {
		QUERY, CANCEL
	}
	
	public JSRequestType type;
	public int queryId;
	public String query;
	
	public JSRequestControl(JSRequestType type, int queryId, String query) {
		super(ID);
		this.type = type;
		this.queryId = queryId;
		this.query = query;
	}
	
	// Constructor for JSServerRequest compatibility
	public JSRequestControl(int queryId, JSServerRequest requestType, Object[] data) {
		super(ID);
		this.type = JSRequestType.QUERY;
		this.queryId = queryId;
		this.query = "";
		// Data is handled separately through the request type
	}
	
	public JSRequestControl(FriendlyByteBuf buf) {
		super(ID);
		type = JSRequestType.values()[buf.readByte()];
		queryId = buf.readInt();
		if (type == JSRequestType.QUERY) {
			query = buf.readUtf();
		}
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeByte(type.ordinal());
		buf.writeInt(queryId);
		if (type == JSRequestType.QUERY && query != null) {
			buf.writeUtf(query);
		}
	}
	
	@Override
	public void handleServer(BlockPos pos, BlockSide side, ScreenBlockEntity tes, NetworkEvent.Context ctx, Function<Integer, Boolean> permissionChecker) throws MissingPermissionException {
		// JS requests handled on server
	}
	
	@Override
	public void handleClient(BlockPos pos, BlockSide side, ScreenBlockEntity tes, NetworkEvent.Context ctx) {
		// JS requests not handled on client directly
	}
}
