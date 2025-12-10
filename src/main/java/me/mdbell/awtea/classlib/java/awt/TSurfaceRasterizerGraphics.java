package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.Setter;
import me.mdbell.awtea.classlib.java.awt.awtea.gfx.TRasterizer;
import me.mdbell.awtea.classlib.java.awt.awtea.gfx.TSurfaceCommand;
import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import me.mdbell.awtea.classlib.java.awt.image.TImageObserver;
import org.teavm.jso.browser.Window;

import java.awt.*;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.List;

public abstract class TSurfaceRasterizerGraphics extends TGraphics2D {

	protected transient boolean scheduled = false;

	private final List<TSurfaceCommand> surfaceCommandsA = new ArrayList<>();
	private final List<TSurfaceCommand> surfaceCommandsB = new ArrayList<>();

	// Which one we are writing to right now
	private transient List<TSurfaceCommand> writeList = surfaceCommandsA;
	// Which one we will read from during the blit
	private transient List<TSurfaceCommand> readList = surfaceCommandsB;

	private transient TSurfaceCommand previous = null;

	protected final TRasterizer rasterizer;

	@Getter
	protected final TAffineTransform transform = new TAffineTransform();

	@Getter
	@Setter
	protected TFont font;

	protected TRectangle clip;

	@Getter
	protected Color color;

	@Getter
	protected Color background;

	public TSurfaceRasterizerGraphics(TRasterizer rasterizer) {
		this.rasterizer = rasterizer;
		reset();
	}

	protected TSurfaceRasterizerGraphics(TSurfaceRasterizerGraphics other) {
		super();
		this.rasterizer = other.rasterizer;
		this.font = other.font;
		this.transform.setTransform(other.transform);
		this.clip = other.clip;
		this.color = other.color;
		this.background = other.background;
	}

	public final void pushOp(TSurfaceCommand op) {
		if (op == null || coalesce(previous, op)) {
			// Coalesced, do not add new op
			return;
		}

		writeList.add(op);
		previous = op;

		if (!scheduled) {
			scheduled = true;
			scheduleRasterize();
		}
	}

	@Override
	public void reset() {
		transform.setToIdentity();
		clip = null;
		color = Color.WHITE;
		background = Color.BLACK;
		font = TFont.getDefaultFont();
		//TODO: clear ops?
	}

	@Override
	public boolean hit(TRectangle rect, TShape s, boolean onStroke) {
		return false;
	}

	@Override
	public void draw(TShape s) {

	}

	@Override
	public void fill(TShape s) {

	}

	@Override
	public void translate(int x, int y) {
		transform.translate(x, y);
		pushTransform();
	}

	@Override
	public void translate(double tx, double ty) {
		transform.translate(tx, ty);
		pushTransform();
	}

	@Override
	public void rotate(double theta) {
		transform.rotate(theta);
		pushTransform();
	}

	@Override
	public void rotate(double theta, double x, double y) {
		transform.rotate(theta, x, y);
		pushTransform();
	}

	@Override
	public void scale(double sx, double sy) {
		transform.scale(sx, sy);
		pushTransform();
	}

	@Override
	public void shear(double shx, double shy) {
		transform.shear(shx, shy);
		pushTransform();
	}

	@Override
	public void transform(TAffineTransform Tx) {
		transform.concatenate(Tx);
		pushTransform();
	}

	@Override
	public void setTransform(TAffineTransform Tx) {
		transform.setTransform(Tx);
		pushTransform();
	}

	private void pushTransform() {
		pushOp(new TSurfaceCommand(TSurfaceCommand.Operation.SET_TRANSFORM, new TAffineTransform(transform)));
	}

	@Override
	public TFontMetrics getFontMetrics(TFont f) {
		return f.getFontMetrics();
	}

	@Override
	public TPaint getPaint() {
		return null;
	}

	@Override
	public TRectangle getClipBounds() {
		return null;
	}

	@Override
	public TRenderingHints getRenderingHints() {
		return null;
	}

	@Override
	public void clearRect(int x, int y, int width, int height) {
		// clearRect does not use rectInternal because it should always push the op
		// and it does not use clipping
		if (width <= 0 || height <= 0) {
			return;
		}
		pushOp(new TSurfaceCommand(TSurfaceCommand.Operation.CLEAR_RECT, x, y, width, height));
	}

	@Override
	public void clip(TRectangle r) {

	}

	@Override
	public void copyArea(int x, int y, int width, int height, int dx, int dy) {

	}

	@Override
	public void dispose() {

	}

	@Override
	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {

	}

	@Override
	public boolean drawImage(TImage img, int x, int y, int width, int height, TImageObserver observer) {
		if (clip != null && !clip.intersects(x, y, width, height)) {
			return false;
		}
		if (img instanceof TBufferedImage) {
			pushOp(new TSurfaceCommand(TSurfaceCommand.Operation.BLIT_IMAGE, img, x, y, width, height));
			return true;
		}
		return false;
	}

	@Override
	public boolean drawImage(TImage img, int x, int y, TImageObserver observer) {
		if (clip != null && !clip.intersects(x, y, img.getWidth(null), img.getHeight(null))) {
			return false;
		}
		if (img instanceof TBufferedImage) {
			pushOp(new TSurfaceCommand(TSurfaceCommand.Operation.BLIT_IMAGE, img, x, y,
				img.getWidth(null), img.getHeight(null)));
			return true;
		}
		return false;
	}

