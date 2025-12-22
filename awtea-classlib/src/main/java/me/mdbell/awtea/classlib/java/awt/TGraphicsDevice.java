package me.mdbell.awtea.classlib.java.awt;

/**
 * @see java.awt.GraphicsDevice
 */
public abstract class TGraphicsDevice {

    public static final int TYPE_RASTER_SCREEN = 0;
    public static final int TYPE_PRINTER = 1;
    public static final int TYPE_IMAGE_BUFFER = 2;

    protected TGraphicsDevice() {

    }

    public abstract int getType();

    public abstract String getIDstring();

    public abstract TGraphicsConfiguration[] getConfigurations();

    public abstract TGraphicsConfiguration getDefaultConfiguration();

    public boolean isDisplayChangeSupported() {
        return false;
    }

    public boolean isFullScreenSupported() {
        return false;
    }

//    public boolean isWindowTranslucencySupported(WindowTranslucency translucency) {
//        return false;
//    }

    public int getAvailableAcceleratedMemory() {
        return 0;
    }

    public TDisplayMode getDisplayMode() {
        return null;
    }

    public TDisplayMode[] getDisplayModes() {
        return new TDisplayMode[0];
    }

    public TWindow getFullScreenWindow() {
        return null;
    }

    public void setFullScreenWindow(TWindow w) {
        throw new UnsupportedOperationException("Full-screen mode not supported");
    }

    public void setDisplayMode(TDisplayMode dm) {
        throw new UnsupportedOperationException("Display mode change not supported");
    }

}
