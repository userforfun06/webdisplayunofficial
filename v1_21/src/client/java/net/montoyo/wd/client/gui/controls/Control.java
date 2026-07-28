/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.gui.controls;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.resources.language.I18n;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.montoyo.wd.client.gui.WDScreen;
import net.montoyo.wd.client.gui.loading.JsonOWrapper;
import net.montoyo.wd.utilities.data.Bounds;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

import static com.mojang.math.Axis.XP;


public abstract class Control {

    public static final int COLOR_BLACK    = 0xFF000000;
    public static final int COLOR_WHITE    = 0xFFFFFFFF;
    public static final int COLOR_RED      = 0xFFFF0000;
    public static final int COLOR_GREEN    = 0xFF00FF00;
    public static final int COLOR_BLUE     = 0xFF0000FF;
    public static final int COLOR_CYAN     = 0xFF00FFFF;
    public static final int COLOR_MANGENTA = 0xFFFF00FF;
    public static final int COLOR_YELLOW   = 0xFFFFFF00;

    protected final Minecraft mc;
    protected final Font font;
    protected final Tesselator tessellator;
    protected final BufferBuilder vBuffer;
    protected static WDScreen parent;
    protected String name;
    protected Object userdata;

    public Control() {
        mc = Minecraft.getInstance();
        font = mc.font;
        tessellator = Tesselator.getInstance();
        vBuffer = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        parent = WDScreen.CURRENT_SCREEN;
    }

    public Object getUserdata() {
        return userdata;
    }

    public void setUserdata(Object userdata) {
        this.userdata = userdata;
    }

    public boolean keyTyped(int keyCode, int modifier) {
        return false;
    }

    public boolean keyUp(int key, int scanCode, int modifiers) {
        return false;
    }

    public boolean keyDown(int key, int scanCode, int modifiers) {
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        return false;
    }

    public void unfocus() {
    }

    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        return false;
    }

    public boolean mouseClickMove(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    public boolean mouseMove(double mouseX, double mouseY) {
        return false;
    }

    public boolean mouseScroll(double mouseX, double mouseY, double amount) {
        return false;
    }

    public void draw(GuiGraphics poseStack, int mouseX, int mouseY, float ptt) {
    }

    public void postDraw(GuiGraphics poseStack, int mouseX, int mouseY, float ptt) {
    }

    public void destroy() {
    }

    public void setDisabled(boolean disabled) {
    }

    public WDScreen getParent() {
        return parent;
    }

    public abstract int getX();
    public abstract int getY();
    public abstract int getWidth();
    public abstract int getHeight();
    public abstract void setPos(int x, int y);

    public void fillRect(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + h, color);
    }

    public void fillTexturedRect(GuiGraphics graphics, int x, int y, int w, int h, double u1, double v1, double u2, double v2) {
        float x1 = (float) x;
        float y1 = (float) y;
        float x2 = (float) (x + w);
        float y2 = (float) (y + h);

        Matrix4f matrix = graphics.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

        Tesselator tess = Tesselator.getInstance();
        BufferBuilder bb = tess.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bb.addVertex(matrix, x1, y2, 0.0f).setUv((float) u1, (float) v2).setColor(255, 255, 255, 255);
        bb.addVertex(matrix, x2, y2, 0.0f).setUv((float) u2, (float) v2).setColor(255, 255, 255, 255);
        bb.addVertex(matrix, x2, y1, 0.0f).setUv((float) u2, (float) v1).setColor(255, 255, 255, 255);
        bb.addVertex(matrix, x1, y1, 0.0f).setUv((float) u1, (float) v1).setColor(255, 255, 255, 255);
        BufferUploader.drawWithShader(bb.build());
    }

    public static void blend(boolean enable) {
        if(enable) {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_CONSTANT_ALPHA);
        } else
            RenderSystem.disableBlend();
    }

    public void bindTexture(ResourceLocation resLoc) {
        if(resLoc == null)
            RenderSystem.setShaderTexture(0, 0);
        else
            RenderSystem.setShaderTexture(0, resLoc);
    }

    public void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        drawBorder(graphics, x, y, w, h, color, 1.0);
    }

    public void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color, double sz) {
        int thickness = (int) Math.max(1, sz);
        graphics.fill(x, y, x + w, y + thickness, color);
        graphics.fill(x, y + h - thickness, x + w, y + h, color);
        graphics.fill(x, y + thickness, x + thickness, y + h - thickness, color);
        graphics.fill(x + w - thickness, y + thickness, x + w, y + h - thickness, color);
    }

    public GuiGraphics beginFramebuffer(RenderTarget fbo, float vpW, float vpH) {
        GuiGraphics tmpGraphics = new GuiGraphics(Minecraft.getInstance(), Minecraft.getInstance().renderBuffers().bufferSource());

        fbo.bindWrite(true);

        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().ortho(0.0f, vpW, vpH, 0.0f, -1.0f,1.0f), VertexSorting.ORTHOGRAPHIC_Z);

        tmpGraphics.pose().last().pose().set(RenderSystem.getModelViewStack());

        PoseStack poseStack = tmpGraphics.pose();
        poseStack.pushPose();
        poseStack.setIdentity();
        poseStack.mulPose(XP.rotationDegrees(180.0f));

        RenderSystem.getModelViewStack().pushMatrix();
        RenderSystem.getModelViewStack().set(poseStack.last().pose());
        RenderSystem.applyModelViewMatrix();

        if(!fbo.useDepth)
            RenderSystem.disableDepthTest();

        return tmpGraphics;
    }

    public void endFramebuffer(GuiGraphics poseStack, RenderTarget fbo) {
        if(!fbo.useDepth)
            RenderSystem.enableDepthTest();

        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.restoreProjectionMatrix();
        poseStack.pose().popPose();
        RenderSystem.getModelViewStack().popMatrix();
        RenderSystem.applyModelViewMatrix();
        fbo.unbindWrite();
        mc.getMainRenderTarget().bindWrite(true);
    }

    public static String tr(String text) {
        if(text.length() >= 2 && text.charAt(0) == '$') {
            if(text.charAt(1) == '$')
                return text.substring(1);
            else
                return I18n.get(text.substring(1));
        } else
            return text;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void load(JsonOWrapper json) {
        name = json.getString("name", "");
    }

    public static Bounds findBounds(java.util.List<Control> controlList) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for(Control ctrl : controlList) {
            int x = ctrl.getX();
            int y = ctrl.getY();
            if(x < minX)
                minX = x;

            if(y < minY)
                minY = y;

            x += ctrl.getWidth();
            y += ctrl.getHeight();

            if(x > maxX)
                maxX = x;

            if(y >= maxY)
                maxY = y;
        }

        return new Bounds(minX, minY, maxX, maxY);
    }

}
