package me.mdbell.awtea.gfx.software;

import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceCommand;
import me.mdbell.awtea.gfx.SurfaceContainer;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.typedarrays.Int32Array;
import org.teavm.jso.typedarrays.Uint8ClampedArray;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Pure Java software rasterizer implementation.
 * <p>
 * This rasterizer supports all standard SurfaceCommand operations and can
 * read/write
 * all pixel formats (ARGB, RGB, RGBA, ABGR, BGR) through format conversion
 * logic.
 * <p>
 * Note: The parent SoftwareSurface can only be created with ARGB, RGB, or BGR
 * formats,
 * but this rasterizer can blit from surfaces with any format via automatic
 * conversion.
 * <p>
 * Transform support: Currently only translation is implemented. Full affine
 * transforms
 * (scale, rotation, shear) would require more complex scan conversion and are
 * deferred
 * as a future enhancement for this software fallback renderer.
 * <p>
 * Alpha blending: Supports standard Porter-Duff compositing rules via
 * AlphaComposite.
 * The default composite is SRC_OVER with alpha = 1.0.
 */
// @Monitored.AllMethods
public class SoftwareRasterizer implements Rasterizer {

    private static final Logger log = LoggerFactory.getLogger(SoftwareRasterizer.class);

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

    // Color caching: store both Color objects and their encoded values for current
    // format
    private Color foreground = Color.WHITE;
    private Color background = Color.BLACK;
    private int encodedForeground = 0;
    private int encodedBackground = 0;
    private int cachedFormat = -1; // Track which format the encoded colors are for

    // Compositing state
    private Composite composite = AlphaComposite.SrcOver;
    private boolean needsBlending = false; // Cached result to avoid repeated instanceof checks

    // Edge table pool for polygon filling
    private static final EdgeTablePool edgeTablePool = new EdgeTablePool();

    SoftwareRasterizer(SoftwareSurface surface) {
        this.surface = surface;
        this.clip = new Rectangle(0, 0, surface.getWidth(), surface.getHeight());
        updateEncodedColors();
        updateBlendingCache();
    }

    private SoftwareRasterizer(SoftwareRasterizer other) {
        this.surface = other.surface;
        this.transform.setTransform(other.transform);
        this.foreground = other.foreground;
        this.background = other.background;
        this.encodedForeground = other.encodedForeground;
        this.encodedBackground = other.encodedBackground;
        this.cachedFormat = other.cachedFormat;
        // Clone the clip rectangle manually (Rectangle copy constructor not available
        // in TeaVM)
        this.clip = other.clip != null ? new Rectangle(other.clip.x, other.clip.y, other.clip.width, other.clip.height)
                : null;
        this.composite = other.composite;
        this.needsBlending = other.needsBlending;
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
        this.composite = AlphaComposite.SrcOver;
        updateEncodedColors();
        updateBlendingCache();
    }

