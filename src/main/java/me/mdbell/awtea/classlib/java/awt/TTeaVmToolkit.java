package me.mdbell.awtea.classlib.java.awt;


import lombok.SneakyThrows;
import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.classlib.java.awt.image.*;
import org.teavm.classlib.java.awt.TDimension;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLImageElement;
import org.teavm.jso.typedarrays.Uint8Array;
import org.teavm.jso.typedarrays.Uint8ClampedArray;
import me.mdbell.awtea.util.JSObjectsExtensions;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

@ExtensionMethod({JSObjectsExtensions.class})
public class TTeaVmToolkit extends TToolkit{

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
		return new String[0];
	}

	@Override
	public void beep() {

	}

	@Override
	public void sync() {

	}

	@Override
	public boolean prepareImage(TImage img, int w, int h, TImageObserver obs) {
		return false;
	}

	@Override
	public int checkImage(TImage img, int w, int h, TImageObserver obs) {
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
        String url = createObjectUrl(blob);
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
            revokeObjectUrl(url);
            callback.complete(context);
        });
        img.onEvent("error", evt -> {
            callback.error(new IOException("Unable to read image from URL"));
        });
        img.setSrc(url);
    }

    @JSBody(params = {"arr"}, script = "return new Blob([arr]);")
    private static native JSObject blob(Uint8Array arr);

    @JSBody(params = {"blob"}, script = "return (window.URL || window.webkitURL).createObjectURL(blob);")
    private static native String createObjectUrl(JSObject blob);

    @JSBody(params = {"url"}, script = "return (window.URL || window.webkitURL).revokeObjectURL(url);")
    private static native void revokeObjectUrl(String url);

    @JSByRef
    @JSBody(params = {"arr"}, script = "return arr;")
    private static native byte[] getArrayFromJS(Uint8ClampedArray arr);
}
