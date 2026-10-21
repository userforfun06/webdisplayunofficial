package net.montoyo.wd.net;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.montoyo.wd.net.client_bound.*;
import net.montoyo.wd.net.server_bound.*;

import java.util.HashMap;
import java.util.function.Function;

public class PacketFactory {
    private static final HashMap<ResourceLocation, Function<RegistryFriendlyByteBuf, Packet>> FACTORY_MAP = new HashMap<>();

    static {
        // Client-bound packets (S2C)
        register("serverinfo", S2CMessageServerInfo::new);
        register("miniservconnect", C2SMessageMiniservConnect::new);
        register("miniservkey", S2CMessageMiniservKey::new);
        register("closegui", S2CMessageCloseGui::new);
        register("opengui", S2CMessageOpenGui::new);
        register("addscreen", S2CMessageAddScreen::new);
        register("screenctrl", C2SMessageScreenCtrl::new);
        register("screenupdate", S2CMessageScreenUpdate::new);
        register("redstonectrl", C2SMessageRedstoneCtrl::new);
        register("acquery", C2SMessageACQuery::new);
        register("acresult", S2CMessageACResult::new);
        register("jsresponse", S2CMessageJSResponse::new);
        register("minepadurl", C2SMessageMinepadUrl::new);
        register("redstoneoutput", C2SMessageRedstoneOutput::new);
        register("screen_config", S2CMessageScreenConfig::new);
    }

    public static void register(ResourceLocation id, Function<RegistryFriendlyByteBuf, Packet> factory) {
        FACTORY_MAP.put(id, factory);
    }
    
    // Backward compatible String-based register
    private static void register(String name, Function<RegistryFriendlyByteBuf, Packet> factory) {
        FACTORY_MAP.put(ResourceLocation.fromNamespaceAndPath("webdisplays", name), factory);
    }

    public static Packet create(ResourceLocation id, RegistryFriendlyByteBuf buf) {
        Function<RegistryFriendlyByteBuf, Packet> factory = FACTORY_MAP.get(id);
        if (factory != null) {
            return factory.apply(buf);
        }
        return null;
    }
}
