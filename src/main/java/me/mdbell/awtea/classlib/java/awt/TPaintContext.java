package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.image.TColorModel;
import me.mdbell.awtea.classlib.java.awt.image.TRaster;

/**
 * @see java.awt.PaintContext
 */
public interface TPaintContext {

	TColorModel getColorModel();

	TRaster getRaster(int x, int y, int w, int h);

	void dispose();
}
