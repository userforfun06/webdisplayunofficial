package net.montoyo.wd;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.utilities.data.BlockSide;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Server-side URL cache that persists URLs to a world file immediately.
 * This is a secondary defense against the race condition during disconnect:
 *  - When a URL packet arrives, it is written to this file immediately.
 *  - On world load, URLs from this file are applied before NBT is loaded,
 *    healing any URLs lost due to a race during save.
 */
public class ServerUrlCache {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CACHE_FILE = "wd_server_urls.json";

    private final File cacheFile;
    private final Map<String, String> memoryCache = new HashMap<>();
    private boolean cacheLoaded = false;

    public ServerUrlCache(MinecraftServer server) {
        this.cacheFile = net.montoyo.wd.client.WebDisplaysDirs.getCacheFile(CACHE_FILE);
    }

    private void ensureCacheLoaded() {
        if (!cacheLoaded) {
            synchronized (memoryCache) {
                if (!cacheLoaded) {
                    loadCacheFromDisk();
                    cacheLoaded = true;
                }
            }
        }
    }

    private void loadCacheFromDisk() {
        if (!cacheFile.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(cacheFile))) {
            JsonObject cache = GSON.fromJson(reader, JsonObject.class);
            memoryCache.clear();
            for (String key : cache.keySet()) {
                memoryCache.put(key, cache.get(key).getAsString());
            }
            WebDisplays.LOGGER.debug("[WebDisplays] ServerUrlCache initialized — URL data persists across disconnect races");
        } catch (IOException e) {
            WebDisplays.LOGGER.warn("[WebDisplays] ServerUrlCache: Failed to load: " + e.getMessage());
        }
    }

    private static String makeKey(BlockPos pos, BlockSide side) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + side.name();
    }

    /**
     * Save a URL to the server-side cache immediately (synchronous).
     * Called from the packet handler so the URL is on disk before the world closes.
     */
    public void saveUrl(BlockPos pos, BlockSide side, String url) {
        String key = makeKey(pos, side);
        ensureCacheLoaded();

        String oldUrl = memoryCache.get(key);
        if (url.equals(oldUrl)) {
            return; // No change
        }

        memoryCache.put(key, url);
        writeCacheToDisk(key, url);
    }

    /**
     * Remove a URL from the cache (called when a screen is broken).
     */
    public void deleteUrl(BlockPos pos, BlockSide side) {
        String key = makeKey(pos, side);
        ensureCacheLoaded();
        if (memoryCache.remove(key) != null) {
            writeCacheToDisk(key, null);
            WebDisplays.LOGGER.debug("[WebDisplays] ServerUrlCache: Deleted " + key);
        }
    }

    private void writeCacheToDisk(String changedKey, String unused) {
        // Write entire cache synchronously (fast for small maps)
        synchronized (memoryCache) {
            try {
                JsonObject cache = new JsonObject();
                for (Map.Entry<String, String> entry : memoryCache.entrySet()) {
                    cache.addProperty(entry.getKey(), entry.getValue());
                }
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(cacheFile))) {
                    GSON.toJson(cache, writer);
                    writer.flush();
                }
                if (changedKey != null) {
                    WebDisplays.LOGGER.debug("[WebDisplays] ServerUrlCache: Saved " + changedKey + " -> " + memoryCache.get(changedKey));
                }
            } catch (IOException e) {
                WebDisplays.LOGGER.warn("[WebDisplays] ServerUrlCache: Write failed: " + e.getMessage());
            }
        }
    }

    /**
     * Flush all pending URLs to disk synchronously. Call during server save.
     */
    public void flush() {
        ensureCacheLoaded();
        synchronized (memoryCache) {
            try {
                JsonObject cache = new JsonObject();
                for (Map.Entry<String, String> entry : memoryCache.entrySet()) {
                    cache.addProperty(entry.getKey(), entry.getValue());
                }
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(cacheFile))) {
                    GSON.toJson(cache, writer);
                    writer.flush();
                }
                WebDisplays.LOGGER.debug("[WebDisplays] ServerUrlCache: Flushed " + memoryCache.size() + " URLs to disk");
            } catch (IOException e) {
                WebDisplays.LOGGER.warn("[WebDisplays] ServerUrlCache: Flush failed: " + e.getMessage());
            }
        }
    }

    /**
     * Apply cached URLs to all ScreenBlockEntities in the level.
     * Called on world load after NBT has been loaded, to heal any URLs lost during the disconnect race.
     */
    public void applyCachedUrls(ServerLevel level) {
        ensureCacheLoaded();
        if (memoryCache.isEmpty()) return;

        Map<String, String> cacheCopy;
        synchronized (memoryCache) {
            cacheCopy = new HashMap<>(memoryCache);
        }

        int applied = 0;
        for (Map.Entry<String, String> entry : cacheCopy.entrySet()) {
            String key = entry.getKey();
            String url = entry.getValue();
            try {
                String[] parts = key.split(",");
                if (parts.length != 4) continue;
                BlockPos pos = new BlockPos(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2])
                );
                net.montoyo.wd.utilities.data.BlockSide side = net.montoyo.wd.utilities.data.BlockSide.valueOf(parts[3]);

                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof ScreenBlockEntity screen) {
                    ScreenData data = screen.getScreen(side);
                    if (data != null && !url.equals(data.url)) {
                        WebDisplays.LOGGER.debug("[WebDisplays] ServerUrlCache: Healing URL at " + pos + " side=" + side +
                            " from '" + data.url + "' to '" + url + "'");
                        screen.setScreenURL(side, url);
                        screen.setChanged();
                        applied++;
                    }
                }
            } catch (Exception e) {
                WebDisplays.LOGGER.warn("[WebDisplays] ServerUrlCache: Failed to apply " + key + ": " + e.getMessage());
            }
        }
        if (applied > 0) {
            WebDisplays.LOGGER.debug("[WebDisplays] ServerUrlCache: Applied " + applied + " healed URLs to level " + level.dimension().location());
        }
    }

    /**
     * Clear the memory cache (not the file) when the server stops.
     */
    public void clearMemory() {
        synchronized (memoryCache) {
            memoryCache.clear();
            cacheLoaded = false;
        }
    }
}
