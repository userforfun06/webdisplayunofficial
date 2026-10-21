package net.montoyo.wd.client.gui;

import com.cinemamod.mcef.MCEFBrowser;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.math.Axis;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.client.WebDisplaysDirs;
import net.montoyo.wd.controls.builtin.ClickControl;
import net.montoyo.wd.controls.builtin.KeyTypedControl;
import net.montoyo.wd.client.gui.camera.KeyboardCamera;
import net.montoyo.wd.client.gui.controls.Button;
import net.montoyo.wd.client.gui.controls.Control;
import net.montoyo.wd.client.gui.controls.Label;
import net.montoyo.wd.client.gui.loading.FillControl;
import net.montoyo.wd.client.renderers.ScreenRenderer;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.net.server_bound.C2SMessageScreenCtrl;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.math.Vector2i;
import net.montoyo.wd.utilities.serialization.TypeData;
import net.montoyo.wd.utilities.serialization.Util;
import org.cef.browser.CefBrowser;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;


public class GuiKeyboard extends WDScreen {

    private static final String WARNING_FNAME = "wd_keyboard_warning.txt";

    private ScreenBlockEntity tes;
    private BlockSide side;
    private ScreenData data;
    private final ArrayList<TypeData> evStack = new ArrayList<>();
    private BlockPos kbPos;
    private boolean showWarning = true;

    @FillControl
    private Label lblInfo;

    @FillControl
    private Button btnOk;

    public GuiKeyboard() {
        super(Component.literal(""));
    }

    public GuiKeyboard(ScreenBlockEntity tes, BlockSide side, BlockPos kbPos) {
        this();
        this.tes = tes;
        this.side = side;
        this.kbPos = kbPos;
    }

    @Override
    protected void addLoadCustomVariables(Map<String, Double> vars) {
        vars.put("showWarning", showWarning ? 1.0 : 0.0);
    }

