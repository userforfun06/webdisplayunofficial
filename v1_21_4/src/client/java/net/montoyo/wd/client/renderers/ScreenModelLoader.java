package net.montoyo.wd.client.renderers;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import java.util.function.Function;

public class ScreenModelLoader {
    public static final ResourceLocation SCREEN_LOADER = ResourceLocation.fromNamespaceAndPath("webdisplays", "screen_loader");

    public static final ResourceLocation SCREEN_SIDE = ResourceLocation.fromNamespaceAndPath("webdisplays", "block/screen");

    static final ResourceLocation[] SIDES = new ResourceLocation[16];
    public static final Material[] MATERIALS_SIDES = new Material[16];
    
    static {
        var BLOCK_ATLAS = ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png");
        for (int i = 0; i < SIDES.length; i++) {
            SIDES[i] = ResourceLocation.fromNamespaceAndPath(SCREEN_SIDE.getNamespace(), SCREEN_SIDE.getPath() + i);
            MATERIALS_SIDES[i] = new Material(BLOCK_ATLAS, SIDES[i]);
        }
    }
    
    public static BakedModel createBakedModel(ModelState modelState, Function<Material, TextureAtlasSprite> spriteGetter, ItemTransforms transforms) {
        return new ScreenBaker(spriteGetter);
    }
}
