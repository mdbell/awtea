package me.mdbell.awtea.gfx.software;

import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceCommand;
import me.mdbell.awtea.gfx.SurfaceContainer;
import me.mdbell.awtea.instrument.Monitored;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.List;

/**
 * Pure Java software rasterizer implementation.
 * <p>
 * This rasterizer supports all standard SurfaceCommand operations and can read/write
 * all pixel formats (ARGB, RGB, RGBA, ABGR, BGR) through format conversion logic.
 * <p>
 * Note: The parent SoftwareSurface can only be created with ARGB, RGB, or BGR formats,
 * but this rasterizer can blit from surfaces with any format via automatic conversion.
 * <p>
 * Transform support: Currently only translation is implemented. Full affine transforms
 * (scale, rotation, shear) would require more complex scan conversion and are deferred
 * as a future enhancement for this software fallback renderer.
 */
@Monitored.AllMethods
public class SoftwareRasterizer implements Rasterizer {

	/**
	 * Functional interface for optimized pixel setting operations.
	 * Allows resolving the pixel format logic once rather than per-pixel.
	 */
	@FunctionalInterface
	private interface PixelSetter {
		void setPixel(Uint8ClampedArray pixels, int idx, int color);
	}

	private final SoftwareSurface surface;
	private final AffineTransform transform = new AffineTransform();
	private Rectangle clip = null;

	// Color caching: store both Color objects and their encoded values for current format
	private Color foreground = Color.WHITE;
	private Color background = Color.BLACK;
	private int encodedForeground = 0;
	private int encodedBackground = 0;
	private int cachedFormat = -1; // Track which format the encoded colors are for


	SoftwareRasterizer(SoftwareSurface surface) {
		this.surface = surface;
		this.clip = new Rectangle(0, 0, surface.getWidth(), surface.getHeight());
		updateEncodedColors();
	}

	private SoftwareRasterizer(SoftwareRasterizer other) {
		this.surface = other.surface;
		this.transform.setTransform(other.transform);
		this.foreground = other.foreground;
		this.background = other.background;
		this.encodedForeground = other.encodedForeground;
		this.encodedBackground = other.encodedBackground;
		this.cachedFormat = other.cachedFormat;
		this.clip = other.clip != null ? new Rectangle(other.clip) : null;
	}

	/**
	 * Updates the encoded color cache when colors or format might have changed.
	 * This ensures we only encode colors once when SET_COLOR is evaluated.
	 */
	private void updateEncodedColors() {
		int format = surface.getFormat();
		if (format != cachedFormat) {
			cachedFormat = format;
			encodedForeground = encodeColor(foreground, format);
			encodedBackground = encodeColor(background, format);
		}
	}

	/**
	 * Updates the encoded foreground color.
	 */
	private void updateEncodedForeground() {
		int format = surface.getFormat();
		cachedFormat = format;
		encodedForeground = encodeColor(foreground, format);
	}

	/**
	 * Updates the encoded background color.
	 */
	private void updateEncodedBackground() {
		int format = surface.getFormat();
		cachedFormat = format;
		encodedBackground = encodeColor(background, format);
	}

	@Override
	public Rasterizer create() {
		return new SoftwareRasterizer(this);
	}

	@Override
	public void reset() {
		this.transform.setToIdentity();
		this.foreground = Color.WHITE;
		this.background = Color.BLACK;
		this.clip = new Rectangle(0, 0, surface.getWidth(), surface.getHeight());
		updateEncodedColors();
	}

	@Override
	public void rasterizeCommands(List<SurfaceCommand> cmds) {
		for (SurfaceCommand cmd : cmds) {
			switch (cmd.type) {
				case SET_COLOR:
					Color c = (Color) cmd.obj;
					if (cmd.arg1 == 0) {
						this.foreground = c;
						updateEncodedForeground();
					} else if (cmd.arg1 == 1) {
						this.background = c;
						updateEncodedBackground();
					}
					break;
				case SET_TRANSFORM:
					AffineTransform at = (AffineTransform) cmd.obj;
					this.transform.setTransform(at);
					break;
				case SET_CLIP_RECT:
					Shape shape = (Shape) cmd.obj;
					if (shape == null) {
						this.clip = null;
					} else {
						this.clip = shape.getBounds();
					}
					break;
				case BLIT_IMAGE:
					Surface srcSurface = ((SurfaceContainer) cmd.obj).getSurface();
					blitImage(srcSurface, cmd.arg1, cmd.arg2, cmd.arg3, cmd.arg4);
					break;
				case DRAW_RECT:
					drawRect(cmd.arg1, cmd.arg2, cmd.arg3, cmd.arg4);
					break;
				case FILL_RECT:
					fillRect(cmd.arg1, cmd.arg2, cmd.arg3, cmd.arg4);
					break;
				case CLEAR_RECT:
					clearRect(cmd.arg1, cmd.arg2, cmd.arg3, cmd.arg4);
					break;
				case DRAW_LINE:
					drawLine(cmd.arg1, cmd.arg2, cmd.arg3, cmd.arg4);
					break;
				case NO_OP:
					break;
				default:
					System.err.println("SoftwareRasterizer: Unhandled command type: " + cmd.type);
					break;
			}
		}
		surface.markDirty();
	}

