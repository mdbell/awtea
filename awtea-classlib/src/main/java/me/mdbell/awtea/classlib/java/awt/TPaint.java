package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;
import me.mdbell.awtea.classlib.java.awt.geom.TRectangle2D;
import me.mdbell.awtea.classlib.java.awt.image.TColorModel;

/**
 * @see java.awt.Paint
 */
public interface TPaint extends TTransparency{

	public TPaintContext createContext(TColorModel cm, TRectangle deviceBounds,
									   TRectangle2D userBounds, TAffineTransform xform, TRenderingHints hints);
}
