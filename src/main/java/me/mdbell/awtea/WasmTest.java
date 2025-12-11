package me.mdbell.awtea;

import me.mdbell.awtea.wasm.SurfaceCommandBuffer;
import me.mdbell.awtea.wasm.WasmAwtEngine;
import me.mdbell.awtea.wasm.WasmPixelFormat;
import me.mdbell.awtea.wasm.WasmSurface;
import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.dom.html.HTMLCanvasElement;

public class WasmTest {

    private static CanvasRenderingContext2D context2D;
    private static WasmSurface framebuffer;
    private static WasmSurface overlay;
    private static SurfaceCommandBuffer buffer;

    public static void main(String[] args) {

        if (args == null || args.length < 1) {
            System.err.println("Usage: WasmTest <canvasId>");
            return;
        }
        String canvasId = args[0];
        HTMLCanvasElement canvasElement = (HTMLCanvasElement) Window.current().getDocument().getElementById(canvasId);
        context2D = (CanvasRenderingContext2D) canvasElement.getContext("2d");

        WasmAwtEngine engine = WasmAwtEngine.get();

        framebuffer = engine.createSurface(canvasElement.getWidth(), canvasElement.getHeight(), WasmPixelFormat.PIXEL_FORMAT_ABGR);
        overlay = engine.createSurface(75, 75, WasmPixelFormat.PIXEL_FORMAT_RGB);

        buffer = engine.createCommandBuffer(1024);

        // note: colors are always argb in command buffer, irrespective of surface pixel format
        buffer.emitSetColor(0x00FF0000, 0); // Red
        buffer.emitFillRect(0, 0, 10, 10);
        buffer.emitSetColor(0x000000FF, 0); // Blue
        buffer.emitFillRect(10, 0, 10, 10);

        buffer.emitSetColor(0x0000FF00, 0); // Green
        buffer.emitFillRect(20, 0, 10, 10);

        float sx = 2.0f;
        float sy = 2.0f;
        buffer.emitSetTransform(
                sx, 0, 0,   // row 1: scale X, no shear, no translation
                0, sy, 0    // row 2: no shear, scale Y, no translation
        );

        buffer.emitSetColor(0x7F0000FF, 0); // Semi-transparent Blue
        buffer.emitDrawRect(10, 10, 10, 10);
        overlay.renderCommands(buffer.getBasePtr(), buffer.getCount());

        buffer.reset();

        Window.requestAnimationFrame(WasmTest::animateFrame);
    }

    private static void animateFrame(double time) {
        buffer.reset();
        // RABG
        buffer.emitSetColor(0xFF00FF00, 1); // black bg
        buffer.emitClearRect(0, 0, framebuffer.getWidth(), framebuffer.getHeight());

        buffer.emitSetClipRect(10, 10, 20, 20);

        // static red square
        buffer.emitSetColor(0x7FFF0000, 0);
        buffer.emitFillRect(0, 0, 100, 100);

        buffer.emitSetClipRect(0, 0, 0, 0);

        int red = (int) ((Math.sin(time * 0.001) * 0.5 + 0.5) * 255);
        int green = (int) ((Math.sin(time * 0.001 + 2) * 0.5 + 0.5) * 255);
        int blue = (int) ((Math.sin(time * 0.001 + 4) * 0.5 + 0.5) * 255);
        int color = (0xFF << 24) | (red << 16) | (green << 8) | blue;
        buffer.emitSetColor(color, 0);
        buffer.emitFillRect(10, 50, 100, 100);

        int x = 25 + (int) ((Math.sin(time * 0.002) * 0.5 + 0.5) * (framebuffer.getWidth() - 75));
        buffer.emitFillRect(x, 150, 20, 20);

        // bounce the BGR image around the screen
        int imgX = 50 + (int) ((Math.sin(time * 0.0015) * 0.5 + 0.5) * (framebuffer.getWidth() - overlay.getWidth() - 50));
        int imgY = 50 + (int) ((Math.cos(time * 0.0015) * 0.5 + 0.5) * (framebuffer.getHeight() - overlay.getHeight() - 50));

        buffer.emitDrawSurface(overlay, imgX, imgY);

        framebuffer.renderCommands(buffer.getBasePtr(), buffer.getCount());

        context2D.putImageData(framebuffer.asImageData(), 0, 0);

        // Request the next frame
        Window.requestAnimationFrame(WasmTest::animateFrame);
    }
}
