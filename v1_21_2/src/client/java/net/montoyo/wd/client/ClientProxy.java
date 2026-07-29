package net.montoyo.wd.client;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefMessageRouter;
import net.montoyo.wd.utilities.browser.handlers.DisplayHandler;
import net.montoyo.wd.utilities.browser.handlers.WDRouter;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.InteractionHand;
import net.minecraft.advancements.Advancement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.CoreShaders;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.WebDisplaysMod;
import net.montoyo.wd.SharedProxy;
import net.montoyo.wd.config.CommonConfig;
import net.montoyo.wd.data.ScreenConfigData;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.item.ItemLaserPointer;
import net.montoyo.wd.item.ItemMinePad2;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.data.Rotation;
import net.montoyo.wd.utilities.math.Vector2i;
import net.montoyo.wd.utilities.browser.WDBrowserHelper;
import net.montoyo.wd.utilities.browser.WDClientBrowser;
import net.montoyo.wd.utilities.math.Vector3i;
import net.montoyo.wd.utilities.serialization.NameUUIDPair;
import net.montoyo.wd.client.gui.*;
import net.montoyo.wd.client.gui.loading.GuiLoader;
import net.montoyo.wd.client.renderers.*;
import net.montoyo.wd.core.HasAdvancement;
import net.montoyo.wd.miniserv.client.Client;
import net.montoyo.wd.block.ScreenBlock;
import net.montoyo.wd.registry.BlockRegistry;
import net.montoyo.wd.registry.ItemRegistry;
import net.montoyo.wd.utilities.Log;
import net.montoyo.wd.utilities.Multiblock;
import com.google.gson.JsonObject;

