package net.montoyo.wd.client.renderers;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

public class ScreenUnbakedModel implements UnbakedModel {
    
    @Override
    public Collection<ResourceLocation> getDependencies() {
        return Collections.emptyList();
    }
    
    @Override
    public void resolveParents(Function<ResourceLocation, UnbakedModel> resolver) {
    }
    
    @Nullable
    @Override
    public BakedModel bake(@Nullable ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState) {
        System.out.println("ScreenUnbakedModel.bake called");
        return ScreenModelHelper.bake(baker, spriteGetter, modelState, ItemOverrides.EMPTY);
    }
}
