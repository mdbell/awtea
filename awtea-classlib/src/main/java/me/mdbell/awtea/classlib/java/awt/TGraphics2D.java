package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;

import java.awt.*;
import java.text.AttributedCharacterIterator;

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

	public abstract void clip(TRectangle r);

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

}
