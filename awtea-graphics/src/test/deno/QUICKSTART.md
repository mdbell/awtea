# Quick Start Guide - WASM Rasterizer Testing

## Running Tests

### Option 1: Using Gradle (Recommended)
```bash
# From the repository root
./gradlew :awtea-graphics:denoTest
```

This will:
1. Build the WASM module if needed
2. Run all Deno tests
3. Report results

### Option 2: Using Deno Directly
```bash
# Navigate to test directory
cd awtea-graphics/src/test/deno

# Run all tests
deno test --allow-read

# Run specific test file
deno test --allow-read basic_test.ts

# Run demo
deno run --allow-read demo.ts
```

## Prerequisites

### Install Deno
```bash
# Linux/macOS
curl -fsSL https://deno.land/install.sh | sh

# Windows (PowerShell)
irm https://deno.land/install.ps1 | iex

# Or use package managers
brew install deno        # macOS/Linux
choco install deno       # Windows
```

### Build WASM Module
```bash
./gradlew :awtea-graphics:buildAwtRasterWasm
```

Requires Docker for Emscripten compiler.

## Quick Example

```typescript
import { WasmRasterizer } from "./wasm_rasterizer.ts";

// Load WASM module
const rasterizer = new WasmRasterizer();
await rasterizer.load("../../../build/wasm/awt_raster.wasm");

// Create a surface
const surfaceId = rasterizer.allocateSurface(100, 100);

// Create command buffer
const cmdBuffer = rasterizer.createCommandBuffer(2);

// Draw a red rectangle
const red = WasmRasterizer.makeARGB(255, 255, 0, 0);
rasterizer.writeCommand(cmdBuffer, 0, WasmRasterizer.setColorCommand(red));
rasterizer.writeCommand(cmdBuffer, 1, WasmRasterizer.fillRectCommand(10, 10, 50, 50));

// Execute
rasterizer.renderCommands(surfaceId, cmdBuffer, 2);

// Inspect pixels
const pixels = rasterizer.copySurfacePixels(surfaceId);
console.log(`Pixel at (10, 10): 0x${pixels[10 * 100 + 10].toString(16)}`);

// Cleanup
rasterizer.freeSurface(surfaceId);
```

## File Structure

```
awtea-graphics/src/test/deno/
├── README.md              # Full documentation
├── QUICKSTART.md          # This file
├── deno.json              # Deno configuration
├── wasm_rasterizer.ts     # TypeScript API wrapper
├── basic_test.ts          # Basic functionality tests (11 tests)
├── advanced_test.ts       # Advanced scenarios (9 tests)
└── demo.ts                # Interactive demo
```

## Common Tasks

### Add a New Test
```typescript
Deno.test("My test name", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load("../../../build/wasm/awt_raster.wasm");
  
  // Your test logic
  
  assertEquals(actual, expected);
});
```

### Debug a Test
```bash
# Run with verbose output
deno test --allow-read --trace-ops

# Run a single test by filtering
deno test --allow-read --filter "Fill rect"
```

### Visualize Output
The demo includes ANSI color visualization for terminals that support 256-color mode:

```typescript
function printSurface(rasterizer: WasmRasterizer, surfaceId: number) {
  const dims = rasterizer.getSurfaceDimensions(surfaceId);
  const pixels = rasterizer.copySurfacePixels(surfaceId);
  
  for (let y = 0; y < dims.height; y++) {
    let row = "";
    for (let x = 0; x < dims.width; x++) {
      const pixel = pixels[y * dims.width + x];
      if (pixel !== 0) {
        // Display with actual color using ANSI escape sequences
        const { r, g, b } = WasmRasterizer.extractARGB(pixel);
        const ansiColor = rgbToAnsi256(r, g, b);
        row += `\x1b[48;5;${ansiColor}m  \x1b[0m`;
      } else {
        row += "··";
      }
    }
    console.log(row);
  }
}
```

Run `demo.ts` to see colored rectangles, patterns, and more rendered in your terminal!

## Available Commands

### Drawing Commands
- `CMD_SET_COLOR` - Set foreground/background color
- `CMD_FILL_RECT` - Fill rectangle
- `CMD_DRAW_RECT` - Draw rectangle outline
- `CMD_DRAW_LINE` - Draw line
- `CMD_CLEAR_RECT` - Clear rectangle (set to background color)

### State Commands
- `CMD_SET_CLIP_RECT` - Set clipping rectangle
- `CMD_SET_TRANSFORM` - Set 2D affine transform

### Image Commands
- `CMD_BLIT_IMAGE` - Blit image to surface

## Helper Functions

All available in `WasmRasterizer` class:

```typescript
// Color utilities
WasmRasterizer.makeARGB(a, r, g, b)
WasmRasterizer.extractARGB(pixel)

// Command helpers
WasmRasterizer.setColorCommand(argb, which?)
WasmRasterizer.fillRectCommand(x, y, w, h)
WasmRasterizer.drawRectCommand(x, y, w, h)
WasmRasterizer.drawLineCommand(x1, y1, x2, y2)
WasmRasterizer.clearRectCommand(x, y, w, h)
WasmRasterizer.setClipRectCommand(x, y, w, h)
WasmRasterizer.blitImageCommand(imageId, x, y)
WasmRasterizer.setTransformCommand(m00, m01, m02, m10, m11, m12)
```

## Troubleshooting

### "WASM module not found"
Build it first: `./gradlew :awtea-graphics:buildAwtRasterWasm`

### "Permission denied"
Add `--allow-read` flag to Deno commands

### Tests timeout
Increase Deno timeout: `deno test --allow-read --timeout=60000`

### Need to debug WASM
Check C source in `awtea-graphics/src/main/native/`

## CI Integration

Tests run automatically on:
- Push to main/develop branches
- Pull requests to main/develop
- Changes to native code or test files

See `.github/workflows/wasm-rasterizer-tests.yml`

## More Information

See [README.md](README.md) for complete documentation.