import org.cef.misc.CefCursorType;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClientProxy extends SharedProxy implements ResourceManagerReloadListener {

	public ClientProxy() {
	}

	public static void renderCrosshair(Options options, int screenWidth, int screenHeight, int offset, GuiGraphics poseStack, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();

		if (mc.screen instanceof GuiKeyboard) {
			ci.cancel();
			return;
		}

		ItemStack stack = mc.player.getMainHandItem();
		ItemStack stack1 = mc.player.getOffhandItem();

		if (!(stack.getItem() instanceof ItemLaserPointer ||
				stack1.getItem() instanceof ItemLaserPointer))
			return;

		ResourceLocation tex = ResourceLocation.fromNamespaceAndPath("webdisplays", "textures/gui/cursors.png");
		int x = (screenWidth - 15) / 2;
		int y = (screenHeight - 15) / 2;

		if (!LaserPointerRenderer.isOn()) {
			blitDirect(poseStack, tex, x, y, 240f, 240f, 15, 15, 256, 256);
			ci.cancel();
			return;
		}

		BlockHitResult result = raycast(64.0);

		BlockPos bpos = result.getBlockPos();

		if (result.getType() != HitResult.Type.BLOCK || mc.level.getBlockState(bpos).getBlock() != BlockRegistry.SCREEN_BLOCK) {
			blitDirect(poseStack, tex, x, y, 240f, 240f, 15, 15, 256, 256);
			ci.cancel();
			return;
		}

		Vector3i pos = new Vector3i(result.getBlockPos());
		BlockSide side = BlockSide.values()[result.getDirection().ordinal()];

		Multiblock.findOrigin(mc.level, pos, side, null);
		BlockEntity be = mc.level.getBlockEntity(pos.toBlock());
		if (!(be instanceof ScreenBlockEntity te)) return;

		ScreenData sc = te.getScreen(side);

		if (sc == null) return;

		int coordX = sc.mouseType * 15;
		int coordY = coordX / 255;
		coordX -= coordY * 255;
		coordY *= 15;
		if (sc.mouseType >= CefCursorType.NOT_ALLOWED.ordinal()) coordX -= 15;

		blitDirect(poseStack, tex, x, y, (float)coordX, (float)coordY, 15, 15, 256, 256);

		ci.cancel();
	}

	private static void blitDirect(GuiGraphics gui, ResourceLocation tex, int x, int y, float u, float v, int w, int h, int texW, int texH) {
		RenderSystem.setShaderTexture(0, tex);
		RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

		Matrix4f matrix = gui.pose().last().pose();

		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder builder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		float uScale = 1f / texW;
		float vScale = 1f / texH;
		builder.addVertex(matrix, x, y + h, 0).setColor(255, 255, 255, 255).setUv(u * uScale, (v + h) * vScale);
		builder.addVertex(matrix, x + w, y + h, 0).setColor(255, 255, 255, 255).setUv((u + w) * uScale, (v + h) * vScale);
		builder.addVertex(matrix, x + w, y, 0).setColor(255, 255, 255, 255).setUv((u + w) * uScale, v * vScale);
		builder.addVertex(matrix, x, y, 0).setColor(255, 255, 255, 255).setUv(u * uScale, v * vScale);
		BufferUploader.drawWithShader(builder.build());

		RenderSystem.defaultBlendFunc();
		RenderSystem.disableBlend();
	}

	public List<ScreenBlockEntity> getScreens() {
		return screenTracking;
	}

	public Set<CefBrowser> getAllBrowsers() {
		return allBrowsers;
	}

	public ScreenBlockEntity getScreenForBrowser(CefBrowser browser) {
		return browserToScreen.get(browser);
	}

	public BlockSide getSideForBrowser(CefBrowser browser) {
		return browserToSide.get(browser);
	}

	public List<PadData> getPads() {
		return padList;
	}

	public class PadData {

		public MCEFBrowser view;
		public final UUID id;
		private boolean isInHotbar;
		private long lastURLSent;
		private final String url;

		public int activeCursor;
		private volatile boolean browserCreationQueued;

		private PadData(String url, UUID id) {
			this.url = url;
			this.id = id;
			this.isInHotbar = true;
			this.view = null;
			this.browserCreationQueued = false;
		}

		public void ensureBrowser() {
			if (view != null) return;
			synchronized (this) {
				if (view != null || browserCreationQueued) return;
				browserCreationQueued = true;
			}
			PadData capture = this;
			ScreenRenderer.queueBrowserCreation(() -> {
				try {
					WDClientBrowser browser = new WDClientBrowser(MCEF.getClient(), capture.url, false);
					browser.setCloseAllowed();
					browser.createImmediately();
					browser.resize((int) WebDisplaysMod.INSTANCE.padResX, (int) WebDisplaysMod.INSTANCE.padResY);
					WDBrowserHelper.registerQueries(browser);
					browser.setCursorChangeListener((cursorId) -> {
						capture.activeCursor = cursorId;
					});
					capture.view = browser;
					WebDisplaysDirs.getSubDir("browsers/minepad/" + capture.id);
					Log.info("MinePad browser created for pad %s", capture.id);
				} catch (Exception e) {
					Log.error("Failed to create MinePad browser", e);
				}
			});
		}

		public PadData(UUID id) {
			this(CommonConfig.Browser.homepage, id);
		}

		public MCEFBrowser getOrCreateBrowser() {
			ensureBrowser();
			return view;
		}

		public void updateTime() {
			lastURLSent = System.currentTimeMillis();
		}

		public String getUrl() {
			return url;
		}

		public long lastSent() {
			return lastURLSent;
		}
	}

	private Minecraft mc;
	private MinePadRenderer minePadRenderer;
	private LaserPointerRenderer laserPointerRenderer;

	private int miniservPort;

	private final Field advancementToProgressField = findAdvancementToProgressField();
	private ClientAdvancements lastAdvMgr;
	private Map<Advancement, AdvancementProgress> advancementToProgress;

	private final ArrayList<ScreenBlockEntity> screenTracking = new ArrayList<>();
	private final Set<CefBrowser> allBrowsers = Collections.synchronizedSet(new HashSet<>());
	private final Map<CefBrowser, ScreenBlockEntity> browserToScreen = Collections.synchronizedMap(new HashMap<>());
	private final Map<CefBrowser, BlockSide> browserToSide = Collections.synchronizedMap(new HashMap<>());
	private int lastTracked = 0;

	private static class PendingMouseUp {
		final ScreenBlockEntity screen;
		final BlockSide side;
		final Vector2i pos;
		PendingMouseUp(ScreenBlockEntity screen, BlockSide side, Vector2i pos) {
			this.screen = screen;
			this.side = side;
			this.pos = pos;
		}
	}
	private PendingMouseUp pendingMouseUp = null;

	private final HashMap<UUID, PadData> padMap = new HashMap<>();
	private final ArrayList<PadData> padList = new ArrayList<>();
	private final ConcurrentHashMap<UUID, String> minepadUrlCache = new ConcurrentHashMap<>();
	private static final java.io.File MINEPAD_CACHE_FILE = WebDisplaysDirs.getCacheFile("wd_minepad_cache.json");
	private static final com.google.gson.Gson MINEPAD_GSON = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
	private boolean minepadCacheLoaded = false;

	private void ensureMinepadCacheLoaded() {
		if (minepadCacheLoaded) return;
		synchronized (minepadUrlCache) {
			if (minepadCacheLoaded) return;
			try {
				if (MINEPAD_CACHE_FILE.exists()) {
					try (java.io.BufferedReader reader = new java.io.BufferedReader(
							new java.io.FileReader(MINEPAD_CACHE_FILE))) {
						com.google.gson.JsonObject json = MINEPAD_GSON.fromJson(reader, com.google.gson.JsonObject.class);
						if (json != null) {
							minepadUrlCache.clear();
							for (String key : json.keySet()) {
								minepadUrlCache.put(UUID.fromString(key), json.get(key).getAsString());
							}
						}
					}
				}
			} catch (Exception e) {
				Log.warning("[WebDisplays] Failed to load MinePad URL cache: %s", e.getMessage());
			}
			minepadCacheLoaded = true;
		}
	}

	private void flushMinepadCacheToDisk() {
		try {
			com.google.gson.JsonObject json = new com.google.gson.JsonObject();
			synchronized (minepadUrlCache) {
				for (Map.Entry<UUID, String> entry : minepadUrlCache.entrySet()) {
					json.addProperty(entry.getKey().toString(), entry.getValue());
				}
			}
			try (java.io.BufferedWriter writer = new java.io.BufferedWriter(
					new java.io.FileWriter(MINEPAD_CACHE_FILE))) {
				MINEPAD_GSON.toJson(json, writer);
				writer.flush();
			}
		} catch (Exception e) {
			Log.warning("[WebDisplays] Failed to flush MinePad URL cache: %s", e.getMessage());
		}
	}

	public String getCachedMinepadUrl(UUID padId) {
		ensureMinepadCacheLoaded();
		return minepadUrlCache.get(padId);
	}

	public void cacheMinepadUrl(UUID padId, String url) {
		if (padId == null || url == null || url.isEmpty()) return;
		ensureMinepadCacheLoaded();
		minepadUrlCache.put(padId, url);
	}
	private int minePadTickCounter = 0;

	@Override
	public void preInit() {
		super.preInit();
		mc = Minecraft.getInstance();
	}

	@Override
	public void init() {
		String mcefPath = System.getProperty("mcef.libraries.path");
		if (mcefPath != null)
			new java.io.File(mcefPath).mkdirs();

		java.io.File cacheDir = WebDisplaysDirs.getSubDir("cache");
		String cachePath = cacheDir.getAbsolutePath();
		System.setProperty("mcef.cache.path", cachePath);
		System.setProperty("mcef.persist_session_cookies", "true");
		Log.info("MCEF cache path configured to: " + cachePath);

		Log.info("CEF using default message loop mode");

		MCEF.scheduleForInit((cef) -> onCefInit());
		Log.info("MCEF library download scheduled");
	}

	@Override
	public void onCefInit() {
		minePadRenderer = new MinePadRenderer();
		laserPointerRenderer = new LaserPointerRenderer();

		if (!MCEF.isInitialized()) return;

		MCEF.getApp().getHandle().registerSchemeHandlerFactory(
				"webdisplays", "",
				(browser, frame, url, request) -> {
					return new WDScheme(request.getURL());
				}
		);

		try {
			MCEF.getClient().addDisplayHandler(DisplayHandler.INSTANCE);
			Log.info("[WebDisplays] DisplayHandler registered successfully");
		} catch (Exception e) {
			Log.error("[WebDisplays] Failed to register DisplayHandler: %s", e.getMessage());
		}
		MCEF.getClient().getHandle().addMessageRouter(CefMessageRouter.create(WDRouter.INSTANCE));
	}

	@Override
	public void postInit() {
		((ReloadableResourceManager) mc.getResourceManager()).registerReloadListener(this);
	}

	@Override
	public Level getWorld(ResourceKey<Level> dim) {
		Level ret = mc.level;
		if (ret != null) {
			if (!ret.dimension().equals(dim))
				throw new RuntimeException("Can't get non-current dimension " + dim + " from client.");
			return ret;
		} else {
			throw new RuntimeException("Level on client is null");
		}
	}

	@Override
	public void enqueue(Runnable r) {
		mc.submit(r);
	}

	@Override
	public void displayGui(String guiName, FriendlyByteBuf data) {
		if ("keyboard".equals(guiName) && data != null) {
			Vector3i screenPos = new Vector3i(data.readInt(), data.readInt(), data.readInt());
			BlockSide side = BlockSide.values()[data.readByte()];
			int kbX = data.readInt();
			int kbY = data.readInt();
			int kbZ = data.readInt();

			BlockPos screenBlockPos = new BlockPos(screenPos.x, screenPos.y, screenPos.z);
			BlockPos kbBlockPos = new BlockPos(kbX, kbY, kbZ);

			BlockEntity be = mc.level.getBlockEntity(screenBlockPos);
			if (be instanceof ScreenBlockEntity tes) {
				mc.execute(() -> mc.setScreen(new GuiKeyboard(tes, side, kbBlockPos)));
			} else {
				Log.error("Keyboard GUI: ScreenBlockEntity not found at %s", screenBlockPos);
			}
		} else if ("seturl".equals(guiName) && data != null) {
			Vector3i pos = new Vector3i(data.readInt(), data.readInt(), data.readInt());
			BlockSide side = BlockSide.valueOf(data.readUtf());
			String url = data.readUtf();
			boolean isRemote = data.readBoolean();
			Vector3i remoteLocation = new Vector3i(data.readInt(), data.readInt(), data.readInt());

			GuiSetURL2 gui = new GuiSetURL2(pos.toBlock(), side, url,
				isRemote ? remoteLocation : new Vector3i());
			mc.setScreen(gui);
		} else if ("server".equals(guiName) && data != null) {
			BlockPos pos = data.readBlockPos();
			String name = data.readUtf();
			UUID uuid = data.readUUID();
			NameUUIDPair owner = new NameUUIDPair(name, uuid);
			GuiServer gui = new GuiServer(new Vector3i(pos), owner);
			mc.setScreen(gui);
		}
	}

	@Override
	public void displayRedstoneCtrl(ResourceLocation dimension, BlockPos pos, String risingEdgeURL, String fallingEdgeURL) {
		mc.execute(() -> mc.setScreen(new GuiRedstoneCtrl(
				Component.literal("Redstone Controller"), dimension, new Vector3i(pos),
				risingEdgeURL, fallingEdgeURL
		)));
	}

	@Override
	public void displayGui(ScreenConfigData data) {
		mc.execute(() -> {
			if (mc.screen instanceof GuiScreenConfig currentGsc) {
				if (currentGsc.isForBlock(data.pos.toBlock(), data.side)) {
					currentGsc.updateFriends(data.friends);
					currentGsc.updateFriendRights(data.friendRights);
					currentGsc.updateOtherRights(data.otherRights);
					currentGsc.updateMyRights();
					return;
				}
			}

			if (data.onlyUpdate)
				return;

			BlockEntity te = mc.level.getBlockEntity(data.pos.toBlock());
			if (te == null || !(te instanceof ScreenBlockEntity tes)) {
				Log.error("TileEntity at %s is not a screen; can't open gui!", data.pos.toString());
				return;
			}

			GuiScreenConfig gui = new GuiScreenConfig(
				tes,
				data.side,
				data.friends,
				data.friendRights,
				data.otherRights,
				data.owner
			);
			mc.setScreen(gui);
		});
	}

	@Override
	public void trackScreen(ScreenBlockEntity tes, boolean track) {
		int idx = -1;
		for (int i = 0; i < screenTracking.size(); i++) {
			if (screenTracking.get(i) == tes) {
				idx = i;
				break;
			}
		}

		if (track) {
			if (idx < 0)
				screenTracking.add(tes);
		} else if (idx >= 0)
			screenTracking.remove(idx);
	}

	@Override
	public void trackBrowser(CefBrowser browser, boolean track, ScreenBlockEntity screen, BlockSide side) {
		if (browser != null) {
			if (track) {
				allBrowsers.add(browser);
				if (screen != null) {
					browserToScreen.put(browser, screen);
					if (side != null) {
						browserToSide.put(browser, side);
					}
				}
				Log.info("[WebDisplays] Browser registered for tracking, total: %d", allBrowsers.size());
			} else {
				allBrowsers.remove(browser);
				browserToScreen.remove(browser);
				browserToSide.remove(browser);
			}
		}
	}

	@Override
	public void setBrowserTargetUrl(CefBrowser browser, String url) {
		if (browser != null && url != null) {
			((DisplayHandler) DisplayHandler.INSTANCE).setTargetUrl(browser, url);
		}
	}

	@Override
	public void forceManualBrowserSave(CefBrowser browser) {
		if (browser != null) {
			((DisplayHandler) DisplayHandler.INSTANCE).forceManualSave(browser);
		}
	}

	@Override
	public void syncUrlToServer(ScreenBlockEntity screen, BlockSide side, String url) {
		if (screen != null && side != null && url != null && !url.isEmpty()) {
			WebDisplays.LOGGER.debug("ClientProxy.syncUrlToServer: {} -> {}", side, url);

			if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
				try {
					var serverLevel = mc.getSingleplayerServer().getLevel(screen.getLevel().dimension());
					if (serverLevel != null) {
						var be = serverLevel.getBlockEntity(screen.getBlockPos());
						if (be instanceof net.montoyo.wd.entity.ScreenBlockEntity serverTE) {
							serverTE.setScreenURL(side, url);
							WebDisplays.LOGGER.debug("DIRECT SYNC SUCCESS (single-player): {} -> {}", side, url);
							return;
						}
					}
				} catch (Exception e) {
					WebDisplays.LOGGER.warn("Direct sync failed, falling back to packet: {}", e.getMessage());
				}
			}

			WebDisplays.LOGGER.debug("Sending packet sync: {} -> {}", side, url);
			net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
				new net.montoyo.wd.net.payload.UrlUpdatePayload(screen.getBlockPos(), side, url)
			);
		}
	}

	@Override
	public void onAutocompleteResult(NameUUIDPair[] pairs) {
		if (mc.screen != null && mc.screen instanceof WDScreen screen) {
			if (pairs.length == 0)
				(screen).onAutocompleteFailure();
			else
				(screen).onAutocompleteResult(pairs);
		}
	}

	@Override
	public GameProfile[] getOnlineGameProfiles() {
		return new GameProfile[]{mc.player.getGameProfile()};
	}

	@Override
	public Player getLocalPlayer() {
		return mc.player;
	}

	@Override
	public void screenUpdateResolutionInGui(Vector3i pos, BlockSide side, Vector2i res) {
		if (mc.screen != null && mc.screen instanceof GuiScreenConfig gsc) {
			if (gsc.isForBlock(pos.toBlock(), side))
				gsc.updateResolution(res);
		}
	}

	@Override
	public void screenUpdateRotationInGui(Vector3i pos, BlockSide side, Rotation rot) {
		if (mc.screen != null && mc.screen instanceof GuiScreenConfig gsc) {
			if (gsc.isForBlock(pos.toBlock(), side))
				gsc.updateRotation(rot);
		}
	}

	@Override
	public void screenUpdateAutoVolumeInGui(Vector3i pos, BlockSide side, boolean av) {
		if (mc.screen != null && mc.screen instanceof GuiScreenConfig gsc) {
			if (gsc.isForBlock(pos.toBlock(), side))
				gsc.updateAutoVolume(av);
		}
	}

	@Override
	public void openScreenConfigGui(Vector3i pos, BlockSide side, ScreenData screen) {
		Level level = mc.level;
		if (level == null) return;

		BlockEntity be = level.getBlockEntity(pos.toBlock());
		if (!(be instanceof ScreenBlockEntity tes)) return;

		NameUUIDPair[] friendsArray = screen.friends != null
			? screen.friends.toArray(new NameUUIDPair[0])
			: new NameUUIDPair[0];

		mc.execute(() -> {
			GuiScreenConfig gui = new GuiScreenConfig(
				tes,
				side,
				friendsArray,
				screen.friendRights,
				screen.otherRights,
				screen.owner
			);
			mc.setScreen(gui);
		});
	}

	@Override
	public void displaySetPadURLGui(ItemStack is, String padURL) {
		mc.setScreen(new GuiSetURL2(is, padURL));
	}

	@Override
	public void openMinePadGui(UUID padId) {
		PadData pd = padMap.get(padId);
		if (pd == null) {
			String url = CommonConfig.Browser.homepage;
			Player player = mc.player;
			if (player != null) {
				for (InteractionHand h : InteractionHand.values()) {
					ItemStack held = h == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem();
					if (held.getItem() instanceof ItemMinePad2) {
						var cd = held.get(DataComponents.CUSTOM_DATA);
						if (cd != null && cd.copyTag().contains("PadURL"))
							url = cd.copyTag().getString("PadURL");
						break;
					}
				}
			}
			String cached = getCachedMinepadUrl(padId);
			if (cached != null && !cached.isEmpty() && !cached.contains("about:blank"))
				url = cached;
			pd = new PadData(url, padId);
			padList.add(pd);
			padMap.put(padId, pd);
		}
		final GuiMinePad gui = new GuiMinePad(pd);
		Minecraft.getInstance().execute(() ->
			Minecraft.getInstance().setScreen(gui)
		);
	}

	@Override
	@NotNull
	public HasAdvancement hasClientPlayerAdvancement(@NotNull ResourceLocation rl) {
		if (advancementToProgressField != null && mc.player != null && mc.player.connection != null) {
			ClientAdvancements cam = mc.player.connection.getAdvancements();
			var advHolder = cam.get(rl);
			if (advHolder == null)
				return HasAdvancement.DONT_KNOW;
			Advancement adv = advHolder.value();

			if (lastAdvMgr != cam) {
				lastAdvMgr = cam;

                try {
					@SuppressWarnings("unchecked")
					Map<Advancement, AdvancementProgress> progress = (Map<Advancement, AdvancementProgress>) advancementToProgressField.get(cam);
					advancementToProgress = progress;
				} catch (Throwable t) {
					Log.warningEx("Could not get ClientAdvancementManager.advancementToProgress field", t);
					advancementToProgress = null;
					return HasAdvancement.DONT_KNOW;
				}
			}

			if (advancementToProgress == null)
				return HasAdvancement.DONT_KNOW;

			AdvancementProgress progress = advancementToProgress.get(adv);
			if (progress == null)
				return HasAdvancement.NO;

			return progress.isDone() ? HasAdvancement.YES : HasAdvancement.NO;
		}

		return HasAdvancement.DONT_KNOW;
	}

	@Override
	public MinecraftServer getServer() {
		return mc.getSingleplayerServer();
	}

	@Override
	public void setMiniservClientPort(int port) {
		miniservPort = port;
	}

	@Override
	public void startMiniservClient() {
		if (miniservPort <= 0) {
			Log.warning("Can't start miniserv client: miniserv is disabled");
			return;
		}

		if (mc.player == null) {
			Log.warning("Can't start miniserv client: player is null");
			return;
		}

		SocketAddress saddr;
		try {
			var connection = mc.getConnection();
			if (connection != null) {
				saddr = connection.getConnection().getRemoteAddress();
				if (saddr == null || !(saddr instanceof InetSocketAddress)) {
					Log.warning("Miniserv client: remote address is not inet, using localhost");
					saddr = new InetSocketAddress("127.0.0.1", miniservPort);
				}
			} else {
				saddr = new InetSocketAddress("127.0.0.1", miniservPort);
			}
		} catch (Exception e) {
			Log.warning("Could not get server address, falling back to localhost: %s", e.getMessage());
			saddr = new InetSocketAddress("127.0.0.1", miniservPort);
		}

		InetSocketAddress msAddr = new InetSocketAddress(((InetSocketAddress) saddr).getAddress(), miniservPort);
		Client.getInstance().start(msAddr);
	}

	@Override
	public boolean isMiniservDisabled() {
		return miniservPort <= 0;
	}

	@Override
	public boolean decryptKey(byte[] encryptedKey) {
		return Client.getInstance().decryptKey(encryptedKey);
	}

	@Override
	public net.montoyo.wd.net.server_bound.C2SMessageMiniservConnect beginMiniservConnection() {
		return Client.getInstance().beginConnection();
	}

	@Override
	public void closeGui(BlockPos bp, BlockSide bs) {
		if (mc.screen instanceof WDScreen) {
			WDScreen scr = (WDScreen) mc.screen;

			if (scr.isForBlock(bp, bs))
				mc.setScreen(null);
		}
	}

	@Override
	public void renderRecipes() {
		Minecraft.getInstance().setScreen(new RenderRecipe());
	}

	@Override
	public boolean isShiftDown() {
		return Screen.hasShiftDown();
	}

	/**************************************** RESOURCE MANAGER METHODS ****************************************/

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {
		Log.info("Resource manager reload: clearing GUI cache...");
		GuiLoader.clearCache();
	}

	private double distanceTo(ScreenBlockEntity tes, net.minecraft.world.phys.Vec3 pos) {
		double dx = tes.getBlockPos().getX() + 0.5 - pos.x;
		double dy = tes.getBlockPos().getY() + 0.5 - pos.y;
		double dz = tes.getBlockPos().getZ() + 0.5 - pos.z;
		return dx*dx + dy*dy + dz*dz;
	}

	private int urlHeartbeatCounter = 0;
	private Map<BlockSide, String> lastSyncedUrls = new HashMap<>();

	@Override
	public void scheduleNativeTouchUp(ScreenBlockEntity screen, BlockSide side) {
		if (pendingMouseUp != null) {
			ScreenData scr = pendingMouseUp.screen.getScreen(pendingMouseUp.side);
			if (scr != null) {
				pendingMouseUp.screen.handleMouseEvent(
					pendingMouseUp.side,
					net.montoyo.wd.controls.builtin.ClickControl.ControlType.UP,
					pendingMouseUp.pos, 1
				);
			}
		}
		ScreenData scr = screen.getScreen(side);
		if (scr != null) {
			pendingMouseUp = new PendingMouseUp(screen, side, scr.lastMousePos);
		}
	}

	public void tickScreenTracking() {
		pollPointerLocks();

		if (pendingMouseUp != null) {
			ScreenData scr = pendingMouseUp.screen.getScreen(pendingMouseUp.side);
			if (scr != null) {
				pendingMouseUp.screen.handleMouseEvent(
					pendingMouseUp.side,
					net.montoyo.wd.controls.builtin.ClickControl.ControlType.UP,
					pendingMouseUp.pos, 1
				);
			}
			pendingMouseUp = null;
		}

		if (mc.player == null || screenTracking.isEmpty())
			return;

		if (++urlHeartbeatCounter >= 200) {
			urlHeartbeatCounter = 0;
			syncUrlsToServer();
		}

		int id = lastTracked % screenTracking.size();
		lastTracked++;

		ScreenBlockEntity tes = screenTracking.get(id);
		if (tes.getLevel() != mc.level)
			return;

		if (tes.getLevel() != mc.player.level()) {
			if (tes.isLoaded())
				tes.unload();
		} else {
			double dist = distanceTo(tes, mc.player.getPosition(0));

			if (tes.isLoaded()) {
				if (dist > WebDisplays.unloadDistance2) {
					tes.unload();
				}
			} else {
				if (dist <= WebDisplays.loadDistance2) {
					tes.load();
				}
			}
		}
	}

	private void syncUrlsToServer() {
		for (ScreenBlockEntity tes : screenTracking) {
			if (tes.getLevel() != mc.player.level()) continue;

			for (int i = 0; i < tes.screenCount(); i++) {
				ScreenData scr = tes.getScreen(i);
				if (scr.browser == null) continue;

				String currentUrl = scr.browser.getURL();
				if (currentUrl == null) continue;

				String lastSynced = lastSyncedUrls.get(scr.side);
				if (!currentUrl.equals(lastSynced)) {
					WebDisplays.LOGGER.debug("Heartbeat URL sync: side={} url={}", scr.side, currentUrl);
					net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
						new net.montoyo.wd.net.payload.UrlUpdatePayload(tes.getBlockPos(), scr.side, currentUrl)
					);
					lastSyncedUrls.put(scr.side, currentUrl);
				}
			}
		}
	}

	private boolean wasPointerLocked = false;
	private int pointerLockTick = 0;

	private void pollPointerLocks() {
		if (++pointerLockTick % 5 != 0) return;
		if (mc.screen != null && !(mc.screen instanceof net.montoyo.wd.client.gui.GuiKeyboard)) return;

		boolean hasLocked = false;
		synchronized (allBrowsers) {
			for (CefBrowser browser : allBrowsers) {
				if (browser instanceof net.montoyo.wd.utilities.browser.WDBrowser wdBrowser) {
					try {
						JsonObject obj = wdBrowser.pointerLockElement().getObj();
                        if (obj != null && obj.getAsJsonPrimitive("exists").getAsBoolean()) {
                            hasLocked = true;
                            RenderSystem.recordRenderCall(() -> {
								GLFW.glfwSetInputMode(mc.getWindow().getWindow(), 208897, GLFW.GLFW_CURSOR_DISABLED);
							});
						}
					} catch (Exception e) {
						Log.warning("pollPointerLocks: Exception while polling: %s", e.getMessage());
					}
				}
			}
		}
		if (!hasLocked && wasPointerLocked) {
			RenderSystem.recordRenderCall(() -> {
				GLFW.glfwSetInputMode(mc.getWindow().getWindow(), 208897, GLFW.GLFW_CURSOR_NORMAL);
			});
		}
		wasPointerLocked = hasLocked;
	}

	public void closeAllBrowsers() {
		WebDisplays.LOGGER.debug("ClientProxy: closeAllBrowsers called - screens tracked: {}, allBrowsers: {}", screenTracking.size(), allBrowsers.size());

		try {
			net.montoyo.wd.utilities.browser.handlers.DisplayHandler dh =
				(net.montoyo.wd.utilities.browser.handlers.DisplayHandler) net.montoyo.wd.utilities.browser.handlers.DisplayHandler.INSTANCE;
			dh.emergencyFlushAll();
		} catch (Exception e) {
			Log.warning("[WebDisplays] Emergency flush failed: %s", e.getMessage());
		}

		for (ScreenBlockEntity tes : screenTracking) {
			WebDisplays.LOGGER.debug("Closing browsers for screen at {}", tes.getBlockPos());
			for (int i = 0; i < tes.screenCount(); i++) {
				ScreenData scr = tes.getScreen(i);
				if (scr.browser != null) {
					WebDisplays.LOGGER.debug("  Closing browser for side {}", scr.side);
					scr.browser.close(true);
					scr.browser = null;
				}
			}
		}
		screenTracking.clear();

		if (!allBrowsers.isEmpty()) {
			WebDisplays.LOGGER.debug("Force closing {} browsers from allBrowsers set", allBrowsers.size());
			for (CefBrowser browser : new ArrayList<>(allBrowsers)) {
				try {
					browser.close(true);
				} catch (Exception e) {
					Log.warning("[WebDisplays] Error closing browser: %s", e.getMessage());
				}
			}
			allBrowsers.clear();
		}

		if (!padList.isEmpty()) {
			WebDisplays.LOGGER.debug("Closing {} MinePad browsers", padList.size());
			for (PadData pd : new ArrayList<>(padList)) {
				if (pd.view != null) {
					try {
						String currentUrl = pd.view.getURL();
						if (currentUrl != null && !currentUrl.isEmpty()) {
							cacheMinepadUrl(pd.id, currentUrl);
							if (mc.player != null) {
								for (InteractionHand h : InteractionHand.values()) {
									ItemStack held = h == InteractionHand.MAIN_HAND ? mc.player.getMainHandItem() : mc.player.getOffhandItem();
									if (held.getItem() instanceof ItemMinePad2) {
										var cd = held.get(DataComponents.CUSTOM_DATA);
										if (cd != null && cd.copyTag().hasUUID("PadID")) {
											UUID itemPadId = cd.copyTag().getUUID("PadID");
											if (itemPadId.equals(pd.id)) {
												CompoundTag tag = cd.copyTag();
												tag.putString("PadURL", currentUrl);
												held.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
												break;
											}
										}
									}
								}
							}
						}
					} catch (Exception e) {
						Log.warning("[WebDisplays] Error saving MinePad URL: %s", e.getMessage());
					}

					pd.view.close(true);
					pd.view = null;
				}
			}
			padList.clear();
			padMap.clear();
			flushMinepadCacheToDisk();
			WebDisplays.LOGGER.debug("MinePad browsers closed, lists cleared, cache flushed");
		}

		System.gc();

		WebDisplays.LOGGER.debug("All browsers closed and tracking cleared");
	}

	public void applyCachedUrls() {
		try {
			((DisplayHandler) DisplayHandler.INSTANCE).getUrlCache().applyCachedUrls();
		} catch (Exception e) {
			WebDisplays.LOGGER.error("Failed to apply cached URLs: {}", e.getMessage());
		}
	}

	@Override
	public String getCachedUrl(net.minecraft.core.BlockPos pos, BlockSide side) {
		try {
			return ((DisplayHandler) DisplayHandler.INSTANCE).getUrlCache().getUrlForPos(pos, side);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public void deleteCachedUrl(net.minecraft.core.BlockPos pos, BlockSide side) {
		try {
			((DisplayHandler) DisplayHandler.INSTANCE).getUrlCache().deleteUrl(pos, side);
		} catch (Exception e) {
			WebDisplays.LOGGER.warn("Failed to delete cached URL: {}", e.getMessage());
		}
	}

	public void clearUrlCache() {
		try {
			UrlCache cache = ((DisplayHandler) DisplayHandler.INSTANCE).getUrlCache();
			cache.flushCache();
			cache.clearCache();
			WebDisplays.LOGGER.debug("URL cache flushed and cleared");
		} catch (Exception e) {
			WebDisplays.LOGGER.warn("Failed to clear URL cache: {}", e.getMessage());
		}
	}

	public void flushUrlCache() {
		try {
			UrlCache cache = ((DisplayHandler) DisplayHandler.INSTANCE).getUrlCache();
			cache.flushCache();
			WebDisplays.LOGGER.debug("URL cache flushed (kept for rejoin)");
		} catch (Exception e) {
			WebDisplays.LOGGER.warn("Failed to flush URL cache: {}", e.getMessage());
		}
	}

	public static BlockHitResult raycast(double dist) {
		Minecraft mc = Minecraft.getInstance();

		Vec3 start = mc.player.getEyePosition(1.0f);
		Vec3 lookVec = mc.player.getLookAngle();
		Vec3 end = start.add(lookVec.x * dist, lookVec.y * dist, lookVec.z * dist);

		HitResult hit = mc.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, mc.player));
		if (hit instanceof BlockHitResult bhr) return bhr;
		return BlockHitResult.miss(start, Direction.UP, BlockPos.containing(start));
	}

	public void updateInventory() {
		if (mc.player == null) return;

		if (++minePadTickCounter >= 10) {
			minePadTickCounter = 0;

			for (PadData pd : padList)
				pd.isInHotbar = false;

			scanPadsNow();

			Iterator<PadData> it = padList.iterator();
			while (it.hasNext()) {
				PadData pd = it.next();
				if (!pd.isInHotbar) {
					if (pd.view != null) {
						pd.view.close();
					}
					padMap.remove(pd.id);
					it.remove();
				}
			}
		}
	}

	public void scanPadsNow() {
		if (mc.player == null) return;
		scanPadsForHand(mc.player.getInventory().items, mc.player.getMainHandItem());
		scanPadsForHand(mc.player.getInventory().offhand, mc.player.getOffhandItem());
	}

	private void scanPadsForHand(NonNullList<ItemStack> inv, ItemStack heldStack) {
		for (int i = 0; i < inv.size(); i++) {
			ItemStack item = inv.get(i);
			if (ItemRegistry.MINEPAD != null && item.getItem() == ItemRegistry.MINEPAD) {
				var cd = item.get(DataComponents.CUSTOM_DATA);
				if (cd != null && cd.copyTag().contains("PadID")) {
					UUID padId = cd.copyTag().getUUID("PadID");
					updatePad(padId, cd.copyTag(), item == heldStack);
				}
			}
		}
	}

	private void updatePad(UUID id, CompoundTag tag, boolean isSelected) {
		boolean hasURL = tag.contains("PadURL");

		if (padMap.containsKey(id)) {
			PadData pd = padMap.get(id);
			pd.isInHotbar = true;
		} else if (isSelected && hasURL) {
			String url = tag.getString("PadURL");
			String cached = getCachedMinepadUrl(id);
			if (cached != null && !cached.isEmpty() && !cached.contains("about:blank"))
				url = cached;
			PadData pd = new PadData(url, id);
			padList.add(pd);
			padMap.put(id, pd);
		}
	}

	public MinePadRenderer getMinePadRenderer() {
		return minePadRenderer;
	}

	public LaserPointerRenderer getLaserPointerRenderer() {
		return laserPointerRenderer;
	}

	public PadData getPadByID(UUID id) {
		return padMap.get(id);
	}

	@Override
	public CefBrowser createBrowser(String url, boolean transparent) {
		WDClientBrowser browser = new WDClientBrowser(MCEF.getClient(), url, transparent);
		browser.setCloseAllowed();
		browser.createImmediately();
		WDBrowserHelper.registerQueries(browser);
		return browser;
	}

	@Override
	public void createBrowserDataDir(BlockPos pos) {
		WebDisplaysDirs.getSubDir("browsers/srceen/" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ());
	}

	@Override
	public void handleAddScreen(Vector3i pos, boolean clear, ScreenData[] screens) {
		Level lvl = Minecraft.getInstance().level;
		if (lvl == null) return;

		BlockEntity te = lvl.getBlockEntity(pos.toBlock());
		if (!(te instanceof ScreenBlockEntity)) {
			BlockState bs = lvl.getBlockState(pos.toBlock());
			if (bs.is(BlockRegistry.SCREEN_BLOCK)) {
				lvl.setBlock(pos.toBlock(), bs.setValue(ScreenBlock.hasTE, true), 3);
			}
			te = lvl.getBlockEntity(pos.toBlock());

			if (!(te instanceof ScreenBlockEntity)) {
				Log.warning("CMessageAddScreen: Block entity not ready at %s (chunk not yet loaded?), will retry", pos.toString());
				Minecraft.getInstance().execute(() -> handleAddScreen(pos, clear, screens));
				return;
			}
		}

		ScreenBlockEntity tes = (ScreenBlockEntity) te;
		if (clear)
			tes.clear();

		for (ScreenData entry : screens) {
			ScreenData scr = tes.addScreen(entry.side, entry.size, entry.resolution, null, false);
			scr.rotation = entry.rotation;
			String webUrl;

			try {
				webUrl = ScreenBlockEntity.url(lvl, entry.url);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			scr.url = webUrl;
			scr.owner = entry.owner;
			scr.upgrades = entry.upgrades;
			scr.createBrowser(tes, true, lvl);
		}

		tes.markLoaded();
	}

	@Override
	public void handleScreenUpdate(Vector3i pos, BlockSide side, boolean clear, ScreenData[] screens) {
		Level level = Minecraft.getInstance().level;
		if (level == null) return;

		BlockEntity te = level.getBlockEntity(pos.toBlock());
		if (!(te instanceof ScreenBlockEntity tes)) {
			Log.error("CMessageScreenUpdate: No screen block entity at %s", pos.toString());
			return;
		}

		if (clear) {
			tes.removeScreen(side);
		}

		for (ScreenData entry : screens) {
			ScreenData scr = tes.addScreen(entry.side, entry.size, entry.resolution, null, false);
			scr.rotation = entry.rotation;

			try {
				String webUrl = ScreenBlockEntity.url(level, entry.url);
				scr.url = webUrl;
				scr.owner = entry.owner;
				scr.upgrades = entry.upgrades;

				if (scr.browser != null) {
					scr.browser.loadURL(webUrl);
				}
			} catch (IOException e) {
				Log.error("Failed to process URL for screen update: %s", entry.url);
			}
		}
	}

	@Override
	public void handleScreenControl(BlockPos pos, BlockSide side, Object control) {
		Level level = Minecraft.getInstance().level;
		if (level == null) return;

		BlockEntity be = level.getBlockEntity(pos);
		if (!(be instanceof ScreenBlockEntity tes)) {
			Log.error("handleScreenControl: No screen block entity at %s", pos.toString());
			return;
		}

		if (control instanceof net.montoyo.wd.controls.ScreenControl sc) {
			sc.handleClient(pos, side, tes, null);
		}

		if (side != null) {
			ScreenData scr = tes.getScreen(side);
			if (scr == null) {
				Log.warning("handleScreenControl: No screen on side %s", side);
			}
		}
	}

	public static final class ScreenSidePair {

		public ScreenBlockEntity tes;
		public BlockSide side;

	}

	public boolean findScreenFromBrowser(MCEFBrowser browser, ScreenSidePair pair) {
		for (ScreenBlockEntity tes : screenTracking) {
			for (int i = 0; i < tes.screenCount(); i++) {
				ScreenData scr = tes.getScreen(i);

				if (scr.browser == browser) {
					pair.tes = tes;
					pair.side = scr.side;
					return true;
				}
			}
		}

		return false;
	}

	private static Field findAdvancementToProgressField() {
		Field[] fields = ClientAdvancements.class.getDeclaredFields();
		Optional<Field> result = Arrays.stream(fields).filter(f -> f.getType() == Map.class).findAny();

		if (result.isPresent()) {
			try {
				Field ret = result.get();
				ret.setAccessible(true);
				return ret;
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

		Log.warning("ClientAdvancementManager.advancementToProgress field could not be found");
		return null;
	}

	public static final KeyMapping KEY_MOUSE = new KeyMapping("webdisplays.key.toggle_mouse", GLFW.GLFW_KEY_R, "key.categories.misc");
	public static final KeyMapping KEY_MINEPAD = new KeyMapping("webdisplays.key.open_minepad", GLFW.GLFW_KEY_F10, "key.categories.misc");
	static boolean rDown = false;
	public static boolean mouseOn = false;
}
