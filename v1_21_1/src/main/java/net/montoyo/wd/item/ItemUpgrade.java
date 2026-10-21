/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.core.IUpgrade;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class ItemUpgrade extends ItemMulti implements IUpgrade, WDItem {
    public final DefaultUpgrade type;

    public ItemUpgrade(DefaultUpgrade type) {
        super(DefaultUpgrade.class, new Properties()/*.tab(WebDisplays.CREATIVE_TAB)*/);
        this.type = type;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("item.webdisplays.upgrade").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY));
        WDItem.addInformation(tooltip);
    }

    @Override
    public void onInstall(@NotNull ScreenBlockEntity tes, @NotNull BlockSide screenSide, @Nullable Player player, @NotNull ItemStack is) {
    }

    @Override
    public boolean onRemove(@NotNull ScreenBlockEntity tes, @NotNull BlockSide screenSide, @Nullable Player player, @NotNull ItemStack is) {
        if (DefaultUpgrade.LASERMOUSE.matchesLaserMouse(is))
            tes.clearLaserUser(screenSide);

        return false;
    }

    @Override
    public boolean isSameUpgrade(@NotNull ItemStack myStack, @NotNull ItemStack otherStack) {
        if (myStack.getItem() instanceof ItemUpgrade upgrade0) {
            if (otherStack.getItem() instanceof ItemUpgrade upgrade1) {
                return upgrade0.type == upgrade1.type;
            }
        }
        return false;
    }

    @Override
    public String getJSName(@NotNull ItemStack is) {
        Item item = is.getItem();
        if (item instanceof ItemUpgrade upgrade)
            return "webdisplays:" + upgrade.type.toString();
        return "webdisplays:null";
    }

    @Override
    public String getName() {
        return type.toString().toLowerCase();
    }

    @Override
    public String getWikiName(@NotNull ItemStack is) {
        return is.getItem().getName(is).getString();
    }
}
