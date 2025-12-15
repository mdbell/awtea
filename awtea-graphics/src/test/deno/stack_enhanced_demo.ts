/**
 * Demonstration of enhanced stack tracking with timestamps and context
 * 
 * This example shows how the stack tracking system captures:
 * - Function names and line numbers
 * - Timestamps for each frame
 * - Optional context information (e.g., surface dimensions, memory allocations)
 */

import { WasmRasterizer } from "./wasm_rasterizer.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";

// Manual demonstration - run this with: deno run --allow-read stack_enhanced_demo.ts
if (import.meta.main) {
  console.log("=== Enhanced WASM Stack Tracking Demonstration ===\n");
  console.log("New Features:");
  console.log("  ✓ Timestamps (millisecond precision)");
  console.log("  ✓ Context information (e.g., surface dimensions)\n");

  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  console.log("1. Create a surface with specific dimensions...");
  const surfaceId = rasterizer.allocateSurface(256, 128);
  console.log(`   - Created surface ${surfaceId} (256x128)\n`);

  console.log("2. Create another surface with different dimensions...");
  const surfaceId2 = rasterizer.allocateSurface(512, 256);
  console.log(`   - Created surface ${surfaceId2} (512x256)\n`);

  console.log("3. When these operations are tracked, the stack shows:");
  console.log("   - Function name: reset_surface");
  console.log("   - Line number: where STACK_ENTER_CTX was called");
  console.log("   - Timestamp: when the function was entered");
  console.log("   - Context: 'surface 0 (256x128)' or 'surface 1 (512x256)'\n");

  console.log("4. Example stack trace format:");
  console.log("   Call stack (depth=3):");
  console.log("     #0: draw_filled_rect (line 23) [1234.567ms]");
  console.log("     #1: render_awt (line 67) [1234.123ms]");
  console.log(
    "     #2: reset_surface (line 48) [1233.012ms] - surface 0 (256x128)",
  );
  console.log("");

  console.log("5. Benefits:");
  console.log("   - Timestamps help identify performance bottlenecks");
  console.log("   - Context shows which specific surface/allocation failed");
  console.log("   - All information available even after crash\n");

  // Clean up
  rasterizer.freeSurface(surfaceId);
  rasterizer.freeSurface(surfaceId2);

  console.log("✅ Enhanced stack tracking demonstration complete!");
  console.log(
    "\nThe system now captures timing and context for better debugging.\n",
  );
}
