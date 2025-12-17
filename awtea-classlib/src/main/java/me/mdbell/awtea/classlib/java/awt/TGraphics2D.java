package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.font.TFontRenderContext;
import me.mdbell.awtea.classlib.java.awt.font.TGlyphVector;
import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImageOp;
import me.mdbell.awtea.classlib.java.awt.image.TImageObserver;
import me.mdbell.awtea.classlib.java.awt.image.TRenderedImage;
import me.mdbell.awtea.classlib.java.awt.image.renderable.TRenderableImage;

import java.awt.*;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 * @see java.awt.Graphics2D
 */
public abstract class TGraphics2D extends TGraphics {

	protected TGraphics2D() {

	}

	public abstract boolean hit(TRectangle rect, TShape s, boolean onStroke);

	public abstract void draw(TShape s);

	public abstract void fill(TShape s);

	public abstract void translate(int x, int y);

	public abstract void translate(double tx, double ty);

	public abstract void rotate(double theta);

	public abstract void rotate(double theta, double x, double y);

	public abstract void scale(double sx, double sy);

	public abstract void shear(double shx, double shy);

	public abstract void transform(TAffineTransform Tx);

	public abstract void setTransform(TAffineTransform Tx);

	public abstract TAffineTransform getTransform();

	public abstract Color getBackground();

	public abstract TFont getFont();

	public abstract TFontMetrics getFontMetrics(TFont f);

	public abstract TPaint getPaint();

	public abstract TRectangle getClipBounds();

	public abstract TRenderingHints getRenderingHints();

	public abstract TComposite getComposite();

	public abstract void setComposite(TComposite comp);

//	public abstract TStroke getStroke();

	public abstract void clearRect(int x, int y, int width, int height);

	/**
	 * Intersects the current clip with the specified rectangle.
	 * 
	 * @param r the rectangle to intersect the clip with
	 * @see java.awt.Graphics2D#clip(java.awt.Rectangle)
	 */
	public void clip(TRectangle r) {
		if (r == null) {
			return;
		}
		clipRect(r.x, r.y, r.width, r.height);
	}

	public abstract void copyArea(int x, int y, int width, int height, int dx, int dy);

	public abstract void dispose();

	public abstract void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle);

	// public abstract void drawGlyphVector(TGlyphVector g, float x, float y);

	// public abstract void drawImage(java.awt.image.BufferedImage, java.awt.image.BufferedImageOp, int, int)

	public abstract void drawLine(int x1, int y1, int x2, int y2);

	public abstract void drawOval(int x, int y, int width, int height);

	public abstract void drawPolygon(int[] xPoints, int[] yPoints, int nPoints);

	public abstract void drawPolyline(int[] xPoints, int[] yPoints, int nPoints);

	// public abstract void drawRenderableImage(java.awt.image.renderable.RenderableImage, TAffineTransform)

	// public abstract void drawRenderedImage(java.awt.image.RenderedImage, TAffineTransform)

	public abstract void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight);

	public abstract void drawString(String str, int x, int y);

	public abstract void drawString(AttributedCharacterIterator iterator, float x, float y);

	public abstract void drawString(AttributedCharacterIterator iterator, int x, int y);

	public abstract void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle);

	public abstract void fillOval(int x, int y, int width, int height);

	public abstract void fillPolygon(int[] xPoints, int[] yPoints, int nPoints);

	public abstract void fillRoundRect(int x, int y, int width, int height);

	public abstract void setBackground(Color color);

	public abstract void setClip(int x, int y, int width, int height);

	public abstract void setColor(Color c);

	public abstract void setFont(TFont font);

	public abstract void setPaint(TPaint paint);

	public abstract void setPaintMode();

	// public abstract void setRenderingHint(TRenderingHints.Key hintKey, Object hintValue);

	// public abstract void setRenderingHints(Map<?, ?> hints);

	// public abstract void setStroke(TStroke s);

	public abstract void setXORMode(Color c1);

	public boolean hitClip(TRectangle rect) {
		TShape clip = getClip();
		if (clip == null) {
			return true;
		}
		return hit(rect, clip, false);
	}

	public TRectangle getClipBounds(TRectangle rv) {
		TRectangle cb = getClipBounds();
		if (cb == null) {
			return null;
		}
		rv.setRect(cb);
		return rv;
	}

	public TRectangle getClipRect() {
		return getClipBounds();
	}

	public void draw3DRect(int x, int y, int width, int height, boolean raised) {
		Color oldColor = getColor();
		Color brighter = oldColor.brighter();
		Color darker = oldColor.darker();
		if (raised) {
			setColor(brighter);
			drawLine(x, y, x + width - 1, y);
			drawLine(x, y, x, y + height - 1);
			setColor(darker);
			drawLine(x + width - 1, y, x + width - 1, y + height - 1);
			drawLine(x, y + height - 1, x + width - 1, y + height - 1);
		} else {
			setColor(darker);
			drawLine(x, y, x + width - 1, y);
			drawLine(x, y, x, y + height - 1);
			setColor(brighter);
			drawLine(x + width - 1, y, x + width - 1, y + height - 1);
			drawLine(x, y + height - 1, x + width - 1, y + height - 1);
		}
		setColor(oldColor);
	}

	public void drawBytes(byte[] bytes, int offset, int length, int x, int y) {
		char[] chars = new char[length];
		for (int i = 0; i < length; i++) {
			chars[i] = (char) (bytes[offset + i] & 0xFF);
		}
		drawString(new String(chars), x, y);
	}

	public void drawChars(char[] chars, int offset, int length, int x, int y) {
		drawString(new String(chars, offset, length), x, y);
	}

