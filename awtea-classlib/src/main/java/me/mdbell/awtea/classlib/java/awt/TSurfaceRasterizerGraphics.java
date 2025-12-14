package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.Setter;
import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import me.mdbell.awtea.classlib.java.awt.image.TImageObserver;
import me.mdbell.awtea.font.FontPeer;
import me.mdbell.awtea.gfx.DefaultSurfaceBackend;
import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceCommand;
import me.mdbell.awtea.gfx.generated.Operation;
import org.teavm.jso.browser.Window;

import java.awt.*;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.List;

public class TSurfaceRasterizerGraphics extends TGraphics2D {

    /**
     * Padding around text glyphs when rendering to a temporary surface.
     * This ensures glyphs that extend beyond their nominal bounds are not clipped.
     */
    private static final int TEXT_SURFACE_PADDING = 4;

    protected transient boolean scheduled = false;
    protected transient boolean disposed = false;

    private final List<SurfaceCommand> surfaceCommandsA = new ArrayList<>();
    private final List<SurfaceCommand> surfaceCommandsB = new ArrayList<>();

    // Which one we are writing to right now
    private transient List<SurfaceCommand> writeList = surfaceCommandsA;
    // Which one we will read from during the blit
    private transient List<SurfaceCommand> readList = surfaceCommandsB;

    private transient SurfaceCommand previous = null;

    protected final Rasterizer rasterizer;

    @Getter(onMethod_ = @Override)
    protected final TAffineTransform transform = new TAffineTransform();

    @Getter(onMethod_ = @Override)
    @Setter(onMethod_ = @Override)
    protected TFont font;

    protected TRectangle clip;

    @Getter(onMethod_ = @Override)
    protected Color color;

    @Getter(onMethod_ = @Override)
    protected Color background;

    @Getter(onMethod_ = @Override)
    protected TComposite composite;

    public TSurfaceRasterizerGraphics(Rasterizer rasterizer) {
        this.rasterizer = rasterizer;
        reset();
    }

    protected TSurfaceRasterizerGraphics(TSurfaceRasterizerGraphics other) {
        super();
        this.rasterizer = other.rasterizer.create(); // Clone the rasterizer for independent state
        this.font = other.font;
        this.transform.setTransform(other.transform);
        this.clip = other.clip;
        this.color = other.color;
        this.background = other.background;
        this.composite = other.composite;
        // Don't copy disposed or scheduled state - new instance starts fresh
        this.disposed = false;
        this.scheduled = false;
    }

    @Override
    public TGraphics create() {
        return new TSurfaceRasterizerGraphics(this);
    }

    public final void pushOp(SurfaceCommand op) {
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
        composite = TAlphaComposite.SrcOver;
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
        pushOp(new SurfaceCommand(Operation.SET_TRANSFORM, new TAffineTransform(transform)));
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
        pushOp(new SurfaceCommand(Operation.CLEAR_RECT, x, y, width, height));
    }

