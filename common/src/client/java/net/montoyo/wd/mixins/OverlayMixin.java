package net.montoyo.wd.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.montoyo.wd.client.ClientProxy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class OverlayMixin {
    @Shadow
    @Final
    protected Minecraft minecraft;

    @Inject(at = @At("HEAD"), method = "renderCrosshair", cancellable = true)
    public void preDrawCrosshair(GuiGraphics pGuiGraphics, DeltaTracker pDeltaTracker, CallbackInfo ci) {
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        ClientProxy.renderCrosshair(minecraft.options, screenWidth, screenHeight, 0, pGuiGraphics, ci);
    }
}
