package me.mdbell.awtea.gfx.webgl;

import lombok.AccessLevel;
import lombok.Getter;
import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceBackend;
import me.mdbell.awtea.gl.Shaders;
import me.mdbell.awtea.util.jso.JSRecord;
import org.teavm.jso.core.JSString;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.webgl.*;

import java.util.HashMap;
import java.util.Map;

public final class WebGLSurfaceBackend implements SurfaceBackend {

    private final HTMLCanvasElement element;
    final WebGL2RenderingContext gl;
    final WebGLContextStack contextStack;

    // TODO: maybe refactor this to move programs to their own class (a.la.
    // WebGLProgramManager/WebGLRenderer)
    // the element would live there, and the backend would just create surfaces with
    // references to the renderer

    private final WebGLProgram colorProgram;
    private final WebGLProgram textureProgram;

    private final WebGLBuffer rectBuffer;

    // uniforms / attribs for color program
    private final WebGLUniformLocation uResolutionLocColor;
    private final WebGLUniformLocation uColorLoc;
    private final WebGLUniformLocation uTransformLocColor;
    private final WebGLUniformLocation uPickingModeLocColor; // Picking mode uniform
    private final WebGLUniformLocation uPickingColorLocColor; // Picking color uniform
    private final int aPositionLocColor;

    // uniforms / attribs for texture program
    private final WebGLUniformLocation uSwizzleModeLoc;
    private final WebGLUniformLocation uResolutionLocTex;
    private final WebGLUniformLocation uTransformLocTex;
    private final WebGLUniformLocation uTextureLoc;
    private final WebGLUniformLocation uPickingColorLoc; // Picking color uniform

    private final int aPositionLocTex;
    private final int aTexCoordLocTex;

    private final WebGLBuffer quadBuffer;
    private final WebGLBuffer quadTexCoordBuffer;

    @Getter(AccessLevel.PACKAGE)
    private final TexturePool texturePool;

    // state tracking

    private WebGLProgramType currentProgram = WebGLProgramType.NONE;

    private final Float32Array rectBufferArray = new Float32Array(12);
    private final ArrayBuffer rectArrayBuffer = rectBufferArray.getBuffer();

    // Picking buffer for GPU-based hit testing
    private WebGLPickingBuffer pickingBuffer;

    // Custom shader programs
    private final Map<String, CustomShaderProgram> customShaders = new HashMap<>();
    private CustomShaderProgram activeCustomShader = null;

    public WebGLSurfaceBackend(HTMLCanvasElement element) {
        this.element = element;

        // Maybe figure out how to pass in options later?
        JSRecord options = JSRecord.create();
        options.put("alpha", false);
        // Rendering goes through our own FBOs; a multisampled default
        // framebuffer would only add a resolve per composite.
        options.put("antialias", false);
        // Prefer the discrete GPU on dual-GPU machines.
        options.put("powerPreference", JSString.valueOf("high-performance"));
        this.gl = (WebGL2RenderingContext) element.getContext("webgl2", options);

        gl.enable(WebGLRenderingContext.BLEND);
        gl.blendFunc(WebGLRenderingContext.SRC_ALPHA, WebGLRenderingContext.ONE_MINUS_SRC_ALPHA);

        // Initialize the context stack for state management
        this.contextStack = new WebGLContextStack(gl);

        // ---- build shader programs ----
        this.colorProgram = createProgram(gl, COLOR_VERTEX_SRC, COLOR_FRAGMENT_SRC);
        this.textureProgram = createProgram(gl, TEX_VERTEX_SRC, TEX_FRAGMENT_SRC);

        // color program locations
        gl.useProgram(colorProgram);
        this.aPositionLocColor = gl.getAttribLocation(colorProgram, "a_position");
        this.uResolutionLocColor = gl.getUniformLocation(colorProgram, "u_resolution");
        this.uColorLoc = gl.getUniformLocation(colorProgram, "u_color");
        this.uTransformLocColor = gl.getUniformLocation(colorProgram, "u_transform");
        this.uPickingModeLocColor = gl.getUniformLocation(colorProgram, "u_pickingMode");
        this.uPickingColorLocColor = gl.getUniformLocation(colorProgram, "u_pickingColor");

        // Set the color uniform location in the context stack for automatic color
        // application
        this.contextStack.setColorUniformLocation(this.uColorLoc);

        // texture program locations
        gl.useProgram(textureProgram);
        this.aPositionLocTex = gl.getAttribLocation(textureProgram, "a_position");
        this.aTexCoordLocTex = gl.getAttribLocation(textureProgram, "a_texCoord");
        this.uResolutionLocTex = gl.getUniformLocation(textureProgram, "u_resolution");
        this.uTransformLocTex = gl.getUniformLocation(textureProgram, "u_transform");
        this.uSwizzleModeLoc = gl.getUniformLocation(textureProgram, "u_swizzleMode");
        this.uTextureLoc = gl.getUniformLocation(textureProgram, "u_texture");
        this.uPickingColorLoc = gl.getUniformLocation(textureProgram, "u_pickingColor");
        gl.uniform1i(uTextureLoc, 0); // texture unit 0

        // ---- buffers ----
        // simple rect buffer: two triangles in [0,0]-[1,1] space
        rectBuffer = gl.createBuffer();

        quadBuffer = gl.createBuffer();
        quadTexCoordBuffer = gl.createBuffer();

        texturePool = new TexturePool(gl);
    }

