/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.net.client_bound;

import net.montoyo.wd.net.NetworkEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.montoyo.wd.WebDisplaysMod;
import net.montoyo.wd.controls.ScreenControl;
import net.montoyo.wd.controls.ScreenControlRegistry;
import net.montoyo.wd.controls.builtin.*;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.net.BufferUtils;
import net.montoyo.wd.net.AbstractPacket;
import net.montoyo.wd.utilities.math.Vector2i;
import net.montoyo.wd.utilities.math.Vector3i;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.data.Rotation;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;

public class S2CMessageScreenUpdate extends AbstractPacket {
    ScreenControl control;
    BlockPos pos;
    BlockSide side;
    
    public S2CMessageScreenUpdate(BlockPos blockPos, BlockSide side) {
        this.pos = blockPos;
        this.side = side;
    }

    public S2CMessageScreenUpdate(FriendlyByteBuf buf) {
        super(buf);
    
        pos = buf.readBlockPos();
        side = (BlockSide) BufferUtils.readEnum(buf, (i) -> BlockSide.values()[i], (byte) 1);
    
        this.control = ScreenControlRegistry.parse(buf);
    }
    
    public static S2CMessageScreenUpdate setURL(ScreenBlockEntity screen, BlockSide side, String weburl) {
        S2CMessageScreenUpdate screenUpdate = new S2CMessageScreenUpdate(screen.getBlockPos(), side);
        screenUpdate.control = new SetURLControl(weburl, new Vector3i(screenUpdate.pos));
        return screenUpdate;
    }
    
    public static S2CMessageScreenUpdate setResolution(ScreenBlockEntity screen, BlockSide side, Vector2i res) {
        S2CMessageScreenUpdate screenUpdate = new S2CMessageScreenUpdate(screen.getBlockPos(), side);
        screenUpdate.control = new ScreenModifyControl(res);
        return screenUpdate;
    }
    
    public static S2CMessageScreenUpdate rotation(ScreenBlockEntity screen, BlockSide side, Rotation rot) {
        S2CMessageScreenUpdate screenUpdate = new S2CMessageScreenUpdate(screen.getBlockPos(), side);
        screenUpdate.control = new ScreenModifyControl(rot);
        return screenUpdate;
    }
    
    public static S2CMessageScreenUpdate upgrade(ScreenBlockEntity screen, BlockSide side, boolean adding, ItemStack stack) {
        S2CMessageScreenUpdate screenUpdate = new S2CMessageScreenUpdate(screen.getBlockPos(), side);
        screenUpdate.control = new ManageRightsAndUpgradesControl(adding, stack);
        return screenUpdate;
    }
    
    public static S2CMessageScreenUpdate click(ScreenBlockEntity screen, BlockSide side, ClickControl.ControlType mouseMove, Vector2i pos) {
        return click(screen, side, mouseMove, pos, -1);
    }

    public static S2CMessageScreenUpdate click(ScreenBlockEntity screen, BlockSide side, ClickControl.ControlType mouseMove, Vector2i pos, int button) {
        S2CMessageScreenUpdate screenUpdate = new S2CMessageScreenUpdate(screen.getBlockPos(), side);
        screenUpdate.control = new ClickControl(mouseMove, pos, button);
        return screenUpdate;
    }
    
    public static S2CMessageScreenUpdate type(ScreenBlockEntity screen, BlockSide side, String text) {
        S2CMessageScreenUpdate screenUpdate = new S2CMessageScreenUpdate(screen.getBlockPos(), side);
        screenUpdate.control = new KeyTypedControl(text, screenUpdate.pos);
        return screenUpdate;
    }
    
    public static S2CMessageScreenUpdate autoVolume(ScreenBlockEntity screen, BlockSide side, boolean av) {
        S2CMessageScreenUpdate screenUpdate = new S2CMessageScreenUpdate(screen.getBlockPos(), side);
        screenUpdate.control = new AutoVolumeControl(av);
        return screenUpdate;
    }
    
    public static S2CMessageScreenUpdate owner(ScreenBlockEntity screen, BlockSide side, NameUUIDPair owner) {
        S2CMessageScreenUpdate screenUpdate = new S2CMessageScreenUpdate(screen.getBlockPos(), side);
        screenUpdate.control = new OwnerControl(owner);
        return screenUpdate;
    }

    public static S2CMessageScreenUpdate turnOff(BlockPos blockPos, BlockSide side) {
        S2CMessageScreenUpdate screenUpdate = new S2CMessageScreenUpdate(blockPos, side);
        screenUpdate.control = TurnOffControl.INSTANCE;
        return screenUpdate;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        BufferUtils.writeEnum(buf, side, (byte) 1);

        buf.writeUtf(control.getId().toString());
        control.write(buf);
    }
	
	@Override
	public ResourceLocation getId() {
		return ResourceLocation.fromNamespaceAndPath("webdisplays", "screenupdate");
	}
	
    @Override
    public void handle(Player player) {
        if (player.level().isClientSide) {
            WebDisplaysMod.PROXY.handleScreenControl(pos, side, control);
        }
    }

    public void handle(NetworkEvent.Context ctx) {
        if (checkClient(ctx)) {
            ctx.enqueueWork(() -> {
                // Delegate to client proxy - control handling happens there
                WebDisplaysMod.PROXY.handleScreenControl(pos, side, control);
            });
            ctx.setPacketHandled(true);
        }
    }
}
