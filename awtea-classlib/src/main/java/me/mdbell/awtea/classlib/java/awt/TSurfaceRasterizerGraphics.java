package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.Setter;
import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import me.mdbell.awtea.classlib.java.awt.image.TImageObserver;
import me.mdbell.awtea.font.FontPeer;
import me.mdbell.awtea.gfx.SurfaceBackendFactory;
import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceCommand;
import me.mdbell.awtea.gfx.generated.Operation;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.browser.Window;

import java.awt.*;
import java.text.AttributedCharacterIterator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TSurfaceRasterizerGraphics extends TGraphics2D {

    private static final Logger log = LoggerFactory.getLogger(TSurfaceRasterizerGraphics.class);

    /**
     * Padding around text glyphs when rendering to a temporary surface.
     * This ensures glyphs that extend beyond their nominal bounds are not clipped.
     */
    private static final int TEXT_SURFACE_PADDING = 4;

    /**
     * Maximum number of SurfaceCommand objects to keep in the pool.
     * This prevents unbounded memory growth while still providing pooling benefits.
     */
    private static final int MAX_POOL_SIZE = 128;

    protected transient boolean scheduled = false;
    protected transient boolean disposed = false;

    // Track if this Graphics instance set up a WebGL shader context
    private transient boolean ownsShaderContext = false;

    private final List<SurfaceCommand> surfaceCommandsA = new ArrayList<>();
    private final List<SurfaceCommand> surfaceCommandsB = new ArrayList<>();

    // Object pool for SurfaceCommand instances to reduce GC pressure
    private final ArrayDeque<SurfaceCommand> commandPool = new ArrayDeque<>();

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
        initializeShaderContext();
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

    /**
     * Sets the active component ID for GPU-based picking.
     * This is called before painting each component so the picking buffer
     * knows which component painted which pixels.
     * 
     * @param componentId the component ID
     */
    public void setActiveComponentId(int componentId) {
        // Check if rasterizer supports picking
        if (rasterizer instanceof me.mdbell.awtea.gfx.PickingRasterizer) {
            ((me.mdbell.awtea.gfx.PickingRasterizer) rasterizer).setActiveComponentId(componentId);
        }
    }

    /**
     * Enables or disables picking mode on the rasterizer.
     * 
     * @param enabled true to enable picking mode
     */
    public void setPickingEnabled(boolean enabled) {
        if (rasterizer instanceof me.mdbell.awtea.gfx.PickingRasterizer) {
            ((me.mdbell.awtea.gfx.PickingRasterizer) rasterizer).setPickingEnabled(enabled);
        }
    }

    /**
     * Returns whether picking mode is currently enabled.
     * 
     * @return true if picking is enabled
     */
    public boolean isPickingEnabled() {
        if (rasterizer instanceof me.mdbell.awtea.gfx.PickingRasterizer) {
            return ((me.mdbell.awtea.gfx.PickingRasterizer) rasterizer).isPickingEnabled();
        }
        return false;
    }
    
    /**
     * Gets the underlying rasterizer.
     * Used to check capabilities like picking support.
     * 
     * @return the rasterizer
     */
    public Rasterizer getRasterizer() {
        return rasterizer;
    }

    /**
     * Acquires a SurfaceCommand from the pool, or creates a new one if the pool is
     * empty.
     * This reduces GC pressure during high-frequency rendering operations.
     * 
     * @return a SurfaceCommand instance ready to be configured
     */
    private SurfaceCommand acquireCommand() {
        SurfaceCommand cmd = commandPool.poll();
        if (cmd == null) {
            cmd = new SurfaceCommand();
        }
        return cmd;
    }

    /**
     * Returns a SurfaceCommand to the pool for reuse, if the pool is not full.
     * The command is reset to prevent memory leaks from retained references.
     * 
     * @param cmd the command to return to the pool
     */
    private void releaseCommand(SurfaceCommand cmd) {
        if (cmd == null) {
            return;
        }
        // Reset the command to clear any object references (images, transforms, etc.)
        cmd.reset();

        // Only add to pool if we haven't exceeded the max size
        if (commandPool.size() < MAX_POOL_SIZE) {
            commandPool.offer(cmd);
        }
        // Otherwise, let it be garbage collected
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

    /**
     * Initializes the WebGL shader context if the rasterizer supports it.
     * This makes the context available during paint() calls via
     * getCurrentContext().
     */
    private void initializeShaderContext() {
        // Check if this is a WebGL rasterizer
        if (rasterizer instanceof me.mdbell.awtea.gfx.webgl.WebGLRasterizer) {
            me.mdbell.awtea.gfx.webgl.WebGLRasterizer webglRasterizer = (me.mdbell.awtea.gfx.webgl.WebGLRasterizer) rasterizer;
            me.mdbell.awtea.gfx.webgl.WebGLSurfaceBackend backend = webglRasterizer.getBackend();
            me.mdbell.awtea.gfx.webgl.WebGLShaderContext context = new me.mdbell.awtea.gfx.webgl.WebGLShaderContext(
                    backend, rasterizer);
            me.mdbell.awtea.gfx.webgl.WebGLShaderContext.setCurrentContext(context);
            ownsShaderContext = true;
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
        // TODO: clear ops?
    }

    @Override
    public boolean hit(TRectangle rect, TShape s, boolean onStroke) {
        return false;
    }

    @Override
    public void draw(TShape s) {
        // TODO: Implement full Shape drawing support
        // Currently unsupported - would require proper path stroking logic
        // For now, specific shape types (Rectangle, Polygon, etc.) should use their
        // specific draw methods
        // @see
        // https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics2D.html#draw-java.awt.Shape-

        // Partial support: If the shape is a known type, delegate to appropriate method
        if (s instanceof TRectangle) {
            TRectangle r = (TRectangle) s;
            drawRect(r.x, r.y, r.width, r.height);
        } else if (s instanceof TPolygon) {
            TPolygon p = (TPolygon) s;
            drawPolygon(p.xpoints, p.ypoints, p.npoints);
        } else {
            // Generic shape drawing not yet implemented
            // Would require converting shape to path and stroking it
            log.debug("draw(Shape): Unsupported shape type: {}", s.getClass().getName());
        }
    }

    @Override
    public void fill(TShape s) {
        // TODO: Implement full Shape filling support
        // Currently unsupported - would require proper path filling logic
        // For now, specific shape types (Rectangle, Polygon, etc.) should use their
        // specific fill methods
        // @see
        // https://docs.oracle.com/javase/8/docs/api/java/awt/Graphics2D.html#fill-java.awt.Shape-

        // Partial support: If the shape is a known type, delegate to appropriate method
        if (s instanceof TRectangle) {
            TRectangle r = (TRectangle) s;
            fillRect(r.x, r.y, r.width, r.height);
        } else if (s instanceof TPolygon) {
            TPolygon p = (TPolygon) s;
            fillPolygon(p.xpoints, p.ypoints, p.npoints);
        } else {
            // Generic shape filling not yet implemented
            // Would require converting shape to path and filling it
            log.debug("fill(Shape): Unsupported shape type: {}", s.getClass().getName());
        }
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
        SurfaceCommand cmd = acquireCommand();
        cmd.configure(Operation.SET_TRANSFORM, new TAffineTransform(transform));
        pushOp(cmd);
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
        SurfaceCommand cmd = acquireCommand();
        cmd.configure(Operation.CLEAR_RECT, null, x, y, width, height);
        pushOp(cmd);
    }

    @Override
    public void clip(TRectangle r) {

    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        if (width <= 0 || height <= 0) {
            return;
        }
        SurfaceCommand cmd = acquireCommand();
        cmd.configure(Operation.COPY_AREA, null, x, y, width, height, dx, dy);
        pushOp(cmd);
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
        if (width <= 0 || height <= 0) {
            return;
        }
        SurfaceCommand cmd = acquireCommand();
        cmd.configure(Operation.DRAW_ARC, null, x, y, width, height, startAngle, arcAngle);
        pushOp(cmd);
    }

    @Override
    public boolean drawImage(TImage img, int x, int y, int width, int height, TImageObserver observer) {
        if (clip != null && !clip.intersects(x, y, width, height)) {
            return false;
        }
        if (img instanceof TBufferedImage) {
            SurfaceCommand cmd = acquireCommand();
            cmd.configure(Operation.BLIT_IMAGE, img, x, y, width, height);
            pushOp(cmd);
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
            SurfaceCommand cmd = acquireCommand();
            cmd.configure(Operation.BLIT_IMAGE, img, x, y,
                    img.getWidth(null), img.getHeight(null));
            pushOp(cmd);
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
            // Transform the clip to device coordinates before sending
            pushTransformedClip();
        }
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            this.clip = null;
            SurfaceCommand cmd = acquireCommand();
            cmd.configure(Operation.SET_CLIP_RECT);
            pushOp(cmd);
            return;
        }
        this.clip = new TRectangle(x, y, width, height);
        // Transform the clip to device coordinates before sending
        pushTransformedClip();

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
        // Transform the clip to device coordinates before sending
        if (this.clip == null) {
            SurfaceCommand cmd = acquireCommand();
            cmd.configure(Operation.SET_CLIP_RECT);
            pushOp(cmd);
        } else {
            pushTransformedClip();
        }
    }

    /**
     * Transform the current clip rectangle to device coordinates and push it to the
     * command buffer.
     * This is necessary because the clip is stored in user space but all
     * rasterizers expect it in
     * device coordinates.
     */
    private void pushTransformedClip() {
        if (clip == null) {
            SurfaceCommand cmd = acquireCommand();
            cmd.configure(Operation.SET_CLIP_RECT);
            pushOp(cmd);
            return;
        }

        // Transform the four corners of the clip rectangle
        double[] pts = new double[] {
                clip.x, clip.y, // top-left
                clip.x + clip.width, clip.y, // top-right
                clip.x, clip.y + clip.height, // bottom-left
                clip.x + clip.width, clip.y + clip.height // bottom-right
        };
        transform.transform(pts, 0, pts, 0, 4);

        // Find the bounding box of the transformed corners
        double minX = Math.min(Math.min(pts[0], pts[2]), Math.min(pts[4], pts[6]));
        double minY = Math.min(Math.min(pts[1], pts[3]), Math.min(pts[5], pts[7]));
        double maxX = Math.max(Math.max(pts[0], pts[2]), Math.max(pts[4], pts[6]));
        double maxY = Math.max(Math.max(pts[1], pts[3]), Math.max(pts[5], pts[7]));

        // Convert to integer rectangle in device coordinates
        int deviceX = (int) Math.floor(minX);
        int deviceY = (int) Math.floor(minY);
        int deviceWidth = Math.max(0, (int) Math.ceil(maxX) - deviceX);
        int deviceHeight = Math.max(0, (int) Math.ceil(maxY) - deviceY);

        // Create a transformed clip rectangle and push it
        TRectangle deviceClip = new TRectangle(deviceX, deviceY, deviceWidth, deviceHeight);
        SurfaceCommand cmd = acquireCommand();
        cmd.configure(Operation.SET_CLIP_RECT, deviceClip);
        pushOp(cmd);
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        SurfaceCommand cmd = acquireCommand();
        cmd.configure(Operation.DRAW_LINE, null, x1, y1, x2, y2);
        pushOp(cmd);
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        if (width <= 0 || height <= 0) {
            return;
        }
        SurfaceCommand cmd = acquireCommand();
        cmd.configure(Operation.DRAW_OVAL, null, x, y, width, height);
        pushOp(cmd);
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {

        int[] xpts = Arrays.copyOf(xPoints, nPoints);
        int[] ypts = Arrays.copyOf(yPoints, nPoints);

        TPolygon polygon = new TPolygon(xpts, ypts, nPoints);
        SurfaceCommand cmd = acquireCommand();
        cmd.configure(Operation.DRAW_POLYGON, polygon);
        pushOp(cmd);
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {

        int[] xpts = Arrays.copyOf(xPoints, nPoints);
        int[] ypts = Arrays.copyOf(yPoints, nPoints);

        TPolygon polygon = new TPolygon(xpts, ypts, nPoints);
        SurfaceCommand cmd = acquireCommand();
        cmd.configure(Operation.FILL_POLYGON, polygon);
        pushOp(cmd);
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        if (xPoints == null || yPoints == null || nPoints < 2) {
            return;
        }
        int[] xpts = Arrays.copyOf(xPoints, nPoints);
        int[] ypts = Arrays.copyOf(yPoints, nPoints);

        TPolygon polygon = new TPolygon(xpts, ypts, nPoints);
        SurfaceCommand cmd = acquireCommand();
        cmd.configure(Operation.DRAW_POLYLINE, polygon);
        pushOp(cmd);
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        if (width <= 0 || height <= 0) {
            return;
        }
        SurfaceCommand cmd = acquireCommand();
        cmd.configure(Operation.DRAW_ROUND_RECT, null, x, y, width, height, arcWidth, arcHeight);
        pushOp(cmd);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        SurfaceCommand cmd = acquireCommand();
        cmd.configure(Operation.FILL_ROUND_RECT, null, x, y, width, height, arcWidth, arcHeight);
        pushOp(cmd);
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

        // Calculate surface dimensions with padding for glyphs that may extend beyond
        // bounds
        int surfaceWidth = textWidth + TEXT_SURFACE_PADDING;
        int surfaceHeight = (int) Math.ceil(metrics.getAscent() + metrics.getDescent()) + TEXT_SURFACE_PADDING;

        if (surfaceWidth <= 0 || surfaceHeight <= 0) {
            return;
        }

        // Create a surface for rendering the text using the backend
        Surface textSurface = SurfaceBackendFactory.getTextBackend().createFontRenderSurface(surfaceWidth,
                surfaceHeight);

        if (textSurface == null) {
            log.warn("Failed to create text surface for string: {}", str);
            return;
        }

        // Create a TBufferedImage wrapper for the surface
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
        // With atlas-based rendering, glyphs will be copied from the atlas rather than
        // rasterized from scratch, greatly reducing memory allocation pressure
        int halfPadding = TEXT_SURFACE_PADDING / 2;
        int renderX = halfPadding;
        int renderY = surfaceHeight - halfPadding;
        peer.renderString(str, textImage, sizePx, renderX, renderY, argb);

        // Blit the rendered text surface to the screen
        int destX = x - halfPadding;
        int destY = y - renderY;
        SurfaceCommand cmd = acquireCommand();
        cmd.configure(Operation.BLIT_IMAGE, textImage, destX, destY, surfaceWidth, surfaceHeight);
        pushOp(cmd);

        // Note: textImage and its surface will be garbage collected after the blit
        // operation completes
        // The memory pressure is now much lower because individual glyphs are cached in
        // the atlas
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
        SurfaceCommand cmd = acquireCommand();
        cmd.configure(opType, null, x, y, width, height);
        pushOp(cmd);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        SurfaceCommand cmd = acquireCommand();
        cmd.configure(Operation.FILL_ARC, null, x, y, width, height, startAngle, arcAngle);
        pushOp(cmd);
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        SurfaceCommand cmd = acquireCommand();
        cmd.configure(Operation.FILL_OVAL, null, x, y, width, height);
        pushOp(cmd);
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height) {

    }

    @Override
    public void setBackground(Color bg) {
        this.background = bg;
        SurfaceCommand cmd = acquireCommand();
        cmd.configure(Operation.SET_COLOR, bg, 1);
        pushOp(cmd);
    }

    @Override
    public void setColor(Color c) {
        if (c == null) {
            c = Color.WHITE;
        }
        this.color = c;
        SurfaceCommand cmd = acquireCommand();
        cmd.configure(Operation.SET_COLOR, c, 0);
        pushOp(cmd);
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
        SurfaceCommand cmd = acquireCommand();
        cmd.configure(Operation.SET_COMPOSITE, comp);
        pushOp(cmd);
    }

    @Override
    public void setPaintMode() {

    }

    @Override
    public void setXORMode(Color c1) {

    }

    public void flush() {
        // Swap lists
        List<SurfaceCommand> temp = readList;
        readList = writeList;
        writeList = temp;

        previous = null;
        writeList.clear();

        rasterizer.rasterizeCommands(readList);

        // Return commands to the pool after rasterization
        for (SurfaceCommand cmd : readList) {
            releaseCommand(cmd);
        }
        readList.clear();

        scheduled = false;

        // Clear WebGL shader context after flush completes (if we set it)
        if (ownsShaderContext) {
            clearShaderContext();
        }
    }

    /**
     * Clears the WebGL shader context if it was set by this Graphics instance.
     */
    private void clearShaderContext() {
        me.mdbell.awtea.gfx.webgl.WebGLShaderContext.setCurrentContext(null);
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
                // Compare arg count first, then compare only the valid portion
                if (previous.argCount != requested.argCount) {
                    return false;
                }
                for (int i = 0; i < previous.argCount; i++) {
                    if (previous.args[i] != requested.args[i]) {
                        return false;
                    }
                }
                return true;
            case SET_TRANSFORM:
            case SET_COLOR:
                previous.obj = requested.obj; // Update to the latest object
                return true;
            default:
                return false;
        }
    }
}
