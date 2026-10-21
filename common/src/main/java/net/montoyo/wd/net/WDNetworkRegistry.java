package net.montoyo.wd.net;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.montoyo.wd.net.client_bound.*;
import net.montoyo.wd.net.payload.UrlUpdatePayload;
import net.montoyo.wd.net.server_bound.*;
import net.montoyo.wd.utilities.Log;

public class WDNetworkRegistry {
	public static final String networkingVersion = "2";
	public static final ResourceLocation CHANNEL_ID = ResourceLocation.fromNamespaceAndPath("webdisplays", "packetsystem");
	public static final SimpleChannel INSTANCE = new SimpleChannel(CHANNEL_ID.toString());
	
	private static MinecraftServer serverInstance;
	
	// Client-side sender callback - set by WebDisplaysClient to avoid classloading issues
	private static java.util.function.Consumer<WDPacketPayload> clientSender;
	
	public static void setClientSender(java.util.function.Consumer<WDPacketPayload> sender) {
		clientSender = sender;
	}
	
	public static void sendToServer(Packet packet) {
		// Client-side: send packet to server
		FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
		packet.write(buf);
		WDPacketPayload payload = WDPacketPayload.create(packet.getId(), buf.array());
		// Use client sender callback to avoid classloading ClientPlayNetworking on server
		if (clientSender != null) {
			clientSender.accept(payload);
		}
	}
	
	private static RegistryFriendlyByteBuf createFriendlyBuf(net.minecraft.core.RegistryAccess registryAccess) {
		return new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
	}

	public static void sendToPlayer(ServerPlayer player, Packet packet) {
		RegistryAccess registryAccess = player.level().registryAccess();
		RegistryFriendlyByteBuf buf = createFriendlyBuf(registryAccess);
		packet.write(buf);
		WDPacketPayload payload = WDPacketPayload.create(packet.getId(), buf.array());
		ServerPlayNetworking.send(player, payload);
	}
	
