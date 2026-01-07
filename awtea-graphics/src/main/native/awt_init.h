#pragma once

// WASM Module Initialization
//
// This file contains the automatic initialization logic for the WASM rasterizer module.
// All initialization is handled via C constructors with explicit priority ordering
// to ensure proper dependency resolution during module loading.
//
// Initialization order:
//   Priority 101: Stack tracking (used for debugging/logging)
//   Priority 102: Alpha blend lookup table (64KB precomputed blending)
//   Priority 103: Surface system (context arrays and state)
//
// These constructors are called automatically when __wasm_call_ctors() is invoked
// after WebAssembly module instantiation.
