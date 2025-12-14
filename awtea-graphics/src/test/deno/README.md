# WASM Rasterizer and Java Test Harness

This directory contains test harnesses for the awtea graphics module, allowing you to test both the WASM rasterizer and Java code in isolation using Deno.

## Overview

The test harness provides:

- **TypeScript interface** to the WASM rasterizer module
- **Java test runner** using TeaVM to compile Java tests to JavaScript
- **Deno-based testing** using Deno's built-in test framework
- **Isolated testing** without GUI, TSurface, TGraphics, or full Java runtime
- **Utilities** for surface allocation, command creation, pixel inspection, and error handling
- **Example scenarios** demonstrating various drawing operations
- **WASM logging support** - see debug, info, warn, and error messages from the native code

## Prerequisites

- [Deno 2.0+](https://deno.land/) installed
- WASM module built (for WASM tests)
- Java tests compiled to JavaScript (for Java tests)

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

### Run WASM Tests Only

```bash
deno test --allow-read basic_test.ts
deno test --allow-read advanced_test.ts
```

### Run Java Tests Only

```bash
deno test --allow-read java_tests.ts
```

### Run via Gradle

You can also run tests using Gradle tasks:

```bash
# From the awtea root directory
./gradlew :awtea-graphics:denoTest       # Run WASM tests
./gradlew :awtea-graphics:denoTestJava   # Run Java tests
```

### Run with Verbose Output

```bash
deno test --allow-read --trace-ops
```

## Java Tests via TeaVM

### Overview

The Java test infrastructure allows you to write JUnit-style tests in Java and execute them in Deno via TeaVM compilation. This provides a way to test Java graphics code without requiring a full JVM.

### How It Works

1. **Java test classes** are written in `src/test/java/me/mdbell/awtea/gfx/test/` with standard `@Test` annotations
2. **Code generation** - The `generateDenoJUnitRunner` Gradle task scans for `@Test` methods and auto-generates `DenoJUnitRunner.java`
3. **Compilation** - Generated runner and test classes are compiled together
4. **TeaVM compilation** converts Java bytecode to JavaScript (ES2015 modules)
5. **Deno test runner** (`java_tests.ts`) imports and executes the compiled tests
6. **Test results** are reported through Deno's test framework

### Building Java Tests

Before running Java tests, compile them to JavaScript:

```bash
# From the awtea root directory
./gradlew :awtea-graphics:buildDenoJavaTests
```

This will:
- Auto-generate `DenoJUnitRunner.java` from `@Test` annotations
- Compile Java test classes
- Use TeaVM to convert them to JavaScript
- Output to `build/deno-tests/classes.js`

### Adding New Java Tests

1. Create a new test method in `SurfaceTests.java` or create a new test class with `@Test` annotations:

```java
@Test
public void testNewFeature() {
    assertEquals("Expected message", expected, actual);
    assertTrue("Condition should be true", condition);
}
```

2. Rebuild the tests:

```bash
./gradlew :awtea-graphics:buildDenoJavaTests
```

The `generateDenoJUnitRunner` task will automatically discover your new `@Test` method and register it with Deno. The test will appear as "Java: New Feature" in Deno's test output (method name is converted from camelCase to readable text).

**Note**: `DenoJUnitRunner.java` is auto-generated in `build/generated/test/java/` and should not be manually edited.

The test will appear as "Java: New feature test" in Deno's test output.

### Java Test Architecture

The Java tests use TeaVM's JSO (JavaScript Objects) API to directly integrate with Deno's test framework:

1. **Automatic test discovery** - Gradle task scans for `@Test` annotated methods at build time
2. **Code generation** - `DenoJUnitRunner.java` is auto-generated with registration code for each test
3. **Deno wrapper** (`Deno.java`) provides a Java interface to `Deno.test()`
4. **Test registration** happens in generated `DenoJUnitRunner.main()` which calls `deno.test(name, fn)` for each test
5. **Direct integration** means Java tests appear as individual Deno tests (not wrapped in a parent test)
6. **1-1 mapping** with Deno's test infrastructure provides proper test isolation and reporting

This approach has several advantages:
- **No manual maintenance** - Just add `@Test` methods and they're automatically discovered
- Tests appear individually in Deno's test output with descriptive names
- Failed assertions properly propagate to Deno's test runner
- No custom test result parsing required
- Standard JUnit assertions work correctly
- Each test is independently tracked by Deno

### Current Java Tests

The following tests are currently implemented (all prefixed with "Java:" in Deno output):

1. **Pixel format constants** - Validates pixel format constant values
2. **Pixel format range** - Checks MIN/MAX format bounds
3. **Pixel format validation** - Tests `isValidPixelFormat()` method
4. **Enum sequential values** - Verifies enum values are sequential
5. **Format range continuous** - Ensures no gaps in format range

These tests validate the Surface interface and pixel format constants, providing a foundation for testing enum synchronization across C, Java, and TypeScript.

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
