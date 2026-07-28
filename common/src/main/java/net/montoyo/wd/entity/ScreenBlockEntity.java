/*
 * Copyright (C) 2019 BARBOTIN Nicolas
 */

package net.montoyo.wd.entity;

import com.cinemamod.mcef.MCEFBrowser;
import org.cef.browser.CefBrowser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.HolderLookup;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.config.CommonConfig;
import net.montoyo.wd.controls.builtin.ClickControl;
import net.montoyo.wd.core.DefaultUpgrade;
import net.montoyo.wd.core.IUpgrade;
import net.montoyo.wd.core.ScreenRights;
import net.montoyo.wd.data.ScreenConfigData;
import net.montoyo.wd.miniserv.SyncPlugin;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.client_bound.S2CMessageAddScreen;
import net.montoyo.wd.net.client_bound.S2CMessageScreenUpdate;
import net.montoyo.wd.registry.BlockRegistry;
import net.montoyo.wd.registry.ItemRegistry;
import net.montoyo.wd.registry.TileRegistry;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Multiblock;
import net.montoyo.wd.utilities.ScreenIterator;
import net.montoyo.wd.utilities.VideoType;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.data.Rotation;
import net.montoyo.wd.utilities.math.MutableAABB;
import net.montoyo.wd.utilities.math.Vector2i;
import net.montoyo.wd.utilities.math.Vector3f;
import net.montoyo.wd.utilities.math.Vector3i;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;
import net.montoyo.wd.utilities.serialization.TypeData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ScreenBlockEntity extends BlockEntity {
    public ScreenBlockEntity(BlockPos arg2, BlockState arg3) {
        super(TileRegistry.SCREEN_BLOCK_ENTITY, arg2, arg3);
    }

    public void forEachScreenBlocks(BlockSide side, Consumer<BlockPos> func) {
        ScreenData scr = getScreen(side);

        if (scr != null) {
            ScreenIterator it = new ScreenIterator(getBlockPos(), side, scr.size);

            while (it.hasNext())
                func.accept(it.next());
        }
    }

    private final ArrayList<ScreenData> screens = new ArrayList<>();
    private net.minecraft.world.phys.AABB renderBB = new net.minecraft.world.phys.AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
    private boolean loaded = true;
    public float ytVolume = Float.POSITIVE_INFINITY;
    
    private transient boolean isBeingRemoved = false;
    private transient List<ScreenData> lastScreens = new ArrayList<>();

    public boolean isLoaded() {
        return loaded;
    }

    public void markLoaded() {
        loaded = true;
    }

    public void load() {
        loaded = true;
        // Browser creation deferred to initBrowsersClient() to prevent freeze
    }

    public void unload() {
        // Close browsers but keep screen data for reload
        for (ScreenData scr : screens) {
            if (scr.browser != null) {
                scr.browser.close(true);
                scr.browser = null;
            }
        }
        // Don't clear screens - just mark unloaded so browsers recreate on load()
        loaded = false;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        ListTag list = tag.getList("WDScreens", CompoundTag.TAG_COMPOUND);
        if (list.isEmpty())
            return;

        Map<BlockSide, String> oldUrls = new HashMap<>();
        Map<BlockSide, CefBrowser> oldBrowsers = new HashMap<>();
        Map<BlockSide, NameUUIDPair> oldOwners = new HashMap<>();

        for (ScreenData screen : screens) {
            oldUrls.put(screen.side, screen.url);
            oldBrowsers.put(screen.side, screen.browser);
            oldOwners.put(screen.side, screen.owner);
        }

        screens.clear();

        for (int i = 0; i < list.size(); i++) {
            CompoundTag screenTag = list.getCompound(i);
            ScreenData newScreen = ScreenData.deserialize(screenTag);

            CefBrowser existingBrowser = oldBrowsers.get(newScreen.side);
            if (existingBrowser != null) {
                newScreen.browser = existingBrowser;
            }

            screens.add(newScreen);

            String loadedUrl = screenTag.getString("URL");
            Log.debug("[WebDisplays] loadAdditional: Loaded screen side=%s url='%s' from NBT", newScreen.side, loadedUrl);
            if (level != null && level.isClientSide && existingBrowser != null) {
                String oldUrl = oldUrls.get(newScreen.side);
                if (oldUrl != null && !oldUrl.equals(newScreen.url)) {
                    Log.debug("Screen URL changed from %s to %s - navigating existing browser", oldUrl, newScreen.url);
                    existingBrowser.loadURL(newScreen.url);
                }
            }

            if (level != null && level.isClientSide) {
                NameUUIDPair oldOwner = oldOwners.get(newScreen.side);
                if (oldOwner != null) {
                    newScreen.owner = oldOwner;
                }
            }
        }

        if (level != null && level.isClientSide) {
            markLoaded();
        }
    }

    // 1. Sends the data to the client when the chunk loads
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        // Use lastScreens when removed (same as saveAdditional) to prevent empty network sync
        java.util.List<ScreenData> source = (isBeingRemoved && lastScreens != null) ? lastScreens : screens;
        for (ScreenData scr : source) {
            list.add(scr.serialize());
        }
        tag.put("WDScreens", list);
        return tag;
    }

    // Called when client receives update packet from server
    // Note: This method is used by Fabric 1.21 to handle chunk data sync
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        Log.debug("ScreenBlockEntity: handleUpdateTag at %s, screens=%d", worldPosition, screens.size());
        loadAdditional(tag, registries);
        updateAABB();
        Log.debug("ScreenBlockEntity: after loadAdditional, screens=%d", screens.size());
        // Reload URL for existing browsers when URL changed; browser creation is
        // handled by ScreenRenderer's lazy queue to prevent game freeze.
        for (ScreenData screen : screens) {
            if (screen.browser != null) {
                String currentUrl = screen.browser.getURL();
                if (currentUrl == null || !currentUrl.equals(screen.url)) {
                    Log.debug("ScreenBlockEntity: Reloading URL from %s to %s", currentUrl, screen.url);
                    screen.browser.loadURL(screen.url);
                }
            }
        }
        // FORCE the renderer to see the new data
        if (level != null && level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(getBlockPos(), state, state, 3);
        }
    }

    // Fabric 1.21 sync method - sends data to clients when block changes
    @Nullable
    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        // This creates the packet that travels from Server to Client
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // Handle incoming sync packet from server (for rejoining world)
    // Note: In 1.21 Fabric, packet handling is done via handleUpdateTag
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, registries);
            // Browser creation deferred to initBrowsersClient() to prevent freeze
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        // FORCE URL sync from browser before saving - prevents "backwards" save issue
        // SKIP if block is being removed (to allow onRemove to clear URLs properly)
        if (level != null && level.isClientSide && !isBeingRemoved) {
            for (ScreenData scr : screens) {
                if (scr.browser != null) {
                    String currentUrl = scr.browser.getURL();
                    if (currentUrl != null && !currentUrl.equals(scr.url)) {
                        Log.debug("[WebDisplays] Pre-save URL sync: '%s' -> '%s'", scr.url, currentUrl);
                        scr.url = currentUrl;
                        scr.videoType = VideoType.getTypeFromURL(currentUrl);
                    }
                }
            }
        } else if (isBeingRemoved) {
            Log.debug("[WebDisplays] saveAdditional: Skipping URL sync - block is being removed");
        }

        super.saveAdditional(tag, registries);

        // When being removed, persist the last known screens instead of empty data.
        // This prevents the final world save (during server stop) from writing empty
        // WDScreens and losing all screen data on reload.
        if (isBeingRemoved) {
            java.util.List<ScreenData> source;
            if (lastScreens != null && !lastScreens.isEmpty()) {
                source = lastScreens;
            } else if (!screens.isEmpty()) {
                source = screens;
            } else {
                source = null;
            }
            if (source != null) {
                ListTag list = new ListTag();
                for (ScreenData scr : source) {
                    CompoundTag screenTag = scr.serialize();
                    list.add(screenTag);
                    Log.debug("[WebDisplays] saveAdditional (removed): Saving preserved screen side=%s url='%s'", 
                        scr.side, screenTag.getString("URL"));
                }
                tag.put("WDScreens", list);
            } else {
                tag.put("WDScreens", new ListTag());
            }
            return;
        }

        ListTag list = new ListTag();
        for (ScreenData scr : screens) {
            CompoundTag screenTag = scr.serialize();
            list.add(screenTag);
            Log.debug("[WebDisplays] saveAdditional: Saving screen side=%s url='%s' or=%d (0x%X)", 
                scr.side, screenTag.getString("URL"), scr.otherRights, scr.otherRights);
        }

        tag.put("WDScreens", list);
    }

 	public ScreenData addScreen(BlockSide side, Vector2i size, @Nullable Vector2i resolution, @Nullable Player owner, boolean sendUpdate) {
		for (ScreenData scr : screens) {
			if (scr.side == side) {
				if (owner != null) {
					scr.owner = new NameUUIDPair(owner.getGameProfile());
					if (!level.isClientSide)
						setChanged();
				}
				return scr;
			}
		}

        ScreenData ret = new ScreenData();
        ret.side = side;
        ret.size = size;
        ret.url = CommonConfig.Browser.homepage;
        ret.friends = new ArrayList<>();
        ret.friendRights = ScreenRights.DEFAULTS;
        ret.otherRights = ScreenRights.DEFAULTS;
        ret.upgrades = new ArrayList<>();

        if (owner != null) {
            ret.owner = new NameUUIDPair(owner.getGameProfile());

            if (side == BlockSide.TOP || side == BlockSide.BOTTOM) {
                int rot = (int) Math.floor(((double) (owner.getYRot() * 4.0f / 360.0f)) + 2.5) & 3;

                if (side == BlockSide.TOP) {
                    if (rot == 1)
                        rot = 3;
                    else if (rot == 3)
                        rot = 1;
                }

                ret.rotation = Rotation.values()[rot];
            }
        }

        if (resolution == null || resolution.x < 1 || resolution.y < 1) {
            float psx = ((float) size.x) * 16.f - 4.f;
            float psy = ((float) size.y) * 16.f - 4.f;
            psx *= 8.f;
            psy *= 8.f;

            ret.resolution = new Vector2i((int) psx, (int) psy);
        } else
            ret.resolution = resolution;

        ret.clampResolution();

        if (!level.isClientSide) {
            ret.setupRedstoneStatus(level, getBlockPos());

            if (sendUpdate)
                WDNetworkRegistry.INSTANCE.sendToAllPlayers(new S2CMessageAddScreen(this, ret));
        }

        screens.add(ret);

        if (level.isClientSide)
            updateAABB();
        else
            setChanged();

//        level.blockEntityChanged(worldPosition);

        return ret;
    }

    public ScreenData getScreen(BlockSide side) {
        for (ScreenData scr : screens) {
            if (scr.side == side)
                return scr;
        }

        return null;
    }

    public int screenCount() {
        return screens.size();
    }

    public ScreenData getScreen(int idx) {
        return screens.get(idx);
    }

    public void markForRemoval() {
        isBeingRemoved = true;
        Log.debug("[WebDisplays] ScreenBlockEntity marked for removal at %s", worldPosition);
    }

    /**
     * Client-side validation: check if screen structure is still valid.
     * Called to detect when blocks are broken and stop rendering.
     */
    public void validateStructureClient() {
        if (!level.isClientSide) return;
        
        for (int i = screens.size() - 1; i >= 0; i--) {
            ScreenData scr = screens.get(i);
            Vector3i origin = new Vector3i(getBlockPos());
            Vector3i error = Multiblock.check(level, origin, scr.size, scr.side);
            if (error != null && level.isLoaded(error.toBlock())) {
                if (scr.browser != null) {
                    scr.browser.close(true);
                    scr.browser = null;
                }
                screens.remove(i);
            }
        }
        
        if (screens.isEmpty()) {
            loaded = false;
            WebDisplays.PROXY.trackScreen(this, false);
        }
    }

    public void clear() {
        // very important that these get closed
        for (ScreenData screen : screens)
            if (screen.browser != null) {
                screen.browser.close(true);
                screen.browser = null;
            }
        screens.clear();

        if (!level.isClientSide)
            setChanged();
    }

    public static String url(Level level, String url) throws IOException {
        Log.debug("URL received: " + url);
        if (!level.isClientSide) {
            List<ServerPlayer> serverPlayers = WebDisplays.PROXY.getServer().getPlayerList().getPlayers();
            SyncPlugin.syncPlayers(serverPlayers);
            for (ServerPlayer serverPlayer : serverPlayers) {
                SyncPlugin.setPlayerString(serverPlayer, url);
            }
            return url;
        } else {
            return url;
        }
    }

    public void setScreenURL(BlockSide side, String url) throws IOException {
        ScreenData scr = getScreen(side);
        if (scr == null) {
            Log.error("Attempt to change URL of non-existing screen on side %s", side.toString());
            return;
        }

        Log.debug("ScreenBlockEntity.setScreenURL: Received URL='%s' for side %s (client=%s)", url, side, level.isClientSide);
        
        String weburl = url(level, url);
        Log.debug("ScreenBlockEntity.setScreenURL: After url() processing: '%s'", weburl);

        weburl = WebDisplays.applyBlacklist(weburl);
        Log.debug("ScreenBlockEntity.setScreenURL: After blacklist: '%s'", weburl);
        
        scr.url = weburl;
        Log.debug("ScreenBlockEntity.setScreenURL: URL set to scr.url='%s'", scr.url);
        scr.videoType = VideoType.getTypeFromURL(weburl);

        if (level.isClientSide) {

            boolean wasCreated = false;
            if (scr.browser == null) {
                scr.createBrowser(this, false, level);
                wasCreated = true;
            }
            if (!wasCreated && scr.browser != null) {
                String currentBrowserUrl = scr.browser.getURL();
                String normalizedCurrent = currentBrowserUrl != null ? currentBrowserUrl.replaceAll("/$", "") : "";
                String normalizedNew = weburl != null ? weburl.replaceAll("/$", "") : "";
                if (!normalizedNew.equalsIgnoreCase(normalizedCurrent)) {
                    scr.browser.loadURL(weburl);
                }
                WebDisplays.PROXY.setBrowserTargetUrl(scr.browser, weburl);
            }
        } else {
            WDNetworkRegistry.INSTANCE.sendToAllPlayers(S2CMessageScreenUpdate.setURL(this, side, weburl));
            // Mark block entity as dirty so it gets saved to NBT
            // This is critical for URL persistence across world saves
            setChanged();
            // Sync BlockEntity data to all clients watching this block
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);

            // Write to server-side URL cache immediately so the server has the
            // latest URL even if a chunk-level NBT race occurs during world save
            try {
                net.montoyo.wd.WebDisplaysMod mod = net.montoyo.wd.WebDisplaysMod.INSTANCE;
                if (mod != null && mod.getServerUrlCache() != null) {
                    WebDisplays.LOGGER.debug("setScreenURL[server-side]: Saving URL to server cache: {}", weburl);
                    mod.getServerUrlCache().saveUrl(getBlockPos(), side, weburl);
                }
            } catch (Exception e) {
                Log.warning("setScreenURL: ServerUrlCache save failed: %s", e.getMessage());
            }
        }
    }

    public void removeScreen(BlockSide side) {
        int idx = -1;
        for (int i = 0; i < screens.size(); i++) {
            if (screens.get(i).side == side) {
                idx = i;
                break;
            }
        }

        if (idx < 0) {
            Log.error("Tried to delete non-existing screen on side %s", side.toString());
            return;
        }

        if (level.isClientSide) {
            if (screens.get(idx).browser != null) {
                screens.get(idx).browser.close(true);
                screens.get(idx).browser = null;
            }
        } else
            WDNetworkRegistry.INSTANCE.sendToAllPlayers(new S2CMessageScreenUpdate(this.getBlockPos(), side)); //Delete the screen

        screens.remove(idx);

        if (!level.isClientSide) {
            if (screens.isEmpty()) //No more screens: remove tile entity
                level.setBlock(getBlockPos(), BlockRegistry.SCREEN_BLOCK.defaultBlockState().setValue(ScreenBlock.hasTE, false), 3);
            else
                setChanged();
        }
    }

    public void setResolution(BlockSide side, Vector2i res) {
        if (res.x < 1 || res.y < 1) {
            Log.warning("Call to TileEntityScreen.setResolution(%s) with suspicious values X=%d and Y=%d", side.toString(), res.x, res.y);
            return;
        }

        ScreenData scr = getScreen(side);
        if (scr == null) {
            Log.error("Tried to change resolution of non-existing screen on side %s", side.toString());
            return;
        }

        scr.resolution = res;
        scr.clampResolution();

        if (level.isClientSide) {
            WebDisplays.PROXY.screenUpdateResolutionInGui(new Vector3i(getBlockPos()), side, res);

            if (scr.browser != null) {
                scr.browser.close(true);
                scr.browser = null; //Will be re-created by renderer
            }
        } else {
            WDNetworkRegistry.INSTANCE.sendToAllPlayers(S2CMessageScreenUpdate.setResolution(this, side, res));
            setChanged();
        }
    }

    private static Player getLaserUser(ScreenData scr) {
        if (scr.laserUser != null) {
            if (scr.laserUser.isRemoved() || !scr.laserUser.getItemInHand(InteractionHand.MAIN_HAND).getItem().equals(ItemRegistry.LASER_POINTER))
                scr.laserUser = null;
        }

        return scr.laserUser;
    }

    private static void checkLaserUserRights(ScreenData scr) {
        if (scr.laserUser != null && (scr.rightsFor(scr.laserUser) & ScreenRights.INTERACT) == 0)
            scr.laserUser = null;
    }

    public void clearLaserUser(BlockSide side) {
        ScreenData scr = getScreen(side);

        if (scr != null)
            scr.laserUser = null;
    }

    public void click(BlockSide side, Vector2i vec) {
        ScreenData scr = getScreen(side);
        if (scr == null) {
            Log.error("Attempt click non-existing screen of side %s", side.toString());
            return;
        }

        if (level.isClientSide)
            Log.warning("TileEntityScreen.click() from client side is useless...");
        else if (getLaserUser(scr) == null)
            WDNetworkRegistry.INSTANCE.sendToAllPlayers(S2CMessageScreenUpdate.click(this, side, ClickControl.ControlType.CLICK, vec));
    }

    public void handleMouseEvent(BlockSide side, ClickControl.ControlType event, @Nullable Vector2i vec, int button) {
        if (button > 1) return;

        ScreenData scr = getScreen(side);
        if (scr == null) {
            Log.error("Attempt inject mouse events on non-existing screen of side %s", side.toString());
            return;
        }

        if (scr.browser instanceof MCEFBrowser mcefBrowser) {
            if (button == 1) button = 0;
            else if (button == 0) button = 1;

            if (event == ClickControl.ControlType.CLICK) {
                mcefBrowser.sendMouseMove(vec.x, vec.y);
                mcefBrowser.sendMousePress(vec.x, vec.y, button);
                mcefBrowser.sendMouseRelease(vec.x, vec.y, button);
            } else if (event == ClickControl.ControlType.DOWN) {
                mcefBrowser.sendMouseMove(vec.x, vec.y);
                mcefBrowser.sendMousePress(vec.x, vec.y, button);
            } else if (event == ClickControl.ControlType.MOVE)
                mcefBrowser.sendMouseMove(vec.x, vec.y);
            else if (event == ClickControl.ControlType.UP)
                mcefBrowser.sendMouseRelease(scr.lastMousePos.x, scr.lastMousePos.y, button);

            mcefBrowser.setFocus(true);

            if (vec != null) {
                scr.lastMousePos.x = vec.x;
                scr.lastMousePos.y = vec.y;
            }
        }
    }

