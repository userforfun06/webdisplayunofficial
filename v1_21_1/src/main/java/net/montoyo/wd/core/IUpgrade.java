package net.montoyo.wd.core;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;

public interface IUpgrade {
    String getName();
    String getJSName(ItemStack stack);
    
    // Called when upgrade is installed on a screen
    default void onInstall(ScreenBlockEntity screen, BlockSide side, Player player, ItemStack stack) {
        // Default implementation - do nothing
    }
    
    // Check if two item stacks represent the same upgrade type
    default boolean isSameUpgrade(ItemStack stack1, ItemStack stack2) {
        return stack1.getItem() == stack2.getItem();
    }
    
    default boolean onRemove(ScreenBlockEntity screen, BlockSide side, Player player, ItemStack stack) {
        // Default implementation - do nothing, return false
        return false;
    }
}
