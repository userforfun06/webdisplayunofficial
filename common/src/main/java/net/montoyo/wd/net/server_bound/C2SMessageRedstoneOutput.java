package net.montoyo.wd.net.server_bound;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.net.AbstractPacket;
import net.montoyo.wd.net.NetworkEvent;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.math.Vector3i;

public class C2SMessageRedstoneOutput extends AbstractPacket implements Runnable {
    public enum Op {
        SET,
        CLEAR,
        CLEAR_ALL
    }

    private ServerPlayer player;
    private BlockPos pos;
    private BlockSide side;
    private Op op;
    private int x;
    private int y;
    private boolean state;

    public C2SMessageRedstoneOutput() {
    }

    public C2SMessageRedstoneOutput(BlockPos pos, BlockSide side, int x, int y, boolean state) {
        this.pos = pos;
        this.side = side;
        this.op = Op.SET;
        this.x = x;
        this.y = y;
        this.state = state;
    }

    public C2SMessageRedstoneOutput(BlockPos pos, BlockSide side, boolean clearAll) {
        this.pos = pos;
        this.side = side;
        this.op = clearAll ? Op.CLEAR_ALL : Op.CLEAR;
    }

    public C2SMessageRedstoneOutput(FriendlyByteBuf buf) {
        super(buf);
        pos = buf.readBlockPos();
        side = BlockSide.values()[buf.readByte()];
        op = Op.values()[buf.readByte()];
        if (op == Op.SET) {
            x = buf.readInt();
            y = buf.readInt();
            state = buf.readBoolean();
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByte(side.ordinal());
        buf.writeByte(op.ordinal());
        if (op == Op.SET) {
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeBoolean(state);
        }
    }

    @Override
    public ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath("webdisplays", "redstoneoutput");
    }

    @Override
    public void run() {
        Level level = player.level();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ScreenBlockEntity tes)) return;

        ScreenData scr = tes.getScreen(side);
        if (scr == null) return;

        // Check ownership
        if (scr.owner == null || !scr.owner.uuid.equals(player.getGameProfile().getId())) return;

        // Check upgrade
        if (!tes.hasUpgrade(side, DefaultUpgrade.REDSTONE_OUTPUT)) return;

        if (op == Op.CLEAR_ALL || op == Op.CLEAR) {
            final Vector3i vec1 = new Vector3i(pos);

            for (int y = 0; y < scr.size.y; y++) {
                Vector3i vec2 = vec1.clone();
                for (int x = 0; x < scr.size.x; x++) {
                    BlockPos bp = vec2.toBlock();
                    BlockState bs = level.getBlockState(bp);
                    if (bs.getValue(ScreenBlock.emitting)) {
                        level.setBlock(bp, bs.setValue(ScreenBlock.emitting, false), Block.UPDATE_ALL_IMMEDIATE);
                    }
                    vec2.add(side.right);
                }
                vec1.add(side.up);
            }

            if (op == Op.CLEAR) {
                // Single clear already handled by the loop - same as clearAll
            }
        } else if (op == Op.SET) {
            if (x < 0 || x >= scr.size.x || y < 0 || y >= scr.size.y) return;

            BlockPos bp = (new Vector3i(pos)).addMul(side.right, x).addMul(side.up, y).toBlock();
            BlockState bs = level.getBlockState(bp);

            if (bs.getValue(ScreenBlock.emitting) != state) {
                level.setBlock(bp, bs.setValue(ScreenBlock.emitting, state), Block.UPDATE_ALL_IMMEDIATE);
            }
        }
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        if (checkServer(ctx)) {
            player = ctx.getSender();
            ctx.enqueueWork(this);
            ctx.setPacketHandled(true);
        }
    }

    @Override
    public void handle(Player player) {
        if (player.level().isClientSide) return;
        this.player = (ServerPlayer) player;
        run();
    }
}
