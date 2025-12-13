package me.mdbell.awtea.classlib.java.awt;

import lombok.ToString;
import me.mdbell.awtea.classlib.java.awt.image.TImageObserver;

import java.awt.*;

/**
 * @see java.awt.Graphics
 */
@ToString
public abstract class TGraphics {

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

}
