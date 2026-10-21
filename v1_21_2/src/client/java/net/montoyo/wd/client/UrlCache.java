package net.montoyo.wd.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.utilities.data.BlockSide;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class UrlCache {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CACHE_FILE = "wd_url_cache.json";
    
    private final File cacheFile;
    private final Map<String, String> memoryCache = new HashMap<>();
    private boolean cacheLoaded = false;
    
    public UrlCache() {
        this.cacheFile = WebDisplaysDirs.getCacheFile(CACHE_FILE);
    }
    
    private void ensureCacheLoaded() {
        if (!cacheLoaded) {
            synchronized (memoryCache) {
                if (!cacheLoaded) {
                    loadCacheToMemory();
                    cacheLoaded = true;
                }
            }
        }
    }
    
    private void loadCacheToMemory() {
        try {
            if (cacheFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(cacheFile))) {
                    JsonObject cache = GSON.fromJson(reader, JsonObject.class);
                    memoryCache.clear();
                    for (String key : cache.keySet()) {
                        memoryCache.put(key, cache.get(key).getAsString());
                    }
                    WebDisplays.LOGGER.debug("[WebDisplays] UrlCache: Loaded " + memoryCache.size() + " URLs into memory");
                }
            }
        } catch (Exception e) {
            WebDisplays.LOGGER.warn("[WebDisplays] UrlCache: Failed to load cache: " + e.getMessage());
        }
    }
    
    private String normalizeUrl(String url) {
        if (url == null) return null;
        if (url.endsWith("/") && !url.equals("https://") && url.length() > 8) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
    
    public void saveUrl(BlockPos pos, BlockSide side, String url) {
        String key = pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + side.name();
        
        url = normalizeUrl(url);
        
        synchronized (memoryCache) {
            String existing = memoryCache.get(key);
            
            String normalizedExisting = normalizeUrl(existing);
            
            if (existing != null && 
                (existing.contains("watch?v=") || existing.contains("/shorts/")) &&
                (url.equals("https://www.youtube.com") || url.equals("https://www.youtube.com/"))) {
                WebDisplays.LOGGER.debug("[WebDisplays] UrlCache: PROTECTED video URL from homepage overwrite: " + existing);
                return;
            }
            
            if (normalizedExisting != null && normalizedExisting.equals(url)) {
                return;
            }
            memoryCache.put(key, url);
        }
        
        final String urlToSave = url;
        new Thread(() -> {
            try {
                JsonObject cache = new JsonObject();
                synchronized (memoryCache) {
                    for (Map.Entry<String, String> entry : memoryCache.entrySet()) {
                        cache.addProperty(entry.getKey(), entry.getValue());
                    }
                }
                
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(cacheFile))) {
                    GSON.toJson(cache, writer);
                    writer.flush();
                }
                
                WebDisplays.LOGGER.debug("[WebDisplays] UrlCache: Saved " + key + " -> " + urlToSave);
            } catch (IOException e) {
                WebDisplays.LOGGER.warn("[WebDisplays] UrlCache: Failed to save URL: " + e.getMessage());
            }
        }, "UrlCache-Save").start();
    }
    
    public String loadUrl(BlockPos pos, BlockSide side) {
        ensureCacheLoaded();
        String key = pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + side.name();
        synchronized (memoryCache) {
            return memoryCache.get(key);
        }
    }
    
    public void applyCachedUrls() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        ensureCacheLoaded();
        
        try {
            boolean appliedAny = false;
            Map<String, String> cacheCopy;
            synchronized (memoryCache) {
                cacheCopy = new HashMap<>(memoryCache);
            }
            
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
                    BlockSide side = BlockSide.valueOf(parts[3]);
                    
                    if (mc.level.getBlockEntity(pos) instanceof ScreenBlockEntity screen) {
                        String currentUrl = null;
                        for (int i = 0; i < screen.screenCount(); i++) {
                            if (screen.getScreen(i).side == side) {
                                currentUrl = screen.getScreen(i).url;
                                break;
                            }
                        }
                        
                        String normalizedCurrent = normalizeUrl(currentUrl);
                        String normalizedCached = normalizeUrl(url);
                        
                        if (currentUrl != null && 
                            (currentUrl.contains("watch?v=") || currentUrl.contains("/shorts/")) &&
                            (url.equals("https://www.youtube.com") || url.equals("https://www.youtube.com/"))) {
                            WebDisplays.LOGGER.debug("[WebDisplays] UrlCache: BLOCKED applying homepage over video: " + currentUrl);
                            continue;
                        }
                        
                        if (!normalizedCached.equals(normalizedCurrent)) {
                            WebDisplays.LOGGER.debug("[WebDisplays] UrlCache: URL mismatch detected - applying cached URL");
                            WebDisplays.LOGGER.debug("[WebDisplays]   Current: " + currentUrl + " (norm: " + normalizedCurrent + ")");
                            WebDisplays.LOGGER.debug("[WebDisplays]   Cached:  " + url + " (norm: " + normalizedCached + ")");
                            screen.setScreenURL(side, url);
                            screen.setChanged();
                            WebDisplays.PROXY.syncUrlToServer(screen, side, url);
                            appliedAny = true;
                        }
                    }
                } catch (Exception e) {
                    WebDisplays.LOGGER.warn("[WebDisplays] UrlCache: Failed to apply URL for key " + key + ": " + e.getMessage());
                }
            }
            
            if (appliedAny) {
                WebDisplays.LOGGER.debug("[WebDisplays] UrlCache: Applied cached URLs to screens");
            }
        } catch (Exception e) {
            WebDisplays.LOGGER.warn("[WebDisplays] UrlCache: Failed to apply cached URLs: " + e.getMessage());
        }
    }
    
    public String getUrlForPos(BlockPos pos, BlockSide side) {
        ensureCacheLoaded();
        String key = pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + side.name();
        synchronized (memoryCache) {
            return memoryCache.get(key);
        }
    }
    
    public void deleteUrl(BlockPos pos, BlockSide side) {
        String key = pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + side.name();
        
        synchronized (memoryCache) {
            if (!memoryCache.containsKey(key)) {
                return;
            }
            memoryCache.remove(key);
            WebDisplays.LOGGER.debug("[WebDisplays] UrlCache: Removed from memory cache: " + key);
        }
        
        try {
            JsonObject cache = new JsonObject();
            synchronized (memoryCache) {
                for (Map.Entry<String, String> entry : memoryCache.entrySet()) {
                    cache.addProperty(entry.getKey(), entry.getValue());
                }
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(cacheFile))) {
                GSON.toJson(cache, writer);
                writer.flush();
            }
            
            WebDisplays.LOGGER.debug("[WebDisplays] UrlCache: Deleted entry for broken block " + key);
        } catch (IOException e) {
            WebDisplays.LOGGER.warn("[WebDisplays] UrlCache: Failed to clear broken block from cache: " + e.getMessage());
        }
    }
    
    public void flushCache() {
        try {
            synchronized (memoryCache) {
                if (memoryCache.isEmpty()) {
                    cacheLoaded = false;
                    return;
                }
                
                JsonObject cache = new JsonObject();
                for (Map.Entry<String, String> entry : memoryCache.entrySet()) {
                    cache.addProperty(entry.getKey(), entry.getValue());
                }
                
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(cacheFile))) {
                    GSON.toJson(cache, writer);
                    writer.flush();
                }
                
                WebDisplays.LOGGER.debug("[WebDisplays] UrlCache: Flushed " + memoryCache.size() + " URLs to disk (file preserved)");
            }
        } catch (Exception e) {
            WebDisplays.LOGGER.warn("[WebDisplays] UrlCache: Failed to flush cache: " + e.getMessage());
        }
    }
    
    public void clearCache() {
        try {
            synchronized (memoryCache) {
                memoryCache.clear();
            }
            cacheLoaded = false;
            WebDisplays.LOGGER.debug("[WebDisplays] UrlCache: Memory cache cleared (file preserved, will reload on next access)");
        } catch (Exception e) {
            WebDisplays.LOGGER.warn("[WebDisplays] UrlCache: Failed to clear memory cache: " + e.getMessage());
        }
    }
}
