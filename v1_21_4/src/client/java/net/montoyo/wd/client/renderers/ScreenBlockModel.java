package net.montoyo.wd.client.renderers;

import net.minecraft.client.renderer.block.model.UnbakedBlockStateModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import java.util.function.Function;

public class ScreenBlockModel implements UnbakedBlockStateModel {

    @Override
    public void resolveDependencies(Resolver resolver) {
    }

    @Override
    public BakedModel bake(ModelBaker baker) {
        Function<Material, TextureAtlasSprite> spriteGetter = baker.sprites()::get;
        return new ScreenBaker(spriteGetter);
    }

    @Override
    public @Nullable Object visualEqualityGroup(@Nullable BlockState state) {
        return this;
    }
}
