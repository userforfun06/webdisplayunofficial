package net.montoyo.wd.client.gui.controls;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.montoyo.wd.client.gui.loading.JsonOWrapper;
import org.lwjgl.glfw.GLFW;

public class Button extends Control {

    protected final net.minecraft.client.gui.components.Button btn;
    protected boolean selected = false;
    protected boolean shiftDown = false;
    protected int originalColor = 0;
    protected int shiftColor = 0;

    public static class ClickEvent extends Event<Button> {

        private final boolean shiftDown;

        public ClickEvent(Button btn) {
            source = btn;
            shiftDown = btn.shiftDown;
        }

        public boolean isShiftDown() {
            return shiftDown;
        }

    }

    public Button() {
        btn = net.minecraft.client.gui.components.Button.builder(Component.literal(""), a -> {})
                .pos(0, 0)
                .size(0, 0)
                .build();
    }

    public Button(String text, int x, int y, int width) {
        btn = net.minecraft.client.gui.components.Button.builder(Component.literal(text), a -> {})
                .pos(x, y)
                .size(width, 20)
                .build();
    }

    public Button(String text, int x, int y) {
        btn = net.minecraft.client.gui.components.Button.builder(Component.literal(text), a -> {})
                .pos(0, 0)
                .size(x, y)
                .build();
    }

    private void setBtnColor(int color) {
        btn.setMessage(btn.getMessage().copy().setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0xFFFFFF))));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if(mouseButton == 0 && btn.mouseClicked(mouseX, mouseY, mouseButton)) {
            selected = true;
            btn.playDownSound(mc.getSoundManager());

            if(!onClick())
                parent.actionPerformed(new ClickEvent(this));
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int state) {
        if(selected && state == 0) {
            btn.mouseReleased(mouseX, mouseY,state);
            selected = false;

            return true;
        }

        return true;
    }

    @Override
    public void draw(GuiGraphics poseStack, int mouseX, int mouseY, float ptt) {
        int color = 16777215;
        if(shiftDown && shiftColor != 0)
            color = shiftColor;
        else if(originalColor != 0)
            color = originalColor;
        btn.setMessage(btn.getMessage().copy().setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color & 0xFFFFFF))));
        btn.render(poseStack, mouseX, mouseY, ptt);
    }

    public void setLabel(String label) {
        btn.setMessage(Component.literal(label));
    }

    public String getLabel() {
        return btn.getMessage().getString();
    }

    public void setWidth(int width) {
        btn.setWidth(width);
    }

    public void setHeight(int height) {
        btn.setHeight(height);
    }

    @Override
    public int getWidth() {
        return btn.getWidth();
    }

    @Override
    public int getHeight() {
        return btn.getHeight();
    }

    @Override
    public void setPos(int x, int y) {
        btn.setPosition(x, y);
    }

    @Override
    public int getX() {
        return btn.getX();
    }

    @Override
    public int getY() {
        return btn.getY();
    }

    public net.minecraft.client.gui.components.Button getMcButton() {
        return btn;
    }

    public void setDisabled(boolean dis) {
        btn.active = !dis;
    }

    public boolean isDisabled() {
        return !btn.active;
    }

    public void enable() {
        btn.active = true;
    }

    public void disable() {
        btn.active = false;
    }

    public void setVisible(boolean visible) {
        btn.visible = visible;
    }

    public boolean isVisible() {
        return btn.visible;
    }

    public void show() {
        btn.visible = true;
    }

    public void hide() {
        btn.visible = false;
    }

    public boolean isShiftDown() {
        return shiftDown;
    }

    @Override
    public boolean keyUp(int key, int scanCode, int modifiers) {
        if(key == GLFW.GLFW_KEY_LEFT_SHIFT || key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            shiftDown = false;
            if(originalColor != 0)
                setBtnColor(originalColor);
            return true;
        }

        return false;
    }

    @Override
    public boolean keyDown(int key, int scanCode, int modifiers) {
        if(key == GLFW.GLFW_KEY_LEFT_SHIFT || key == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            shiftDown = true;
            if(shiftColor != 0)
                setBtnColor(shiftColor);
            return true;
        }

        return false;
    }

    public void setTextColor(int color) {
        originalColor = color;
        if(!shiftDown)
            setBtnColor(color);
    }

    public int getTextColor() {
        return originalColor;
    }

    public void setShiftTextColor(int shiftColor) {
        this.shiftColor = shiftColor;
        if(shiftDown)
            setBtnColor(shiftColor);
    }

    public int getShiftTextColor() {
        return shiftColor;
    }

    @Override
    public void load(JsonOWrapper json) {
        super.load(json);
        btn.setPosition(
                json.getInt("x", 0),
                json.getInt("y", 0)
        );
        btn.setWidth(json.getInt("width", 200));
        btn.setHeight(json.getInt("height", 20));
        btn.setMessage(Component.literal(tr(json.getString("label", ""))));
        btn.active =  json.getBool("active", btn.active);
        btn.visible = json.getBool("visible", btn.visible);

        originalColor = json.getColor("color", originalColor);
        shiftColor = json.getColor("shiftColor", shiftColor);
        if(originalColor != 0)
            setBtnColor(originalColor);
    }

    protected boolean onClick() {
        return false;
    }

}
