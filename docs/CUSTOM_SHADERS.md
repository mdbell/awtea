# Custom Shader API

The WebGL backend supports custom GLSL shaders for advanced rendering effects. This document describes the API for creating, managing, and using custom shaders.

## Overview

The custom shader API allows you to:
- Upload and compile custom vertex and fragment shaders
- Bind custom uniforms and attributes
- Render custom geometry with your own shaders
- Access engine-provided uniforms for standard transformations and state

## Basic Usage

### Command Queueing Pattern (Recommended)

The recommended way to use custom shaders is through the command queueing pattern within `paint(Graphics g)`. This ensures proper timing and integration with the rendering pipeline:

```java
public void paint(Graphics g) {
    // Draw standard AWT content first
    g.setColor(Color.WHITE);
    g.drawString("Before shader", 10, 20);
    
    // Get the current WebGL shader context
    WebGLShaderContext ctx = WebGLShaderContext.getCurrentContext();
    if (ctx != null) {
        // Initialize shader on first use
        if (ctx.getShader("myShader") == null) {
            ctx.getBackend().registerCustomShader("myShader", vertexSource, fragmentSource);
        }
        
        // Queue custom shader rendering
        CustomShaderProgram shader = ctx.getShader("myShader");
        if (shader != null) {
            ctx.queueShaderCall(shader, (backend, rasterizer) -> {
                // Set uniforms
                shader.setUniform2f("u_resolution", width, height);
                shader.setUniform1f("u_time", elapsedSeconds);
                
                // Bind vertex data and draw
                WebGL2RenderingContext gl = backend.getGL();
                gl.bindBuffer(ARRAY_BUFFER, vertexBuffer);
                shader.enableVertexAttribArray("a_position");
                shader.vertexAttribPointer("a_position", 2, FLOAT, false, 0, 0);
                rasterizer.drawCustomGeometry(TRIANGLES, 0, vertexCount);
            });
        }
    }
    
    // Draw standard AWT content after
    g.drawString("After shader", 10, 40);
}
```

**Key Points:**
- The context is automatically set up when the Graphics object is created (for WebGL backends)
- `getCurrentContext()` returns null for non-WebGL backends
- Shader callbacks are queued and executed at the correct point in the rendering pipeline
- The shader is automatically activated before the callback and deactivated after
- Commands queued in `paint()` are executed on the next animation frame

### Direct API Usage (Advanced)

For advanced use cases where you need direct control, you can access the backend directly:

```java
// From a WebGLSurface's rasterizer
WebGLRasterizer rasterizer = (WebGLRasterizer) surface.createRasterizer();
WebGLSurfaceBackend backend = rasterizer.getBackend();
```

### Register a Custom Shader

Create and register a shader program with vertex and fragment shader sources:

```java
String vertexShader = """
    precision mediump float;
    
    attribute vec2 a_position;
    uniform vec2 u_resolution;
    uniform mat3 u_transform;
    
    void main() {
        vec3 pos = u_transform * vec3(a_position, 1.0);
        vec2 clipSpace = (pos.xy / u_resolution) * 2.0 - 1.0;
        gl_Position = vec4(clipSpace * vec2(1, -1), 0, 1);
    }
    """;

String fragmentShader = """
    precision mediump float;
    uniform vec4 u_color;
    
    void main() {
        gl_FragColor = u_color;
    }
    """;

CustomShaderProgram shader = backend.registerCustomShader(
    "myShader", 
    vertexShader, 
    fragmentShader
);
```

### Activate and Use the Shader (Direct API)

For direct API usage (not using the command queueing pattern):

```java
backend.activateCustomShader("myShader");

// Set custom uniforms
CustomShaderProgram shader = backend.getActiveCustomShader();
shader.setUniform4f("u_color", 1.0f, 0.0f, 0.0f, 1.0f); // Red color
shader.setUniform2f("u_resolution", 800f, 600f);

// Set engine-provided uniforms (automatically managed)
shader.setUniformMatrix3fv("u_transform", false, 
    backend.getContextStack().getTransformArray());
```

### Upload Vertex Data and Render (Direct API)

