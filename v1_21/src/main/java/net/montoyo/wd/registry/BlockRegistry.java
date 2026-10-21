package net.montoyo.wd.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.core.registries.BuiltInRegistries;
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
    
    static {
        SCREEN_BLOCK = register("screen", new ScreenBlock(BlockBehaviour.Properties.of()
            .strength(1.5f, 6.0f)
            .requiresCorrectToolForDrops()
            .pushReaction(PushReaction.IGNORE)));

        KEYBOARD_BLOCK = register("keyboard", new KeyboardBlockLeft());
        
        blockKbRight = register("keyboard_right", new KeyboardBlockRight());
        
        REDSTONE_CONTROL_BLOCK = register("redstone_control", new PeripheralBlock(DefaultPeripheral.REDSTONE_CONTROLLER));
        
        REMOTE_CONTROLLER_BLOCK = register("remote_controller", new PeripheralBlock(DefaultPeripheral.REMOTE_CONTROLLER));
        
        SERVER_BLOCK = register("server", new PeripheralBlock(DefaultPeripheral.SERVER));
    }
}
