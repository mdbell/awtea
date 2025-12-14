#pragma once

#include <stdint.h>

// Performance timing - returns high-resolution timestamp in milliseconds
extern double wasm_get_time_ms(void);

// Memory usage reporting - called by C code to report memory stats to host
extern void wasm_report_memory_usage(int allocated_bytes, int peak_bytes);

// Assertion failure handler - called when assertions fail
extern void wasm_assertion_failed(const char* expr, const char* file, int line);

// Assertion macro (can be disabled via ENABLE_WASM_ASSERTIONS=0)
#ifndef ENABLE_WASM_ASSERTIONS
#define ENABLE_WASM_ASSERTIONS 1
#endif

#if ENABLE_WASM_ASSERTIONS
#define WASM_ASSERT(expr) \
    do { \
        if (!(expr)) { \
            wasm_assertion_failed(#expr, __FILE__, __LINE__); \
        } \
    } while (0)
#else
#define WASM_ASSERT(expr) ((void)0)
#endif
