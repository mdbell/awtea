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
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLImageElement;
import org.teavm.jso.typedarrays.Uint8Array;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@ExtensionMethod({JSObjectsExtensions.class})
public class TAWTeaToolkit extends TToolkit {

	private static final TEventQueue systemEventQueue = new TEventQueue();

	private static final TColorModel colorModel = new TDirectColorModel(32,
		0x00FF0000,  // Red
		0x0000FF00,  // Green
		0x000000FF,  // Blue
		0xFF000000   // Alpha
	);

	// Image caches
	private final Map<String, TImage> filenameCache = new HashMap<>();
	private final Map<String, TImage> urlCache = new HashMap<>();

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
		// Check cache first
		TImage cached = filenameCache.get(filename);
		if (cached != null) {
			return cached;
		}
		
		// Load and cache the image
		TImage image = createImage(filename);
		filenameCache.put(filename, image);
		return image;
	}

	@Override
	public TImage getImage(URL url) {
		String urlString = url.toString();
		
		// Check cache first
		TImage cached = urlCache.get(urlString);
		if (cached != null) {
			return cached;
		}
		
		// Load and cache the image
		TImage image = createImage(url);
		urlCache.put(urlString, image);
		return image;
	}

	@Override
	public TColorModel getColorModel() {
		return colorModel;
	}

	@Override
	public String[] getFontList() {
		return new String[0];
	}

	@Override
	public void beep() {

	}

	@Override
	public void sync() {

	}

	@Override
	protected TEventQueue getSystemEventQueueImpl() {
		return systemEventQueue;
	}

	@Override
	public boolean prepareImage(TImage img, int w, int h, TImageObserver obs) {
		// Check if image is already loaded
		if (img instanceof TBufferedImage) {
			// TBufferedImage is always fully loaded
			if (obs != null) {
				int width = img.getWidth(null);
				int height = img.getHeight(null);
				obs.imageUpdate(img, TImageObserver.ALLBITS | TImageObserver.WIDTH | TImageObserver.HEIGHT, 
					0, 0, width, height);
			}
			return true;
		}
		
		// For images that need dimensions
		int width = img.getWidth(obs);
		int height = img.getHeight(obs);
		
		// If we got valid dimensions, the image is loaded
		if (width > 0 && height > 0) {
			// Notify observer if provided
			if (obs != null) {
				obs.imageUpdate(img, TImageObserver.ALLBITS | TImageObserver.WIDTH | TImageObserver.HEIGHT, 
					0, 0, width, height);
			}
			return true;
		}
		
		// Image needs loading - return false to indicate loading is needed
		return false;
	}

	@Override
	public int checkImage(TImage img, int w, int h, TImageObserver obs) {
		// Check if image is fully loaded
		if (img instanceof TBufferedImage) {
			// TBufferedImage is always fully loaded
			return TImageObserver.ALLBITS | TImageObserver.WIDTH | TImageObserver.HEIGHT;
		}
		
		// For other images, check dimensions
		int width = img.getWidth(obs);
		int height = img.getHeight(obs);
		
		// If we got valid dimensions, the image is loaded
		if (width > 0 && height > 0) {
			return TImageObserver.ALLBITS | TImageObserver.WIDTH | TImageObserver.HEIGHT;
		}
		
		// Image encountered an error (negative dimensions indicate error)
		if (width < 0 || height < 0) {
			return TImageObserver.ERROR;
		}
		
		// No information available yet (dimensions are 0 or unknown)
		return 0;
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
		bufferedImage.putImageData(context2D.getImageData(0, 0, width, height));
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
