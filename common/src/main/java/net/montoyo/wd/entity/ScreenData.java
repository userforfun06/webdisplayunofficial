package net.montoyo.wd.entity;

import com.cinemamod.mcef.MCEFBrowser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.config.CommonConfig;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.utilities.*;
import net.montoyo.wd.utilities.browser.InWorldQueries;
import net.montoyo.wd.utilities.browser.WDBrowser;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.data.Rotation;
import net.montoyo.wd.utilities.math.Vector2i;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;
import org.cef.browser.CefBrowser;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScreenData {
    public BlockSide side;
    public Vector2i size;
    public Vector2i resolution;
    public Rotation rotation = Rotation.ROT_0;
    public String url;
    protected VideoType videoType;
    public NameUUIDPair owner;
    public ArrayList<NameUUIDPair> friends;
    public int friendRights;
    public int otherRights;
    public CefBrowser browser;
    public ArrayList<ItemStack> upgrades;
    public boolean doTurnOnAnim;
    public long turnOnTime;
    public Player laserUser;
    public final Vector2i lastMousePos = new Vector2i();
    public NibbleArray redstoneStatus; //null on client
    public boolean autoVolume = true;
    public List<AbstractInterfaceBlockEntity> interfaces = new ArrayList<>();

    public int mouseType;
    
    // Prevent multiple concurrent browser creation attempts
    public transient boolean isCreatingBrowser = false;
    
    // Native touch scroll tracking
    public boolean isDragging = false;
    public int dragStartX = 0;
    public int dragStartY = 0;
    
    // ECHO GUARD: Set to true when loading URL from server packet, prevents onAddressChange from echoing back
    public transient boolean ignoreNextUrlChange = false;

    // General dedup: timestamp of last local click (native touch, laser, keyboard) for suppressing duplicate server echo
    public transient long lastClickTime = 0;

    public static ScreenData deserialize(CompoundTag tag) {
        ScreenData ret = new ScreenData();
        ret.side = BlockSide.values()[tag.getInt("Side")];
        ret.size = new Vector2i(tag.getInt("Width"), tag.getInt("Height"));
        ret.resolution = new Vector2i(tag.getInt("ResolutionX"), tag.getInt("ResolutionY"));
        ret.rotation = Rotation.values()[tag.getInt("Rotation")];
        String loadedUrl = tag.getString("URL");
        Log.debug("ScreenData.deserialize: Loaded URL='%s' from NBT for side", loadedUrl);
        ret.url = loadedUrl;
        // Fallback to default if URL is missing or empty
        if (ret.url.isEmpty()) {
            ret.url = CommonConfig.Browser.homepage;
            Log.debug("ScreenData.deserialize: URL was empty, using default: '{}'", ret.url);
        }
        // Migrate old default URL to new default
        if (ret.url.startsWith("mod://")) {
            Log.debug("ScreenData.deserialize: Migrating old URL '{}' to '{}'", ret.url, CommonConfig.Browser.homepage);
            ret.url = CommonConfig.Browser.homepage;
        }
        ret.videoType = VideoType.getTypeFromURL(ret.url);

        if (ret.resolution.x <= 0 || ret.resolution.y <= 0) {
            float psx = ((float) ret.size.x) * 16.f - 4.f;
            float psy = ((float) ret.size.y) * 16.f - 4.f;
            psx *= 8.f;
            psy *= 8.f;

            ret.resolution.x = (int) psx;
            ret.resolution.y = (int) psy;
        }

        if (tag.contains("OwnerName")) {
            String name = tag.getString("OwnerName");
            UUID uuid = tag.getUUID("OwnerUUID");
            ret.owner = new NameUUIDPair(name, uuid);
        }

        ListTag friends = tag.getList("Friends", 10);
        ret.friends = new ArrayList<>(friends.size());

        for (int i = 0; i < friends.size(); i++) {
            CompoundTag nf = friends.getCompound(i);
            NameUUIDPair pair = new NameUUIDPair(nf.getString("Name"), nf.getUUID("UUID"));
            ret.friends.add(pair);
        }

        ret.friendRights = tag.getInt("FriendRights");
        // Migration: screens saved before otherRights was initialized won't have the tag.
        // getInt returns 0 for missing tags, but 0 is also a valid value (NONE).
        // Use tag.contains to differentiate: only migrate if the tag truly doesn't exist.
        if (tag.contains("OtherRights")) {
            ret.otherRights = tag.getInt("OtherRights");
        } else {
            ret.otherRights = ScreenRights.DEFAULTS;
        }
        Log.debug("ScreenData.deserialize: Loaded friendRights=%d otherRights=%d from NBT", ret.friendRights, ret.otherRights);

        ListTag upgrades = tag.getList("Upgrades", 10);
        ret.upgrades = new ArrayList<>();

        for (int i = 0; i < upgrades.size(); i++) {
            // MC 1.21: Use ItemStack.CODEC for proper Data Components deserialization
            ItemStack.CODEC.parse(
                net.minecraft.nbt.NbtOps.INSTANCE, upgrades.get(i)
            ).result().ifPresent(ret.upgrades::add);
        }

        if (tag.contains("AutoVolume"))
            ret.autoVolume = tag.getBoolean("AutoVolume");

        return ret;
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Side", side.ordinal());
        tag.putInt("Width", size.x);
        tag.putInt("Height", size.y);
        tag.putInt("ResolutionX", resolution.x);
        tag.putInt("ResolutionY", resolution.y);
        tag.putInt("Rotation", rotation.ordinal());
        // Ensure URL is never empty when saving
        String urlToSave = (url == null || url.isEmpty()) ? CommonConfig.Browser.homepage : url;
        tag.putString("URL", urlToSave);
        Log.debug("ScreenData.serialize: Saving URL='%s' for side %s", urlToSave, side);

        if (owner == null)
            Log.warning("Found TES with NO OWNER!!");
        else {
            tag.putString("OwnerName", owner.name);
            tag.putUUID("OwnerUUID", owner.uuid);
        }

        ListTag list = new ListTag();
        for (NameUUIDPair f : friends) {
            CompoundTag nf = new CompoundTag();
            nf.putString("Name", f.name);
            nf.putUUID("UUID", f.uuid);

            list.add(nf);
        }

        tag.put("Friends", list);
        tag.putInt("FriendRights", friendRights);
        tag.putInt("OtherRights", otherRights);
        Log.debug("ScreenData.serialize: Saving friendRights=%d otherRights=%d to NBT", friendRights, otherRights);

        ListTag upgradesList = new ListTag();
        for (ItemStack is : upgrades) {
            // MC 1.21: Use ItemStack.CODEC for proper Data Components serialization
            if (!is.isEmpty()) {
                ItemStack.CODEC.encodeStart(
                    net.minecraft.nbt.NbtOps.INSTANCE, is
                ).result().ifPresent(upgradesList::add);
            }
        }
        tag.put("Upgrades", upgradesList);
        tag.putBoolean("AutoVolume", autoVolume);
        return tag;
    }

    public int rightsFor(Player ply) {
        return rightsFor(ply.getGameProfile().getId());
    }

    public int rightsFor(UUID uuid) {
        if (owner == null)
            return ScreenRights.NONE;
        if (owner.uuid.equals(uuid))
            return ScreenRights.ALL;

        return friends.stream().anyMatch(f -> f.uuid.equals(uuid)) ? friendRights : otherRights;
    }

    public void setupRedstoneStatus(Level world, BlockPos start) {
        if (world.isClientSide()) {
            Log.warning("Called Screen.setupRedstoneStatus() on client.");
            return;
        }

        if (redstoneStatus != null) {
            Log.warning("Called Screen.setupRedstoneStatus() on server, but redstone status is non-null");
            return;
        }

        Direction[] VALUES = Direction.values();
        redstoneStatus = new NibbleArray(size.x * size.y);
        final Direction facing = VALUES[side.reverse().ordinal()];
        final ScreenIterator it = new ScreenIterator(start, side, size);

        while (it.hasNext()) {
            int idx = it.getIndex();
            redstoneStatus.set(idx, world.getSignal(it.next(), facing));
        }
    }


    public void clampResolution() {
        if (resolution.x > CommonConfig.Screen.maxResolutionX) {
            float newY = ((float) resolution.y) * ((float) CommonConfig.Screen.maxResolutionX) / ((float) resolution.x);
            resolution.x = CommonConfig.Screen.maxResolutionX;
            resolution.y = (int) newY;
        }

        if (resolution.y > CommonConfig.Screen.maxResolutionY) {
            float newX = ((float) resolution.x) * ((float) CommonConfig.Screen.maxResolutionY) / ((float) resolution.y);
            resolution.x = (int) newX;
            resolution.y = CommonConfig.Screen.maxResolutionY;
        }
    }

    public void createBrowser(ScreenBlockEntity be, boolean doAnim, Level level) {
        if (!level.isClientSide()) return;
        if (isCreatingBrowser) return;
        isCreatingBrowser = true;
        try {
            if (browser != null) {
                WebDisplays.PROXY.trackBrowser(browser, false);
                browser.close(true);
                browser = null;
            }

            String nbtUrl = url != null ? url : "https://www.google.com";
            String cachedUrl = WebDisplays.PROXY.getCachedUrl(be.getBlockPos(), side);
            String targetUrl;

            WebDisplays.LOGGER.debug("createBrowser: DECISION POINT nbtUrl='{}' cacheUrl='{}'", nbtUrl, cachedUrl != null ? cachedUrl : "null");

            boolean nbtHasAnyUrl = !nbtUrl.isEmpty();
            boolean cacheHasUrl = cachedUrl != null && !cachedUrl.isEmpty();

            if (nbtHasAnyUrl) {
                targetUrl = WebDisplays.applyBlacklist(nbtUrl);
            } else if (cacheHasUrl) {
                targetUrl = WebDisplays.applyBlacklist(cachedUrl);
                url = cachedUrl;
            } else {
                targetUrl = "https://www.google.com";
            }

            browser = WDBrowser.createBrowser(targetUrl, false);
            WebDisplays.PROXY.setBrowserTargetUrl(browser, targetUrl);

            WebDisplays.PROXY.createBrowserDataDir(be.getBlockPos());

            if (level.isClientSide()) {
                WebDisplays.PROXY.trackBrowser(browser, true, be, side);
            }

            if (browser instanceof MCEFBrowser mcefBrowser) {
                if (rotation.isVertical)
                    mcefBrowser.resize(resolution.y, resolution.x);
                else
                    mcefBrowser.resize(resolution.x, resolution.y);
                mcefBrowser.setCursorChangeListener((type) -> mouseType = type);
            }

            if (browser instanceof WDBrowser wdBrowser) {
                InWorldQueries.attach(be, side, wdBrowser);
            }

            doTurnOnAnim = doAnim;
            turnOnTime = System.currentTimeMillis();
        } catch (Exception e) {
            WebDisplays.LOGGER.error("createBrowser ERROR: {}", e.getMessage());
        } finally {
            isCreatingBrowser = false;
        }
    }
}
