/**
 * Test for RELEASE build mode
 * 
 * NOTE: These tests are designed to verify release builds.
 * They will be skipped if running against a debug build.
 * 
 * To test release mode, rebuild with:
 * ./gradlew :awtea-graphics:buildAwtRasterWasm -PwasmBuildMode=release --rerun-tasks
 * 
 * This test verifies that in release mode, all debug flags are disabled.
 */

import { WasmRasterizer } from "./wasm_rasterizer.ts";
import { decodeNullTerminatedString } from "./test_helpers.ts";

const WASM_PATH = "../../../build/wasm/awt_raster.wasm";

Deno.test("Release build has all debug flags disabled", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const exports = rasterizer.getExportsPublic();
  const flags = exports.get_build_flags();

  // Define flag constants (must match awt_build_info.h)
  const BUILD_FLAG_DEBUG = 1 << 0;

  // Skip if this is a debug build
  if ((flags & BUILD_FLAG_DEBUG) !== 0) {
    console.log("Skipping release build test - running against DEBUG build");
    return; // Skip test
  }

  console.log(`Build flags (raw): 0x${flags.toString(16).padStart(8, "0")}`);

  const BUILD_FLAG_STACK_TRACKING = 1 << 1;
  const BUILD_FLAG_ASSERTIONS = 1 << 2;
  const BUILD_FLAG_LOGGING = 1 << 3;
  const BUILD_FLAG_MEMORY_TRACKING = 1 << 4;

  // Release build should NOT have stack tracking enabled
  if ((flags & BUILD_FLAG_STACK_TRACKING) !== 0) {
    throw new Error("Expected STACK_TRACKING flag to be CLEAR in release build");
  }

  // Release build should NOT have assertions enabled
  if ((flags & BUILD_FLAG_ASSERTIONS) !== 0) {
    throw new Error("Expected ASSERTIONS flag to be CLEAR in release build");
  }

  // Release build should NOT have logging enabled
  if ((flags & BUILD_FLAG_LOGGING) !== 0) {
    throw new Error("Expected LOGGING flag to be CLEAR in release build");
  }

  // Release build should NOT have memory tracking enabled
  if ((flags & BUILD_FLAG_MEMORY_TRACKING) !== 0) {
    throw new Error("Expected MEMORY_TRACKING flag to be CLEAR in release build");
  }

  console.log("All debug flags are correctly disabled in release build ✓");
});

Deno.test("Release build flags string shows RELEASE", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const exports = rasterizer.getExportsPublic();
  const flags = exports.get_build_flags();
  const BUILD_FLAG_DEBUG = 1 << 0;

  // Skip if this is a debug build
  if ((flags & BUILD_FLAG_DEBUG) !== 0) {
    console.log("Skipping release build test - running against DEBUG build");
    return;
  }

  const flagsStrPtr = exports.get_build_flags_string_ptr();

  if (flagsStrPtr === 0) {
    throw new Error("Build flags string pointer is null");
  }

  const flagsStr = decodeNullTerminatedString(
    rasterizer.getMemory(),
    flagsStrPtr,
  );
  console.log(`Build flags description: ${flagsStr}`);

  if (!flagsStr.includes("RELEASE")) {
    throw new Error(`Expected "RELEASE" in flags string, got: ${flagsStr}`);
  }

  // Should NOT contain debug indicators
  if (flagsStr.includes("DEBUG")) {
    throw new Error(`Should not contain "DEBUG" in release build: ${flagsStr}`);
  }
});

Deno.test("Release build version has no -dev suffix", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const exports = rasterizer.getExportsPublic();
  const flags = exports.get_build_flags();
  const BUILD_FLAG_DEBUG = 1 << 0;

  // Skip if this is a debug build
  if ((flags & BUILD_FLAG_DEBUG) !== 0) {
    console.log("Skipping release build test - running against DEBUG build");
    return;
  }

  const versionPtr = exports.get_build_version_ptr();
  const version = decodeNullTerminatedString(
    rasterizer.getMemory(),
    versionPtr,
  );

  console.log(`Release build version: ${version}`);

  // Release version should not have -dev suffix
  if (version.includes("-dev")) {
    throw new Error(`Release build should not have -dev suffix: ${version}`);
  }
});

Deno.test("Release build stack info is disabled", async () => {
  const rasterizer = new WasmRasterizer();
  await rasterizer.load(WASM_PATH);

  const exports = rasterizer.getExportsPublic();
  const flags = exports.get_build_flags();
  const BUILD_FLAG_DEBUG = 1 << 0;

  // Skip if this is a debug build
  if ((flags & BUILD_FLAG_DEBUG) !== 0) {
    console.log("Skipping release build test - running against DEBUG build");
    return;
  }

  const stackInfoPtr = exports.get_stack_info_ptr();
  const stackInfoCount = exports.get_stack_info_count();

  console.log(`Stack info pointer: 0x${stackInfoPtr.toString(16)}`);
  console.log(`Stack info count: ${stackInfoCount}`);

  // In release build, stack tracking should be disabled
  if (stackInfoPtr !== 0) {
    throw new Error("Stack info pointer should be null in release build");
  }

  if (stackInfoCount !== 0) {
    throw new Error(`Stack info count should be 0 in release build, got: ${stackInfoCount}`);
  }

  console.log("Stack tracking is correctly disabled in release build ✓");
});
