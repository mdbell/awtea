package me.mdbell.awtea.classlib.java.awt;

import lombok.ToString;
import me.mdbell.awtea.classlib.java.awt.image.TImageObserver;

import java.awt.*;

/**
 * @see java.awt.Graphics
 */
@ToString
public abstract class TGraphics implements AutoCloseable{

    protected TGraphics() {

    }

    public abstract TGraphics create();

    public TGraphics create(int x, int y, int width, int height) {
        TGraphics g = create();
        if (g == null) {
            return null;
        }
        g.clipRect(x, y, width, height);
        return g;
    }

    public abstract void setClip(int x, int y, int width, int height);

    public abstract void setXORMode(Color c1);

    public abstract void setPaintMode();

    public abstract void translate(int deltaX, int deltaY);

    public abstract TFont getFont();

    public abstract void setFont(TFont font);

    public abstract Color getColor();

    public abstract void setColor(Color c);

    public TFontMetrics getFontMetrics() {
        return getFontMetrics(getFont());
    }

    public abstract TFontMetrics getFontMetrics(TFont f);

    public abstract TRectangle getClipBounds();

    public abstract void drawString(String str, int x, int y);

    public abstract void drawRect(int x, int y, int width, int height);

    public abstract void fillRect(int x, int y, int width, int height);

    public abstract void clearRect(int x, int y, int width, int height);

