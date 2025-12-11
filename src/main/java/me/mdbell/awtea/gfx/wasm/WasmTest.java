package me.mdbell.awtea.gfx.wasm;

import me.mdbell.awtea.classlib.java.awt.TSurfaceRasterizerGraphics;
import me.mdbell.awtea.util.jso.JSRecord;
import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.core.JSObjects;
import org.teavm.jso.dom.html.HTMLCanvasElement;

import java.awt.*;

public class WasmTest {

	private static CanvasRenderingContext2D context2D;
	private static WasmSurface framebuffer;
	private static SurfaceCommandBuffer buffer;
	private static TSurfaceRasterizerGraphics gfx;

	public static void main(String[] args) {

		if (args == null || args.length < 1) {
			System.err.println("Usage: WasmTest <canvasId>");
			return;
		}
		String canvasId = args[0];
		HTMLCanvasElement canvasElement = (HTMLCanvasElement) Window.current().getDocument().getElementById(canvasId);
		JSRecord attrs = JSObjects.create();
		attrs.put("alpha", false);
		context2D = (CanvasRenderingContext2D) canvasElement.getContext("2d", attrs);

		WasmSurfaceBackend engine = new WasmSurfaceBackend();

		framebuffer = engine.createSurface(canvasElement.getWidth(), canvasElement.getHeight(), WasmPixelFormat.PIXEL_FORMAT_ABGR);

		gfx = new TSurfaceRasterizerGraphics(new WasmRasterizer(framebuffer));

		Window.setInterval(WasmTest::animateFrame, 1000 / 60);
	}

	private static void animateFrame() {
		gfx.setColor(new Color(0, 0, 255));
		gfx.fillRect(0, 0, framebuffer.getWidth(), framebuffer.getHeight());
		gfx.setColor(new Color(255, 0, 0));
		gfx.drawRect(10, 10, 100, 100);

		gfx.setColor(new Color(0, 255, 0));
		gfx.drawLine(
			10,
			10,
			110,
			110
		);

		context2D.putImageData(framebuffer.asImageData(), 0, 0);
	}
}
