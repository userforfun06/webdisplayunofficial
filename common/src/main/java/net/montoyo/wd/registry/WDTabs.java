/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.registry;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;

public class WDTabs {
	public static final CreativeModeTab MAIN_TAB = FabricItemGroup.builder()
			.title(Component.translatable("itemGroup.webdisplays"))
			.icon(() -> new ItemStack(ItemRegistry.SCREEN))
			.displayItems((params, output) -> {
				// core items
				output.accept(ItemRegistry.SCREEN);
				output.accept(ItemRegistry.KEYBOARD);
				output.accept(ItemRegistry.LINKER);
				// remote control
				output.accept(ItemRegistry.REMOTE_CONTROLLER);
				// redstone stuff
				output.accept(ItemRegistry.REDSTONE_CONTROLLER);
				// admin tools
				output.accept(ItemRegistry.OWNERSHIP_THEIF);
				// tool items
				output.accept(ItemRegistry.SERVER);
				output.accept(ItemRegistry.CONFIGURATOR);
				output.accept(ItemRegistry.MINEPAD);
				output.accept(ItemRegistry.LASER_POINTER);
				
				// upgrades
				for (int i = 0; i < ItemRegistry.countUpgrades(); i++) output.accept(ItemRegistry.getUpgradeItem(i));
				// cc
				for (int i = 0; i < ItemRegistry.countCompCraftItems(); i++) output.accept(ItemRegistry.getComputerCraftItem(i));
			})
			.build();
	
	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ResourceLocation.fromNamespaceAndPath("webdisplays", "main"), MAIN_TAB);
	}
}