    public abstract void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight);

    public abstract void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight);

    public abstract void drawPolygon(int[] xPoints, int[] yPoints, int nPoints);

    public abstract void fillPolygon(int[] xPoints, int[] yPoints, int nPoints);

    public abstract void drawLine(int x1, int y1, int x2, int y2);

    public abstract void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle);

    public abstract boolean drawImage(TImage img, int x, int y, int width, int height, TImageObserver observer);

    public abstract boolean drawImage(TImage img, int x, int y, TImageObserver observer);

    public abstract TFontMetrics measureText(TFont font);

    public void resetTranslate(int x, int y) {
        translate(-x, -y);
    }

    public abstract void reset();

    public abstract TShape getClip();

    public abstract void clipRect(int x, int y, int width, int height);

    public abstract void setClip(TShape clip);

    public void dispose() {

    }



    /**
     * Copies an area of the component by a distance specified by dx and dy.
     * 
     * @param x the x coordinate of the source rectangle
     * @param y the y coordinate of the source rectangle
     * @param width the width of the source rectangle
     * @param height the height of the source rectangle
     * @param dx the horizontal distance to copy the pixels
     * @param dy the vertical distance to copy the pixels
     * @see java.awt.Graphics#copyArea(int, int, int, int, int, int)
     */
    public abstract void copyArea(int x, int y, int width, int height, int dx, int dy);

    /**
     * Draws a 3-D highlighted outline of the specified rectangle.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     * @param raised a boolean that determines whether the rectangle appears to be raised or etched
     * @see java.awt.Graphics#draw3DRect(int, int, int, int, boolean)
     */
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

    /**
     * Draws as much of the specified image as has already been scaled to fit inside the specified rectangle.
     * 
     * @param img the image to be drawn
     * @param x the x coordinate
     * @param y the y coordinate
     * @param bgcolor the background color
     * @param observer object to be notified as more of the image is converted
     * @return true if the image is fully loaded; false otherwise
     * @see java.awt.Graphics#drawImage(java.awt.Image, int, int, java.awt.Color, java.awt.image.ImageObserver)
     */
    public boolean drawImage(TImage img, int x, int y, Color bgcolor, TImageObserver observer) {
        // TODO: Implement background color support
        // @see https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics.html#drawImage-java.awt.Image-int-int-java.awt.Color-java.awt.image.ImageObserver-
        return drawImage(img, x, y, observer);
    }

    /**
     * Draws as much of the specified image as has already been scaled to fit inside the specified rectangle.
     * 
     * @param img the image to be drawn
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     * @param bgcolor the background color
     * @param observer object to be notified as more of the image is converted
     * @return true if the image is fully loaded; false otherwise
     * @see java.awt.Graphics#drawImage(java.awt.Image, int, int, int, int, java.awt.Color, java.awt.image.ImageObserver)
     */
    public boolean drawImage(TImage img, int x, int y, int width, int height, Color bgcolor, TImageObserver observer) {
        // TODO: Implement background color support
        // @see https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics.html#drawImage-java.awt.Image-int-int-int-int-java.awt.Color-java.awt.image.ImageObserver-
        return drawImage(img, x, y, width, height, observer);
    }

    /**
     * Draws as much of the specified area of the specified image as is currently available.
     * 
     * @param img the image to be drawn
     * @param dx1 the x coordinate of the first corner of the destination rectangle
     * @param dy1 the y coordinate of the first corner of the destination rectangle
     * @param dx2 the x coordinate of the second corner of the destination rectangle
     * @param dy2 the y coordinate of the second corner of the destination rectangle
     * @param sx1 the x coordinate of the first corner of the source rectangle
     * @param sy1 the y coordinate of the first corner of the source rectangle
     * @param sx2 the x coordinate of the second corner of the source rectangle
     * @param sy2 the y coordinate of the second corner of the source rectangle
     * @param observer object to be notified as more of the image is converted
     * @return true if the image is fully loaded; false otherwise
     * @see java.awt.Graphics#drawImage(java.awt.Image, int, int, int, int, int, int, int, int, java.awt.image.ImageObserver)
     */
    public boolean drawImage(TImage img, int dx1, int dy1, int dx2, int dy2,
                            int sx1, int sy1, int sx2, int sy2,
                            TImageObserver observer) {
        // TODO: Implement region-based image drawing
        // @see https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics.html#drawImage-java.awt.Image-int-int-int-int-int-int-int-int-java.awt.image.ImageObserver-
        throw new UnsupportedOperationException("TGraphics.drawImage(region) not yet implemented");
    }

    /**
     * Draws as much of the specified area of the specified image as is currently available.
     * 
     * @param img the image to be drawn
     * @param dx1 the x coordinate of the first corner of the destination rectangle
     * @param dy1 the y coordinate of the first corner of the destination rectangle
     * @param dx2 the x coordinate of the second corner of the destination rectangle
     * @param dy2 the y coordinate of the second corner of the destination rectangle
     * @param sx1 the x coordinate of the first corner of the source rectangle
     * @param sy1 the y coordinate of the first corner of the source rectangle
     * @param sx2 the x coordinate of the second corner of the source rectangle
     * @param sy2 the y coordinate of the second corner of the source rectangle
     * @param bgcolor the background color
     * @param observer object to be notified as more of the image is converted
     * @return true if the image is fully loaded; false otherwise
     * @see java.awt.Graphics#drawImage(java.awt.Image, int, int, int, int, int, int, int, int, java.awt.Color, java.awt.image.ImageObserver)
     */
    public boolean drawImage(TImage img, int dx1, int dy1, int dx2, int dy2,
                            int sx1, int sy1, int sx2, int sy2,
                            Color bgcolor, TImageObserver observer) {
        // TODO: Implement region-based image drawing with background color
        // @see https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics.html#drawImage-java.awt.Image-int-int-int-int-int-int-int-int-java.awt.Color-java.awt.image.ImageObserver-
        return drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }

    /**
     * Draws an oval bounded by the specified rectangle.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width of the oval
     * @param height the height of the oval
     * @see java.awt.Graphics#drawOval(int, int, int, int)
     */
    public abstract void drawOval(int x, int y, int width, int height);

    /**
     * Draws a closed polygon defined by arrays of x and y coordinates.
     * 
     * @param p a polygon
     * @see java.awt.Graphics#drawPolygon(java.awt.Polygon)
     */
    public void drawPolygon(TPolygon p) {
        drawPolygon(p.xpoints, p.ypoints, p.npoints);
    }

    /**
     * Draws a sequence of connected lines defined by arrays of x and y coordinates.
     * 
     * @param xPoints an array of x coordinates
     * @param yPoints an array of y coordinates
     * @param nPoints the total number of points
     * @see java.awt.Graphics#drawPolyline(int[], int[], int)
     */
    public abstract void drawPolyline(int[] xPoints, int[] yPoints, int nPoints);

    /**
     * Draws the text given by the specified byte array.
     * 
     * @param data the data to be drawn
     * @param offset the start offset in the data
     * @param length the number of bytes that are drawn
     * @param x the x coordinate
     * @param y the y coordinate
     * @see java.awt.Graphics#drawBytes(byte[], int, int, int, int)
     */
    public void drawBytes(byte[] data, int offset, int length, int x, int y) {
        char[] chars = new char[length];
        for (int i = 0; i < length; i++) {
            chars[i] = (char) (data[offset + i] & 0xFF);
        }
        drawString(new String(chars), x, y);
    }

    /**
     * Draws the text given by the specified character array.
     * 
     * @param data the array of characters to be drawn
     * @param offset the start offset in the data
     * @param length the number of characters to be drawn
     * @param x the x coordinate
     * @param y the y coordinate
     * @see java.awt.Graphics#drawChars(char[], int, int, int, int)
     */
    public void drawChars(char[] data, int offset, int length, int x, int y) {
        drawString(new String(data, offset, length), x, y);
    }

    /**
     * Paints a 3-D highlighted rectangle filled with the current color.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     * @param raised a boolean that determines whether the rectangle appears to be raised or etched
     * @see java.awt.Graphics#fill3DRect(int, int, int, int, boolean)
     */
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

    /**
     * Fills an arc bounded by the specified rectangle.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width of the arc
     * @param height the height of the arc
     * @param startAngle the beginning angle
     * @param arcAngle the angular extent of the arc
     * @see java.awt.Graphics#fillArc(int, int, int, int, int, int)
     */
    public abstract void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle);

    /**
     * Fills an oval bounded by the specified rectangle.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width of the oval
     * @param height the height of the oval
     * @see java.awt.Graphics#fillOval(int, int, int, int)
     */
    public abstract void fillOval(int x, int y, int width, int height);

    /**
     * Fills a closed polygon defined by a Polygon object.
     * 
     * @param p the polygon to fill
     * @see java.awt.Graphics#fillPolygon(java.awt.Polygon)
     */
    public void fillPolygon(TPolygon p) {
        fillPolygon(p.xpoints, p.ypoints, p.npoints);
    }

    /**
     * Returns the bounding rectangle of the current clipping area.
     * 
     * @param r the rectangle to be filled with the current clip bounds
     * @return the bounding rectangle of the current clipping area
     * @see java.awt.Graphics#getClipBounds(java.awt.Rectangle)
     */
    public TRectangle getClipBounds(TRectangle r) {
        TRectangle bounds = getClipBounds();
        if (bounds == null) {
            return null;
        }
        if (r == null) {
            return new TRectangle(bounds.x, bounds.y, bounds.width, bounds.height);
        }
        r.setRect(bounds);
        return r;
    }

    /**
     * Returns the bounding rectangle of the current clipping area (deprecated method name).
     * 
     * @return the bounding rectangle of the current clipping area
     * @deprecated As of JDK version 1.1, replaced by getClipBounds()
     * @see java.awt.Graphics#getClipRect()
     */
    @Deprecated
    public TRectangle getClipRect() {
        return getClipBounds();
    }

    /**
     * Returns true if the specified rectangular area might intersect the current clipping area.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width of the area
     * @param height the height of the area
     * @return true if the given rectangle intersects the clip; false otherwise
     * @see java.awt.Graphics#hitClip(int, int, int, int)
     */
    public boolean hitClip(int x, int y, int width, int height) {
        TRectangle clipBounds = getClipBounds();
        if (clipBounds == null) {
            return true;
        }
        return clipBounds.intersects(x, y, width, height);
    }

    @Override
    public void close() throws Exception {
        dispose();
    }

    /**
     * Disposes of this graphics context once it is no longer referenced.
     * 
     * @see java.awt.Graphics#finalize()
     */
    @Override
    @Deprecated
    protected void finalize() throws Throwable {
        try {
            dispose();
        } finally {
            super.finalize();
        }
    }
}
