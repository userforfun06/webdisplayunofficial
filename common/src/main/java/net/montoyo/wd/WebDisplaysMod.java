package net.montoyo.wd;

import com.google.gson.Gson;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.Entity;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.montoyo.wd.core.*;
import net.montoyo.wd.miniserv.server.Server;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.client_bound.S2CMessageAddScreen;
import net.montoyo.wd.net.client_bound.S2CMessageServerInfo;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.config.ClientConfig;
import net.montoyo.wd.config.CommonConfig;
import net.montoyo.wd.registry.BlockRegistry;
import net.montoyo.wd.registry.ItemRegistry;
import net.montoyo.wd.registry.TileRegistry;
import net.montoyo.wd.registry.WDTabs;
import net.montoyo.wd.registry.SoundRegistry;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.serialization.Util;
import net.montoyo.wd.utilities.data.BlockSide;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
public class WebDisplaysMod implements ModInitializer {
    public static WebDisplaysMod INSTANCE;

    public static SharedProxy PROXY = null;
    
    // Vulkan rendering flag - true if VulkanMod is loaded and initialized successfully
    public static boolean useVulkan = false;
    
    public static final String MOD_ID = "webdisplays";
    public static final String BLACKLIST_URL = "mod://webdisplays/blacklisted.html";
    public static final Gson GSON = new Gson();
    public static final ResourceLocation CAPABILITY = ResourceLocation.fromNamespaceAndPath("webdisplays", "customdatacap");

    public SoundEvent soundTyping;
    public SoundEvent soundUpgradeAdd;
    public SoundEvent soundUpgradeDel;
    public SoundEvent soundScreenCfg;
    public SoundEvent soundServer;
    public SoundEvent soundIronic;

    public Criterion criterionPadBreak;
    public Criterion criterionUpgradeScreen;
    public Criterion criterionLinkPeripheral;
    public Criterion criterionKeyboardCat;
    public Criterion criterionRenderScreen;

    public static final double PAD_RATIO = 59.0 / 30.0;
    public double padResX;
    public double padResY;
    private int lastPadId = 0;
    public double unloadDistance2;
    public double loadDistance2;
    public int miniservPort;
    public long miniservQuota;
    public float ytVolume;
    public float avDist100;
    public float avDist0;
    
    private boolean joinMessage;

    ArrayList<ResourceKey<Level>> serverStartedDimensions = new ArrayList<>();
    private MinecraftServer serverInstance;
    private ServerUrlCache serverUrlCache;

    @Override
    public void onInitialize() {
        // Check for VulkanMod - optional dependency with graceful fallback
        // Note: This check cannot be done reliably in the main initializer on dedicated servers
        // The client-side code will handle VulkanMod detection properly
        useVulkan = false;

        // Check if MCEF library is available
        try {
            Class.forName("com.cinemamod.mcef.MCEF");
        } catch (ClassNotFoundException e) {
            Log.error("WebDisplays requires MCEF");
        }

        Log.info("WebDisplays mod initializing...");
        
        INSTANCE = this;
        
        // Initialize config defaults (AnnoCFG is a stub, so call postLoad() manually)
        ClientConfig.init();
        ClientConfig.postLoad();
        padResY = ClientConfig.padResolution;
        padResX = padResY * PAD_RATIO;
        miniservPort = 25566;
        miniservQuota = 104857600L;
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER) {
            PROXY = new SharedProxy();
            // Also update WebDisplays.PROXY for backward compatibility
            WebDisplays.PROXY = PROXY;
            Log.info("Initialized SharedProxy on server");
        } else {
            WebDisplays.LOGGER.debug("Skipping SharedProxy initialization on client - ClientProxy will handle it");
        }
        
        CommonConfig.init();
        miniservPort = CommonConfig.MiniServ.miniservPort;
        miniservQuota = CommonConfig.MiniServ.miniservQuota;
        joinMessage = true; // Default value
        
