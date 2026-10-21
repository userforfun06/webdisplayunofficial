/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.client.ClientProxy;
import net.montoyo.wd.controls.builtin.ClickControl;
import net.montoyo.wd.controls.builtin.LaserControl;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.server_bound.C2SMessageScreenCtrl;
import net.montoyo.wd.registry.BlockRegistry;
import net.montoyo.wd.utilities.Multiblock;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.math.Vector2i;
import net.montoyo.wd.utilities.math.Vector3i;

public final class ItemLaserPointerClient {
	private ItemLaserPointerClient() {
	}

	private static ScreenBlockEntity pointedScreen;
	private static BlockSide pointedScreenSide;
	private static long lastPointPacket;
	public static final Vector2i lastHitPos = new Vector2i();

	private static boolean left;
	private static boolean middle;
	private static boolean right;

	private static BlockPos lastHitBlockPos;
	private static Vector3i lastOriginPos;

	public static void tick(Minecraft mc) {
		BlockHitResult result = ClientProxy.raycast(64.0);

		BlockPos bpos = result.getBlockPos();

		if (result.getType() == HitResult.Type.BLOCK && mc.level.getBlockState(bpos).getBlock() == BlockRegistry.SCREEN_BLOCK) {
			Vector3i pos;
			BlockSide side = BlockSide.values()[result.getDirection().ordinal()];

			if (!bpos.equals(lastHitBlockPos) || lastOriginPos == null) {
				pos = new Vector3i(result.getBlockPos());
				Multiblock.findOrigin(mc.level, pos, side, null);
				lastHitBlockPos = bpos;
				lastOriginPos = new Vector3i(pos.x, pos.y, pos.z);
			} else {
				pos = new Vector3i(lastOriginPos.x, lastOriginPos.y, lastOriginPos.z);
			}
			ScreenBlockEntity te = (ScreenBlockEntity) mc.level.getBlockEntity(pos.toBlock());

			if (te != null && te.hasUpgrade(side, DefaultUpgrade.LASERMOUSE)) { //hasUpgrade returns false if there's no screen on side 'side'
				ScreenData scr = te.getScreen(side);

				if (scr != null && scr.browser != null) {
					float hitX = ((float) result.getLocation().x) - (float) pos.x;
					float hitY = ((float) result.getLocation().y) - (float) pos.y;
					float hitZ = ((float) result.getLocation().z) - (float) pos.z;
					Vector2i tmp = new Vector2i();

					if (ScreenBlock.hit2pixels(side, bpos, new Vector3i(result.getBlockPos()), scr, hitX, hitY, hitZ, tmp)) {
						laserClick(te, side, tmp);
					}
				}
			}
		} else {
			lastHitBlockPos = null;
			lastOriginPos = null;
		}
	}

	public static void deselect(Minecraft mc) {
		deselectScreen();
	}

	private static void laserClick(ScreenBlockEntity tes, BlockSide side, Vector2i hit) {
		lastHitPos.x = hit.x;
		lastHitPos.y = hit.y;
		tes.handleMouseEvent(side, ClickControl.ControlType.MOVE, hit, -1);
		if (pointedScreen == tes && pointedScreenSide == side) {
			long t = System.currentTimeMillis();

			if (t - lastPointPacket >= 100) {
				lastPointPacket = t;
				WDNetworkRegistry.sendToServer(C2SMessageScreenCtrl.laserMove(tes, side, hit));
			}
		} else {
			deselectScreen();
			pointedScreen = tes;
			pointedScreenSide = side;
		}
	}

	public static ScreenBlockEntity getPointedScreen() {
		return pointedScreen;
	}

	public static BlockSide getPointedScreenSide() {
		return pointedScreenSide;
	}

	private static void deselectScreen() {
		pointedScreen = null;
		pointedScreenSide = null;
	}

	public static void press(boolean press, int button) {
		if (button <= 1 && net.montoyo.wd.config.ClientConfig.Input.switchButtons)
			button = 1 - button;

		if (button == 0) left = press;
		else if (button == 1) right = press;
		else if (button == 2) middle = press;

		Minecraft mc = Minecraft.getInstance();

		BlockHitResult result = ClientProxy.raycast(64.0);
		Vector3i pos = new Vector3i(result.getBlockPos());
		BlockSide side = BlockSide.values()[result.getDirection().ordinal()];
		Multiblock.findOrigin(mc.level, pos, side, null);

		BlockEntity be = mc.level.getBlockEntity(pos.toBlock());
		if (!(be instanceof ScreenBlockEntity)) return;

		ScreenBlockEntity te = (ScreenBlockEntity) be;

		if (te.hasUpgrade(side, DefaultUpgrade.LASERMOUSE)) { //hasUpgrade returns false if there's no screen on side 'side'
			int finalButton = button;
			te.interact(result, (hit) -> {
				ScreenData scr = te.getScreen(side);
				if (scr != null) {
					// Set dedup timestamp before local processing so server echo is suppressed
					scr.lastClickTime = System.currentTimeMillis();
				}

				te.handleMouseEvent(side, ClickControl.ControlType.MOVE, hit, -1);
				te.handleMouseEvent(side, press ? ClickControl.ControlType.DOWN : ClickControl.ControlType.UP, hit, finalButton);

				if (press)
					WDNetworkRegistry.sendToServer(C2SMessageScreenCtrl.laserDown(te, side, hit, finalButton));
				else
					WDNetworkRegistry.sendToServer(new C2SMessageScreenCtrl(te, side, new LaserControl(LaserControl.ControlType.UP, null, finalButton)));
			});
		}
	}

	public static boolean isOn() {
		return left || right || middle;
	}
}
