package net.montoyo.wd.registry;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.montoyo.wd.entity.*;

public class TileRegistry {
    public static void init() {
        // Register all block entity types
        SCREEN_BLOCK_ENTITY = register("screen",
                FabricBlockEntityTypeBuilder.create(ScreenBlockEntity::new, BlockRegistry.SCREEN_BLOCK).build());

        KEYBOARD = register("keyboard",
                FabricBlockEntityTypeBuilder.create(KeyboardBlockEntity::new, BlockRegistry.KEYBOARD_BLOCK).build());

        REMOTE_CONTROLLER = register("remote_controller",
                FabricBlockEntityTypeBuilder.create(RemoteControlBlockEntity::new, BlockRegistry.REMOTE_CONTROLLER_BLOCK).build());

        REDSTONE_CONTROLLER = register("redstone_controller",
                FabricBlockEntityTypeBuilder.create(RedstoneControlBlockEntity::new, BlockRegistry.REDSTONE_CONTROL_BLOCK).build());

        SERVER = register("server",
                FabricBlockEntityTypeBuilder.create(ServerBlockEntity::new, BlockRegistry.SERVER_BLOCK).build());
    }

    public static BlockEntityType<ScreenBlockEntity> SCREEN_BLOCK_ENTITY;
    public static BlockEntityType<KeyboardBlockEntity> KEYBOARD;
    public static BlockEntityType<RemoteControlBlockEntity> REMOTE_CONTROLLER;
    public static BlockEntityType<RedstoneControlBlockEntity> REDSTONE_CONTROLLER;
    public static BlockEntityType<ServerBlockEntity> SERVER;

    private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType<T> type) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("webdisplays", name);
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, type);
    }
}