        criterionPadBreak = new Criterion("pad_break");
        criterionUpgradeScreen = new Criterion("upgrade_screen");
        criterionLinkPeripheral = new Criterion("link_peripheral");
        criterionKeyboardCat = new Criterion("keyboard_cat");
        criterionRenderScreen = new Criterion("render_screen");
        registerTrigger(criterionPadBreak, criterionUpgradeScreen, criterionLinkPeripheral, criterionKeyboardCat, criterionRenderScreen);

        BlockRegistry.init();
        ItemRegistry.init();
        TileRegistry.init();
        WDTabs.init();
        SoundRegistry.init();
        
        WDNetworkRegistry.init();
        
        // Only call PROXY lifecycle methods if PROXY is initialized (server side)
        if (PROXY != null) {
            PROXY.preInit();
            PROXY.init();
            PROXY.postInit();
        }

        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(this::onServerStopping);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> this.onPlayerJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> this.onPlayerLeave(handler.getPlayer()));
        ServerWorldEvents.LOAD.register((server, world) -> this.afterWorldLoad(world));
        ServerWorldEvents.UNLOAD.register((server, world) -> this.onWorldUnload(world));
    }

    private void onServerStarting(MinecraftServer server) {
        this.serverInstance = server;
        WDNetworkRegistry.setServer(server);
        File f = net.montoyo.wd.client.WebDisplaysDirs.getCacheFile("wd_next.txt");

        if (f.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String idx = br.readLine();
                Util.silentClose(br);

                if (idx == null)
                    throw new RuntimeException("Seems like the file is empty (1)");

                idx = idx.trim();
                if (idx.isEmpty())
                    throw new RuntimeException("Seems like the file is empty (2)");

                lastPadId = Integer.parseInt(idx);
            } catch (Throwable t) {
                Log.warningEx("Could not read last minePad ID from %s. I'm afraid this might break all minePads.", t, f.getAbsolutePath());
            }
        }

        if (miniservPort != 0) {
            Server sv = Server.getInstance();
            sv.setPort(miniservPort);
            sv.setDirectory(net.montoyo.wd.client.WebDisplaysDirs.getSubDir("wd_filehost"));
            sv.start();
        }

        // Initialize server-side URL cache — lives on disk, survives chunk-race deaths
        serverUrlCache = new ServerUrlCache(server);
        Log.info("[WebDisplays] ServerUrlCache initialized — URL data persists across disconnect races");
    }

    private void onServerStopping(MinecraftServer server) {
        try {
            // Flush all cached URLs to disk one last time before the world closes
            if (serverUrlCache != null) {
                serverUrlCache.flush();
                serverUrlCache.clearMemory();
                Log.info("[WebDisplays] ServerUrlCache: flushed and cleared on server stop");
            }
            Server.getInstance().stopServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void onPlayerJoin(ServerPlayer player) {
        if (player != null) {
            // Grant all webdisplays recipes so they always appear in the recipe book
            var recipeManager = player.server.getRecipeManager();
            if (recipeManager != null) {
                player.awardRecipes(
                    recipeManager.getRecipes().stream()
                        .filter(r -> r.toString().contains("webdisplays"))
                        .collect(java.util.stream.Collectors.toList())
                );
            }
            
            // Allow client UUID for miniserv authentication
            Server.getInstance().allowClient(player.getUUID());
            Log.info("Added player " + player.getName().getString() + " with UUID " + player.getUUID() + " to miniserv allow list");
            
            // Always send server info so the miniserv client can connect,
            // independent of whether join messages are shown.
            S2CMessageServerInfo message = new S2CMessageServerInfo(miniservPort);
            WDNetworkRegistry.sendToPlayer(player, message);
            
            if (!joinMessage) return;
            
            // Show welcome message on first join
            WelcomeState welcome = new WelcomeState(player.server);
            if (!welcome.hasBeenWelcomed(player.getUUID())) {
                Util.toast(player, ChatFormatting.LIGHT_PURPLE, "welcome1");
                Util.toast(player, ChatFormatting.LIGHT_PURPLE, "welcome2");
                Util.toast(player, ChatFormatting.LIGHT_PURPLE, "welcome3");
                welcome.markWelcomed(player.getUUID());
            }
            
            // Sync all loaded screens to the joining player
            MinecraftServer server = player.server;
            for (ServerLevel level : server.getAllLevels()) {
                int viewDist = server.getPlayerList().getViewDistance();
                int chunkX = player.chunkPosition().x;
                int chunkZ = player.chunkPosition().z;
                for (int dx = -viewDist; dx <= viewDist; dx++) {
                    for (int dz = -viewDist; dz <= viewDist; dz++) {
                        int cx = chunkX + dx;
                        int cz = chunkZ + dz;
                        if (level.hasChunk(cx, cz)) {
                            LevelChunk chunk = level.getChunk(cx, cz);
                            for (BlockEntity be : chunk.getBlockEntities().values()) {
                                if (be instanceof ScreenBlockEntity screen) {
                                    WDNetworkRegistry.sendToPlayer(player, new S2CMessageAddScreen(screen));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void onPlayerLeave(ServerPlayer player) {
        if (player != null) {
            Server.getInstance().getClientManager().revokeClientKey(player.getGameProfile().getId());
        }
    }

    private void afterWorldLoad(Level world) {
        if (world.isClientSide() || world.dimension() != Level.OVERWORLD)
            return;
        
        if (!serverStartedDimensions.contains(world.dimension())) {
            serverStartedDimensions.add(world.dimension());
        }

        // HEAL: Apply cached URLs from the server-side file to fix any that were
        // lost in a disconnect race during the previous session.
        if (world instanceof net.minecraft.server.level.ServerLevel serverLevel && serverUrlCache != null) {
            serverUrlCache.applyCachedUrls(serverLevel);
        }
    }

    private void onWorldUnload(Level world) {
        if (world.isClientSide() || world.dimension() != Level.OVERWORLD)
            return;
        
        serverStartedDimensions.remove(world.dimension());
    }

    public void onRegisterSounds() {
        soundTyping = registerSound("keyboard_type");
        soundUpgradeAdd = registerSound("upgrade_add");
        soundUpgradeDel = registerSound("upgrade_del");
        soundScreenCfg = registerSound("screencfg_open");
        soundServer = registerSound("server");
        soundIronic = registerSound("ironic");
    }

    private SoundEvent registerSound(String resName) {
        ResourceLocation resLoc = ResourceLocation.fromNamespaceAndPath("webdisplays", resName);
        return SoundEvent.createVariableRangeEvent(resLoc);
    }

    private static void registerTrigger(Criterion... criteria) {
        for(Criterion c: criteria) {
            CriteriaTriggers.register(c.getId().toString(), c);
        }
    }

    public static int getNextAvailablePadID() {
        return INSTANCE.lastPadId++;
    }

    public static boolean isSiteBlacklisted(String url) {
        try {
            // MC 1.21: Use URI instead of deprecated URL constructor
            new URI(Util.addProtocol(url));
            return false;
        } catch(URISyntaxException ex) {
            return false;
        }
    }

    public static String applyBlacklist(String url) {
        return isSiteBlacklisted(url) ? BLACKLIST_URL : url;
    }

    public static void attachCapability(Entity entity, CompoundTag tag) {
        // if (entity instanceof Player && !entity.getCapability(WDDCapability.Provider.cap).isPresent()) {
        //     entity.getPersistentData().putCompound("webdisplays:wddcapability", tag);
        // }
    }

    public MinecraftServer getServer() {
        return serverInstance;
    }

    public ServerUrlCache getServerUrlCache() {
        return serverUrlCache;
    }

    /**
     * Save a URL directly to the server-side cache. Intended for use by
     * {@link net.montoyo.wd.utilities.browser.handlers.DisplayHandler}
     * during the emergency-flush path when the client disconnects from an
     * integrated server.  In single-player the integrated server shares the
     * same JVM, so the client can reach this singleton instance and write
     * straight to <code>wd_server_urls.json</code> without going through
     * the network.
     */
    public void saveUrlToServerCache(BlockPos pos, BlockSide side, String url) {
        ServerUrlCache cache = getServerUrlCache();
        if (cache != null) {
            cache.saveUrl(pos, side, url);
        }
    }
}
