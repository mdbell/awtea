/**
 * Demonstration of stack tracking on assertion failures
 *
 * This test deliberately triggers an assertion to show how the stack
 * tracking system works when the rasterizer encounters an error.
 */

import { WasmRasterizer } from "./wasm_rasterizer.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";

// This test is expected to fail - it's demonstrating error reporting
Deno.test({
  name: "Stack tracking - demonstration of assertion failure (expected to fail)",
  ignore: true, // Ignore by default since it's expected to fail
  fn: async () => {
    const rasterizer = new WasmRasterizer();
    await rasterizer.load(WASM_PATH);

    console.log("\n=== Demonstrating Stack Tracking on Error ===\n");

    // Try to use an invalid surface ID - this should trigger an assertion
    try {
      const invalidSurfaceId = 9999;
      console.log(
        `Attempting to create context with invalid surface ID: ${invalidSurfaceId}`,
      );
      rasterizer.createContext(invalidSurfaceId);
    } catch (e) {
      console.log("\nCaught error:", e);
      console.log("\nStack trace should have been printed above by WASM callback");
    }

    console.log("\n=============================================\n");
  },
});

// Manual demonstration - run this with: deno run --allow-read stack_demo.ts
if (import.meta.main) {
  console.log("=== WASM Stack Tracking Demonstration ===\n");

  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  console.log("1. Stack tracking initialized successfully");
  console.log(`   - Max stack depth: ${rasterizer.getMaxStackDepth()}`);
  console.log(
    `   - Stack buffer at: 0x${rasterizer.getStackBufferPtr().toString(16)}`,
  );
  console.log(`   - Current depth: ${rasterizer.getStackDepth()}\n`);

  console.log("2. Performing normal operations...");
  const surfaceId = rasterizer.allocateSurface(100, 100);
  console.log(`   - Created surface ${surfaceId}`);

  const contextId = rasterizer.createContext(surfaceId);
  console.log(`   - Created context ${contextId}`);
  console.log(`   - Stack depth: ${rasterizer.getStackDepth()}\n`);

  console.log("3. Cleaning up...");
  rasterizer.destroyContext(contextId);
  rasterizer.freeSurface(surfaceId);
  console.log(`   - Stack depth after cleanup: ${rasterizer.getStackDepth()}\n`);

  console.log("4. Reading stack trace:");
  const trace = rasterizer.readStackTrace();
  if (trace === "") {
    console.log("   - (empty - all functions have returned)\n");
  } else {
    console.log(trace);
  }

  console.log("✅ Stack tracking demonstration complete!");
  console.log(
    "\nThe stack tracking system successfully tracks function calls",
  );
  console.log("and will be available when crashes or assertions occur.\n");
}
