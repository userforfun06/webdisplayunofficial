/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.montoyo.wd.WebDisplaysMod;

import org.jetbrains.annotations.NotNull;
import java.util.List;

public interface WDItem {
    static void addInformation(List<Component> tt) {
        if (tt != null && WebDisplaysMod.PROXY.isShiftDown())
            tt.add(Component.translatable("item.webdisplays.wiki").withStyle(ChatFormatting.GRAY));
    }

    String getWikiName(@NotNull ItemStack is);
}

