/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Ocelot;
import net.montoyo.wd.WebDisplaysMod;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.data.KeyboardData;
import net.montoyo.wd.registry.TileRegistry;
import net.montoyo.wd.utilities.serialization.Util;

public class KeyboardBlockEntity extends AbstractPeripheralBlockEntity {
    private static final String RANDOM_CHARS = "AZERTYUIOPQSDFGHJKLMWXCVBNazertyuiopqsdfghjklmwxcvbn0123456789"; //Yes I have an AZERTY keyboard, u care?

    public KeyboardBlockEntity(BlockPos arg2, BlockState arg3) {
        super(TileRegistry.KEYBOARD, arg2, arg3);
    }

    public InteractionResult onRightClick(Player player, InteractionHand hand) {
        if(level.isClientSide)
            return InteractionResult.SUCCESS;

        if(!isLinked()) {
            Util.toast(player, "notLinked");
            return InteractionResult.SUCCESS;
        }

        if(!isScreenChunkLoaded()) {
            Util.toast(player, "chunkUnloaded");
            return InteractionResult.SUCCESS;
        }

        ScreenBlockEntity tes = getConnectedScreenEx();
        if(tes == null) {
            Util.toast(player, "notLinked");
            return InteractionResult.SUCCESS;
        }

        ScreenData scr = tes.getScreen(screenSide);
        if((scr.rightsFor(player) & ScreenRights.INTERACT) == 0) {
            Util.toast(player, "restrictions");
            return InteractionResult.SUCCESS;
        }

        (new KeyboardData(tes, screenSide, getBlockPos())).sendTo((ServerPlayer) player);
        return InteractionResult.SUCCESS;
    }

    public void simulateCat(Entity ent) {
        if(!isScreenChunkLoaded())
            return;
        
        ScreenBlockEntity tes = getConnectedScreenEx();

        if(tes != null) {
            ScreenData scr = tes.getScreen(screenSide);
            if (scr == null)
                return;
            
            boolean ok;

            if(ent instanceof Player)
                ok = (scr.rightsFor((Player) ent) & ScreenRights.INTERACT) != 0;
            else
                ok = (scr.otherRights & ScreenRights.INTERACT) != 0;

            if(ok) {
                char rnd = RANDOM_CHARS.charAt((int) (Math.random() * ((double) RANDOM_CHARS.length())));
                tes.type(screenSide, "t" + rnd, getBlockPos());

                Player owner = level.getPlayerByUUID(scr.owner.uuid);
                if(owner instanceof ServerPlayer && ent instanceof Ocelot)
                    WebDisplaysMod.INSTANCE.criterionKeyboardCat.trigger((ServerPlayer) owner);
            }
        }
    }

    @Override
    public void onNeighborChange(net.minecraft.world.level.block.Block neighborType, net.minecraft.core.BlockPos neighborPos) {
        // Keyboard doesn't react to neighbor changes
    }
}
