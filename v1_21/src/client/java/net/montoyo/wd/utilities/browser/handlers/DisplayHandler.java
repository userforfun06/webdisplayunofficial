package net.montoyo.wd.utilities.browser.handlers;

import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.client.ClientProxy;
import net.montoyo.wd.client.UrlCache;
import net.montoyo.wd.client.WebDisplaysDirs;
import net.montoyo.wd.utilities.browser.handlers.js.Scripts;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.payload.UrlUpdatePayload;
import net.montoyo.wd.net.server_bound.C2SMessageMinepadUrl;
import net.montoyo.wd.utilities.data.BlockSide;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandler;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

public class DisplayHandler implements CefDisplayHandler {

    public static final CefDisplayHandler INSTANCE = new DisplayHandler();
    
    // Layer 1: Priority-based URL filtering
    // Track last synced URL per browser to prevent "backwards" overwrites
    private final Map<CefBrowser, String> lastSyncedUrl = new HashMap<>();
    
    // Layer 2: Target URL tracking (prevent sync loops - don't sync URLs server told us to load)
    private final Map<CefBrowser, String> targetUrls = new HashMap<>();
    
    // Emergency flush flag - when true, bypass all filters (for disconnect)
    private volatile boolean emergencyFlushActive = false;
    
    // Debounce for network packets - prevent feedback loops
    private final Map<CefBrowser, Long> lastPacketTime = new ConcurrentHashMap<>();
    private static final long PACKET_DEBOUNCE_MS = 1000; // 1 second between packets
    
    // Client-side URL cache for persistence across disconnects
    private final UrlCache urlCache = new UrlCache();
    
    // Debounce mechanism: Track pending URL changes to prevent feedback loops
    // Only save after URL has been stable for 1 second
    private final Map<String, PendingUrlChange> pendingChanges = new ConcurrentHashMap<>();
    private final ScheduledExecutorService debounceExecutor = Executors.newSingleThreadScheduledExecutor();
    
    private static class PendingUrlChange {
        final String url;
        final BlockPos pos;
        final BlockSide side;
        
        PendingUrlChange(String url, BlockPos pos, BlockSide side) {
            this.url = url;
            this.pos = pos;
            this.side = side;
        }
    }

    /**
     * Set the target URL that the server told us to load.
     * Used to prevent sync loops - we don't report back URLs that we were told to load.
     */
    public void setTargetUrl(CefBrowser browser, String url) {
        if (browser != null && url != null) {
            targetUrls.put(browser, url);
        }
    }
    
    /**
     * Debounced URL save - only saves after URL has been stable for 1 second.
     * This prevents feedback loops during redirects.
     */
    private void scheduleDebouncedSave(BlockPos pos, BlockSide side, String url) {
        WebDisplays.LOGGER.debug("[WebDisplays] scheduleDebouncedSave CALLED for " + pos + " side=" + side + " url=" + url);
        String key = pos.getX() + "," + pos.getY() + "," + pos.getZ() + "," + side.name();
        
        // Cancel any existing pending change for this position
        PendingUrlChange existing = pendingChanges.get(key);
        if (existing != null) {
            // Update the pending change with new URL and time
            pendingChanges.put(key, new PendingUrlChange(url, pos, side));
            WebDisplays.LOGGER.debug("[WebDisplays] Debounce: Updated pending URL for " + key + " -> " + url);
            return;
        }
        
        // Create new pending change
        pendingChanges.put(key, new PendingUrlChange(url, pos, side));
        
        // Schedule save after 1 second
        debounceExecutor.schedule(() -> {
            PendingUrlChange pending = pendingChanges.remove(key);
            if (pending != null) {
                // Only save if URL hasn't changed again
                urlCache.saveUrl(pending.pos, pending.side, pending.url);
                WebDisplays.LOGGER.debug("[WebDisplays] Debounce: Saved stable URL for " + key + " -> " + pending.url);
            }
        }, 1, TimeUnit.SECONDS);
        
        WebDisplays.LOGGER.debug("[WebDisplays] Debounce: Scheduled save for " + key + " -> " + url);
    }

