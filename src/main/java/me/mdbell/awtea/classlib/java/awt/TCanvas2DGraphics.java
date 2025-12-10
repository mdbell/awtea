package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import me.mdbell.awtea.util.ColorUtil;
import me.mdbell.awtea.util.JSObjectsExtensions;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.dom.html.HTMLCanvasElement;

import java.awt.*;
import java.util.List;

@Getter
@ExtensionMethod({JSObjectsExtensions.class, ColorUtil.class})
public class TCanvas2DGraphics extends TCanvasGraphics {

	private final CanvasRenderingContext2D context;

	private final TAffineTransform transform = new TAffineTransform();

	@Getter
	@Setter
	private TRectangle clipBounds;

	public TCanvas2DGraphics(HTMLCanvasElement canvasElement) {
		super(canvasElement);
		this.context = (CanvasRenderingContext2D) canvasElement.getContext("2d");
		initContext();
	}

	private TCanvas2DGraphics(CanvasRenderingContext2D context, boolean initReset) {
		super(context.getCanvas());
		this.context = context;
		if (initReset) {
			initContext();
		}
	}

	private void initContext() {
		this.context.setImageSmoothingEnabled(false);
		this.context.setLineJoin("miter");
		this.context.setLineCap("butt");
		setClipBounds(new TRectangle(context.getCanvas().getWidth(), context.getCanvas().getHeight()));
		reset();
	}

	@Override
	public TGraphics create() {
		TCanvas2DGraphics gfx = new TCanvas2DGraphics(this.context, false);
		gfx.setFont(this.getFont());
		gfx.setColor(this.getColor());
		gfx.setClipBounds(this.getClipBounds());
		gfx.transform.setTransform(this.transform);
		gfx.syncTransform();
		return gfx;
	}

	@Override
	public void setColor(Color c) {
		//TODO: resolve why the fuck teavm colors aren't working
		this.color = c;
		String color = c.toCSS();
		context.setFillStyle(color);
		context.setStrokeStyle(color);
		context.setShadowColor("transparent");
	}

	@Override
	public TFontMetrics getFontMetrics(TFont f) {
		return f.getFontMetrics();
	}

	@Override
	public void setClip(int x, int y, int width, int height) {
	}

	@Override
	public void setXORMode(Color c1) {

	}

	@Override
	public void setPaintMode() {

	}

	@Override
	public void drawRect(int x, int y, int width, int height) {
		context.beginPath();
		context.rect(x, y, width, height);
		context.stroke();
	}

	@Override
	public void fillRect(int x, int y, int width, int height) {
		context.beginPath();
		context.rect(x, y, width, height);
		context.fill();
	}

	@Override
	public void clearRect(int x, int y, int width, int height) {
		//context.clearRect(x, y, width, height);
	}

	private void roundRectPath(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		double aw = arcWidth / 2.0;
		double ah = arcHeight / 2.0;

		context.beginPath();
		context.moveTo(x + aw, y);
		context.lineTo(x + width - aw, y);
		context.quadraticCurveTo(x + width, y, x + width, y + ah);
		context.lineTo(x + width, y + height - ah);
		context.quadraticCurveTo(x + width, y + height, x + width - aw, y + height);
		context.lineTo(x + aw, y + height);
		context.quadraticCurveTo(x, y + height, x, y + height - ah);
		context.lineTo(x, y + ah);
		context.quadraticCurveTo(x, y, x + aw, y);
		context.closePath();
	}

	@Override
	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		double aw = arcWidth / 2.0;
		double ah = arcHeight / 2.0;

