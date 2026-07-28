package net.montoyo.wd.client.renderers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.component.DataComponents;
import net.montoyo.wd.WebDisplaysMod;
import net.montoyo.wd.client.ClientProxy;
import net.montoyo.wd.config.ClientConfig;
import org.joml.Matrix4f;
import net.montoyo.wd.item.ItemMinePad2;

import static com.mojang.math.Axis.*;

public final class MinePadRenderer implements IItemRenderer {
	private static final float PI = (float) Math.PI;
	private final Minecraft mc = Minecraft.getInstance();
	private final ModelMinePad model = new ModelMinePad();
	private final ClientProxy clientProxy = (ClientProxy) WebDisplaysMod.PROXY;

	private float sinSqrtSwingProg1;
	private float sinSqrtSwingProg2;
	private float sinSwingProg1;
	private float sinSwingProg2;

	public static boolean renderAtSide(float handSideSign) {
		float relSide = handSideSign;
		if (Minecraft.getInstance().player.getMainArm() == HumanoidArm.LEFT) relSide *= -1;

		boolean sideHold = Minecraft.getInstance().player.isShiftKeyDown() != ClientConfig.sidePad;
		if (
				(relSide < 0 && Minecraft.getInstance().player.getMainHandItem().getItem() instanceof ItemMinePad2) ||
						(relSide > 0 && Minecraft.getInstance().player.getOffhandItem().getItem() instanceof ItemMinePad2)
		) sideHold = true;

		return sideHold;
	}

	@Override
	public final boolean render(PoseStack stack, ItemStack is, float handSideSign, float swingProgress, float equipProgress, MultiBufferSource multiBufferSource, int packedLight) {
		float sqrtSwingProg = (float) Math.sqrt(swingProgress);
		sinSqrtSwingProg1 = (float) Math.sin(sqrtSwingProg * PI);
		sinSqrtSwingProg2 = (float) Math.sin(sqrtSwingProg * PI * 2.0f);
		sinSwingProg1 = (float) Math.sin(swingProgress * PI);
		sinSwingProg2 = (float) Math.sin(swingProgress * swingProgress * PI);

		boolean sideHold = renderAtSide(handSideSign);

		// Render arm
		stack.pushPose();
		renderArmFirstPerson(stack, multiBufferSource, packedLight, equipProgress, handSideSign);
		stack.popPose();

		// Prepare minePad transform
		stack.pushPose();
		stack.translate((double)(handSideSign * -0.4f * sinSqrtSwingProg1), (double)(0.2f * sinSqrtSwingProg2), (double)(-0.2f * sinSwingProg1));
		stack.translate((double)(handSideSign * 0.56f), (double)(-0.52f - equipProgress * 0.6f), (double)(-0.72f));
		stack.mulPose(YP.rotationDegrees(handSideSign * (45.0f - sinSwingProg2 * 20.0f)));
		stack.mulPose(ZP.rotationDegrees(handSideSign * sinSqrtSwingProg1 * -20.0f));
		stack.mulPose(XP.rotationDegrees(sinSqrtSwingProg1 * -80.0f));
		stack.mulPose(YP.rotationDegrees(handSideSign * -45.0f));

		if (sideHold) {
			stack.translate(0.0, 0.0, (double)(-0.2f));
			stack.mulPose(YP.rotationDegrees(20.0f * -handSideSign));
			float total = 0.475f;
			float off = -0.025f;
			stack.translate((double)(-(total - off) + (off * handSideSign)), (double)(-0.1f), 0.0);
			stack.mulPose(ZP.rotationDegrees(1.0f));
		} else if (handSideSign >= 0)
			stack.translate(-1.065, 0.0, 0.0);
		else
			stack.translate(0.065, 0.0, 0.0);

		// Render model
		stack.translate(0.063, 0.28, 0.001);
		model.render(multiBufferSource, stack);
		stack.translate(-0.063, -0.28, -0.001);

		multiBufferSource.getBuffer(RenderType.LINES);

		// Render web view — dual approach:
		// 1. Deferred capture (for shader compatibility — drawn at renderLevel RETURN via ScreenDeferredMixin)
		// 2. Immediate Tesselator draw (for non-shader correctness — drawn on top of model right now)
		var customData = is.get(DataComponents.CUSTOM_DATA);
		if (customData != null && customData.copyTag().contains("PadID")) {
			ClientProxy.PadData pd = clientProxy.getPadByID(customData.copyTag().getUUID("PadID"));
			if (pd != null) {
				pd.ensureBrowser();
				if (pd.view != null && pd.view.getRenderer() != null) {
					int textureId = pd.view.getRenderer().getTextureID();

					float x1 = 0.0f;
					float y1 = 0.0f;
					float x2 = (float)(27.65 / 32.0 + 0.01);
					float y2 = (float)(14.0 / 32.0 + 0.002);

					// Capture for deferred draw (shader path)
					stack.translate(0.063, 0.28, 0.001);
					Matrix4f matrix = new Matrix4f(stack.last().pose());
					Matrix4f view = new Matrix4f(RenderSystem.getModelViewStack());
					Matrix4f proj = new Matrix4f(RenderSystem.getProjectionMatrix());
					ScreenRenderer.addDeferredMinePad(textureId, matrix, view, proj, x1, y1, x2, y2);
					stack.translate(-0.063, -0.28, -0.001);

					// Immediate Tesselator draw (non-shader path)
					stack.translate(0.063, 0.28, 0.001);
					RenderSystem.disableDepthTest();
					RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
					RenderSystem.setShaderTexture(0, textureId);
		Tesselator tess = Tesselator.getInstance();
		BufferBuilder buf = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		buf.addVertex(matrix, x1, y1, 0.0f).setUv(0.0f, 1.0f).setColor(255, 255, 255, 255);
		buf.addVertex(matrix, x2, y1, 0.0f).setUv(1.0f, 1.0f).setColor(255, 255, 255, 255);
		buf.addVertex(matrix, x2, y2, 0.0f).setUv(1.0f, 0.0f).setColor(255, 255, 255, 255);
		buf.addVertex(matrix, x1, y2, 0.0f).setUv(0.0f, 0.0f).setColor(255, 255, 255, 255);
					MeshData builtData = buf.build();
					if (builtData != null) {
						BufferUploader.drawWithShader(builtData);
						builtData.close();
					}
					RenderSystem.enableDepthTest();
					stack.translate(-0.063, -0.28, -0.001);
				}
			}
		}

		stack.popPose();
		RenderSystem.enableCull();

		return true;
	}

