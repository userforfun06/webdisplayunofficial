/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.renderers;

import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

public interface IItemRenderer {
	
	/**
	 * @param pose the pose stack
	 * @param stack the item stack
	 * @param handSideSign
	 * @param swingProgress
	 * @param equipProgress
	 * @param multiBufferSource the buffer source
	 * @param packedLight packed light
	 * @return whether or not to cancel vanilla rendering
	 */
	boolean render(PoseStack pose, ItemStack stack, float handSideSign, float swingProgress, float equipProgress, MultiBufferSource multiBufferSource, int packedLight);
	
}
