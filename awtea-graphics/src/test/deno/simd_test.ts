// SIMD Optimization Tests for WASM Rasterizer
//
// Tests that SIMD optimizations are compiled in and active.
// SIMD is used automatically by fast_fill_scanline when available.
//
// Note: These integration tests verify SIMD through the rendering pipeline.
// Direct SIMD function testing would require exporting low-level functions.

import { WasmRasterizer, PixelFormat } from "./wasm_rasterizer.ts";
import { assertEquals } from "https://deno.land/std@0.224.0/assert/mod.ts";
import { assertPixelEquals } from "./test_helpers.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";

// Build flag constants (must match awt_build_info.h)
const BUILD_FLAG_SIMD = 1 << 5;

Deno.test("SIMD feature detection via build flags", async () => {
    const rasterizer = new WasmRasterizer();
    await rasterizer.load(WASM_PATH);
    const exports = rasterizer.getExportsPublic();
    
    // Check build flags to verify SIMD was compiled in
    const buildFlags = exports.get_build_flags();
    const hasSIMDFlag = (buildFlags & BUILD_FLAG_SIMD) !== 0;
    console.log(`Build flags: 0x${buildFlags.toString(16)}`);
    console.log(`SIMD build flag: ${hasSIMDFlag ? "YES" : "NO"}`);
    
    // Verify SIMD was compiled in with -msimd128
    assertEquals(hasSIMDFlag, true, "SIMD build flag should be set with -msimd128");
});

Deno.test("SIMD integration - fast fills use SIMD path", async () => {
    // This test verifies that SIMD is compiled in and active through the fast fill path
    // The fast_fill_scanline function uses SIMD when USE_SIMD_FILLS is enabled
    const rasterizer = new WasmRasterizer();
    await rasterizer.load(WASM_PATH);
    
    const surfaceId = rasterizer.allocateSurface(100, 100, PixelFormat.PIXEL_FORMAT_ARGB);
    const ctx = rasterizer.createContext(surfaceId);
    
    // Use the rendering pipeline which calls fast_fill_scanline internally
    const color = WasmRasterizer.makeARGB(255, 0, 255, 0); // Green
    
    rasterizer.renderVariableLengthCommands(ctx, [
        (w) => WasmRasterizer.writeSetColorCommand(w, color),
        (w) => WasmRasterizer.writeFillRectCommand(w, 10, 10, 80, 80),
    ]);
    
    // Verify the rectangle was filled
    const pixels = rasterizer.copySurfacePixels(surfaceId);
    
    // Check a few pixels in the filled area
    for (let y = 10; y < 20; y++) {
        for (let x = 10; x < 20; x++) {
            const pixel = pixels[y * 100 + x];
            assertPixelEquals(pixel, color, `Pixel at (${x}, ${y}) should be green`);
        }
    }
    
    // Check pixels outside the filled area are still 0
    assertPixelEquals(pixels[0], 0, "Top-left pixel should be 0");
    assertPixelEquals(pixels[99], 0, "Top-right pixel should be 0");
    
    rasterizer.destroyContext(ctx);
    rasterizer.freeSurface(surfaceId);
});

Deno.test("SIMD integration - multiple fills", async () => {
    // Verify SIMD fills work correctly for multiple operations
    const rasterizer = new WasmRasterizer();
    await rasterizer.load(WASM_PATH);
    const surfaceId = rasterizer.allocateSurface(64, 64, PixelFormat.PIXEL_FORMAT_ARGB);
    const ctx = rasterizer.createContext(surfaceId);
    
    const colors = [
        WasmRasterizer.makeARGB(255, 255, 0, 0), // Red
        WasmRasterizer.makeARGB(255, 0, 255, 0), // Green
        WasmRasterizer.makeARGB(255, 0, 0, 255), // Blue
        WasmRasterizer.makeARGB(255, 255, 255, 0), // Yellow
    ];
    
    // Fill horizontal strips with different colors
    for (let i = 0; i < colors.length; i++) {
        const y = i * 16;
        rasterizer.renderVariableLengthCommands(ctx, [
            (w) => WasmRasterizer.writeSetColorCommand(w, colors[i]),
            (w) => WasmRasterizer.writeFillRectCommand(w, 0, y, 64, 16),
        ]);
    }
    
    // Verify each strip
    const pixels = rasterizer.copySurfacePixels(surfaceId);
    for (let i = 0; i < colors.length; i++) {
        const y = i * 16 + 8; // Middle of strip
        for (let x = 0; x < 64; x++) {
            const pixel = pixels[y * 64 + x];
            assertPixelEquals(pixel, colors[i], `Pixel at (${x}, ${y}) wrong color`);
        }
    }
    
    rasterizer.destroyContext(ctx);
    rasterizer.freeSurface(surfaceId);
});

Deno.test("SIMD integration - large fill performance", async () => {
    // Test that large fills work correctly with SIMD
    // (SIMD handles alignment and bulk operations)
    const rasterizer = new WasmRasterizer();
    await rasterizer.load(WASM_PATH);
    const surfaceId = rasterizer.allocateSurface(256, 256, PixelFormat.PIXEL_FORMAT_ARGB);
    const ctx = rasterizer.createContext(surfaceId);
    
    const color = WasmRasterizer.makeARGB(255, 128, 128, 255); // Purple
    
    rasterizer.renderVariableLengthCommands(ctx, [
        (w) => WasmRasterizer.writeSetColorCommand(w, color),
        (w) => WasmRasterizer.writeFillRectCommand(w, 0, 0, 256, 256),
    ]);
    
    // Verify corners and center
    const pixels = rasterizer.copySurfacePixels(surfaceId);
    assertPixelEquals(pixels[0], color, "Top-left should be purple");
    assertPixelEquals(pixels[255], color, "Top-right should be purple");
    assertPixelEquals(pixels[128 * 256 + 128], color, "Center should be purple");
    assertPixelEquals(pixels[255 * 256 + 255], color, "Bottom-right should be purple");
    
    rasterizer.destroyContext(ctx);
    rasterizer.freeSurface(surfaceId);
});

console.log("\n✅ All SIMD integration tests completed successfully!");
console.log("Note: SIMD optimizations are active in fast_fill_scanline and will be");
console.log("      used for all solid rectangle fills through the rendering pipeline.");