    @Override
    public void clip(TRectangle r) {

    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {

    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        flush();
        rasterizer.dispose();
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
            pushOp(new SurfaceCommand(Operation.BLIT_IMAGE, img, x, y, width, height));
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
            pushOp(new SurfaceCommand(Operation.BLIT_IMAGE, img, x, y,
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
            pushOp(new SurfaceCommand(Operation.SET_CLIP_RECT, this.clip));
        }
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            this.clip = null;
            pushOp(new SurfaceCommand(Operation.SET_CLIP_RECT));
            return;
        }
        this.clip = new TRectangle(x, y, width, height);
        pushOp(new SurfaceCommand(Operation.SET_CLIP_RECT, this.clip));

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
        pushOp(new SurfaceCommand(Operation.SET_CLIP_RECT, this.clip));
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        pushOp(new SurfaceCommand(Operation.DRAW_LINE, null, x1, y1, x2, y2));
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
        if (str == null || str.isEmpty()) {
            return;
        }

        // Check for null font and color
        if (font == null || color == null) {
            return;
        }

        // Get font peer for rendering
        FontPeer peer = font.getFontPeer();
        if (peer == null) {
            return;
        }

        // Measure the string to determine surface size
        float sizePx = font.getSize();
        int textWidth = peer.measureString(str, sizePx);
        FontPeer.FontMetrics metrics = peer.getFontMetrics(sizePx);

        // Calculate surface dimensions with padding for glyphs that may extend beyond bounds
        int surfaceWidth = textWidth + TEXT_SURFACE_PADDING;
        int surfaceHeight = (int) Math.ceil(metrics.getAscent() + metrics.getDescent()) + TEXT_SURFACE_PADDING;

        if (surfaceWidth <= 0 || surfaceHeight <= 0) {
            return;
        }

        // Create a surface for rendering the text using the backend
        DefaultSurfaceBackend backend = DefaultSurfaceBackend.getDefault();
        Surface textSurface = backend.createFontRenderSurface(surfaceWidth, surfaceHeight);

        if (textSurface == null) {
            // If surface creation failed, silently return
            return;
        }

        // Create a TBufferedImage wrapper for the surface to act as a RasterTarget
        // The TBufferedImage will own the surface and destroy it when garbage collected
        TBufferedImage textImage = new TBufferedImage(textSurface);

        // Clear the surface to fully transparent (ARGB format)
        // This ensures text is rendered with transparent background
        TGraphics g = textImage.getGraphics();
        g.setColor(new Color(0, 0, 0, 0)); // Fully transparent
        g.fillRect(0, 0, surfaceWidth, surfaceHeight);
        g.dispose();

        // Convert AWT Color to ARGB int
        int argb = (color.getAlpha() << 24) | (color.getRed() << 16) |
                (color.getGreen() << 8) | color.getBlue();

        // Render the string to the surface
        // Position text with half padding as offset, and baseline at ascent + half padding
        int halfPadding = TEXT_SURFACE_PADDING / 2;
        int renderX = halfPadding;
        int renderY = surfaceHeight - halfPadding;
        peer.renderString(str, textImage, sizePx, renderX, renderY, argb);

        // Blit the rendered text surface to the screen
        // The surface baseline is at renderY, and we want it at the destination y coordinate
        // So destination y = y (desired baseline) - renderY (baseline within surface)
        int destX = x - halfPadding;
        int destY = y - renderY;
        pushOp(new SurfaceCommand(Operation.BLIT_IMAGE, textImage,
                destX, destY, surfaceWidth, surfaceHeight));

        // Note: textImage and its surface will be garbage collected after the blit operation completes
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        // Convert iterator to string and delegate to drawString(String, int, int)
        StringBuilder sb = new StringBuilder();
        for (char c = iterator.first(); c != AttributedCharacterIterator.DONE; c = iterator.next()) {
            sb.append(c);
        }
        drawString(sb.toString(), (int) x, (int) y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        // Convert iterator to string and delegate to drawString(String, int, int)
        StringBuilder sb = new StringBuilder();
        for (char c = iterator.first(); c != AttributedCharacterIterator.DONE; c = iterator.next()) {
            sb.append(c);
        }
        drawString(sb.toString(), x, y);
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        rectInternal(Operation.DRAW_RECT, x, y, width, height);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        rectInternal(Operation.FILL_RECT, x, y, width, height);
    }

    private void rectInternal(Operation opType, int x, int y, int width, int height) {
        if (width <= 0 || height <= 0 || (clip != null && !clip.intersects(x, y, width, height))) {
            return;
        }
        pushOp(new SurfaceCommand(opType, x, y, width, height));
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
        pushOp(new SurfaceCommand(Operation.SET_COLOR, bg, 1));
    }

    @Override
    public void setColor(Color c) {
        if (c == null) {
            c = Color.WHITE;
        }
        this.color = c;
        pushOp(new SurfaceCommand(Operation.SET_COLOR, c, 0));
    }

    @Override
    public void setPaint(TPaint paint) {

    }

    @Override
    public void setComposite(TComposite comp) {
        if (comp == null) {
            comp = TAlphaComposite.SrcOver;
        }
        this.composite = comp;
        pushOp(new SurfaceCommand(Operation.SET_COMPOSITE, comp));
    }

    @Override
    public void setPaintMode() {

    }

    @Override
    public void setXORMode(Color c1) {

    }

    public void flush() {
        if (disposed) {
            return; // Don't flush if already disposed
        }

        // Swap lists
        List<SurfaceCommand> temp = readList;
        readList = writeList;
        writeList = temp;

        previous = null;
        writeList.clear();

        rasterizer.rasterizeCommands(readList);

        scheduled = false;
    }

    // Schedule rasterization on the next animation frame

    protected final void scheduleRasterize() {
        Window.requestAnimationFrame(time -> {
            flush();
        });
    }

    private boolean coalesce(SurfaceCommand previous, SurfaceCommand requested) {
        if (requested.type == Operation.NO_OP) {
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
            case DRAW_LINE: // not a rect, but has same arg structure
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