    private void appendBrowserHistory(String dir, String url) {
        try {
            File f = new File(WebDisplaysDirs.getSubDir(dir), "history.txt");
            String line = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "|" + url + "\n";
            if (f.length() > 100_000) {
                // Keep file from growing unbounded: trim to last ~500 lines
                java.util.List<String> lines = Files.readAllLines(f.toPath());
                if (lines.size() > 500) {
                    Files.write(f.toPath(), lines.subList(lines.size() - 500, lines.size()));
                }
            }
            Files.write(f.toPath(), line.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            WebDisplays.LOGGER.error("Failed to write browser history", e);
        }
    }

    /**
     * Normalize URL for comparison - strips tracking parameters and trailing slashes
     */
    public static String normalizeUrl(String url) {
        if (url == null) return "";
        String normalized = url.toLowerCase()
            .replaceAll("/$", "")  // Remove trailing slash
            .split("\\?si=")[0]      // Remove share tracking
            .split("&feature=")[0]   // Remove feature tracking
            .split("&t=")[0]        // Remove timestamp
            .split("&pp=")[0];      // Remove other tracking
        return normalized;
    }
    
    /**
     * Check if two URLs are the same (ignoring tracking parameters)
     */
    public static boolean urlsEqual(String url1, String url2) {
        return normalizeUrl(url1).equals(normalizeUrl(url2));
    }
    
    /**
     * Check if a URL is "high priority" (video/content URL) vs "low priority" (base/home URL)
     */
    private boolean isHighPriorityUrl(String url) {
        if (url == null) return false;
        String normalized = normalizeUrl(url);
        // Generic content indicators (works for ALL websites, not just YouTube)
        if (normalized.contains("/watch?") || normalized.contains("/video/") || normalized.contains("/v/") ||
            normalized.contains("/apps/") || normalized.contains("/game/") || normalized.contains("/play/")) {
            return true;
        }
        try {
            java.net.URI uri = new java.net.URI(url);
            String path = uri.getPath();
            if (path != null && path.length() > 1 && path.split("/").length >= 3) {
                return true; // Any deep path is high priority
            }
        } catch (Exception e) {}
        return false;
    }
    
    /**
     * Check if a URL is a "base" or "home" page (low priority)
     */
    private boolean isLowPriorityBaseUrl(String url) {
        if (url == null) return false;
        // Generic home pages
        if (url.endsWith("/") && !url.contains("?") && !url.contains("/watch")) {
            // Check for common home page patterns
            String[] homePatterns = {"/home", "/index", "/main", "/feed"};
            for (String pattern : homePatterns) {
                if (url.contains(pattern)) return true;
            }
        }
        return false;
    }
    
    /**
     * Get the URL cache for applying cached URLs on world load
     */
    public UrlCache getUrlCache() {
        return urlCache;
    }
    
    @Override
    public void onAddressChange(CefBrowser browser, CefFrame cefFrame, String url) {
        if (browser == null || url == null || url.isEmpty()) return;
        
        // Layer 1: Trash URL filter
        if (url.equals("about:blank")) return;
        
        ClientProxy proxy = ((ClientProxy) WebDisplays.PROXY);
        
        // MinePad URL sync (matches 1.20 Forge behavior) - runs BEFORE filters
        long now = System.currentTimeMillis();
        for (ClientProxy.PadData pd : proxy.getPads()) {
            if (pd.view == browser && now - pd.lastSent() >= 1000) {
                pd.updateTime();
                WDNetworkRegistry.sendToServer(new C2SMessageMinepadUrl(pd.id, url));
                // Save to persistent client-side cache
                proxy.cacheMinepadUrl(pd.id, url);
                appendBrowserHistory("browsers/minepad/" + pd.id, url);
                break;
            }
        }
        
        // Log for debugging - ALL URLs, not just YouTube
        WebDisplays.LOGGER.debug("[WebDisplays] onAddressChange: " + url);
        
        // Layer 2: Target URL check - don't sync what server told us to load
        // Use normalized comparison to handle tracking parameter differences
        String targetUrl = targetUrls.get(browser);
        if (!emergencyFlushActive && targetUrl != null && urlsEqual(url, targetUrl)) {
            WebDisplays.LOGGER.debug("[WebDisplays] URL matches target (no sync needed): " + url);
            lastSyncedUrl.put(browser, url); // Still track it
            // Still save to cache even for target match (GUI-set URLs need cache persistence)
            boolean foundScreen = false;
            for (ScreenBlockEntity tes : proxy.getScreens()) {
                BlockSide side = tes.getSideForBrowser(browser);
                if (side != null) {
                    WebDisplays.LOGGER.debug("[WebDisplays] TARGET MATCH: Found screen at " + tes.getBlockPos() + " side=" + side + ", scheduling cache save");
                    scheduleDebouncedSave(tes.getBlockPos(), side, url);
                    appendBrowserHistory("browsers/srceen/" + tes.getBlockPos().getX() + "_" + tes.getBlockPos().getY() + "_" + tes.getBlockPos().getZ(), url);
                    foundScreen = true;
                    break;
                }
            }
            if (!foundScreen) {
                WebDisplays.LOGGER.debug("[WebDisplays] TARGET MATCH: No screen found for browser (transient - screen may be replacing). Screens count={}", proxy.getScreens().size());
            }
            return;
        }
        
        // PRIORITY FILTER: Don't let Low Priority (home/base) overwrite High Priority (video)
        // This is the key fix for the "backwards" bug
        if (!emergencyFlushActive) {
            String lastUrl = lastSyncedUrl.get(browser);
            
            // If we had a high priority URL (video) and now have low priority (home), 
            // don't sync immediately - user might be navigating
            if (lastUrl != null && isHighPriorityUrl(lastUrl) && isLowPriorityBaseUrl(url)) {
                WebDisplays.LOGGER.debug("[WebDisplays] BLOCKED: Home page would overwrite video. Last: " + lastUrl + " -> New: " + url);
                // Store locally but don't sync to server
                lastSyncedUrl.put(browser, url);
                // Still save to cache for persistence (find screen first)
                for (ScreenBlockEntity tes : proxy.getScreens()) {
                    BlockSide side = tes.getSideForBrowser(browser);
                    if (side != null) {
                        scheduleDebouncedSave(tes.getBlockPos(), side, url);
                        appendBrowserHistory("browsers/srceen/" + tes.getBlockPos().getX() + "_" + tes.getBlockPos().getY() + "_" + tes.getBlockPos().getZ(), url);
                        break;
                    }
                }
                return;
            }
        }
        
        // SYNC TO SERVER (with debounce to prevent feedback loops)
        Long lastTime = lastPacketTime.get(browser);
        boolean canSendPacket = lastTime == null || (now - lastTime) > PACKET_DEBOUNCE_MS || emergencyFlushActive;
        
        for (ScreenBlockEntity tes : proxy.getScreens()) {
            BlockSide side = tes.getSideForBrowser(browser);
            if (side != null) {
                ScreenData scr = tes.getScreen(side);
                
                // ECHO GUARD: Check if this URL change came from a server packet
                if (scr != null && scr.ignoreNextUrlChange) {
                    WebDisplays.LOGGER.debug("[WebDisplays] ECHO GUARD: Ignoring URL change from server packet: " + url);
                    scr.ignoreNextUrlChange = false; // Reset the flag
                    lastSyncedUrl.put(browser, url); // Track it as already synced
                    // Still save to cache even for server echo (prevents cache desync)
                    scheduleDebouncedSave(tes.getBlockPos(), side, url);
                    appendBrowserHistory("browsers/srceen/" + tes.getBlockPos().getX() + "_" + tes.getBlockPos().getY() + "_" + tes.getBlockPos().getZ(), url);
                    return; // Don't sync back to server!
                }
                
                tes.updateClientSideURL(browser, url);
                lastSyncedUrl.put(browser, url);
                
                WebDisplays.LOGGER.debug("[WebDisplays] SYNCING: " + (emergencyFlushActive ? "[EMERGENCY] " : "") + 
                    "pos=" + tes.getBlockPos() + " side=" + side + " url='" + url + "'");
                
                // DEBOUNCE: Only send packet if 1 second has passed (or emergency flush)
                if (canSendPacket) {
                    lastPacketTime.put(browser, now);
                    net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                        new UrlUpdatePayload(tes.getBlockPos(), side, url)
                    );
                } else {
                    WebDisplays.LOGGER.debug("[WebDisplays] Debounce: Skipping packet (too soon), last sent " + (now - lastTime) + "ms ago");
                }
                
                // CRITICAL: Save to cache for persistence across disconnects
                // This runs independently of packet debounce - cache saves after URL is stable
                scheduleDebouncedSave(tes.getBlockPos(), side, url);
                appendBrowserHistory("browsers/srceen/" + tes.getBlockPos().getX() + "_" + tes.getBlockPos().getY() + "_" + tes.getBlockPos().getZ(), url);
            }
        }

        // enables a custom pointer lock api
        browser.executeJavaScript(Scripts.POINTER_LOCK, "WebDisplays", 0);
        
        // Generic SPA detection: Inject JavaScript to capture ALL navigation events
        // Works for any site using JS routing
        if (url != null) {
            String spaScript = 
                "if (!window.__wd_spa_installed) {" +
                " window.__wd_spa_installed = true;" +
                " var lastReported = window.location.href;" +
                " var notify = function() {" +
                " if (window.location.href !== lastReported) {" +
                " lastReported = window.location.href;" +
                " console.log('WD_URL_CHANGE:' + window.location.href);" +
                " }" +
                " };" +
                " var origPush = window.history.pushState;" +
                " window.history.pushState = function() {" +
                " var ret = origPush.apply(this, arguments);" +
                " setTimeout(notify, 50);" +
                " return ret;" +
                " };" +
                " var origReplace = window.history.replaceState;" +
                " window.history.replaceState = function() {" +
                " var ret = origReplace.apply(this, arguments);" +
                " setTimeout(notify, 50);" +
                " return ret;" +
                " };" +
                " window.addEventListener('popstate', notify);" +
                " window.addEventListener('hashchange', notify);" +
                " window.addEventListener('yt-navigate-finish', notify);" +
                " setInterval(notify, 500);" +
                " console.log('WD_SPA_LISTENER_INSTALLED:' + window.location.href);" +
                "}";
            browser.executeJavaScript(spaScript, url, 0);
            WebDisplays.LOGGER.debug("[WebDisplays] SPA listener injected for: " + url);
        }
    }

