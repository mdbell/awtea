package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.Setter;
import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import me.mdbell.awtea.classlib.java.awt.image.TImageObserver;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLCanvasElement;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class TCanvasGraphics extends TGraphics {

	protected final HTMLCanvasElement canvas;

	@Getter
	protected final TAffineTransform transform = new TAffineTransform();

	protected boolean blitScheduled = false;

	private final List<BlitOp> blitOpsA = new ArrayList<>();
	private final List<BlitOp> blitOpsB = new ArrayList<>();

	// Which one we are writing to right now
	private List<BlitOp> writeList = blitOpsA;
	// Which one we will read from during the blit
	private List<BlitOp> readList = blitOpsB;

	protected TRectangle clip;

	@Getter
	protected Color color = Color.BLACK;

	@Getter
	@Setter
	protected TFont font = TFont.getDefaultFont();

	private BlitOp previousOp = null;

	protected TCanvasGraphics(HTMLCanvasElement canvas) {
		this.canvas = canvas;
	}

	protected TCanvasGraphics(TCanvasGraphics other) {
		this.canvas = other.canvas;
		this.clip = other.clip;
		this.transform.setTransform(other.transform);
		this.color = other.color;
		this.font = other.font;
	}

	@Override
	public void reset() {
		transform.setToIdentity();
		clip = null;
		color = Color.BLACK;
		font = TFont.getDefaultFont();
	}

	public enum Operation {
		BLIT_IMAGE,
		SET_COLOR,
		DRAW_RECT,
		FILL_RECT,
		CLEAR_RECT
	}

	@Override
	public void translate(int tx, int ty) {
		transform.translate(tx, ty);
	}

	public void scale(double sx, double sy) {
		transform.scale(sx, sy);
	}

	public void rotate(double theta) {
		transform.rotate(theta);
	}

	public void rotate(double vecX, double vecY) {
		transform.rotate(vecX, vecY);
	}

	@Override
	public void setClip(int x, int y, int width, int height) {
		if (width <= 0 || height <= 0) {
			this.clip = null;
			return;
		}
		this.clip = new TRectangle(x, y, width, height);
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
	}

	@Override
	public void clipRect(int x, int y, int width, int height) {
		if (clip == null) {
			setClip(x, y, width, height);
		} else {
			clip = clip.intersection(new TRectangle(x, y, width, height));
		}
	}


	@Override
	public TShape getClip() {
		return clip;
	}

	@Override
	public TFontMetrics measureText(TFont font) {
		return font.getFontMetrics();
	}

	@Override
	public void drawString(String str, int x, int y) {
		//TODO: get font from native renderer
		//TFontRenderer.getRenderer(this.getFont()).drawString(context, str, x + translateX, y + translateY);
	}

	@Override
	public void setColor(Color c) {
		if (c == null) {
			throw new IllegalArgumentException("Color cannot be null");
		}
		this.color = c;
		pushOp(new BlitOp(Operation.SET_COLOR, c, 0, 0, 0, 0));
	}

	@Override
	public void drawRect(int x, int y, int width, int height) {
		if (width <= 0 || height <= 0) {
			return;
		}
		if (clip != null && !clip.intersects(x, y, width, height)) {
			return;
		}
		pushOp(new BlitOp(Operation.DRAW_RECT, x, y, width, height));
	}

	@Override
	public void fillRect(int x, int y, int width, int height) {
		if (width <= 0 || height <= 0) {
			return;
		}
		if (clip != null && !clip.intersects(x, y, width, height)) {
			return;
		}
		pushOp(new BlitOp(Operation.FILL_RECT, x, y, width, height));
	}

	@Override
	public void clearRect(int x, int y, int width, int height) {
		if (width <= 0 || height <= 0) {
			return;
		}
		pushOp(new BlitOp(Operation.CLEAR_RECT, x, y, width, height));
	}

	@Override
	public boolean drawImage(TImage img, int x, int y, int width, int height, TImageObserver observer) {
		if (clip != null && !clip.intersects(x, y, width, height)) {
			return false;
		}
		if (img instanceof TBufferedImage) {
			pushOp(new BlitOp(Operation.BLIT_IMAGE, img, x, y, width, height));
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
			pushOp(new BlitOp((TBufferedImage) img, x, y));
			return true;
		}
		return false;
	}

	public void onCanvasResize(int newWidth, int newHeight) {
		// Override in subclasses if needed
	}

	protected abstract void performBlit(List<BlitOp> ops);

	// helpers for getting translation components

	protected int getTx() {
		return (int) Math.round(transform.getTranslateX());
	}

	protected int getTy() {
		return (int) Math.round(transform.getTranslateY());
	}

	// Blitting related methods


	private boolean coalesce(BlitOp previous, BlitOp requested) {
		if (previous == null || requested == null) {
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
			case SET_COLOR:
				previous.obj = requested.obj; // Update to the latest color
				return true;
			default:
				return false;
		}
	}

	public final void pushOp(BlitOp op) {

		if (coalesce(previousOp, op)) {
			// Coalesced, do not add new op
			return;
		}

		writeList.add(op);
		previousOp = op;

		if (!blitScheduled) {
			blitScheduled = true;
			scheduleBlit();
		}
	}

	protected final void scheduleBlit() {
		Window.requestAnimationFrame(time -> {
			// Swap lists
			List<BlitOp> temp = readList;
			readList = writeList;
			writeList = temp;

			previousOp = null;
			writeList.clear();

			performBlit(readList);

			blitScheduled = false;
		});
	}

	static class BlitOp {
		public Operation type;
		public Object obj;
		public int arg1;
		public int arg2;
		public int arg3;
		public int arg4;

		BlitOp(Operation type, int x, int y, int w, int h) {
			this(type, null, x, y, w, h);
		}

		BlitOp(TBufferedImage buffer, int x, int y) {
			this(Operation.BLIT_IMAGE, buffer, x, y, buffer.getWidth(), buffer.getHeight());
		}

		BlitOp(Operation type, Object target, int x, int y, int width, int height) {
			this.type = type;
			this.obj = target;
			this.arg1 = x;
			this.arg2 = y;
			this.arg3 = width;
			this.arg4 = height;
		}
	}

}
