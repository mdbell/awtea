package me.mdbell.awtea.gfx.software;

import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.gfx.Surface;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

public class SoftwareSurface implements Surface {
    @Override
    public Rasterizer createRasterizer() {
        return null;
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public Uint8ClampedArray getPixelData() {
        return null;
    }

    @Override
    public void destroy() {

    }
}