		roundRectPath(x, y, width, height, arcWidth, arcHeight);
		context.stroke();
	}

	@Override
	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		roundRectPath(x, y, width, height, arcWidth, arcHeight);
		context.fill();
	}

	@Override
	public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
		if (nPoints < 2) {
			return;
		}

		context.beginPath();
		context.moveTo(xPoints[0], yPoints[0]);
		for (int i = 1; i < nPoints; i++) {
			context.lineTo(xPoints[i], yPoints[i]);
		}
		context.closePath();
		context.stroke();
	}

	@Override
	public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
		if (nPoints < 2) {
			return;
		}

		context.beginPath();
		context.moveTo(xPoints[0], yPoints[0]);
		for (int i = 1; i < nPoints; i++) {
			context.lineTo(xPoints[i], yPoints[i]);
		}
		context.closePath();
		context.fill();
	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2) {
		context.beginPath();
		context.moveTo(x1, y1);
		context.lineTo(x2, y2);
		context.stroke();
	}

	@Override
	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		double cx = x + width / 2.0;
		double cy = y + height / 2.0;
		double rx = width / 2.0;
		double ry = height / 2.0;

		double startRad = Math.toRadians(startAngle);
		double endRad = Math.toRadians(startAngle + arcAngle);
		boolean anticlockwise = arcAngle < 0;
		context.beginPath();
		context.save();
		context.translate(cx, cy);
		context.scale(rx, ry);
		context.arc(0, 0, 1, startRad, endRad, anticlockwise);
		context.restore();
		context.stroke();
	}

	@Override
	public TShape getClip() {
		return getClipBounds();
	}

	@Override
	public void clipRect(int x, int y, int width, int height) {
		TRectangle currentClip = getClipBounds();
		if (currentClip == null) {
			// If no current clip, set the new rectangle as the clip
			setClipBounds(new TRectangle(x, y, width, height));
		} else {
			// Intersect the current clip with the new rectangle
			TRectangle newClip = currentClip.intersection(x, y, width, height);
			setClipBounds(newClip);
		}

		// Note: We don't actually apply the clip to the canvas context to avoid
		// stacking save/restore states which can block rendering. The clip bounds
		// are tracked and can be queried via getClip().
	}

	@Override
	public void setClip(TShape clip) {
		if (clip == null) {
			// Clear the clip - reset to full canvas size
			setClipBounds(null);
		} else if (clip instanceof TRectangle) {
			// Direct TRectangle - use it directly
			TRectangle rect = (TRectangle) clip;
			setClipBounds(new TRectangle(rect.x, rect.y, rect.width, rect.height));
		} else {
			// For other TShape types, use the bounds
			TRectangle bounds = clip.getBounds();
			setClipBounds(new TRectangle(bounds.x, bounds.y, bounds.width, bounds.height));
		}

		// Note: We don't actually apply the clip to the canvas context to avoid
		// stacking save/restore states which can block rendering. The clip bounds
		// are tracked and can be queried via getClip().
	}

	@Override
	public void reset() {
		//Debug.trigger();
		//setColor(Color.WHITE);

		this.transform.setToIdentity();
		syncTransform();
	}

	// transform operations

	@Override
	public void translate(int deltaX, int deltaY) {
		this.transform.translate(deltaX, deltaY);
		syncTransform();
	}

	private void syncTransform() {
		this.context.setTransform(
			this.transform.getScaleX(), this.transform.getShearY(),
			this.transform.getShearX(), this.transform.getScaleY(),
			this.transform.getTranslateX(), this.transform.getTranslateY()
		);
	}

	public void putImageData(int x, int y, ImageData imageData) {
		this.context.putImageData(imageData, x, y);
	}

	@Override
	protected void performBlit(List<BlitOp> ops) {
		for (BlitOp blit : ops) {
			if (blit.type != Operation.BLIT_IMAGE) {
				System.err.println("Unsupported blit operation: " + blit.type);
				continue;
			}
			drawImageImpl(blit);
		}
	}

	private void drawImageImpl(BlitOp op) {
		TBufferedImage img = (TBufferedImage) op.img;
		ImageData imageData = img.getImageData();
		//TODO: implement scaling
		this.context.putImageData(imageData, op.arg1, op.arg2);
	}
}
