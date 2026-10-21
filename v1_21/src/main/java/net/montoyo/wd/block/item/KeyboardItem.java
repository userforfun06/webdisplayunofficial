package net.montoyo.wd.block.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.montoyo.wd.block.KeyboardBlockLeft;
import net.montoyo.wd.block.KeyboardBlockRight;
import net.montoyo.wd.registry.BlockRegistry;

public class KeyboardItem extends BlockItem {
    public KeyboardItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean placeBlock(BlockPlaceContext context, BlockState state) {
        Direction facing = context.getHorizontalDirection();
        state = state.setValue(KeyboardBlockLeft.FACING, facing);
        Direction d = KeyboardBlockLeft.mapDirection(facing);
        BlockPos clickedPos = context.getClickedPos();
        Level level = context.getLevel();
        if (isValid(clickedPos, level, state, d)) {
            Block kbRight = BlockRegistry.blockKbRight;
            BlockState rightState = kbRight.defaultBlockState().setValue(KeyboardBlockRight.FACING, facing);
            return level.setBlock(clickedPos.relative(d), rightState, 11) && level.setBlock(clickedPos, state, 11);
        } else if (isValid(clickedPos.relative(d.getOpposite(), 2), level, state, d)) {
            Block kbRight = BlockRegistry.blockKbRight;
            BlockState rightState = kbRight.defaultBlockState().setValue(KeyboardBlockRight.FACING, facing);
            return level.setBlock(clickedPos, rightState, 11) && level.setBlock(clickedPos.relative(d.getOpposite()), state, 11);
        }
        return false;
    }

    private boolean isValid(BlockPos pos, Level level, BlockState state, Direction d) {
        return level.getBlockState(pos.relative(d)).isAir();
    }
}