	private void fillRect(int x, int y, int width, int height) {
		// Apply transform
		// Note: Currently only translation is supported for simplicity.
		// Full affine transform support (scale, rotation, shear) would require
		// transforming the rectangle corners and scan-converting the resulting quad,
		// which is complex for a software fallback renderer.
		int tx = (int) (x + transform.getTranslateX());
		int ty = (int) (y + transform.getTranslateY());

		// Apply clip
		Rectangle bounds = clip != null ? clip : new Rectangle(0, 0, surface.getWidth(), surface.getHeight());
		int x0 = Math.max(tx, bounds.x);
		int y0 = Math.max(ty, bounds.y);
		int x1 = Math.min(tx + width, bounds.x + bounds.width);
		int y1 = Math.min(ty + height, bounds.y + bounds.height);

		if (x0 >= x1 || y0 >= y1) {
			return;
		}

		int[] pixels = surface.getPixelDataAsInt32Array();
		if (pixels == null) {
			return;
		}

		int surfaceWidth = surface.getWidth();
		int surfaceHeight = surface.getHeight();

		for (int row = y0; row < y1; row++) {
			if (row < 0 || row >= surfaceHeight) {
				continue;
			}
			for (int col = x0; col < x1; col++) {
				if (col < 0 || col >= surfaceWidth) {
					continue;
				}
				int idx = (row * surfaceWidth + col);
				pixels[idx] = encodedForeground;
			}
		}
	}

	private void drawRect(int x, int y, int width, int height) {
		if (width <= 0 || height <= 0) {
			return;
		}
		// Draw 4 lines to form the rectangle
		fillRect(x, y, width, 1); // top
		fillRect(x, y + height - 1, width, 1); // bottom
		if (height > 2) {
			fillRect(x, y + 1, 1, height - 2); // left
			fillRect(x + width - 1, y + 1, 1, height - 2); // right
		}
	}

	private void clearRect(int x, int y, int width, int height) {
		Color oldFg = foreground;
		int oldFgInt = encodedForeground;
		foreground = background;
		encodedForeground = encodedBackground;
		fillRect(x, y, width, height);
		foreground = oldFg;
		encodedForeground = oldFgInt;
	}

	private void drawLine(int x1, int y1, int x2, int y2) {
		// Apply transform
		int tx1 = (int) (x1 + transform.getTranslateX());
		int ty1 = (int) (y1 + transform.getTranslateY());
		int tx2 = (int) (x2 + transform.getTranslateX());
		int ty2 = (int) (y2 + transform.getTranslateY());

		// Bresenham's line algorithm
		int dx = Math.abs(tx2 - tx1);
		int dy = Math.abs(ty2 - ty1);
		int sx = tx1 < tx2 ? 1 : -1;
		int sy = ty1 < ty2 ? 1 : -1;
		int err = dx - dy;

		int[] pixels = surface.getPixelDataAsInt32Array();
		if (pixels == null) {
			return;
		}

		int surfaceWidth = surface.getWidth();
		int surfaceHeight = surface.getHeight();


		Rectangle bounds = clip != null ? clip : new Rectangle(0, 0, surfaceWidth, surfaceHeight);

		while (true) {
			if (tx1 >= bounds.x && tx1 < bounds.x + bounds.width &&
				ty1 >= bounds.y && ty1 < bounds.y + bounds.height) {
				int idx = (ty1 * surfaceWidth + tx1);
				pixels[idx] = encodedForeground;
			}

			if (tx1 == tx2 && ty1 == ty2) {
				break;
			}

			int e2 = 2 * err;
			if (e2 > -dy) {
				err -= dy;
				tx1 += sx;
			}
			if (e2 < dx) {
				err += dx;
				ty1 += sy;
			}
		}
	}

