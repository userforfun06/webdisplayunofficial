package net.montoyo.wd.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.montoyo.wd.block.KeyboardBlockLeft;
import net.montoyo.wd.block.KeyboardBlockRight;
import net.montoyo.wd.block.PeripheralBlock;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.core.DefaultPeripheral;

public class BlockRegistry {
    public static void init() {
    }

    public static final ScreenBlock SCREEN_BLOCK;
    public static final KeyboardBlockLeft KEYBOARD_BLOCK;
    public static final KeyboardBlockRight blockKbRight;
    public static final PeripheralBlock REDSTONE_CONTROL_BLOCK;
    public static final PeripheralBlock REMOTE_CONTROLLER_BLOCK;
    public static final PeripheralBlock SERVER_BLOCK;
    
    private static <T extends Block> T register(String name, T block) {
        return Registry.register(BuiltInRegistries.BLOCK, ResourceLocation.fromNamespaceAndPath("webdisplays", name), block);
    }

    private static BlockBehaviour.Properties props(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("webdisplays", name);
        return BlockBehaviour.Properties.of()
            .setId(ResourceKey.create(Registries.BLOCK, id));
    }
    
    static {
        SCREEN_BLOCK = register("screen", new ScreenBlock(props("screen")
            .strength(1.5f, 6.0f)
            .requiresCorrectToolForDrops()
            .pushReaction(PushReaction.IGNORE)
            .noLootTable()));
        
        KEYBOARD_BLOCK = register("keyboard", new KeyboardBlockLeft(props("keyboard")
            .strength(1.5f, 6.0f)
            .requiresCorrectToolForDrops()
            .noLootTable()));
        
        blockKbRight = register("keyboard_right", new KeyboardBlockRight(props("keyboard_right")
            .strength(1.5f, 10.f)
            .requiresCorrectToolForDrops()
            .noLootTable()));
        
        REDSTONE_CONTROL_BLOCK = register("redstone_control", new PeripheralBlock(props("redstone_control")
            .strength(1.5f, 6.0f)
            .requiresCorrectToolForDrops()
            .noLootTable(), DefaultPeripheral.REDSTONE_CONTROLLER));
        
        REMOTE_CONTROLLER_BLOCK = register("remote_controller", new PeripheralBlock(props("remote_controller")
            .strength(1.5f, 6.0f)
            .requiresCorrectToolForDrops()
            .noLootTable(), DefaultPeripheral.REMOTE_CONTROLLER));
        
        SERVER_BLOCK = register("server", new PeripheralBlock(props("server")
            .strength(1.5f, 6.0f)
            .requiresCorrectToolForDrops()
            .noLootTable(), DefaultPeripheral.SERVER));
    }
}
