package net.montoyo.wd.client.renderers;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.resources.ResourceLocation;
import net.montoyo.wd.block.ScreenBlock;

public class ScreenModelLoadingPlugin implements ClientModInitializer, ModelLoadingPlugin {

    @Override
    public void onInitializeClient() {
        ModelLoadingPlugin.register(this);
    }

    @Override
    public void initialize(ModelLoadingPlugin.Context pluginContext) {
        pluginContext.addModels(ResourceLocation.fromNamespaceAndPath("webdisplays", "block/screen_builtin"));
        pluginContext.modifyBlockModelOnLoad().register((model, context) -> {
            if (context.state().getBlock() instanceof ScreenBlock) {
                return new ScreenBlockModel();
            }
            return model;
        });
    }
}
