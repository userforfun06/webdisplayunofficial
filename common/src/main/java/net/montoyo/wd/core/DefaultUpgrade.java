package net.montoyo.wd.core;

import net.minecraft.world.item.ItemStack;

public enum DefaultUpgrade {
    LASERMOUSE,
    REDSTONE_INPUT,
    REDSTONE_OUTPUT;

    public boolean matches(ItemStack is, DefaultUpgrade upgrade) {
        if (is == null || is.isEmpty()) return false;
        if (is.getItem() instanceof net.montoyo.wd.item.ItemUpgrade upgradeItem)
            return upgradeItem.type == upgrade;
        return false;
    }

    // Helper methods for specific upgrade types (used as method references)
    public boolean matchesLaserMouse(ItemStack is) {
        return matches(is, LASERMOUSE);
    }

    public boolean matchesRedInput(ItemStack is) {
        return matches(is, REDSTONE_INPUT);
    }

    public boolean matchesRedOutput(ItemStack is) {
        return matches(is, REDSTONE_OUTPUT);
    }

    public static boolean matchesUpgrade(ItemStack is, DefaultUpgrade upgrade) {
        return upgrade.matches(is, upgrade);
    }
}
