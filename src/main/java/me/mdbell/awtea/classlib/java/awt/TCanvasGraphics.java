package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.awtea.gfx.TCanvasRasterizerRegistry;
import org.teavm.jso.dom.html.HTMLCanvasElement;

public class TCanvasGraphics extends TSurfaceRasterizerGraphics {

    protected final HTMLCanvasElement canvas;

    public TCanvasGraphics(HTMLCanvasElement canvas, boolean webGl) {
        super(webGl ? TCanvasRasterizerRegistry.getWebGLRasterizer(canvas) : TCanvasRasterizerRegistry.getCanvas2DRasterizer(canvas));
        this.canvas = canvas;
    }

    protected TCanvasGraphics(TCanvasGraphics other) {
        super(other);
        this.canvas = other.canvas;
        this.clip = other.clip;
        this.transform.setTransform(other.transform);
        this.color = other.color;
        this.font = other.font;
    }

    @Override
    public TGraphics create() {
        return new TCanvasGraphics(this);
    }

    public void onCanvasResize(int newWidth, int newHeight) {
        rasterizer.onResize(newWidth, newHeight);
    }
}
