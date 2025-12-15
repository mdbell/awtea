package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.event.TInputEvent;
import me.mdbell.awtea.classlib.java.awt.image.TColorModel;
import me.mdbell.awtea.classlib.java.awt.image.TImageObserver;
import me.mdbell.awtea.classlib.java.awt.image.TImageProducer;
import org.teavm.classlib.java.awt.TDimension;

import java.net.URL;

/**
 * @see java.awt.Toolkit
 */
public abstract class TToolkit {

	private static final TToolkit INSTANCE = TToolKitFactory.createToolkit();

	public abstract boolean prepareImage(TImage img, int w, int h, TImageObserver obs);

	public abstract int checkImage(TImage img, int w, int h, TImageObserver obs);

	public abstract int getScreenResolution();

	public abstract TDimension getScreenSize();

	public abstract TFontMetrics getFontMetrics(TFont font);

	public abstract TImage createImage(TImageProducer producer);

	public TImage createImage(byte[] imagedata) {
		return createImage(imagedata, 0, imagedata.length);
	}

	public abstract TImage createImage(byte[] imagedata, int imageoffset, int imagelength);

	public abstract TImage createImage(String filename);

	public abstract TImage createImage(URL url);

	public abstract TImage getImage(String filename);

	public abstract TImage getImage(URL url);

	// print job

	// clipboard

	public abstract TColorModel getColorModel();

	public abstract String[] getFontList();

	public abstract void beep();

	public abstract void sync();

	protected abstract TEventQueue getSystemEventQueueImpl();

	public final TEventQueue getSystemEventQueue() {
		return getDefaultToolkit().getSystemEventQueueImpl();
	}

	public final Object getDesktopProperty(String name) {
		return null;
	}

	public int getMaximumCursorColors() {
		return 0;
	}

	public int getMenuShortcutKeyMask() {
		return TInputEvent.CTRL_MASK;
	}

	public int getMenuShortcutKeyMaskEx() {
		return TInputEvent.CTRL_DOWN_MASK;
	}

	// createCustomCursor

	public TDimension getBestCursorSize(int preferredWidth, int preferredHeight) {
		return new TDimension(preferredWidth, preferredHeight);
	}

	public TInsets getScreenInsets(TGraphicsConfiguration gc) {
		if (this != TToolkit.getDefaultToolkit()) {
			return TToolkit.getDefaultToolkit().getScreenInsets(gc);
		} else {
			return new TInsets(0, 0, 0, 0);
		}
	}

	public static TEventQueue getEventQueue() {
		return getDefaultToolkit().getSystemEventQueue();
	}

	public static String getProperty(String key, String defaultValue) {
		return defaultValue;
	}

	public static TToolkit getDefaultToolkit() {
		return INSTANCE;
	}

}
