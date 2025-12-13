package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;

import java.awt.*;
import java.util.Locale;

public abstract class TGraphicsEnvironment {

    protected TGraphicsEnvironment() {

    }

    public abstract TFont[] getAllFonts();

    public abstract TGraphics2D createGraphics(TBufferedImage image);

    public abstract TGraphicsDevice getDefaultScreenDevice();

    public abstract TGraphicsDevice[] getScreenDevices();

    public abstract String[] getAvailableFontFamilyNames();

    public abstract String[] getAvailableFontFamilyNames(Locale locale);

    public boolean isHeadlessInstance() {
        return true;
    }

    public boolean registerFont(TFont font) {
        return false;
    }

    public Point getCenterPoint() {
        TGraphicsDevice device = getDefaultScreenDevice();
        TRectangle bounds = device.getDefaultConfiguration().getBounds();
        return new Point(bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
    }

    public TRectangle getMaximumWindowBounds() {
        TGraphicsDevice device = getDefaultScreenDevice();
        TRectangle bounds = device.getDefaultConfiguration().getBounds();
        TInsets insets = TToolkit.getDefaultToolkit().getScreenInsets(device.getDefaultConfiguration());
        return new TRectangle(bounds.x + insets.left,
                bounds.y + insets.top,
                bounds.width - insets.left - insets.right,
                bounds.height - insets.top - insets.bottom);
    }

    public void preferLocaleFonts() {
        // No-op
    }

    public void preferProportionalFonts() {
        // No-op
    }

    public static boolean isHeadless() {
        TGraphicsEnvironment env = getLocalGraphicsEnvironment();
        if (env != null) {
            return env.isHeadlessInstance();
        }
        return true;
    }

    public static TGraphicsEnvironment getLocalGraphicsEnvironment() {
        return null;
    }
}
