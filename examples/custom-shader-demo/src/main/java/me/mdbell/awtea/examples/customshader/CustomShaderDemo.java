package me.mdbell.awtea.examples.customshader;

import me.mdbell.awtea.gfx.SurfaceBackendFactory;
import me.mdbell.awtea.gfx.webgl.CustomShaderProgram;
import me.mdbell.awtea.gfx.webgl.WebGLRasterizer;
import me.mdbell.awtea.gfx.webgl.WebGLSurface;
import me.mdbell.awtea.gfx.webgl.WebGLSurfaceBackend;
import me.mdbell.awtea.util.StubAppletStub;
import me.mdbell.awtea.util.logging.LogLevel;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.JSExport;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.AnimationFrameCallback;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLBuffer;
import org.teavm.jso.webgl.WebGLRenderingContext;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Example demonstrating custom shader usage with a glowing animated triangle.
 * This Applet shows how to:
 * - Register and compile custom GLSL shaders
 * - Set uniforms for animation and effects
 * - Upload and bind vertex data
 * - Render custom geometry with drawCustomGeometry
 * - Handle mouse events with AWT
 */
public class CustomShaderDemo extends Applet {

    private WebGLSurfaceBackend backend;
    private WebGLSurface surface;
    private WebGLRasterizer rasterizer;
    private CustomShaderProgram glowShader;
    
    private WebGLBuffer vertexBuffer;
    private WebGLBuffer colorBuffer;
    
    private long startTime;
    private boolean isAnimating = true;
    
    private static OnVisibleCallback onVisible = null;
    private HTMLCanvasElement canvas;

    @JSFunctor
    private interface OnVisibleCallback extends JSObject {
        void invoke();
    }

    @JSExport
    public static void setOpenCallback(OnVisibleCallback callback) {
        onVisible = callback;
    }

