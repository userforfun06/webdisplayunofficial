package net.montoyo.wd.registry;

import net.minecraft.core.Registry;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.montoyo.wd.entity.*;

public class TileRegistry {
    public static void init() {
        // Register all block entity types
        SCREEN_BLOCK_ENTITY = register("screen", BlockEntityType.Builder
                .of(ScreenBlockEntity::new, BlockRegistry.SCREEN_BLOCK));

        KEYBOARD = register("keyboard", BlockEntityType.Builder
                .of(KeyboardBlockEntity::new, BlockRegistry.KEYBOARD_BLOCK));

        REMOTE_CONTROLLER = register("remote_controller", BlockEntityType.Builder
                .of(RemoteControlBlockEntity::new, BlockRegistry.REMOTE_CONTROLLER_BLOCK));

        REDSTONE_CONTROLLER = register("redstone_controller", BlockEntityType.Builder
                .of(RedstoneControlBlockEntity::new, BlockRegistry.REDSTONE_CONTROL_BLOCK));

        SERVER = register("server", BlockEntityType.Builder
                .of(ServerBlockEntity::new, BlockRegistry.SERVER_BLOCK));
    }

    public static BlockEntityType<ScreenBlockEntity> SCREEN_BLOCK_ENTITY;
    public static BlockEntityType<KeyboardBlockEntity> KEYBOARD;
    public static BlockEntityType<RemoteControlBlockEntity> REMOTE_CONTROLLER;
    public static BlockEntityType<RedstoneControlBlockEntity> REDSTONE_CONTROLLER;
    public static BlockEntityType<ServerBlockEntity> SERVER;

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> register(String name, BlockEntityType.Builder<T> builder) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("webdisplays", name);
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, builder.build(null));
    }
}
