/**
 * Tests for build info and debug exports
 * 
 * Verifies that the new build metadata exports work correctly:
 * - Build version
 * - Build date/time
 * - Debug flags (bit-packed and string)
 * - Stack info initialization
 */

import { WasmRasterizer } from "./wasm_rasterizer.ts";
import { decodeNullTerminatedString } from "./test_helpers.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";

Deno.test("Build version export exists and returns valid pointer", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const exports = rasterizer.getExportsPublic();
  const versionPtr = exports.get_build_version_ptr();

  // Pointer should be non-zero
  if (versionPtr === 0) {
    throw new Error("Build version pointer is null");
  }

  // Decode the string
  const version = decodeNullTerminatedString(
    rasterizer.getMemory(),
    versionPtr,
  );
  console.log(`Build version: ${version}`);

  // Version should be non-empty
  if (version.length === 0) {
    throw new Error("Build version string is empty");
  }

  // Should contain expected format (e.g., "0.1.0-dev")
  if (!version.match(/^\d+\.\d+\.\d+/)) {
    throw new Error(`Invalid version format: ${version}`);
  }
});

Deno.test("Build date export exists and returns valid pointer", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const exports = rasterizer.getExportsPublic();
  const datePtr = exports.get_build_date_ptr();

  // Pointer should be non-zero
  if (datePtr === 0) {
    throw new Error("Build date pointer is null");
  }

  // Decode the string
  const date = decodeNullTerminatedString(rasterizer.getMemory(), datePtr);
  console.log(`Build date: ${date}`);

  // Date should be non-empty
  if (date.length === 0) {
    throw new Error("Build date string is empty");
  }

  // Should match __DATE__ format: "Jan 15 2024"
  if (!date.match(/^[A-Z][a-z]{2}\s+\d+\s+\d{4}$/)) {
    throw new Error(`Invalid date format: ${date}`);
  }
});

Deno.test("Build time export exists and returns valid pointer", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const exports = rasterizer.getExportsPublic();
  const timePtr = exports.get_build_time_ptr();

  // Pointer should be non-zero
  if (timePtr === 0) {
    throw new Error("Build time pointer is null");
  }

  // Decode the string
  const time = decodeNullTerminatedString(rasterizer.getMemory(), timePtr);
  console.log(`Build time: ${time}`);

  // Time should be non-empty
  if (time.length === 0) {
    throw new Error("Build time string is empty");
  }

  // Should match __TIME__ format: "12:34:56"
  if (!time.match(/^\d{2}:\d{2}:\d{2}$/)) {
    throw new Error(`Invalid time format: ${time}`);
  }
});

Deno.test("Build flags export returns debug build flags", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const exports = rasterizer.getExportsPublic();
  const flags = exports.get_build_flags();

  console.log(`Build flags (raw): 0x${flags.toString(16).padStart(8, "0")}`);

  // Define flag constants (must match awt_build_info.h)
  const BUILD_FLAG_DEBUG = 1 << 0;
  const BUILD_FLAG_STACK_TRACKING = 1 << 1;
  const BUILD_FLAG_ASSERTIONS = 1 << 2;
  const BUILD_FLAG_LOGGING = 1 << 3;
  const BUILD_FLAG_MEMORY_TRACKING = 1 << 4;

  // Debug build should have DEBUG flag set
  if ((flags & BUILD_FLAG_DEBUG) === 0) {
    throw new Error("Expected DEBUG flag to be set in debug build");
  }

  // Debug build should have stack tracking enabled
  if ((flags & BUILD_FLAG_STACK_TRACKING) === 0) {
    throw new Error("Expected STACK_TRACKING flag to be set in debug build");
  }

  // Debug build should have assertions enabled
  if ((flags & BUILD_FLAG_ASSERTIONS) === 0) {
    throw new Error("Expected ASSERTIONS flag to be set in debug build");
  }

  // Debug build should have logging enabled
  if ((flags & BUILD_FLAG_LOGGING) === 0) {
    throw new Error("Expected LOGGING flag to be set in debug build");
  }

  // Debug build should have memory tracking enabled
  if ((flags & BUILD_FLAG_MEMORY_TRACKING) === 0) {
    throw new Error("Expected MEMORY_TRACKING flag to be set in debug build");
  }

  console.log("All expected debug flags are set ✓");
});

Deno.test("Build flags string export returns human-readable description", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const exports = rasterizer.getExportsPublic();
  const flagsStrPtr = exports.get_build_flags_string_ptr();

  // Pointer should be non-zero
  if (flagsStrPtr === 0) {
    throw new Error("Build flags string pointer is null");
  }

  // Decode the string
  const flagsStr = decodeNullTerminatedString(
    rasterizer.getMemory(),
    flagsStrPtr,
  );
  console.log(`Build flags description: ${flagsStr}`);

  // String should be non-empty
  if (flagsStr.length === 0) {
    throw new Error("Build flags string is empty");
  }

  // Debug build should contain "DEBUG"
  if (!flagsStr.includes("DEBUG")) {
    throw new Error(`Expected "DEBUG" in flags string, got: ${flagsStr}`);
  }

  // Should contain stack tracking indicator
  if (!flagsStr.includes("STACK")) {
    throw new Error(`Expected "STACK" in flags string, got: ${flagsStr}`);
  }
});

Deno.test("Stack info pointer export works (initialization-safe)", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const exports = rasterizer.getExportsPublic();

  // Now query stack info (should already be initialized by load())
  const stackInfoPtr = exports.get_stack_info_ptr();
  const stackInfoCount = exports.get_stack_info_count();

  console.log(`Stack info pointer: 0x${stackInfoPtr.toString(16)}`);
  console.log(`Stack info count: ${stackInfoCount}`);

  // In debug build, stack tracking should be enabled
  if (stackInfoPtr === 0) {
    throw new Error("Stack info pointer is null in debug build");
  }

  if (stackInfoCount <= 0) {
    throw new Error(`Stack info count should be positive, got: ${stackInfoCount}`);
  }

  // Verify count matches max_stack_depth export
  const maxDepth = exports.get_max_stack_depth();
  if (stackInfoCount !== maxDepth) {
    throw new Error(`Stack info count (${stackInfoCount}) should match max depth (${maxDepth})`);
  }

  console.log("Stack info exports are working correctly ✓");
});

Deno.test("All build info exports are consistent", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const exports = rasterizer.getExportsPublic();

  // Get all the metadata
  const versionPtr = exports.get_build_version_ptr();
  const datePtr = exports.get_build_date_ptr();
  const timePtr = exports.get_build_time_ptr();
  const flags = exports.get_build_flags();
  const flagsStrPtr = exports.get_build_flags_string_ptr();

  const version = decodeNullTerminatedString(
    rasterizer.getMemory(),
    versionPtr,
  );
  const date = decodeNullTerminatedString(rasterizer.getMemory(), datePtr);
  const time = decodeNullTerminatedString(rasterizer.getMemory(), timePtr);
  const flagsStr = decodeNullTerminatedString(
    rasterizer.getMemory(),
    flagsStrPtr,
  );

  console.log("\n=== Build Information ===");
  console.log(`Version: ${version}`);
  console.log(`Built on: ${date} at ${time}`);
  console.log(`Flags: 0x${flags.toString(16).padStart(8, "0")}`);
  console.log(`Description: ${flagsStr}`);
  console.log("========================\n");

  // All should be valid
  if (!version || !date || !time || !flagsStr) {
    throw new Error("One or more build info fields are invalid");
  }
});
