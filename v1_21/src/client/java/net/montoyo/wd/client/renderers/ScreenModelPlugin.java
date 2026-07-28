package net.montoyo.wd.client.renderers;

import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.resources.ResourceLocation;

public class ScreenModelPlugin implements ModelLoadingPlugin {

    public static final ResourceLocation SCREEN_MODEL_ID =
        ResourceLocation.fromNamespaceAndPath("webdisplays", "screen");

    @Override
    public void onInitializeModelLoader(Context ctx) {
        ctx.resolveModel().register(context -> {
            if (context.id().equals(SCREEN_MODEL_ID)) {
                return new ScreenUnbakedModel();
            }
            return null;
        });
    }
}
