/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.renderers;

import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.core.BlockPos;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.CoreShaders;
import com.mojang.blaze3d.ProjectionType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.utilities.Multiblock;
import net.montoyo.wd.utilities.math.Vector3f;
import net.montoyo.wd.utilities.math.Vector3i;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;

import static com.mojang.math.Axis.*;

public class ScreenRenderer implements BlockEntityRenderer<ScreenBlockEntity> {

	// Deferred browser creation: create at most 1 browser per render frame
	// to prevent render-thread freeze when many screens exist.
	private static final HashSet<ScreenData> PENDING_CREATION = new HashSet<>();
	private static final Deque<Runnable> BROWSER_QUEUE = new ArrayDeque<>();

	private record DeferredScreen(int texId, Matrix4f matrix, Matrix4f view, Matrix4f proj, float sw, float sh) {}
	private static final List<DeferredScreen> DEFERRED = new ArrayList<>();
	private static final List<DeferredScreen> DRAW = new ArrayList<>();

	private record DeferredMinePad(int texId, Matrix4f matrix, Matrix4f view, Matrix4f proj, float x1, float y1, float x2, float y2) {}
	private static final List<DeferredMinePad> DEFERRED_MINE = new ArrayList<>();
	private static final List<DeferredMinePad> DRAW_MINE = new ArrayList<>();

	public ScreenRenderer() {
	}

	public static void flushBrowserQueue() {
		if (!BROWSER_QUEUE.isEmpty()) {
			Runnable task = BROWSER_QUEUE.poll();
			task.run();
		}
	}

	public static void queueBrowserCreation(Runnable task) {
		BROWSER_QUEUE.add(task);
	}

	public static class ScreenRendererProvider implements BlockEntityRendererProvider<ScreenBlockEntity> {
		@Override
		public @NotNull BlockEntityRenderer<ScreenBlockEntity> create(@NotNull Context arg) {
			return new ScreenRenderer();
		}
	}
	
	private final Vector3f mid = new Vector3f();
	private final Vector3i tmpi = new Vector3i();
	private final Vector3f tmpf = new Vector3f();
	
