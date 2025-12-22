#pragma once
#include "awt_build_info.h"
#include <stdlib.h>
#include <stdint.h>
#include <string.h>

// Logging - called by C code to send log messages to host
extern void wasm_log_callback(int level, const char* message_ptr, int message_len);

// Performance timing - returns high-resolution timestamp in milliseconds
extern double wasm_get_time_ms(void);

// Memory usage reporting - called by C code to report memory stats to host
extern void wasm_report_memory_usage(size_t allocated_bytes, size_t alloc_count, size_t peak_bytes);

// Assertion failure handler - called when assertions fail
// Uses pointer+length for both expression and file for consistency with other imports
extern void wasm_assertion_failed(const char* expr_ptr, int expr_len, 
                                   const char* file_ptr, int file_len, 
                                   int line);


#if ENABLE_WASM_ASSERTIONS
#define WASM_ASSERT(expr) \
    do { \
        if (!(expr)) { \
            const char* _expr_str = #expr; \
            const char* _file_str = __FILE__; \
            wasm_assertion_failed(_expr_str, strlen(_expr_str), \
                                 _file_str, strlen(_file_str), \
                                 __LINE__); \
        } \
    } while (0)
#else
#define WASM_ASSERT(expr) ((void)0)
#endif
