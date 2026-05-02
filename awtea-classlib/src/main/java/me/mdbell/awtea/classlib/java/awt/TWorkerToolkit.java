package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.awtea.TMainThreadBridge;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import me.mdbell.awtea.classlib.java.awt.image.TColorModel;
import me.mdbell.awtea.classlib.java.awt.image.TDirectColorModel;
import me.mdbell.awtea.classlib.java.awt.image.TImageObserver;
import me.mdbell.awtea.classlib.java.awt.image.TImageProducer;
import org.teavm.classlib.java.awt.TDimension;
import org.teavm.jso.JSBody;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.typedarrays.Uint8Array;

import java.net.URL;
import java.util.Arrays;

/**
 * Worker-side Toolkit implementation.
 *
 * Pure-logic methods (font list, color model, image creation from bytes) are
 * implemented directly. All DOM-touching operations delegate to TMainThreadBridge
 * via postMessage, except sync() which uses the worker's own requestAnimationFrame.
 */
public class TWorkerToolkit extends TToolkit {

    public TWorkerToolkit() {
        TMainThreadBridge.init();
    }

    private static final TEventQueue systemEventQueue = new TEventQueue();

    private static final TColorModel colorModel = new TDirectColorModel(32,
            0x00FF0000,
            0x0000FF00,
            0x000000FF,
            0xFF000000
    );

    // -------------------------------------------------------------------------
    // DOM-delegating methods
    // -------------------------------------------------------------------------

    @Override
    public TDimension getScreenSize() {
        TMainThreadBridge.BridgeResponse resp = TMainThreadBridge.request("getScreenSize");
        return new TDimension(resp.getWidth(), resp.getHeight());
    }

    @Override
    public void sync() {
        // requestAnimationFrame is available on DedicatedWorkerGlobalScope —
        // no main-thread round-trip needed.
        workerRaf().await();
    }

    @JSBody(script =
            "return new Promise(function(resolve) {" +
            "  requestAnimationFrame(function() { resolve(null); });" +
            "});")
    private static native JSPromise<Void> workerRaf();

    @Override
    public void beep() {
        TMainThreadBridge.send("beep");
    }

    @Override
    public TImage createImage(URL url) {
        return pixelsFromResponse(TMainThreadBridge.request("loadImage",
                req -> req.setUrl(url.toString())));
    }

    private static TImage pixelsFromResponse(TMainThreadBridge.BridgeResponse resp) {
        int w = resp.getWidth();
        int h = resp.getHeight();
        Uint8Array rgba = new Uint8Array(resp.getData());
        int[] argb = new int[w * h];
        for (int i = 0; i < argb.length; i++) {
            int r = rgba.get(i * 4)     & 0xFF;
            int g = rgba.get(i * 4 + 1) & 0xFF;
            int b = rgba.get(i * 4 + 2) & 0xFF;
            int a = rgba.get(i * 4 + 3) & 0xFF;
            argb[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        TBufferedImage img = new TBufferedImage(w, h, TBufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, w, h, argb, 0, w);
        return img;
    }

    // -------------------------------------------------------------------------
    // Pure-logic methods — same as TAWTeaToolkit
    // -------------------------------------------------------------------------

    @Override
    public TImage createImage(byte[] imagedata, int imageoffset, int imagelength) {
        byte[] sub = Arrays.copyOfRange(imagedata, imageoffset, imageoffset + imagelength);
        Uint8Array arr = new Uint8Array(sub.length);
        arr.set(sub);
        TMainThreadBridge.BridgeResponse resp = TMainThreadBridge.request("loadImage",
                req -> req.setBytes(arr.getBuffer()));
        return pixelsFromResponse(resp);
    }

    @Override
    public TImage createImage(String filename) {
        try {
            return createImage(new java.net.URI(filename).toURL());
        } catch (Exception e) {
            return new TBufferedImage(1, 1, TBufferedImage.TYPE_INT_ARGB);
        }
    }

    @Override
    public TImage getImage(String filename) {
        return createImage(filename);
    }

    @Override
    public TImage getImage(URL url) {
        return createImage(url);
    }

    @Override
    public TColorModel getColorModel() {
        return colorModel;
    }

    @Override
    public String[] getFontList() {
        return new String[]{
                "Dialog", "DialogInput", "Serif", "SansSerif", "Monospaced",
                "NotoSans", "Helvetica"
        };
    }

    @Override
    public int getScreenResolution() {
        return 96;
    }

    @Override
    public TFontMetrics getFontMetrics(TFont font) {
        return font.getFontMetrics();
    }

    @Override
    public TImage createImage(TImageProducer producer) {
        return new TProducerImage(producer);
    }

    @Override
    public boolean prepareImage(TImage img, int w, int h, TImageObserver obs) {
        if (img == null) return true;
        int imgWidth  = (w < 0) ? img.getWidth(null)  : w;
        int imgHeight = (h < 0) ? img.getHeight(null) : h;
        if (obs != null) {
            obs.imageUpdate(img,
                    TImageObserver.ALLBITS | TImageObserver.WIDTH | TImageObserver.HEIGHT,
                    0, 0, imgWidth, imgHeight);
        }
        return true;
    }

    @Override
    public int checkImage(TImage img, int w, int h, TImageObserver obs) {
        if (img == null) return 0;
        return TImageObserver.ALLBITS | TImageObserver.WIDTH
                | TImageObserver.HEIGHT | TImageObserver.PROPERTIES;
    }

    @Override
    protected TEventQueue getSystemEventQueueImpl() {
        return systemEventQueue;
    }
}
