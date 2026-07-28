package net.montoyo.wd.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

@Mixin(Screen.class)
public interface ScreenAccessor {
    @Accessor("renderables")
    List<Renderable> getRenderables();

    @Accessor("children")
    List<GuiEventListener> getChildren();

    @Invoker("addRenderableWidget")
    <T extends GuiEventListener & Renderable> T invokeAddRenderableWidget(T widget);
}
