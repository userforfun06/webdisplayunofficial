package net.montoyo.wd.mixins;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.montoyo.wd.WebDisplaysMod;
import net.montoyo.wd.client.ClientProxy;
import net.montoyo.wd.client.renderers.IItemRenderer;
import net.montoyo.wd.client.renderers.LaserPointerRenderer;
import net.montoyo.wd.registry.ItemRegistry;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
	private static Matrix4f wd$worldMatrix = null;

	@Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
	private void onRenderArmWithItem(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swingProgress, ItemStack stack, float equipProgress, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, CallbackInfo ci) {
		IItemRenderer renderer;

		if (stack.getItem() == ItemRegistry.MINEPAD) {
			renderer = ((ClientProxy) WebDisplaysMod.PROXY).getMinePadRenderer();
		} else {
			if (stack.getItem() == ItemRegistry.LASER_POINTER) {
				wd$worldMatrix = new Matrix4f(poseStack.last().pose());
				HumanoidArm arm = player.getMainArm();
				if (hand == InteractionHand.OFF_HAND) {
					arm = arm.getOpposite();
				}
			}
			return;
		}

		if (renderer == null) return;

		HumanoidArm arm = player.getMainArm();
		if (hand == InteractionHand.OFF_HAND) {
			arm = arm.getOpposite();
		}
		float sign = (arm == HumanoidArm.RIGHT) ? 1.0f : -1.0f;

		if (renderer.render(poseStack, stack, sign, swingProgress, equipProgress, bufferSource, combinedLight)) {
			ci.cancel();
		}
	}

	@Inject(method = "renderItem", at = @At("HEAD"), cancellable = true)
	private void onRenderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext context, boolean leftHand, PoseStack poseStack, MultiBufferSource buffer, int light, CallbackInfo ci) {
		if (stack.getItem() == ItemRegistry.LASER_POINTER) {
			LaserPointerRenderer renderer = (LaserPointerRenderer) ((ClientProxy) WebDisplaysMod.PROXY).getLaserPointerRenderer();
			if (renderer != null) {
				float sign = leftHand ? -1.0f : 1.0f;
				renderer.renderAfterVanilla(poseStack, stack, sign, buffer, light, wd$worldMatrix);
				wd$worldMatrix = null;
				ci.cancel();
			}
		}
	}
}