	@Override
	public void render(ScreenBlockEntity te, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
		if (!te.isLoaded())
			return;
		
		// Client-side validation: check if screen structure is still valid
		// This detects when blocks are broken and stops rendering immediately
		te.validateStructureClient();
		
		// Re-check if still loaded after validation
		if (!te.isLoaded() || te.screenCount() == 0)
			return;

		//Disable lighting
//		RenderSystem.enableTexture();
//      RenderSystem.disableCull();
		RenderSystem.disableBlend();
		
		for (int i = 0; i < te.screenCount(); i++) {
			ScreenData scr = te.getScreen(i);
			
			// Only render from the origin block to prevent overlapping in multiblock screens
			// The origin is the bottom-left corner of the screen
			tmpi.set(te.getBlockPos().getX(), te.getBlockPos().getY(), te.getBlockPos().getZ());
			Multiblock.findOrigin(te.getLevel(), tmpi, scr.side, Multiblock.NULL_OVERRIDE);
			BlockPos origin = new BlockPos(tmpi.x, tmpi.y, tmpi.z);
			if (!te.getBlockPos().equals(origin)) {
				continue; // This block is not the origin, skip rendering
			}
			
			if (scr.browser == null) {
				// Deferred browser creation: queue it (Set guards against duplicate queuing)
				// flushBrowserQueue() at top of next render call will create it (1 per frame)
				if (PENDING_CREATION.add(scr)) {
					ScreenBlockEntity captureTe = te;
					BROWSER_QUEUE.add(() -> {
						captureTe.getScreen(scr.side).createBrowser(captureTe, true, captureTe.getLevel());
						PENDING_CREATION.remove(scr);
					});
				}
				continue;
			}
			
			tmpi.set(scr.side.right);
			tmpi.mul(scr.size.x);
			tmpi.addMul(scr.side.up, scr.size.y);
			tmpf.set(tmpi);
			mid.set(0.5, 0.5, 0.5);
			mid.addMul(tmpf, 0.5f);
			tmpf.set(scr.side.left);
			mid.addMul(tmpf, 0.5f);
			tmpf.set(scr.side.down);
			mid.addMul(tmpf, 0.5f);
			
			if (!(scr.browser instanceof MCEFBrowser mcefBrowser))
				continue;
			if (mcefBrowser.getRenderer() == null)
				continue;
			
			int textureId = mcefBrowser.getRenderer().getTextureID();
			if (textureId <= 0)
				continue;

			poseStack.pushPose();
			poseStack.translate(mid.x, mid.y, mid.z);
			
			switch (scr.side) {
				case BOTTOM:
					poseStack.mulPose(XP.rotation(90.f + 49.8f));
					break;
				
				case TOP:
					poseStack.mulPose(XN.rotation(90.f + 49.8f));
					break;
				
				case NORTH:
					poseStack.mulPose(YN.rotationDegrees(180.f));
					break;
				
				case SOUTH:
					break;
				
				case WEST:
					poseStack.mulPose(YN.rotationDegrees(90.f));
					break;
				
				case EAST:
					poseStack.mulPose(YP.rotationDegrees(90.f));
					break;
			}
			
			if (scr.doTurnOnAnim) {
				long lt = System.currentTimeMillis() - scr.turnOnTime;
				float ft = ((float) lt) / 100.0f;
				
				if (ft >= 1.0f) {
					ft = 1.0f;
					scr.doTurnOnAnim = false;
				}
				
				poseStack.scale(ft, ft, 1.0f);
			}
			
			if (!scr.rotation.isNull)
				poseStack.mulPose(ZP.rotationDegrees(scr.rotation.angle));
			
			// Bezel inset (1/16 per edge, matching hit2pixels)
			float bezel = 1.0f / 16.0f;
			float sw = ((float) scr.size.x) * 0.5f - bezel;
			float sh = ((float) scr.size.y) * 0.5f - bezel;

			if (scr.rotation.isVertical) {
				float tmp = sw;
				sw = sh;
				sh = tmp;
			}

			// Store data for deferred color draw at GameRenderer RETURN
			Matrix4f matrix = new Matrix4f(poseStack.last().pose());
			Matrix4f view = new Matrix4f(RenderSystem.getModelViewStack());
			Matrix4f proj = new Matrix4f(RenderSystem.getProjectionMatrix());
			synchronized (DEFERRED) {
				DEFERRED.add(new DeferredScreen(textureId, matrix, view, proj, sw, sh));
			}

			poseStack.popPose();

//			// debug hit2pixels
//			HitResult result = Minecraft.getInstance().hitResult;
//			VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
//			poseStack.translate(-sw, -sh, 0);
//			if (result instanceof BlockHitResult hit) {
//				BlockPos bpos = hit.getBlockPos();
//
//				Vector3i pos = new Vector3i(hit.getBlockPos());
//				float hitX = ((float) result.getLocation().x) - (float) te.getBlockPos().getX();
//				float hitY = ((float) result.getLocation().y) - (float) te.getBlockPos().getY();
//				float hitZ = ((float) result.getLocation().z) - (float) te.getBlockPos().getZ();
//				Vector2i tmp = new Vector2i();
//
//				if (BlockScreen.hit2pixels(scr.side, bpos, pos, scr, hitX, hitY, hitZ, tmp)) {
//					float x = tmp.x / (float) scr.resolution.x * scr.size.x;
//					float y = tmp.y / (float) scr.resolution.y * scr.size.y;
//					y = scr.size.y - y;
//
//					x /= scr.size.x;
//					y /= scr.size.y;
//					x *= sw * 2;
//					y *= sh * 2;
//
//					LevelRenderer.renderLineBox(
//							poseStack,
//							consumer, new AABB(
//									x - 0.01, y - 0.01, 0.5 - 0.01,
//									x + 0.01, y + 0.01, 0.5 + 0.01
//							),
//							1f, 0, 0, 1f
//					);
//				}
//			}
		}


//        //Bounding box debugging
//        poseStack.pushPose();
//        poseStack.translate(-te.getBlockPos().getX(), -te.getBlockPos().getY(), -te.getBlockPos().getZ());
//        LevelRenderer.renderLineBox(
//                poseStack, bufferSource.getBuffer(RenderType.LINES),
//                te.getRenderBoundingBox(), 1, 1, 1, 1f
//        );
//        poseStack.popPose();
		
		RenderSystem.enableBlend();
	}

