package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import me.mdbell.awtea.classlib.java.awt.image.TImageObserver;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLCanvasElement;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public abstract class TCanvasGraphics extends TGraphics {

	protected final HTMLCanvasElement canvas;

	protected boolean needsBlit = false;
	protected boolean blitScheduled = false;

	protected final List<BlitOp> blitOps = new ArrayList<>();

	@Getter
	protected Color color = Color.BLACK;

	@Getter
	@Setter
	protected TFont font = TFont.getDefaultFont();

	public enum Operation {
		BLIT_IMAGE,
		SET_COLOR,
		DRAW_RECT,
		FILL_RECT,
		CLEAR_RECT
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
		pushOp(new BlitOp(Operation.DRAW_RECT, x, y, width, height));
	}

	@Override
	public void fillRect(int x, int y, int width, int height) {
		if (width <= 0 || height <= 0) {
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
		if (img instanceof TBufferedImage) {
			pushOp(new BlitOp(Operation.BLIT_IMAGE, img, x, y, width, height));
			return true;
		}
		return false;
	}

	@Override
	public boolean drawImage(TImage img, int x, int y, TImageObserver observer) {
		if (img instanceof TBufferedImage) {
			pushOp(new BlitOp((TBufferedImage) img, x, y));
			return true;
		}
		return false;
	}

	// Blitting related methods

	protected abstract void performBlit(List<BlitOp> ops);

	private BlitOp previousOp = null;

	public final void pushOp(BlitOp op) {
		if (!needsBlit && blitScheduled) {
			blitScheduled = false;
			previousOp = null;
			blitOps.clear();
		}

		if (previousOp != null && previousOp.type == op.type) {
			// Merge consecutive set color operations
			if (op.type == Operation.SET_COLOR) {
				blitOps.remove(blitOps.size() - 1);
			}
			if (op.type == Operation.BLIT_IMAGE && previousOp.obj == op.obj
				&& previousOp.arg1 == op.arg1 && previousOp.arg2 == op.arg2 // x,y
				&& previousOp.arg3 == op.arg3 && previousOp.arg4 == op.arg4 // width,height
			) {
				// Merge consecutive blit image operations for the same image
				blitOps.remove(blitOps.size() - 1);
//				System.out.println(" Merged duplicate blit image operation");
			}
		}

		blitOps.add(op);
		previousOp = op;

		needsBlit = true;
		if (!blitScheduled) {
			scheduleBlit();
		}
	}

	protected final void scheduleBlit() {
		Window.requestAnimationFrame(time -> {
			notifyScheduled();

			if (!needsBlit) {
				return; // nothing new to draw
			}

			// we need to make the list unmodifiable to prevent concurrent modification exceptions
			// in case new blit ops are added while performing the blit
			// although this should be rare since blit ops are usually added in response to painting
			// which should not happen during the blit operation itself
			performBlit(Collections.unmodifiableList(blitOps));

			clearBlitRequest();
		});
	}

	protected final void notifyScheduled() {
		blitScheduled = true;
	}

	protected final void clearBlitRequest() {
		needsBlit = false;
	}

	public static class BlitOp {
		public final Operation type;
		public final Object obj;
		public final int arg1;
		public final int arg2;
		public final int arg3;
		public final int arg4;

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
