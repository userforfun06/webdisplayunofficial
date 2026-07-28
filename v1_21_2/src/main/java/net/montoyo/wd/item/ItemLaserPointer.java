/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.montoyo.wd.WebDisplaysMod;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemLaserPointer extends Item implements WDItem {
	
	public ItemLaserPointer(Properties properties) {
		super(properties
//				.tab(WebDisplays.CREATIVE_TAB)
		);
	}
	
	@Nullable
	@Override
	public String getWikiName(@NotNull ItemStack is) {
		return is.getItem().getName(is).getString();
	}
	
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide) {
            return InteractionResult.PASS;
        }
        
        // Client-side: use proxy to open laser GUI
        WebDisplaysMod.PROXY.openLaserGui(player, stack, hand);
        
        return InteractionResult.SUCCESS;
    }
    
    public static void tick(Object mc) {
    }
    
    public static void deselect(Object mc) {
    }
}