	public static void renderDeferred() {
		flushBrowserQueue();
		synchronized (DEFERRED) {
			if (DEFERRED.isEmpty())
				return;
			DRAW.clear();
			DRAW.addAll(DEFERRED);
			DEFERRED.clear();
		}

		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(515); // GL_LEQUAL

		for (DeferredScreen ds : DRAW) {
			Matrix4fStack mvStack = RenderSystem.getModelViewStack();
			mvStack.pushMatrix();
			try {
				mvStack.set(ds.view);
				// Model view stack is managed via Matrix4fStack directly

				RenderSystem.backupProjectionMatrix();
				RenderSystem.setProjectionMatrix(ds.proj, ProjectionType.ORTHOGRAPHIC);

				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
				RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);

				RenderSystem.enablePolygonOffset();
				RenderSystem.polygonOffset(-0.5f, -2.0f);

				Tesselator tess = Tesselator.getInstance();
				RenderSystem.setShaderTexture(0, ds.texId);
				BufferBuilder bb = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
				bb.addVertex(ds.matrix, -ds.sw, -ds.sh, 0.505f).setUv(0.0f, 1.0f).setColor(255, 255, 255, 255);
				bb.addVertex(ds.matrix, ds.sw, -ds.sh, 0.505f).setUv(1.0f, 1.0f).setColor(255, 255, 255, 255);
				bb.addVertex(ds.matrix, ds.sw, ds.sh, 0.505f).setUv(1.0f, 0.0f).setColor(255, 255, 255, 255);
				bb.addVertex(ds.matrix, -ds.sw, ds.sh, 0.505f).setUv(0.0f, 0.0f).setColor(255, 255, 255, 255);
				BufferUploader.drawWithShader(bb.build());

				RenderSystem.disablePolygonOffset();

				RenderSystem.restoreProjectionMatrix();
			} catch (Exception e) {
				WebDisplays.LOGGER.error("ScreenRenderer deferred render error: {}", e.getMessage());
			} finally {
				mvStack.popMatrix();
			}
		}

		RenderSystem.depthFunc(515);
	}

	public static void addDeferredMinePad(int texId, Matrix4f matrix, Matrix4f view, Matrix4f proj, float x1, float y1, float x2, float y2) {
		synchronized (DEFERRED_MINE) {
			DEFERRED_MINE.add(new DeferredMinePad(texId, matrix, view, proj, x1, y1, x2, y2));
		}
	}

	public static void renderDeferredMinePad() {
		synchronized (DEFERRED_MINE) {
			if (DEFERRED_MINE.isEmpty())
				return;
			DRAW_MINE.clear();
			DRAW_MINE.addAll(DEFERRED_MINE);
			DEFERRED_MINE.clear();
		}

		RenderSystem.disableDepthTest();

		for (DeferredMinePad dm : DRAW_MINE) {
			Matrix4fStack mvStack = RenderSystem.getModelViewStack();
			mvStack.pushMatrix();
			try {
				mvStack.set(dm.view);

				RenderSystem.backupProjectionMatrix();
				RenderSystem.setProjectionMatrix(dm.proj, ProjectionType.ORTHOGRAPHIC);

				RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
				RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
				RenderSystem.setShaderTexture(0, dm.texId);

				Tesselator tess = Tesselator.getInstance();

				BufferBuilder bb = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
				bb.addVertex(dm.matrix, dm.x1, dm.y1, 0.0f).setUv(0.0f, 1.0f).setColor(255, 255, 255, 255);
				bb.addVertex(dm.matrix, dm.x2, dm.y1, 0.0f).setUv(1.0f, 1.0f).setColor(255, 255, 255, 255);
				bb.addVertex(dm.matrix, dm.x2, dm.y2, 0.0f).setUv(1.0f, 0.0f).setColor(255, 255, 255, 255);
				bb.addVertex(dm.matrix, dm.x1, dm.y2, 0.0f).setUv(0.0f, 0.0f).setColor(255, 255, 255, 255);
				BufferUploader.drawWithShader(bb.build());

				RenderSystem.restoreProjectionMatrix();
			} catch (Exception e) {
				WebDisplays.LOGGER.error("ScreenRenderer deferred MinePad render error: {}", e.getMessage());
			} finally {
				mvStack.popMatrix();
				// Model view stack is managed via Matrix4fStack directly
			}
		}

		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(515);
	}
}
