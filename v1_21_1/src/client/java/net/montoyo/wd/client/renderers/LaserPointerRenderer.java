package net.montoyo.wd.client.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.GameRenderer;
import net.montoyo.wd.client.ClientProxy;
import net.montoyo.wd.registry.ItemRegistry;
import org.joml.Matrix4f;

import static com.mojang.math.Axis.*;

public final class LaserPointerRenderer implements IItemRenderer {

	public LaserPointerRenderer() {
	}

	public static boolean isOn() {
		if (Minecraft.getInstance().screen != null) return false;

		Minecraft mc = Minecraft.getInstance();
		return mc.player != null && mc.level != null &&
				(ClientProxy.mouseOn || net.montoyo.wd.item.ItemLaserPointerClient.isOn()) &&
                mc.player.getMainHandItem().getItem() == ItemRegistry.LASER_POINTER &&
				(mc.hitResult == null || mc.hitResult.getType() == HitResult.Type.BLOCK || mc.hitResult.getType() == HitResult.Type.MISS);
	}

	public void renderAfterVanilla(PoseStack poseStack, ItemStack is, float handSideSign, MultiBufferSource multiBufferSource, int packedLight, Matrix4f worldMatrix) {
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();

		RenderSystem.setShader(GameRenderer::getPositionColorShader);

		poseStack.pushPose();
		poseStack.translate(0.0, 0.2f, 0.0);
		poseStack.mulPose(XP.rotationDegrees(10.0f));
		poseStack.scale(1.0f / 16.0f, 1.0f / 16.0f, 1.0f / 16.0f);
		var matrix = poseStack.last().pose();

		Tesselator t = Tesselator.getInstance();
		BufferBuilder bb = t.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

		bb.addVertex(matrix, 0.0f, 0.0f, 0.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 1.0f, 0.0f, 0.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 1.0f, 0.0f, 4.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 0.0f, 0.0f, 4.0f).setColor(127, 127, 127, 255);

		bb.addVertex(matrix, 0.0f, 0.0f, 0.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 0.0f, -1.0f, 0.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 0.0f, -1.0f, 4.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 0.0f, 0.0f, 4.0f).setColor(127, 127, 127, 255);

		bb.addVertex(matrix, 1.0f, 0.0f, 0.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 1.0f, -1.0f, 0.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 1.0f, -1.0f, 4.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 1.0f, 0.0f, 4.0f).setColor(127, 127, 127, 255);

		bb.addVertex(matrix, 0.0f, -1.0f, 4.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 1.0f, -1.0f, 4.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 1.0f, 0.0f, 4.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 0.0f, 0.0f, 4.0f).setColor(127, 127, 127, 255);

		if (isOn()) drawLineBetween(bb, worldMatrix, matrix, new Vec3(0.5f, -0.5f, 0.5f), new Vec3(-40.0f, 4000.5f, -100.0f));

		MeshData meshData = bb.build();
		if (meshData != null) {
			BufferUploader.drawWithShader(meshData);
			meshData.close();
		}

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.depthFunc(515);
		RenderSystem.enableCull();
		poseStack.popPose();

		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
	}

