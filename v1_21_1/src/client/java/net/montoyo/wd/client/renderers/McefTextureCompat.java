package net.montoyo.wd.client.renderers;

import com.cinemamod.mcef.MCEFBrowser;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;

public final class McefTextureCompat {
    private static final Map<Integer, ResourceLocation> ID_TO_LOCATION = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, MCEFTextureWrapper> LOCATION_TO_TEXTURE = new ConcurrentHashMap<>();

    private McefTextureCompat() {
    }

    public static ResourceLocation getTextureLocation(MCEFBrowser browser) {
        if (browser == null || browser.getRenderer() == null) {
            return ResourceLocation.fromNamespaceAndPath("minecraft", "missingno");
        }

        int glId = browser.getRenderer().getTextureID();
        if (glId <= 0) {
            return ResourceLocation.fromNamespaceAndPath("minecraft", "missingno");
        }

        int browserHash = System.identityHashCode(browser);

        ResourceLocation loc = ID_TO_LOCATION.get(browserHash);
        if (loc == null) {
            loc = ResourceLocation.fromNamespaceAndPath("webdisplays", "mcef/tex_" + browserHash);
            MCEFTextureWrapper texture = new MCEFTextureWrapper(glId, loc);
            LOCATION_TO_TEXTURE.put(loc, texture);
            Minecraft.getInstance().getTextureManager().register(loc, texture);
            ID_TO_LOCATION.put(browserHash, loc);
        } else {
            MCEFTextureWrapper texture = LOCATION_TO_TEXTURE.get(loc);
            if (texture != null && texture.getGlTextureId() != glId) {
                texture.updateGlTextureId(glId);
            }
        }

        return loc;
    }

    public static void updateTextures() {
    }

    public static void removeBrowser(MCEFBrowser browser) {
        if (browser == null || browser.getRenderer() == null) return;
        int glId = browser.getRenderer().getTextureID();
        ResourceLocation loc = ID_TO_LOCATION.remove(glId);
        if (loc != null) {
            MCEFTextureWrapper texture = LOCATION_TO_TEXTURE.remove(loc);
            if (texture != null) {
                texture.close();
            }
        }
    }

    private static class MCEFTextureWrapper extends AbstractTexture {
        private int glTextureId;
        private final ResourceLocation location;

        MCEFTextureWrapper(int glTextureId, ResourceLocation location) {
            this.glTextureId = glTextureId;
            this.location = location;
            this.id = glTextureId;
        }

        int getGlTextureId() {
            return glTextureId;
        }

        void updateGlTextureId(int newId) {
            this.glTextureId = newId;
            this.id = newId;
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
            ID_TO_LOCATION.values().remove(location);
            LOCATION_TO_TEXTURE.remove(location);
        }
    }
}
