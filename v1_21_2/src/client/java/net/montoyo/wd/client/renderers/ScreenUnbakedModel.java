package net.montoyo.wd.client.renderers;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

public class ScreenUnbakedModel implements UnbakedModel {

    @Override
    public void resolveDependencies(Resolver resolver) {
    }

    public Collection<ResourceLocation> getDependencies() {
        return Collections.emptyList();
    }

    public void resolveParents(Function<ResourceLocation, UnbakedModel> resolver) {
    }

    @Nullable
    @Override
    public BakedModel bake(@Nullable ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState) {
        return ScreenModelHelper.bake(baker, spriteGetter, modelState);
    }
}
