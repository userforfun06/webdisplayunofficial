package net.montoyo.wd.client.renderers;

import java.util.function.Function;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.world.inventory.InventoryMenu;

public class ScreenModelHelper {
    
    public static final ResourceLocation SCREEN_SIDE = ResourceLocation.fromNamespaceAndPath("webdisplays", "block/screen");
    
    private static final ResourceLocation[] SIDES = new ResourceLocation[16];
    public static final Material[] MATERIALS_SIDES = new Material[16];
    
    static {
        for (int i = 0; i < SIDES.length; i++) {
            SIDES[i] = ResourceLocation.fromNamespaceAndPath(SCREEN_SIDE.getNamespace(), SCREEN_SIDE.getPath() + i);
            MATERIALS_SIDES[i] = new Material(InventoryMenu.BLOCK_ATLAS, SIDES[i]);
        }
    }
    
    public static net.minecraft.client.resources.model.BakedModel bake(ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState) {
        return new ScreenBaker(spriteGetter);
    }
}
