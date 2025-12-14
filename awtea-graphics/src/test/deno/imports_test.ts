/**
 * Tests for new WASM imports (timing, memory tracking, assertions)
 * 
 * These tests verify that the WASM module can be loaded with all required
 * callbacks and that the module initialization succeeds.
 */

import { WasmRasterizer } from "./wasm_rasterizer.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";

Deno.test("Performance timing callback is accepted", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);
  
  // The timing callback should be invoked by any timed operation
  // We can't easily test this directly, but we can verify the module loads
  // with the import satisfied
  
  // If we get here without throwing, the module loaded successfully
  // and accepted the wasm_get_time_ms callback
});

Deno.test("Memory tracking callback is accepted", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);
  
  // Similar verification that the callback is provided
  // The module should load successfully with wasm_report_memory_usage callback
});

Deno.test("Assertion callback is accepted", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);
  
  // Verify the module can call the assertion callback
  // The module should load successfully with wasm_assertion_failed callback
});

Deno.test("Module loads with all new callbacks", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);
  
  // Perform a simple operation to ensure the module works correctly
  const surfaceId = rasterizer.allocateSurface(10, 10);
  const dims = rasterizer.getSurfaceDimensions(surfaceId);
  
  // Basic sanity check
  if (dims.width !== 10 || dims.height !== 10) {
    throw new Error("Surface dimensions incorrect");
  }
  
  rasterizer.freeSurface(surfaceId);
});
