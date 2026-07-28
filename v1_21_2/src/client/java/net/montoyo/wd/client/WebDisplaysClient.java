package net.montoyo.wd.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.WebDisplaysMod;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.client.renderers.ScreenRenderer;
import net.montoyo.wd.registry.ItemRegistry;
import net.montoyo.wd.registry.TileRegistry;
import net.montoyo.wd.net.WDPacketPayload;
import net.montoyo.wd.net.PacketFactory;
import net.montoyo.wd.net.Packet;


public class WebDisplaysClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        
        ClientProxy proxy = new ClientProxy();
        proxy.preInit();
        proxy.init();
        
        WebDisplaysMod.PROXY = proxy;
        WebDisplays.PROXY = proxy;
        
        KeyBindingHelper.registerKeyBinding(ClientProxy.KEY_MOUSE);
        
        BlockEntityRenderers.register(TileRegistry.SCREEN_BLOCK_ENTITY, context -> new ScreenRenderer());

        WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((context, hitResult) -> {
            if (hitResult instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
                net.minecraft.world.level.block.state.BlockState state = context.world().getBlockState(blockHit.getBlockPos());
                if (state.getBlock() instanceof net.montoyo.wd.block.ScreenBlock) {
                    var pos = new net.montoyo.wd.utilities.math.Vector3i(blockHit.getBlockPos());
                    var side = net.montoyo.wd.utilities.data.BlockSide.values()[blockHit.getDirection().ordinal()];
                    net.montoyo.wd.utilities.Multiblock.findOrigin(context.world(), pos, side, null);
                    var te = context.world().getBlockEntity(pos.toBlock());
                    if (te instanceof net.montoyo.wd.entity.ScreenBlockEntity te2) {
                        for (int i = 0; i < te2.screenCount(); i++) {
                            if (te2.getScreen(i).browser != null) return false;
                        }
                    }
                }
            }
            return true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (WebDisplays.PROXY instanceof ClientProxy cp) {
                cp.updateInventory();
                cp.tickScreenTracking();
                
                if (ClientProxy.KEY_MOUSE.isDown()) {
                    if (!ClientProxy.rDown) {
                        ClientProxy.rDown = true;
                        ClientProxy.mouseOn = !ClientProxy.mouseOn;
                    }
                } else {
                    ClientProxy.rDown = false;
                }
                if (client.player == null || client.player.getMainHandItem().getItem() != ItemRegistry.LASER_POINTER) {
                    ClientProxy.mouseOn = false;
                }

                if (net.montoyo.wd.client.renderers.LaserPointerRenderer.isOn()) {
                    net.montoyo.wd.item.ItemLaserPointerClient.tick(client);
                } else {
                    net.montoyo.wd.item.ItemLaserPointerClient.deselect(client);
                }
            }
            
            net.montoyo.wd.client.gui.camera.KeyboardCamera.gameTick();
        });
        
        
        
        ClientPlayNetworking.registerGlobalReceiver(WDPacketPayload.TYPE, (payload, context) -> {
            WebDisplays.LOGGER.debug("Client received packet: id={}", payload.id());
            context.client().execute(() -> {
                var buf = payload.getData(context.client().level.registryAccess());
                Packet packet = PacketFactory.create(payload.id(), buf);
                WebDisplays.LOGGER.debug("Client packet created: {}", packet != null ? packet.getClass().getSimpleName() : "null");
                if (packet != null) {
                    var clientPlayer = Minecraft.getInstance().player;
                    packet.handle(clientPlayer);
                }
            });
        });
        
        WDNetworkRegistry.setClientSender(payload -> {
            ClientPlayNetworking.send(payload);
        });
        
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            WebDisplays.LOGGER.debug("DISCONNECT event - closing all browsers");
            if (WebDisplays.PROXY instanceof ClientProxy cp) {
                cp.closeAllBrowsers();
                WebDisplays.LOGGER.debug("DISCONNECT: Flushing URL cache");
                cp.flushUrlCache();
            }
            WebDisplays.LOGGER.debug("DISCONNECT: Stopping miniserv client");
            net.montoyo.wd.miniserv.client.Client.getInstance().stop();
        });
        
        ClientPlayConnectionEvents.INIT.register((handler, client) -> {
            WebDisplays.LOGGER.debug("World connect - ensuring clean state");
            if (WebDisplays.PROXY instanceof ClientProxy cp) {
                cp.closeAllBrowsers();
            }
        });
        
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            WebDisplays.LOGGER.debug("World joined - scanning for MinePads");
            if (WebDisplays.PROXY instanceof ClientProxy cp) {
                cp.scanPadsNow();
            }
        });
        
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof CreativeModeInventoryScreen creativeScreen)) return;
            if (!(screen instanceof FabricCreativeInventoryScreen fabricScreen)) return;
            if (!fabricScreen.hasAdditionalPages()) return;

            var accessor = (net.montoyo.wd.mixins.ContainerScreenAccessor) creativeScreen;
            int leftPos = accessor.getLeftPos();
            int topPos = accessor.getTopPos();
            int imageWidth = accessor.getImageWidth();

            var screenAccessor = (net.montoyo.wd.mixins.ScreenAccessor) creativeScreen;
            var renderables = screenAccessor.getRenderables();

            var oldButtons = new java.util.ArrayList<>(creativeScreen.children());
            for (var widget : oldButtons) {
                if (!(widget instanceof Button btn)) continue;
                int bx = btn.getX();
                int by = btn.getY();
                if (bx >= leftPos + 160 && bx <= leftPos + 190 &&
                    by >= topPos - 2 && by <= topPos + 16) {
                    renderables.remove(btn);
                    creativeScreen.children().remove(btn);
                }
            }

            int btnY = topPos - 50;
            int btnSize = 20;
            int leftBtnX = leftPos;
            int rightBtnX = leftPos + imageWidth - btnSize;

            screenAccessor.invokeAddRenderableWidget(Button.builder(
                Component.literal("<"),
                btn -> fabricScreen.switchToPreviousPage()
            ).pos(leftBtnX, btnY).size(btnSize, btnSize).build());

            screenAccessor.invokeAddRenderableWidget(Button.builder(
                Component.literal(">"),
                btn -> fabricScreen.switchToNextPage()
            ).pos(rightBtnX, btnY).size(btnSize, btnSize).build());

            var font = Minecraft.getInstance().font;
            renderables.add((gui, mx, my, delta) -> {
                String text = (fabricScreen.getCurrentPage() + 1) + " / " + fabricScreen.getPageCount();
                int centerX = leftPos + imageWidth / 2;
                gui.drawString(font, text, centerX - font.width(text) / 2, btnY + 6, 0xFFFFFFFF, true);
            });
        });
    
    }
}
