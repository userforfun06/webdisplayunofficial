package net.montoyo.wd.controls.builtin;

import net.montoyo.wd.net.NetworkEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.montoyo.wd.controls.ScreenControl;
import net.montoyo.wd.core.MissingPermissionException;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;

import java.util.function.Function;

public class KeyTypedControl extends ScreenControl {
	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("webdisplays", "key_typed");
	
	public String text;
	public BlockPos pos;
	
	public KeyTypedControl(String text, BlockPos pos) {
		super(ID);
		this.text = text;
		this.pos = pos;
	}
	
	public KeyTypedControl(FriendlyByteBuf buf) {
		super(ID);
		text = buf.readUtf();
		pos = buf.readBlockPos();
	}
	
	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeUtf(text);
		buf.writeBlockPos(pos);
	}
	
	@Override
	public void handleServer(BlockPos pos, BlockSide side, ScreenBlockEntity tes, NetworkEvent.Context ctx, Function<Integer, Boolean> permissionChecker) throws MissingPermissionException {
		ServerPlayer player = ctx.getSender();
		checkPerms(ScreenRights.INTERACT, permissionChecker, player);
		tes.type(side, this.text, this.pos, player);
	}
	
	@Override
	public void handleClient(BlockPos pos, BlockSide side, ScreenBlockEntity tes, NetworkEvent.Context ctx) {
		// Type text into the browser - correct signature: type(BlockSide, String, BlockPos)
		tes.type(side, this.text, this.pos);
	}
}
