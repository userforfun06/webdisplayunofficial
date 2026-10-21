package net.montoyo.wd.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.montoyo.wd.WebDisplaysMod;
import net.montoyo.wd.config.CommonConfig;
import net.minecraft.ChatFormatting;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.server_bound.C2SMessageMinepadUrl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class ItemMinePad2 extends Item implements WDItem {

    public ItemMinePad2(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("webdisplays.minepad.turnon").withStyle(ChatFormatting.GRAY));
        WDItem.addInformation(tooltip);
    }

    private static String getURL(ItemStack is) {
        CustomData customData = is.get(DataComponents.CUSTOM_DATA);
        if (customData == null || !customData.copyTag().contains("PadURL")) {
            return CommonConfig.Browser.homepage;
        } else {
            return customData.copyTag().getString("PadURL");
        }
    }

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(Level world, Player ply, @NotNull InteractionHand hand) {
        ItemStack is = ply.getItemInHand(hand);
        boolean ok;
        if (ply.isShiftKeyDown()) {
            if (world.isClientSide) {
                WebDisplaysMod.PROXY.displaySetPadURLGui(is, getURL(is));
            }
            ok = true;
        } else {
            onUse(world, ply, is);
            ok = true;
        }
        if (ok) {
            ply.swing(hand);
        }
        return new InteractionResultHolder<>(ok ? InteractionResult.SUCCESS : InteractionResult.PASS, is);
    }

	private void onUse(Level world, Player ply, ItemStack is) {
		if (world.isClientSide) {
			CustomData customData = is.get(DataComponents.CUSTOM_DATA);
			if (customData != null && customData.copyTag().contains("PadID")) {
				WebDisplaysMod.PROXY.openMinePadGui(customData.copyTag().getUUID("PadID"));
			} else {
				UUID id = UUID.randomUUID();
				String url = getURL(is);
				CompoundTag tag = new CompoundTag();
				tag.putString("PadURL", url);
				tag.putUUID("PadID", id);
				is.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
				WDNetworkRegistry.sendToServer(new C2SMessageMinepadUrl(id, url));
			}
		}
	}

    @Nullable
    @Override
    public String getWikiName(@NotNull ItemStack is) {
        return "MinePad";
    }
}
