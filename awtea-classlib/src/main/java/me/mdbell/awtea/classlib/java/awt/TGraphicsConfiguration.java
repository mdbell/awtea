package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import me.mdbell.awtea.classlib.java.awt.image.TColorModel;
import me.mdbell.awtea.classlib.java.awt.image.TWritableRaster;

/**
 * @see java.awt.GraphicsConfiguration
 */
public abstract class TGraphicsConfiguration {

    protected TGraphicsConfiguration() {

    }

    public abstract TGraphicsDevice getDevice();

    public abstract TRectangle getBounds();

    public abstract TAffineTransform getDefaultTransform();

    public abstract TAffineTransform getNormalizingTransform();

    public abstract TColorModel getColorModel();

    public abstract TColorModel getColorModel(int transparency);

    public boolean isTranslucencyCapable() {
        // By default, assume not capable
        return false;
    }

    public TBufferedImage createCompatibleImage(int width, int height) {
        TColorModel model = getColorModel();
        TWritableRaster raster =
                model.createCompatibleWritableRaster(width, height);
        return new TBufferedImage(model, raster,
                model.isAlphaPremultiplied(), null);
    }

    public TBufferedImage createCompatibleImage(int width, int height,
                                                int transparency) {
        if (getColorModel().getTransparency() == transparency) {
            return createCompatibleImage(width, height);
        }

        TColorModel cm = getColorModel(transparency);
        if (cm == null) {
            throw new IllegalArgumentException("Unknown transparency: " +
                    transparency);
        }
        TWritableRaster wr = cm.createCompatibleWritableRaster(width, height);
        return new TBufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
    }
}
