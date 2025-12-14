# WASM Rasterizer Test Harness

This directory contains a standalone test harness for the awtea WASM rasterizer, allowing you to test and debug the rasterization engine in isolation without requiring the full Java/AWT stack.

## Overview

The test harness provides:

- **TypeScript interface** to the WASM rasterizer module
- **Deno-based testing** using Deno's built-in test framework
- **Isolated testing** without GUI, TSurface, TGraphics, or Java runtime
- **Utilities** for surface allocation, command creation, pixel inspection, and error handling
- **Example scenarios** demonstrating various drawing operations
- **WASM logging support** - see debug, info, warn, and error messages from the native code

## Prerequisites

- [Deno 2.0+](https://deno.land/) installed
- WASM module built (see Building section)

**Note on imports**: While JSR (`jsr:@std/assert`) is the preferred modern way to import Deno standard libraries, this test harness uses `https://deno.land/std/` imports for maximum compatibility. In environments with full JSR access, you can update the imports in `deno.json` and test files to use `jsr:@std/assert@1` instead.

## WASM Logging

The WASM rasterizer includes built-in logging support. When running tests or demos, you'll see log messages from the native WASM code:

- `[WASM DEBUG]` - Detailed debug information (surface creation, parameters, etc.)
- `[WASM INFO]` - Informational messages (successful operations)
- `[WASM WARN]` - Warnings (resource exhaustion, potential issues)
- `[WASM ERROR]` - Error messages (allocation failures, invalid parameters)

The test harness automatically provides the `wasm_log_callback` function that the WASM module requires for logging.

## Building the WASM Module

Before running tests, you need to build the WASM rasterizer module:

```bash
# From the awtea root directory
./gradlew :awtea-graphics:buildAwtRasterWasm
```

This will compile the C source files in `src/main/native/` to `build/wasm/awt_raster.wasm` using Emscripten (via Docker).

## Installing Deno

If you don't have Deno installed:

```bash
# Linux/macOS
curl -fsSL https://deno.land/install.sh | sh

# Windows (PowerShell)
irm https://deno.land/install.ps1 | iex

# Using package managers
# Homebrew (macOS/Linux)
brew install deno

# Chocolatey (Windows)
choco install deno
```

## Running Tests

### Run All Tests

```bash
cd awtea-graphics/src/test/deno
deno test --allow-read
```

### Run Specific Test File

```bash
deno test --allow-read basic_test.ts
deno test --allow-read advanced_test.ts
```

### Run with Verbose Output

```bash
deno test --allow-read --trace-ops
```

## Running the Demo

The demo file showcases various drawing capabilities with ASCII visualization:

```bash
cd awtea-graphics/src/test/deno
deno run --allow-read demo.ts
```

## Project Structure

```
awtea-graphics/src/test/deno/
├── README.md              # This file
├── wasm_rasterizer.ts     # Core TypeScript interface to WASM module
├── basic_test.ts          # Basic functionality tests
├── advanced_test.ts       # Advanced scenarios and edge cases
└── demo.ts                # Interactive demo/examples
```

## API Overview

### WasmRasterizer Class

The main class for interacting with the WASM rasterizer:

```typescript
import { WasmRasterizer, PixelFormat } from "./wasm_rasterizer.ts";

const rasterizer = new WasmRasterizer();
await rasterizer.load("../../../build/wasm/awt_raster.wasm");
```

### Surface Management

```typescript
// Allocate a surface
const surfaceId = rasterizer.allocateSurface(width, height, PixelFormat.ARGB);

// Get surface dimensions
const dims = rasterizer.getSurfaceDimensions(surfaceId);

// Access pixel buffer
const pixels = rasterizer.getSurfacePixels(surfaceId); // Uint32Array view
const pixelsCopy = rasterizer.copySurfacePixels(surfaceId); // Independent copy

// Free surface
rasterizer.freeSurface(surfaceId);
```

### Drawing Commands

Commands are batched and sent to the rasterizer for execution:

```typescript
// Create command buffer
const cmdBuffer = rasterizer.createCommandBuffer(maxCommands);

// Write commands
rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setColorCommand(0xFF0000FF)); // Red
rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.fillRectCommand(10, 10, 50, 50));

// Execute commands
rasterizer.renderCommands(surfaceId, cmdBuffer, 2); // 2 commands
```

### Available Commands

- **CMD_SET_COLOR**: Set foreground/background color
- **CMD_FILL_RECT**: Fill a rectangle with current color
- **CMD_DRAW_RECT**: Draw rectangle outline
- **CMD_DRAW_LINE**: Draw a line
- **CMD_CLEAR_RECT**: Clear a rectangle (set to transparent)
- **CMD_SET_CLIP_RECT**: Set clipping rectangle
- **CMD_SET_TRANSFORM**: Set 2D affine transform
- **CMD_BLIT_IMAGE**: Blit an image onto the surface

### Helper Functions

```typescript
// Color creation
const red = WasmRasterizer.makeARGB(255, 255, 0, 0); // ARGB

// Color extraction
const { a, r, g, b } = WasmRasterizer.extractARGB(pixelValue);

// Command creation helpers
const fillCmd = WasmRasterizer.fillRectCommand(x, y, width, height);
const drawCmd = WasmRasterizer.drawRectCommand(x, y, width, height);
const lineCmd = WasmRasterizer.drawLineCommand(x1, y1, x2, y2);
const clipCmd = WasmRasterizer.setClipRectCommand(x, y, width, height);
const blitCmd = WasmRasterizer.blitImageCommand(imageId, x, y);
const transformCmd = WasmRasterizer.setTransformCommand(m00, m01, m02, m10, m11, m12);
```

### Image Management

```typescript
// Register an image
const imageId = rasterizer.registerImage(width, height, PixelFormat.ARGB);

// Get image pixel buffer
const imagePixels = rasterizer.getImagePixels(imageId, width, height);

// Fill image with data
for (let i = 0; i < imagePixels.length; i++) {
  imagePixels[i] = someColor;
}

// Blit image onto surface
const cmdBuffer = rasterizer.createCommandBuffer(1);
rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.blitImageCommand(imageId, x, y));
rasterizer.renderCommands(surfaceId, cmdBuffer, 1);
```

## Example: Drawing a Red Rectangle

```typescript
import { WasmRasterizer } from "./wasm_rasterizer.ts";

const rasterizer = new WasmRasterizer();
await rasterizer.load("../../../build/wasm/awt_raster.wasm");

// Create surface
const surfaceId = rasterizer.allocateSurface(100, 100);

// Create commands
const cmdBuffer = rasterizer.createCommandBuffer(2);

// Set color to red
const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setColorCommand(red));

// Fill rectangle
rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.fillRectCommand(10, 10, 50, 50));

// Execute
rasterizer.renderCommands(surfaceId, cmdBuffer, 2);

// Read pixels
const pixels = rasterizer.copySurfacePixels(surfaceId);
console.log("Pixel at (10, 10):", pixels[10 * 100 + 10].toString(16));

// Cleanup
rasterizer.freeSurface(surfaceId);
```

## Example: Using Transforms

```typescript
// Create surface
const surfaceId = rasterizer.allocateSurface(100, 100);

const cmdBuffer = rasterizer.createCommandBuffer(3);

// Set translation transform (shift by 20, 20)
rasterizer.writeCommand(cmdBuffer, 0, 
  WasmRasterizer.setTransformCommand(1, 0, 20, 0, 1, 20));

// Set color and draw
const blue = WasmRasterizer.makeARGB(255, 0, 0, 255);
rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.setColorCommand(blue));
rasterizer.writeCommand(cmdBuffer, 2, WasmRasterizer.fillRectCommand(0, 0, 10, 10));

// Execute - rectangle at (0,0) will appear at (20,20)
rasterizer.renderCommands(surfaceId, cmdBuffer, 3);
```

## Extending the Test Harness

### Adding New Tests

1. Create a new test file or add to an existing one:

```typescript
import { assertEquals } from "jsr:@std/assert";
import { WasmRasterizer } from "./wasm_rasterizer.ts";

Deno.test("My new test", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load("../../../build/wasm/awt_raster.wasm");
  
  // Your test logic here
  
  assertEquals(actual, expected);
});
```

2. Run your test:

```bash
deno test --allow-read your_test_file.ts
```

### Adding New Command Types

If you add new commands to the C rasterizer:

1. Update `SurfaceOperation` enum in `wasm_rasterizer.ts`
2. Add a helper function for command creation
3. Add tests to validate the new command

### Exporting Images

To export rendered surfaces as images, you can use the PPM format (simple, text-based):

```typescript
async function savePPM(rasterizer: WasmRasterizer, surfaceId: number, filename: string) {
  const dims = rasterizer.getSurfaceDimensions(surfaceId);
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  
  let ppm = `P3\n${dims.width} ${dims.height}\n255\n`;
  for (let i = 0; i < pixels.length; i++) {
    const { r, g, b } = WasmRasterizer.extractARGB(pixels[i]);
    ppm += `${r} ${g} ${b} `;
  }
  
  await Deno.writeTextFile(filename, ppm);
}
```

For PNG output, consider using a PNG encoding library compatible with Deno.

## Debugging Tips

### Visualizing Surfaces

### Visualizing Surfaces

The demo includes a `printSurface` helper that uses ANSI escape sequences to display surfaces with actual colors in the terminal:

```typescript
function printSurface(rasterizer: WasmRasterizer, surfaceId: number) {
  const dims = rasterizer.getSurfaceDimensions(surfaceId);
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  
  for (let y = 0; y < dims.height; y++) {
    let row = "";
    for (let x = 0; x < dims.width; x++) {
      const pixel = pixels[y * dims.width + x];
      
      if (pixel > threshold) {
        // Extract color and display with ANSI background color
        const { r, g, b } = WasmRasterizer.extractARGB(pixel);
        const ansiColor = rgbToAnsi256(r, g, b);
        row += `\x1b[48;5;${ansiColor}m  \x1b[0m`;
      } else {
        row += "··";  // Empty pixel
      }
    }
    console.log(row);
  }
}
```

This displays red, green, blue, and other colors as colored blocks in terminals that support ANSI 256-color mode.

### Checking Pixel Values

```typescript
const pixels = rasterizer.copySurfacePixels(surfaceId);
const color = WasmRasterizer.extractARGB(pixels[y * width + x]);
console.log(`Pixel at (${x}, ${y}):`, color);
```

### Memory Debugging

The WASM module uses malloc/free for memory management. Surfaces and command buffers are allocated on demand. Always free surfaces when done to avoid memory leaks in long-running tests.

## Integration with Gradle

To run Deno tests as part of the Gradle build, add a task in `build.gradle.kts`:

```kotlin
tasks.register<Exec>("denoTest") {
    dependsOn("buildAwtRasterWasm")
    workingDir = file("src/test/deno")
    commandLine("deno", "test", "--allow-read")
}

tasks.named("test") {
    dependsOn("denoTest")
}
```

Then run:

```bash
./gradlew :awtea-graphics:denoTest
```

## CI Integration

Add Deno tests to your CI pipeline:

```yaml
# Example GitHub Actions workflow
- name: Setup Deno
  uses: denoland/setup-deno@v1
  with:
    deno-version: v2.x

- name: Build WASM
  run: ./gradlew :awtea-graphics:buildAwtRasterWasm

- name: Run Deno Tests
  run: |
    cd awtea-graphics/src/test/deno
    deno test --allow-read
```

## Troubleshooting

### WASM module not found

Ensure you've built the WASM module:
```bash
./gradlew :awtea-graphics:buildAwtRasterWasm
```

### Permission denied errors

Deno requires explicit permissions. Always use `--allow-read` when running tests:
```bash
deno test --allow-read
```

### Pixel values unexpected

Check:
- Color format (ARGB vs RGBA)
- Byte order (endianness)
- Alpha blending behavior
- Clipping rectangle settings

## Performance Considerations

- **Command batching**: Batch multiple commands in a single buffer for better performance
- **Surface reuse**: Reuse surfaces instead of allocating/freeing repeatedly
- **Large surfaces**: Test with realistic surface sizes (256x256, 512x512, etc.)
- **Memory**: Monitor WASM memory usage for large operations

## Known Limitations

- **Compositing**: Alpha compositing (Porter-Duff) may not be fully implemented in WASM backend
- **Fonts**: Text rendering is not available through this interface
- **Anti-aliasing**: Line and shape anti-aliasing may be limited

## References

- [Deno Documentation](https://deno.land/manual)
- [Deno Testing](https://deno.land/manual/testing)
- [WebAssembly in Deno](https://deno.land/manual/runtime/webassembly)
- Issue #53: Multi-context architecture
- Issue #55: Surface pooling
- Issue #56: Safety & best practices

## Contributing

To extend or improve the test harness:

1. Add tests for new functionality
2. Document new helper functions
3. Update this README with examples
4. Ensure tests pass in CI

## License

Same as the parent awtea project (Apache License 2.0).
