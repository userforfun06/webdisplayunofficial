/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.block;

import net.montoyo.wd.entity.KeyboardBlockEntity;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;

import net.montoyo.wd.core.IPeripheral;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.item.ItemLinker;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.math.Vector3i;

public class KeyboardBlockLeft extends Block implements EntityBlock, IPeripheral {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    
    public static final VoxelShape[] KEYBOARD_AABBS = new VoxelShape[]{
            Shapes.box(0.0, 0.0, 3.0 / 16, 1.0, 2.0 / 16.0, 1.0),
            Shapes.box(0.0, 0.0, 0.0, 1.0, 2.0 / 16.0, 13 / 16.0),
            Shapes.box(3.0 / 16, 0.0, 0.0, 1.0, 2.0 / 16.0, 1.0),
            Shapes.box(0.0, 0.0, 0.0, 13 / 16.0, 2.0 / 16.0, 1.0),
    };

    public KeyboardBlockLeft(BlockBehaviour.Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KeyboardBlockEntity(pos, state);
    }

    public static KeyboardBlockEntity getTileEntity(BlockState state, Level world, BlockPos pos) {
        Log.info("KeyboardBlockLeft.getTileEntity: pos=%s, block=%s", pos, state.getBlock());
        
        // Check if clicked on KeyboardBlockLeft
        if (state.getBlock() instanceof KeyboardBlockLeft) {
            BlockEntity te = world.getBlockEntity(pos);
            Log.info("KeyboardBlockLeft.getTileEntity: Clicked on KeyboardBlockLeft, te=%s", te);
            if (te instanceof KeyboardBlockEntity)
                return (KeyboardBlockEntity) te;
        }
        
        // Check if clicked on KeyboardBlockRight - find the left piece instead
        if (state.getBlock() instanceof KeyboardBlockRight) {
            Direction facing = state.getValue(FACING);
            BlockPos leftPos = pos.relative(KeyboardBlockLeft.mapDirection(facing.getOpposite()));
            BlockState leftState = world.getBlockState(leftPos);
            Log.info("KeyboardBlockLeft.getTileEntity: Clicked on KeyboardBlockRight, checking leftPos=%s, leftBlock=%s", leftPos, leftState.getBlock());
            
            if (leftState.getBlock() instanceof KeyboardBlockLeft) {
                BlockEntity te = world.getBlockEntity(leftPos);
                Log.info("KeyboardBlockLeft.getTileEntity: Found KeyboardBlockLeft at relative pos, te=%s", te);
                if (te instanceof KeyboardBlockEntity)
                    return (KeyboardBlockEntity) te;
            }
        }
    
        Log.info("KeyboardBlockLeft.getTileEntity: Returning null");
        return null;
    }
    
    public static Direction mapDirection(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> facing;
        };
    }
    
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof ItemLinker) {
            UseOnContext ctx = new UseOnContext(player, hand, hit);
            InteractionResult result = ((ItemLinker) stack.getItem()).useOn(ctx);
            return result == InteractionResult.SUCCESS ? InteractionResult.SUCCESS :
                   result == InteractionResult.CONSUME ? InteractionResult.CONSUME :
                   result == InteractionResult.PASS ? InteractionResult.PASS :
                   InteractionResult.FAIL;
        }

        if (stack.isEmpty()) {
            if (world.isClientSide)
                return InteractionResult.SUCCESS;
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if(world.isClientSide)
            return InteractionResult.SUCCESS;

        KeyboardBlockEntity keyboard = getTileEntity(state, world, pos);
        if(keyboard != null)
            return keyboard.onRightClick(player, InteractionHand.MAIN_HAND);

        return InteractionResult.SUCCESS;
    }
    
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return KEYBOARD_AABBS[state.getValue(FACING).ordinal() - 2];
    }
    
    
    private static void removeRightPiece(BlockState state, Level world, BlockPos pos) {
        BlockPos relative = pos.relative(KeyboardBlockLeft.mapDirection(state.getValue(FACING)));
    
        BlockState ns = world.getBlockState(relative);
        if (ns.getBlock() instanceof KeyboardBlockRight)
            world.setBlock(relative, Blocks.AIR.defaultBlockState(), 3);
    }
    
    public static void remove(BlockState state, Level world, BlockPos pos, boolean setState, boolean drop) {
        removeRightPiece(state, world, pos);
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
    public boolean connect(Level world, BlockPos pos, BlockState state, Vector3i scrPos, BlockSide scrSide) {
        KeyboardBlockEntity keyboard = getTileEntity(state, world, pos);
        return keyboard != null && keyboard.connect(world, pos, state, scrPos, scrSide);
    }
}
