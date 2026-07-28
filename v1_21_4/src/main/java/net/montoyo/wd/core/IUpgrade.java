package net.montoyo.wd.core;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;

public interface IUpgrade {
    String getUpgradeName();
    String getJSName(ItemStack stack);

    default void onInstall(ScreenBlockEntity screen, BlockSide side, Player player, ItemStack stack) {
    }

    default boolean isSameUpgrade(ItemStack stack1, ItemStack stack2) {
        return stack1.getItem() == stack2.getItem();
    }

    default boolean onRemove(ScreenBlockEntity screen, BlockSide side, Player player, ItemStack stack) {
        return false;
    }
}