    @Override
    public void onTitleChange(CefBrowser cefBrowser, String s) {
    }

    @Override
    public boolean onTooltip(CefBrowser cefBrowser, String s) {
        return false;
    }

    @Override
    public void onStatusMessage(CefBrowser cefBrowser, String s) {
    }

    @Override
    public boolean onConsoleMessage(CefBrowser browser, CefSettings.LogSeverity level, String message, String source, int line) {
        // Layer 2: SPA JS Injector - capture navigation events from any site
        if (message != null && message.startsWith("WD_URL_CHANGE:")) {
            String realUrl = message.substring(14);
            WebDisplays.LOGGER.debug("[WebDisplays] SPA JS URL change detected: " + realUrl + " (from " + source + ")");
            
            // Apply same priority filter as onAddressChange
            ClientProxy proxy = ((ClientProxy) WebDisplays.PROXY);
            
            // Skip sync only if URL matches the target (no actual change)
            // Use normalized comparison to handle tracking parameter differences
            String targetUrl = targetUrls.get(browser);
            if (targetUrl != null && urlsEqual(realUrl, targetUrl)) {
                lastSyncedUrl.put(browser, realUrl);
                return true;
            }
            
            // MinePad URL sync for SPA navigations (e.g. YouTube history API)
            for (ClientProxy.PadData pd : proxy.getPads()) {
                if (pd.view == browser) {
                    WDNetworkRegistry.sendToServer(new C2SMessageMinepadUrl(pd.id, realUrl));
                    proxy.cacheMinepadUrl(pd.id, realUrl);
                    break;
                }
            }
            
            // Sync to server - try screenTracking first, then allBrowsers as fallback
            boolean synced = false;
            for (ScreenBlockEntity tes : proxy.getScreens()) {
                BlockSide side = tes.getSideForBrowser(browser);
                if (side != null) {
                    tes.updateClientSideURL(browser, realUrl);
                    lastSyncedUrl.put(browser, realUrl);
                    WebDisplays.PROXY.syncUrlToServer(tes, side, realUrl);
                    // DEBOUNCE: Schedule save after 1 second of URL stability
                    // This prevents feedback loops during redirects
                    scheduleDebouncedSave(tes.getBlockPos(), side, realUrl);
                    appendBrowserHistory("browsers/srceen/" + tes.getBlockPos().getX() + "_" + tes.getBlockPos().getY() + "_" + tes.getBlockPos().getZ(), realUrl);
                    synced = true;
                    break;
                }
            }
            
            // Fallback: if not in screenTracking, use browserToScreen map
            if (!synced) {
                ScreenBlockEntity tes = proxy.getScreenForBrowser(browser);
                if (tes != null) {
                    BlockSide side = proxy.getSideForBrowser(browser);
                    if (side != null) {
                        tes.updateClientSideURL(browser, realUrl);
                        lastSyncedUrl.put(browser, realUrl);
                        WebDisplays.PROXY.syncUrlToServer(tes, side, realUrl);
                        scheduleDebouncedSave(tes.getBlockPos(), side, realUrl);
                        appendBrowserHistory("browsers/srceen/" + tes.getBlockPos().getX() + "_" + tes.getBlockPos().getY() + "_" + tes.getBlockPos().getZ(), realUrl);
                        synced = true;
                    }
                }
            }
            return true;
        }
        return false;
    }
    
