/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;

import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.WebDisplaysMod;
import net.montoyo.wd.config.CommonConfig;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.core.IUpgrade;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.data.SetURLData;
import net.montoyo.wd.entity.AbstractPeripheralBlockEntity;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.*;
import net.montoyo.wd.utilities.math.Vector2i;
import net.montoyo.wd.utilities.math.Vector3f;
import net.montoyo.wd.utilities.math.Vector3i;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.serialization.Util;
import org.jetbrains.annotations.NotNull;

public class ScreenBlock extends BaseEntityBlock {
    public static final BooleanProperty hasTE = BooleanProperty.create("haste");
    public static final BooleanProperty emitting = BooleanProperty.create("emitting");
    private static final Property<?>[] properties = new Property<?>[]{hasTE, emitting};

    public static final MapCodec<ScreenBlock> CODEC = simpleCodec(ScreenBlock::new);

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public ScreenBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(hasTE, false).setValue(emitting, false));
    }

    @Override
    public void onRemove(BlockState p_60515_, Level p_60516_, BlockPos p_60517_, BlockState p_60518_, boolean p_60519_) {
        // Return early if the block type didn't change (e.g. hasTE property toggle)
        // This prevents onRemove from firing when setBlockAndUpdate changes hasTE
        if (p_60518_.getBlock() == p_60515_.getBlock()) return;

        // Unload screen and stop browser/audio when ANY block of the screen is broken
        // Check all 6 sides to find if this block was part of a screen
        for (BlockSide side : BlockSide.values()) {
            Vector3i vec = new Vector3i(p_60517_.getX(), p_60517_.getY(), p_60517_.getZ());
            // Find the origin of the screen on this side
            Multiblock.findOrigin(p_60516_, vec, side, null);
            BlockPos originPos = new BlockPos(vec.x, vec.y, vec.z);
            
            // Get the screen entity at the origin
            BlockEntity be = p_60516_.getBlockEntity(originPos);
            if (be instanceof ScreenBlockEntity screenBE) {
                // Check if there's actually a screen on this side
                if (screenBE.getScreen(side) != null) {
                    // Check if our broken block is part of this screen
                    // by checking if it's within the screen bounds
                    screenBE.unload(); // Close browser and stop audio immediately

                    try {
                        if (!p_60516_.isClientSide) {
                            // Clear all peripherals linked to this screen
                            BlockPos.betweenClosed(
                                originPos.getX() - 16, originPos.getY() - 16, originPos.getZ() - 16,
                                originPos.getX() + 16, originPos.getY() + 16, originPos.getZ() + 16
                            ).forEach(checkPos -> {
                                BlockEntity pe = p_60516_.getBlockEntity(checkPos);
                                if (pe instanceof AbstractPeripheralBlockEntity peripheral && peripheral.isLinked()) {
                                    Vector3i pScreenPos = peripheral.getScreenPos();
                                    if (pScreenPos.x == originPos.getX() && pScreenPos.y == originPos.getY() && pScreenPos.z == originPos.getZ()) {
                                        peripheral.setScreenPos(null);
                                        peripheral.setScreenSide(null);
                                    }
                                }
                            });
                        }
                    } catch (Exception e) {
                        WebDisplays.LOGGER.warn("Error clearing peripheral links: {}", e.getMessage());
                    }

                    // If we're not the origin, also remove the origin block entity
                    if (!originPos.equals(p_60517_)) {
                        p_60516_.removeBlockEntity(originPos);
                        p_60516_.setBlock(
                            originPos, p_60516_.getBlockState(originPos).setValue(hasTE, false),
                            11
                        );
                    }
                }
            }
        }
        
        // Mark block entity for removal - saveAdditional will clear data naturally
        if (!p_60516_.isClientSide) {
            if (p_60516_.getBlockEntity(p_60517_) instanceof ScreenBlockEntity be) {
                be.markForRemoval();
                WebDisplays.LOGGER.debug("ScreenBlock: Block marked for removal at {}", p_60517_);
            }
        } else {
            // CLIENT FIX: Clear local JSON cache for this position
            for (BlockSide side : BlockSide.values()) {
                WebDisplays.PROXY.deleteCachedUrl(p_60517_, side);
            }
            WebDisplays.LOGGER.debug("ScreenBlock: Cleared client cache for broken block at {}", p_60517_);
        }

        super.onRemove(p_60515_, p_60516_, p_60517_, p_60518_, p_60519_);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack heldItem = stack;

        boolean isUpgrade = false;
        if (heldItem.isEmpty())
            heldItem = null;
        else if (!(isUpgrade = heldItem.getItem() instanceof IUpgrade))
            return InteractionResult.FAIL;

        if (level.isClientSide) {
            if (hand == InteractionHand.MAIN_HAND && !isUpgrade)
                return InteractionResult.CONSUME;
            return InteractionResult.FAIL;
        }

        if (hand == InteractionHand.OFF_HAND && !isUpgrade)
            return InteractionResult.FAIL;

        InteractionResult result = useWithoutItem(state, level, pos, player, hit);
        return result == InteractionResult.SUCCESS ? InteractionResult.SUCCESS : 
               result == InteractionResult.CONSUME ? InteractionResult.CONSUME :
               result == InteractionResult.PASS ? InteractionResult.PASS :
               InteractionResult.FAIL;
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        ItemStack heldItem = player.getItemInHand(InteractionHand.MAIN_HAND);
        boolean isUpgrade = false;
        if (heldItem.isEmpty())
            heldItem = null; //Easier to work with
        else
            isUpgrade = heldItem.getItem() instanceof IUpgrade;

        // Non-upgrade items should never reach screen creation (matches Forge use() behavior)
        if (!isUpgrade && heldItem != null)
            return InteractionResult.FAIL;

        boolean sneaking = player.isShiftKeyDown();
        Vector3i blockPos = new Vector3i(pos);
        BlockSide side = BlockSide.values()[hit.getDirection().ordinal()];
        Multiblock.findOrigin(world, blockPos, side, null);
        ScreenBlockEntity te = (ScreenBlockEntity) world.getBlockEntity(blockPos.toBlock());

        if (te != null && te.getScreen(side) != null) {
            ScreenData scr = te.getScreen(side);

            if (world.isClientSide) {
                if (sneaking)
                    return InteractionResult.SUCCESS;

                if (heldItem == null) {
                    return InteractionResult.FAIL;
                }

                return InteractionResult.SUCCESS;
            }

            if (sneaking) { //Shift + Right Click - Change URL
            if ((scr.rightsFor(player) & ScreenRights.CHANGE_URL) == 0)
                Util.toast(player, "restrictions");
            else
                (new SetURLData(blockPos, scr.side, scr.url, false, new Vector3i())).sendTo((ServerPlayer) player);

            return InteractionResult.SUCCESS;
        } else if (heldItem != null) {
            if (!te.hasUpgrade(side, heldItem)) {
                if ((scr.rightsFor(player) & ScreenRights.MANAGE_UPGRADES) == 0) {
                    Util.toast(player, "restrictions");
                    return InteractionResult.CONSUME;
                }

                if (te.addUpgrade(side, heldItem, player, false)) {
                    if (!player.isCreative())
                        heldItem.shrink(1);

                    Util.toast(player, ChatFormatting.AQUA, "upgradeOk");
                    if (player instanceof ServerPlayer serverPlayer)
                        WebDisplaysMod.INSTANCE.criterionUpgradeScreen.trigger(serverPlayer);
                } else
                    Util.toast(player, "upgradeError");

                return InteractionResult.CONSUME;
            }

            // Upgrade already exists on this screen side
            return InteractionResult.CONSUME;
            } else {
            if ((scr.rightsFor(player) & ScreenRights.INTERACT) == 0) {
                Util.toast(player, "restrictions");
                return InteractionResult.FAIL;
            }

            Vector2i tmp = new Vector2i();

            float hitX = ((float) hit.getLocation().x) - (float) te.getBlockPos().getX();
            float hitY = ((float) hit.getLocation().y) - (float) te.getBlockPos().getY();
            float hitZ = ((float) hit.getLocation().z) - (float) te.getBlockPos().getZ();

            if (hit2pixels(side, hit.getBlockPos(), new Vector3i(hit.getBlockPos()), scr, hitX, hitY, hitZ, tmp))
                te.click(side, tmp);
                return InteractionResult.CONSUME;
            }
        }
//        else if(sneaking) {
//            Util.toast(player, "turnOn");
//            return InteractionResult.SUCCESS;
//        }

        if (world.isClientSide)
            return InteractionResult.SUCCESS;

        Vector2i size = Multiblock.measure(world, blockPos, side);
        if (size.x < 2 && size.y < 2) {
            Util.toast(player, "tooSmall");
            return InteractionResult.SUCCESS;
        }

        if (size.x > CommonConfig.Screen.maxScreenSizeX || size.y > CommonConfig.Screen.maxScreenSizeY) {
            Util.toast(player, "tooBig", CommonConfig.Screen.maxScreenSizeX, CommonConfig.Screen.maxScreenSizeY);
            return InteractionResult.SUCCESS;
        }

        Vector3i err = Multiblock.check(world, blockPos, size, side);
        if (err != null) {
            Util.toast(player, "invalid", err.toString());
            return InteractionResult.SUCCESS;
        }

        Log.info("Player %s (UUID %s) created a screen at %s of size %dx%d", player.getName(), player.getGameProfile().getId().toString(), blockPos.toString(), size.x, size.y);

        if (te == null) {
            BlockPos bp = blockPos.toBlock();
            world.setBlock(bp, world.getBlockState(bp).setValue(hasTE, true), 3);
            te = (ScreenBlockEntity) world.getBlockEntity(bp);
        }

        te.addScreen(side, size, null, player, true);
        te.load(); // Ensure screen is marked as loaded so it renders
        if (player instanceof ServerPlayer serverPlayer)
            WebDisplaysMod.INSTANCE.criterionRenderScreen.trigger(serverPlayer);
        return InteractionResult.SUCCESS;
    }

    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos source,
                                boolean isMoving) {
        if (block != this && !world.isClientSide && !state.getValue(emitting)) {
            for (BlockSide side : BlockSide.values()) {
                Vector3i vec = new Vector3i(pos);
                Multiblock.findOrigin(world, vec, side, null);

                ScreenBlockEntity tes = (ScreenBlockEntity) world.getBlockEntity(vec.toBlock());
                if (tes != null && tes.hasUpgrade(side, DefaultUpgrade.REDSTONE_INPUT)) {

                    vec.sub(pos.getX(), pos.getY(), pos.getZ()).neg();
//                    tes.updateJSRedstone(side, new Vector2i(vec.dot(side.right), vec.dot(side.up)), world.getSignal(pos, facing));
                }
            }
        }
    }
    
    public static boolean hit2pixels(BlockSide side, BlockPos bpos, Vector3i pos, ScreenData scr, float hitX, float hitY, float hitZ, Vector2i dst) {
        if(side.right.x < 0)
            hitX -= 1.f;

        if(side.right.z < 0 || side == BlockSide.TOP || side == BlockSide.BOTTOM)
            hitZ -= 1.f;

        Vector3f rel = new Vector3f(hitX, hitY, hitZ);

        // this dot is acting as a "get distance from plane" where the plane is the edge of the screen
        float cx = rel.dot(side.right.toFloat());
        float cy = rel.dot(side.up.toFloat());
        float sw = ((float) scr.size.x);
        float sh = ((float) scr.size.y);

        // Adjust for bezel (1/16th of a block on each side = 2/16 total per axis)
        // The visible browser area is inset by 1/16 from each edge
        final float BEZEL = 1.0f / 16.0f;
        cx -= BEZEL;
        cy -= BEZEL;
        sw -= 2.0f * BEZEL;
        sh -= 2.0f * BEZEL;

        cx /= sw;
        cy /= sh;

        if (cx >= 0.f && cx <= 1.0 && cy >= 0.f && cy <= 1.f) {
            if (side != BlockSide.BOTTOM)
                cy = 1.f - cy;

            switch (scr.rotation) {
                case ROT_0:
                    break;

                case ROT_90:
                    cy = 1.0f - cy;
                    break;

                case ROT_180:
                    cx = 1.0f - cx;
                    cy = 1.0f - cy;
                    break;

                case ROT_270:
                    cx = 1.0f - cx;
                    break;
            }

            cx *= (float) scr.resolution.x;
            cy *= (float) scr.resolution.y;

            if (scr.rotation.isVertical) {
                dst.x = (int) cy;
                dst.y = (int) cx;
            } else {
                dst.x = (int) cx;
                dst.y = (int) cy;
            }

            return true;
        }

        return false;
    }

    /************************************************* DESTRUCTION HANDLING *************************************************/

    private void onDestroy(Level world, BlockPos pos, Player ply) {
        Vector3i bp = new Vector3i(pos);
        Multiblock.BlockOverride override = new Multiblock.BlockOverride(bp, Multiblock.OverrideAction.SIMULATE);

        for (BlockSide bs : BlockSide.values())
            destroySide(world, bp.clone(), bs, override, ply);
    }

    private void destroySide(Level world, Vector3i pos, BlockSide side, Multiblock.BlockOverride override, Player
            source) {
        Multiblock.findOrigin(world, pos, side, override);
        BlockPos bp = pos.toBlock();
        BlockEntity te = world.getBlockEntity(bp);

        if (te instanceof ScreenBlockEntity) {
            ((ScreenBlockEntity) te).onDestroy(source);
            if (!world.isClientSide) {
                world.setBlock(bp, world.getBlockState(bp).setValue(hasTE, false), Block.UPDATE_ALL_IMMEDIATE); //Destroy tile entity.
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        onDestroy(level, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void setPlacedBy(Level world, @NotNull BlockPos pos, @NotNull BlockState
            state, @org.jetbrains.annotations.Nullable LivingEntity whoDidThisShit, @NotNull ItemStack stack) {
    }

    /************************************************* STUFF THAT'S UNLIKELY TO BE TOUCHED BUT NEEDS TO BE HERE *************************************************/

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(emitting) ? 15 : 0;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return state.getValue(emitting);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(hasTE) ? new ScreenBlockEntity(pos, state) : null;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(properties);
    }
}
