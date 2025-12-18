package me.mdbell.awtea.gfx.webgl;

import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceCommand;
import me.mdbell.awtea.gfx.SurfaceContainer;
import me.mdbell.awtea.instrument.Monitored;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
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
class WebGLRasterizer implements Rasterizer {

    private static final Logger log = LoggerFactory.getLogger(WebGLRasterizer.class);

    private final WebGLSurfaceBackend backend;
    private final WebGL2RenderingContext gl;
    private final WebGLSurface surface;
    private final WebGLFramebuffer framebuffer;

    private final AffineTransform transform = new AffineTransform();
    private transient final Float32Array transformArray = new Float32Array(9);
    private Rectangle clip = null;

    private Color foreground = Color.WHITE;
    private Color background = Color.BLACK;
    
    private Composite composite = AlphaComposite.SrcOver;

    private boolean pushToScreen = false;

    // identity transform array for pushing to screen
    private static final Float32Array identityTransformArray = Float32Array.fromJavaArray(new float[]{
            1f, 0f, 0f,
            0f, 1f, 0f,
            0f, 0f, 1f
    });

    WebGLRasterizer(WebGLSurfaceBackend backend, WebGLSurface surface, boolean pushToScreen) {
        this.backend = backend;
        this.framebuffer = surface.framebuffer;
        this.gl = backend.gl;
        this.surface = surface;
        transform.setToIdentity();
        updateTransformFloats(transform);
        this.clip = new Rectangle(0, 0, surface.getWidth(), surface.getHeight());
        this.pushToScreen = pushToScreen;
    }

    private WebGLRasterizer(WebGLRasterizer other) {
        this.surface = other.surface;
        this.framebuffer = other.framebuffer;
        this.backend = other.backend;
        this.gl = other.gl;
        this.transform.setTransform(other.transform);
        this.foreground = other.foreground;
        this.background = other.background;
        this.composite = other.composite;
        this.clip = other.clip;
        updateTransformFloats(this.transform);
    }

    @Override
    public Rasterizer create() {
        return new WebGLRasterizer(this);
    }

    @Override
    public void reset() {
        this.transform.setToIdentity();
        updateTransformFloats(transform);
        this.foreground = Color.WHITE;
        this.background = Color.BLACK;
        this.composite = AlphaComposite.SrcOver;
        this.clip = new Rectangle(0, 0, surface.getWidth(), surface.getHeight());
    }

    private void fillRect(float x, float y, float width, float height) {
        int h = surface.getHeight();
        y = h - (y + height); // flip Y coordinate
        useColorProgram();

        setColor(foreground);
        backend.setRectBuffer(x, y, width, height);

        gl.drawArrays(WebGLRenderingContext.TRIANGLES, 0, 6);

        surface.markDirty();
    }

    private void drawRect(float x, float y, float width, float height, float lineWidth) {
        // draw 4 filled rects to make the border
        fillRect(x, y, width, lineWidth); // top
        fillRect(x, y + height - lineWidth, width, lineWidth); // bottom
        fillRect(x, y + lineWidth, lineWidth, height - 2 * lineWidth); // left
        fillRect(x + width - lineWidth, y + lineWidth, lineWidth, height - 2 * lineWidth); // right

        surface.markDirty();
    }

    private void setColor(Color c) {
        float r = c.getRed() / 255.0f;
        float g = c.getGreen() / 255.0f;
        float b = c.getBlue() / 255.0f;
        float a = c.getAlpha() / 255.0f;

        backend.setColor(r, g, b, a);
    }

    private void useColorProgram() {
        int width = surface.getWidth();
        int height = surface.getHeight();

        backend.useColorProgram(width, height, this.transformArray);
    }