    /**
     * Layer 3: Emergency Flush - called on disconnect, bypasses ALL filters
     * This is our last chance to save the correct URL before the world closes
     */
    public void emergencyFlushAll() {
        emergencyFlushActive = true;
        WebDisplays.LOGGER.debug("[WebDisplays] ===============================================");
        WebDisplays.LOGGER.debug("[WebDisplays] EMERGENCY FLUSH STARTING - bypassing all filters");
        WebDisplays.LOGGER.debug("[WebDisplays] ===============================================");
        
        ClientProxy proxy = ((ClientProxy) WebDisplays.PROXY);
        int syncedCount = 0;
        
        WebDisplays.LOGGER.debug("[WebDisplays] Proxy screens tracked: " + proxy.getScreens().size());
        
        // Flush all active screens via screenTracking
        for (ScreenBlockEntity tes : proxy.getScreens()) {
            for (int i = 0; i < tes.screenCount(); i++) {
                ScreenData scr = tes.getScreen(i);
                if (scr.browser != null) {
                    String finalUrl = scr.browser.getURL();
                    if (finalUrl != null && !finalUrl.isEmpty()) {
                        // Bypass all filters - just send it
                        tes.updateClientSideURL(scr.browser, finalUrl);
                        lastSyncedUrl.put(scr.browser, finalUrl);
                        
                        // NUCLEAR OPTION: In single-player, directly call server method
                        // This bypasses the network entirely and guarantees the save
                        syncUrlToServer(tes, scr.side, finalUrl);
                        // CRITICAL: Save to disk for persistence (packets may be dropped during disconnect)
                        urlCache.saveUrl(tes.getBlockPos(), scr.side, finalUrl);
                        // DIRECT WRITE: Save straight to server-side wd_server_urls.json via shared singleton.
                        // In single-player/client+server same-JVM the server-side ServerUrlCache is reachable
                        // through WebDisplaysMod.INSTANCE; this eliminates the dependency on a late-arriving
                        // network packet during the closing window.
                        try {
                            net.montoyo.wd.WebDisplaysMod.INSTANCE.saveUrlToServerCache(tes.getBlockPos(), scr.side, finalUrl);
                        } catch (Exception e) {
                            WebDisplays.LOGGER.warn("[WebDisplays] ServerUrlCache direct-write failed (non-fatal): {}", e.getMessage());
                        }
                        
                        WebDisplays.LOGGER.debug("[WebDisplays] EMERGENCY FLUSH (screenTracking): " + scr.side + " -> " + finalUrl);
                        syncedCount++;
                    }
                    
                    // Kill audio immediately
                    scr.browser.loadURL("about:blank");
                }
            }
        }
        
        // FALLBACK: If screenTracking is empty but allBrowsers has browsers,
        // use browserToScreen map to sync URLs (this happens during disconnect)
        if (syncedCount == 0) {
            WebDisplays.LOGGER.debug("[WebDisplays] screenTracking empty, trying allBrowsers fallback...");
            for (CefBrowser browser : proxy.getAllBrowsers()) {
                ScreenBlockEntity tes = proxy.getScreenForBrowser(browser);
                BlockSide side = proxy.getSideForBrowser(browser);
                if (tes != null && side != null && browser != null) {
                    String finalUrl = browser.getURL();
                    if (finalUrl != null && !finalUrl.isEmpty()) {
                        tes.updateClientSideURL(browser, finalUrl);
                        lastSyncedUrl.put(browser, finalUrl);
                        syncUrlToServer(tes, side, finalUrl);
                        // CRITICAL: Save to disk for persistence (packets may be dropped during disconnect)
                        urlCache.saveUrl(tes.getBlockPos(), side, finalUrl);
                        // DIRECT WRITE: Save straight to server-side wd_server_urls.json via shared singleton.
                        // In single-player the client and integrated server share a JVM, so WebDisplaysMod.INSTANCE
                        // exposes the same server-side ServerUrlCache instance.  This eliminates the dependency on a
                        // late-arriving network packet during the closing window.
                        try {
                            net.montoyo.wd.WebDisplaysMod.INSTANCE.saveUrlToServerCache(tes.getBlockPos(), side, finalUrl);
                        } catch (Exception e) {
                            WebDisplays.LOGGER.warn("[WebDisplays] ServerUrlCache direct-write failed (non-fatal): {}", e.getMessage());
                        }
                        WebDisplays.LOGGER.debug("[WebDisplays] EMERGENCY FLUSH (allBrowsers): " + side + " -> " + finalUrl);
                        syncedCount++;
                    }
                    browser.loadURL("about:blank");
                }
            }
        }
        
        WebDisplays.LOGGER.debug("[WebDisplays] EMERGENCY FLUSH COMPLETE - synced " + syncedCount + " screens");
        
        emergencyFlushActive = false;
    }
    