	private void blitImage(Surface srcSurface, int destX, int destY, int destWidth, int destHeight) {
		if (srcSurface == null) {
			return;
		}

		// Apply transform
		int tx = (int) (destX + transform.getTranslateX());
		int ty = (int) (destY + transform.getTranslateY());

		Uint8ClampedArray srcPixelsBytes = srcSurface.getPixelData();
		int[] destPixels = surface.getPixelDataAsInt32Array();

		if (srcPixelsBytes == null || destPixels == null) {
			return;
		}

		// Create Int32Array view of source pixels for faster access
		Int32Array srcPixels = new Int32Array(srcPixelsBytes.getBuffer(),
			srcPixelsBytes.getByteOffset(), srcPixelsBytes.getByteLength() / 4);

		int srcWidth = srcSurface.getWidth();
		int srcHeight = srcSurface.getHeight();
		int destSurfaceWidth = surface.getWidth();
		int destSurfaceHeight = surface.getHeight();
		int srcFormat = srcSurface.getFormat();
		int destFormat = surface.getFormat();

		// If dest dimensions are 0, use source dimensions (no scaling)
		if (destWidth == 0) {
			destWidth = srcWidth;
		}
		if (destHeight == 0) {
			destHeight = srcHeight;
		}

		// Apply clip
		Rectangle bounds = clip != null ? clip : new Rectangle(0, 0, destSurfaceWidth, destSurfaceHeight);
		int x0 = Math.max(tx, bounds.x);
		int y0 = Math.max(ty, bounds.y);
		int x1 = Math.min(tx + destWidth, bounds.x + bounds.width);
		int y1 = Math.min(ty + destHeight, bounds.y + bounds.height);

		if (x0 >= x1 || y0 >= y1) {
			return;
		}

		// Calculate scaling factors as floats for better precision
		float scaleX = (float) srcWidth / destWidth;
		float scaleY = (float) srcHeight / destHeight;

		// Simple nearest-neighbor blit with improved rounding
		for (int destRow = y0; destRow < y1; destRow++) {
			if (destRow < 0 || destRow >= destSurfaceHeight) {
				continue;
			}

			// Use proper rounding for better nearest-neighbor sampling
			int srcRow = (int) ((destRow - ty) * scaleY + 0.5f);
			if (srcRow < 0 || srcRow >= srcHeight) {
				continue;
			}

			for (int destCol = x0; destCol < x1; destCol++) {
				if (destCol < 0 || destCol >= destSurfaceWidth) {
					continue;
				}

				int srcCol = (int) ((destCol - tx) * scaleX + 0.5f);
				if (srcCol < 0 || srcCol >= srcWidth) {
					continue;
				}

				// Read directly from Int32Array instead of reconstructing from bytes
				int srcIdx = srcRow * srcWidth + srcCol;
				int srcColor = srcPixels.get(srcIdx);
				int destIdx = destRow * destSurfaceWidth + destCol;
				destPixels[destIdx] = convertColor(srcColor, srcFormat, destFormat);
			}
		}
	}

	private int encodeColor(Color c, int format) {
		int r = c.getRed();
		int g = c.getGreen();
		int b = c.getBlue();
		int a = c.getAlpha();

		switch (format) {
			case Surface.FORMAT_INT_ARGB:
				return (a << 24) | (r << 16) | (g << 8) | b;
			case Surface.FORMAT_INT_RGB:
				return (r << 16) | (g << 8) | b;
			case Surface.FORMAT_INT_RGBA:
				return (r << 24) | (g << 16) | (b << 8) | a;
			case Surface.FORMAT_INT_ABGR:
				return (a << 24) | (b << 16) | (g << 8) | r;
			case Surface.FORMAT_INT_BGR:
				return (b << 16) | (g << 8) | r;
			default:
				return (a << 24) | (r << 16) | (g << 8) | b;
		}
	}

