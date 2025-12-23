package me.mdbell.awtea.gfx.webgl;

import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.gfx.PickingRasterizer;
import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceCommand;
import me.mdbell.awtea.gfx.SurfaceContainer;
import me.mdbell.awtea.instrument.Monitored;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Uint8ClampedArray;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLFramebuffer;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLTexture;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.List;

@Monitored.AllMethods
public class WebGLRasterizer implements Rasterizer, PickingRasterizer {

    private static final Logger log = LoggerFactory.getLogger(WebGLRasterizer.class);

    private final WebGLSurfaceBackend backend;
    private final WebGL2RenderingContext gl;
    private final WebGLSurface surface;
    private final WebGLFramebuffer framebuffer;

    private boolean pushToScreen = false;
    private final boolean isChildRasterizer;
    
    // Picking support: current component ID for dual rendering
    // Each component sets its ID before painting, and all its paint operations
    // are rendered to both the picking buffer (with ID color) and normal framebuffer
    private static final int INVALID_COMPONENT_ID = 0;
    private int activeComponentId = INVALID_COMPONENT_ID;
    private boolean pickingEnabled = false;

    WebGLRasterizer(WebGLSurfaceBackend backend, WebGLSurface surface, boolean pushToScreen) {
        this.backend = backend;
        this.framebuffer = surface.framebuffer;
        this.gl = backend.gl;
        this.surface = surface;
        backend.contextStack.getTransform().setToIdentity();
        backend.contextStack.setClip(new Rectangle(0, 0, surface.getWidth(), surface.getHeight()));
        this.pushToScreen = pushToScreen;
        this.isChildRasterizer = false; // Root rasterizer
    }

    private WebGLRasterizer(WebGLRasterizer other) {
        this.surface = other.surface;
        this.framebuffer = other.framebuffer;
        this.backend = other.backend;
        this.gl = other.gl;
        this.isChildRasterizer = true; // Child rasterizer
        this.pushToScreen = other.pushToScreen;
        this.activeComponentId = other.activeComponentId;
        this.pickingEnabled = other.pickingEnabled;
        
        // Save state on creation for isolation
        backend.contextStack.save();
    }
    
    /**
     * Sets the active component ID for picking buffer rendering.
     * Call this before a component starts painting.
     * 
     * @param componentId the component ID
     */
    @Override
    public void setActiveComponentId(int componentId) {
        this.activeComponentId = componentId;
        log.trace("Set active component ID to {}", componentId);
    }
    
    /**
     * Enables or disables picking buffer rendering.
     * When enabled, all paint operations are duplicated to the picking buffer.
     * 
     * @param enabled true to enable picking rendering
     */
    @Override
    public void setPickingEnabled(boolean enabled) {
        this.pickingEnabled = enabled;
    }
    
    /**
     * Returns whether picking mode is currently enabled.
     * 
     * @return true if picking is enabled
     */
    @Override
    public boolean isPickingEnabled() {
        return pickingEnabled;
    }

    @Override
    public Rasterizer create() {
        return new WebGLRasterizer(this);
    }

    @Override
    public void dispose() {
        // Only restore state if this is a child rasterizer
        if (isChildRasterizer) {
            backend.contextStack.restore();
        }
    }

    @Override
    public void reset() {
        backend.contextStack.getTransform().setToIdentity();
        backend.contextStack.setForeground(Color.WHITE);
        backend.contextStack.setBackground(Color.BLACK);
        backend.contextStack.setComposite(AlphaComposite.SrcOver);
        backend.contextStack.setClip(new Rectangle(0, 0, surface.getWidth(), surface.getHeight()));
    }

    private void fillRect(float x, float y, float width, float height) {
        int h = surface.getHeight();
        final float finalY = h - (y + height); // flip Y coordinate
        final float finalX = x;
        final float finalWidth = width;
        final float finalHeight = height;
        
        // If picking is enabled, render to picking buffer first with ID color
        if (pickingEnabled && activeComponentId != INVALID_COMPONENT_ID && backend.hasPickingBuffer()) {
            renderRectToPicking(activeComponentId, finalX, finalY, finalWidth, finalHeight);
        }
        
        // Render to normal framebuffer with actual colors
        useColorProgram();
        backend.setRectBuffer(finalX, finalY, finalWidth, finalHeight);
        gl.drawArrays(WebGLRenderingContext.TRIANGLES, 0, 6);

        surface.markDirty();
    }
    
    /**
     * Renders a rectangle to the picking buffer with the specified component ID color.
     */
    private void renderRectToPicking(int componentId, float x, float y, float width, float height) {
        WebGLPickingBuffer pickingBuffer = backend.getPickingBuffer();
        if (pickingBuffer == null) {
            return;
        }
        
        // Bind picking framebuffer
        gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, pickingBuffer.getFramebuffer());
        gl.viewport(0, 0, pickingBuffer.getWidth(), pickingBuffer.getHeight());
        
        // Encode component ID as color
        float[] idColor = PickingColorEncoder.encodeId(componentId);
        
        // Use color program in picking mode
        backend.useColorProgram(surface.getWidth(), surface.getHeight(), idColor);
        backend.setRectBuffer(x, y, width, height);
        gl.drawArrays(WebGLRenderingContext.TRIANGLES, 0, 6);
        