    private void applyClip() {
        if (clip == null) {
            gl.disable(WebGLRenderingContext.SCISSOR_TEST);
            return;
        }
        gl.enable(WebGLRenderingContext.SCISSOR_TEST);

        int tx = (int) transform.getTranslateX();
        int ty = (int) transform.getTranslateY();
        int h = surface.getHeight();

        int cx = clip.x + tx;
        int cy = clip.y + ty;

        gl.scissor(cx, h - (cy + clip.height), clip.width, clip.height);
    }

    private void updateTransformFloats(AffineTransform transform) {
        // Matrix needs to be in column-major order:
        // ---------------
        // | m00 m10  0  |
        // | m01 m11  0  |
        // | m02 m12  1  |
        // ---------------
        transformArray.set(0, (float) transform.getScaleX());
        transformArray.set(1, (float) transform.getShearY());
        transformArray.set(2, 0f);
        transformArray.set(3, (float) transform.getShearX());
        transformArray.set(4, (float) transform.getScaleY());
        transformArray.set(5, 0f);
        transformArray.set(6, (float) transform.getTranslateX());
        transformArray.set(7, (float) transform.getTranslateY());
        transformArray.set(8, 1f);
    }

    private void clearRect(int x, int y, int width, int height) {
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
        applyClip(); // restore previous clip

        surface.markDirty();
    }

    private void setClip(Shape shape) {
        if (shape == null) {
            this.clip = null;
            gl.disable(WebGLRenderingContext.SCISSOR_TEST);
            return;
        }
        this.clip = shape.getBounds();
        applyClip();
    }

    private void drawImage(Object img, int x, int y, int width, int height) {
        if (img instanceof WebGLSurface) {
            drawWebGLSurface((WebGLSurface) img, x, y, width, height);
        } else if (img instanceof Surface) {
            // generic Surface drawing (not optimized - gets copied into GPU texture and then drawn)
            Surface surface = (Surface) img;
            drawSurface(surface, x, y, width, height);
        } else {
            log.error("WebGLRasterizer: drawImage: Unsupported image type: {}", img.getClass().getName());
        }
    }

