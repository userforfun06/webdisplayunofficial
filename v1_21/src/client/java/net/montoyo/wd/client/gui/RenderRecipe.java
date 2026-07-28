/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.montoyo.wd.utilities.Log;
import java.util.ArrayList;




public class RenderRecipe extends Screen {
    public RenderRecipe() {
        super(Component.literal(""));
    }

    private static class NameRecipePair {

        private final String name;
        private final ShapedRecipe recipe;

        private NameRecipePair(String n, ShapedRecipe r) {
            this.name = n;
            this.recipe = r;
        }

    }

    private static final ResourceLocation CRAFTING_TABLE_GUI_TEXTURES = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/crafting_table.png");
    private static final int SIZE_X = 176;
    private static final int SIZE_Y = 166;
    private int x;
    private int y;
    private final ItemStack[] recipe = new ItemStack[3 * 3];
    private ItemStack recipeResult;
    private String recipeName;
    private final ArrayList<NameRecipePair> recipes = new ArrayList<>();


    @Override
    public void init() {
        x = (width - SIZE_X) / 2;
        y = (height - SIZE_Y) / 2;
        Log.info("Loaded %d recipes", recipes.size());
        nextRecipe();
    }
    
    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float partialTick) {
        this.setInitialFocus();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, CRAFTING_TABLE_GUI_TEXTURES);
//        context.blit(x, y, 0, 0, SIZE_X, SIZE_Y);
//        font.draw(poseStack, I18n.get("container.crafting"), x + 28, y + 6, 0x404040);

        Lighting.setupForFlatItems();
//        RenderSystem.disableLighting();

        for(int sy = 0; sy < 3; sy++) {
            for(int sx = 0; sx < 3; sx++) {
                ItemStack is = recipe[sy * 3 + sx];

                if(is != null) {
                    int x = this.x + 30 + sx * 18;
                    int y = this.y + 17 + sy * 18;

                    context.renderItem(is, x, y);
                }
            }
        }

        if(recipeResult != null) {
            context.renderItem(recipeResult, x, y);
        }

//        GlStateManager.enableLighting();
        Lighting.setupFor3DItems();
    }

    private void setRecipe(ShapedRecipe recipe) {
        // for(int y = 0; y < recipe.getRecipeHeight(); y++) {
        //     for(int x = 0; x < recipe.getRecipeWidth(); x++) {
        //         ItemStack[] stacks = recipe.getIngredients().get(pos++).getItems();
        //
        //         if(stacks.length > 0)
        //             this.recipe[y * 3 + x] = stacks[0];
        //     }
        // }

//        recipeResult = recipe.getResultItem();
    }

    private void nextRecipe() {
        if(recipes.isEmpty())
            minecraft.setScreen(null);
        else {
            NameRecipePair pair = recipes.remove(0);
            setRecipe(pair.recipe);
            recipeName = pair.name;
        }
    }

    private void takeScreenshot() throws Throwable {
        /*
        int x = screen2DisplayX(this.x + 27);
        int y = minecraft.getWindow().getHeight() - screen2DisplayY(this.y + 4);
        int w = screen2DisplayX(120);
        int h = screen2DisplayY(68);
        y -= h;

        if(buffer == null)
            buffer = BufferUtils.createByteBuffer(w * h);

        int oldPack = glGetInteger(GL_PACK_ALIGNMENT);
        RenderSystem.pixelStore(GL_PACK_ALIGNMENT, 1);
        buffer.clear();
        RenderSystem.readPixels(x, y, w, h, EXTBGRA.GL_BGRA_EXT, GL_UNSIGNED_BYTE, buffer);
        RenderSystem.pixelStore(GL_PACK_ALIGNMENT, oldPack);

        if(array == null)
            array = new int[w * h];

        buffer.clear();
        buffer.asIntBuffer().get(array);
        TextureUtil.processPixelValues(array, w, h);

        File f = WebDisplaysDirs.getSubDir("wd_recipes");
        f = new File(f, recipeName + ".png");

        BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        bi.setRGB(0, 0, w, h, array, 0, w);
        ImageIO.write(bi, "PNG", f);
        */
    }

    @Override
    public void tick() {
        if(recipeName != null) {
            try {
                takeScreenshot();
                nextRecipe();
            } catch(Throwable t) {
                t.printStackTrace();
                minecraft.setScreen(null);
            }
        }
    }

}
