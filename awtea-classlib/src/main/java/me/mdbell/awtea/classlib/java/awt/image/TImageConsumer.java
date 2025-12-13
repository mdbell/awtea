package me.mdbell.awtea.classlib.java.awt.image;

import java.util.Hashtable;

/**
 * @see java.awt.image.ImageConsumer
 */
public interface TImageConsumer {

    void setProperties(Hashtable<?, ?> props);

    void setDimensions(int width, int height);

    void setColorModel(TColorModel model);

    void setHints(int hintflags);

    int RANDOMPIXELORDER = 1;

    int TOPDOWNLEFTRIGHT = 2;

    int COMPLETESCANLINES = 4;

    int SINGLEPASS = 8;

    int SINGLEFRAME = 16;

    void setPixels(int x, int y, int w, int h,
				   TColorModel model, byte[] pixels, int off, int scansize);

    void setPixels(int x, int y, int w, int h,
				   TColorModel model, int[] pixels, int off, int scansize);


    void imageComplete(int status);

    int IMAGEERROR = 1;

    int SINGLEFRAMEDONE = 2;

    int STATICIMAGEDONE = 3;

    int IMAGEABORTED = 4;

}
