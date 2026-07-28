package net.montoyo.wd;

import com.google.gson.Gson;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebDisplays implements ModInitializer {
    public static final String MOD_ID = "webdisplays";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Gson GSON = new Gson();
    public static WebDisplays INSTANCE;
    
    public static final String BLACKLIST_URL = "about:blank";
    
    public static SharedProxy PROXY = new SharedProxy();
    
    public static double unloadDistance2 = 1024.0;   // 32 blocks (config target)
    public static double loadDistance2 = 900.0;      // 30 blocks (config target)
    
    // Miniserv server settings
    public int miniservPort = 25566;
    public long miniservQuota = 104857600L; // 100 MB default
    
    // Sound events
    public SoundEvent soundTyping;
    public SoundEvent soundUpgradeAdd;
    public SoundEvent soundUpgradeDel;
    public SoundEvent soundScreenCfg;
    public SoundEvent soundServer;
    public SoundEvent soundIronic;
    
    @Override
    public void onInitialize() {
        INSTANCE = this;
        
        // Register sounds
        registerSounds();
        
        LOGGER.info("WebDisplays initialized!");
    }
    
    private void registerSounds() {
        soundTyping = registerSound("keyboard_type");
        soundUpgradeAdd = registerSound("upgrade_add");
        soundUpgradeDel = registerSound("upgrade_del");
        soundScreenCfg = registerSound("screencfg_open");
        soundServer = registerSound("server");
        soundIronic = registerSound("ironic");
    }
    
    private SoundEvent registerSound(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MOD_ID, name);
        SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(id);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, soundEvent);
    }
    
    public static String applyBlacklist(String url) {
        return url;
    }
    
    public static boolean isSiteBlacklisted(String url) {
        return false;
    }
}
