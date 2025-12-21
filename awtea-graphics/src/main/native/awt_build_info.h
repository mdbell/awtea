#pragma once

/**
 * Build Information and Debug Flags
 * 
 * This header provides build metadata and debug configuration exports
 * for diagnostics and environment validation.
 */

#include <stdint.h>

// Master debug build flag - controls all debug features
#ifndef AWTEA_DEBUG_BUILD
#ifdef NDEBUG
#define AWTEA_DEBUG_BUILD 0
#else
#define AWTEA_DEBUG_BUILD 1
#endif
#endif

// Individual debug feature flags (controlled by master flag)
#ifndef ENABLE_WASM_STACK_TRACKING
#define ENABLE_WASM_STACK_TRACKING AWTEA_DEBUG_BUILD
#endif

#ifndef ENABLE_WASM_ASSERTIONS
#define ENABLE_WASM_ASSERTIONS AWTEA_DEBUG_BUILD
#endif

#ifndef ENABLE_WASM_LOGGING
#define ENABLE_WASM_LOGGING AWTEA_DEBUG_BUILD
#endif

#ifndef ENABLE_WASM_MEMORY_TRACKING
#define ENABLE_WASM_MEMORY_TRACKING AWTEA_DEBUG_BUILD
#endif

// Build info bit flags (32-bit packed)
#define BUILD_FLAG_DEBUG            (1 << 0)  // Debug build
#define BUILD_FLAG_STACK_TRACKING   (1 << 1)  // Stack tracking enabled
#define BUILD_FLAG_ASSERTIONS       (1 << 2)  // Assertions enabled
#define BUILD_FLAG_LOGGING          (1 << 3)  // Logging enabled
#define BUILD_FLAG_MEMORY_TRACKING  (1 << 4)  // Memory tracking enabled

// Build version and metadata exports
__attribute__((export_name("get_build_version_ptr")))
uint32_t get_build_version_ptr(void);

__attribute__((export_name("get_build_date_ptr")))
uint32_t get_build_date_ptr(void);

__attribute__((export_name("get_build_time_ptr")))
uint32_t get_build_time_ptr(void);

__attribute__((export_name("get_build_flags")))
uint32_t get_build_flags(void);

__attribute__((export_name("get_build_flags_string_ptr")))
uint32_t get_build_flags_string_ptr(void);

// Stack trace initialization info (safe to query after crash)
__attribute__((export_name("get_stack_info_ptr")))
uint32_t get_stack_info_ptr(void);

__attribute__((export_name("get_stack_info_count")))
int get_stack_info_count(void);