    /**
     * NUCLEAR OPTION: Direct server sync for single-player
     * In single-player mode, we can directly access the integrated server
     * and call setScreenURL() synchronously, bypassing the network entirely.
     * This guarantees the URL is saved before the world closes.
     */
    private void syncUrlToServer(ScreenBlockEntity clientTE, BlockSide side, String url) {
        WebDisplays.LOGGER.debug("[WebDisplays] syncUrlToServer START: " + side + " -> " + url);
        try {
            Minecraft mc = Minecraft.getInstance();
            WebDisplays.LOGGER.debug("[WebDisplays]   hasSingleplayerServer=" + mc.hasSingleplayerServer() + ", getSingleplayerServer=" + mc.getSingleplayerServer());
            if (mc.getSingleplayerServer() != null) {
                // SINGLE-PLAYER: Direct server access
                ServerLevel serverLevel = mc.getSingleplayerServer().getLevel(clientTE.getLevel().dimension());
                WebDisplays.LOGGER.debug("[WebDisplays]   serverLevel=" + serverLevel);
                if (serverLevel != null) {
                    var be = serverLevel.getBlockEntity(clientTE.getBlockPos());
                    WebDisplays.LOGGER.debug("[WebDisplays]   serverBE=" + be);
                    
                    // If block entity is null, force-load the chunk
                    if (be == null) {
                        WebDisplays.LOGGER.debug("[WebDisplays]   Block entity not found, force-loading chunk...");
                        var chunk = serverLevel.getChunk(clientTE.getBlockPos());
                        WebDisplays.LOGGER.debug("[WebDisplays]   Chunk loaded: " + chunk);
                        be = serverLevel.getBlockEntity(clientTE.getBlockPos());
                        WebDisplays.LOGGER.debug("[WebDisplays]   serverBE after chunk load=" + be);
                    }
                    
                    if (be instanceof net.montoyo.wd.entity.ScreenBlockEntity serverTE) {
                        // Get old URL for logging
                        String oldUrl = "unknown";
                        try {
                            var screen = serverTE.getScreen(side);
                            if (screen != null) oldUrl = screen.url;
                        } catch (Exception e) {}
                        WebDisplays.LOGGER.debug("[WebDisplays]   Setting URL: '" + oldUrl + "' -> '" + url + "'");
                        
                        serverTE.setScreenURL(side, url);
                        
                        // CRITICAL: setScreenURL() server-branch now saves to ServerUrlCache,
                        // but that might fail if ServerUrlCache is not yet initialized in this context.
                        // Do an explicit, direct save here too — single-player direct sync
                        // completely bypasses WDNetworkRegistry, so without this line
                        // ServerUrlCache stays at 0 URLs for the entire session.
                        try {
                            net.montoyo.wd.WebDisplaysMod mod = net.montoyo.wd.WebDisplaysMod.INSTANCE;
                            if (mod != null && mod.getServerUrlCache() != null) {
                                WebDisplays.LOGGER.debug("[WebDisplays]   Saving to ServerUrlCache (direct sync): " + url);
                                mod.getServerUrlCache().saveUrl(clientTE.getBlockPos(), side, url);
                            }
                        } catch (Exception e) {
                            WebDisplays.LOGGER.warn("[WebDisplays]   ServerUrlCache save failed: {}", e.getMessage());
                        }
                        
                        WebDisplays.LOGGER.debug("[WebDisplays] DIRECT SYNC SUCCESS: " + side + " -> " + url);
                        return; // Done - no packet needed
                    }
                }
            }
            // MULTIPLAYER: Fall back to packet
            WebDisplays.LOGGER.debug("[WebDisplays]   Falling back to packet");
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                new UrlUpdatePayload(clientTE.getBlockPos(), side, url)
            );
            WebDisplays.LOGGER.debug("[WebDisplays] PACKET SYNC: " + side + " -> " + url);
        } catch (Exception e) {
            WebDisplays.LOGGER.error("[WebDisplays] syncUrlToServer ERROR: {}", e.getMessage());
            e.printStackTrace();
            // Fallback to packet on any error
            net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                new UrlUpdatePayload(clientTE.getBlockPos(), side, url)
            );
            WebDisplays.LOGGER.debug("[WebDisplays] PACKET SYNC (error fallback): " + side + " -> " + url);
        }
    }
    
    /**
     * Manual save - can be called from GUI. Bypasses priority filter.
     */
    public void forceManualSave(CefBrowser browser) {
        if (browser == null) return;
        
        String url = browser.getURL();
        if (url == null || url.isEmpty()) return;
        
        WebDisplays.LOGGER.debug("[WebDisplays] MANUAL SAVE: " + url);
        
        ClientProxy proxy = ((ClientProxy) WebDisplays.PROXY);
        for (ScreenBlockEntity tes : proxy.getScreens()) {
            BlockSide side = tes.getSideForBrowser(browser);
            if (side != null) {
                tes.updateClientSideURL(browser, url);
                lastSyncedUrl.put(browser, url);
                net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
                    new UrlUpdatePayload(tes.getBlockPos(), side, url)
                );
            }
        }
    }

	@Override
	public boolean onCursorChange(CefBrowser browser, int cursorType) {
		ClientProxy proxy = ((ClientProxy) WebDisplays.PROXY);
		ScreenBlockEntity tes = proxy.getScreenForBrowser(browser);
		if (tes != null) {
			BlockSide side = proxy.getSideForBrowser(browser);
			if (side != null) {
				ScreenData scr = tes.getScreen(side);
				if (scr != null) scr.mouseType = cursorType;
			}
		}
		return false;
	}
}