	@Override
	public boolean render(PoseStack poseStack, ItemStack is, float handSideSign, float swingProgress, float equipProgress, MultiBufferSource multiBufferSource, int packedLight) {
		RenderSystem.disableCull();
		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();

		float PI = (float) Math.PI;

		float sqrtSwingProg = (float) Math.sqrt(swingProgress);
		float sinSqrtSwingProg1 = (float) Math.sin(sqrtSwingProg * PI);

		RenderSystem.setShader(GameRenderer::getPositionColorShader);

		var matrix0 = poseStack.last().pose();
		poseStack.pushPose();
		poseStack.translate((double)(handSideSign * -0.4f * sinSqrtSwingProg1), (double)(0.2f * Math.sin(sqrtSwingProg * PI * 2.0f)), (double)(-0.2f * Math.sin(swingProgress * PI)));
		poseStack.translate((double)(handSideSign * 0.56f), (double)(-0.52f - equipProgress * 0.6f), (double)(-0.72f));
		poseStack.mulPose(YP.rotationDegrees((float)(handSideSign * (45.0f - Math.sin(swingProgress * swingProgress * PI) * 20.0f))));
		poseStack.mulPose(ZP.rotationDegrees(handSideSign * sinSqrtSwingProg1 * -20.0f));
		poseStack.mulPose(XP.rotationDegrees(sinSqrtSwingProg1 * -80.0f));
		poseStack.mulPose(YP.rotationDegrees(handSideSign * -30.0f));
		poseStack.translate(0.0, 0.2f, 0.0);
		poseStack.mulPose(XP.rotationDegrees(10.0f));
		poseStack.scale(1.0f / 16.0f, 1.0f / 16.0f, 1.0f / 16.0f);
		var matrix = poseStack.last().pose();

		Tesselator t = Tesselator.getInstance();
		BufferBuilder bb = t.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

		bb.addVertex(matrix, 0.0f, 0.0f, 0.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 1.0f, 0.0f, 0.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 1.0f, 0.0f, 4.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 0.0f, 0.0f, 4.0f).setColor(127, 127, 127, 255);

		bb.addVertex(matrix, 0.0f, 0.0f, 0.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 0.0f, -1.0f, 0.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 0.0f, -1.0f, 4.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 0.0f, 0.0f, 4.0f).setColor(127, 127, 127, 255);

		bb.addVertex(matrix, 1.0f, 0.0f, 0.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 1.0f, -1.0f, 0.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 1.0f, -1.0f, 4.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 1.0f, 0.0f, 4.0f).setColor(127, 127, 127, 255);

		bb.addVertex(matrix, 0.0f, -1.0f, 4.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 1.0f, -1.0f, 4.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 1.0f, 0.0f, 4.0f).setColor(127, 127, 127, 255);
		bb.addVertex(matrix, 0.0f, 0.0f, 4.0f).setColor(127, 127, 127, 255);

		if (isOn()) drawLineBetween(bb, matrix0, matrix, new Vec3(0.5f, -0.5f, 0.5f), new Vec3(-40.0f, 4000.5f, -100.0f));

		MeshData meshData = bb.build();
		if (meshData != null) {
			BufferUploader.drawWithShader(meshData);
			meshData.close();
		}

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.enableCull();
		poseStack.popPose();

		RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

		return true;
	}

	private static void drawLineBetween(BufferBuilder bb, Matrix4f matrix0, Matrix4f matrix, Vec3 local, Vec3 target) {
		float distance = (float) local.distanceTo(target) / 2;
		float quarterWidth = 0.25f;
		float biggerWidth = 10;

		bb.addVertex(matrix, 0.25f, -0.25f, 0.5f).setColor(127, 0, 0, 255);
		bb.addVertex(matrix, quarterWidth + 0.25f, -0.25f, 0.5f).setColor(127, 0, 0, 255);
		bb.addVertex(matrix0, biggerWidth - 6f, 3f, -distance).setColor(127, 0, 0, 255);
		bb.addVertex(matrix0, -6f, 3f, -distance).setColor(127, 0, 0, 255);

		bb.addVertex(matrix, 0.25f, -0.25f, 0.5f).setColor(127, 0, 0, 255);
		bb.addVertex(matrix, 0.25f, -quarterWidth - 0.25f, 0.5f).setColor(127, 0, 0, 255);
		bb.addVertex(matrix0, -6f, -biggerWidth + 3f, -distance).setColor(127, 0, 0, 255);
		bb.addVertex(matrix0, -6f, 3f, -distance).setColor(127, 0, 0, 255);

		bb.addVertex(matrix, quarterWidth + 0.25f, -0.25f, 0.5f).setColor(127, 0, 0, 255);
		bb.addVertex(matrix, quarterWidth + 0.25f, -quarterWidth - 0.25f, 0.5f).setColor(127, 0, 0, 255);
		bb.addVertex(matrix0, biggerWidth - 6f, -biggerWidth + 3f, -distance).setColor(127, 0, 0, 255);
		bb.addVertex(matrix0, biggerWidth - 6f, 3f, -distance).setColor(127, 0, 0, 255);
	}
}
