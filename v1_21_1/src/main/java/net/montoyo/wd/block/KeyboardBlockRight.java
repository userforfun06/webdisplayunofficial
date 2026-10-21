/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.block;

import net.montoyo.wd.core.IPeripheral;
import net.montoyo.wd.entity.KeyboardBlockEntity;
import net.montoyo.wd.item.ItemLinker;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.math.Vector3i;

import static net.montoyo.wd.block.KeyboardBlockLeft.KEYBOARD_AABBS;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.ItemInteractionResult;

public class KeyboardBlockRight extends Block implements IPeripheral {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public KeyboardBlockRight() {
        super(Properties.of()
                .strength(1.5f, 10.f)
                .requiresCorrectToolForDrops());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    private static void removeLeftPiece(BlockState state, Level world, BlockPos pos) {
        BlockPos relative = pos.relative(KeyboardBlockLeft.mapDirection(state.getValue(FACING).getOpposite()));
        
        BlockState ns = world.getBlockState(relative);
        if (ns.getBlock() instanceof KeyboardBlockLeft)
            world.setBlock(relative, Blocks.AIR.defaultBlockState(), 3);
    }
    
    public static void remove(BlockState state, Level world, BlockPos pos, boolean setState, boolean drop) {
        removeLeftPiece(state, world, pos);
        if (setState)
            world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        // Packet sending removed - needs update for 1.21
    }
    
    @Override
    public void onRemove(BlockState arg, Level arg2, BlockPos arg3, BlockState arg4, boolean bl) {
        if (!arg2.isClientSide)
            remove(arg, arg2, arg3, false, false);
        super.onRemove(arg, arg2, arg3, arg4, bl);
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public boolean isCollisionShapeFullBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return KEYBOARD_AABBS[state.getValue(FACING).ordinal() - 2];
    }

    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        double rpos = (entity.getY() - ((double) pos.getY())) * 16.0;
        if (!world.isClientSide && rpos >= 1.0 && rpos <= 2.0 && Math.random() < 0.25) {
            KeyboardBlockEntity tek = KeyboardBlockLeft.getTileEntity(state, world, pos);

            if (tek != null)
                tek.simulateCat(entity);
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // Allow linking tool to handle the interaction directly
        if (stack.getItem() instanceof ItemLinker) {
            UseOnContext ctx = new UseOnContext(player, hand, hit);
            InteractionResult result = ((ItemLinker) stack.getItem()).useOn(ctx);
            return result == InteractionResult.SUCCESS ? ItemInteractionResult.SUCCESS :
                   result == InteractionResult.CONSUME ? ItemInteractionResult.CONSUME :
                   result == InteractionResult.PASS ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION :
                   ItemInteractionResult.FAIL;
        }

        // Only interact with empty hands - block when holding items
        if (stack.isEmpty())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        // Holding an item (not linker) - don't interact
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;

        KeyboardBlockEntity tek = KeyboardBlockLeft.getTileEntity(state, level, pos);
        if (tek != null)
            return tek.onRightClick(player, InteractionHand.MAIN_HAND);

        return InteractionResult.PASS;
    }
    
    @Override
    public VoxelShape getOcclusionShape(BlockState arg, BlockGetter arg2, BlockPos arg3) {
        return Shapes.empty();
    }

    @Override
    public boolean connect(Level world, BlockPos pos, BlockState state, Vector3i scrPos, BlockSide scrSide) {
        KeyboardBlockEntity keyboard = KeyboardBlockLeft.getTileEntity(state, world, pos);
        return keyboard != null && keyboard.connect(world, pos, state, scrPos, scrSide);
    }
}
