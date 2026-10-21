package net.montoyo.wd.client.renderers;

import com.cinemamod.mcef.MCEFBrowser;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.Minecraft;

/**
 * Wraps MCEF's OpenGL texture ID for use with Minecraft 1.21's rendering pipeline.
 * This allows MCEF to update its texture natively while satisfying Minecraft's
 * requirement for a ResourceLocation-based texture.
 */
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
    
    /**
     * Gets or creates a ResourceLocation for the given MCEF browser's texture.
     * Registers the texture with Minecraft's TextureManager.
     */
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
            
            // Register with Minecraft's texture manager
            Minecraft.getInstance().getTextureManager().register(loc, texture);
            
            return loc;
        });
    }
    
    /**
     * Returns the OpenGL texture ID for direct binding if needed.
     */
    public int getGlTextureId() {
        return glTextureId;
    }
    
    /**
     * Returns this texture's ResourceLocation.
     */
    public ResourceLocation getLocation() {
        return location;
    }
    
    @Override
    public void close() {
        // Don't delete the texture - MCEF owns it
        // Just remove from our maps
        ID_TO_LOCATION.remove(glTextureId);
        LOCATION_TO_TEXTURE.remove(location);
    }
    
    /**
     * Called when the browser's texture ID changes (e.g., after resize).
     * Updates our internal reference.
     */
    public void updateTextureId(int newId) {
        if (newId != this.glTextureId && newId > 0) {
            this.id = newId;
        }
    }
}