//	public void updateJSRedstone(BlockSide side, Vector2i vec, int redstoneLevel) {
//		Screen scr = getScreen(side);
//		if (scr == null) {
//			Log.error("Called updateJSRedstone on non-existing side %s", side.toString());
//			return;
//		}
//
//		if (level.isClientSide) {
//			if (scr.browser != null)
//				scr.browser.runJS("if(typeof webdisplaysRedstoneCallback == \"function\") webdisplaysRedstoneCallback(" + vec.x + ", " + vec.y + ", " + redstoneLevel + ");", "");
//		} else {
//			boolean sendMsg = false;
//
//			if (scr.redstoneStatus == null) {
//				scr.setupRedstoneStatus(level, getBlockPos());
//				sendMsg = true;
//			} else {
//				int idx = vec.y * scr.size.x + vec.x;
//
//				if (scr.redstoneStatus.get(idx) != redstoneLevel) {
//					scr.redstoneStatus.set(idx, redstoneLevel);
//					sendMsg = true;
//				}
//			}
//
////            if (sendMsg)
////                WDNetworkRegistry.INSTANCE.send(PacketDistributor.NEAR.with(() -> point(level, getBlockPos())), S2CMessageScreenUpdate.jsRedstone(this, side, vec, redstoneLevel));
//		}
//	}
//
//	public void handleJSRequest(ServerPlayer src, BlockSide side, int reqId, JSServerRequest req, Object[] data) {
//		if (level.isClientSide) {
//			Log.error("Called handleJSRequest client-side");
//			return;
//		}
//
//		Screen scr = getScreen(side);
//		if (scr == null) {
//			Log.error("Called handleJSRequest on non-existing side %s", side.toString());
//			WDNetworkRegistry.INSTANCE.send(PacketDistributor.PLAYER.with(() -> src), new S2CMessageJSResponse(reqId, req, 403, "Invalid side"));
//			return;
//		}
//
//		if (!scr.owner.uuid.equals(src.getGameProfile().getId())) {
//			Log.warning("Player %s (UUID %s) tries to use the redstone output API on a screen he doesn't own!", src.getName(), src.getGameProfile().getId().toString());
//			WDNetworkRegistry.INSTANCE.send(PacketDistributor.PLAYER.with(() -> src), new S2CMessageJSResponse(reqId, req, 403, "Only the owner can do that"));
//			return;
//		}
//
//		if (scr.upgrades.stream().noneMatch(DefaultUpgrade.REDOUTPUT::matchesRedInput)) {
//			WDNetworkRegistry.INSTANCE.send(PacketDistributor.PLAYER.with(() -> src), new S2CMessageJSResponse(reqId, req, 403, "Missing upgrade"));
//			return;
//		}
//
//		if (req == JSServerRequest.CLEAR_REDSTONE) {
//			final BlockPos.MutableBlockPos mbp = new BlockPos.MutableBlockPos();
//			final Vector3i vec1 = new Vector3i(getBlockPos());
//			final Vector3i vec2 = new Vector3i();
//
//			for (int y = 0; y < scr.size.y; y++) {
//				vec2.set(vec1);
//
//				for (int x = 0; x < scr.size.x; x++) {
//					vec2.toBlock(mbp);
//
//					BlockState bs = level.getBlockState(mbp);
//					if (bs.getValue(BlockScreen.emitting))
//						level.setBlock(mbp, bs.setValue(BlockScreen.emitting, false), Block.UPDATE_ALL_IMMEDIATE);
//
//					vec2.add(side.right.x, side.right.y, side.right.z);
//				}
//
//				vec1.add(side.up.x, side.up.y, side.up.z);
//			}
//
//			WDNetworkRegistry.INSTANCE.send(PacketDistributor.PLAYER.with(() -> src), new S2CMessageJSResponse(reqId, req, new byte[0]));
//		} else if (req == JSServerRequest.SET_REDSTONE_AT) {
//			int x = (Integer) data[0];
//			int y = (Integer) data[1];
//			boolean state = (Boolean) data[2];
//
//			if (x < 0 || x >= scr.size.x || y < 0 || y >= scr.size.y)
//				WDNetworkRegistry.INSTANCE.send(PacketDistributor.PLAYER.with(() -> src), new S2CMessageJSResponse(reqId, req, 403, "Out of range"));
//			else {
//				BlockPos bp = (new Vector3i(getBlockPos())).addMul(side.right, x).addMul(side.up, y).toBlock();
//				BlockState bs = level.getBlockState(bp);
//
//				if (!bs.getValue(BlockScreen.emitting).equals(state))
//					level.setBlockAndUpdate(bp, bs.setValue(BlockScreen.emitting, state));
//
//				WDNetworkRegistry.INSTANCE.send(PacketDistributor.PLAYER.with(() -> src), new S2CMessageJSResponse(reqId, req, new byte[0]));
//			}
//		} else
//			WDNetworkRegistry.INSTANCE.send(PacketDistributor.PLAYER.with(() -> src), new S2CMessageJSResponse(reqId, req, 400, "Invalid request"));
//	}

    // Fabric 1.21 equivalent of Forge onLoad() — called when BE is added to a world
    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level.isClientSide) {
            if (WebDisplays.PROXY != null) {
                WebDisplays.PROXY.trackScreen(this, true);
            }
        }
    }

    @Override
    public void setRemoved() {
        if (level != null) {
            Log.debug("ScreenBlockEntity: setRemoved - closing %d screens at %s (client=%s)", 
                screens.size(), worldPosition, level.isClientSide);
            
            // Save lastScreens immediately for NBT persistence and browser cleanup
            lastScreens = new ArrayList<>(screens);
            
            // CLIENT-SIDE: Sync URLs to server BEFORE any cleanup
            // isBeingRemoved is NOT yet true, so saveAdditional() will write URLs to NBT
            if (level.isClientSide) {
                for (ScreenData scr : lastScreens) {
                    if (scr.browser != null) {
                        String finalUrl = scr.browser.getURL();
                        if (finalUrl != null && !finalUrl.isEmpty()) {
                            // Update local URL data and force NBT write now (not later!)
                            if (!finalUrl.equals(scr.url)) {
                                Log.debug("  Pre-removal URL sync for side %s: '%s' -> '%s'", scr.side, scr.url, finalUrl);
                                scr.url = finalUrl;
                                setChanged(); // Mark dirty NOW so forest fires don't eat our data
                            }
                            // Send to server immediately
                            Log.debug("  Sending URL sync packet for side %s: %s", scr.side, finalUrl);
                            WebDisplays.PROXY.syncUrlToServer(this, scr.side, finalUrl);
                        }
                    }
                }
                // Untrack from client proxy
                WebDisplays.PROXY.trackScreen(this, false);
                
                // Clear cache entries for broken blocks (prevents "ghost URLs")
                for (ScreenData scr : lastScreens) {
                    WebDisplays.PROXY.deleteCachedUrl(worldPosition, scr.side);
                }
            }
            
            // Both sides: close all browsers
            for (ScreenData scr : lastScreens) {
                if (scr.browser != null) {
                    Log.debug("  Closing browser for side %s", scr.side);
                    WebDisplays.PROXY.trackBrowser(scr.browser, false);
                    scr.browser.close(true);
                    scr.browser = null;
                }
            }
            screens.clear();
            isBeingRemoved = true;
        }
        loaded = false;
        super.setRemoved();
    }

    // 1.21 method name for getUpdateTag - ensures chunk data sync to client
    public CompoundTag toInitialChunkDataNbt(HolderLookup.Provider registries) {
        return getUpdateTag(registries);
    }

    private void updateAABB() {
        Vector3i origin = new Vector3i(getBlockPos());
        MutableAABB box = null;

        for (ScreenData scr : screens) {
            Vector3i f = scr.side.forward;

            int fx = Math.max(f.x, 0);
            int fy = Math.max(f.y, 0);
            int fz = Math.max(f.z, 0);
            int ox = 0;
            if (scr.side.equals(BlockSide.NORTH)) ox = 1;
            int oz = 0;
            if (
                    scr.side.equals(BlockSide.EAST) ||
                            scr.side.equals(BlockSide.TOP) ||
                            scr.side.equals(BlockSide.BOTTOM)
            ) oz = 1;

            if (box == null) {
                box = new MutableAABB(
                        origin.x + fx + ox,
                        origin.y + fy,
                        origin.z + fz + oz,

                        origin.x + ox + scr.side.right.x * scr.size.x + fx + scr.side.up.x * scr.size.y,
                        origin.y + scr.side.right.y * scr.size.x + fy + scr.side.up.y * scr.size.y,
                        origin.z + oz + scr.side.right.z * scr.size.x + fz + scr.side.up.z * scr.size.y
                );
            } else {
                box.expand(
                        origin.x + fx + ox,
                        origin.y + fy,
                        origin.z + fz + oz,

                        origin.x + ox + scr.side.right.x * scr.size.x + fx + scr.side.up.x * scr.size.y,
                        origin.y + scr.side.right.y * scr.size.x + fy + scr.side.up.y * scr.size.y,
                        origin.z + oz + scr.side.right.z * scr.size.x + fz + scr.side.up.z * scr.size.y
                );
            }
        }

        if (box == null) renderBB = new AABB(worldPosition);
        else renderBB = box.toMc();
    }

    @NotNull
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return renderBB;
    }