	/**
	 * Returns an optimized PixelSetter for the given format.
	 * This allows resolving the format switch once rather than per-pixel.
	 */
	private PixelSetter getPixelSetterForFormat(int format) {
		switch (format) {
			case Surface.FORMAT_INT_ARGB:
				return (pixels, idx, color) -> {
					// 0xAARRGGBB: write as [BB, GG, RR, AA]
					pixels.set(idx, color & 0xFF);         // B
					pixels.set(idx + 1, (color >> 8) & 0xFF);  // G
					pixels.set(idx + 2, (color >> 16) & 0xFF); // R
					pixels.set(idx + 3, (color >> 24) & 0xFF); // A
				};
			case Surface.FORMAT_INT_RGB:
				return (pixels, idx, color) -> {
					// 0x00RRGGBB: write as [BB, GG, RR, 0xFF]
					pixels.set(idx, color & 0xFF);         // B
					pixels.set(idx + 1, (color >> 8) & 0xFF);  // G
					pixels.set(idx + 2, (color >> 16) & 0xFF); // R
					pixels.set(idx + 3, 0xFF);             // A = opaque
				};
			case Surface.FORMAT_INT_RGBA:
				return (pixels, idx, color) -> {
					// 0xRRGGBBAA: write as [AA, BB, GG, RR]
					pixels.set(idx, color & 0xFF);         // A
					pixels.set(idx + 1, (color >> 8) & 0xFF);  // B
					pixels.set(idx + 2, (color >> 16) & 0xFF); // G
					pixels.set(idx + 3, (color >> 24) & 0xFF); // R
				};
			case Surface.FORMAT_INT_ABGR:
				return (pixels, idx, color) -> {
					// 0xAABBGGRR: write as [RR, GG, BB, AA]
					pixels.set(idx, color & 0xFF);         // R
					pixels.set(idx + 1, (color >> 8) & 0xFF);  // G
					pixels.set(idx + 2, (color >> 16) & 0xFF); // B
					pixels.set(idx + 3, (color >> 24) & 0xFF); // A
				};
			case Surface.FORMAT_INT_BGR:
				return (pixels, idx, color) -> {
					// 0x00BBGGRR: write as [RR, GG, BB, 0xFF]
					pixels.set(idx, color & 0xFF);         // R
					pixels.set(idx + 1, (color >> 8) & 0xFF);  // G
					pixels.set(idx + 2, (color >> 16) & 0xFF); // B
					pixels.set(idx + 3, 0xFF);             // A = opaque
				};
			default:
				// Default to ARGB
				return (pixels, idx, color) -> {
					pixels.set(idx, color & 0xFF);
					pixels.set(idx + 1, (color >> 8) & 0xFF);
					pixels.set(idx + 2, (color >> 16) & 0xFF);
					pixels.set(idx + 3, (color >> 24) & 0xFF);
				};
		}
	}

	private int convertColor(int color, int srcFormat, int destFormat) {
		if (srcFormat == destFormat) {
			return color;
		}

		// Extract RGBA components from source format
		int r, g, b, a;
		switch (srcFormat) {
			case Surface.FORMAT_INT_ARGB:
				a = (color >> 24) & 0xFF;
				r = (color >> 16) & 0xFF;
				g = (color >> 8) & 0xFF;
				b = color & 0xFF;
				break;
			case Surface.FORMAT_INT_RGB:
				a = 0xFF;
				r = (color >> 16) & 0xFF;
				g = (color >> 8) & 0xFF;
				b = color & 0xFF;
				break;
			case Surface.FORMAT_INT_RGBA:
				r = (color >> 24) & 0xFF;
				g = (color >> 16) & 0xFF;
				b = (color >> 8) & 0xFF;
				a = color & 0xFF;
				break;
			case Surface.FORMAT_INT_ABGR:
				a = (color >> 24) & 0xFF;
				b = (color >> 16) & 0xFF;
				g = (color >> 8) & 0xFF;
				r = color & 0xFF;
				break;
			case Surface.FORMAT_INT_BGR:
				a = 0xFF;
				b = (color >> 16) & 0xFF;
				g = (color >> 8) & 0xFF;
				r = color & 0xFF;
				break;
			default:
				a = (color >> 24) & 0xFF;
				r = (color >> 16) & 0xFF;
				g = (color >> 8) & 0xFF;
				b = color & 0xFF;
				break;
		}

		// Encode into destination format
		switch (destFormat) {
			case Surface.FORMAT_INT_ARGB:
				return (a << 24) | (r << 16) | (g << 8) | b;
			case Surface.FORMAT_INT_RGB:
				return (r << 16) | (g << 8) | b;
			case Surface.FORMAT_INT_RGBA:
				return (r << 24) | (g << 16) | (b << 8) | a;
			case Surface.FORMAT_INT_ABGR:
				return (a << 24) | (b << 16) | (g << 8) | r;
			case Surface.FORMAT_INT_BGR:
				return (b << 16) | (g << 8) | r;
			default:
				return (a << 24) | (r << 16) | (g << 8) | b;
		}
	}
}
