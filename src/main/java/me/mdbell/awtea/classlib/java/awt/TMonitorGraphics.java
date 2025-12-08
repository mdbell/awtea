package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.image.TImageObserver;
import me.mdbell.awtea.monitor.OperationsMonitor;

import java.awt.*;

public class TMonitorGraphics extends TGraphics {

	private final TGraphics parent;
	private final OperationsMonitor monitor;

	public TMonitorGraphics(TGraphics parent) {
		this.parent = parent;
		Class<?> clazz = parent.getClass();
		this.monitor = OperationsMonitor.get(clazz, clazz.getSimpleName(), false);
	}

	@Override
	public TGraphics create() {
		monitor.onOperationEntered(parent, "create");
		TGraphics g = parent.create();
		monitor.onOperationLeft(parent, "create");
		return g;
	}

	@Override
	public void setClip(int x, int y, int width, int height) {
		monitor.onOperationEntered(parent, "setClip");
		parent.setClip(x, y, width, height);
		monitor.onOperationLeft(parent, "setClip");
	}

	@Override
	public void setXORMode(Color c1) {
		monitor.onOperationEntered(parent, "setXORMode");
		parent.setXORMode(c1);
		monitor.onOperationLeft(parent, "setXORMode");
	}

	@Override
	public void setPaintMode() {
		monitor.onOperationEntered(parent, "setPaintMode");
		parent.setPaintMode();
		monitor.onOperationLeft(parent, "setPaintMode");
	}

	@Override
	public void translate(int deltaX, int deltaY) {
		monitor.onOperationEntered(parent, "translate");
		parent.translate(deltaX, deltaY);
		monitor.onOperationLeft(parent, "translate");
	}

	@Override
	public TFont getFont() {
		monitor.onOperationEntered(parent, "getFont");
		TFont font = parent.getFont();
		monitor.onOperationLeft(parent, "getFont");
		return font;
	}

	@Override
	public void setFont(TFont font) {
		monitor.onOperationEntered(parent, "setFont");
		parent.setFont(font);
		monitor.onOperationLeft(parent, "setFont");
	}

	@Override
	public Color getColor() {
		monitor.onOperationEntered(parent, "getColor");
		Color color = parent.getColor();
		monitor.onOperationLeft(parent, "getColor");
		return color;
	}

	@Override
	public void setColor(Color c) {
		monitor.onOperationEntered(parent, "setColor");
		parent.setColor(c);
		monitor.onOperationLeft(parent, "setColor");
	}

	@Override
	public TFontMetrics getFontMetrics(TFont f) {
		monitor.onOperationEntered(parent, "getFontMetrics");
		TFontMetrics metrics = parent.getFontMetrics(f);
		monitor.onOperationLeft(parent, "getFontMetrics");
		return metrics;
	}

	@Override
	public TRectangle getClipBounds() {
		monitor.onOperationEntered(parent, "getClipBounds");
		TRectangle rect = parent.getClipBounds();
		monitor.onOperationLeft(parent, "getClipBounds");
		return rect;
	}

	@Override
	public void drawString(String str, int x, int y) {
		monitor.onOperationEntered(parent, "drawString");
		parent.drawString(str, x, y);
		monitor.onOperationLeft(parent, "drawString");
	}

	@Override
	public void drawRect(int x, int y, int width, int height) {
		monitor.onOperationEntered(parent, "drawRect");
		parent.drawRect(x, y, width, height);
		monitor.onOperationLeft(parent, "drawRect");
	}

	@Override
	public void fillRect(int x, int y, int width, int height) {
		monitor.onOperationEntered(parent, "fillRect");
		parent.fillRect(x, y, width, height);
		monitor.onOperationLeft(parent, "fillRect");
	}

	@Override
	public void clearRect(int x, int y, int width, int height) {
		monitor.onOperationEntered(parent, "clearRect");
		parent.clearRect(x, y, width, height);
		monitor.onOperationLeft(parent, "clearRect");
	}

	@Override
	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		monitor.onOperationEntered(parent, "drawRoundRect");
		parent.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
		monitor.onOperationLeft(parent, "drawRoundRect");
	}

	@Override
	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		monitor.onOperationEntered(parent, "fillRoundRect");
		parent.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
		monitor.onOperationLeft(parent, "fillRoundRect");
	}

	@Override
	public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
		monitor.onOperationEntered(parent, "drawPolygon");
		parent.drawPolygon(xPoints, yPoints, nPoints);
		monitor.onOperationLeft(parent, "drawPolygon");
	}

	@Override
	public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
		monitor.onOperationEntered(parent, "fillPolygon");
		parent.fillPolygon(xPoints, yPoints, nPoints);
		monitor.onOperationLeft(parent, "fillPolygon");
	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2) {
		monitor.onOperationEntered(parent, "drawLine");
		parent.drawLine(x1, y1, x2, y2);
		monitor.onOperationLeft(parent, "drawLine");
	}

	@Override
	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		monitor.onOperationEntered(parent, "drawArc");
		parent.drawArc(x, y, width, height, startAngle, arcAngle);
		monitor.onOperationLeft(parent, "drawArc");
	}

	@Override
	public boolean drawImage(TImage img, int x, int y, int width, int height, TImageObserver observer) {
		monitor.onOperationEntered(parent, "drawImage");
		boolean result = parent.drawImage(img, x, y, width, height, observer);
		monitor.onOperationLeft(parent, "drawImage");
		return result;
	}

	@Override
	public boolean drawImage(TImage img, int x, int y, TImageObserver observer) {
		monitor.onOperationEntered(parent, "drawImage");
		boolean result = parent.drawImage(img, x, y, observer);
		monitor.onOperationLeft(parent, "drawImage");
		return result;
	}

	@Override
	public TFontMetrics measureText(TFont font) {
		monitor.onOperationEntered(parent, "measureText");
		TFontMetrics metrics = parent.measureText(font);
		monitor.onOperationLeft(parent, "measureText");
		return metrics;
	}

	@Override
	public void reset() {
		monitor.onOperationEntered(parent, "reset");
		parent.reset();
		monitor.onOperationLeft(parent, "reset");
	}

	@Override
	public TShape getClip() {
		monitor.onOperationEntered(parent, "getClip");
		TShape clip = parent.getClip();
		monitor.onOperationLeft(parent, "getClip");
		return clip;
	}

	@Override
	public void clipRect(int x, int y, int width, int height) {
		monitor.onOperationEntered(parent, "clipRect");
		parent.clipRect(x, y, width, height);
		monitor.onOperationLeft(parent, "clipRect");
	}

	@Override
	public void setClip(TShape clip) {
		monitor.onOperationEntered(parent, "setClip");
		parent.setClip(clip);
		monitor.onOperationLeft(parent, "setClip");
	}
}
