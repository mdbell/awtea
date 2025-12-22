package me.mdbell.awtea.gfx.webgl;

/**
 * Utility for encoding component IDs as RGB colors for GPU-based hit picking.
 * <p>
 * Each component gets a unique integer ID. We encode this ID as an RGB color
 * by splitting the 24-bit ID into 8-bit R, G, B channels. This allows us to
 * render components to an offscreen buffer where each pixel's color represents
 * the component ID at that location.
 * </p>
 * <p>
 * The alpha channel is always set to 1.0 (opaque) to ensure the picking buffer
 * is fully opaque and IDs are preserved during rendering.
 * </p>
 */
public class PickingColorEncoder {
    
    /**
     * Maximum component ID that can be encoded (2^24 - 1 = 16,777,215).
     * This should be sufficient for most applications.
     */
    public static final int MAX_COMPONENT_ID = 0xFFFFFF;
    
    /**
     * Encodes a component ID as an RGB color array [r, g, b] with values in [0.0, 1.0].
     * 
     * @param componentId the component ID to encode
     * @return float array [r, g, b] with values in [0.0, 1.0]
     * @throws IllegalArgumentException if componentId is negative or exceeds MAX_COMPONENT_ID
     */
    public static float[] encodeId(int componentId) {
        if (componentId < 0 || componentId > MAX_COMPONENT_ID) {
            throw new IllegalArgumentException(
                "Component ID must be in range [0, " + MAX_COMPONENT_ID + "], got: " + componentId);
        }
        
        int r = (componentId >> 16) & 0xFF;
        int g = (componentId >> 8) & 0xFF;
        int b = componentId & 0xFF;
        
        return new float[] {
            r / 255.0f,
            g / 255.0f,
            b / 255.0f
        };
    }
    
    /**
     * Decodes a component ID from RGB values.
     * 
     * @param r red channel value (0-255)
     * @param g green channel value (0-255)
     * @param b blue channel value (0-255)
     * @return the decoded component ID
     */
    public static int decodeId(int r, int g, int b) {
        return (r << 16) | (g << 8) | b;
    }
    
    /**
     * Decodes a component ID from a pixel read from the picking buffer.
     * Handles potential precision issues from GPU rendering.
     * 
     * @param pixel array of [r, g, b, a] values (0-255)
     * @return the decoded component ID, or 0 if the pixel is fully transparent
     */
    public static int decodeIdFromPixel(int[] pixel) {
        if (pixel.length < 3) {
            return 0;
        }
        
        // Check alpha channel if present - transparent pixels have no component
        if (pixel.length >= 4 && pixel[3] == 0) {
            return 0;
        }
        
        return decodeId(pixel[0], pixel[1], pixel[2]);
    }
}
