package net.montoyo.wd.client.renderers;

import com.cinemamod.mcef.MCEFBrowser;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;

public class MCEFTexture extends AbstractTexture {
    private static final Map<Integer, ResourceLocation> ID_TO_LOCATION = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, MCEFTexture> LOCATION_TO_TEXTURE = new ConcurrentHashMap<>();

    private final int glTextureId;
    private final ResourceLocation location;

    private MCEFTexture(int glTextureId, ResourceLocation location) {
        this.glTextureId = glTextureId;
        this.location = location;
        this.id = glTextureId;
    }

    public static ResourceLocation getTextureLocation(MCEFBrowser browser) {
        if (browser == null || browser.getRenderer() == null) {
            return ResourceLocation.fromNamespaceAndPath("minecraft", "missingno");
        }

        int glId = browser.getRenderer().getTextureID();
        if (glId <= 0) {
            return ResourceLocation.fromNamespaceAndPath("minecraft", "missingno");
        }

        return ID_TO_LOCATION.computeIfAbsent(glId, id -> {
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("webdisplays", "mcef/tex_" + id);
            MCEFTexture texture = new MCEFTexture(id, loc);
            LOCATION_TO_TEXTURE.put(loc, texture);

            Minecraft.getInstance().getTextureManager().register(loc, texture);

            return loc;
        });
    }

    public int getGlTextureId() {
        return glTextureId;
    }

    public ResourceLocation getLocation() {
        return location;
    }

    @Override
    public void load(ResourceManager manager) throws IOException {
        registerTexture();
    }

    private void registerTexture() {
        Minecraft.getInstance().getTextureManager().register(location, this);
    }

    @Override
    public void close() {
        ID_TO_LOCATION.remove(glTextureId);
        LOCATION_TO_TEXTURE.remove(location);
    }

    public void updateTextureId(int newId) {
        if (newId != this.glTextureId && newId > 0) {
            this.id = newId;
        }
    }
}
