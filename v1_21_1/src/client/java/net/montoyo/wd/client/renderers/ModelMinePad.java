package net.montoyo.wd.client.renderers;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

public final class ModelMinePad {
	public void render(MultiBufferSource buffers, PoseStack stack) {
		double x1 = 0.0;
		double y1 = 0.0;
		double x2 = 27.65 / 32.0 + 0.01;
		double y2 = 14.0 / 32.0 + 0.002;

		Matrix4f positionMatrix = stack.last().pose();

		int width = 32;
		int height = 32;

		float padding = 1f / 23;
		float padding1 = 1f / 21;
		float z = 0;

		VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(
			ResourceLocation.fromNamespaceAndPath("webdisplays", "textures/item/model/minepad_item.png")
		));

		consumer.addVertex(positionMatrix, (float) x1, (float) y1 - padding, z).setColor(255, 255, 255, 255).setUv(1f / width, 12f / height).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0f, 0.0f, 1.0f);
		consumer.addVertex(positionMatrix, (float) x2, (float) y1 - padding, z).setColor(255, 255, 255, 255).setUv(19f / width, 12f / height).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0f, 0.0f, 1.0f);
		consumer.addVertex(positionMatrix, (float) x2, (float) y2 + padding, z).setColor(255, 255, 255, 255).setUv(19f / width, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0f, 0.0f, 1.0f);
		consumer.addVertex(positionMatrix, (float) x1, (float) y2 + padding, z).setColor(255, 255, 255, 255).setUv(1f / width, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0f, 0.0f, 1.0f);

		consumer.addVertex(positionMatrix, (float) x1 - padding1, (float) y1, z).setColor(255, 255, 255, 255).setUv(0, 10f / height).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0f, 0.0f, 1.0f);
		consumer.addVertex(positionMatrix, (float) x2 + padding1, (float) y1, z).setColor(255, 255, 255, 255).setUv(20f / width, 10f / height).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0f, 0.0f, 1.0f);
		consumer.addVertex(positionMatrix, (float) x2 + padding1, (float) y2, z).setColor(255, 255, 255, 255).setUv(20f / width, 1f / height).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0f, 0.0f, 1.0f);
		consumer.addVertex(positionMatrix, (float) x1 - padding1, (float) y2, z).setColor(255, 255, 255, 255).setUv(0, 1f / height).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0.0f, 0.0f, 1.0f);
	}
}
