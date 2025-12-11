package me.mdbell.awtea.gfx;

import org.teavm.jso.typedarrays.Uint8ClampedArray;

public interface Surface {

    /**
     * Creates a new rasterizer for drawing to this surface.
     *
     * @return A new TRasterizer instance.
     */
    Rasterizer createRasterizer();

    /**
     * Resizes this surface to the specified width and height.
     * Resizing does not preserve the existing pixel data, and
     * the contents of the surface after resizing are undefined.
     *
     * @param width  The new width in pixels.
     * @param height The new height in pixels.
     */
    void resize(int width, int height);

    /**
     * Gets the width of this surface in pixels.
     *
     * @return The width of the surface.
     */
    int getWidth();

    /**
     * Gets the height of this surface in pixels.
     *
     * @return The height of the surface.
     */
    int getHeight();

    /**
     * Gets a Uint8ClampedArray view of the pixel data for this surface.
     * Note: this should be a direct view of the underlying pixel data, so modifying
     * the contents of the array will modify the surface's pixels.
     *
     * @return A Uint8ClampedArray containing the pixel data.
     */
    Uint8ClampedArray getPixelData();

    /**
     * Frees any resources associated with this surface.
     */
    void destroy();
}