```java
// Create and bind vertex buffer
WebGL2RenderingContext gl = backend.getGL();
WebGLBuffer vertexBuffer = gl.createBuffer();
gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, vertexBuffer);

// Upload vertex positions (triangle)
float[] vertices = {
    0.0f, 0.0f,      // vertex 1
    100.0f, 0.0f,    // vertex 2
    50.0f, 100.0f    // vertex 3
};
gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, 
    Float32Array.fromJavaArray(vertices).getBuffer(),
    WebGLRenderingContext.STATIC_DRAW);

// Set up vertex attribute pointer
shader.enableVertexAttribArray("a_position");
shader.vertexAttribPointer("a_position", 2, WebGLRenderingContext.FLOAT, false, 0, 0);

// Draw the geometry
rasterizer.drawCustomGeometry(WebGLRenderingContext.TRIANGLES, 0, 3);

// Clean up
gl.deleteBuffer(vertexBuffer);
backend.deactivateCustomShader();
```

## Engine-Provided Uniforms

The following uniforms are commonly used and can be obtained from the context stack:

### u_resolution (vec2)
Screen or surface resolution in pixels. Used for coordinate transformation.

```glsl
uniform vec2 u_resolution;
```

Access from Java:
```java
int width = surface.getWidth();
int height = surface.getHeight();
shader.setUniform2f("u_resolution", (float)width, (float)height);
```

### u_transform (mat3)
Current transformation matrix (2D affine transform as 3x3 matrix).

```glsl
uniform mat3 u_transform;
```

Access from Java:
```java
shader.setUniformMatrix3fv("u_transform", false, 
    backend.getContextStack().getTransformArray());
```

### u_color (vec4)
Current foreground color from the graphics context.

```glsl
uniform vec4 u_color;
```

Access from Java:
```java
Color color = backend.getContextStack().getForeground();
float r = color.getRed() / 255.0f;
float g = color.getGreen() / 255.0f;
float b = color.getBlue() / 255.0f;
float a = color.getAlpha() / 255.0f;
shader.setUniform4f("u_color", r, g, b, a);
```

### Time Uniform (Custom)
For animated effects, you can provide a time uniform:

```glsl
uniform float u_time;
```

```java
shader.setUniform1f("u_time", System.currentTimeMillis() / 1000.0f);
```

## Standard Vertex Shader Template

Here's a standard vertex shader that works with the engine's coordinate system and transformations:

```glsl
precision mediump float;

attribute vec2 a_position;
uniform vec2 u_resolution;
uniform mat3 u_transform;

void main() {
    // Apply 2D transform (translation, rotation, scale)
    vec3 pos = u_transform * vec3(a_position, 1.0);
    
    // Convert to normalized device coordinates (NDC)
    vec2 zeroToOne = pos.xy / u_resolution;
    vec2 zeroToTwo = zeroToOne * 2.0;
    vec2 clipSpace = zeroToTwo - 1.0;
    
    // Flip Y axis (WebGL Y+ is up, screen Y+ is down)
    gl_Position = vec4(clipSpace * vec2(1, -1), 0, 1);
}
```

## Standard Attributes

### a_position (vec2)
Vertex position in screen space (pixels from top-left corner).

```glsl
attribute vec2 a_position;
```

### a_texCoord (vec2)
Texture coordinates (for textured rendering).

```glsl
attribute vec2 a_texCoord;
varying vec2 v_texCoord;

void main() {
    v_texCoord = a_texCoord;
    // ... position calculation
}
```

## API Reference

### WebGLSurfaceBackend

#### registerCustomShader(name, vertexSource, fragmentSource)
Compiles and registers a new shader program.

**Parameters:**
- `name` (String): Unique identifier for the shader
- `vertexSource` (String): GLSL vertex shader source code
- `fragmentSource` (String): GLSL fragment shader source code

**Returns:** `CustomShaderProgram`

**Throws:** `RuntimeException` if compilation/linking fails or name already exists

#### getCustomShader(name)
Retrieves a registered shader by name.

**Returns:** `CustomShaderProgram` or `null`

#### activateCustomShader(name)
Activates a shader for rendering. All subsequent custom geometry draws will use this shader.

**Throws:** `RuntimeException` if shader not found

#### deactivateCustomShader()
Deactivates the current custom shader. Next rendering will use built-in shaders.

#### getActiveCustomShader()
Gets the currently active custom shader.

**Returns:** `CustomShaderProgram` or `null`

#### unregisterCustomShader(name)
Removes and disposes a custom shader.

#### disposeAllCustomShaders()
Disposes all registered custom shaders.

#### getGL()
Gets the WebGL2 rendering context for advanced operations.

**Returns:** `WebGL2RenderingContext`

#### getContextStack()
Gets the context stack for accessing transform, clip, and color state.

**Returns:** `WebGLContextStack`

### CustomShaderProgram

#### setUniform1f(name, value)
Sets a float uniform.

#### setUniform2f(name, x, y)
Sets a vec2 uniform.

