package net.montoyo.wd.data;

import net.minecraft.world.level.Level;
import net.minecraft.client.gui.screens.Screen;

public interface GuiData {
    Object createGui(Screen parent, Level level);
}
