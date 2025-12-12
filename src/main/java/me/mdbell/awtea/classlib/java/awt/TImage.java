package me.mdbell.awtea.classlib.java.awt;

import lombok.Data;
import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import me.mdbell.awtea.classlib.java.awt.image.TImageObserver;
import me.mdbell.awtea.classlib.java.awt.image.TImageProducer;
import me.mdbell.awtea.support.ImageDataProvider;
import me.mdbell.awtea.util.JSObjectsExtensions;
import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLImageElement;


@Data
@ExtensionMethod({JSObjectsExtensions.class})
public abstract class TImage {

	public static final Object UndefinedProperty = new Object();

	public static final int SCALE_DEFAULT = 1;

	public static final int SCALE_FAST = 2;

	public static final int SCALE_SMOOTH = 4;

	public static final int SCALE_REPLICATE = 8;

	public static final int SCALE_AREA_AVERAGING = 16;

	private float accelerationPriority = 0.5f;

	public abstract int getWidth(TImageObserver observer);

	public abstract int getHeight(TImageObserver observer);

	public abstract TImageProducer getSource();

	public abstract TGraphics getGraphics();

	public abstract Object getProperty(String name, TImageObserver observer);

	public TImage getScaledInstance(int width, int height, int hints) {
		// Get source dimensions
		int srcWidth = getWidth(null);
		int srcHeight = getHeight(null);
		
		// If no scaling needed, return this
		if (srcWidth == width && srcHeight == height) {
			return this;
		}
		
		// Create canvas with target dimensions
		HTMLCanvasElement canvas = (HTMLCanvasElement) Window.current().getDocument().createElement("canvas");
		canvas.setWidth(width);
		canvas.setHeight(height);
		
		CanvasRenderingContext2D context = canvas.getContext2d(true);
		
		// Configure smoothing based on hints
		boolean smooth = true;
		if (hints == SCALE_FAST || hints == SCALE_REPLICATE) {
			smooth = false;
		}
		JSObjectsExtensions.setImageSmoothingEnabled(context, smooth);
		
		// Draw scaled image
		if (this instanceof ImageDataProvider) {
			ImageDataProvider provider = (ImageDataProvider) this;
			context.putImageData(provider.getImageData(), 0, 0);
			
			// Create temporary canvas with original size
			HTMLCanvasElement tempCanvas = (HTMLCanvasElement) Window.current().getDocument().createElement("canvas");
			tempCanvas.setWidth(srcWidth);
			tempCanvas.setHeight(srcHeight);
			CanvasRenderingContext2D tempContext = tempCanvas.getContext2d(true);
			tempContext.putImageData(provider.getImageData(), 0, 0);
			
			// Draw scaled from temp canvas
			context.drawImage(tempCanvas, 0, 0, width, height);
		} else {
			// Try to use HTMLImageElement if available
			HTMLImageElement imgElement = (HTMLImageElement) Window.current().getDocument().createElement("img");
			// This path is for images that might have a source URL
			// For now, we'll create a blank scaled image as fallback
			context.clearRect(0, 0, width, height);
		}
		
		// Extract pixel data and create new TBufferedImage
		TBufferedImage result = new TBufferedImage(width, height, TBufferedImage.TYPE_INT_ARGB);
		result.putImageData(context.getImageData(0, 0, width, height));
		
		return result;
	}

}
