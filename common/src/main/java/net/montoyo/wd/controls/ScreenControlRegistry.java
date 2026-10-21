package net.montoyo.wd.controls;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.montoyo.wd.controls.builtin.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ScreenControlRegistry {
	private static final Map<ResourceLocation, Function<FriendlyByteBuf, ScreenControl>> CONTROL_FACTORIES = new HashMap<>();
	
	static {
		// Register all control types
		register(ClickControl.ID, ClickControl::new);
		register(SetURLControl.ID, SetURLControl::new);
		register(LaserControl.ID, LaserControl::new);
		register(ScreenModifyControl.ID, ScreenModifyControl::new);
		register(ManageRightsAndUpgradesControl.ID, ManageRightsAndUpgradesControl::new);
		register(KeyTypedControl.ID, KeyTypedControl::new);
		register(AutoVolumeControl.ID, AutoVolumeControl::new);
		register(OwnerControl.ID, OwnerControl::new);
		register(TurnOffControl.ID, TurnOffControl::new);
		register(JSRequestControl.ID, JSRequestControl::new);
		register(ModifyFriendListControl.ID, ModifyFriendListControl::new);
	}
	
	public static void register(ResourceLocation id, Function<FriendlyByteBuf, ScreenControl> factory) {
		CONTROL_FACTORIES.put(id, factory);
	}
	
	public static ScreenControl parse(FriendlyByteBuf buf) {
		ResourceLocation id = ResourceLocation.tryParse(buf.readUtf());
		Function<FriendlyByteBuf, ScreenControl> factory = CONTROL_FACTORIES.get(id);
		if (factory != null) {
			return factory.apply(buf);
		}
		return null;
	}
}
