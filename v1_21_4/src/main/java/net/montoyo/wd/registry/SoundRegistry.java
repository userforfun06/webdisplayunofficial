package net.montoyo.wd.registry;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.montoyo.wd.WebDisplaysMod;

public class SoundRegistry {
    
    public static SoundEvent KEYBOARD_TYPING;
    
    public static void init() {
        KEYBOARD_TYPING = register("keyboard_type");
    }
    
    private static SoundEvent register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(WebDisplaysMod.MOD_ID, name);
        if (BuiltInRegistries.SOUND_EVENT.containsKey(id)) {
            return BuiltInRegistries.SOUND_EVENT.get(id).get().value();
        }
        SoundEvent sound = SoundEvent.createVariableRangeEvent(id);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, sound);
    }
}
