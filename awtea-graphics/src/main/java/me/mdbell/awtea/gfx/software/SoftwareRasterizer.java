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
 * This rasterizer supports all standard SurfaceCommand operations and can read/write
 * all pixel formats (ARGB, RGB, RGBA, ABGR, BGR) through format conversion logic.
 * <p>
 * Note: The parent SoftwareSurface can only be created with ARGB, RGB, or BGR formats,
 * but this rasterizer can blit from surfaces with any format via automatic conversion.
 * <p>
 * Transform support: Currently only translation is implemented. Full affine transforms
 * (scale, rotation, shear) would require more complex scan conversion and are deferred
 * as a future enhancement for this software fallback renderer.
 * <p>
 * Alpha blending: Supports standard Porter-Duff compositing rules via AlphaComposite.
 * The default composite is SRC_OVER with alpha = 1.0.
 */
//@Monitored.AllMethods
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

    // Color caching: store both Color objects and their encoded values for current format
    private Color foreground = Color.WHITE;
    private Color background = Color.BLACK;
    private int encodedForeground = 0;
    private int encodedBackground = 0;
    private int cachedFormat = -1; // Track which format the encoded colors are for

    // Compositing state
    private Composite composite = AlphaComposite.SrcOver;
    private boolean needsBlending = false; // Cached result to avoid repeated instanceof checks


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
        // Clone the clip rectangle manually (Rectangle copy constructor not available in TeaVM)
        this.clip = other.clip != null ? new Rectangle(other.clip.x, other.clip.y, other.clip.width, other.clip.height) : null;
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
                case SET_COMPOSITE:
                    Composite comp = (Composite) cmd.obj;
                    this.composite = comp != null ? comp : AlphaComposite.SrcOver;
                    updateBlendingCache();
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

        // fast path, rectangle is completely outside clip
        if (clip != null) {
            if (x + width <= clip.x || x >= clip.x + clip.width ||
                    y + height <= clip.y || y >= clip.y + clip.height) {
                return;
            }
        }

        int x0 = clipX(x);
        int y0 = clipY(y);
        int x1 = clipX(x + width - 1);
        int y1 = clipY(y + height - 1);

        if (x0 >= x1 || y0 >= y1) {
            return;
        }

        Uint8ClampedArray pixels = surface.getPixelData();
        if (pixels == null) {
            return;
        }
        int[] pixelDataAsInt32 = surface.getPixelDataAsInt32Array();

        int format = surface.getFormat();

        if (transform.isIdentity()) {
            x0 += (int) transform.getTranslateX();
            y0 += (int) transform.getTranslateY();
            x1 += (int) transform.getTranslateX();
            y1 += (int) transform.getTranslateY();
        } else {
            Point2D p1 = new Point2D.Float(x0, y0);
            Point2D p2 = new Point2D.Float(x1, y1);
            transform.transform(p1, p1);
            transform.transform(p2, p2);
            x0 = Math.round((float) p1.getX());
            y0 = Math.round((float) p1.getY());
            x1 = Math.round((float) p2.getX());
            y1 = Math.round((float) p2.getY());
        }

        boolean blend = needsBlending();

        int surfaceWidth = surface.getWidth();

        if (blend) {
            int srcColorARGB = convertColorToARGB(encodedForeground, format);
            for (int row = y0; row <= y1; row++) {
                for (int col = x0; col <= x1; col++) {
                    int dstColor = pixelDataAsInt32[row * surfaceWidth + col];
                    pixelDataAsInt32[row * surfaceWidth + col] = blendPixel(srcColorARGB, convertColorToARGB(dstColor, format), composite);
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

        // fast path, both points are outside clip
        if (clip != null) {
            if ((x1 < clip.x && x2 < clip.x) ||
                    (x1 >= clip.x + clip.width && x2 >= clip.x + clip.width) ||
                    (y1 < clip.y && y2 < clip.y) ||
                    (y1 >= clip.y + clip.height && y2 >= clip.y + clip.height)) {
                return;
            }
        }

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
                pixelDataAsInt32[idx] = blendPixel(convertColorToARGB(encodedForeground, surface.getFormat()), convertColorToARGB(dstColor, surface.getFormat()), composite);
            } else {
                pixelDataAsInt32[idx] = encodedForeground;
            }
            return;
        }

        Point2D p1 = new Point2D.Float(x1, y1);
        Point2D p2 = new Point2D.Float(x2, y2);
        if (!transform.isIdentity()) {
            transform.transform(p1, p1);
            transform.transform(p2, p2);
        }

        int[] pixelDataAsInt32 = surface.getPixelDataAsInt32Array();

        x1 = Math.round((float) p1.getX());
        y1 = Math.round((float) p1.getY());
        x2 = Math.round((float) p2.getX());
        y2 = Math.round((float) p2.getY());

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
                pixelDataAsInt32[idx] = blendPixel(convertColorToARGB(encodedForeground, surface.getFormat()), convertColorToARGB(dstColor, surface.getFormat()), composite);
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

    private void blitImage(Surface srcSurface, int destX, int destY, int destWidth, int destHeight) {
        if (srcSurface == null) {
            return;
        }
        // fast path, rectangle is completely outside clip
        if (clip != null) {
            if (destX + destWidth <= clip.x || destX >= clip.x + clip.width ||
                    destY + destHeight <= clip.y || destY >= clip.y + clip.height) {
                return;
            }
        }
        blitImage(srcSurface, 0, 0,
                srcSurface.getWidth(), srcSurface.getHeight(),
                destX, destY, destWidth, destHeight);
//        if (srcSurface == null) {
//            return;
//        }
//
//        // fast path, rectangle is completely outside clip
//        if (clip != null) {
//            if (destX + destWidth <= clip.x || destX >= clip.x + clip.width ||
//                    destY + destHeight <= clip.y || destY >= clip.y + clip.height) {
//                return;
//            }
//        }
//
//        Point2D pt = new Point2D.Float(destX, destY);
//        if (!transform.isIdentity()) {
//            transform.transform(pt, pt);
//            destX = Math.round((float) pt.getX());
//            destY = Math.round((float) pt.getY());
//        } else {
//            destX += (int) transform.getTranslateX();
//            destY += (int) transform.getTranslateY();
//        }
//
//        // For simplicity, only support 1:1 pixel mapping (no scaling)
//        Uint8ClampedArray srcPixArray = srcSurface.getPixelData();
//        int[] destPixels = surface.getPixelDataAsInt32Array();
//
//        if (srcPixArray == null || destPixels == null) {
//            return;
//        }
//        int[] srcPixels = new Int32Array(srcPixArray.getBuffer(), srcPixArray.getByteOffset(),
//                srcPixArray.getLength() / 4).toJavaArray();
//
//        int srcFormat = srcSurface.getFormat();
//        int destFormat = surface.getFormat();
//        int surfaceWidth = surface.getWidth();
//
//        for (int row = 0; row < destHeight; row++) {
//            for (int col = 0; col < destWidth; col++) {
//                int srcIdx = row * destWidth + col;
//                int destIdx = (destY + row) * surfaceWidth + (destX + col);
//
//                int srcColor = srcPixels[srcIdx];
//                int convertedColor = convertColor(srcColor, srcFormat, destFormat);
//
//                if (needsBlending()) {
//                    int dstColor = destPixels[destIdx];
//                    int blendedColor = blendPixel(convertColorToARGB(convertedColor, destFormat),
//                            convertColorToARGB(dstColor, destFormat), composite);
//                    destPixels[destIdx] = blendedColor;
//                } else {
//                    destPixels[destIdx] = convertedColor;
//                }
//            }
//        }
    }

    private void blitImage(Surface surface, int srcX, int srcY,
                           int srcWidth, int srcHeight,
                           int destX, int destY, int destWidth, int destHeight) {
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

        // Apply translation from transform
        Point2D pt = new Point2D.Float(destX, destY);
        if (!transform.isIdentity()) {
            transform.transform(pt, pt);
            destX = Math.round((float) pt.getX());
            destY = Math.round((float) pt.getY());
        } else {
            destX += (int) transform.getTranslateX();
            destY += (int) transform.getTranslateY();
        }

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
                // Source over destination (default blending  - inline calculation for clarity)
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
}
