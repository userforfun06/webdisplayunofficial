package net.montoyo.wd.client.renderers;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.*;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class ScreenUnbakedModel implements UnbakedModel {

    private static final BlockModel TEXTURE_HOLDER = createTextureHolder();

    private static BlockModel createTextureHolder() {
        var builder = new TextureSlots.Data.Builder();
        for (int i = 0; i < 16; i++) {
            builder.addReference("screen" + i, "webdisplays:block/screen" + i);
        }
        return new BlockModel(
            null,
            List.of(),
            builder.build(),
            false,
            UnbakedModel.GuiLight.FRONT,
            ItemTransforms.NO_TRANSFORMS
        );
    }

    @Override
    public void resolveDependencies(Resolver resolver) {
    }

    @Nullable
    @Override
    public BakedModel bake(TextureSlots textureSlots, ModelBaker baker, ModelState state, boolean bl, boolean bl2, ItemTransforms itemTransforms) {
        SpriteGetter spriteGetter = baker.sprites();
        return ScreenModelHelper.bake(baker, spriteGetter::get, state);
    }

    @Override
    public TextureSlots.Data getTextureSlots() {
        return TEXTURE_HOLDER.getTextureSlots();
    }
}
