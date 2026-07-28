package net.montoyo.wd.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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

    private static Item.Properties itemProps(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("webdisplays", name);
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id));
    }
    
    static {
        CONFIGURATOR = register("screencfg", new ItemScreenConfigurator(itemProps("screencfg")));
        OWNERSHIP_THEIF = register("ownerthief", new ItemOwnershipThief(itemProps("ownerthief")));
        LINKER = register("linker", new ItemLinker(itemProps("linker")));
        MINEPAD = register("minepad", new ItemMinePad2(itemProps("minepad")));
        LASER_POINTER = register("laserpointer", new ItemLaserPointer(itemProps("laserpointer")));

        DefaultUpgrade[] defaultUpgrades = DefaultUpgrade.values();
        UPGRADE_ITEMS = new Item[defaultUpgrades.length];
        for (int i = 0; i < defaultUpgrades.length; i++) {
            DefaultUpgrade upgrade = defaultUpgrades[i];
            String name = "upgrade_" + upgrade.name().toLowerCase(Locale.ROOT);
            UPGRADE_ITEMS[i] = register(name, new ItemUpgrade(itemProps(name), upgrade));
        }

        CraftComponent[] components = CraftComponent.values();
        COMP_CRAFT_ITEMS = new Item[components.length];
        for (int i = 0; i < components.length; i++) {
            CraftComponent cc = components[i];
            String name = "component_" + cc.name().toLowerCase(Locale.ROOT);
            COMP_CRAFT_ITEMS[i] = register(name, new ItemCraftComponent(itemProps(name)));
        }

        SCREEN = register("screen", new BlockItem(BlockRegistry.SCREEN_BLOCK, itemProps("screen").useBlockDescriptionPrefix()));
        KEYBOARD = register("keyboard", new KeyboardItem(BlockRegistry.KEYBOARD_BLOCK, itemProps("keyboard").useBlockDescriptionPrefix()));
        REDSTONE_CONTROLLER = register("redstone_control", new BlockItem(BlockRegistry.REDSTONE_CONTROL_BLOCK, itemProps("redstone_control").useBlockDescriptionPrefix()));
        REMOTE_CONTROLLER = register("remote_controller", new BlockItem(BlockRegistry.REMOTE_CONTROLLER_BLOCK, itemProps("remote_controller").useBlockDescriptionPrefix()));
        SERVER = register("server", new BlockItem(BlockRegistry.SERVER_BLOCK, itemProps("server").useBlockDescriptionPrefix()));
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
