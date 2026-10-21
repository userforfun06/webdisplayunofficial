/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.block;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.montoyo.wd.core.DefaultPeripheral;
import net.montoyo.wd.entity.AbstractInterfaceBlockEntity;
import net.montoyo.wd.entity.AbstractPeripheralBlockEntity;
import net.montoyo.wd.entity.ServerBlockEntity;
import net.montoyo.wd.item.ItemLinker;
import net.montoyo.wd.item.ItemOwnershipThief;
import org.jetbrains.annotations.Nullable;

public class PeripheralBlock extends Block implements EntityBlock {
    private final DefaultPeripheral type;

    public static BlockPos point = BlockPos.ZERO;

    public PeripheralBlock(BlockBehaviour.Properties props, DefaultPeripheral type) {
        super(props);
        this.type = type;
    }

    public DefaultPeripheral getType() {
        return type;
    }

    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return type.createBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Allow linking tool to handle the interaction
        if (stack.getItem() instanceof ItemLinker) {
            UseOnContext ctx = new UseOnContext(player, hand, hit);
            InteractionResult result = ((ItemLinker) stack.getItem()).useOn(ctx);
            return result == InteractionResult.SUCCESS ? InteractionResult.SUCCESS :
                   result == InteractionResult.CONSUME ? InteractionResult.CONSUME :
                   result == InteractionResult.PASS ? InteractionResult.PASS :
                   InteractionResult.FAIL;
        }

        // Only open URL box with empty hands - block interaction when holding items
        if (stack.isEmpty())
            return useWithoutItem(state, level, pos, player, hit);

        // Allow Ownership Thief to interact with server blocks
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof ServerBlockEntity && stack.getItem() instanceof ItemOwnershipThief) {
            UseOnContext ctx = new UseOnContext(player, hand, hit);
            InteractionResult result = ((ItemOwnershipThief) stack.getItem()).useOn(ctx);
            return result == InteractionResult.SUCCESS ? InteractionResult.SUCCESS :
                   result == InteractionResult.CONSUME ? InteractionResult.CONSUME :
                   result == InteractionResult.PASS ? InteractionResult.PASS :
                   InteractionResult.FAIL;
        }

        // Holding an item (not linker) - don't open URL box
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof AbstractPeripheralBlockEntity)
            return ((AbstractPeripheralBlockEntity) te).onRightClick(player, InteractionHand.MAIN_HAND);
        else if (te instanceof ServerBlockEntity) {
            ((ServerBlockEntity) te).onPlayerRightClick(player);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
    
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (level.isClientSide)
            return;
        
        if (placer instanceof Player) {
            BlockEntity te = level.getBlockEntity(pos);
            if (te instanceof ServerBlockEntity) {
                ((ServerBlockEntity) te).setOwner((Player) placer);
            } else if (te instanceof AbstractInterfaceBlockEntity) {
                ((AbstractInterfaceBlockEntity) te).setOwner((Player) placer);
            }
        }
    }

    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof AbstractPeripheralBlockEntity)
            ((AbstractPeripheralBlockEntity) te).onNeighborChange(neighborBlock, neighborPos);
    }

    // Network targeting helper - returns block position for packet distribution
    public static BlockPos point(BlockPos pos) {
        return pos;
    }
}
