package net.montoyo.wd;

import com.mojang.authlib.GameProfile;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.montoyo.wd.core.HasAdvancement;
import net.montoyo.wd.core.JSServerRequest;
import net.montoyo.wd.data.ScreenConfigData;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.data.Rotation;
import net.montoyo.wd.utilities.math.Vector2i;
import net.montoyo.wd.utilities.math.Vector3i;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;

import java.util.UUID;
import org.cef.browser.CefBrowser;

public class SharedProxy {

    public CefBrowser createBrowser(String url, boolean transparent) {
        return null; // Client-side only - override in ClientProxy
    }

    /**
     * Create browser data directory for a screen block position
     * Client-side only - override in ClientProxy
     */
    public void createBrowserDataDir(BlockPos pos) {
        // No-op on server, implemented in ClientProxy
    }

    /**
     * Track a browser for force-cleanup when leaving world
     * Client-side only - override in ClientProxy
     */
    public void trackBrowser(CefBrowser browser, boolean track) {
        trackBrowser(browser, track, null, null);
    }
    
    /**
     * Track a browser with associated screen/side for URL sync
     * Client-side only - override in ClientProxy
     */
    public void trackBrowser(CefBrowser browser, boolean track, ScreenBlockEntity screen, BlockSide side) {
        // No-op on server, implemented in ClientProxy
    }
    
    /**
     * Set the target URL for a browser to prevent sync loops.
     * When server tells client to load a URL, we mark it as "target" so
     * onAddressChange doesn't sync it back to server.
     * Client-side only - override in ClientProxy
     */
    public void setBrowserTargetUrl(CefBrowser browser, String url) {
        // No-op on server, implemented in ClientProxy
    }
    
    /**
     * Force immediate manual save of current browser URL to server.
     * This bypasses all cooldowns and stability checks for user-initiated saves.
     * Client-side only - override in ClientProxy
     */
    public void forceManualBrowserSave(CefBrowser browser) {
        // No-op on server, implemented in ClientProxy
    }
    
    /**
     * Sync URL to server immediately. Used during setRemoved to ensure URL is saved.
     * Client-side only - override in ClientProxy
     */
    public void syncUrlToServer(ScreenBlockEntity screen, BlockSide side, String url) {
        // No-op on server, implemented in ClientProxy
    }
    
    /**
     * Get cached URL for a position/side. Used during browser creation to prevent double-load.
     * Client-side only - override in ClientProxy
     */
    public String getCachedUrl(net.minecraft.core.BlockPos pos, BlockSide side) {
        return null; // No-op on server, implemented in ClientProxy
    }
    
    /**
     * Delete cached URL when block is broken. Prevents "ghost URLs".
     * Client-side only - override in ClientProxy
     */
    public void deleteCachedUrl(net.minecraft.core.BlockPos pos, BlockSide side) {
        // No-op on server, implemented in ClientProxy
    }

    public void preInit() {
    }

    public void init() {
    }

    public void postInit() {
    }

    public void onCefInit() {
    }

    public Level getWorld(ResourceKey<Level> dim) {
        return null;
    }

    public void enqueue(Runnable r) {
        r.run();
    }

    public void displayGui(String guiName, FriendlyByteBuf data) {
    }

    public void displayGui(ScreenConfigData data) {
    }

    public void displayRedstoneCtrl(ResourceLocation dimension, BlockPos pos, String risingEdgeURL, String fallingEdgeURL) {
    }

    public void trackScreen(ScreenBlockEntity tes, boolean track) {
    }

    public void onAutocompleteResult(NameUUIDPair[] pairs) {
    }

    public Player getLocalPlayer() {
        return null;
    }

    public Player getPlayer(UUID id) {
        return null;
    }

    public Player[] getPlayers() {
        return new Player[0];
    }

    public GameProfile[] getOnlineGameProfiles() {
        MinecraftServer server = WDNetworkRegistry.getServer();
        if (server == null) return new GameProfile[0];
        return server.getPlayerList().getPlayers().stream()
            .map(Player::getGameProfile).toArray(GameProfile[]::new);
    }

    public void screenUpdateResolutionInGui(Vector3i pos, BlockSide side, Vector2i res) {
    }

    public void screenUpdateRotationInGui(Vector3i pos, BlockSide side, Rotation rot) {
    }

    public void screenUpdateAutoVolumeInGui(Vector3i pos, BlockSide side, boolean av) {
    }

    public void displaySetPadURLGui(ItemStack is, String padURL) {
    }

    public void openMinePadGui(UUID padId) {
    }

    public void openLaserGui(Player player, ItemStack stack, InteractionHand hand) {
        // Client-side only - override in ClientProxy
    }

    public HasAdvancement hasClientPlayerAdvancement(ResourceLocation rl) {
        return HasAdvancement.DONT_KNOW;
    }

    public MinecraftServer getServer() {
        return null;
    }

    public void setMiniservClientPort(int port) {
    }

    public void startMiniservClient() {
    }

    public boolean decryptKey(byte[] encryptedKey) {
        return false;
    }

    public net.montoyo.wd.net.server_bound.C2SMessageMiniservConnect beginMiniservConnection() {
        return null;
    }

    public boolean isMiniservDisabled() {
        return true;
    }

    public void closeGui(net.minecraft.core.BlockPos bp, BlockSide bs) {
    }

    public void renderRecipes() {
    }

    public void handleJSResponseSuccess(int queryId, JSServerRequest type, byte[] data) {
        // Override in client proxy to handle successful JS responses
    }

    public void handleJSResponseError(int queryId, JSServerRequest type, int errorCode, String errorMessage) {
        // Override in client proxy to handle failed JS responses
    }

    public boolean isShiftDown() {
        return false;
    }

    // Client-side screen handling methods
    public void handleAddScreen(Vector3i pos, boolean clear, ScreenData[] screens) {
        // Client-side only - override in ClientProxy
    }

    public void handleScreenUpdate(Vector3i pos, BlockSide side, boolean clear, ScreenData[] screens) {
        // Client-side only - override in ClientProxy
    }

    public void handleScreenControl(BlockPos pos, BlockSide side, Object control) {
        // Client-side only - override in ClientProxy
    }

    public void openScreenConfigGui(Vector3i pos, BlockSide side, ScreenData screen) {
        // Client-side only - override in ClientProxy
    }

    // Native touch click - split CLICK into DOWN (now) + UP (next tick)
    // This gives YouTube's JS time to process the mousedown before mouseup
    public void scheduleNativeTouchUp(ScreenBlockEntity screen, BlockSide side) {
        // Client-side only - override in ClientProxy
    }
}
