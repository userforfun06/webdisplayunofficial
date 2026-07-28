/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.montoyo.wd.config.CommonConfig;
import net.montoyo.wd.core.CraftComponent;
import net.montoyo.wd.registry.ItemRegistry;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class ItemCraftComponent extends ItemMulti implements WDItem {
    public ItemCraftComponent(Properties properties) {
        super(CraftComponent.class, properties
//                .tab(WebDisplays.CREATIVE_TAB)
        );

        //Hide the bad extension card from the creative tab
        creativeTabItems.clear(CraftComponent.BADEXTCARD.ordinal());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (stack.getItem() == ItemRegistry.getComputerCraftItem(CraftComponent.BADEXTCARD.ordinal())) {
            tooltip.add(Component.translatable("webdisplays.extcard.bad").withStyle(ChatFormatting.RED));
        } else if (stack.getItem() == ItemRegistry.getComputerCraftItem(CraftComponent.EXTCARD.ordinal()) && CommonConfig.hardRecipes) {
            tooltip.add(Component.translatable("webdisplays.extcard.cantcraft1").withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable("webdisplays.extcard.cantcraft2").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("item.webdisplays.component").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY));
        }
        WDItem.addInformation(tooltip);
    }

    @Override
    public String getWikiName(@NotNull ItemStack is) {
        return is.getItem().getName(is).getString();
    }
}