    @Override
    public void init() {
        System.out.println("Initializing Custom Shader Demo Applet...");
        
        setLayout(new BorderLayout());
        
        // Create a canvas that will receive mouse events
        Canvas renderCanvas = new Canvas() {
            @Override
            public void paint(Graphics g) {
                // Draw instruction text overlay
                g.setColor(new Color(255, 255, 255, 180));
                g.setFont(new Font("SansSerif", Font.PLAIN, 14));
                g.drawString("Click to pause/resume animation", 10, getHeight() - 10);
            }
        };
        
        // Add mouse listener using AWT events
        renderCanvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                isAnimating = !isAnimating;
                if (isAnimating) {
                    startTime = System.currentTimeMillis();
                }
                System.out.println("Animation " + (isAnimating ? "resumed" : "paused"));
            }
        });
        
        add(renderCanvas, BorderLayout.CENTER);
        
        startTime = System.currentTimeMillis();
    }

    @Override
    public void start() {
        System.out.println("Starting Custom Shader Demo...");
        
        // Get the canvas element
        HTMLDocument document = HTMLDocument.current();
        String canvasId = System.getProperty("me.mdbell.awtea.classlib.java.awt.Applet.canvasId", "custom-shader-canvas");
        canvas = (HTMLCanvasElement) document.getElementById(canvasId);
        
        if (canvas == null) {
            System.err.println("Canvas element not found: " + canvasId);
            return;
        }
        
        // Initialize WebGL backend and surface
        backend = (WebGLSurfaceBackend) SurfaceBackendFactory.getWebGLBackend(canvas);
        surface = (WebGLSurface) backend.createScreenSurface(
            canvas.getWidth(), 
            canvas.getHeight()
        );
        rasterizer = (WebGLRasterizer) surface.createRasterizer();
        
        // Initialize shader and geometry
        initializeShader();
        initializeGeometry();
        
        // Notify that we're ready
        if (onVisible != null) {
            onVisible.invoke();
        }
        
        // Start animation loop
        System.out.println("Animation loop starting...");
        requestAnimationFrame();
    }
    
    private void initializeShader() {
        // Vertex shader with position and color attributes, plus animation
        String vertexShader =
            "attribute vec2 a_position;\n" +
            "attribute vec3 a_color;\n" +
            "\n" +
            "uniform vec2 u_resolution;\n" +
            "uniform mat3 u_transform;\n" +
            "uniform float u_time;\n" +
            "\n" +
            "varying vec3 v_color;\n" +
            "\n" +
            "void main() {\n" +
            "    // Apply transform\n" +
            "    vec3 pos = u_transform * vec3(a_position, 1.0);\n" +
            "    \n" +
            "    // Add wave animation based on time\n" +
            "    float wave = sin(pos.y * 0.01 + u_time * 2.0) * 20.0;\n" +
            "    pos.x += wave;\n" +
            "    \n" +
            "    // Convert to clip space\n" +
            "    vec2 zeroToOne = pos.xy / u_resolution;\n" +
            "    vec2 clipSpace = (zeroToOne * 2.0 - 1.0) * vec2(1, -1);\n" +
            "    \n" +
            "    gl_Position = vec4(clipSpace, 0, 1);\n" +
            "    v_color = a_color;\n" +
            "}\n";
        
        // Fragment shader with glow effect
        String fragmentShader =
            "precision mediump float;\n" +
            "\n" +
            "varying vec3 v_color;\n" +
            "\n" +
            "uniform float u_time;\n" +
            "uniform float u_glowIntensity;\n" +
            "\n" +
            "void main() {\n" +
            "    // Pulsing glow based on time\n" +
            "    float pulse = sin(u_time * 3.0) * 0.3 + 0.7;\n" +
            "    \n" +
            "    // Add glow intensity\n" +
            "    vec3 glowColor = v_color * (u_glowIntensity * pulse);\n" +
            "    \n" +
            "    // Clamp to prevent over-saturation\n" +
            "    glowColor = min(glowColor, vec3(1.0));\n" +
            "    \n" +
            "    gl_FragColor = vec4(glowColor, 1.0);\n" +
            "}\n";
        
        try {
            glowShader = backend.registerCustomShader(
                "glowEffect",
                vertexShader,
                fragmentShader
            );
            System.out.println("Custom glow shader compiled successfully!");
        } catch (RuntimeException e) {
            System.err.println("Failed to compile glow shader: " + e.getMessage());
            throw e;
        }
    }
    
    private void initializeGeometry() {
        WebGL2RenderingContext gl = backend.getGL();
        
        // Triangle vertices (3 vertices forming a large triangle)
        float cx = 400f, cy = 300f, size = 150f;
        float[] vertices = {
            cx, cy - size,              // top
            cx - size, cy + size,       // bottom left
            cx + size, cy + size        // bottom right
        };
        
        // Colors for each vertex (cyan gradient)
        float[] colors = {
            0.0f, 1.0f, 1.0f,  // cyan
            0.0f, 0.8f, 1.0f,  // slightly blue
            0.0f, 1.0f, 0.8f   // slightly green
        };
        
        // Create and upload vertex position buffer
        vertexBuffer = gl.createBuffer();
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, vertexBuffer);
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER,
            Float32Array.fromJavaArray(vertices).getBuffer(),
            WebGLRenderingContext.STATIC_DRAW);
        
        // Create and upload vertex color buffer
        colorBuffer = gl.createBuffer();
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, colorBuffer);
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER,
            Float32Array.fromJavaArray(colors).getBuffer(),
            WebGLRenderingContext.STATIC_DRAW);
        
        System.out.println("Geometry initialized");
    }
    
    private void render() {
        if (backend == null || glowShader == null) {
            return;
        }
        
        WebGL2RenderingContext gl = backend.getGL();
        
        // Clear the canvas
        gl.clearColor(0.1f, 0.1f, 0.15f, 1.0f);
        gl.clear(WebGLRenderingContext.COLOR_BUFFER_BIT);
        
        // Calculate time for animation
        float time = isAnimating ? (System.currentTimeMillis() - startTime) / 1000.0f : 0.0f;
        
        // Activate custom shader
        backend.activateCustomShader(glowShader);
        
        // Set uniforms
        glowShader.setUniform2f("u_resolution", (float)canvas.getWidth(), (float)canvas.getHeight());
        glowShader.setUniformMatrix3fv("u_transform", false, backend.getContextStack().getTransformArray());
        glowShader.setUniform1f("u_time", time);
        glowShader.setUniform1f("u_glowIntensity", 1.5f);
        
        // Bind vertex position buffer
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, vertexBuffer);
        glowShader.enableVertexAttribArray("a_position");
        glowShader.vertexAttribPointer("a_position", 2, WebGLRenderingContext.FLOAT, false, 0, 0);
        
        // Bind vertex color buffer
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, colorBuffer);
        glowShader.enableVertexAttribArray("a_color");
        glowShader.vertexAttribPointer("a_color", 3, WebGLRenderingContext.FLOAT, false, 0, 0);
        
        // Draw the triangle
        rasterizer.drawCustomGeometry(WebGLRenderingContext.TRIANGLES, 0, 3);
        
        // Deactivate custom shader
        backend.deactivateCustomShader();
    }
    
    private void requestAnimationFrame() {
        Window.requestAnimationFrame(new AnimationFrameCallback() {
            @Override
            public void onAnimationFrame(double timestamp) {
                render();
                requestAnimationFrame();
            }
        });
    }

    public static void main(String[] args) {
        LoggerFactory.setGlobalLevel(LogLevel.INFO);
        
        System.out.println("Starting Custom Shader Demo...");
        
        // Get canvas ID from args
        String canvasId = args.length > 0 ? args[0] : "custom-shader-canvas";
        
        // Set system property for canvas ID so the Applet uses this canvas
        System.setProperty("me.mdbell.awtea.classlib.java.awt.Applet.canvasId", canvasId);
        
        // Create the Applet
        CustomShaderDemo applet = new CustomShaderDemo();
        applet.setStub(new StubAppletStub());
        
        // Initialize and start the applet
        applet.init();
        applet.start();
    }
}