#### setUniform3f(name, x, y, z)
Sets a vec3 uniform.

#### setUniform4f(name, x, y, z, w)
Sets a vec4 uniform.

#### setUniform1i(name, value)
Sets an int uniform (useful for sampler2D texture units).

#### setUniformMatrix3fv(name, transpose, value)
Sets a mat3 uniform.

**Parameters:**
- `transpose` (boolean): Whether to transpose the matrix
- `value` (float[]): 9 floats in column-major order

#### setUniformMatrix4fv(name, transpose, value)
Sets a mat4 uniform.

**Parameters:**
- `transpose` (boolean): Whether to transpose the matrix
- `value` (float[]): 16 floats in column-major order

#### enableVertexAttribArray(name)
Enables a vertex attribute array.

#### disableVertexAttribArray(name)
Disables a vertex attribute array.

#### vertexAttribPointer(name, size, type, normalized, stride, offset)
Sets up a vertex attribute pointer.

**Parameters:**
- `name` (String): Attribute name
- `size` (int): Components per vertex (1-4)
- `type` (int): Data type (e.g., `WebGLRenderingContext.FLOAT`)
- `normalized` (boolean): Whether to normalize the data
- `stride` (int): Byte offset between consecutive attributes
- `offset` (int): Byte offset to first attribute

#### use()
Activates this shader for rendering.

#### dispose()
Releases GPU resources. The shader cannot be used after disposal.

### WebGLRasterizer

#### drawCustomGeometry(mode, first, count)
Draws custom geometry using the active shader.

**Parameters:**
- `mode` (int): Primitive type (e.g., `WebGLRenderingContext.TRIANGLES`)
- `first` (int): Starting vertex index
- `count` (int): Number of vertices to render

**Throws:** `IllegalStateException` if no custom shader is active

#### drawCustomElements(mode, count, type, offset)
Draws indexed custom geometry using the active shader.

**Parameters:**
- `mode` (int): Primitive type
- `count` (int): Number of elements to render
- `type` (int): Index type (e.g., `WebGLRenderingContext.UNSIGNED_SHORT`)
- `offset` (int): Byte offset in the element array buffer

**Throws:** `IllegalStateException` if no custom shader is active

#### getBackend()
Gets the WebGLSurfaceBackend for shader management.

**Returns:** `WebGLSurfaceBackend`

## Complete Example: Glowing Outline Effect

See the `examples/custom-shader-demo` directory for a complete working example that demonstrates:
- Registering a custom shader with glow effect
- Rendering custom geometry with time-based animation
- Managing shader uniforms and attributes
- Proper resource cleanup

## Best Practices

1. **Precision Qualifiers**: Always specify precision in both vertex and fragment shaders to avoid linking errors:
   ```glsl
   // Add this at the top of BOTH vertex and fragment shaders
   precision mediump float;
   ```
   Without matching precision qualifiers, shared uniforms (like `u_time`) will fail to link between shaders.

2. **Reuse Shaders**: Register shaders once and reuse them. Don't create new shaders every frame.

3. **Cache Uniform Locations**: The `CustomShaderProgram` caches uniform/attribute locations automatically.

4. **Clean Up Resources**: Always dispose shaders when done:
   ```java
   backend.disposeAllCustomShaders();
   ```

5. **Error Handling**: Wrap shader compilation in try-catch blocks:
   ```java
   try {
       CustomShaderProgram shader = backend.registerCustomShader(...);
   } catch (RuntimeException e) {
       System.err.println("Shader compilation failed: " + e.getMessage());
   }
   ```

6. **Use Engine Uniforms**: Leverage engine-provided uniforms for consistency:
   - Always use `u_transform` for proper coordinate transformations
   - Use `u_resolution` for screen-space calculations
   - Respect the engine's coordinate system (top-left origin, Y+ down)

7. **Performance**: Minimize state changes. Batch draw calls that use the same shader.

8. **Debugging**: Use browser developer tools to inspect WebGL errors. Enable WebGL error checking in development builds.

## Limitations

- Custom shaders are only available in the WebGL backend (not WASM or software backends)
- Shaders must be written in GLSL ES 2.0 (WebGL 1.0 compatible)
- Maximum number of shaders limited by GPU memory
- Picking buffer integration is not automatic for custom geometry (manual implementation required)

## Future Enhancements

Planned features for future releases:
- Built-in shader library with common effects
- Post-processing shader support with framebuffer targets
- Geometry shader support (WebGL 2.0)
- Compute shader support for GPGPU operations
- Shader hot-reloading for development
