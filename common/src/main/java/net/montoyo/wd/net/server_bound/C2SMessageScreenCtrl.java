package net.montoyo.wd.net.server_bound;

import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.montoyo.wd.controls.ScreenControl;
import net.montoyo.wd.controls.ScreenControlRegistry;
import net.montoyo.wd.controls.builtin.LaserControl;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.net.NetworkEvent;
import net.montoyo.wd.net.AbstractPacket;
import net.montoyo.wd.core.MissingPermissionException;
import net.montoyo.wd.net.BufferUtils;
import net.montoyo.wd.utilities.math.Vector2i;
import net.montoyo.wd.utilities.data.BlockSide;

public class C2SMessageScreenCtrl extends AbstractPacket {

	ScreenControl control;
	BlockPos pos;
	BlockSide side;

	public C2SMessageScreenCtrl() {
	}

	public C2SMessageScreenCtrl(ScreenBlockEntity screen, BlockSide side, ScreenControl control) {
		this.pos = screen.getBlockPos();
		this.side = side;
		this.control = control;
	}

	protected static C2SMessageScreenCtrl base(ScreenBlockEntity screen, BlockSide side) {
		C2SMessageScreenCtrl packet = new C2SMessageScreenCtrl();
		packet.pos = screen.getBlockPos();
		packet.side = side;
		return packet;
	}

	public static C2SMessageScreenCtrl laserMove(ScreenBlockEntity tes, BlockSide side, Vector2i vec) {
		C2SMessageScreenCtrl ret = base(tes, side);
		ret.control = new LaserControl(LaserControl.ControlType.MOVE, vec);
		return ret;
	}

	public static C2SMessageScreenCtrl laserDown(ScreenBlockEntity tes, BlockSide side, Vector2i vec, int button) {
		C2SMessageScreenCtrl ret = base(tes, side);
		ret.control = new LaserControl(LaserControl.ControlType.DOWN, vec, button);
		return ret;
	}

	public static C2SMessageScreenCtrl laserUp(ScreenBlockEntity tes, BlockSide side, int button) {
		C2SMessageScreenCtrl ret = base(tes, side);
		ret.control = new LaserControl(LaserControl.ControlType.UP, null, button);
		return ret;
	}

	public C2SMessageScreenCtrl(FriendlyByteBuf buf) {
		super(buf);

		pos = buf.readBlockPos();
		side = (BlockSide) BufferUtils.readEnum(buf, (i) -> BlockSide.values()[i], (byte) 1);

		this.control = ScreenControlRegistry.parse(buf);
	}

	@Override
	public void write(FriendlyByteBuf buf) {
		buf.writeBlockPos(pos);
		BufferUtils.writeEnum(buf, side, (byte) 1);

		buf.writeUtf(control.getId().toString());
		control.write(buf);
	}

	@Override
	public net.minecraft.resources.ResourceLocation getId() {
		return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("webdisplays", "screenctrl");
	}

	public void checkPermission(ServerPlayer sender, ScreenBlockEntity scr, int right) throws MissingPermissionException {
		int prights = scr.getScreen(side).rightsFor(sender);
		if ((prights & right) == 0)
			throw new MissingPermissionException(right, sender);
	}

	public void handle(NetworkEvent.Context ctx) {
		if (checkServer(ctx)) {
			ctx.enqueueWork(() -> {
				try {
					if (control == null) return;

					Level level = ctx.getSender().level();
					BlockEntity be = level.getBlockEntity(pos);
					if (be instanceof ScreenBlockEntity tes) {
						control.handleServer(pos, side, tes, ctx, (perm) -> {
							try {
								checkPermission(ctx.getSender(), tes, perm);
								return true;
							} catch (Throwable ignored) {
								return false;
							}
						});
					}
				} catch (MissingPermissionException e) {
					e.printStackTrace();
				} catch (Throwable ignored) {
				}
			});
			ctx.setPacketHandled(true);
		}
	}

}
