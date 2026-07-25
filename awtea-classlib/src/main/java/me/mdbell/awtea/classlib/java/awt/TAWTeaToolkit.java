package me.mdbell.awtea.classlib.java.awt;

import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.classlib.java.awt.image.*;
import me.mdbell.awtea.util.BrowserToolkitSupport;
import me.mdbell.awtea.util.JSObjectsExtensions;
import org.teavm.classlib.java.awt.TDimension;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.core.JSArray;
import org.teavm.jso.file.Blob;
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
		BrowserToolkitSupport.playBeepSound();
	}

	@Override
	public void sync() {
		// In browser context, ensure all pending rendering operations are flushed.
		// We use requestAnimationFrame to wait for the browser to process all pending
		// paint operations. Unlike the previous async implementation, this blocks
		// until the animation frame callback is executed.
		BrowserToolkitSupport.syncRendering().await();
	}

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
		return font.getFontMetrics();
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
		// Typed Blob ctor instead of @JSBody: the wasm-gc backend does not
		// process @JSBody methods declared in package-mapped classlib classes
		// (they reach codegen as bare natives and fail with "not annotated
		// with @Import"), and classlib should avoid raw JSO helpers anyway.
		Blob blob = new Blob(JSArray.of(arr));
		String url = blob.createObjectUrl();
		return loadImage(url);
	}

	// Await a promise whose JS callbacks live in BrowserToolkitSupport
	// (unmapped): the previous @Async implementation's onLoad lambda was
	// declared in this package-mapped class, got CPS-tainted under wasm-gc,
	// and trapped on image completion — permanently suspending the caller.
	private static CanvasRenderingContext2D loadImage(String url) {
		return BrowserToolkitSupport.loadImage(url).await();
	}

}
