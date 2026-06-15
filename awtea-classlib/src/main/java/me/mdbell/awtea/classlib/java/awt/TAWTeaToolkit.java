package me.mdbell.awtea.classlib.java.awt;

import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.classlib.java.awt.image.*;
import me.mdbell.awtea.util.JSObjectsExtensions;
import org.teavm.classlib.java.awt.TDimension;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.core.JSPromise;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLImageElement;
import org.teavm.jso.typedarrays.Uint8Array;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

@ExtensionMethod({JSObjectsExtensions.class})
public class TAWTeaToolkit extends TToolkit {

	private static final TEventQueue systemEventQueue = new TEventQueue();

	private static final TColorModel colorModel = new TDirectColorModel(32,
		0x00FF0000,  // Red
		0x0000FF00,  // Green
		0x000000FF,  // Blue
		0xFF000000   // Alpha
	);

	@Override
	public TImage createImage(byte[] imagedata, int imageoffset, int imagelength) {
		byte[] sub = Arrays.copyOfRange(imagedata, imageoffset, imageoffset + imagelength);
		CanvasRenderingContext2D context2D = loadImage(sub);
		return loadImageFromContext(context2D);
	}

	@SneakyThrows
	@Override
	public TImage createImage(String filename) {
		byte[] data = Files.readAllBytes(Paths.get(filename));
		return createImage(data);
	}

	@Override
	public TImage createImage(URL url) {
		String urlString = url.toString();
		CanvasRenderingContext2D context2D = loadImage(urlString);
		return loadImageFromContext(context2D);
	}

	@Override
	public TImage getImage(String filename) {
		// normally the toolkit should cache images loaded by filename
		// but for simplicity we just load a new image each time
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
		// Return the logical font names as per AWT specification, plus available physical fonts
		return new String[] {
			// Standard AWT logical fonts
			"Dialog",
			"DialogInput", 
			"Serif",
			"SansSerif",
			"Monospaced",
			// Physical fonts available in awtea
			"NotoSans",
			"Helvetica"
		};
	}

	@Override
	public void beep() {
		playBeepSound();
	}

	/**
	 * Plays a system beep sound using the Web Audio API.
	 * Creates a short 440Hz tone (standard beep frequency).
	 */
	@JSBody(script = 
		"try {" +
		"  var ctx = new (window.AudioContext || window.webkitAudioContext)();" +
		"  var osc = ctx.createOscillator();" +
		"  var gain = ctx.createGain();" +
		"  osc.connect(gain);" +
		"  gain.connect(ctx.destination);" +
		"  osc.frequency.value = 440;" + // Standard A4 note
		"  gain.gain.value = 0.3;" +      // 30% volume to avoid being too loud
		"  osc.start(ctx.currentTime);" +
		"  osc.stop(ctx.currentTime + 0.1);" + // 100ms beep
		"} catch(e) {" +
		"  console.warn('Unable to play beep sound:', e);" +
		"}"
	)
	private static native void playBeepSound();

	@Override
	public void sync() {
		// In browser context, ensure all pending rendering operations are flushed.
		// We use requestAnimationFrame to wait for the browser to process all pending
		// paint operations. Unlike the previous async implementation, this blocks
		// until the animation frame callback is executed.
		syncRendering().await();
	}

	/**
	 * Synchronizes rendering by waiting for the next animation frame.
	 * This ensures all pending DOM and canvas operations are flushed.
	 * Returns a promise that resolves when the next animation frame is processed.
	 * 
	 * @return a promise that resolves after the next animation frame
	 */
	@JSBody(script = 
		"return new Promise(function(resolve) {" +
		"  if (typeof requestAnimationFrame !== 'undefined') {" +
		"    requestAnimationFrame(function() { resolve(null); });" +
		"  } else {" +
		"    resolve(null);" +
		"  }" +
		"});"
	)
	private static native JSPromise<Void> syncRendering();

	@Override
	protected TEventQueue getSystemEventQueueImpl() {
		return systemEventQueue;
	}