	private void renderArmFirstPerson(PoseStack stack, MultiBufferSource buffer, int combinedLight, float equipProgress, float handSideSign) {
		float tx = -0.3f * sinSqrtSwingProg1;
		float ty = 0.4f * sinSqrtSwingProg2;
		float tz = -0.4f * sinSwingProg1;

		stack.translate((double)(handSideSign * (tx + 0.64000005f)), (double)(ty - 0.6f - equipProgress * 0.6f), (double)(tz - 0.71999997f));
		stack.mulPose(YP.rotationDegrees(handSideSign * 45.0f));
		stack.mulPose(YP.rotationDegrees(handSideSign * sinSqrtSwingProg1 * 70.0f));
		stack.mulPose(ZP.rotationDegrees(handSideSign * sinSwingProg2 * -20.0f));
		stack.translate((double)(-handSideSign), 3.6, 3.5);
		stack.mulPose(ZP.rotationDegrees(handSideSign * 120.0f));
		stack.mulPose(XP.rotationDegrees(200.0f));
		stack.mulPose(YP.rotationDegrees(handSideSign * -135.0f));
		stack.translate((double)(handSideSign * 5.6f), 0.0, 0.0);

		PlayerRenderer playerRenderer = (PlayerRenderer) mc.getEntityRenderDispatcher().getRenderer(mc.player);
		RenderSystem.setShaderTexture(0, mc.getSkinManager().getInsecureSkin(mc.player.getGameProfile()).texture());

		if (handSideSign >= 0.0f)
			playerRenderer.renderRightHand(stack, buffer, combinedLight, mc.player);
		else
			playerRenderer.renderLeftHand(stack, buffer, combinedLight, mc.player);
	}
}