    private void drawWebGLSurface(WebGLSurface img, int x, int y, int width, int height) {
        WebGLTexture other = img.texture;

        // no swizzling needed when drawing from one WebGLSurface to another
        drawTexture(other, WebGLSurfaceBackend.SwizzleMode.NONE, x, y, img.getWidth(), img.getHeight(), width, height, null);
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
            drawTexture(tmp, mode, x, y, surface.getWidth(), surface.getHeight(), width, height, surface.getPixelData());
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
                log.warn("WebGLRasterizer: Unknown surface format: {}, defaulting to no swizzling", surface.getFormat());
                return WebGLSurfaceBackend.SwizzleMode.NONE;
        }
    }

    private void drawTexture(WebGLTexture texture, WebGLSurfaceBackend.SwizzleMode mode,
                             int x, int y, int srcW, int srcH, int width, int height, Uint8ClampedArray pixelData) {
        backend.useTextureProgram(mode, surface.getWidth(), surface.getHeight(), this.transformArray);

        // the surface associated with this rasterizer already has its texture on the GPU,
        // and we have already called gl.bindTexture for it at the start of rasterization,

        // we can skip the texture upload when using a WebGLSurface, as its texture is already on the GPU

        //TODO: optimize vertex buffer usage

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
        // Use Bresenham's algorithm implemented via fillRect for pixel-perfect lines
        // Transform coordinates
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1;
        int sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;

        while (true) {
            // Draw a 1x1 pixel at current position
            fillRect(x1, y1, 1, 1);
            
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

    private void drawPolygon(int[] xpoints, int[] ypoints, int npoints) {
        if (npoints < 2) return;
        
        // Draw lines connecting consecutive points
        for (int i = 1; i < npoints; i++) {
            drawLine(xpoints[i - 1], ypoints[i - 1], xpoints[i], ypoints[i]);
        }
        // Close the polygon
        drawLine(xpoints[npoints - 1], ypoints[npoints - 1], xpoints[0], ypoints[0]);
    }

    private void fillPolygon(int[] xpoints, int[] ypoints, int npoints) {
        if (npoints < 3) return;
        
        // Calculate bounding box
        int minX = xpoints[0];
        int maxX = xpoints[0];
        int minY = ypoints[0];
        int maxY = ypoints[0];
        
        for (int i = 1; i < npoints; i++) {
            minX = Math.min(minX, xpoints[i]);
            maxX = Math.max(maxX, xpoints[i]);
            minY = Math.min(minY, ypoints[i]);
            maxY = Math.max(maxY, ypoints[i]);
        }
        
        // Scanline fill algorithm
        for (int y = minY; y <= maxY; y++) {
            // Find intersections with edges
            java.util.ArrayList<Integer> intersections = new java.util.ArrayList<>();
            
            for (int i = 0; i < npoints; i++) {
                int j = (i + 1) % npoints;
                int y1 = ypoints[i];
                int y2 = ypoints[j];
                
                // Check if edge crosses scanline
                if ((y1 <= y && y < y2) || (y2 <= y && y < y1)) {
                    int x1 = xpoints[i];
                    int x2 = xpoints[j];
                    
                    // Calculate intersection x coordinate
                    int x = x1 + (y - y1) * (x2 - x1) / (y2 - y1);
                    intersections.add(x);
                }
            }
            
            // Sort intersections
            intersections.sort(Integer::compareTo);
            
            // Fill between pairs of intersections
            for (int i = 0; i < intersections.size() - 1; i += 2) {
                int x1 = intersections.get(i);
                int x2 = intersections.get(i + 1);
                if (x2 > x1) {
                    fillRect(x1, y, x2 - x1, 1);
                }
            }
        }
    }

    private void fillOval(int x, int y, int width, int height) {
        // Use midpoint ellipse algorithm
        int cx = x + width / 2;
        int cy = y + height / 2;
        int rx = width / 2;
        int ry = height / 2;
        
        if (rx == 0 || ry == 0) return;
        
        // Scanline fill approach
        for (int dy = -ry; dy <= ry; dy++) {
            // Calculate width at this y coordinate using ellipse equation
            // x^2/rx^2 + y^2/ry^2 = 1
            // x = rx * sqrt(1 - y^2/ry^2)
            double dx = rx * Math.sqrt(1.0 - (double)(dy * dy) / (ry * ry));
            int x1 = cx - (int)dx;
            int x2 = cx + (int)dx;
            fillRect(x1, cy + dy, x2 - x1, 1);
        }
    }

    private void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        if (arcWidth == 0 || arcHeight == 0) {
            fillRect(x, y, width, height);
            return;
        }
        
        int rx = arcWidth / 2;
        int ry = arcHeight / 2;
        
        // Clamp arc dimensions
        rx = Math.min(rx, width / 2);
        ry = Math.min(ry, height / 2);
        
        // Fill center rectangle
        fillRect(x + rx, y, width - 2 * rx, height);
        
        // Fill left and right rectangles
        fillRect(x, y + ry, rx, height - 2 * ry);
        fillRect(x + width - rx, y + ry, rx, height - 2 * ry);
        
        // Fill four corner arcs using scanline approach
        // Top-left corner
        for (int dy = 0; dy < ry; dy++) {
            double dx = rx * Math.sqrt(1.0 - (double)((ry - dy) * (ry - dy)) / (ry * ry));
            fillRect(x + rx - (int)dx, y + dy, (int)dx, 1);
        }
        
        // Top-right corner
        for (int dy = 0; dy < ry; dy++) {
            double dx = rx * Math.sqrt(1.0 - (double)((ry - dy) * (ry - dy)) / (ry * ry));
            fillRect(x + width - rx, y + dy, (int)dx, 1);
        }
        
        // Bottom-left corner
        for (int dy = 0; dy < ry; dy++) {
            double dx = rx * Math.sqrt(1.0 - (double)((ry - dy) * (ry - dy)) / (ry * ry));
            fillRect(x + rx - (int)dx, y + height - ry + dy, (int)dx, 1);
        }
        
        // Bottom-right corner
        for (int dy = 0; dy < ry; dy++) {
            double dx = rx * Math.sqrt(1.0 - (double)((ry - dy) * (ry - dy)) / (ry * ry));
            fillRect(x + width - rx, y + height - ry + dy, (int)dx, 1);
        }
    }

    private void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        int cx = x + width / 2;
        int cy = y + height / 2;
        int rx = width / 2;
        int ry = height / 2;
        
        if (rx == 0 || ry == 0) return;
        
        // Convert angles to radians
        double startRad = Math.toRadians(startAngle);
        double endRad = Math.toRadians(startAngle + arcAngle);
        
        // Normalize angles
        while (startRad < 0) startRad += 2 * Math.PI;
        while (endRad < 0) endRad += 2 * Math.PI;
        
        // Draw pie slice using scanline approach
        for (int dy = -ry; dy <= ry; dy++) {
            double dx = rx * Math.sqrt(1.0 - (double)(dy * dy) / (ry * ry));
            
            for (int px = -(int)dx; px <= (int)dx; px++) {
                // Check if point is within arc angle range
                double angle = Math.atan2(-dy, px); // negative dy for screen coordinates
                if (angle < 0) angle += 2 * Math.PI;
                
                boolean inArc;
                if (arcAngle >= 0) {
                    if (endRad >= startRad) {
                        inArc = angle >= startRad && angle <= endRad;
                    } else {
                        inArc = angle >= startRad || angle <= endRad;
                    }
                } else {
                    if (startRad >= endRad) {
                        inArc = angle <= startRad && angle >= endRad;
                    } else {
                        inArc = angle <= startRad || angle >= endRad;
                    }
                }
                
                if (inArc) {
                    fillRect(cx + px, cy + dy, 1, 1);
                }
            }
        }
    }

    private void drawOval(int x, int y, int width, int height) {
        // Use midpoint ellipse algorithm for outline
        int cx = x + width / 2;
        int cy = y + height / 2;
        int rx = width / 2;
        int ry = height / 2;
        
        if (rx == 0 || ry == 0) return;
        
        // Midpoint ellipse algorithm
        int x1 = 0;
        int y1 = ry;
        int rx2 = rx * rx;
        int ry2 = ry * ry;
        int twoRx2 = 2 * rx2;
        int twoRy2 = 2 * ry2;
        int p1 = (int)(ry2 - rx2 * ry + 0.25 * rx2);
        int px = 0;
        int py = twoRx2 * y1;
        
        // Region 1
        while (px < py) {
            plotEllipsePoints(cx, cy, x1, y1);
            x1++;
            px += twoRy2;
            if (p1 < 0) {
                p1 += ry2 + px;
            } else {
                y1--;
                py -= twoRx2;
                p1 += ry2 + px - py;
            }
        }
        
        // Region 2
        int p2 = (int)(ry2 * (x1 + 0.5) * (x1 + 0.5) + rx2 * (y1 - 1) * (y1 - 1) - rx2 * ry2);
        while (y1 >= 0) {
            plotEllipsePoints(cx, cy, x1, y1);
            y1--;
            py -= twoRx2;
            if (p2 > 0) {
                p2 += rx2 - py;
            } else {
                x1++;
                px += twoRy2;
                p2 += rx2 - py + px;
            }
        }
    }

    private void plotEllipsePoints(int cx, int cy, int x, int y) {
        fillRect(cx + x, cy + y, 1, 1);
        fillRect(cx - x, cy + y, 1, 1);
        fillRect(cx + x, cy - y, 1, 1);
        fillRect(cx - x, cy - y, 1, 1);
    }

    private void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        int cx = x + width / 2;
        int cy = y + height / 2;
        int rx = width / 2;
        int ry = height / 2;
        
        if (rx == 0 || ry == 0) return;
        
        // Convert angles to radians
        double startRad = Math.toRadians(startAngle);
        double endRad = Math.toRadians(startAngle + arcAngle);
        
        // Draw arc using parametric equations
        double angleStep = Math.toRadians(1); // 1 degree steps
        int steps = Math.abs(arcAngle);
        
        double angleInc = (endRad - startRad) / steps;
        double prevX = cx + rx * Math.cos(startRad);
        double prevY = cy - ry * Math.sin(startRad); // negative for screen coordinates
        
        for (int i = 1; i <= steps; i++) {
            double angle = startRad + i * angleInc;
            double nextX = cx + rx * Math.cos(angle);
            double nextY = cy - ry * Math.sin(angle);
            drawLine((int)prevX, (int)prevY, (int)nextX, (int)nextY);
            prevX = nextX;
            prevY = nextY;
        }
    }

    private void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        if (arcWidth == 0 || arcHeight == 0) {
            drawRect(x, y, width, height, 1.0f);
            return;
        }
        
        int rx = arcWidth / 2;
        int ry = arcHeight / 2;
        
        // Clamp arc dimensions
        rx = Math.min(rx, width / 2);
        ry = Math.min(ry, height / 2);
        
        // Draw four straight edges
        drawLine(x + rx, y, x + width - rx, y); // top
        drawLine(x + width, y + ry, x + width, y + height - ry); // right
        drawLine(x + width - rx, y + height, x + rx, y + height); // bottom
        drawLine(x, y + height - ry, x, y + ry); // left
        
        // Draw four corner arcs
        drawArc(x, y, 2 * rx, 2 * ry, 90, 90); // top-left
        drawArc(x + width - 2 * rx, y, 2 * rx, 2 * ry, 0, 90); // top-right
        drawArc(x, y + height - 2 * ry, 2 * rx, 2 * ry, 180, 90); // bottom-left
        drawArc(x + width - 2 * rx, y + height - 2 * ry, 2 * rx, 2 * ry, 270, 90); // bottom-right
    }

    private void drawPolyline(int[] xpoints, int[] ypoints, int npoints) {
        if (npoints < 2) return;
        
        // Draw lines connecting consecutive points (but don't close)
        for (int i = 1; i < npoints; i++) {
            drawLine(xpoints[i - 1], ypoints[i - 1], xpoints[i], ypoints[i]);
        }
    }

    private void copyArea(int x, int y, int width, int height, int dx, int dy) {
        // For WebGL, we need to read pixels and redraw them
        // This is expensive, but necessary for compatibility
        
        int srcX = x + (int)transform.getTranslateX();
        int srcY = y + (int)transform.getTranslateY();
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
        this.composite = composite != null ? composite : AlphaComposite.SrcOver;
        
        // Update WebGL blend function based on composite
        if (composite instanceof AlphaComposite) {
            AlphaComposite ac = (AlphaComposite) composite;
            int rule = ac.getRule();
            
            // Map AlphaComposite rules to WebGL blend functions
            switch (rule) {
                case AlphaComposite.CLEAR:
                    gl.blendFunc(WebGLRenderingContext.ZERO, WebGLRenderingContext.ZERO);
                    break;
                case AlphaComposite.SRC:
                    gl.blendFunc(WebGLRenderingContext.ONE, WebGLRenderingContext.ZERO);
                    break;
                case AlphaComposite.DST:
                    gl.blendFunc(WebGLRenderingContext.ZERO, WebGLRenderingContext.ONE);
                    break;
                case AlphaComposite.SRC_OVER:
                    gl.blendFunc(WebGLRenderingContext.SRC_ALPHA, WebGLRenderingContext.ONE_MINUS_SRC_ALPHA);
                    break;
                case AlphaComposite.DST_OVER:
                    gl.blendFunc(WebGLRenderingContext.ONE_MINUS_DST_ALPHA, WebGLRenderingContext.DST_ALPHA);
                    break;
                case AlphaComposite.SRC_IN:
                    gl.blendFunc(WebGLRenderingContext.DST_ALPHA, WebGLRenderingContext.ZERO);
                    break;
                case AlphaComposite.DST_IN:
                    gl.blendFunc(WebGLRenderingContext.ZERO, WebGLRenderingContext.SRC_ALPHA);
                    break;
                case AlphaComposite.SRC_OUT:
                    gl.blendFunc(WebGLRenderingContext.ONE_MINUS_DST_ALPHA, WebGLRenderingContext.ZERO);
                    break;
                case AlphaComposite.DST_OUT:
                    gl.blendFunc(WebGLRenderingContext.ZERO, WebGLRenderingContext.ONE_MINUS_SRC_ALPHA);
                    break;
                case AlphaComposite.SRC_ATOP:
                    gl.blendFunc(WebGLRenderingContext.DST_ALPHA, WebGLRenderingContext.ONE_MINUS_SRC_ALPHA);
                    break;
                case AlphaComposite.DST_ATOP:
                    gl.blendFunc(WebGLRenderingContext.ONE_MINUS_DST_ALPHA, WebGLRenderingContext.SRC_ALPHA);
                    break;
                case AlphaComposite.XOR:
                    gl.blendFunc(WebGLRenderingContext.ONE_MINUS_DST_ALPHA, WebGLRenderingContext.ONE_MINUS_SRC_ALPHA);
                    break;
                default:
                    // Default to SRC_OVER
                    gl.blendFunc(WebGLRenderingContext.SRC_ALPHA, WebGLRenderingContext.ONE_MINUS_SRC_ALPHA);
                    break;
            }
        }
    }

    @Override
    public void rasterizeCommands(List<SurfaceCommand> cmds) {

        gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, framebuffer);
        gl.viewport(0, 0, surface.getWidth(), surface.getHeight());
        gl.framebufferTexture2D(WebGLRenderingContext.FRAMEBUFFER, WebGLRenderingContext.COLOR_ATTACHMENT0,
                WebGLRenderingContext.TEXTURE_2D, this.surface.texture, 0);

        gl.enable(WebGLRenderingContext.BLEND);
        gl.blendFunc(WebGLRenderingContext.SRC_ALPHA, WebGLRenderingContext.ONE_MINUS_SRC_ALPHA);

        updateTransformFloats(this.transform);
        applyClip();

        for (SurfaceCommand cmd : cmds) {
            switch (cmd.type) {
                case SET_COLOR:
                    Color c = (Color) cmd.obj;
                    if (cmd.argCount > 0 && cmd.args[0] == 0) {
                        this.foreground = c;
                    } else if (cmd.argCount > 0 && cmd.args[0] == 1) {
                        this.background = c;
                    } else {
                        log.error("WebGLRasterizer: Unknown color target: {}", cmd.argCount > 0 ? cmd.args[0] : -1);
                    }
                    break;
                case SET_TRANSFORM:
                    AffineTransform at = (AffineTransform) cmd.obj;
                    this.transform.setTransform(at);
                    updateTransformFloats(this.transform);
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
                case NO_OP:
                    // do nothing (shouldn't be in the command list in the first place)
                    break;
                default:
                    log.error("WebGLRasterizer: Unhandled command type: {}", cmd.type);
                    break;
            }
        }

        if (pushToScreen) {
            pushToScreen();
        }
    }

    private void pushToScreen() {
        int width = gl.getCanvas().getWidth();
        int height = gl.getCanvas().getHeight();


        gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, null);

        gl.viewport(0, 0, width, height);

        gl.activeTexture(WebGLRenderingContext.TEXTURE0);
        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, surface.texture);
        // no swizzling when pushing to screen, as the surface texture is already in RGBA format
        backend.useTextureProgram(WebGLSurfaceBackend.SwizzleMode.NONE, surface.getWidth(), surface.getHeight(),
                identityTransformArray);

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
    }
}
