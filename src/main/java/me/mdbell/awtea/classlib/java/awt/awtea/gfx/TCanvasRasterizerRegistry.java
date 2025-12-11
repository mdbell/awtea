package me.mdbell.awtea.classlib.java.awt.awtea.gfx;

import me.mdbell.awtea.classlib.java.awt.awtea.gfx.raster.TWebGLRasterizer;
import me.mdbell.awtea.gfx.Rasterizer;
import org.teavm.jso.dom.html.HTMLCanvasElement;

public class TCanvasRasterizerRegistry {
    public static Rasterizer getWebGLRasterizer(HTMLCanvasElement element) {
        return new TWebGLRasterizer(element);
    }

    public static Rasterizer getCanvas2DRasterizer(HTMLCanvasElement element) {
        return null;
    }
}
