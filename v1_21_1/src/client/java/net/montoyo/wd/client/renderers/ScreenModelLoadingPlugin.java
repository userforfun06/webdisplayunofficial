package net.montoyo.wd.client.renderers;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.resources.model.ModelResourceLocation;

public class ScreenModelLoadingPlugin implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(pluginContext -> {
            pluginContext.modifyModelOnLoad().register((original, context) -> {
                ModelResourceLocation id = context.topLevelId();
                if (id != null &&
                    id.id().getNamespace().equals("webdisplays") &&
                    id.id().getPath().equals("screen")) {
                    return new ScreenUnbakedModel();
                }
                return original;
            });
        });
    }
}