//	public void updateTrackDistance(double d, float masterVolume) {
//		final WebDisplays wd = WebDisplays.INSTANCE;
//		boolean needsComputation = true;
//		int intPart = 0; //Need to initialize those because the compiler is stupid
//		int fracPart = 0;
//
//		for (Screen scr : screens) {
//			if (scr.autoVolume && scr.videoType != null && scr.browser != null && !scr.browser.isPageLoading()) {
//				if (needsComputation) {
//					float dist = (float) Math.sqrt(d);
//					float vol;
//
//					if (dist <= wd.avDist100)
//						vol = masterVolume * wd.ytVolume;
//					else if (dist >= wd.avDist0)
//						vol = 0.0f;
//					else
//						vol = (1.0f - (dist - wd.avDist100) / (wd.avDist0 - wd.avDist100)) * masterVolume * wd.ytVolume;
//
//					if (Math.abs(ytVolume - vol) < 0.5f)
//						return; //Delta is too small
//
//					ytVolume = vol;
//					intPart = (int) vol; //Manually convert to string, probably faster in that case...
//					fracPart = ((int) (vol * 100.0f)) - intPart * 100;
//					needsComputation = false;
//				}
//
//				scr.browser.runJS(scr.videoType.getVolumeJSQuery(intPart, fracPart), "");
//			}
//		}
//	}

    // Called from DisplayHandler when browser navigates to a new URL
    // This updates ScreenData.url so it persists when saving/quitting
    // NOTE: DisplayHandler also sends C2S packet to sync server-side URL
    public void updateClientSideURL(Object target, String url) {
        if (target == null || url == null) return;

        for (ScreenData scr : screens) {
            if (scr.browser == target) {
                // Update the URL in ScreenData so it gets saved to NBT
                if (!url.equals(scr.url)) {
                    Log.debug("ScreenBlockEntity: URL changed from '%s' to '%s'", scr.url, url);
                    scr.url = url;
                    scr.videoType = VideoType.getTypeFromURL(url);
                    // Mark as changed so NBT is saved
                    setChanged();
                }
                break;
            }
        }
    }
    
    // Get the side for a given browser (used by DisplayHandler for C2S sync)
    public BlockSide getSideForBrowser(Object browser) {
        if (browser == null) return null;
        for (ScreenData scr : screens) {
            if (scr.browser == browser) {
                return scr.side;
            }
        }
        return null;
    }



    public void addFriend(ServerPlayer ply, BlockSide side, NameUUIDPair pair) {
        if (!level.isClientSide) {
            ScreenData scr = getScreen(side);
            if (scr == null) {
                Log.error("Tried to add friend to invalid screen side %s", side.toString());
                return;
            }

            if (!scr.friends.contains(pair)) {
                scr.friends.add(pair);
                (new ScreenConfigData(new Vector3i(getBlockPos()), side, scr)).updateOnly().sendTo(level, getBlockPos());
                setChanged();
            }
        }
    }

    public void removeFriend(ServerPlayer ply, BlockSide side, NameUUIDPair pair) {
        if (!level.isClientSide) {
            ScreenData scr = getScreen(side);
            if (scr == null) {
                Log.error("Tried to remove friend from invalid screen side %s", side.toString());
                return;
            }

            if (scr.friends.remove(pair)) {
                checkLaserUserRights(scr);
                (new ScreenConfigData(new Vector3i(getBlockPos()), side, scr)).updateOnly().sendTo(level, getBlockPos());
                setChanged();
            }
        }
    }

    public void setRights(ServerPlayer ply, BlockSide side, int fr, int or) {
        if (!level.isClientSide) {
            ScreenData scr = getScreen(side);
            if (scr == null) {
                Log.error("Tried to change rights of invalid screen on side %s", side.toString());
                return;
            }

            Log.debug("setRights: fr=%d->%d or=%d->%d", scr.friendRights, fr, scr.otherRights, or);
            scr.friendRights = fr;
            scr.otherRights = or;

            checkLaserUserRights(scr);
            (new ScreenConfigData(new Vector3i(getBlockPos()), side, scr)).updateOnly().sendTo(level, getBlockPos());
            setChanged();
        }
    }

    public void type(BlockSide side, String text, BlockPos soundPos) {
        type(side, text, soundPos, null);
    }

    public void type(BlockSide side, String text, BlockPos soundPos, @Nullable ServerPlayer sender) {
        ScreenData scr = getScreen(side);
        if (scr == null) {
            Log.error("Tried to type on invalid screen on side %s", side.toString());
            return;
        }

        if (level.isClientSide) {
            if (scr.browser instanceof MCEFBrowser mcefBrowser) {
                try {
                    if (text.startsWith("t")) {
                        for (int i = 1; i < text.length(); i++) {
                            char chr = text.charAt(i);
                            if (chr == 1)
                                break;

                            mcefBrowser.sendKeyTyped(chr, 0);
                        }
                    } else {
                        TypeData[] data = WebDisplays.GSON.fromJson(text, TypeData[].class);

                        for (TypeData ev : data) {
                            if (ev.getKeyCode() == 257) {
                                ev = new TypeData(
                                        ev.getAction(),
                                        10, ev.getModifier(),
                                        ev.getScanCode()
                                );
                            }

                            switch (ev.getAction()) {
                                case PRESS -> {
                                    mcefBrowser.sendKeyPress(ev.getKeyCode(), ev.getScanCode(), ev.getModifier());
                                    if (ev.getKeyCode() == 10)
                                        mcefBrowser.sendKeyTyped('\r', ev.getModifier());
                                }
                                case RELEASE ->
                                        mcefBrowser.sendKeyRelease(ev.getKeyCode(), ev.getScanCode(), ev.getModifier());
                                case TYPE ->
                                        mcefBrowser.sendKeyTyped((char) ev.getKeyCode(), ev.getModifier());

                                default -> throw new RuntimeException("Invalid type action '" + ev.getAction() + '\'');
                            }
                        }
                    }
                } catch (Throwable t) {
                    Log.warningEx("Suspicious keyboard type packet received...", t);
                }
            }
        } else {
            if (sender != null)
                WDNetworkRegistry.sendToNearExcept(sender, S2CMessageScreenUpdate.type(this, side, text));
            else
                WDNetworkRegistry.INSTANCE.sendToAllPlayers(S2CMessageScreenUpdate.type(this, side, text));

            if (soundPos != null)
                playSoundAt(WebDisplays.INSTANCE.soundTyping, soundPos, 0.25f, 1.f);
        }
    }

    private void playSoundAt(SoundEvent snd, BlockPos at, float vol, float pitch) {
        double x = at.getX();
        double y = at.getY();
        double z = at.getZ();

        level.playSound(null, x + 0.5, y + 0.5, z + 0.5, snd, SoundSource.BLOCKS, vol, pitch);
    }

