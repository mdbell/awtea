package me.mdbell.awtea.classlib.java.awt.awtea.gfx;

import me.mdbell.awtea.classlib.java.awt.awtea.gfx.raster.TWebGLRasterizer;
import org.teavm.jso.dom.html.HTMLCanvasElement;

public class TRasterizerRegistry {
	public static TRasterizer getWebGLRasterizer(HTMLCanvasElement element) {
		return new TWebGLRasterizer(element);
	}

	public static TRasterizer getCanvas2DRasterizer(HTMLCanvasElement element) {
		return null;
	}
}
