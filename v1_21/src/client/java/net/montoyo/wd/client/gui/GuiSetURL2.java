package net.montoyo.wd.client.gui;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.montoyo.wd.WebDisplays;
import net.montoyo.wd.WebDisplaysMod;
import net.montoyo.wd.client.ClientProxy;
import net.montoyo.wd.client.gui.controls.Button;
import net.montoyo.wd.client.gui.controls.TextField;
import net.montoyo.wd.client.gui.loading.FillControl;
import net.montoyo.wd.entity.ScreenBlockEntity;
import net.montoyo.wd.entity.ScreenData;
import net.montoyo.wd.item.ItemMinePad2;
import net.montoyo.wd.net.WDNetworkRegistry;
import net.montoyo.wd.net.server_bound.C2SMessageMinepadUrl;
import net.montoyo.wd.net.payload.UrlUpdatePayload;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.serialization.Util;
import net.montoyo.wd.utilities.math.Vector3i;

import java.util.Map;
import java.util.UUID;

public class GuiSetURL2 extends WDScreen {

	private ScreenBlockEntity tileEntity;
	private BlockPos blockPos;
	private BlockSide screenSide;
	private Vector3i remoteLocation;

	private ItemStack stack;
	private final boolean isPad;

	private final String screenURL;

	@FillControl
	private TextField tfURL;

	@FillControl
	private Button btnShutDown;

	@FillControl
	private Button btnCancel;

	@FillControl
	private Button btnOk;

	public GuiSetURL2(ScreenBlockEntity tes, BlockSide side, String url, Vector3i rl) {
		super(Component.literal(""));
		tileEntity = tes;
		blockPos = (tes != null) ? tes.getBlockPos() : null;
		screenSide = side;
		remoteLocation = rl;
		isPad = false;
		screenURL = url;
	}

	public GuiSetURL2(BlockPos pos, BlockSide side, String url, Vector3i rl) {
		super(Component.literal(""));
		tileEntity = null;
		blockPos = pos;
		screenSide = side;
		remoteLocation = rl;
		isPad = false;
		screenURL = url;
	}

	public GuiSetURL2(ItemStack is, String url) {
		super(Component.literal(""));
		isPad = true;
		stack = is;
		screenURL = url;
	}

	@Override
	public void init() {
		super.init();
		loadFrom(ResourceLocation.fromNamespaceAndPath("webdisplays", "gui/seturl.json"));
		if (tfURL != null) {
			String urlToShow = screenURL;
			if (!isPad && tileEntity != null) {
				ScreenData scr = tileEntity.getScreen(screenSide);
				if (scr != null && scr.url != null && !scr.url.isEmpty()) {
					urlToShow = scr.url;
				}
			}
			tfURL.setText(urlToShow);
		}
	}

	@Override
	protected void addLoadCustomVariables(Map<String, Double> vars) {
		vars.put("isPad", isPad ? 1.0 : 0.0);
	}

	protected UUID getUUID() {
		if (stack == null || !(stack.getItem() instanceof ItemMinePad2))
			throw new RuntimeException("Get UUID is being called for a non-minepad UI");
		CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
		CompoundTag tag = customData.copyTag();
		if (!tag.hasUUID("PadID"))
			tag.putUUID("PadID", UUID.randomUUID());
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		return tag.getUUID("PadID");
	}

	@GuiSubscribe
	public void onButtonClicked(Button.ClickEvent ev) {
		if (ev.getSource() == btnCancel)
			minecraft.setScreen(null);
		else if (ev.getSource() == btnOk)
			validate(tfURL.getText());
		else if (ev.getSource() == btnShutDown) {
			if (isPad) {
				WDNetworkRegistry.INSTANCE.sendToServer(new C2SMessageMinepadUrl(
						getUUID(),
						""
				));
				CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
				if (customData != CustomData.EMPTY) {
					CompoundTag tag = customData.copyTag();
					tag.remove("PadID");
					stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
				}
			}
			minecraft.setScreen(null);
		}
	}

	@GuiSubscribe
	public void onEnterPressed(TextField.EnterPressedEvent ev) {
		validate(ev.getText());
	}

	private void validate(String url) {
		if (url == null || url.trim().isEmpty()) {
			minecraft.setScreen(null);
			return;
		}

		url = url.trim();
		url = Util.addProtocol(url);

		if (!url.startsWith("http://") && !url.startsWith("https://")) {
			minecraft.setScreen(null);
			return;
		}

		if (isPad) {
			UUID uuid = getUUID();
			WDNetworkRegistry.INSTANCE.sendToServer(new C2SMessageMinepadUrl(uuid, url));

			CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
			CompoundTag tag = customData.copyTag();
			tag.putString("PadURL", url);
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

			ClientProxy.PadData pd = ((ClientProxy) WebDisplaysMod.PROXY).getPadByID(uuid);
			if (pd != null && pd.view != null) {
				pd.view.loadURL(WebDisplaysMod.applyBlacklist(url));
			}
		} else {
			BlockPos targetPos = (tileEntity != null) ? tileEntity.getBlockPos() : blockPos;
			if (targetPos != null) {
				net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
					new UrlUpdatePayload(targetPos, screenSide, url)
				);
			}
		}

		minecraft.setScreen(null);
	}

	@Override
	public boolean isForBlock(BlockPos bp, BlockSide side) {
		if (blockPos != null) {
			return (remoteLocation != null && remoteLocation.equalsBlockPos(bp)) || (bp.equals(blockPos) && side == screenSide);
		}
		if (tileEntity == null) return false;
		return (remoteLocation != null && remoteLocation.equalsBlockPos(bp)) || (bp.equals(tileEntity.getBlockPos()) && side == screenSide);
	}

	public net.minecraft.world.level.Level getWorld() {
		if (tileEntity == null) {
			WebDisplays.LOGGER.debug("GuiSetURL2.getWorld(): tileEntity is null, returning minecraft.level");
			return minecraft.level;
		}
		return tileEntity.getLevel();
	}

}
