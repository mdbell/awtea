package me.mdbell.awtea.examples.postprocess;

import me.mdbell.awtea.gfx.webgl.*;
import me.mdbell.awtea.util.StubAppletStub;
import me.mdbell.awtea.util.logging.LogLevel;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.JSExport;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.webgl.WebGL2RenderingContext;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Example demonstrating post-processing effects with bloom.
 * Shows how to use RenderTarget, RenderTargetPool, and PostProcessPipeline
 * to apply multi-pass effects to rendered content.
 */
public class PostProcessDemo extends Applet {

    private HTMLCanvasElement canvas;
    private WebGLSurfaceBackend backend;
    private RenderTargetPool pool;
    private PostProcessPipeline pipeline;
    private RenderTarget sceneTarget;
    private BloomEffect bloomEffect;

    // Scene state
    private float time = 0;
    private boolean bloomEnabled = true;
    private float bloomThreshold = 0.6f;
    private float bloomIntensity = 1.5f;
    
    private static OnVisibleCallback onVisible = null;

    @JSFunctor
    private interface OnVisibleCallback extends JSObject {
        void invoke();
    }

    @JSExport
    public static void setOpenCallback(OnVisibleCallback callback) {
        onVisible = callback;
    }

    public PostProcessDemo() {
        setBackground(Color.BLACK);
        
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyChar()) {
                    case 'b':
                    case 'B':
                        bloomEnabled = !bloomEnabled;
                        System.out.println("Bloom " + (bloomEnabled ? "enabled" : "disabled"));
                        break;
                    case '+':
                    case '=':
                        bloomIntensity = Math.min(3.0f, bloomIntensity + 0.1f);
                        bloomEffect.setIntensity(bloomIntensity);
                        System.out.println("Bloom intensity: " + bloomIntensity);
                        break;
                    case '-':
                    case '_':
                        bloomIntensity = Math.max(0.0f, bloomIntensity - 0.1f);
                        bloomEffect.setIntensity(bloomIntensity);
                        System.out.println("Bloom intensity: " + bloomIntensity);
                        break;
                    case 't':
                    case 'T':
                        bloomThreshold = (bloomThreshold < 0.9f) ? bloomThreshold + 0.1f : 0.1f;
                        bloomEffect.setThreshold(bloomThreshold);
                        System.out.println("Bloom threshold: " + bloomThreshold);
                        break;
                }
            }
        });
    }

    @Override
    public void init() {
        System.out.println("Initializing Post-Process Demo...");
        setLayout(new BorderLayout());
    }

    @Override
    public void start() {
        System.out.println("Starting Post-Process Demo...");

        // Get canvas
        HTMLDocument document = HTMLDocument.current();
        String canvasId = System.getProperty("me.mdbell.awtea.classlib.java.awt.Applet.canvasId",
                "post-process-canvas");
        canvas = (HTMLCanvasElement) document.getElementById(canvasId);

        if (canvas == null) {
            System.err.println("Canvas element not found: " + canvasId);
            return;
        }

        // Initialize post-processing infrastructure
        initializePostProcessing();

        // Notify ready
        if (onVisible != null) {
            onVisible.invoke();
        }

        // Start render loop
        scheduleRepaint();
    }

    private void initializePostProcessing() {
        // Get WebGL context from backend (created by Applet)
        // We need to access the underlying WebGLSurfaceBackend
        // For this demo, we'll create our own backend instance
        backend = new WebGLSurfaceBackend(canvas);
        WebGL2RenderingContext gl = backend.getGL();
        
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Create render target pool
        pool = new RenderTargetPool(gl);
        pool.setMaxPooledPerSize(6);

        // Create scene render target
        sceneTarget = new RenderTarget(gl, width, height);

        // Create post-processing pipeline
        pipeline = new PostProcessPipeline(backend, pool);

        // Add bloom effect
        bloomEffect = new BloomEffect(backend, pool);
        bloomEffect.setThreshold(bloomThreshold);
        bloomEffect.setIntensity(bloomIntensity);
        bloomEffect.setBlurRadius(2.0f);
        
        pipeline.addEffect(bloomEffect);

        System.out.println("Post-processing initialized");
        System.out.println("Controls: B=toggle bloom, +/- = intensity, T=cycle threshold");
    }

    @Override
    public void paint(Graphics g) {
        if (backend == null) {
            g.setColor(Color.WHITE);
            g.drawString("Initializing...", 10, 20);
            return;
        }
        
        time += 0.016f; // Assume ~60fps

        // Get dimensions
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Step 1: Render scene to offscreen target
        sceneTarget.bind();
        renderScene(sceneTarget);
        sceneTarget.unbind();

        // Step 2: Apply post-processing (if enabled)
        RenderTarget finalOutput;
        if (bloomEnabled) {
            finalOutput = pipeline.apply(sceneTarget);
        } else {
            finalOutput = sceneTarget;
        }

        // Step 3: Blit to screen
        blitToScreen(finalOutput, width, height);

        // Step 4: Release output (if not the scene target)
        if (finalOutput != sceneTarget) {
            pool.release(finalOutput);
        }
        
        // Draw UI on top
        drawUI(g);
    }

    private void renderScene(RenderTarget target) {
        // Clear to black
        target.clear(0, 0, 0, 1);
        
        WebGL2RenderingContext gl = backend.getGL();
        int width = target.getWidth();
        int height = target.getHeight();

        // Draw some bright animated shapes using simple shader
        float cx = width / 2.0f;
        float cy = height / 2.0f;
        
        // Use a simple custom shader for drawing circles
        if (circleShader == null) {
            initCircleShader();
        }
        
        backend.activateCustomShader(circleShader);
        
        // Central bright sun
        drawCircle(gl, width, height, cx, cy, 60, 1.0f, 1.0f, 0.8f, 1.0f);
        
        // Orbiting bright objects
        int numOrbiters = 5;
        for (int i = 0; i < numOrbiters; i++) {
            float angle = time + (i * 2.0f * (float)Math.PI / numOrbiters);
            float radius = 150 + 50 * (float)Math.sin(time * 0.5f + i);
            float x = cx + radius * (float)Math.cos(angle);
            float y = cy + radius * (float)Math.sin(angle);
            
            // Bright colored orbs
            float hue = i / (float)numOrbiters;
            float[] rgb = hsvToRgb(hue, 1.0f, 1.0f);
            drawCircle(gl, width, height, x, y, 30, rgb[0], rgb[1], rgb[2], 1.0f);
        }
        
        // Some dimmer stars
        for (int i = 0; i < 20; i++) {
            float x = (float)(Math.sin(i * 13.7) * 0.4 + 0.5) * width;
            float y = (float)(Math.sin(i * 27.3) * 0.4 + 0.5) * height;
            float brightness = 0.3f + 0.3f * (float)Math.sin(time * 2 + i);
            drawCircle(gl, width, height, x, y, 5, brightness, brightness, brightness, 1.0f);
        }
        
        backend.deactivateCustomShader();
    }
    
    private CustomShaderProgram circleShader;
    
    private void initCircleShader() {
        String vertexShader =
            "precision mediump float;\n" +
            "attribute vec2 a_position;\n" +
            "uniform vec2 u_resolution;\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 clipSpace = (a_position / u_resolution) * 2.0 - 1.0;\n" +
            "    gl_Position = vec4(clipSpace * vec2(1, -1), 0, 1);\n" +
            "}\n";
        
        String fragmentShader =
            "precision mediump float;\n" +
            "uniform vec4 u_color;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_FragColor = u_color;\n" +
            "}\n";
        
        circleShader = backend.registerCustomShader("circle", vertexShader, fragmentShader);
    }

    private void drawCircle(WebGL2RenderingContext gl, int screenW, int screenH, 
                           float cx, float cy, float radius, 
                           float r, float g, float b, float a) {
        // Simple circle using triangle fan
        int segments = 32;
        float[] vertices = new float[(segments + 2) * 2];
        
        // Center
        vertices[0] = cx;
        vertices[1] = screenH - cy; // Flip Y
        
        // Circle points
        for (int i = 0; i <= segments; i++) {
            float angle = (float)(2.0 * Math.PI * i / segments);
            vertices[(i + 1) * 2] = cx + radius * (float)Math.cos(angle);
            vertices[(i + 1) * 2 + 1] = screenH - (cy + radius * (float)Math.sin(angle));
        }
        
        // Set shader uniforms
        circleShader.setUniform2f("u_resolution", screenW, screenH);
        circleShader.setUniform4f("u_color", r, g, b, a);
        
        // Upload vertices
        org.teavm.jso.typedarrays.Float32Array vertArray = 
            org.teavm.jso.typedarrays.Float32Array.fromJavaArray(vertices);
        
        org.teavm.jso.webgl.WebGLBuffer buffer = gl.createBuffer();
        gl.bindBuffer(org.teavm.jso.webgl.WebGLRenderingContext.ARRAY_BUFFER, buffer);
        gl.bufferData(
            org.teavm.jso.webgl.WebGLRenderingContext.ARRAY_BUFFER,
            vertArray.getBuffer(),
            org.teavm.jso.webgl.WebGLRenderingContext.STREAM_DRAW
        );
        
        // Setup attribute
        circleShader.enableVertexAttribArray("a_position");
        circleShader.vertexAttribPointer("a_position", 2, 
            org.teavm.jso.webgl.WebGLRenderingContext.FLOAT, false, 0, 0);
        
        // Draw
        gl.drawArrays(org.teavm.jso.webgl.WebGLRenderingContext.TRIANGLE_FAN, 0, segments + 2);
        
        // Cleanup
        gl.deleteBuffer(buffer);
    }

    private void blitToScreen(RenderTarget target, int width, int height) {
        WebGL2RenderingContext gl = backend.getGL();
        
        // Bind default framebuffer
        gl.bindFramebuffer(org.teavm.jso.webgl.WebGLRenderingContext.FRAMEBUFFER, null);
        gl.viewport(0, 0, width, height);

        // Use PostProcessContext for simple blit
        PostProcessContext ctx = new PostProcessContext(backend);
        ctx.blitSimple(target, null); // null output means screen
    }

    private void drawUI(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        int y = 20;
        g.drawString("Post-Processing Demo - Bloom Effect", 10, y);
        y += 20;
        g.drawString("Bloom: " + (bloomEnabled ? "ON" : "OFF") + " (B to toggle)", 10, y);
        y += 20;
        g.drawString("Intensity: " + String.format("%.1f", bloomIntensity) + " (+/- to adjust)", 10, y);
        y += 20;
        g.drawString("Threshold: " + String.format("%.1f", bloomThreshold) + " (T to cycle)", 10, y);
        y += 20;
        
        // Pool stats
        g.drawString("Pool: " + pool.getPooledTargetCount() + " / " + pool.getTotalTargetCount(), 10, y);
    }

    private float[] hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h * 6) % 2 - 1));
        float m = v - c;
        
        float r, g, b;
        if (h < 1.0f/6) {
            r = c; g = x; b = 0;
        } else if (h < 2.0f/6) {
            r = x; g = c; b = 0;
        } else if (h < 3.0f/6) {
            r = 0; g = c; b = x;
        } else if (h < 4.0f/6) {
            r = 0; g = x; b = c;
        } else if (h < 5.0f/6) {
            r = x; g = 0; b = c;
        } else {
            r = c; g = 0; b = x;
        }
        
        return new float[] { r + m, g + m, b + m };
    }

    private void scheduleRepaint() {
        org.teavm.jso.browser.Window.setTimeout(() -> {
            repaint();
            scheduleRepaint();
        }, 16); // ~60fps
    }

    @Override
    public void destroy() {
        if (pipeline != null) {
            pipeline.destroy();
        }
        if (sceneTarget != null) {
            sceneTarget.destroy();
        }
        if (pool != null) {
            pool.destroy();
        }
        super.destroy();
    }

    public static void main(String[] args) {
        LoggerFactory.setGlobalLevel(LogLevel.INFO);

        System.out.println("Starting Post-Process Demo...");

        // Get canvas ID from args
        String canvasId = args.length > 0 ? args[0] : "post-process-canvas";

        // Set system property
        System.setProperty("me.mdbell.awtea.classlib.java.awt.Applet.canvasId", canvasId);

        // Create and start applet
        PostProcessDemo applet = new PostProcessDemo();
        applet.setStub(new StubAppletStub());
        applet.init();
        applet.start();
    }
}
