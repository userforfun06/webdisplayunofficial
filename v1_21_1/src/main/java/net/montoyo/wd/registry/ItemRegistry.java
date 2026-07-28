package net.montoyo.wd.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.montoyo.wd.block.item.KeyboardItem;
import net.montoyo.wd.core.CraftComponent;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.item.*;

import java.util.Locale;

public class ItemRegistry {
    public static void init() {
    }

    protected static final Item[] COMP_CRAFT_ITEMS;
    protected static final Item[] UPGRADE_ITEMS;

    public static final Item CONFIGURATOR;
    public static final Item OWNERSHIP_THEIF;
    public static final Item LINKER;
    public static final Item MINEPAD;
    public static final Item LASER_POINTER;

    public static final Item SCREEN;
    public static final Item KEYBOARD;
    public static final Item REDSTONE_CONTROLLER;
    public static final Item REMOTE_CONTROLLER;
    public static final Item SERVER;
    
    private static Item register(String name, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("webdisplays", name), item);
    }
    
    static {
        CONFIGURATOR = register("screencfg", new ItemScreenConfigurator(new Item.Properties()));
        OWNERSHIP_THEIF = register("ownerthief", new ItemOwnershipThief(new Item.Properties()));
        LINKER = register("linker", new ItemLinker(new Item.Properties()));
        MINEPAD = register("minepad", new ItemMinePad2(new Item.Properties()));
        LASER_POINTER = register("laserpointer", new ItemLaserPointer(new Item.Properties()));

        DefaultUpgrade[] defaultUpgrades = DefaultUpgrade.values();
        UPGRADE_ITEMS = new Item[defaultUpgrades.length];
        for (int i = 0; i < defaultUpgrades.length; i++) {
            DefaultUpgrade upgrade = defaultUpgrades[i];
            UPGRADE_ITEMS[i] = register("upgrade_" + upgrade.name().toLowerCase(Locale.ROOT), new ItemUpgrade(upgrade));
        }

        CraftComponent[] components = CraftComponent.values();
        COMP_CRAFT_ITEMS = new Item[components.length];
        for (int i = 0; i < components.length; i++) {
            CraftComponent cc = components[i];
            COMP_CRAFT_ITEMS[i] = register("component_" + cc.name().toLowerCase(Locale.ROOT), new ItemCraftComponent(new Item.Properties()));
        }

        SCREEN = register("screen", new BlockItem(BlockRegistry.SCREEN_BLOCK, new Item.Properties()));
        KEYBOARD = register("keyboard", new KeyboardItem(BlockRegistry.KEYBOARD_BLOCK, new Item.Properties()));
        REDSTONE_CONTROLLER = register("redstone_control", new BlockItem(BlockRegistry.REDSTONE_CONTROL_BLOCK, new Item.Properties()));
        REMOTE_CONTROLLER = register("remote_controller", new BlockItem(BlockRegistry.REMOTE_CONTROLLER_BLOCK, new Item.Properties()));
        SERVER = register("server", new BlockItem(BlockRegistry.SERVER_BLOCK, new Item.Properties()));
    }

    public static Item getComputerCraftItem(int index) {
        return COMP_CRAFT_ITEMS[index];
    }

    public static Item getUpgradeItem(int index) {
        return UPGRADE_ITEMS[index];
    }

    public static int countCompCraftItems() {
        return COMP_CRAFT_ITEMS.length;
    }

    public static int countUpgrades() {
        return UPGRADE_ITEMS.length;
    }

    public static boolean isCompCraftItem(Item item) {
        for (Item craftItem : COMP_CRAFT_ITEMS)
            if (item == craftItem)
                return true;
        return false;
    }
}