    @Override
    public void init() {
        super.init();

        if (minecraft.getSingleplayerServer() != null && !minecraft.getSingleplayerServer().isPublished())
            showWarning = false;
        else
            showWarning = !hasUserReadWarning();

        loadFrom(ResourceLocation.fromNamespaceAndPath("webdisplays", "gui/kb_right.json"));

        if (showWarning) {
            int maxLabelW = 0;
            int totalH = 0;

            for (Control ctrl : controls) {
                if (ctrl != lblInfo && ctrl instanceof Label) {
                    if (ctrl.getWidth() > maxLabelW)
                        maxLabelW = ctrl.getWidth();

                    totalH += ctrl.getHeight();
                    ctrl.setPos((width - ctrl.getWidth()) / 2, 0);
                }
            }

            btnOk.setWidth(maxLabelW);
            btnOk.setPos((width - maxLabelW) / 2, 0);
            totalH += btnOk.getHeight();

            int y = (height - totalH) / 2;
            for (Control ctrl : controls) {
                if (ctrl != lblInfo) {
                    ctrl.setPos(ctrl.getX(), y);
                    y += ctrl.getHeight();
                }
            }
        } else {
            if (!minecraft.isWindowActive()) {
                minecraft.setWindowActive(true);
                minecraft.mouseHandler.grabMouse();
            }
        }

        defaultBackground = showWarning;
        syncTicks = 5;

        KeyboardCamera.focus(tes, side);

        data = tes.getScreen(side);
        if (data == null) return;
        if (data.browser == null)
            ScreenRenderer.flushBrowserQueue();
        data = tes.getScreen(side);
        if (data == null) return;
        CefBrowser browser = data.browser;
		if (browser instanceof MCEFBrowser mcef) {
			var prev = GLFW.glfwSetErrorCallback((error, desc) -> {});
			mcef.setCursor(org.cef.misc.CefCursorType.fromId(data.mouseType));
			GLFW.glfwSetErrorCallback(prev);
			mcef.setCursorChangeListener((id) -> {
				if (data != null)
					data.mouseType = id;
				int cursorId = id;
				minecraft.execute(() -> {
					if (data != null && data.browser instanceof MCEFBrowser m) {
						var p = GLFW.glfwSetErrorCallback((err, desc) -> {});
						m.setCursor(org.cef.misc.CefCursorType.fromId(cursorId));
						GLFW.glfwSetErrorCallback(p);
					}
				});
			});
		}
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void removed() {
        super.removed();
        KeyboardCamera.focus(null, null);
        if (data != null && data.browser instanceof MCEFBrowser mcef) {
            var prev = GLFW.glfwSetErrorCallback((error, desc) -> {});
            mcef.setCursor(org.cef.misc.CefCursorType.POINTER);
            GLFW.glfwSetErrorCallback(prev);
            data.mouseType = 0;
            mcef.setCursorChangeListener((cursor) -> data.mouseType = cursor);
        }
    }

    @Override
    public void onClose() {
        removed();
        super.onClose();
        this.minecraft.setScreen(null);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (quitOnEscape && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        addKey(new TypeData(TypeData.Action.PRESS, keyCode, modifiers, scanCode));
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        addKey(new TypeData(TypeData.Action.TYPE, codePoint, modifiers, 0));
        return true;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        addKey(new TypeData(TypeData.Action.RELEASE, keyCode, modifiers, scanCode));
        return true;
    }

    void addKey(TypeData data) {
        tes.type(side, "[" + WebDisplays.GSON.toJson(data) + "]", kbPos);

        evStack.add(data);
        if (!evStack.isEmpty() && !syncRequested())
            requestSync();
    }

    @Override
    protected void sync() {
        if(!evStack.isEmpty()) {
            String json = WebDisplays.GSON.toJson(evStack);
            C2SMessageScreenCtrl packet = new C2SMessageScreenCtrl(tes, side, new KeyTypedControl(json, kbPos));
            net.montoyo.wd.net.WDNetworkRegistry.sendToServer(packet);
            evStack.clear();
        }
    }

    @GuiSubscribe
    public void onClick(Button.ClickEvent ev) {
        if(showWarning && ev.getSource() == btnOk) {
            writeUserAcknowledge();

            for(Control ctrl: controls) {
                if(ctrl instanceof Label) {
                    Label lbl = (Label) ctrl;
                    lbl.setVisible(!lbl.isVisible());
                }
            }

            btnOk.setDisabled(true);
            btnOk.setVisible(false);
            showWarning = false;
            defaultBackground = false;
            minecraft.setWindowActive(true);
            minecraft.mouseHandler.grabMouse();
        }
    }

    private boolean hasUserReadWarning() {
        try {
            File f = WebDisplaysDirs.getCacheFile(WARNING_FNAME);

            if(f.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String str = br.readLine();
                Util.silentClose(br);

                return str != null && str.trim().equalsIgnoreCase("read");
            }
        } catch(Throwable t) {
            Log.warningEx("Can't know if user has already read the warning", t);
        }

        return false;
    }

    private void writeUserAcknowledge() {
        try {
            File f = WebDisplaysDirs.getCacheFile(WARNING_FNAME);

            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            bw.write("read\n");
            Util.silentClose(bw);
        } catch(Throwable t) {
            Log.warningEx("Can't write that the user read the warning", t);
        }
    }

    @Override
    public boolean isForBlock(BlockPos bp, BlockSide side) {
        return bp.equals(kbPos) || (bp.equals(tes.getBlockPos()) && side == this.side);
    }

    private static Method fovMethod;

    protected void mouse(double mouseX, double mouseY, Consumer<Vector2i> func) {
        Entity e = Minecraft.getInstance().getEntityRenderDispatcher().camera.getEntity();
        float pct = Minecraft.getInstance().getTimer().getGameTimeDeltaTicks();

        double fov;
        try {
            if (fovMethod == null) {
                fovMethod = GameRenderer.class.getDeclaredMethod("getFov", Camera.class, float.class, boolean.class);
                fovMethod.setAccessible(true);
            }
            fov = (double) fovMethod.invoke(Minecraft.getInstance().gameRenderer, Minecraft.getInstance().getEntityRenderDispatcher().camera, pct, true);
        } catch (Exception e2) {
            fov = Minecraft.getInstance().options.fov().get();
        }

        mouseX /= width;
        mouseY /= height;

        mouseX -= 0.5;
        mouseY -= 0.5;
        mouseY = -mouseY;

        Matrix4f proj = new Matrix4f(Minecraft.getInstance().gameRenderer.getProjectionMatrix(fov));

        PoseStack cameraStack = new PoseStack();
        float[] angle = KeyboardCamera.getAngle(e, pct);
        cameraStack.mulPose(Axis.XP.rotationDegrees(angle[0]));
        cameraStack.mulPose(Axis.YP.rotationDegrees(angle[1] + 180.0F));

        Vector4f coord = new Vector4f(2f * (float) mouseX, 2 * (float) mouseY, 0, 1f);
        coord.add(proj.invert().transform(coord));
        coord = new Matrix4f(cameraStack.last().pose()).invert().transform(coord);

        Vec3 vec3 = e.getEyePosition(pct);
        Vec3 vec31 = new Vec3(coord.x, coord.y, coord.z).normalize();

        BlockHitResult result = tes.trace(side, vec3, vec31);
        if (result.getType() != HitResult.Type.MISS) {
            tes.interact(result, func);
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        mouse(mouseX, mouseY, (hit) -> {
            tes.handleMouseEvent(side, ClickControl.ControlType.MOVE, hit, -1);
        });

        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        mouse(mouseX, mouseY, (hit) -> {
            tes.handleMouseEvent(side, ClickControl.ControlType.MOVE, hit, -1);
            tes.handleMouseEvent(side, ClickControl.ControlType.DOWN, hit, button);
        });

        KeyboardCamera.setMouse(button, true);

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        mouse(mouseX, mouseY, (hit) -> {
            tes.handleMouseEvent(side, ClickControl.ControlType.MOVE, hit, -1);
            tes.handleMouseEvent(side, ClickControl.ControlType.UP, hit, button);
        });

        KeyboardCamera.setMouse(button, false);

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        if (data == null || !(data.browser instanceof MCEFBrowser)) {
            ScreenData newData = tes.getScreen(side);
            if (newData != null && newData.browser instanceof MCEFBrowser mcef) {
                data = newData;
                mcef.setCursorChangeListener((id) -> {
                    data.mouseType = id;
                    int cursorId = id;
                    minecraft.execute(() -> {
                        if (data != null && data.browser instanceof MCEFBrowser m) {
                            var p = GLFW.glfwSetErrorCallback((err, desc) -> {});
                            m.setCursor(org.cef.misc.CefCursorType.fromId(cursorId));
                            GLFW.glfwSetErrorCallback(p);
                        }
                    });
                });
            }
        }

        double mouseX = Minecraft.getInstance().mouseHandler.xpos() / Minecraft.getInstance().getWindow().getWidth();
        double mouseY = Minecraft.getInstance().mouseHandler.ypos() / Minecraft.getInstance().getWindow().getHeight();

        mouse(mouseX * width, mouseY * height, (hit) -> {
            tes.handleMouseEvent(side, ClickControl.ControlType.MOVE, hit, -1);
        });

        super.tick();
    }
}