//	public void updateUpgrades(BlockSide side, ItemStack[] upgrades) {
//		if (!level.isClientSide) {
//			Log.error("Tried to call TileEntityScreen.updateUpgrades() from server side...");
//			return;
//		}
//
//		Screen scr = getScreen(side);
//		if (scr == null) {
//			Log.error("Tried to update upgrades on invalid screen on side %s", side.toString());
//			return;
//		}
//
//		scr.upgrades.clear();
//		Collections.addAll(scr.upgrades, upgrades);
//
//		if (scr.browser != null)
//			scr.browser.runJS("if(typeof webdisplaysUpgradesChanged == \"function\") webdisplaysUpgradesChanged();", "");
//	}

    private static String safeName(ItemStack is) {
        return is.getItem().getName(is).getString();
    }

    //If equal is null, no duplicate check is preformed
    public boolean addUpgrade(BlockSide side, ItemStack is, @Nullable Player player, boolean abortIfExisting) {
        if (level.isClientSide) {
            IUpgrade itemAsUpgrade = (IUpgrade) is.getItem();
            ScreenData scr = getScreen(side);
//            if (abortIfExisting && scr.upgrades.stream().anyMatch(otherStack -> itemAsUpgrade.isSameUpgrade(is, otherStack)))
//                return false; //Upgrade already exists
            ItemStack isCopy = is.copy();
            scr.upgrades.add(isCopy);
            itemAsUpgrade.onInstall(this, side, player, isCopy);
            return false;
        }

        ScreenData scr = getScreen(side);
        if (scr == null) {
            Log.error("Tried to add an upgrade on invalid screen on side %s", side.toString());
            return false;
        }

        if (!(is.getItem() instanceof IUpgrade)) {
            Log.error("Tried to add a non-upgrade item %s to screen (%s does not implement IUpgrade)", safeName(is), is.getItem().getClass().getCanonicalName());
            return false;
        }

        if (scr.upgrades.size() >= 16) {
            Log.error("Can't insert upgrade %s in screen %s at %s: too many upgrades already!", safeName(is), side.toString(), getBlockPos().toString());
            return false;
        }

        IUpgrade itemAsUpgrade = (IUpgrade) is.getItem();
        if (abortIfExisting && scr.upgrades.stream().anyMatch(otherStack -> itemAsUpgrade.isSameUpgrade(is, otherStack)))
            return false; //Upgrade already exists

        ItemStack isCopy = is.copyWithCount(1);

        scr.upgrades.add(isCopy);
        if (player != null && !player.level().isClientSide) {
            WDNetworkRegistry.INSTANCE.sendToAllPlayers(S2CMessageScreenUpdate.upgrade(this, side, true, is));
            itemAsUpgrade.onInstall(this, side, player, isCopy);
            playSoundAt(WebDisplays.INSTANCE.soundUpgradeAdd, getBlockPos(), 1.0f, 1.0f);
        }
        setChanged();
        return true;
    }

    public boolean hasUpgrade(BlockSide side, ItemStack is) {
        ScreenData scr = getScreen(side);
        if (scr == null)
            return false;

        if (!(is.getItem() instanceof IUpgrade))
            return false;

        IUpgrade itemAsUpgrade = (IUpgrade) is.getItem();
        return scr.upgrades.stream().anyMatch(otherStack -> itemAsUpgrade.isSameUpgrade(is, otherStack));
    }

    public boolean hasUpgrade(BlockSide side, DefaultUpgrade du) {
        ScreenData scr = getScreen(side);
        if (scr == null) return false;
        return scr.upgrades.stream().anyMatch(stack -> du.matches(stack, du));
    }

    public void removeUpgrade(BlockSide side, ItemStack is, @Nullable Player player) {
        if (level.isClientSide)
            return;

        ScreenData scr = getScreen(side);
        if (scr == null) {
            Log.error("Tried to remove an upgrade on invalid screen on side %s", side.toString());
            return;
        }

        if (!(is.getItem() instanceof IUpgrade)) {
            Log.error("Tried to remove a non-upgrade item %s to screen (%s does not implement IUpgrade)", safeName(is), is.getItem().getClass().getCanonicalName());
            return;
        }

        int idxToRemove = -1;
        IUpgrade itemAsUpgrade = (IUpgrade) is.getItem();

        for (int i = 0; i < scr.upgrades.size(); i++) {
            if (itemAsUpgrade.isSameUpgrade(is, scr.upgrades.get(i))) {
                idxToRemove = i;
                break;
            }
        }

        if (idxToRemove >= 0) {
            dropUpgrade(scr.upgrades.get(idxToRemove), side, player);
            scr.upgrades.remove(idxToRemove);
            if (player != null && player instanceof ServerPlayer serverPlayer && !player.level().isClientSide) {
                WDNetworkRegistry.sendToNearExcept(serverPlayer, S2CMessageScreenUpdate.upgrade(this, side, false, is));
                playSoundAt(WebDisplays.INSTANCE.soundUpgradeDel, getBlockPos(), 1.0f, 1.0f);
            }
            setChanged();
        } else
            Log.warning("Tried to remove non-existing upgrade %s to screen %s at %s", safeName(is), side.toString(), getBlockPos().toString());
    }

    private void dropUpgrade(ItemStack is, BlockSide side, @Nullable Player ply) {
        if (!((IUpgrade) is.getItem()).onRemove(this, side, ply, is)) { //Drop upgrade item
            boolean spawnDrop = true;

            if (ply != null) {
                if (ply.isCreative() || ply.getInventory().add(is))
                    spawnDrop = false; //If in creative or if the item was added to the player's inventory, don't spawn drop entity
            }

            if (spawnDrop) {
                Vector3f pos = new Vector3f((float) this.getBlockPos().getX(), (float) this.getBlockPos().getY(), (float) this.getBlockPos().getZ());
                pos.addMul(side.backward.toFloat(), 1.5f);

                if (level != null) {
                    level.addFreshEntity(new ItemEntity(level, pos.x, pos.y, pos.z, is));
                }
            }
        }
    }

    private ScreenData getScreenForLaserOp(BlockSide side, Player ply) {
        if (level.isClientSide)
            return null;

        ScreenData scr = getScreen(side);
        if (scr == null) {
            Log.error("Called laser operation on invalid screen on side %s", side.toString());
            return null;
        }

        if ((scr.rightsFor(ply) & ScreenRights.INTERACT) == 0)
            return null; //Don't output an error, it can 'legally' happen

        if (scr.upgrades.stream().noneMatch(DefaultUpgrade.LASERMOUSE::matchesLaserMouse)) {
            return null;
        }

        return scr; //Okay, go for it...
    }

    public void laserDownMove(BlockSide side, Player ply, Vector2i pos, boolean down, int button) {
        ScreenData scr = getScreenForLaserOp(side, ply);

        if (scr != null) {
            if (button == -1)
                WDNetworkRegistry.INSTANCE.sendToAllPlayers(S2CMessageScreenUpdate.click(this, side, ClickControl.ControlType.MOVE, pos, -1));
            else if (down)
                WDNetworkRegistry.INSTANCE.sendToAllPlayers(S2CMessageScreenUpdate.click(this, side, ClickControl.ControlType.DOWN, pos, button));
            else
                WDNetworkRegistry.INSTANCE.sendToAllPlayers(S2CMessageScreenUpdate.click(this, side, ClickControl.ControlType.UP, pos, button));
        }
    }

    public void laserUp(BlockSide side, Player ply, int button) {
        ScreenData scr = getScreenForLaserOp(side, ply);

        if (scr != null) {
            if (getLaserUser(scr) == ply) {
                scr.laserUser = null;
                WDNetworkRegistry.INSTANCE.sendToAllPlayers(S2CMessageScreenUpdate.click(this, side, ClickControl.ControlType.UP, null, button));
            }
        }
    }

    public void onDestroy(@Nullable Player ply) {
        for (ScreenData scr : screens) {
            scr.upgrades.forEach(is -> dropUpgrade(is, scr.side, ply));
            scr.upgrades.clear();
        }

        WDNetworkRegistry.INSTANCE.sendToAllPlayers(S2CMessageScreenUpdate.turnOff(getBlockPos(), null));
    }

    public void disableScreen(BlockSide side) {
        ScreenData remove = null;
        for (ScreenData screen : screens) {
            if (screen.side == side) {
                remove = screen;
                break;
            }
        }

        if (remove == null) return;

        if (level != null && !level.isClientSide) {
            final ScreenData scrn = remove;
            remove.upgrades.forEach(is -> dropUpgrade(is, scrn.side, null));
        }

        remove.upgrades.clear();
        if (remove.browser != null) {
            remove.browser.close(true);
        }
        screens.remove(remove);
    }

    public void setOwner(BlockSide side, Player newOwner) {
        if (level.isClientSide) {
            Log.error("Called TileEntityScreen.setOwner() on client...");
            return;
        }

        if (newOwner == null) {
            Log.error("Called TileEntityScreen.setOwner() with null owner");
            return;
        }

        ScreenData scr = getScreen(side);
        if (scr == null) {
            Log.error("Called TileEntityScreen.setOwner() on invalid screen on side %s", side.toString());
            return;
        }

        scr.owner = new NameUUIDPair(newOwner.getGameProfile());
        WDNetworkRegistry.INSTANCE.sendToAllPlayers(S2CMessageScreenUpdate.owner(this, side, scr.owner));
        checkLaserUserRights(scr);
        setChanged();
    }

    public void setOwner(BlockSide side, NameUUIDPair newOwner) {
        if (level.isClientSide) {
            Log.error("Called TileEntityScreen.setOwner() on client...");
            return;
        }

        if (newOwner == null) {
            Log.error("Called TileEntityScreen.setOwner() with null owner");
            return;
        }

        ScreenData scr = getScreen(side);
        if (scr == null) {
            Log.error("Called TileEntityScreen.setOwner() on invalid screen on side %s", side.toString());
            return;
        }

        scr.owner = newOwner;
        WDNetworkRegistry.INSTANCE.sendToAllPlayers(S2CMessageScreenUpdate.owner(this, side, scr.owner));
        checkLaserUserRights(scr);
        setChanged();
    }

    public void setRotation(BlockSide side, Rotation rot) {
        ScreenData scr = getScreen(side);
        if (scr == null) {
            Log.error("Trying to change rotation of invalid screen on side %s", side.toString());
            return;
        }

        if (level.isClientSide) {
            boolean oldWasVertical = scr.rotation.isVertical;
            scr.rotation = rot;

            WebDisplays.PROXY.screenUpdateRotationInGui(new Vector3i(getBlockPos()), side, rot);

            if (scr.browser != null && oldWasVertical != rot.isVertical) {
                scr.browser.close(true);
                scr.browser = null; //Will be re-created by renderer
            }
        } else {
            scr.rotation = rot;
            WDNetworkRegistry.INSTANCE.sendToAllPlayers(S2CMessageScreenUpdate.rotation(this, side, rot));
            setChanged();
        }
    }