    void setRectBuffer(float x, float y, float width, float height) {

        // two triangles forming a rectangle
        // first triangle (x,y)-(x+width,y)-(x,y+height)
        rectBufferArray.set(0, x);
        rectBufferArray.set(1, y);
        rectBufferArray.set(2, x + width);
        rectBufferArray.set(3, y);
        rectBufferArray.set(4, x);
        rectBufferArray.set(5, y + height);

        // second triangle (x,y+height)-(x+width,y)-(x+width,y+height)
        rectBufferArray.set(6, x);
        rectBufferArray.set(7, y + height);
        rectBufferArray.set(8, x + width);
        rectBufferArray.set(9, y);
        rectBufferArray.set(10, x + width);
        rectBufferArray.set(11, y + height);

        uploadRectVertices();
    }

    void uploadRectVertices() {
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, rectArrayBuffer, WebGLRenderingContext.STREAM_DRAW);
    }

    void uploadQuadVertices(float[] verts, float[] uvs) {
        ArrayBuffer vertBuf = Float32Array.fromJavaArray(verts).getBuffer();
        ArrayBuffer uvBuf = Float32Array.fromJavaArray(uvs).getBuffer();

        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, quadBuffer);
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, vertBuf, WebGLRenderingContext.STREAM_DRAW);

        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, quadTexCoordBuffer);
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, uvBuf, WebGLRenderingContext.STREAM_DRAW);
    }

    public Surface createScreenSurface(int width, int height) {
        return new WebGLSurface(this, width, height, true);
    }

    @Override
    public Surface createCompatibleSurface(Object cm, Object raster, boolean isRasterPremultiplied,
                                           int bufferedImageType) {
        return null;
    }

    @Override
    public Surface createCompatibleSurface(int width, int height, int bufferedImageType) {
        return null;
    }

    protected void useColorProgram(int width, int height) {
        useColorProgram(width, height, null);
    }

    protected void useColorProgram(int width, int height, float[] pickingColor) {
        if (currentProgram != WebGLProgramType.COLOR) {
            gl.useProgram(colorProgram);
            currentProgram = WebGLProgramType.COLOR;
        }

        // Apply state from context stack (updates transform array, blend, clip)
        contextStack.apply();

        // Apply color uniform (only valid for color program)
        contextStack.applyColorUniform();

        gl.uniform2f(uResolutionLocColor,
                (float) width,
                (float) height);
        gl.uniformMatrix3fv(uTransformLocColor, false, contextStack.getTransformArray());

        // Set picking mode: 1 if picking color provided, 0 otherwise
        if (pickingColor != null) {
            gl.uniform1i(uPickingModeLocColor, 1);
            gl.uniform4f(uPickingColorLocColor, pickingColor[0], pickingColor[1], pickingColor[2], 1.0f);
        } else {
            gl.uniform1i(uPickingModeLocColor, 0);
        }

        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, rectBuffer);
        gl.enableVertexAttribArray(aPositionLocColor);
        gl.vertexAttribPointer(aPositionLocColor, 2,
                WebGLRenderingContext.FLOAT,
                false, 0, 0);
    }

    void useTextureProgram(SwizzleMode mode, int width, int height) {
        useTextureProgram(mode, width, height, null);
    }

    void useTextureProgram(SwizzleMode mode, int width, int height, float[] pickingColor) {

        if (currentProgram != WebGLProgramType.TEXTURE) {
            gl.useProgram(textureProgram);
            currentProgram = WebGLProgramType.TEXTURE;
        }

        // Apply state from context stack (updates transform array)
        contextStack.apply();

        gl.uniform2f(uResolutionLocTex,
                (float) width,
                (float) height);

        gl.uniformMatrix3fv(uTransformLocTex, false,
                contextStack.getTransformArray());

        gl.uniform1i(uSwizzleModeLoc, mode.ordinal());

        // Set picking color if in picking mode
        if (mode == SwizzleMode.PICKING && pickingColor != null) {
            gl.uniform4f(uPickingColorLoc, pickingColor[0], pickingColor[1], pickingColor[2], 1.0f);
        }

        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, quadBuffer);
        gl.enableVertexAttribArray(aPositionLocTex);
        gl.vertexAttribPointer(aPositionLocTex, 2, WebGLRenderingContext.FLOAT, false, 0, 0);

        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, quadTexCoordBuffer);
        gl.enableVertexAttribArray(aTexCoordLocTex);
        gl.vertexAttribPointer(aTexCoordLocTex, 2,
                WebGLRenderingContext.FLOAT,
                false, 0, 0);
    }

    private WebGLProgram createProgram(WebGLRenderingContext gl, String vsSource, String fsSource) {
        WebGLShader vs = compileShader(gl, WebGLRenderingContext.VERTEX_SHADER, vsSource);
        WebGLShader fs = compileShader(gl, WebGLRenderingContext.FRAGMENT_SHADER, fsSource);
        WebGLProgram program = gl.createProgram();
        gl.attachShader(program, vs);
        gl.attachShader(program, fs);
        gl.linkProgram(program);
        if (!gl.getProgramParameterb(program, WebGLRenderingContext.LINK_STATUS)) {
            String log = gl.getProgramInfoLog(program);
            throw new RuntimeException("Could not link WebGL program: " + log);
        }
        return program;
    }

    private WebGLShader compileShader(WebGLRenderingContext gl, int type, String src) {
        WebGLShader shader = gl.createShader(type);
        gl.shaderSource(shader, src);
        gl.compileShader(shader);
        if (!gl.getShaderParameterb(shader, WebGLRenderingContext.COMPILE_STATUS)) {
            String log = gl.getShaderInfoLog(shader);
            throw new RuntimeException("Could not compile shader: " + log);
        }
        return shader;
    }

    public void notifyCustomRendering() {
        currentProgram = WebGLProgramType.CUSTOM;
    }

    private enum WebGLProgramType {
        NONE,
        COLOR,
        TEXTURE,
        CUSTOM
    }

    // TODO: it would be nice to remove this and just use Surface pixel format
    // directly
    protected enum SwizzleMode {
        NONE,
        ARGB_TO_RGBA,
        RGB_TO_RGBA,
        BGR_TO_ABGR,
        PICKING // Special mode for picking buffer - outputs solid color from uniform
    }

    // Picking buffer management

    /**
     * Creates and initializes the picking buffer for GPU-based hit testing.
     * Should be called once when enabling picking for this backend.
     *
     * @param width  the picking buffer width
     * @param height the picking buffer height
     */
    public void createPickingBuffer(int width, int height) {
        if (pickingBuffer != null) {
            pickingBuffer.destroy();
        }
        pickingBuffer = new WebGLPickingBuffer(gl, width, height);
        pickingBuffer.setBackend(this); // Set backend reference for rendering
    }

    /**
     * Gets the picking buffer, creating it if necessary.
     *
     * @return the picking buffer, or null if not initialized
     */
    public WebGLPickingBuffer getPickingBuffer() {
        return pickingBuffer;
    }

    /**
     * Checks if debug picking visualization mode is enabled via system property.
     * When enabled, the picking buffer debug visualization is rendered instead of
     * normal output.
     *
     * @return true if me.mdbell.awtea.hit_test.debug_render is set to true
     */
    public boolean isPickingDebugRenderEnabled() {
        String prop = System.getProperty("me.mdbell.awtea.hit_test.debug_render");
        return "true".equalsIgnoreCase(prop);
    }

    /**
     * Renders the picking buffer debug visualization to the main framebuffer if
     * enabled.
     * This is called after normal rendering to potentially replace the output.
     *
     * @param screenWidth  the screen width
     * @param screenHeight the screen height
     */
    public void renderPickingDebugIfEnabled(int screenWidth, int screenHeight) {
        if (pickingBuffer != null && isPickingDebugRenderEnabled()) {
            pickingBuffer.renderDebugVisualization(null, screenWidth, screenHeight);
        }
    }

    /**
     * Draws a texture to the screen at the specified position and size.
     * Used internally for debug visualization rendering.
     *
     * @param texture    the WebGL texture to draw
     * @param x          the x position on screen
     * @param y          the y position on screen
     * @param destWidth  the destination width
     * @param destHeight the destination height
     * @param srcWidth   the source texture width
     * @param srcHeight  the source texture height
     */
    public void drawDebugTexture(WebGLTexture texture, int x, int y, int destWidth, int destHeight, int srcWidth,
                                 int srcHeight) {
        // Use texture program with NONE swizzle mode
        useTextureProgram(SwizzleMode.NONE, destWidth, destHeight);

        // Bind texture
        gl.activeTexture(WebGLRenderingContext.TEXTURE0);
        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, texture);

        // Set up quad vertices for full screen
        float[] vertices = {
                x, y,
                x + destWidth, y,
                x, y + destHeight,
                x, y + destHeight,
                x + destWidth, y,
                x + destWidth, y + destHeight
        };

        // Flip V coordinates because WebGL textures have (0,0) at bottom-left
        // but we want to display with (0,0) at top-left
        float[] uvs = {
                0f, 1f,  // top-left -> bottom-left in texture
                1f, 1f,  // top-right -> bottom-right in texture
                0f, 0f,  // bottom-left -> top-left in texture
                0f, 0f,  // bottom-left -> top-left in texture
                1f, 1f,  // top-right -> bottom-right in texture
                1f, 0f   // bottom-right -> top-right in texture
        };

        // Upload vertex data
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, quadBuffer);
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER,
                org.teavm.jso.typedarrays.Float32Array.fromJavaArray(vertices).getBuffer(),
                WebGLRenderingContext.DYNAMIC_DRAW);
        gl.enableVertexAttribArray(aPositionLocTex);
        gl.vertexAttribPointer(aPositionLocTex, 2, WebGLRenderingContext.FLOAT, false, 0, 0);

        // Upload texture coordinate data
        gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, quadTexCoordBuffer);
        gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER,
                org.teavm.jso.typedarrays.Float32Array.fromJavaArray(uvs).getBuffer(),
                WebGLRenderingContext.DYNAMIC_DRAW);
        gl.enableVertexAttribArray(aTexCoordLocTex);
        gl.vertexAttribPointer(aTexCoordLocTex, 2, WebGLRenderingContext.FLOAT, false, 0, 0);

        // Draw
        gl.drawArrays(WebGLRenderingContext.TRIANGLES, 0, 6);

        // Cleanup
        gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, null);
    }

    /**
     * Checks if this backend has a picking buffer.
     */
    public boolean hasPickingBuffer() {
        return pickingBuffer != null;
    }

    /**
     * Destroys the picking buffer and releases its resources.
     */
    public void destroyPickingBuffer() {
        if (pickingBuffer != null) {
            pickingBuffer.destroy();
            pickingBuffer = null;
        }
    }

    // Custom Shader Management

    /**
     * Registers a custom shader program for use in rendering.
     *
     * @param name           a unique name to identify this shader
     * @param vertexSource   GLSL vertex shader source code
     * @param fragmentSource GLSL fragment shader source code
     * @return the created CustomShaderProgram
     * @throws RuntimeException if a shader with this name already exists or if compilation fails
     */
    public CustomShaderProgram registerCustomShader(String name, String vertexSource, String fragmentSource) {
        if (customShaders.containsKey(name)) {
            throw new RuntimeException("Custom shader '" + name + "' is already registered");
        }

        CustomShaderProgram shader = new CustomShaderProgram(gl, name, vertexSource, fragmentSource);
        customShaders.put(name, shader);
        return shader;
    }

    /**
     * Gets a registered custom shader by name.
     *
     * @param name the shader name
     * @return the CustomShaderProgram, or null if not found
     */
    public CustomShaderProgram getCustomShader(String name) {
        return customShaders.get(name);
    }

    /**
     * Activates a custom shader for rendering. The shader will remain active
     * until another shader is activated or useColorProgram/useTextureProgram is called.
     *
     * @param name the name of the shader to activate
     * @throws RuntimeException if the shader is not found
     */
    public void activateCustomShader(String name) {
        CustomShaderProgram shader = customShaders.get(name);
        if (shader == null) {
            throw new RuntimeException("Custom shader '" + name + "' not found");
        }
        activateCustomShader(shader);
    }

    /**
     * Activates a custom shader for rendering. The shader will remain active
     * until another shader is activated or useColorProgram/useTextureProgram is called.
     *
     * @param shader the shader to activate
     */
    public void activateCustomShader(CustomShaderProgram shader) {
        shader.use();
        activeCustomShader = shader;
        currentProgram = WebGLProgramType.CUSTOM;
    }

    /**
     * Deactivates the current custom shader. The next rendering operation
     * will use the appropriate built-in shader.
     */
    public void deactivateCustomShader() {
        activeCustomShader = null;
        currentProgram = WebGLProgramType.NONE;
    }

    /**
     * Gets the currently active custom shader, if any.
     *
     * @return the active custom shader, or null if none is active
     */
    public CustomShaderProgram getActiveCustomShader() {
        return activeCustomShader;
    }

    /**
     * Unregisters and disposes a custom shader.
     *
     * @param name the name of the shader to remove
     */
    public void unregisterCustomShader(String name) {
        CustomShaderProgram shader = customShaders.remove(name);
        if (shader != null) {
            if (activeCustomShader == shader) {
                deactivateCustomShader();
            }
            shader.dispose();
        }
    }

    /**
     * Disposes all custom shaders.
     */
    public void disposeAllCustomShaders() {
        for (CustomShaderProgram shader : customShaders.values()) {
            shader.dispose();
        }
        customShaders.clear();
        activeCustomShader = null;
    }

    /**
     * Gets the WebGL context for advanced custom rendering operations.
     *
     * @return the WebGL2 rendering context
     */
    public WebGL2RenderingContext getGL() {
        return gl;
    }

    /**
     * Gets the context stack for accessing current transform, clip, and color state.
     *
     * @return the WebGL context stack
     */
    public WebGLContextStack getContextStack() {
        return contextStack;
    }

    // Shaders

    private static final String COLOR_VERTEX_SRC = Shaders.colorVertex();

    private static final String COLOR_FRAGMENT_SRC = Shaders.colorFragment();

    private static final String TEX_VERTEX_SRC = Shaders.textureVertex();

    private static final String TEX_FRAGMENT_SRC = Shaders.textureFragment();
}
