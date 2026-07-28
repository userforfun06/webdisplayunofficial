/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.montoyo.wd.WebDisplaysMod;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.core.IPeripheral;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.Multiblock;
import net.montoyo.wd.utilities.serialization.Util;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.math.Vector3i;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class ItemLinker extends Item implements WDItem {
    public ItemLinker(Properties properties) {
        super(properties
            .stacksTo(1)
//            .tab(WebDisplays.CREATIVE_TAB)
        );
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag != null && tag.contains("ScreenX") && tag.contains("ScreenY") && tag.contains("ScreenZ") && tag.contains("ScreenSide")) {
                BlockSide side = BlockSide.values()[tag.getInt("ScreenSide")];
                tooltip.add(Component.translatable("webdisplays.linker.selectPeripheral").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("webdisplays.linker.posInfo", tag.getInt("ScreenX"), tag.getInt("ScreenY"), tag.getInt("ScreenZ")).withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("webdisplays.linker.sideInfo", Component.translatable("webdisplays.side." + side.toString().toLowerCase())).withStyle(ChatFormatting.GRAY));
                WDItem.addInformation(tooltip);
                return;
            }
        }
        tooltip.add(Component.translatable("webdisplays.linker.selectScreen").withStyle(ChatFormatting.GRAY));
        WDItem.addInformation(tooltip);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Log.debug("ItemLinker.useOn() called! hand=%s pos=%s isClient=%s",
            context.getHand(), context.getClickedPos(), context.getLevel().isClientSide());
        
        if (context.getLevel().isClientSide())
            return InteractionResult.SUCCESS;

        ItemStack stack = context.getItemInHand();
        
        // 1.21 Data Components: read CustomData from the stack
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = null;
        if (customData != null) {
            tag = customData.copyTag();
        }

        // First: If tag has screen data, try to connect peripheral
        if (tag != null && tag.contains("ScreenX") && tag.contains("ScreenY") && tag.contains("ScreenZ") && tag.contains("ScreenSide")) {
            Log.debug("ItemLinker: Has screen data, attempting to connect peripheral");

            Vector3i tePos = new Vector3i(tag.getInt("ScreenX"), tag.getInt("ScreenY"), tag.getInt("ScreenZ"));
            BlockSide scrSide = BlockSide.values()[tag.getInt("ScreenSide")];

            // Verify the screen still exists before attempting to connect
            BlockEntity scrTe = context.getLevel().getBlockEntity(tePos.toBlock());
            if (!(scrTe instanceof ScreenBlockEntity) || ((ScreenBlockEntity) scrTe).getScreen(scrSide) == null) {
                Log.debug("ItemLinker: Screen no longer exists at %s side=%s, showing turnOn", tePos, scrSide);
                Util.toast(context.getPlayer(), "turnOn");
                stack.remove(DataComponents.CUSTOM_DATA);
                return InteractionResult.SUCCESS;
            }

            BlockState state = context.getLevel().getBlockState(context.getClickedPos());
            IPeripheral target;

            if (state.getBlock() instanceof IPeripheral)
                target = (IPeripheral) state.getBlock();
            else {
                BlockEntity te = context.getLevel().getBlockEntity(context.getClickedPos());
                if (te == null || !(te instanceof IPeripheral)) {
                    if (context.getPlayer().isShiftKeyDown()) {
                        Util.toast(context.getPlayer(), ChatFormatting.GOLD, "linkAbort");
                        stack.remove(DataComponents.CUSTOM_DATA);
                    } else
                        Util.toast(context.getPlayer(), "peripheral");

                    return InteractionResult.SUCCESS;
                }

                target = (IPeripheral) te;
            }

            Log.debug("ItemLinker: Connecting peripheral at %s to screen at %s side=%s", 
                context.getClickedPos(), tePos, scrSide);

            if (target.connect(context.getLevel(), context.getClickedPos(), state, tePos, scrSide)) {
                Log.debug("ItemLinker: SUCCESS - Connected to screen");
                Util.toast(context.getPlayer(), ChatFormatting.AQUA, "linked");
                if (context.getPlayer() instanceof ServerPlayer)
                    WebDisplaysMod.INSTANCE.criterionLinkPeripheral.trigger((ServerPlayer) context.getPlayer());
            } else {
                Log.debug("ItemLinker: FAILED - Could not connect");
                Util.toast(context.getPlayer(), "linkError");
            }

            stack.remove(DataComponents.CUSTOM_DATA);
            return InteractionResult.SUCCESS;
        }

        // Second: No screen data in tag, check if clicking on a screen
        Log.debug("ItemLinker: No screen data in tag, checking if clicked block is screen");
        
        Block clickedBlock = context.getLevel().getBlockState(context.getClickedPos()).getBlock();
        Log.debug("ItemLinker: Clicked block class: %s", clickedBlock != null ? clickedBlock.getClass().getName() : "null");
        
        if (!(clickedBlock instanceof ScreenBlock)) {
            Log.debug("ItemLinker: Clicked block is NOT ScreenBlock, returning notAScreen");
            Util.toast(context.getPlayer(), "notAScreen");
            return InteractionResult.SUCCESS;
        }

        Vector3i pos = new Vector3i(context.getClickedPos());
        BlockSide side = BlockSide.values()[context.getClickedFace().ordinal()];
        
        Log.debug("ItemLinker: Clicked pos=%s face=%s side=%s", context.getClickedPos(), context.getClickedFace(), side);
        
        Multiblock.findOrigin(context.getLevel(), pos, side, null);
        Log.debug("ItemLinker: Screen origin found at %s", pos);

        BlockEntity te = context.getLevel().getBlockEntity(pos.toBlock());
        if (te == null || !(te instanceof ScreenBlockEntity)) {
            Log.debug("ItemLinker: No ScreenBlockEntity at origin, returning turnOn");
            Util.toast(context.getPlayer(), "turnOn");
            return InteractionResult.SUCCESS;
        }