	@Override
	public TFontMetrics measureText(TFont font) {
		return font.getFontMetrics();
	}

	@Override
	public TShape getClip() {
		return clip;
	}

	@Override
	public void clipRect(int x, int y, int width, int height) {
		if (clip == null) {
			setClip(x, y, width, height);
		} else {
			clip = clip.intersection(new TRectangle(x, y, width, height));
			// op gets pushed in setClip, so we only need to push it here
			pushOp(new TSurfaceCommand(TSurfaceCommand.Operation.SET_CLIP_RECT, this.clip));
		}
	}

	@Override
	public void setClip(int x, int y, int width, int height) {
		if (width <= 0 || height <= 0) {
			this.clip = null;
			pushOp(new TSurfaceCommand(TSurfaceCommand.Operation.SET_CLIP_RECT));
			return;
		}
		this.clip = new TRectangle(x, y, width, height);
		pushOp(new TSurfaceCommand(TSurfaceCommand.Operation.SET_CLIP_RECT, this.clip));

	}

	@Override
	public void setClip(TShape clip) {
		if (clip instanceof TRectangle) {
			this.clip = (TRectangle) clip;
		} else if (clip == null) {
			this.clip = null;
		} else {
			// non-rect clips not implemented
			throw new UnsupportedOperationException("Non-rect clip not supported yet");
		}
		pushOp(new TSurfaceCommand(TSurfaceCommand.Operation.SET_CLIP_RECT, this.clip));
	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2) {

	}

	@Override
	public void drawOval(int x, int y, int width, int height) {

	}

	@Override
	public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {

	}

	@Override
	public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {

	}

	@Override
	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {

	}

	@Override
	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
	}

	@Override
	public void drawString(String str, int x, int y) {
		//TODO: get font from native renderer
		//TFontRenderer.getRenderer(this.getFont()).drawString(context, str, x + translateX, y + translateY);
	}

	@Override
	public void drawString(AttributedCharacterIterator iterator, float x, float y) {
		//TODO: get font from native renderer
		//TFontRenderer.getRenderer(this.getFont()).drawString(context, str, x + translateX, y + translateY);
	}

	@Override
	public void drawString(AttributedCharacterIterator iterator, int x, int y) {
		//TODO: get font from native renderer
		//TFontRenderer.getRenderer(this.getFont()).drawString(context, str, x + translateX, y + translateY);
	}

	@Override
	public void drawRect(int x, int y, int width, int height) {
		rectInternal(TSurfaceCommand.Operation.DRAW_RECT, x, y, width, height);
	}

	@Override
	public void fillRect(int x, int y, int width, int height) {
		rectInternal(TSurfaceCommand.Operation.FILL_RECT, x, y, width, height);
	}

	private void rectInternal(TSurfaceCommand.Operation opType, int x, int y, int width, int height) {
		if (width <= 0 || height <= 0 || (clip != null && !clip.intersects(x, y, width, height))) {
			return;
		}
		pushOp(new TSurfaceCommand(opType, x, y, width, height));
	}

	@Override
	public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {

	}

	@Override
	public void fillOval(int x, int y, int width, int height) {

	}

	@Override
	public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {

	}

	@Override
	public void fillRoundRect(int x, int y, int width, int height) {

	}

	@Override
	public void setBackground(Color bg) {
		this.background = bg;
		pushOp(new TSurfaceCommand(TSurfaceCommand.Operation.SET_COLOR, bg, 1));
	}

	@Override
	public void setColor(Color c) {
		if (c == null) {
			c = Color.WHITE;
		}
		this.color = c;
		pushOp(new TSurfaceCommand(TSurfaceCommand.Operation.SET_COLOR, c, 0));
	}

	@Override
	public void setPaint(TPaint paint) {

	}

	@Override
	public void setPaintMode() {

	}

	@Override
	public void setXORMode(Color c1) {

	}

	// Schedule rasterization on the next animation frame

	protected final void scheduleRasterize() {
		Window.requestAnimationFrame(time -> {
			// Swap lists
			List<TSurfaceCommand> temp = readList;
			readList = writeList;
			writeList = temp;

			previous = null;
			writeList.clear();

			rasterizer.rasterizeCommands(readList);

			scheduled = false;
		});
	}

	private boolean coalesce(TSurfaceCommand previous, TSurfaceCommand requested) {
		if (requested.type == TSurfaceCommand.Operation.NO_OP) {
			return true;
		}
		if (previous == null) {
			return false;
		}

		if (previous.type != requested.type) {
			return false;
		}

		switch (previous.type) {
			case BLIT_IMAGE:
				if (previous.obj != requested.obj) {
					return false;
				}
				// Fallthrough to position/size check
			case DRAW_RECT:
			case FILL_RECT:
			case CLEAR_RECT:
				return previous.arg1 == requested.arg1 && previous.arg2 == requested.arg2 &&
					previous.arg3 == requested.arg3 && previous.arg4 == requested.arg4;
			case SET_TRANSFORM:
			case SET_COLOR:
				previous.obj = requested.obj; // Update to the latest object
				return true;
			default:
				return false;
		}
	}
}