    @Override
    public void rasterizeCommands(List<SurfaceCommand> cmds) {
        for (SurfaceCommand cmd : cmds) {
            switch (cmd.type) {
                case SET_COLOR:
                    Color c = (Color) cmd.obj;
                    if (cmd.argCount > 0 && cmd.args[0] == 0) {
                        this.foreground = c;
                        updateEncodedForeground();
                    } else if (cmd.argCount > 0 && cmd.args[0] == 1) {
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
                case SET_COMPOSITE:
                    Composite comp = (Composite) cmd.obj;
                    this.composite = comp != null ? comp : AlphaComposite.SrcOver;
                    updateBlendingCache();
                    break;
                case BLIT_IMAGE:
                    Surface srcSurface = ((SurfaceContainer) cmd.obj).getSurface();
                    blitImage(srcSurface, cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3]);
                    break;
                case DRAW_RECT:
                    drawRect(cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3]);
                    break;
                case FILL_RECT:
                    fillRect(cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3]);
                    break;
                case CLEAR_RECT:
                    clearRect(cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3]);
                    break;
                case DRAW_LINE:
                    drawLine(cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3]);
                    break;
                case DRAW_POLYGON: {
                    java.awt.Polygon polygon = (java.awt.Polygon) cmd.obj;
                    drawPolygon(polygon.xpoints, polygon.ypoints);
                }
                    break;
                case FILL_POLYGON: {
                    java.awt.Polygon polygon = (java.awt.Polygon) cmd.obj;
                    fillPolygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
                }
                    break;
                case FILL_OVAL:
                    fillOval(cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3]);
                    break;
                case FILL_ROUND_RECT:
                    fillRoundRect(cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3], cmd.args[4], cmd.args[5]);
                    break;
                case FILL_ARC:
                    fillArc(cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3], cmd.args[4], cmd.args[5]);
                    break;
                case DRAW_OVAL:
                    drawOval(cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3]);
                    break;
                case DRAW_ARC:
                    drawArc(cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3], cmd.args[4], cmd.args[5]);
                    break;
                case DRAW_ROUND_RECT:
                    drawRoundRect(cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3], cmd.args[4], cmd.args[5]);
                    break;
                case DRAW_POLYLINE: {
                    java.awt.Polygon polygon = (java.awt.Polygon) cmd.obj;
                    drawPolyline(polygon.xpoints, polygon.ypoints, polygon.npoints);
                }
                    break;
                case COPY_AREA:
                    copyArea(cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3], cmd.args[4], cmd.args[5]);
                    break;
                case NO_OP:
                    break;
                default:
                    log.error("SoftwareRasterizer: Unhandled command type: {}", cmd.type);
                    break;
            }
        }
        surface.markDirty();
    }

    private static int clamp(int val, int min, int max) {
        if (val < min) {
            return min;
        }
        return Math.min(val, max);
    }

    private int clipX(int x) {
        x = clamp(x, 0, surface.getWidth() - 1);
        if (clip != null) {
            x = clamp(x, clip.x, clip.x + clip.width - 1);
        }
        return x;
    }

    private int clipY(int y) {
        y = clamp(y, 0, surface.getHeight() - 1);
        if (clip != null) {
            y = clamp(y, clip.y, clip.y + clip.height - 1);
        }
        return y;
    }

    private void drawRect(int x, int y, int width, int height) {
        // Top edge
        drawLine(x, y, x + width, y);
        // Bottom edge
        drawLine(x, y + height, x + width, y + height);
        // Left edge
        drawLine(x, y, x, y + height);
        // Right edge
        drawLine(x + width, y, x + width, y + height);
    }

    private void fillRect(int x, int y, int width, int height) {

        // Transform coordinates to device space first
        int x0, y0, x1, y1;

        if (transform.isIdentity()) {
            x0 = x + (int) transform.getTranslateX();
            y0 = y + (int) transform.getTranslateY();
            x1 = x + width - 1 + (int) transform.getTranslateX();
            y1 = y + height - 1 + (int) transform.getTranslateY();
        } else {
            Point2D p1 = new Point2D.Float(x, y);
            Point2D p2 = new Point2D.Float(x + width - 1, y + height - 1);
            transform.transform(p1, p1);
            transform.transform(p2, p2);
            x0 = Math.round((float) p1.getX());
            y0 = Math.round((float) p1.getY());
            x1 = Math.round((float) p2.getX());
            y1 = Math.round((float) p2.getY());
        }

        // Fast path: rectangle is completely outside clip (in device space)
        if (clip != null) {
            if (x1 < clip.x || x0 >= clip.x + clip.width ||
                    y1 < clip.y || y0 >= clip.y + clip.height) {
                return;
            }
        }

        // Clip in device space
        x0 = clipX(x0);
        y0 = clipY(y0);
        x1 = clipX(x1);
        y1 = clipY(y1);

        if (x0 >= x1 || y0 >= y1) {
            return;
        }

        Uint8ClampedArray pixels = surface.getPixelData();
        if (pixels == null) {
            return;
        }
        int[] pixelDataAsInt32 = surface.getPixelDataAsInt32Array();

        int format = surface.getFormat();

        boolean blend = needsBlending();

        int surfaceWidth = surface.getWidth();

        if (blend) {
            int srcColorARGB = convertColorToARGB(encodedForeground, format);
            for (int row = y0; row <= y1; row++) {
                for (int col = x0; col <= x1; col++) {
                    int dstColor = pixelDataAsInt32[row * surfaceWidth + col];
                    pixelDataAsInt32[row * surfaceWidth + col] = blendPixel(srcColorARGB,
                            convertColorToARGB(dstColor, format), composite);
                }
            }
        } else {
            for (int row = y0; row <= y1; row++) {
                for (int col = x0; col <= x1; col++) {
                    int idx = (row * surfaceWidth + col) * 4;
                    pixelDataAsInt32[row * surfaceWidth + col] = encodedForeground;
                }
            }
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

        // Transform endpoints to device space first
        Point2D p1 = new Point2D.Float(x1, y1);
        Point2D p2 = new Point2D.Float(x2, y2);
        if (!transform.isIdentity()) {
            transform.transform(p1, p1);
            transform.transform(p2, p2);
        }

        x1 = Math.round((float) p1.getX());
        y1 = Math.round((float) p1.getY());
        x2 = Math.round((float) p2.getX());
        y2 = Math.round((float) p2.getY());

        // Fast path: both points are outside clip (in device space)
        if (clip != null) {
            if ((x1 < clip.x && x2 < clip.x) ||
                    (x1 >= clip.x + clip.width && x2 >= clip.x + clip.width) ||
                    (y1 < clip.y && y2 < clip.y) ||
                    (y1 >= clip.y + clip.height && y2 >= clip.y + clip.height)) {
                return;
            }
        }

        // Clip endpoints in device space
        x1 = clipX(x1);
        y1 = clipY(y1);
        x2 = clipX(x2);
        y2 = clipY(y2);

        if (x1 == x2 && y1 == y2) {
            // Single point
            int[] pixelDataAsInt32 = surface.getPixelDataAsInt32Array();
            int idx = y1 * surface.getWidth() + x1;
            if (needsBlending()) {
                int dstColor = pixelDataAsInt32[idx];
                pixelDataAsInt32[idx] = blendPixel(convertColorToARGB(encodedForeground, surface.getFormat()),
                        convertColorToARGB(dstColor, surface.getFormat()), composite);
            } else {
                pixelDataAsInt32[idx] = encodedForeground;
            }
            return;
        }

        int[] pixelDataAsInt32 = surface.getPixelDataAsInt32Array();

        // Bresenham's line algorithm
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;

        int err = dx - dy;

        boolean needsBlend = needsBlending();

        while (true) {

            if (needsBlend) {
                int idx = y1 * surface.getWidth() + x1;
                int dstColor = pixelDataAsInt32[idx];
                pixelDataAsInt32[idx] = blendPixel(convertColorToARGB(encodedForeground, surface.getFormat()),
                        convertColorToARGB(dstColor, surface.getFormat()), composite);
            } else {
                if (x1 >= 0 && x1 < surface.getWidth() && y1 >= 0 && y1 < surface.getHeight()) {
                    int idx = y1 * surface.getWidth() + x1;
                    pixelDataAsInt32[idx] = encodedForeground;
                }
            }

            if (x1 == x2 && y1 == y2) {
                break;
            }
            int err2 = 2 * err;
            if (err2 > -dy) {
                err -= dy;
                x1 += sx;
            }
            if (err2 < dx) {
                err += dx;
                y1 += sy;
            }
        }
    }

    private void drawPolygon(int[] xpoints, int[] ypoints) {
        int count = xpoints.length;
        for (int i = 1; i < count; i++) {
            drawLine(xpoints[i], ypoints[i], xpoints[i - 1], ypoints[i - 1]);
        }
        drawLine(xpoints[0], ypoints[0], xpoints[count - 1], ypoints[count - 1]);
    }

    private void blitImage(Surface srcSurface, int destX, int destY, int destWidth, int destHeight) {
        if (srcSurface == null) {
            return;
        }

        // Transform destination rectangle to device space first
        Point2D topLeft = new Point2D.Float(destX, destY);
        Point2D bottomRight = new Point2D.Float(destX + destWidth, destY + destHeight);
        if (!transform.isIdentity()) {
            transform.transform(topLeft, topLeft);
            transform.transform(bottomRight, bottomRight);
        }

        int transformedDestX = Math.round((float) topLeft.getX());
        int transformedDestY = Math.round((float) topLeft.getY());
        int transformedDestWidth = Math.round((float) bottomRight.getX()) - transformedDestX;
        int transformedDestHeight = Math.round((float) bottomRight.getY()) - transformedDestY;

        // Fast path: rectangle is completely outside clip (in device space)
        if (clip != null) {
            if (transformedDestX + transformedDestWidth <= clip.x || transformedDestX >= clip.x + clip.width ||
                    transformedDestY + transformedDestHeight <= clip.y || transformedDestY >= clip.y + clip.height) {
                return;
            }
        }

        blitImage(srcSurface, 0, 0,
                srcSurface.getWidth(), srcSurface.getHeight(),
                transformedDestX, transformedDestY, transformedDestWidth, transformedDestHeight);
    }

    private void blitImage(Surface surface, int srcX, int srcY,
            int srcWidth, int srcHeight,
            int destX, int destY, int destWidth, int destHeight) {
        // Clip in device space (coordinates are already transformed)
        int clippedDestX0 = clipX(destX);
        int clippedDestY0 = clipY(destY);
        int clippedDestX1 = clipX(destX + destWidth - 1);
        int clippedDestY1 = clipY(destY + destHeight - 1);

        // Adjust source coordinates based on clipping
        srcX += (clippedDestX0 - destX) * srcWidth / destWidth;
        srcY += (clippedDestY0 - destY) * srcHeight / destHeight;

        destX = clippedDestX0;
        destY = clippedDestY0;
        destWidth = clippedDestX1 - clippedDestX0 + 1;
        destHeight = clippedDestY1 - clippedDestY0 + 1;
        if (destWidth <= 0 || destHeight <= 0) {
            return;
        }

        // Coordinates are already in device space, no need to transform again

        Uint8ClampedArray srcPixArray = surface.getPixelData();
        int[] destPixels = this.surface.getPixelDataAsInt32Array();
        if (srcPixArray == null || destPixels == null) {
            return;
        }
        int[] srcPixels = new Int32Array(srcPixArray.getBuffer(), srcPixArray.getByteOffset(),
                srcPixArray.getLength() / 4).toJavaArray();

        int srcFormat = surface.getFormat();
        int destFormat = this.surface.getFormat();
        int surfaceWidth = this.surface.getWidth();
        for (int row = 0; row < destHeight; row++) {
            for (int col = 0; col < destWidth; col++) {
                int srcIdx = (srcY + row * srcHeight / destHeight) * surface.getWidth() +
                        (srcX + col * srcWidth / destWidth);
                int destIdx = (destY + row) * surfaceWidth + (destX + col);

                int srcColor = srcPixels[srcIdx];
                int convertedColor = convertColor(srcColor, srcFormat, destFormat);

                if (needsBlending()) {
                    int dstColor = destPixels[destIdx];
                    int blendedColor = blendPixel(convertColorToARGB(convertedColor, destFormat),
                            convertColorToARGB(dstColor, destFormat), composite);
                    destPixels[destIdx] = blendedColor;
                } else {
                    destPixels[destIdx] = convertedColor;
                }
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
                    pixels.set(idx, color & 0xFF); // B
                    pixels.set(idx + 1, (color >> 8) & 0xFF); // G
                    pixels.set(idx + 2, (color >> 16) & 0xFF); // R
                    pixels.set(idx + 3, (color >> 24) & 0xFF); // A
                };
            case Surface.FORMAT_INT_RGB:
                return (pixels, idx, color) -> {
                    // 0x00RRGGBB: write as [BB, GG, RR, 0xFF]
                    pixels.set(idx, color & 0xFF); // B
                    pixels.set(idx + 1, (color >> 8) & 0xFF); // G
                    pixels.set(idx + 2, (color >> 16) & 0xFF); // R
                    pixels.set(idx + 3, 0xFF); // A = opaque
                };
            case Surface.FORMAT_INT_RGBA:
                return (pixels, idx, color) -> {
                    // 0xRRGGBBAA: write as [AA, BB, GG, RR]
                    pixels.set(idx, color & 0xFF); // A
                    pixels.set(idx + 1, (color >> 8) & 0xFF); // B
                    pixels.set(idx + 2, (color >> 16) & 0xFF); // G
                    pixels.set(idx + 3, (color >> 24) & 0xFF); // R
                };
            case Surface.FORMAT_INT_ABGR:
                return (pixels, idx, color) -> {
                    // 0xAABBGGRR: write as [RR, GG, BB, AA]
                    pixels.set(idx, color & 0xFF); // R
                    pixels.set(idx + 1, (color >> 8) & 0xFF); // G
                    pixels.set(idx + 2, (color >> 16) & 0xFF); // B
                    pixels.set(idx + 3, (color >> 24) & 0xFF); // A
                };
            case Surface.FORMAT_INT_BGR:
                return (pixels, idx, color) -> {
                    // 0x00BBGGRR: write as [RR, GG, BB, 0xFF]
                    pixels.set(idx, color & 0xFF); // R
                    pixels.set(idx + 1, (color >> 8) & 0xFF); // G
                    pixels.set(idx + 2, (color >> 16) & 0xFF); // B
                    pixels.set(idx + 3, 0xFF); // A = opaque
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

    /**
     * Converts a color from any format to ARGB format.
     *
     * @param color     the color value in the source format
     * @param srcFormat the source format
     * @return the color in ARGB format
     */
    private int convertColorToARGB(int color, int srcFormat) {
        return convertColor(color, srcFormat, Surface.FORMAT_INT_ARGB);
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

    /**
     * Applies alpha compositing to blend source and destination pixels.
     * Supports Porter-Duff compositing rules.
     *
     * @param srcColor  source color in ARGB format
     * @param dstColor  destination color in ARGB format
     * @param composite the composite operation to apply
     * @return the blended color in ARGB format
     */
    private int blendPixel(int srcColor, int dstColor, Composite composite) {
        if (!(composite instanceof AlphaComposite)) {
            // If not an alpha composite, just return source (SRC mode)
            return srcColor;
        }

        AlphaComposite alphaComp = (AlphaComposite) composite;
        int rule = alphaComp.getRule();
        float extraAlpha = alphaComp.getAlpha();

        // Extract source ARGB components
        int sa = (srcColor >> 24) & 0xFF;
        int sr = (srcColor >> 16) & 0xFF;
        int sg = (srcColor >> 8) & 0xFF;
        int sb = srcColor & 0xFF;

        // Apply extra alpha to source
        sa = (int) (sa * extraAlpha);

        // Extract destination ARGB components
        int da = (dstColor >> 24) & 0xFF;
        int dr = (dstColor >> 16) & 0xFF;
        int dg = (dstColor >> 8) & 0xFF;
        int db = dstColor & 0xFF;

        // Normalize alpha values to [0, 1] range
        float srcAlpha = sa / 255.0f;
        float dstAlpha = da / 255.0f;

        float outAlpha;
        float srcFactor;
        float dstFactor;

        // Apply Porter-Duff compositing rules
        switch (rule) {
            case AlphaComposite.CLEAR:
                return 0; // Fully transparent

            case AlphaComposite.SRC:
                // Replace destination with source
                return (sa << 24) | (sr << 16) | (sg << 8) | sb;

            case AlphaComposite.DST:
                // Leave destination unchanged
                return dstColor;

            case AlphaComposite.SRC_OVER:
                // Source over destination (default blending - inline calculation for clarity)
                outAlpha = srcAlpha + dstAlpha * (1.0f - srcAlpha);

                // Calculate output colors directly using SRC_OVER formula
                int tempA = (int) (outAlpha * 255.0f + 0.5f);
                int tempR, tempG, tempB;
                if (outAlpha > 0.0f) {
                    tempR = (int) ((sr * srcAlpha + dr * dstAlpha * (1.0f - srcAlpha)) / outAlpha + 0.5f);
                    tempG = (int) ((sg * srcAlpha + dg * dstAlpha * (1.0f - srcAlpha)) / outAlpha + 0.5f);
                    tempB = (int) ((sb * srcAlpha + db * dstAlpha * (1.0f - srcAlpha)) / outAlpha + 0.5f);
                } else {
                    tempR = tempG = tempB = 0;
                }

                // Clamp and return directly
                tempA = Math.min(255, Math.max(0, tempA));
                tempR = Math.min(255, Math.max(0, tempR));
                tempG = Math.min(255, Math.max(0, tempG));
                tempB = Math.min(255, Math.max(0, tempB));
                return (tempA << 24) | (tempR << 16) | (tempG << 8) | tempB;

            case AlphaComposite.DST_OVER:
                // Destination over source
                outAlpha = dstAlpha + srcAlpha * (1.0f - dstAlpha);
                srcFactor = 1.0f - dstAlpha;
                dstFactor = 1.0f;
                break;

            case AlphaComposite.SRC_IN:
                // Source where destination is opaque
                outAlpha = srcAlpha * dstAlpha;
                srcFactor = dstAlpha;
                dstFactor = 0.0f;
                break;

            case AlphaComposite.DST_IN:
                // Destination where source is opaque
                outAlpha = dstAlpha * srcAlpha;
                srcFactor = 0.0f;
                dstFactor = srcAlpha;
                break;

            case AlphaComposite.SRC_OUT:
                // Source where destination is transparent
                outAlpha = srcAlpha * (1.0f - dstAlpha);
                srcFactor = 1.0f - dstAlpha;
                dstFactor = 0.0f;
                break;

            case AlphaComposite.DST_OUT:
                // Destination where source is transparent
                outAlpha = dstAlpha * (1.0f - srcAlpha);
                srcFactor = 0.0f;
                dstFactor = 1.0f - srcAlpha;
                break;

            case AlphaComposite.SRC_ATOP:
                // Source over destination, only where destination is opaque
                outAlpha = dstAlpha;
                srcFactor = dstAlpha;
                dstFactor = 1.0f - srcAlpha;
                break;

            case AlphaComposite.DST_ATOP:
                // Destination over source, only where source is opaque
                outAlpha = srcAlpha;
                srcFactor = 1.0f - dstAlpha;
                dstFactor = srcAlpha;
                break;

            case AlphaComposite.XOR:
                // Source xor destination
                outAlpha = srcAlpha + dstAlpha - 2.0f * srcAlpha * dstAlpha;
                srcFactor = 1.0f - dstAlpha;
                dstFactor = 1.0f - srcAlpha;
                break;

            default:
                // Default to SRC_OVER
                outAlpha = srcAlpha + dstAlpha * (1.0f - srcAlpha);
                srcFactor = 1.0f;
                dstFactor = 1.0f - srcAlpha;
                break;
        }

        // Blend color channels
        int outA = (int) (outAlpha * 255.0f + 0.5f);
        int outR, outG, outB;

        if (outAlpha > 0.0f) {
            // Premultiply and blend, then un-premultiply
            outR = (int) ((sr * srcAlpha * srcFactor + dr * dstAlpha * dstFactor) / outAlpha + 0.5f);
            outG = (int) ((sg * srcAlpha * srcFactor + dg * dstAlpha * dstFactor) / outAlpha + 0.5f);
            outB = (int) ((sb * srcAlpha * srcFactor + db * dstAlpha * dstFactor) / outAlpha + 0.5f);
        } else {
            outR = outG = outB = 0;
        }

        // Clamp to [0, 255]
        outA = Math.min(255, Math.max(0, outA));
        outR = Math.min(255, Math.max(0, outR));
        outG = Math.min(255, Math.max(0, outG));
        outB = Math.min(255, Math.max(0, outB));

        return (outA << 24) | (outR << 16) | (outG << 8) | outB;
    }

    /**
     * Updates the cached blending flag based on the current composite.
     * Called whenever the composite is changed.
     */
    private void updateBlendingCache() {
        if (!(composite instanceof AlphaComposite)) {
            needsBlending = false;
            return;
        }
        AlphaComposite alphaComp = (AlphaComposite) composite;
        // Need blending if not SRC with full alpha
        needsBlending = alphaComp.getRule() != AlphaComposite.SRC || alphaComp.getAlpha() < 1.0f;
    }

    /**
     * Checks if alpha blending is needed based on the current composite.
     * This now returns the cached value instead of checking instanceof every time.
     *
     * @return true if alpha blending should be applied
     */
    private boolean needsBlending() {
        return needsBlending;
    }

    /**
     * Fill polygon using edge table algorithm
     */
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        if (xPoints == null || yPoints == null || nPoints < 3) {
            log.error("fillPolygon: invalid parameters");
            return;
        }

        log.debug("fillPolygon: nPoints={}", nPoints);

        // Calculate bounding box
        int minX = xPoints[0];
        int maxX = xPoints[0];
        int minY = yPoints[0];
        int maxY = yPoints[0];

        for (int i = 1; i < nPoints; i++) {
            if (xPoints[i] < minX)
                minX = xPoints[i];
            if (xPoints[i] > maxX)
                maxX = xPoints[i];
            if (yPoints[i] < minY)
                minY = yPoints[i];
            if (yPoints[i] > maxY)
                maxY = yPoints[i];
        }

        // Apply transform to bounding box
        if (!transform.isIdentity()) {
            Point2D.Float p1 = new Point2D.Float(minX, minY);
            Point2D.Float p2 = new Point2D.Float(maxX, maxY);
            transform.transform(p1, p1);
            transform.transform(p2, p2);
            minX = Math.round(p1.x);
            minY = Math.round(p1.y);
            maxX = Math.round(p2.x);
            maxY = Math.round(p2.y);
        }

        // Clip bounding box to surface
        minY = clamp(minY, 0, surface.getHeight() - 1);
        maxY = clamp(maxY, 0, surface.getHeight() - 1);

        if (minY >= maxY) {
            log.debug("fillPolygon: clipped out entirely");
            return;
        }

        // Get edge table from pool
        EdgeTable et = edgeTablePool.acquire(minY, maxY, surface.getWidth(), surface.getHeight());

        // Transform points and add edges
        int[] transformedX = new int[nPoints];
        int[] transformedY = new int[nPoints];

        for (int i = 0; i < nPoints; i++) {
            if (!transform.isIdentity()) {
                Point2D.Float p = new Point2D.Float(xPoints[i], yPoints[i]);
                transform.transform(p, p);
                transformedX[i] = Math.round(p.x);
                transformedY[i] = Math.round(p.y);
            } else {
                transformedX[i] = xPoints[i];
                transformedY[i] = yPoints[i];
            }
        }

        // Add all edges of the polygon
        for (int i = 0; i < nPoints; i++) {
            int next = (i + 1) % nPoints;
            et.addLine(transformedX[i], transformedY[i],
                    transformedX[next], transformedY[next]);
        }

        // Fill using edge table with even-odd rule
        int[] pixelData = surface.getPixelDataAsInt32Array();
        et.fill(pixelData, surface.getWidth(), surface.getHeight(),
                encodedForeground, surface.getFormat(), EdgeTable.FILL_RULE_EVENODD,
                composite, clip);

        // Return edge table to pool
        edgeTablePool.release(et);

        log.debug("fillPolygon: completed");
    }

    /**
     * Fill oval using edge table algorithm
     */
    public void fillOval(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            log.error("fillOval: invalid dimensions");
            return;
        }

        log.debug("fillOval: x={}, y={}, w={}, h={}", x, y, width, height);

        // Calculate center and radii
        int cx = x + width / 2;
        int cy = y + height / 2;
        int rx = width / 2;
        int ry = height / 2;

        // Apply transform
        if (!transform.isIdentity()) {
            Point2D.Float center = new Point2D.Float(cx, cy);
            transform.transform(center, center);
            cx = Math.round(center.x);
            cy = Math.round(center.y);
            // Note: Transform can affect radii, but for translation-only this is fine
        }

        // Calculate bounding box
        int minY = clamp(cy - ry, 0, surface.getHeight() - 1);
        int maxY = clamp(cy + ry, 0, surface.getHeight() - 1);

        if (minY >= maxY) {
            log.debug("fillOval: clipped out entirely");
            return;
        }

        // Get edge table from pool
        EdgeTable et = edgeTablePool.acquire(minY, maxY, surface.getWidth(), surface.getHeight());

        // Add arc for full ellipse (0 to 2*PI)
        et.addArc(cx, cy, rx, ry, 0.0, 2.0 * Math.PI);

        // Fill using edge table
        int[] pixelData = surface.getPixelDataAsInt32Array();
        et.fill(pixelData, surface.getWidth(), surface.getHeight(),
                encodedForeground, surface.getFormat(), EdgeTable.FILL_RULE_EVENODD,
                composite, clip);

        // Return edge table to pool
        edgeTablePool.release(et);

        log.debug("fillOval: completed");
    }

    /**
     * Fill arc using edge table algorithm
     */
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        if (width <= 0 || height <= 0) {
            log.error("fillArc: invalid dimensions");
            return;
        }

        log.debug("fillArc: x={}, y={}, w={}, h={}, start={}, arc={}",
                x, y, width, height, startAngle, arcAngle);

        // Calculate center and radii
        int cx = x + width / 2;
        int cy = y + height / 2;
        int rx = width / 2;
        int ry = height / 2;

        // Apply transform
        if (!transform.isIdentity()) {
            Point2D.Float center = new Point2D.Float(cx, cy);
            transform.transform(center, center);
            cx = Math.round(center.x);
            cy = Math.round(center.y);
        }

        // Convert angles from degrees to radians
        // Java AWT uses degrees with 0 at 3 o'clock, positive = counter-clockwise
        double startRad = -startAngle * Math.PI / 180.0;
        double endRad = -(startAngle + arcAngle) * Math.PI / 180.0;

        // Normalize to standard angles
        startRad = -startRad;
        endRad = -endRad;

        // Calculate bounding box
        int minY = clamp(cy - ry, 0, surface.getHeight() - 1);
        int maxY = clamp(cy + ry, 0, surface.getHeight() - 1);

        if (minY >= maxY) {
            log.debug("fillArc: clipped out entirely");
            return;
        }

        // Get edge table from pool
        EdgeTable et = edgeTablePool.acquire(minY, maxY, surface.getWidth(), surface.getHeight());

        // Add arc
        et.addArc(cx, cy, rx, ry, startRad, endRad);

        // Close the arc by adding lines from endpoints to center (pie slice)
        int startX = cx + (int) (rx * Math.cos(startRad));
        int startY = cy + (int) (ry * Math.sin(startRad));
        int endX = cx + (int) (rx * Math.cos(endRad));
        int endY = cy + (int) (ry * Math.sin(endRad));

        et.addLine(endX, endY, cx, cy);
        et.addLine(cx, cy, startX, startY);

        // Fill using edge table
        int[] pixelData = surface.getPixelDataAsInt32Array();
        et.fill(pixelData, surface.getWidth(), surface.getHeight(),
                encodedForeground, surface.getFormat(), EdgeTable.FILL_RULE_EVENODD,
                composite, clip);

        // Return edge table to pool
        edgeTablePool.release(et);

        log.debug("fillArc: completed");
    }

    /**
     * Fill rounded rectangle using edge table algorithm
     */
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        if (width <= 0 || height <= 0) {
            log.error("fillRoundRect: invalid dimensions");
            return;
        }

        log.debug("fillRoundRect: x={}, y={}, w={}, h={}, arcW={}, arcH={}",
                x, y, width, height, arcWidth, arcHeight);

        // Clamp arc dimensions
        if (arcWidth > width)
            arcWidth = width;
        if (arcHeight > height)
            arcHeight = height;

        int rx = arcWidth / 2;
        int ry = arcHeight / 2;

        // Apply transform
        if (!transform.isIdentity()) {
            Point2D.Float topLeft = new Point2D.Float(x, y);
            transform.transform(topLeft, topLeft);
            x = Math.round(topLeft.x);
            y = Math.round(topLeft.y);
        }

        // Calculate bounding box
        int minY = clamp(y, 0, surface.getHeight() - 1);
        int maxY = clamp(y + height, 0, surface.getHeight() - 1);

        if (minY >= maxY) {
            log.debug("fillRoundRect: clipped out entirely");
            return;
        }

        // Get edge table from pool
        EdgeTable et = edgeTablePool.acquire(minY, maxY, surface.getWidth(), surface.getHeight());

        // Add four corner arcs and four straight edges
        // Top edge
        et.addLine(x + rx, y, x + width - rx, y);

        // Top-right corner arc (0 to 90 degrees, or 0 to PI/2 radians)
        et.addArc(x + width - rx, y + ry, rx, ry, -Math.PI / 2.0, 0.0);

        // Right edge
        et.addLine(x + width, y + ry, x + width, y + height - ry);

        // Bottom-right corner arc (90 to 180 degrees, or PI/2 to PI radians)
        et.addArc(x + width - rx, y + height - ry, rx, ry, 0.0, Math.PI / 2.0);

        // Bottom edge
        et.addLine(x + width - rx, y + height, x + rx, y + height);

        // Bottom-left corner arc (180 to 270 degrees, or PI to 3*PI/2 radians)
        et.addArc(x + rx, y + height - ry, rx, ry, Math.PI / 2.0, Math.PI);

        // Left edge
        et.addLine(x, y + height - ry, x, y + ry);

        // Top-left corner arc (270 to 360 degrees, or 3*PI/2 to 2*PI radians)
        et.addArc(x + rx, y + ry, rx, ry, Math.PI, 3.0 * Math.PI / 2.0);

        // Fill using edge table
        int[] pixelData = surface.getPixelDataAsInt32Array();
        et.fill(pixelData, surface.getWidth(), surface.getHeight(),
                encodedForeground, surface.getFormat(), EdgeTable.FILL_RULE_EVENODD,
                composite, clip);

        // Return edge table to pool
        edgeTablePool.release(et);

        log.debug("fillRoundRect: completed");
    }

    /**
     * Draw oval outline using midpoint ellipse algorithm.
     * This draws the perimeter of an ellipse bounded by the specified rectangle.
     */
    public void drawOval(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            log.error("drawOval: invalid dimensions");
            return;
        }

        log.debug("drawOval: x={}, y={}, w={}, h={}", x, y, width, height);

        // Calculate center and radii
        int cx = x + width / 2;
        int cy = y + height / 2;
        int rx = width / 2;
        int ry = height / 2;

        // Apply transform
        if (!transform.isIdentity()) {
            Point2D.Float center = new Point2D.Float(cx, cy);
            transform.transform(center, center);
            cx = Math.round(center.x);
            cy = Math.round(center.y);
        }

        // Use midpoint ellipse algorithm to draw the outline
        drawEllipse(cx, cy, rx, ry);

        log.debug("drawOval: completed");
    }

    /**
     * Draw arc outline.
     * This draws the arc segment bounded by the specified rectangle.
     */
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        if (width <= 0 || height <= 0) {
            log.error("drawArc: invalid dimensions");
            return;
        }

        log.debug("drawArc: x={}, y={}, w={}, h={}, start={}, arc={}",
                x, y, width, height, startAngle, arcAngle);

        // Calculate center and radii
        int cx = x + width / 2;
        int cy = y + height / 2;
        int rx = width / 2;
        int ry = height / 2;

        // Apply transform
        if (!transform.isIdentity()) {
            Point2D.Float center = new Point2D.Float(cx, cy);
            transform.transform(center, center);
            cx = Math.round(center.x);
            cy = Math.round(center.y);
        }

        // Convert angles from degrees to radians
        // Java AWT uses degrees with 0 at 3 o'clock, positive = counter-clockwise
        double startRad = -Math.toRadians(startAngle);
        double endRad = -Math.toRadians(startAngle + arcAngle);

        // Draw arc using parametric equations
        drawArcSegment(cx, cy, rx, ry, startRad, endRad);

        log.debug("drawArc: completed");
    }

    /**
     * Draw rounded rectangle outline.
     */
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        if (width <= 0 || height <= 0) {
            log.error("drawRoundRect: invalid dimensions");
            return;
        }

        log.debug("drawRoundRect: x={}, y={}, w={}, h={}, arcW={}, arcH={}",
                x, y, width, height, arcWidth, arcHeight);

        // Clamp arc dimensions
        if (arcWidth > width)
            arcWidth = width;
        if (arcHeight > height)
            arcHeight = height;

        int rx = arcWidth / 2;
        int ry = arcHeight / 2;

        // Apply transform
        if (!transform.isIdentity()) {
            Point2D.Float topLeft = new Point2D.Float(x, y);
            transform.transform(topLeft, topLeft);
            x = Math.round(topLeft.x);
            y = Math.round(topLeft.y);
        }

        // Draw four corner arcs and four straight edges
        // Top edge
        drawLine(x + rx, y, x + width - rx, y);

        // Top-right corner arc (0 to 90 degrees)
        drawArcSegment(x + width - rx, y + ry, rx, ry, -Math.PI / 2.0, 0.0);

        // Right edge
        drawLine(x + width, y + ry, x + width, y + height - ry);

        // Bottom-right corner arc (90 to 180 degrees)
        drawArcSegment(x + width - rx, y + height - ry, rx, ry, 0.0, Math.PI / 2.0);

        // Bottom edge
        drawLine(x + width - rx, y + height, x + rx, y + height);

        // Bottom-left corner arc (180 to 270 degrees)
        drawArcSegment(x + rx, y + height - ry, rx, ry, Math.PI / 2.0, Math.PI);

        // Left edge
        drawLine(x, y + height - ry, x, y + ry);

        // Top-left corner arc (270 to 360 degrees)
        drawArcSegment(x + rx, y + ry, rx, ry, Math.PI, 3.0 * Math.PI / 2.0);

        log.debug("drawRoundRect: completed");
    }

    /**
     * Draw polyline - a sequence of connected line segments.
     */
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        if (xPoints == null || yPoints == null || nPoints < 2) {
            log.error("drawPolyline: invalid parameters");
            return;
        }

        log.debug("drawPolyline: nPoints={}", nPoints);

        // Draw lines connecting consecutive points (but don't close the polygon)
        for (int i = 1; i < nPoints; i++) {
            drawLine(xPoints[i - 1], yPoints[i - 1], xPoints[i], yPoints[i]);
        }

        log.debug("drawPolyline: completed");
    }

    /**
     * Copy an area of the surface to another location.
     */
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        if (width <= 0 || height <= 0) {
            log.error("copyArea: invalid dimensions");
            return;
        }

        log.debug("copyArea: x={}, y={}, w={}, h={}, dx={}, dy={}", x, y, width, height, dx, dy);

        // Apply transform to source coordinates
        Point2D.Float srcTopLeft = new Point2D.Float(x, y);
        if (!transform.isIdentity()) {
            transform.transform(srcTopLeft, srcTopLeft);
        }
        int srcX = Math.round(srcTopLeft.x);
        int srcY = Math.round(srcTopLeft.y);

        // Destination is source + delta (in device space)
        int dstX = srcX + dx;
        int dstY = srcY + dy;

        // Clip source and destination to surface bounds
        int surfaceWidth = surface.getWidth();
        int surfaceHeight = surface.getHeight();

        int copyWidth = width;
        int copyHeight = height;

        // Clip source rectangle
        if (srcX < 0) {
            copyWidth += srcX;
            dstX -= srcX;
            srcX = 0;
        }
        if (srcY < 0) {
            copyHeight += srcY;
            dstY -= srcY;
            srcY = 0;
        }
        if (srcX + copyWidth > surfaceWidth) {
            copyWidth = surfaceWidth - srcX;
        }
        if (srcY + copyHeight > surfaceHeight) {
            copyHeight = surfaceHeight - srcY;
        }

        // Clip destination rectangle
        if (dstX < 0) {
            copyWidth += dstX;
            srcX -= dstX;
            dstX = 0;
        }
        if (dstY < 0) {
            copyHeight += dstY;
            srcY -= dstY;
            dstY = 0;
        }
        if (dstX + copyWidth > surfaceWidth) {
            copyWidth = surfaceWidth - dstX;
        }
        if (dstY + copyHeight > surfaceHeight) {
            copyHeight = surfaceHeight - dstY;
        }

        if (copyWidth <= 0 || copyHeight <= 0) {
            log.debug("copyArea: clipped out entirely");
            return;
        }

        // Get pixel data
        int[] pixelData = surface.getPixelDataAsInt32Array();
        if (pixelData == null) {
            return;
        }

        // Copy pixels - need to handle overlapping regions
        // If the regions overlap and we're copying down or right, copy from bottom-right to top-left
        boolean overlaps = !(dstX + copyWidth <= srcX || dstX >= srcX + copyWidth ||
                dstY + copyHeight <= srcY || dstY >= srcY + copyHeight);

        if (overlaps && (dy > 0 || (dy == 0 && dx > 0))) {
            // Copy from bottom-right to top-left
            for (int row = copyHeight - 1; row >= 0; row--) {
                for (int col = copyWidth - 1; col >= 0; col--) {
                    int srcIdx = (srcY + row) * surfaceWidth + (srcX + col);
                    int dstIdx = (dstY + row) * surfaceWidth + (dstX + col);
                    pixelData[dstIdx] = pixelData[srcIdx];
                }
            }
        } else {
            // Copy from top-left to bottom-right (normal case)
            for (int row = 0; row < copyHeight; row++) {
                for (int col = 0; col < copyWidth; col++) {
                    int srcIdx = (srcY + row) * surfaceWidth + (srcX + col);
                    int dstIdx = (dstY + row) * surfaceWidth + (dstX + col);
                    pixelData[dstIdx] = pixelData[srcIdx];
                }
            }
        }

        log.debug("copyArea: completed");
    }

    /**
     * Helper method to draw an ellipse outline using midpoint algorithm.
     */
    private void drawEllipse(int cx, int cy, int rx, int ry) {
        int[] pixelData = surface.getPixelDataAsInt32Array();
        if (pixelData == null) {
            return;
        }

        int surfaceWidth = surface.getWidth();
        int surfaceHeight = surface.getHeight();
        boolean blend = needsBlending();

        // Midpoint ellipse algorithm
        int x = 0;
        int y = ry;
        int rx2 = rx * rx;
        int ry2 = ry * ry;
        int twoRx2 = 2 * rx2;
        int twoRy2 = 2 * ry2;

        // Region 1
        int p1 = (int) (ry2 - rx2 * ry + 0.25 * rx2);
        int px = 0;
        int py = twoRx2 * y;

        while (px < py) {
            plotEllipsePoints(pixelData, surfaceWidth, surfaceHeight, cx, cy, x, y, blend);
            x++;
            px += twoRy2;
            if (p1 < 0) {
                p1 += ry2 + px;
            } else {
                y--;
                py -= twoRx2;
                p1 += ry2 + px - py;
            }
        }

        // Region 2
        int p2 = (int) (ry2 * (x + 0.5) * (x + 0.5) + rx2 * (y - 1) * (y - 1) - rx2 * ry2);
        while (y >= 0) {
            plotEllipsePoints(pixelData, surfaceWidth, surfaceHeight, cx, cy, x, y, blend);
            y--;
            py -= twoRx2;
            if (p2 > 0) {
                p2 += rx2 - py;
            } else {
                x++;
                px += twoRy2;
                p2 += rx2 - py + px;
            }
        }
    }

    /**
     * Helper to plot the 4 symmetric points of an ellipse.
     */
    private void plotEllipsePoints(int[] pixelData, int width, int height, int cx, int cy, int x, int y,
            boolean blend) {
        plotPixel(pixelData, width, height, cx + x, cy + y, blend);
        plotPixel(pixelData, width, height, cx - x, cy + y, blend);
        plotPixel(pixelData, width, height, cx + x, cy - y, blend);
        plotPixel(pixelData, width, height, cx - x, cy - y, blend);
    }

    /**
     * Helper to plot a single pixel with clipping and optional blending.
     */
    private void plotPixel(int[] pixelData, int width, int height, int x, int y, boolean blend) {
        // Clip to surface bounds
        if (x < 0 || x >= width || y < 0 || y >= height) {
            return;
        }

        // Apply clip rectangle if set
        if (clip != null) {
            if (x < clip.x || x >= clip.x + clip.width || y < clip.y || y >= clip.y + clip.height) {
                return;
            }
        }

        int idx = y * width + x;
        if (blend) {
            int dstColor = pixelData[idx];
            pixelData[idx] = blendPixel(convertColorToARGB(encodedForeground, surface.getFormat()),
                    convertColorToARGB(dstColor, surface.getFormat()), composite);
        } else {
            pixelData[idx] = encodedForeground;
        }
    }

    /**
     * Helper to draw an arc segment using parametric equations.
     */
    private void drawArcSegment(int cx, int cy, int rx, int ry, double startAngle, double endAngle) {
        // Normalize angles
        while (startAngle < 0)
            startAngle += 2 * Math.PI;
        while (endAngle < 0)
            endAngle += 2 * Math.PI;
        while (startAngle > 2 * Math.PI)
            startAngle -= 2 * Math.PI;
        while (endAngle > 2 * Math.PI)
            endAngle -= 2 * Math.PI;

        // Handle wrapping
        if (endAngle < startAngle) {
            endAngle += 2 * Math.PI;
        }

        // Calculate step size based on arc length
        double angleDiff = Math.abs(endAngle - startAngle);
        double arcLength = angleDiff * Math.max(rx, ry);
        int steps = Math.max(10, (int) Math.ceil(arcLength / 2.0)); // At least 10 steps, or 2 pixels per step

        double angleStep = (endAngle - startAngle) / steps;
        int prevX = cx + (int) Math.round(rx * Math.cos(startAngle));
        int prevY = cy + (int) Math.round(ry * Math.sin(startAngle));

        for (int i = 1; i <= steps; i++) {
            double angle = startAngle + i * angleStep;
            int x = cx + (int) Math.round(rx * Math.cos(angle));
            int y = cy + (int) Math.round(ry * Math.sin(angle));
            drawLine(prevX, prevY, x, y);
            prevX = x;
            prevY = y;
        }
    }
}