        // Restore original framebuffer
        gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, framebuffer);
        gl.viewport(0, 0, surface.getWidth(), surface.getHeight());
    }
    
    /**
     * Executes a render operation on the picking buffer with the specified component ID color.
     * Used for non-rectangle primitives (ovals, polygons, etc.)
     */
    private void renderToPicking(int componentId, Runnable renderOp) {
        WebGLPickingBuffer pickingBuffer = backend.getPickingBuffer();
        if (pickingBuffer == null) {
            return;
        }
        
        // Bind picking framebuffer
        gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, pickingBuffer.getFramebuffer());
        gl.viewport(0, 0, pickingBuffer.getWidth(), pickingBuffer.getHeight());
        
        // Encode component ID as color
        float[] idColor = PickingColorEncoder.encodeId(componentId);
        
        // Use color program in picking mode
        backend.useColorProgram(surface.getWidth(), surface.getHeight(), idColor);
        
        // Execute the render operation
        renderOp.run();
        
        // Restore original framebuffer
        gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, framebuffer);
        
        // Restore viewport
        gl.viewport(0, 0, surface.getWidth(), surface.getHeight());
    }
    
    /**
     * Renders a texture to the picking buffer using PICKING swizzle mode.
     * This ensures text and other surface-based content appears in the picking buffer
     * with the component's ID color.
     */
    private void renderTextureToPickingBuffer(WebGLTexture texture, int x, int y, 
            int srcW, int srcH, int width, int height, Uint8ClampedArray pixelData) {
        WebGLPickingBuffer pickingBuffer = backend.getPickingBuffer();
        if (pickingBuffer == null) {
            return;
        }
        
        // Bind picking framebuffer
        gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, pickingBuffer.getFramebuffer());
        gl.viewport(0, 0, pickingBuffer.getWidth(), pickingBuffer.getHeight());
        
        // Encode component ID as color for uniform
        float[] idColor = PickingColorEncoder.encodeId(activeComponentId);
        
        // Use texture program with PICKING swizzle mode
        backend.useTextureProgram(WebGLSurfaceBackend.SwizzleMode.PICKING, 
            surface.getWidth(), surface.getHeight(), idColor);
        
        // Flip Y for picking buffer
        int flippedY = surface.getHeight() - y - srcH;
        
        // Setup vertices and UVs
        float[] verts = {
                x, flippedY,
                x + width, flippedY,
                x, flippedY + height,
                x, flippedY + height,
                x + width, flippedY,
                x + width, flippedY + height
        };

        float[] uvs = {
                0f, 1f,
                1f, 1f,
                0f, 0f,
                0f, 0f,
                1f, 1f,
                1f, 0f
        };

        backend.uploadQuadVertices(verts, uvs);
        
        // Bind texture
        gl.activeTexture(WebGLRenderingContext.TEXTURE0);
        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, texture);

        if (pixelData != null) {
            gl.texImage2D(WebGLRenderingContext.TEXTURE_2D, 0, WebGLRenderingContext.RGBA,
                    srcW, srcH, 0, WebGLRenderingContext.RGBA,
                    WebGLRenderingContext.UNSIGNED_BYTE, pixelData);
        }

        // Render to picking buffer
        gl.drawArrays(WebGLRenderingContext.TRIANGLES, 0, 6);
        
        // Restore original framebuffer
        gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, framebuffer);
        gl.viewport(0, 0, surface.getWidth(), surface.getHeight());
        
        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, null);
    }

    private void drawRect(float x, float y, float width, float height, float lineWidth) {
        // draw 4 filled rects to make the border
        fillRect(x, y, width, lineWidth); // top
        fillRect(x, y + height - lineWidth, width, lineWidth); // bottom
        fillRect(x, y + lineWidth, lineWidth, height - 2 * lineWidth); // left
        fillRect(x + width - lineWidth, y + lineWidth, lineWidth, height - 2 * lineWidth); // right

        surface.markDirty();
    }

    private void useColorProgram() {
        int width = surface.getWidth();
        int height = surface.getHeight();

        backend.useColorProgram(width, height);
    }

    private void clearRect(int x, int y, int width, int height) {
        AffineTransform transform = backend.contextStack.getTransform();
        Color background = backend.contextStack.getBackground();
        int tx = (int) transform.getTranslateX();
        int ty = (int) transform.getTranslateY();
        int h = surface.getHeight();

        int cx = x + tx;
        int cy = y + ty;

        gl.enable(WebGLRenderingContext.SCISSOR_TEST);
        gl.scissor(cx, h - (cy + height), width, height);
        if (background != null) {
            float r = background.getRed() / 255.0f;
            float g = background.getGreen() / 255.0f;
            float b = background.getBlue() / 255.0f;
            float a = background.getAlpha() / 255.0f;
            gl.clearColor(r, g, b, a);
        } else {
            gl.clearColor(0f, 0f, 0f, 0f);
        }
        gl.clear(WebGLRenderingContext.COLOR_BUFFER_BIT);
        // Clip will be restored by next useProgram call which applies state
        
        surface.markDirty();
    }

    private void setClip(Shape shape) {
        if (shape == null) {
            backend.contextStack.setClip(null);
            return;
        }
        Rectangle bounds = shape.getBounds();
        backend.contextStack.setClip(bounds);
    }

    private void drawImage(Object img, int x, int y, int width, int height) {
        if (img instanceof WebGLSurface) {
            drawWebGLSurface((WebGLSurface) img, x, y, width, height);
        } else if (img instanceof Surface) {
            // generic Surface drawing (not optimized - gets copied into GPU texture and
            // then drawn)
            Surface surface = (Surface) img;
            drawSurface(surface, x, y, width, height);
        } else {
            log.error("WebGLRasterizer: drawImage: Unsupported image type: {}", img.getClass().getName());
        }
    }

    private void drawWebGLSurface(WebGLSurface img, int x, int y, int width, int height) {
        WebGLTexture other = img.texture;

        // no swizzling needed when drawing from one WebGLSurface to another
        drawTexture(other, WebGLSurfaceBackend.SwizzleMode.NONE, x, y, img.getWidth(), img.getHeight(), width, height,
                null);
    }

    private void drawSurface(Surface surface, int x, int y, int width, int height) {
        WebGLTexture tmp = gl.createTexture();
        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, tmp);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                WebGLRenderingContext.TEXTURE_WRAP_S,
                WebGLRenderingContext.CLAMP_TO_EDGE);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                WebGLRenderingContext.TEXTURE_WRAP_T,
                WebGLRenderingContext.CLAMP_TO_EDGE);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                WebGLRenderingContext.TEXTURE_MIN_FILTER,
                WebGLRenderingContext.LINEAR);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                WebGLRenderingContext.TEXTURE_MAG_FILTER,
                WebGLRenderingContext.LINEAR);
        WebGLSurfaceBackend.SwizzleMode mode = determineSwizzleMode(surface);
        try {
            drawTexture(tmp, mode, x, y, surface.getWidth(), surface.getHeight(), width, height,
                    surface.getPixelData());
        } finally {
            // clean up temporary texture
            gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, null);
            gl.deleteTexture(tmp);
        }
    }

    private WebGLSurfaceBackend.SwizzleMode determineSwizzleMode(Surface surface) {
        switch (surface.getFormat()) {
            case Surface.FORMAT_INT_ARGB:
                return WebGLSurfaceBackend.SwizzleMode.ARGB_TO_RGBA;
            case Surface.FORMAT_INT_RGB:
                return WebGLSurfaceBackend.SwizzleMode.RGB_TO_RGBA;
            case Surface.FORMAT_INT_BGR:
                return WebGLSurfaceBackend.SwizzleMode.BGR_TO_ABGR;
            case Surface.FORMAT_INT_RGBA:
                return WebGLSurfaceBackend.SwizzleMode.NONE;
            default:
                log.warn("WebGLRasterizer: Unknown surface format: {}, defaulting to no swizzling",
                        surface.getFormat());
                return WebGLSurfaceBackend.SwizzleMode.NONE;
        }
    }

    private void drawTexture(WebGLTexture texture, WebGLSurfaceBackend.SwizzleMode mode,
            int x, int y, int srcW, int srcH, int width, int height, Uint8ClampedArray pixelData) {
        
        // If picking is enabled, render to picking buffer with PICKING swizzle mode
        if (pickingEnabled && activeComponentId != INVALID_COMPONENT_ID && backend.hasPickingBuffer()) {
            renderTextureToPickingBuffer(texture, x, y, srcW, srcH, width, height, pixelData);
        }
        
        // Always render to normal framebuffer
        backend.useTextureProgram(mode, surface.getWidth(), surface.getHeight());

        y = surface.getHeight() - y - srcH;

        // the surface associated with this rasterizer already has its texture on the
        // GPU,
        // and we have already called gl.bindTexture for it at the start of
        // rasterization,

        // we can skip the texture upload when using a WebGLSurface, as its texture is
        // already on the GPU

        // TODO: optimize vertex buffer usage

        float[] verts = {
                x, y,
                x + width, y,
                x, y + height,
                x, y + height,
                x + width, y,
                x + width, y + height
        };

        float[] uvs = {
                0f, 1f,
                1f, 1f,
                0f, 0f,
                0f, 0f,
                1f, 1f,
                1f, 0f
        };

        backend.uploadQuadVertices(verts, uvs);

        // bind the texture
        gl.activeTexture(WebGLRenderingContext.TEXTURE0);
        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, texture);

        if (pixelData != null) {
            gl.texImage2D(WebGLRenderingContext.TEXTURE_2D, 0, WebGLRenderingContext.RGBA,
                    srcW, srcH, 0, WebGLRenderingContext.RGBA,
                    WebGLRenderingContext.UNSIGNED_BYTE, pixelData);

        }

        gl.drawArrays(WebGLRenderingContext.TRIANGLES, 0, 6);

        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, null);

        surface.markDirty();
    }

    private void drawLine(int x1, int y1, int x2, int y2) {
        // Use WebGL line primitive for efficient GPU rendering
        int h = surface.getHeight();
        useColorProgram();

        // Create line vertices (flip Y coordinates to WebGL space: Y=0 at bottom)
        float[] verts = {
                x1, h - y1,
                x2, h - y2
        };

        // Upload vertices to the already-bound rectBuffer
        ArrayBuffer vertBuf = Float32Array.fromJavaArray(verts).getBuffer();
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, vertBuf, WebGLRenderingContext.STREAM_DRAW);

        gl.drawArrays(WebGLRenderingContext.LINES, 0, 2);
        surface.markDirty();
    }

    private void drawPolygon(int[] xpoints, int[] ypoints, int npoints) {
        if (npoints < 2)
            return;

        // Use WebGL LINE_LOOP for efficient GPU rendering
        int h = surface.getHeight();
        useColorProgram();

        // Create vertices array (flip Y coordinates to WebGL space: Y=0 at bottom)
        float[] verts = new float[npoints * 2];
        for (int i = 0; i < npoints; i++) {
            verts[i * 2] = xpoints[i];
            verts[i * 2 + 1] = h - ypoints[i];
        }

        ArrayBuffer vertBuf = Float32Array.fromJavaArray(verts).getBuffer();
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, vertBuf, WebGLRenderingContext.STREAM_DRAW);

        gl.drawArrays(WebGLRenderingContext.LINE_LOOP, 0, npoints);
        surface.markDirty();
    }

    private void fillPolygon(int[] xpoints, int[] ypoints, int npoints) {
        if (npoints < 3)
            return;

        // Use WebGL TRIANGLE_FAN for efficient GPU polygon filling
        int h = surface.getHeight();

        // Create vertices array (flip Y coordinates to WebGL space: Y=0 at bottom)
        float[] verts = new float[npoints * 2];
        for (int i = 0; i < npoints; i++) {
            verts[i * 2] = xpoints[i];
            verts[i * 2 + 1] = h - ypoints[i];
        }

        // If picking is enabled, render to picking buffer first
        if (pickingEnabled && activeComponentId != INVALID_COMPONENT_ID && backend.hasPickingBuffer()) {
            final float[] finalVerts = verts;
            renderToPicking(activeComponentId, () -> {
                ArrayBuffer vertBuf = Float32Array.fromJavaArray(finalVerts).getBuffer();
                gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, vertBuf, WebGLRenderingContext.STREAM_DRAW);
                gl.drawArrays(WebGLRenderingContext.TRIANGLE_FAN, 0, npoints);
            });
        }

        // Render to normal framebuffer
        useColorProgram();
        ArrayBuffer vertBuf = Float32Array.fromJavaArray(verts).getBuffer();
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, vertBuf, WebGLRenderingContext.STREAM_DRAW);
        gl.drawArrays(WebGLRenderingContext.TRIANGLE_FAN, 0, npoints);
        
        surface.markDirty();
    }

    private void fillOval(int x, int y, int width, int height) {
        // Use triangle fan with parametric ellipse points for GPU rendering
        int cx = x + width / 2;
        int cy = y + height / 2;
        int rx = width / 2;
        int ry = height / 2;

        if (rx == 0 || ry == 0)
            return;

        int h = surface.getHeight();

        // Generate ellipse vertices using parametric equations
        // Use enough segments for smooth appearance
        int segments = Math.max(32, (Math.max(rx, ry) / 2));
        float[] verts = new float[(segments + 2) * 2]; // center + segments + first point again

        // Center point (flip Y to WebGL space)
        verts[0] = cx;
        verts[1] = h - cy;

        // Generate points around ellipse (flip Y to WebGL space)
        for (int i = 0; i <= segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            verts[(i + 1) * 2] = (float) (cx + rx * Math.cos(angle));
            verts[(i + 1) * 2 + 1] = (float) (h - (cy + ry * Math.sin(angle)));
        }

        // If picking is enabled, render to picking buffer first
        if (pickingEnabled && activeComponentId != INVALID_COMPONENT_ID && backend.hasPickingBuffer()) {
            final float[] finalVerts = verts;
            final int finalSegments = segments;
            renderToPicking(activeComponentId, () -> {
                ArrayBuffer vertBuf = Float32Array.fromJavaArray(finalVerts).getBuffer();
                gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, vertBuf, WebGLRenderingContext.STREAM_DRAW);
                gl.drawArrays(WebGLRenderingContext.TRIANGLE_FAN, 0, finalSegments + 2);
            });
        }

        // Render to normal framebuffer
        useColorProgram();
        ArrayBuffer vertBuf = Float32Array.fromJavaArray(verts).getBuffer();
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, vertBuf, WebGLRenderingContext.STREAM_DRAW);
        gl.drawArrays(WebGLRenderingContext.TRIANGLE_FAN, 0, segments + 2);
        
        surface.markDirty();
    }

    private void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        if (arcWidth == 0 || arcHeight == 0) {
            fillRect(x, y, width, height);
            return;
        }

        int rx = Math.min(arcWidth / 2, width / 2);
        int ry = Math.min(arcHeight / 2, height / 2);

        int h = surface.getHeight();

        // Build rounded rectangle as a single triangle fan
        // Segments per corner arc
        int segsPerCorner = Math.max(4, Math.max(rx, ry) / 4);
        // Calculate correct vertex count: center + edges + corners (each corner is
        // segsPerCorner+1 vertices)
        int totalVerts = 1 + 2 + 4 * (segsPerCorner + 1) + 4 + 1; // center + 2 top edges + 4 corners + 4 edges + 1
                                                                  // closing
        float[] verts = new float[totalVerts * 2];

        // Center point (flip Y to WebGL space)
        int cx = x + width / 2;
        int cy = y + height / 2;
        verts[0] = cx;
        verts[1] = h - cy;

        int idx = 1;

        // Top edge + top-right corner (flip Y to WebGL space)
        verts[idx * 2] = x + rx;
        verts[idx * 2 + 1] = h - y;
        idx++;

        verts[idx * 2] = x + width - rx;
        verts[idx * 2 + 1] = h - y;
        idx++;

        // Top-right corner arc (flip Y to WebGL space)
        for (int i = 0; i <= segsPerCorner; i++) {
            double angle = -Math.PI / 2 + (Math.PI / 2) * i / segsPerCorner;
            verts[idx * 2] = (float) (x + width - rx + rx * Math.cos(angle));
            verts[idx * 2 + 1] = (float) (h - (y + ry + ry * Math.sin(angle)));
            idx++;
        }

        // Right edge (flip Y to WebGL space)
        verts[idx * 2] = x + width;
        verts[idx * 2 + 1] = h - (y + height - ry);
        idx++;

        // Bottom-right corner arc (flip Y to WebGL space)
        for (int i = 0; i <= segsPerCorner; i++) {
            double angle = 0 + (Math.PI / 2) * i / segsPerCorner;
            verts[idx * 2] = (float) (x + width - rx + rx * Math.cos(angle));
            verts[idx * 2 + 1] = (float) (h - (y + height - ry + ry * Math.sin(angle)));
            idx++;
        }

        // Bottom edge (flip Y to WebGL space)
        verts[idx * 2] = x + rx;
        verts[idx * 2 + 1] = h - (y + height);
        idx++;

        // Bottom-left corner arc (flip Y to WebGL space)
        for (int i = 0; i <= segsPerCorner; i++) {
            double angle = Math.PI / 2 + (Math.PI / 2) * i / segsPerCorner;
            verts[idx * 2] = (float) (x + rx + rx * Math.cos(angle));
            verts[idx * 2 + 1] = (float) (h - (y + height - ry + ry * Math.sin(angle)));
            idx++;
        }

        // Left edge (flip Y to WebGL space)
        verts[idx * 2] = x;
        verts[idx * 2 + 1] = h - (y + ry);
        idx++;

        // Top-left corner arc (flip Y to WebGL space)
        for (int i = 0; i <= segsPerCorner; i++) {
            double angle = Math.PI + (Math.PI / 2) * i / segsPerCorner;
            verts[idx * 2] = (float) (x + rx + rx * Math.cos(angle));
            verts[idx * 2 + 1] = (float) (h - (y + ry + ry * Math.sin(angle)));
            idx++;
        }

        // Close to first edge point (flip Y to WebGL space)
        verts[idx * 2] = x + rx;
        verts[idx * 2 + 1] = h - y;
        idx++;

        final int finalIdx = idx;
        final float[] finalVerts = java.util.Arrays.copyOf(verts, idx * 2);
        
        // If picking is enabled, render to picking buffer first
        if (pickingEnabled && activeComponentId != INVALID_COMPONENT_ID && backend.hasPickingBuffer()) {
            renderToPicking(activeComponentId, () -> {
                ArrayBuffer vertBuf = Float32Array.fromJavaArray(finalVerts).getBuffer();
                gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, vertBuf, WebGLRenderingContext.STREAM_DRAW);
                gl.drawArrays(WebGLRenderingContext.TRIANGLE_FAN, 0, finalIdx);
            });
        }

        // Render to normal framebuffer
        useColorProgram();
        ArrayBuffer vertBuf = Float32Array.fromJavaArray(finalVerts).getBuffer();
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, vertBuf, WebGLRenderingContext.STREAM_DRAW);
        gl.drawArrays(WebGLRenderingContext.TRIANGLE_FAN, 0, idx);
        
        surface.markDirty();
    }

    private void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        int cx = x + width / 2;
        int cy = y + height / 2;
        int rx = width / 2;
        int ry = height / 2;

        if (rx == 0 || ry == 0)
            return;

        int h = surface.getHeight();

        // Convert angles to radians (Java AWT uses degrees, 0 at 3 o'clock, CCW
        // positive)
        double startRad = Math.toRadians(-startAngle); // negate for screen coords
        double endRad = Math.toRadians(-(startAngle + arcAngle));

        // Generate arc as triangle fan (center + arc points)
        int segments = Math.max(8, Math.abs(arcAngle) / 5); // ~5 degrees per segment
        float[] verts = new float[(segments + 2) * 2]; // center + arc points + close

        // Center point (flip Y to WebGL space)
        verts[0] = cx;
        verts[1] = h - cy;

        // Generate points along arc (flip Y to WebGL space)
        for (int i = 0; i <= segments; i++) {
            double angle = startRad + (endRad - startRad) * i / segments;
            verts[(i + 1) * 2] = (float) (cx + rx * Math.cos(angle));
            verts[(i + 1) * 2 + 1] = (float) (h - (cy - ry * Math.sin(angle)));
        }

        // If picking is enabled, render to picking buffer first
        if (pickingEnabled && activeComponentId != INVALID_COMPONENT_ID && backend.hasPickingBuffer()) {
            final float[] finalVerts = verts;
            final int finalSegments = segments;
            renderToPicking(activeComponentId, () -> {
                ArrayBuffer vertBuf = Float32Array.fromJavaArray(finalVerts).getBuffer();
                gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, vertBuf, WebGLRenderingContext.STREAM_DRAW);
                gl.drawArrays(WebGLRenderingContext.TRIANGLE_FAN, 0, finalSegments + 2);
            });
        }

        // Render to normal framebuffer
        useColorProgram();
        ArrayBuffer vertBuf = Float32Array.fromJavaArray(verts).getBuffer();
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, vertBuf, WebGLRenderingContext.STREAM_DRAW);
        gl.drawArrays(WebGLRenderingContext.TRIANGLE_FAN, 0, segments + 2);
        
        surface.markDirty();
    }

    private void drawOval(int x, int y, int width, int height) {
        // Use LINE_LOOP with parametric ellipse points for GPU rendering
        int cx = x + width / 2;
        int cy = y + height / 2;
        int rx = width / 2;
        int ry = height / 2;

        if (rx == 0 || ry == 0)
            return;

        int h = surface.getHeight();
        useColorProgram();

        // Generate ellipse vertices
        int segments = Math.max(32, (Math.max(rx, ry) / 2));
        float[] verts = new float[segments * 2];

        for (int i = 0; i < segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            verts[i * 2] = (float) (cx + rx * Math.cos(angle));
            verts[i * 2 + 1] = (float) (h - (cy + ry * Math.sin(angle)));
        }

        ArrayBuffer vertBuf = Float32Array.fromJavaArray(verts).getBuffer();
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, vertBuf, WebGLRenderingContext.STREAM_DRAW);

        gl.drawArrays(WebGLRenderingContext.LINE_LOOP, 0, segments);
        surface.markDirty();
    }

    private void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        int cx = x + width / 2;
        int cy = y + height / 2;
        int rx = width / 2;
        int ry = height / 2;

        if (rx == 0 || ry == 0)
            return;

        int h = surface.getHeight();
        useColorProgram();

        // Convert angles to radians (Java AWT uses degrees, 0 at 3 o'clock, CCW
        // positive)
        double startRad = Math.toRadians(-startAngle); // negate for screen coords
        double endRad = Math.toRadians(-(startAngle + arcAngle));

        // Generate arc as line strip
        int segments = Math.max(8, Math.abs(arcAngle) / 5); // ~5 degrees per segment
        float[] verts = new float[(segments + 1) * 2];

        for (int i = 0; i <= segments; i++) {
            double angle = startRad + (endRad - startRad) * i / segments;
            verts[i * 2] = (float) (cx + rx * Math.cos(angle));
            verts[i * 2 + 1] = (float) (h - (cy - ry * Math.sin(angle)));
        }

        ArrayBuffer vertBuf = Float32Array.fromJavaArray(verts).getBuffer();
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, vertBuf, WebGLRenderingContext.STREAM_DRAW);

        gl.drawArrays(WebGLRenderingContext.LINE_STRIP, 0, segments + 1);
        surface.markDirty();
    }

    private void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        if (arcWidth == 0 || arcHeight == 0) {
            drawRect(x, y, width, height, 1.0f);
            return;
        }

        int rx = Math.min(arcWidth / 2, width / 2);
        int ry = Math.min(arcHeight / 2, height / 2);

        int h = surface.getHeight();
        useColorProgram();

        // Build rounded rectangle outline as a single line loop
        int segsPerCorner = Math.max(4, Math.max(rx, ry) / 4);
        // Calculate correct vertex count: corners (each is segsPerCorner+1) + edges
        int totalVerts = 2 + 4 * (segsPerCorner + 1) + 4; // 2 top edge + 4 corners + 4 edges between corners
        float[] verts = new float[totalVerts * 2];

        int idx = 0;

        // Top edge (flip Y to WebGL space)
        verts[idx * 2] = x + rx;
        verts[idx * 2 + 1] = h - y;
        idx++;

        verts[idx * 2] = x + width - rx;
        verts[idx * 2 + 1] = h - y;
        idx++;

        // Top-right corner (flip Y to WebGL space)
        for (int i = 0; i <= segsPerCorner; i++) {
            double angle = -Math.PI / 2 + (Math.PI / 2) * i / segsPerCorner;
            verts[idx * 2] = (float) (x + width - rx + rx * Math.cos(angle));
            verts[idx * 2 + 1] = (float) (h - (y + ry + ry * Math.sin(angle)));
            idx++;
        }

        // Right edge (flip Y to WebGL space)
        verts[idx * 2] = x + width;
        verts[idx * 2 + 1] = h - (y + height - ry);
        idx++;

        // Bottom-right corner (flip Y to WebGL space)
        for (int i = 0; i <= segsPerCorner; i++) {
            double angle = 0 + (Math.PI / 2) * i / segsPerCorner;
            verts[idx * 2] = (float) (x + width - rx + rx * Math.cos(angle));
            verts[idx * 2 + 1] = (float) (h - (y + height - ry + ry * Math.sin(angle)));
            idx++;
        }

        // Bottom edge (flip Y to WebGL space)
        verts[idx * 2] = x + rx;
        verts[idx * 2 + 1] = h - (y + height);
        idx++;

        // Bottom-left corner (flip Y to WebGL space)
        for (int i = 0; i <= segsPerCorner; i++) {
            double angle = Math.PI / 2 + (Math.PI / 2) * i / segsPerCorner;
            verts[idx * 2] = (float) (x + rx + rx * Math.cos(angle));
            verts[idx * 2 + 1] = (float) (h - (y + height - ry + ry * Math.sin(angle)));
            idx++;
        }

        // Left edge (flip Y to WebGL space)
        verts[idx * 2] = x;
        verts[idx * 2 + 1] = h - (y + ry);
        idx++;

        // Top-left corner (flip Y to WebGL space)
        for (int i = 0; i <= segsPerCorner; i++) {
            double angle = Math.PI + (Math.PI / 2) * i / segsPerCorner;
            verts[idx * 2] = (float) (x + rx + rx * Math.cos(angle));
            verts[idx * 2 + 1] = (float) (h - (y + ry + ry * Math.sin(angle)));
            idx++;
        }

        ArrayBuffer vertBuf = Float32Array.fromJavaArray(java.util.Arrays.copyOf(verts, idx * 2)).getBuffer();
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, vertBuf, WebGLRenderingContext.STREAM_DRAW);

        gl.drawArrays(WebGLRenderingContext.LINE_LOOP, 0, idx);
        surface.markDirty();
    }

    private void drawPolyline(int[] xpoints, int[] ypoints, int npoints) {
        if (npoints < 2)
            return;

        // Use WebGL LINE_STRIP for efficient GPU rendering
        int h = surface.getHeight();
        useColorProgram();

        // Create vertices array (flip Y coordinates to WebGL space)
        float[] verts = new float[npoints * 2];
        for (int i = 0; i < npoints; i++) {
            verts[i * 2] = xpoints[i];
            verts[i * 2 + 1] = h - ypoints[i];
        }

        ArrayBuffer vertBuf = Float32Array.fromJavaArray(verts).getBuffer();
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, vertBuf, WebGLRenderingContext.STREAM_DRAW);

        gl.drawArrays(WebGLRenderingContext.LINE_STRIP, 0, npoints);
        surface.markDirty();
    }

    private void copyArea(int x, int y, int width, int height, int dx, int dy) {
        // For WebGL, we need to read pixels and redraw them
        // Note: This is expensive (requires GPU-CPU-GPU round-trip with texture
        // operations)
        // Consider using software rendering for complex copyArea operations

        java.awt.geom.AffineTransform transform = backend.contextStack.getTransform();
        int srcX = x + (int) transform.getTranslateX();
        int srcY = y + (int) transform.getTranslateY();
        int destX = srcX + dx;
        int destY = srcY + dy;

        // Create a temporary texture to hold the source region
        WebGLTexture tmpTexture = gl.createTexture();
        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, tmpTexture);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                WebGLRenderingContext.TEXTURE_WRAP_S,
                WebGLRenderingContext.CLAMP_TO_EDGE);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                WebGLRenderingContext.TEXTURE_WRAP_T,
                WebGLRenderingContext.CLAMP_TO_EDGE);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                WebGLRenderingContext.TEXTURE_MIN_FILTER,
                WebGLRenderingContext.NEAREST);
        gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
                WebGLRenderingContext.TEXTURE_MAG_FILTER,
                WebGLRenderingContext.NEAREST);

        // Copy from framebuffer to texture
        gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, framebuffer);
        gl.copyTexImage2D(WebGLRenderingContext.TEXTURE_2D, 0,
                WebGLRenderingContext.RGBA, srcX, surface.getHeight() - (srcY + height),
                width, height, 0);

        // Draw the texture at destination
        drawTexture(tmpTexture, WebGLSurfaceBackend.SwizzleMode.NONE,
                destX, destY, width, height, width, height, null);

        // Clean up
        gl.deleteTexture(tmpTexture);
    }

    private void setComposite(Composite composite) {
        backend.contextStack.setComposite(composite);
    }

    /**
     * Renders custom geometry using the currently active custom shader.
     * This method provides low-level access to WebGL draw calls for advanced rendering.
     * 
     * @param mode the WebGL primitive type (e.g., WebGLRenderingContext.TRIANGLES)
     * @param first the starting index in the enabled arrays
     * @param count the number of vertices to render
     * @throws IllegalStateException if no custom shader is active
     */
    public void drawCustomGeometry(int mode, int first, int count) {
        if (backend.getActiveCustomShader() == null) {
            throw new IllegalStateException("No custom shader is active. Call activateCustomShader() first.");
        }
        
        gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, framebuffer);
        gl.viewport(0, 0, surface.getWidth(), surface.getHeight());
        
        // Apply context stack state (transform, clip, blend)
        backend.contextStack.apply();
        
        gl.drawArrays(mode, first, count);
        surface.markDirty();
    }

    /**
     * Renders indexed custom geometry using the currently active custom shader.
     * This method provides low-level access to WebGL indexed draw calls for advanced rendering.
     * 
     * @param mode the WebGL primitive type (e.g., WebGLRenderingContext.TRIANGLES)
     * @param count the number of elements to render
     * @param type the type of values in the element array buffer (e.g., WebGLRenderingContext.UNSIGNED_SHORT)
     * @param offset the byte offset in the element array buffer
     * @throws IllegalStateException if no custom shader is active
     */
    public void drawCustomElements(int mode, int count, int type, int offset) {
        if (backend.getActiveCustomShader() == null) {
            throw new IllegalStateException("No custom shader is active. Call activateCustomShader() first.");
        }
        
        gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, framebuffer);
        gl.viewport(0, 0, surface.getWidth(), surface.getHeight());
        
        // Apply context stack state (transform, clip, blend)
        backend.contextStack.apply();
        
        gl.drawElements(mode, count, type, offset);
        surface.markDirty();
    }

    /**
     * Gets the WebGLSurfaceBackend for advanced custom shader operations.
     * Provides access to the WebGL context, custom shader management, and rendering state.
     * 
     * @return the WebGL surface backend
     */
    public WebGLSurfaceBackend getBackend() {
        return backend;
    }
    
    private ShaderCallbackWrapper pendingCallback = null;
    
    @Override
    public void queueRenderCallback(Object wrapper) {
        if (wrapper instanceof ShaderCallbackWrapper) {
            pendingCallback = (ShaderCallbackWrapper) wrapper;
        }
    }

    @Override
    public void rasterizeCommands(List<SurfaceCommand> cmds) {

        gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, framebuffer);
        gl.viewport(0, 0, surface.getWidth(), surface.getHeight());
        gl.framebufferTexture2D(WebGLRenderingContext.FRAMEBUFFER, WebGLRenderingContext.COLOR_ATTACHMENT0,
                WebGLRenderingContext.TEXTURE_2D, this.surface.texture, 0);
        
        // Set surface dimensions for clip application
        backend.contextStack.setSurfaceDimensions(surface.getWidth(), surface.getHeight());
        
        // Set up the shader context for this rendering pass if not already set
        // (TSurfaceRasterizerGraphics may have already set it during paint())
        WebGLShaderContext existingContext = WebGLShaderContext.getCurrentContext();
        boolean contextWasSet = existingContext != null;
        if (!contextWasSet) {
            WebGLShaderContext context = new WebGLShaderContext(backend, this);
            WebGLShaderContext.setCurrentContext(context);
        }
        
        try {
            for (SurfaceCommand cmd : cmds) {
                switch (cmd.type) {
                case SET_COLOR:
                    Color c = (Color) cmd.obj;
                    if (cmd.argCount > 0 && cmd.args[0] == 0) {
                        backend.contextStack.setForeground(c);
                    } else if (cmd.argCount > 0 && cmd.args[0] == 1) {
                        backend.contextStack.setBackground(c);
                    } else {
                        log.error("WebGLRasterizer: Unknown color target: {}", cmd.argCount > 0 ? cmd.args[0] : -1);
                    }
                    break;
                case SET_TRANSFORM:
                    AffineTransform at = (AffineTransform) cmd.obj;
                    backend.contextStack.setTransform((java.awt.geom.AffineTransform) at);
                    break;
                case SET_CLIP_RECT:
                    setClip((Shape) cmd.obj);
                    break;
                case SET_COMPOSITE:
                    setComposite((Composite) cmd.obj);
                    break;
                case BLIT_IMAGE:
                    Surface s = ((SurfaceContainer) cmd.obj).getSurface();
                    drawImage(s, cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3]);
                    break;
                case DRAW_RECT:
                    drawRect(cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3], 1.0f);
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
                    drawPolygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
                    break;
                }
                case FILL_POLYGON: {
                    java.awt.Polygon polygon = (java.awt.Polygon) cmd.obj;
                    fillPolygon(polygon.xpoints, polygon.ypoints, polygon.npoints);
                    break;
                }
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
                    break;
                }
                case COPY_AREA:
                    copyArea(cmd.args[0], cmd.args[1], cmd.args[2], cmd.args[3], cmd.args[4], cmd.args[5]);
                    break;
                case RENDER_CALLBACK:
                    executeRenderCallback(cmd.obj);
                    break;
                case NO_OP:
                    // do nothing (shouldn't be in the command list in the first place)
                    break;
                default:
                    log.error("WebGLRasterizer: Unhandled command type: {}", cmd.type);
                    break;
            }
        }
        
        // Execute any pending callback queued via queueRenderCallback()
        if (pendingCallback != null) {
            executeRenderCallback(pendingCallback);
            pendingCallback = null;
        }

        // Execute post-processing callbacks (if this is the root rasterizer that owns the context)
        boolean postProcessExecuted = false;
        if (pushToScreen && !isChildRasterizer) {
            WebGLShaderContext ctx = WebGLShaderContext.getCurrentContext();
            if (ctx != null && ctx.hasPostProcessCallbacks()) {
                ctx.executePostProcessCallbacks();
                postProcessExecuted = true;
            }
        }

        // Only push to screen if post-processing didn't handle it
        if (pushToScreen && !postProcessExecuted) {
            pushToScreen();
        }
        } finally {
            // Only clear the context if we set it (not if TSurfaceRasterizerGraphics set it)
            // TSurfaceRasterizerGraphics will clear it after flush() completes
            if (!contextWasSet) {
                WebGLShaderContext.setCurrentContext(null);
            }
        }
    }
    
    /**
     * Executes a custom shader rendering callback.
     * The shader is activated before the callback and deactivated afterwards.
     */
    private void executeRenderCallback(Object obj) {
        if (!(obj instanceof ShaderCallbackWrapper)) {
            log.error("RENDER_CALLBACK command received with invalid object type: {}", 
                obj != null ? obj.getClass().getName() : "null");
            return;
        }
        
        ShaderCallbackWrapper wrapper = (ShaderCallbackWrapper) obj;
        CustomShaderProgram shader = wrapper.getShader();
        ShaderRenderCallback callback = wrapper.getCallback();
        
        if (shader == null || callback == null) {
            log.error("RENDER_CALLBACK command received with null shader or callback");
            return;
        }
        
        // Activate the shader
        backend.activateCustomShader(shader);
        
        try {
            // Execute the callback
            callback.render(backend, this);
        } catch (Exception e) {
            log.error("Error executing shader callback: {}", e.getMessage(), e);
        } finally {
            // Always deactivate the shader
            backend.deactivateCustomShader();
        }
    }

    private void pushToScreen() {
        int width = surface.getWidth();
        int height = surface.getHeight();

        gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, null);

        gl.viewport(0, 0, width, height);

        gl.activeTexture(WebGLRenderingContext.TEXTURE0);
        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, surface.texture);
        
        // Save current transform and temporarily use identity for screen push
        backend.contextStack.save();
        backend.contextStack.getTransform().setToIdentity();
        
        // no swizzling when pushing to screen, as the surface texture is already in
        // RGBA format
        backend.useTextureProgram(WebGLSurfaceBackend.SwizzleMode.NONE, gl.getCanvas().getWidth(), gl.getCanvas().getHeight());

        // full-screen quad

        float[] verts = {
                0, 0,
                width, 0,
                0, height,
                0, height,
                width, 0,
                width, height
        };

        float[] uvs = {
                0f, 0f,
                1f, 0f,
                0f, 1f,
                0f, 1f,
                1f, 0f,
                1f, 1f
        };

        backend.uploadQuadVertices(verts, uvs);
        gl.drawArrays(WebGLRenderingContext.TRIANGLES, 0, 6);
        
        // Restore previous transform
        backend.contextStack.restore();
        
        // Render picking buffer debug visualization if enabled
        backend.renderPickingDebugIfEnabled(gl.getCanvas().getWidth(), gl.getCanvas().getHeight());
    }
}
