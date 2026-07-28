/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.core;

import net.minecraft.world.item.ItemStack;
import net.montoyo.wd.registry.ItemRegistry;

public enum CraftComponent {
    STONEKEY("stonekey", "StoneKey"),
    UPGRADE("upgrade", "Upgrade"),
    PERIPHERAL("peripheral", "Peripheral"),
    BATCELL("batcell", "BatCell"),
    BATPACK("batpack", "BatPack"),
    LASERDIODE("laserdiode", "LaserDiode"),
    BACKLIGHT("backlight", "Backlight"),
    EXTCARD("extcard", "ExtCard"),
    BADEXTCARD("badextcard", "BadExtCard");

    private final String name;

    CraftComponent(String n, String wikiName) {
        name = n;
    }

    @Override
    public String toString() {
        return name;
    }

    public ItemStack makeItemStack() {
        return new ItemStack(ItemRegistry.getComputerCraftItem(ordinal()), 1);
    }
}
