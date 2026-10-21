package net.montoyo.wd.mixins;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.client.Camera;
import net.montoyo.wd.client.gui.camera.KeyboardCamera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public class CameraMixin {

    @Inject(at = @At("RETURN"), method = "setup")
    private void onSetup(BlockGetter level, Entity entity, boolean detached, boolean thirdPerson, float partialTick, CallbackInfo ci) {
        float[] angle = KeyboardCamera.getCameraOverride(partialTick);
        if (angle != null) {
            ((CameraAccessor)(Camera)(Object)this).invokeSetRotation(angle[1], angle[0]);
        }
    }
}
