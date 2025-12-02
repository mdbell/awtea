package me.mdbell.awtea.classlib.java.awt.image;

import me.mdbell.awtea.classlib.java.awt.*;
import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;
import me.mdbell.awtea.classlib.java.awt.geom.TPoint2D;
import lombok.*;
import org.teavm.jso.canvas.ImageData;
import me.mdbell.awtea.font.TrueTypeFont;
import me.mdbell.awtea.impl.Debug;
import me.mdbell.awtea.support.ImageDataProvider;
import me.mdbell.awtea.util.GlyphRasterizer;

import java.awt.*;

@RequiredArgsConstructor(access=AccessLevel.PACKAGE)
public class TSoftwareGraphics extends TGraphics {

	private final TBufferedImage image;

	@Getter
	@Setter
	private Color color = Color.white;

	@Getter
	@Setter
	private TFont font = TFont.getDefaultFont();

	private final TAffineTransform transform = new TAffineTransform();

	@Override
	public TGraphics create() {
		return new TSoftwareGraphics(image);
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
	public void translate(int deltaX, int deltaY) {
		this.transform.translate(deltaX, deltaY);
	}

	@Override
	public TFontMetrics getFontMetrics(TFont f) {
		return null;
	}

	@Override
	public TRectangle getClipBounds() {
		return null;
	}

	@Override
	public void drawString(String str, int x, int y) {

		if (str == null || str.isEmpty()) {
			return;
		}

		TFont font = getFont();

		TrueTypeFont trueTypeFont = font.getTrueType();

		TPoint2D point = this.transform.deltaTransform(new TPoint2D.Double(x, y), null);

		float sizePx = getFont().getSize();
		GlyphRasterizer.drawString(trueTypeFont, str, (GlyphRasterizer.RasterTarget) image,
			sizePx, (int) point.getX(), (int) point.getY(), color.getRGB());
	}

	@Override
	public void drawRect(int x, int y, int width, int height) {
		if (width <= 0 || height <= 0) {
			return;
		}

		// Top horizontal edge
		fillRect(x, y, width, 1);

		// Bottom horizontal edge
		if (height > 1) {
			fillRect(x, y + height - 1, width, 1);
		}

		// Left vertical edge
		if (height > 2) { // avoid double-drawing the corners
			fillRect(x, y + 1, 1, height - 2);
		}

		// Right vertical edge
		if (width > 1 && height > 2) {
			fillRect(x + width - 1, y + 1, 1, height - 2);
		}
	}

	@Override
	public void fillRect(int x, int y, int width, int height) {

		TPoint2D pt = this.transform.deltaTransform(new TPoint2D.Double(x, y), null);
		x = (int) pt.getX();
		y = (int) pt.getY();

		// Clamp to surface bounds
		int x0 = Math.max(x, 0);
		int y0 = Math.max(y, 0);
		int x1 = Math.min(x + width,  image.getWidth());
		int y1 = Math.min(y + height, image.getHeight());
		if (x0 >= x1 || y0 >= y1) {
			return;
		}

		int w = x1 - x0;

		TWritableRaster raster = image.getRaster();
		TSampleModel sm = raster.getSampleModel();
		TDataBuffer db = raster.getBuffer();
		TColorModel cm = image.getColorModel();

		Color c = getColor();

		int argb = c.getRGB() | c.getAlpha() << 24;


		// ----------------------------------------------------------------
		// FAST PATH: int-packed RGB/ARGB with SinglePixelPackedSampleModel
		// ----------------------------------------------------------------
		boolean fast =
			(db instanceof TDataBufferInt) &&
				(sm instanceof TSinglePixelPackedSampleModel) &&
				(image.getImageType() == TBufferedImage.TYPE_INT_RGB ||
					image.getImageType() == TBufferedImage.TYPE_INT_ARGB ||
					image.getImageType() == TBufferedImage.TYPE_INT_ARGB_PRE);

		if (fast) {
			int[] data = ((TDataBufferInt) db).getData();
			TSinglePixelPackedSampleModel sppm = (TSinglePixelPackedSampleModel) sm;
			int scanlineStride = sppm.getScanlineStride();

			int packed;

			switch (image.getImageType()) {
				case TBufferedImage.TYPE_INT_RGB: {
					// Drop alpha, force opaque: 0x00RRGGBB
					packed = argb & 0x00FFFFFF;
					break;
				}

				case TBufferedImage.TYPE_INT_ARGB_PRE: {
					// Premultiply RGB by A

					int a = argb >> 24 & 0xFF;
					int r = argb >> 16 & 0xFF;
					int g = argb >> 8 & 0xFF;
					int b = argb & 0xFF;

					if (a == 0) {
						r = g = b = 0;
					} else if (a != 255) {
						// r' = r * a / 255 with rounding
						r = (r * a + 127) / 255;
						g = (g * a + 127) / 255;
						b = (b * a + 127) / 255;
					}

					packed = (a << 24) | (r << 16) | (g << 8) | b;
					break;
				}

				case TBufferedImage.TYPE_INT_ARGB:
				default: // shouldn't happen due to previous checks, but still.
					// Straight ARGB
					packed = argb;
					break;
			}

			// Fill scanlines directly in the int[] buffer
			for (int row = y0; row < y1; row++) {
				int base = row * scanlineStride + x0;
				java.util.Arrays.fill(data, base, base + w, packed);
			}

			return;
		}

		// ----------------------------------------------------------------
		// GENERIC PATH: use ColorModel + Raster (slower)
		// ----------------------------------------------------------------
		// Convert the Color into the pixel format for this color model
		Object pixelData = cm.getDataElements(argb, null);

		for (int row = y0; row < y1; row++) {
			for (int col = x0; col < x1; col++) {
				raster.setDataElements(col, row, pixelData);
			}
		}
	}

	@Override
	public void clearRect(int x, int y, int width, int height) {

	}

	@Override
	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {

	}

	@Override
	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {

	}

	@Override
	public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {

	}

	@Override
	public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {

	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2) {

	}

	@Override
	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {

	}

	@Override
	public boolean drawImage(TImage img, int x, int y, int width, int height, TImageObserver observer) {

		TPoint2D pt = this.transform.transform(new TPoint2D.Double(x, y), null);
		x = (int) pt.getX();
		y = (int) pt.getY();

		if(img instanceof ImageDataProvider) {
			ImageData data = ((ImageDataProvider) img).getImageData();
			if(data != null) {
				image.putImageData(x, y, width, height, data);
				return true;
			}
		}
		throw Debug.unimplemented();
	}

	@Override
	public boolean drawImage(TImage img, int x, int y, TImageObserver observer) {

		TPoint2D pt = this.transform.transform(new TPoint2D.Double(x, y), null);
		x = (int) pt.getX();
		y = (int) pt.getY();

		if(img instanceof ImageDataProvider) {
			ImageData data = ((ImageDataProvider) img).getImageData();
			if(data != null) {
				image.putImageData(x, y, data);
				return true;
			}
		}
		throw Debug.unimplemented();
	}

	@Override
	public TFontMetrics measureText(TFont font) {
		return font.getFontMetrics();
	}

	@Override
	public void reset() {
		this.transform.setToIdentity();
	}

	@Override
	public TShape getClip() {
		throw Debug.unimplemented();
	}

	@Override
	public void clipRect(int x, int y, int width, int height) {
		throw Debug.unimplemented();
	}

	@Override
	public void setClip(TShape clip) {
		throw Debug.unimplemented();
	}
}
