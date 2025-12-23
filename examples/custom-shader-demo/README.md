# Custom Shader Demo

This example demonstrates the custom shader API in the WebGL rendering backend.

## Features

- **Glowing Triangle**: A large triangle with a pulsing glow effect
- **Custom Vertex Shader**: Applies wave animation and coordinate transformation
- **Custom Fragment Shader**: Implements time-based glow intensity with color gradients
- **Interactive**: Click to pause/resume animation
- **Proper Resource Management**: Demonstrates shader registration and geometry setup

## Building and Running

Build the example:
```bash
./gradlew :examples:custom-shader-demo:build --no-daemon
```

The output will be in `examples/custom-shader-demo/build/dist/`. Open `index.html` in a web browser.

## What This Demonstrates

1. **Shader Registration**: How to compile and register custom GLSL shaders
2. **Uniform Management**: Setting uniforms for time, resolution, and transform
3. **Vertex Attributes**: Binding position and color attributes
4. **Custom Rendering**: Using `drawCustomGeometry()` for custom draw calls
5. **Animation**: Time-based shader effects with wave distortion and pulsing glow
6. **Backend Access**: Using `SurfaceBackendFactory` to get WebGL backend

## Code Structure

- `CustomShaderDemo.java`: Main demo with shader setup, geometry initialization, and animation loop

## Shader Details

### Vertex Shader
- Receives position and color attributes
- Applies transform matrix from engine
- Adds wave animation based on time and vertical position
- Converts to WebGL clip space

### Fragment Shader
- Creates pulsing glow effect with time-based modulation
- Multiplies vertex color by glow intensity
- Prevents over-saturation with min/max clamping

## Key API Usage

```java
// Get WebGL backend via factory
backend = (WebGLSurfaceBackend) SurfaceBackendFactory.getWebGLBackend(canvas);

// Register shader
glowShader = backend.registerCustomShader("glowEffect", vertexSrc, fragmentSrc);

// Activate and set uniforms
backend.activateCustomShader(glowShader);
glowShader.setUniform1f("u_time", elapsedSeconds);
glowShader.setUniformMatrix3fv("u_transform", false, backend.getContextStack().getTransformArray());

// Bind vertex data
gl.bindBuffer(ARRAY_BUFFER, vertexBuffer);
glowShader.enableVertexAttribArray("a_position");
glowShader.vertexAttribPointer("a_position", 2, FLOAT, false, 0, 0);

// Render
rasterizer.drawCustomGeometry(TRIANGLES, 0, 3);
```

## See Also

- [CUSTOM_SHADERS.md](../../docs/CUSTOM_SHADERS.md) - Complete API documentation
- [WebGL Tutorial](https://developer.mozilla.org/en-US/docs/Web/API/WebGL_API/Tutorial)