//	public void evalJS(BlockSide side, String code) {
//		Screen scr = getScreen(side);
//		if (scr == null) {
//			Log.error("Trying to run JS code on invalid screen on side %s", side.toString());
//			return;
//		}
//
//		if (level.isClientSide) {
//			if (scr.browser != null)
//				scr.browser.runJS(code, "");
//		}
////        else WDNetworkRegistry.INSTANCE.send(PacketDistributor.NEAR.with(() -> point(level, getBlockPos())), S2CMessageScreenUpdate.js(this, side, code));
//	}

    public void setAutoVolume(BlockSide side, boolean av) {
        ScreenData scr = getScreen(side);
        if (scr == null) {
            Log.error("Trying to toggle auto-volume on invalid screen (side %s)", side.toString());
            return;
        }

        scr.autoVolume = av;

        if (level.isClientSide)
            WebDisplays.PROXY.screenUpdateAutoVolumeInGui(new Vector3i(getBlockPos()), side, av);
        else {
            WDNetworkRegistry.INSTANCE.sendToAllPlayers(S2CMessageScreenUpdate.autoVolume(this, side, av));
            setChanged();
        }
    }

    public void deactivate() {
        for (ScreenData screen : screens) {
            if (screen.browser != null) {
                screen.browser.close(true);
                screen.browser = null;
            }
        }
    }

    public void turnOff(BlockSide side) {
        ScreenData scr = getScreen(side);
        if (scr != null && scr.browser != null) {
            scr.browser.close(true);
            scr.browser = null;
        }
    }

    public void activate() {
        for (ScreenData screen : screens) {
            if (screen.browser == null)
                screen.createBrowser(this, false, level);
        }
    }

    public void interact(BlockHitResult result, Consumer<Vector2i> func) {
        BlockState state = getBlockState();
        if (state.getBlock() instanceof ScreenBlock) {
            Vector3i pos = new Vector3i(result.getBlockPos());
            BlockSide side = BlockSide.values()[result.getDirection().ordinal()];

            // Use level parameter instead of Minecraft.getInstance().level for common code
            Multiblock.findOrigin(level, pos, side, null);

            //Since rights aren't synchronized, let the server check them for us...
            ScreenData scr = this.getScreen(side);

            if (scr != null && scr.browser != null) {
                float hitX = ((float) result.getLocation().x) - (float) pos.x;
                float hitY = ((float) result.getLocation().y) - (float) pos.y;
                float hitZ = ((float) result.getLocation().z) - (float) pos.z;
                Vector2i tmp = new Vector2i();

                if (ScreenBlock.hit2pixels(side, result.getBlockPos(), new Vector3i(result.getBlockPos()), scr, hitX, hitY, hitZ, tmp)) {
                    func.accept(tmp);
                }
            }
        }
    }

    private static Direction getNearestDirection(double x, double y, double z) {
        Direction dir = Direction.NORTH;
        float max = Float.MIN_VALUE;
        for (Direction d : Direction.values()) {
            float dot = (float)(x * d.getStepX() + y * d.getStepY() + z * d.getStepZ());
            if (dot > max) {
                max = dot;
                dir = d;
            }
        }
        return dir;
    }

    public BlockHitResult trace(BlockSide side, Vec3 start, Vec3 look) {
        AABB box = getRenderBoundingBox();
        double pHitDistance = box.distanceToSqr(start) + 2.0;

        Vec3 vec32 = start.add(look.x * pHitDistance, look.y * pHitDistance, look.z * pHitDistance);

        box = box.move(
                -getBlockPos().getX(),
                -getBlockPos().getY(),
                -getBlockPos().getZ()
        );

        BlockHitResult bhr = AABB.clip(java.util.Arrays.asList(box), start, vec32, getBlockPos());
        if (bhr == null || bhr.getType() != HitResult.Type.BLOCK || bhr.getDirection().ordinal() != side.ordinal()) {
            bhr = AABB.clip(java.util.Arrays.asList(box), vec32, start, getBlockPos());
            if (bhr == null || bhr.getType() != HitResult.Type.BLOCK || bhr.getDirection().ordinal() != side.ordinal()) {
                return BlockHitResult.miss(
                        vec32,
                        bhr == null ? getNearestDirection(look.x, look.y, look.z).getOpposite() : bhr.getDirection(),
                        getBlockPos()
                );
            }
        }

        return bhr;
    }

    public void onInterfaceRemoved(AbstractInterfaceBlockEntity iface) {
        // Remove the interface from all screens' interfaces lists
        for (ScreenData data : screens) {
            if (data != null && data.interfaces != null) {
                data.interfaces.remove(iface);
            }
        }
    }

//    @Override
//    public boolean shouldRefresh(Level world, BlockPos pos, @NotNull BlockState oldState, @NotNull BlockState newState) {
//        if(oldState.getBlock() != WebDisplays.INSTANCE.blockScreen || newState.getBlock() != WebDisplays.INSTANCE.blockScreen)
//            return true;
//
//        return oldState.getValue(BlockScreen.hasTE) != newState.getValue(BlockScreen.hasTE);
//    }
}