ScreenData scr = ((ScreenBlockEntity) te).getScreen(side);
        if(scr == null) {
            Log.debug("ItemLinker: ScreenData is null for side %s, returning turnOn", side);
            Util.toast(context.getPlayer(), "turnOn");
        }
        else {
            int playerRights = scr.rightsFor(context.getPlayer());
            Log.debug("ItemLinker: Screen owner=%s, player=%s, playerRights=%d, MANAGE_UPGRADES=%d",
                scr.owner != null ? scr.owner.uuid : "null",
                context.getPlayer().getGameProfile().getId(),
                playerRights,
                ScreenRights.MANAGE_UPGRADES);
            
            // Allow linking if player has MANAGE_UPGRADES permission OR is the owner (ALL rights)
            // This matches 1.20 behavior - only owner can link peripherals
            boolean canLink = (playerRights & ScreenRights.MANAGE_UPGRADES) != 0 || playerRights == ScreenRights.ALL;
            
            if (!canLink) {
                Log.debug("ItemLinker: Player cannot link - no MANAGE_UPGRADES permission and not owner");
                Util.toast(context.getPlayer(), "restrictions");
            }
            else {
                // Store screen position in item
                tag = new CompoundTag();
                tag.putInt("ScreenX", pos.x);
                tag.putInt("ScreenY", pos.y);
                tag.putInt("ScreenZ", pos.z);
                tag.putInt("ScreenSide", side.ordinal());

                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                
                Log.debug("ItemLinker: Screen position stored: x=%d y=%d z=%d side=%s", pos.x, pos.y, pos.z, side);
                Util.toast(context.getPlayer(), ChatFormatting.AQUA, "screenSet2");
            }
        }

        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public String getWikiName(@NotNull ItemStack is) {
        return is.getItem().getName(is).getString();
    }
}