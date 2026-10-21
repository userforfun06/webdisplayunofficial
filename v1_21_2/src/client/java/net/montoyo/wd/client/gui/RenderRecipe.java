package net.montoyo.wd.client.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
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
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, CRAFTING_TABLE_GUI_TEXTURES);

        Lighting.setupForFlatItems();

        for (int sy = 0; sy < 3; sy++) {
            for (int sx = 0; sx < 3; sx++) {
                ItemStack is = recipe[sy * 3 + sx];

                if (is != null) {
                    int x = this.x + 30 + sx * 18;
                    int y = this.y + 17 + sy * 18;

                    context.renderItem(is, x, y);
                    context.renderItemDecorations(font, is, x, y);
                }
            }
        }

        if (recipeResult != null) {
            context.renderItem(recipeResult, x, y);
            context.renderItemDecorations(font, recipeResult, x, y);
        }

        Lighting.setupFor3DItems();
    }

    private void setRecipe(ShapedRecipe recipe) {
    }

    private void nextRecipe() {
        if (recipes.isEmpty())
            minecraft.setScreen(null);
        else {
            NameRecipePair pair = recipes.remove(0);
            setRecipe(pair.recipe);
            recipeName = pair.name;
        }
    }

    private void takeScreenshot() throws Throwable {
    }

    @Override
    public void tick() {
        if (recipeName != null) {
            try {
                takeScreenshot();
                nextRecipe();
            } catch (Throwable t) {
                t.printStackTrace();
                minecraft.setScreen(null);
            }
        }
    }

}