	@Override
	public boolean prepareImage(TImage img, int w, int h, TImageObserver obs) {
		// In awtea, images are loaded synchronously, so they're always ready
		// Return true to indicate the image is fully prepared
		if (img == null) {
			return true;
		}
		
		// Get the actual dimensions if requested dimensions are -1
		int imgWidth = (w < 0) ? img.getWidth(null) : w;
		int imgHeight = (h < 0) ? img.getHeight(null) : h;
		
		// Image is always ready in our implementation
		// Notify observer if provided
		if (obs != null) {
			obs.imageUpdate(img, 
				TImageObserver.ALLBITS | TImageObserver.WIDTH | TImageObserver.HEIGHT,
				0, 0, imgWidth, imgHeight);
		}
		
		return true;
	}

	@Override
	public int checkImage(TImage img, int w, int h, TImageObserver obs) {
		// In awtea, images are loaded synchronously, so return all bits available
		if (img == null) {
			return 0;
		}
		
		// Return flags indicating the image is fully loaded
		return TImageObserver.ALLBITS | TImageObserver.WIDTH | 
		       TImageObserver.HEIGHT | TImageObserver.PROPERTIES;
	}

	@Override
	public int getScreenResolution() {
		return 96; // Common default DPI - hardcoded for simplicity (can likely be obtained
		// through some CSS/JS fuckery if needed)
		// See: https://gist.github.com/bsorrentino/cf3f8a439ef688d2f869e1c00aaeecf9
	}

	@Override
	public TDimension getScreenSize() {
		return new TDimension(Window.current().getInnerWidth(), Window.current().getInnerHeight());
	}

	@Override
	public TFontMetrics getFontMetrics(TFont font) {
		// Toolkit creates metrics with default rendering context (no AA, no fractional metrics)
		// This matches AWT behavior where Toolkit.getFontMetrics() provides baseline metrics
		me.mdbell.awtea.classlib.java.awt.font.TFontRenderContext defaultContext = 
			new me.mdbell.awtea.classlib.java.awt.font.TFontRenderContext(null, false, false);
		return new TFontMetrics(font, defaultContext);
	}

	public TImage createImage(TImageProducer producer) {
		return new TProducerImage(producer);
	}

	private TImage loadImageFromContext(CanvasRenderingContext2D context2D) {
		int width = context2D.getCanvas().getWidth();
		int height = context2D.getCanvas().getHeight();
		TBufferedImage bufferedImage = new TBufferedImage(width, height, TBufferedImage.TYPE_INT_ARGB);
		bufferedImage.putImageData(0, 0, width, height, context2D.getImageData(0, 0, width, height));
		return bufferedImage;
	}

	public static CanvasRenderingContext2D loadImage(byte[] data) {
		Uint8Array arr = new Uint8Array(data.length);
		arr.set(data);
		JSObject blob = blob(arr);
		String url = blob.createObjectUrl();
		return loadImage(url);
	}

	@Async
	private static native CanvasRenderingContext2D loadImage(String url);

	private static void loadImage(String url, AsyncCallback<CanvasRenderingContext2D> callback) {
		HTMLImageElement img = (HTMLImageElement) Window.current().getDocument().createElement("img");
		HTMLCanvasElement canvasElement = (HTMLCanvasElement) Window.current().getDocument().createElement("canvas");
		img.onLoad(evt -> {
			canvasElement.setWidth(img.getWidth());
			canvasElement.setHeight(img.getHeight());
			CanvasRenderingContext2D context = canvasElement.getContext2d(true);
			context.drawImage(img, 0, 0);
			url.revokeObjectUrl();
			callback.complete(context);
		});
		img.onEvent("error", evt -> {
			callback.error(new IOException("Unable to read image from URL"));
		});
		img.setSrc(url);
	}

	@JSBody(params = {"arr"}, script = "return new Blob([arr]);")
	private static native JSObject blob(Uint8Array arr);
}
