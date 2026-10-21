/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.config.CommonConfig;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.entity.ServerBlockEntity;
import net.montoyo.wd.utilities.*;
import net.montoyo.wd.utilities.math.Vector3i;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.serialization.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class ItemOwnershipThief extends Item implements WDItem {
    public ItemOwnershipThief(Properties properties) {
        super(properties
                .stacksTo(1)
//                .tab(WebDisplays.CREATIVE_TAB)
        );
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag != null && tag.contains("PosX") && tag.contains("PosY") && tag.contains("PosZ") && tag.contains("Side")) {
                tooltip.add(Component.translatable("webdisplays.linker.posInfo", tag.getInt("PosX"), tag.getInt("PosY"), tag.getInt("PosZ")));
                tooltip.add(Component.translatable("webdisplays.linker.sideInfo", Component.translatable("webdisplays.side." + BlockSide.values()[tag.getInt("Side")].toString().toLowerCase())));
                WDItem.addInformation(tooltip);
                return;
            }
        }
        tooltip.add(Component.literal("WARNING: Admin tool").withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("webdisplays.linker.selectScreen"));
        tooltip.add(Component.translatable("webdisplays.message.screenSet"));
        WDItem.addInformation(tooltip);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer().isShiftKeyDown())
            return InteractionResult.PASS;

        if (context.getLevel().isClientSide())
            return InteractionResult.SUCCESS;

        if (CommonConfig.disableOwnershipThief) {
            Util.toast(context.getPlayer(), "otDisabled");
            return InteractionResult.SUCCESS;
        }

        ItemStack stack = context.getItemInHand();

        // Check if clicking a ServerBlockEntity first (always steals server ownership)
        BlockEntity clickedTe = context.getLevel().getBlockEntity(context.getClickedPos());
        if (clickedTe instanceof ServerBlockEntity serverBE) {
            Log.warning("Owner of server at %d %d %d was changed from %s (UUID %s) to %s (UUID %s)", context.getClickedPos().getX(), context.getClickedPos().getY(), context.getClickedPos().getZ(), serverBE.getOwnerName(), serverBE.getOwnerUUID(), context.getPlayer().getName(), context.getPlayer().getGameProfile().getId().toString());
            context.getPlayer().swing(context.getHand());
            context.getPlayer().setItemInHand(context.getHand(), ItemStack.EMPTY);
            serverBE.setOwner(context.getPlayer());
            Util.toast(context.getPlayer(), ChatFormatting.AQUA, "newOwner");
            return InteractionResult.SUCCESS;
        }

        // 1.21 Data Components: read CustomData from the stack
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = null;
        if (customData != null) {
            tag = customData.copyTag();
        }
        if (tag != null) {

            if (tag.contains("PosX") && tag.contains("PosY") && tag.contains("PosZ") && tag.contains("Side")) {
                BlockPos bp = new BlockPos(tag.getInt("PosX"), tag.getInt("PosY"), tag.getInt("PosZ"));
                BlockSide side = BlockSide.values()[tag.getInt("Side")];

                if (!(context.getLevel().getBlockState(bp).getBlock() instanceof ScreenBlock))
                    return InteractionResult.SUCCESS;

                BlockEntity te = context.getLevel().getBlockEntity(bp);
                if (te == null || !(te instanceof ScreenBlockEntity))
                    return InteractionResult.SUCCESS;

                ScreenBlockEntity tes = (ScreenBlockEntity) te;
                ScreenData scr = tes.getScreen(side);
                if(scr == null)
                    return InteractionResult.SUCCESS;

                Log.warning("Owner of screen at %d %d %d, side %s was changed from %s (UUID %s) to %s (UUID %s)", bp.getX(), bp.getY(), bp.getZ(), side.toString(), scr.owner.name, scr.owner.uuid.toString(), context.getPlayer().getName(), context.getPlayer().getGameProfile().getId().toString());
                context.getPlayer().swing(context.getHand());
                context.getPlayer().setItemInHand(context.getHand(), ItemStack.EMPTY);
                tes.setOwner(side, context.getPlayer());
                Util.toast(context.getPlayer(), ChatFormatting.AQUA, "newOwner");
                return InteractionResult.SUCCESS;
            }
        }

        if (!(context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof ScreenBlock))
            return InteractionResult.SUCCESS;

        Vector3i pos = new Vector3i(context.getClickedPos());
        BlockSide side = BlockSide.values()[context.getClickedFace().ordinal()];
        Multiblock.findOrigin(context.getLevel(), pos, side, null);

        BlockEntity te = context.getLevel().getBlockEntity(pos.toBlock());
        if (te == null || !(te instanceof ScreenBlockEntity)) {
            Util.toast(context.getPlayer(), "turnOn");
            return InteractionResult.SUCCESS;
        }

        if (((ScreenBlockEntity) te).getScreen(side) == null)
            Util.toast(context.getPlayer(), "turnOn");
        else {
            CompoundTag newTag = new CompoundTag();
            newTag.putInt("PosX", pos.x);
            newTag.putInt("PosY", pos.y);
            newTag.putInt("PosZ", pos.z);
            newTag.putInt("Side", side.ordinal());

            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(newTag));
            Util.toast(context.getPlayer(), ChatFormatting.AQUA, "screenSet");
            Log.warning("Player %s (UUID %s) created an Ownership Thief item for screen at %d %d %d, side %s!", context.getPlayer().getName(), context.getPlayer().getGameProfile().getId().toString(), pos.x, pos.y, pos.z, side.toString());
        }

        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public String getWikiName(@NotNull ItemStack is) {
        return "Ownership_Thief";
    }
}
