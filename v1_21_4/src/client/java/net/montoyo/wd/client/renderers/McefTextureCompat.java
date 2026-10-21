/*
 * Copyright (C) 2025 WebDisplays
 *
 * MCEF Texture compatibility wrapper for Minecraft 1.21.
 * Wraps MCEF's OpenGL texture ID for use with Minecraft's rendering pipeline.
 */

package net.montoyo.wd.client.renderers;

import com.cinemamod.mcef.MCEFBrowser;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;

/**
 * Simple wrapper for MCEF textures that works with Minecraft 1.21's rendering.
 */
public final class McefTextureCompat {
    private static final Map<Integer, ResourceLocation> ID_TO_LOCATION = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, MCEFTextureWrapper> LOCATION_TO_TEXTURE = new ConcurrentHashMap<>();

    private McefTextureCompat() {
    }

    /**
     * Gets or creates a ResourceLocation for the given MCEF browser's texture.
     */
    public static ResourceLocation getTextureLocation(MCEFBrowser browser) {
        if (browser == null || browser.getRenderer() == null) {
            return ResourceLocation.fromNamespaceAndPath("minecraft", "missingno");
        }

        int glId = browser.getRenderer().getTextureID();
        if (glId <= 0) {
            return ResourceLocation.fromNamespaceAndPath("minecraft", "missingno");
        }

        // Use browser hash as key to track texture changes per browser
        int browserHash = System.identityHashCode(browser);
        
        ResourceLocation loc = ID_TO_LOCATION.get(browserHash);
        if (loc == null) {
            // First time seeing this browser - create new texture
            loc = ResourceLocation.fromNamespaceAndPath("webdisplays", "mcef/tex_" + browserHash);
            MCEFTextureWrapper texture = new MCEFTextureWrapper(glId, loc);
            LOCATION_TO_TEXTURE.put(loc, texture);
            Minecraft.getInstance().getTextureManager().register(loc, texture);
            ID_TO_LOCATION.put(browserHash, loc);
        } else {
            // Existing browser - check if GL ID changed
            MCEFTextureWrapper texture = LOCATION_TO_TEXTURE.get(loc);
            if (texture != null && texture.getGlTextureId() != glId) {
                // MCEF changed texture ID (e.g., after resize) - update wrapper
                texture.updateGlTextureId(glId);
            }
        }
        
        return loc;
    }

    /**
     * Updates all browser textures before rendering.
     * (No-op for this implementation - MCEF handles texture updates internally)
     */
    public static void updateTextures() {
        // MCEF updates its textures internally via OpenGL
    }

    /**
     * Removes a browser's texture entry when closed.
     */
    public static void removeBrowser(MCEFBrowser browser) {
        if (browser == null || browser.getRenderer() == null) return;
        int browserHash = System.identityHashCode(browser);
        ResourceLocation loc = ID_TO_LOCATION.remove(browserHash);
        if (loc != null) {
            MCEFTextureWrapper texture = LOCATION_TO_TEXTURE.remove(loc);
            if (texture != null) {
                texture.close();
            }
        }
    }

    /**
     * Wrapper for MCEF's OpenGL texture.
     */
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
        public void close() {
            ID_TO_LOCATION.values().remove(location);
            LOCATION_TO_TEXTURE.remove(location);
        }
    }
}