	public static void sendToAllPlayers(Packet packet) {
		if (serverInstance == null) return;
		RegistryAccess registryAccess = serverInstance.registryAccess();
		RegistryFriendlyByteBuf buf = createFriendlyBuf(registryAccess);
		packet.write(buf);
		WDPacketPayload payload = WDPacketPayload.create(packet.getId(), buf.array());
		for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(player, payload);
		}
	}
	
	public static void sendToNearExcept(ServerPlayer exceptPlayer, Packet packet) {
		if (serverInstance == null) return;
		RegistryAccess registryAccess = serverInstance.registryAccess();
		RegistryFriendlyByteBuf buf = createFriendlyBuf(registryAccess);
		packet.write(buf);
		WDPacketPayload payload = WDPacketPayload.create(packet.getId(), buf.array());
		for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
			if (player != exceptPlayer) {
				ServerPlayNetworking.send(player, payload);
			}
		}
	}
	
	public static void sendToNear(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, Packet packet) {
		if (serverInstance == null) return;
		net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim = level.dimension();
		RegistryAccess registryAccess = serverInstance.registryAccess();
		RegistryFriendlyByteBuf buf = createFriendlyBuf(registryAccess);
		packet.write(buf);
		WDPacketPayload payload = WDPacketPayload.create(packet.getId(), buf.array());
		// Send to all players near this block position (same dimension, within 64 blocks)
		for (ServerPlayer player : serverInstance.getPlayerList().getPlayers()) {
			if (player.level().dimension() == dim && pos.distSqr(player.blockPosition()) < 4096.0) {
				ServerPlayNetworking.send(player, payload);
			}
		}
	}
	
	public static MinecraftServer getServer() {
		return serverInstance;
	}
	
	public static void setServer(MinecraftServer server) {
		serverInstance = server;
	}
	
	static {
		// Register packets
		PacketFactory.register(ResourceLocation.fromNamespaceAndPath("webdisplays", "serverinfo"), S2CMessageServerInfo::new);
		PacketFactory.register(ResourceLocation.fromNamespaceAndPath("webdisplays", "miniservkey"), S2CMessageMiniservKey::new);
		PacketFactory.register(ResourceLocation.fromNamespaceAndPath("webdisplays", "closegui"), S2CMessageCloseGui::new);
		PacketFactory.register(ResourceLocation.fromNamespaceAndPath("webdisplays", "opengui"), S2CMessageOpenGui::new);
		PacketFactory.register(ResourceLocation.fromNamespaceAndPath("webdisplays", "addscreen"), S2CMessageAddScreen::new);
		PacketFactory.register(ResourceLocation.fromNamespaceAndPath("webdisplays", "screenupdate"), S2CMessageScreenUpdate::new);
		PacketFactory.register(ResourceLocation.fromNamespaceAndPath("webdisplays", "acresult"), S2CMessageACResult::new);
		PacketFactory.register(ResourceLocation.fromNamespaceAndPath("webdisplays", "jsresponse"), S2CMessageJSResponse::new);
		PacketFactory.register(ResourceLocation.fromNamespaceAndPath("webdisplays", "miniservconnect"), C2SMessageMiniservConnect::new);
		PacketFactory.register(ResourceLocation.fromNamespaceAndPath("webdisplays", "screenctrl"), C2SMessageScreenCtrl::new);
		PacketFactory.register(ResourceLocation.fromNamespaceAndPath("webdisplays", "redstonectrl"), C2SMessageRedstoneCtrl::new);
		PacketFactory.register(ResourceLocation.fromNamespaceAndPath("webdisplays", "acquery"), C2SMessageACQuery::new);
		PacketFactory.register(ResourceLocation.fromNamespaceAndPath("webdisplays", "minepadurl"), C2SMessageMinepadUrl::new);
		PacketFactory.register(ResourceLocation.fromNamespaceAndPath("webdisplays", "redstoneoutput"), C2SMessageRedstoneOutput::new);
	}
	
	public static void init() {
		// Register payload types first (required before registering receivers in Fabric 1.21)
		PayloadTypeRegistry.playC2S().register(WDPacketPayload.TYPE, WDPacketPayload.STREAM_CODEC);
		PayloadTypeRegistry.playS2C().register(WDPacketPayload.TYPE, WDPacketPayload.STREAM_CODEC);
		
		// Register URL Update Payload (Fabric 1.21 native approach)
		PayloadTypeRegistry.playC2S().register(UrlUpdatePayload.TYPE, UrlUpdatePayload.STREAM_CODEC);
		ServerPlayNetworking.registerGlobalReceiver(UrlUpdatePayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				Log.debug("WDNetworkRegistry: Received UrlUpdatePayload: pos=%s side=%s url='%s'", payload.pos(), payload.side(), payload.url());
				
				// Try to get block entity - during disconnect, use server level directly
				net.montoyo.wd.entity.ScreenBlockEntity be = null;
				
				// First try player's level (normal case)
				if (context.player() != null && context.player().level() != null) {
					if (context.player().level().getBlockEntity(payload.pos()) instanceof net.montoyo.wd.entity.ScreenBlockEntity screenBE) {
						be = screenBE;
					}
				}
				
				// If that fails (e.g., during disconnect), try all server levels
				if (be == null) {
					MinecraftServer server = context.server();
					for (net.minecraft.server.level.ServerLevel level : server.getAllLevels()) {
						if (level.getBlockEntity(payload.pos()) instanceof net.montoyo.wd.entity.ScreenBlockEntity screenBE) {
							be = screenBE;
                            Log.debug("WDNetworkRegistry: Found ScreenBlockEntity in level %s", level.dimension().location());
							break;
						}
					}
				}
				
			if (be != null) {
				try {
					be.setScreenURL(payload.side(), payload.url());
					
					// Get the level from the block entity
					var level = be.getLevel();
					if (level != null) {
						// Mark BE as dirty so NBT save picks up the new URL immediately
						be.setChanged();
						// Force the chunk to resave now (not later) - critical for 1.21 chunk system
						level.blockEntityChanged(payload.pos());
						// Sync client-side visual update
						level.sendBlockUpdated(payload.pos(), be.getBlockState(), be.getBlockState(), 3);
					}

					// Save to server-side disk cache as immediate secondary defense
					// This survives even if the chunk NBT save races with world close
					try {
						net.montoyo.wd.WebDisplaysMod mod = net.montoyo.wd.WebDisplaysMod.INSTANCE;
						if (mod != null && mod.getServerUrlCache() != null) {
							net.montoyo.wd.utilities.data.BlockSide side =
								net.montoyo.wd.utilities.data.BlockSide.values()[payload.side().ordinal()];
							mod.getServerUrlCache().saveUrl(payload.pos(), side, payload.url());
						}
					} catch (Exception e) {
						Log.warning("WDNetworkRegistry: ServerUrlCache save failed: %s", e.getMessage());
					}
					
					Log.debug("WDNetworkRegistry: URL updated at %s (forced save + cache)", payload.pos());
					} catch (Exception e) {
						Log.error("WDNetworkRegistry: Error updating URL: %s", e.getMessage());
						e.printStackTrace();
					}
				} else {
					Log.error("WDNetworkRegistry: No ScreenBlockEntity found at %s in any level", payload.pos());
				}
			});
		});
		
		// Register server-side packet handler
		ServerPlayNetworking.registerGlobalReceiver(WDPacketPayload.TYPE, (payload, context) -> {
			context.server().execute(() -> {
				RegistryAccess registryAccess = context.player().level().registryAccess();
				RegistryFriendlyByteBuf buf = payload.getData(registryAccess);
				Packet packet = PacketFactory.create(payload.id(), buf);
				if (packet != null) {
					packet.handle(context.player());
				}
			});
		});
	}
}
