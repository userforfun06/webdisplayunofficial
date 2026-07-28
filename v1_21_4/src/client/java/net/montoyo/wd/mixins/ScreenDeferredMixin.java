package net.montoyo.wd.mixins;

import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.DeltaTracker;
import net.montoyo.wd.client.renderers.ScreenRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class ScreenDeferredMixin {
    @Inject(at = @At("RETURN"), method = "renderLevel")
    private void onRenderLevelReturn(GraphicsResourceAllocator allocator, DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        ScreenRenderer.renderDeferred();
        ScreenRenderer.renderDeferredMinePad();
    }
}
