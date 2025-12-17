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
		// Desktop properties in browser environment
		// Common properties that can be queried: awt.font.desktophints, DnD.gestureMotionThreshold, etc.
		// For now, return null as there are no specific desktop properties in web context
		// Subclasses can override if needed
		return null;
	}

	public int getMaximumCursorColors() {
		// Browser environments typically support full-color cursors
		// Return 2^24 (16 million colors) to indicate true color support
		return 0x1000000;
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
		// Delegate to system properties for consistency
		return System.getProperty(key, defaultValue);
	}

	public static TToolkit getDefaultToolkit() {
		return INSTANCE;
	}

}
