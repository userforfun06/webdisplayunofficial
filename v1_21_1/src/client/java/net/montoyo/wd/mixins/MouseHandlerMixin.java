package net.montoyo.wd.mixins;

import net.minecraft.world.phys.HitResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.montoyo.wd.registry.ItemRegistry;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(at = @At("HEAD"), method = "onPress", cancellable = true)
    public void prePress(long p_91531_, int p_91532_, int p_91533_, int p_91534_, CallbackInfo ci) {
        boolean flag = p_91533_ == 1;

        if (Minecraft.getInstance().screen == null) {
                if (
                        minecraft.player != null && minecraft.level != null &&
                                minecraft.player.getMainHandItem().getItem().equals(ItemRegistry.LASER_POINTER) &&
                                (minecraft.hitResult == null || minecraft.hitResult.getType() == HitResult.Type.BLOCK || minecraft.hitResult.getType() == HitResult.Type.MISS)
                ) {
                net.montoyo.wd.item.ItemLaserPointerClient.press(flag, p_91532_);
                ci.cancel();
            }
        }
    }
}