//	public void drawPolygon(TPolygon p) {
//		drawPolygon(p.xpoints, p.ypoints, p.npoints);
//	}

	public void fill3DRect(int x, int y, int width, int height, boolean raised) {
		Color oldColor = getColor();
		Color brighter = oldColor.brighter();
		Color darker = oldColor.darker();
		if (raised) {
			setColor(brighter);
			fillRect(x, y, width - 1, 1);
			fillRect(x, y, 1, height - 1);
			setColor(darker);
			fillRect(x + width - 1, y, 1, height);
			fillRect(x, y + height - 1, width, 1);
		} else {
			setColor(darker);
			fillRect(x, y, width - 1, 1);
			fillRect(x, y, 1, height - 1);
			setColor(brighter);
			fillRect(x + width - 1, y, 1, height);
			fillRect(x, y + height - 1, width, 1);
		}
		setColor(oldColor);
	}

//	public void fillPolygon(TPolygon p) {
//		fillPolygon(p.xpoints, p.ypoints, p.npoints);
//	}

	/**
	 * Draws a polygon defined by a Polygon object.
	 * 
	 * @param p the polygon to draw
	 * @see java.awt.Graphics2D#drawPolygon(java.awt.Polygon)
	 */
	public void drawPolygon(TPolygon p) {
		drawPolygon(p.xpoints, p.ypoints, p.npoints);
	}

	/**
	 * Fills a polygon defined by a Polygon object.
	 * 
	 * @param p the polygon to fill
	 * @see java.awt.Graphics2D#fillPolygon(java.awt.Polygon)
	 */
	public void fillPolygon(TPolygon p) {
		fillPolygon(p.xpoints, p.ypoints, p.npoints);
	}

	/**
	 * Intersects the current Clip with the interior of the specified Shape.
	 * 
	 * @param s the Shape to be intersected with the current Clip
	 * @see java.awt.Graphics2D#clip(java.awt.Shape)
	 */
	public void clip(TShape s) {
		// TODO: Implement shape-based clipping
		// @see https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics2D.html#clip-java.awt.Shape-
		throw new UnsupportedOperationException("TGraphics2D.clip(Shape) not yet implemented");
	}

	/**
	 * Renders the text specified by the specified GlyphVector at the specified coordinates.
	 * 
	 * @param g the GlyphVector to be rendered
	 * @param x the x position where the glyphs should be rendered
	 * @param y the y position where the glyphs should be rendered
	 * @see java.awt.Graphics2D#drawGlyphVector(java.awt.font.GlyphVector, float, float)
	 */
	public void drawGlyphVector(TGlyphVector g, float x, float y) {
		// TODO: Implement glyph vector rendering
		// @see https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics2D.html#drawGlyphVector-java.awt.font.GlyphVector-float-float-
		throw new UnsupportedOperationException("TGraphics2D.drawGlyphVector() not yet implemented");
	}

	/**
	 * Renders a BufferedImage that is filtered with a BufferedImageOp.
	 * 
	 * @param img the specified BufferedImage to be rendered
	 * @param op the filter to be applied to the image before rendering
	 * @param x the x coordinate of the location in user space where the image is rendered
	 * @param y the y coordinate of the location in user space where the image is rendered
	 * @see java.awt.Graphics2D#drawImage(java.awt.image.BufferedImage, java.awt.image.BufferedImageOp, int, int)
	 */
	public void drawImage(TBufferedImage img, TBufferedImageOp op, int x, int y) {
		// TODO: Implement BufferedImage with op rendering
		// @see https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics2D.html#drawImage-java.awt.image.BufferedImage-java.awt.image.BufferedImageOp-int-int-
		throw new UnsupportedOperationException("TGraphics2D.drawImage(BufferedImage, BufferedImageOp) not yet implemented");
	}

	/**
	 * Renders an image, applying a transform from image space into user space before drawing.
	 * 
	 * @param img the specified image to be rendered
	 * @param xform the transformation from image space into user space
	 * @param obs the ImageObserver to be notified as more of the Image is converted
	 * @return true if the image is fully loaded and was completely rendered; false if the image is not fully loaded
	 * @see java.awt.Graphics2D#drawImage(java.awt.Image, java.awt.geom.AffineTransform, java.awt.image.ImageObserver)
	 */
	public boolean drawImage(TImage img, TAffineTransform xform, TImageObserver obs) {
		// TODO: Implement image with transform rendering
		// @see https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics2D.html#drawImage-java.awt.Image-java.awt.geom.AffineTransform-java.awt.image.ImageObserver-
		throw new UnsupportedOperationException("TGraphics2D.drawImage(Image, AffineTransform) not yet implemented");
	}

	/**
	 * Renders a RenderableImage, applying a transform from image space into user space before drawing.
	 * 
	 * @param img the image to be rendered
	 * @param xform the transformation from image space into user space
	 * @see java.awt.Graphics2D#drawRenderableImage(java.awt.image.renderable.RenderableImage, java.awt.geom.AffineTransform)
	 */
	public void drawRenderableImage(TRenderableImage img, TAffineTransform xform) {
		// TODO: Implement RenderableImage rendering
		// @see https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics2D.html#drawRenderableImage-java.awt.image.renderable.RenderableImage-java.awt.geom.AffineTransform-
		throw new UnsupportedOperationException("TGraphics2D.drawRenderableImage() not yet implemented");
	}

	/**
	 * Renders a RenderedImage, applying a transform from image space into user space before drawing.
	 * 
	 * @param img the image to be rendered
	 * @param xform the transformation from image space into user space
	 * @see java.awt.Graphics2D#drawRenderedImage(java.awt.image.RenderedImage, java.awt.geom.AffineTransform)
	 */
	public void drawRenderedImage(TRenderedImage img, TAffineTransform xform) {
		// TODO: Implement RenderedImage rendering
		// @see https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics2D.html#drawRenderedImage-java.awt.image.RenderedImage-java.awt.geom.AffineTransform-
		throw new UnsupportedOperationException("TGraphics2D.drawRenderedImage() not yet implemented");
	}

	/**
	 * Renders the text of the specified String, using the current text attribute state in the Graphics2D context.
	 * 
	 * @param str the String to be rendered
	 * @param x the x coordinate of the location where the String should be rendered
	 * @param y the y coordinate of the location where the String should be rendered
	 * @see java.awt.Graphics2D#drawString(java.lang.String, float, float)
	 */
	public void drawString(String str, float x, float y) {
		drawString(str, (int) x, (int) y);
	}

	/**
	 * Returns the device configuration associated with this Graphics2D.
	 * 
	 * @return the device configuration of this Graphics2D
	 * @see java.awt.Graphics2D#getDeviceConfiguration()
	 */
	public TGraphicsConfiguration getDeviceConfiguration() {
		// TODO: Implement device configuration retrieval
		// @see https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics2D.html#getDeviceConfiguration--
		throw new UnsupportedOperationException("TGraphics2D.getDeviceConfiguration() not yet implemented");
	}

	/**
	 * Gets the font render context from the Graphics2D context.
	 * 
	 * @return the FontRenderContext of this Graphics2D
	 * @see java.awt.Graphics2D#getFontRenderContext()
	 */
	public TFontRenderContext getFontRenderContext() {
		// TODO: Implement font render context retrieval
		// @see https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics2D.html#getFontRenderContext--
		throw new UnsupportedOperationException("TGraphics2D.getFontRenderContext() not yet implemented");
	}

	/**
	 * Returns the value of a single preference for the rendering algorithms.
	 * 
	 * @param hintKey the key corresponding to the hint to get
	 * @return an object representing the value for the specified hint key
	 * @see java.awt.Graphics2D#getRenderingHint(java.awt.RenderingHints.Key)
	 */
	public Object getRenderingHint(TRenderingHints.Key hintKey) {
		TRenderingHints hints = getRenderingHints();
		if (hints == null) {
			return null;
		}
		return hints.get(hintKey);
	}

	/**
	 * Returns the current Stroke in the Graphics2D context.
	 * 
	 * @return the current Graphics2D Stroke
	 * @see java.awt.Graphics2D#getStroke()
	 */
	public TStroke getStroke() {
		// TODO: Implement stroke retrieval
		// @see https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics2D.html#getStroke--
		throw new UnsupportedOperationException("TGraphics2D.getStroke() not yet implemented");
	}

	/**
	 * Sets a rendering hint preference for the rendering algorithms.
	 * 
	 * @param hintKey the key of the hint to be set
	 * @param hintValue the value indicating preferences for the specified hint category
	 * @see java.awt.Graphics2D#setRenderingHint(java.awt.RenderingHints.Key, java.lang.Object)
	 */
	public void setRenderingHint(TRenderingHints.Key hintKey, Object hintValue) {
		// TODO: Implement rendering hint setting
		// @see https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics2D.html#setRenderingHint-java.awt.RenderingHints.Key-java.lang.Object-
		throw new UnsupportedOperationException("TGraphics2D.setRenderingHint() not yet implemented");
	}

	/**
	 * Replaces the values of all preferences for the rendering algorithms with the specified hints.
	 * 
	 * @param hints the rendering hints to be set
	 * @see java.awt.Graphics2D#setRenderingHints(java.util.Map)
	 */
	public void setRenderingHints(Map<?, ?> hints) {
		// TODO: Implement rendering hints setting
		// @see https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics2D.html#setRenderingHints-java.util.Map-
		throw new UnsupportedOperationException("TGraphics2D.setRenderingHints() not yet implemented");
	}

	/**
	 * Adds a number of preferences for the rendering algorithms.
	 * 
	 * @param hints the rendering hints to be added
	 * @see java.awt.Graphics2D#addRenderingHints(java.util.Map)
	 */
	public void addRenderingHints(Map<?, ?> hints) {
		// TODO: Implement rendering hints addition
		// @see https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics2D.html#addRenderingHints-java.util.Map-
		throw new UnsupportedOperationException("TGraphics2D.addRenderingHints() not yet implemented");
	}

	/**
	 * Sets the Stroke for the Graphics2D context.
	 * 
	 * @param s the Stroke object to be used to stroke a Shape during the rendering process
	 * @see java.awt.Graphics2D#setStroke(java.awt.Stroke)
	 */
	public void setStroke(TStroke s) {
		// TODO: Implement stroke setting
		// @see https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics2D.html#setStroke-java.awt.Stroke-
		throw new UnsupportedOperationException("TGraphics2D.setStroke() not yet implemented");
	}

}
