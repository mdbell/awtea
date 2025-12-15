#pragma once
#include <stdint.h>
#include <stddef.h>

// Stack tracking for debugging WASM crashes
// Provides a circular buffer of function calls that can be read from Java/JS
// even after the rasterizer crashes.

// Maximum stack depth (circular buffer size)
#define MAX_STACK_DEPTH 256

// Stack frame structure (24 bytes per frame)
typedef struct {
    const char* function_name;  // Pointer to static string (4 bytes)
    int line_number;            // Line where function was called (4 bytes)
    double timestamp_ms;        // Timestamp in milliseconds (8 bytes)
    const char* context;        // Optional context string pointer (4 bytes)
    uint32_t reserved;          // Reserved for future use (4 bytes)
} StackFrame;

// Control stack tracking at compile time
#ifndef ENABLE_WASM_STACK_TRACKING
#define ENABLE_WASM_STACK_TRACKING 1
#endif

#if ENABLE_WASM_STACK_TRACKING

// Initialize stack tracking system (called during init_surface_system)
void init_stack_tracking(void);

// Get pointer to stack buffer (exported to WASM)
__attribute__((export_name("get_stack_buffer_ptr")))
uint32_t get_stack_buffer_ptr(void);

// Get current stack depth (exported to WASM)
__attribute__((export_name("get_stack_depth")))
int get_stack_depth(void);

// Get max stack depth (exported to WASM)
__attribute__((export_name("get_max_stack_depth")))
int get_max_stack_depth(void);

// Helper functions for formatting context strings
const char* stack_format_alloc_context(size_t bytes);
const char* stack_format_surface_context(int id, int width, int height);

// Push/pop functions (internal use)
void stack_push(const char* function_name, int line);
void stack_push_with_context(const char* function_name, int line, const char* context);
void stack_pop(void);

// Convenience macros for tracking function entry/exit
#define STACK_ENTER() stack_push(__FUNCTION__, __LINE__)
#define STACK_ENTER_CTX(ctx) stack_push_with_context(__FUNCTION__, __LINE__, ctx)
#define STACK_EXIT() stack_pop()

// RAII-style helper structure for automatic cleanup
typedef struct {
    int dummy;  // unused, just to make the struct non-empty
} StackTracer;

// Helper functions for RAII pattern
static inline StackTracer stack_tracer_create(const char* function_name, int line) {
    stack_push(function_name, line);
    StackTracer tracer = {0};
    return tracer;
}

static inline StackTracer stack_tracer_create_with_context(const char* function_name, int line, const char* context) {
    stack_push_with_context(function_name, line, context);
    StackTracer tracer = {0};
    return tracer;
}

static inline void stack_tracer_destroy(StackTracer* tracer) {
    (void)tracer;  // unused
    stack_pop();
}

// RAII-style macro for automatic stack tracking
// Note: This requires C11 or later for cleanup attribute
#define STACK_TRACE() \
    StackTracer _stack_trace __attribute__((cleanup(stack_tracer_destroy))) = \
        stack_tracer_create(__FUNCTION__, __LINE__)

#define STACK_TRACE_CTX(ctx) \
    StackTracer _stack_trace __attribute__((cleanup(stack_tracer_destroy))) = \
        stack_tracer_create_with_context(__FUNCTION__, __LINE__, ctx)

#else

// Disabled versions (zero overhead)
#define init_stack_tracking() ((void)0)
#define STACK_ENTER() ((void)0)
#define STACK_ENTER_CTX(ctx) ((void)0)
#define STACK_EXIT() ((void)0)
#define STACK_TRACE() ((void)0)
#define STACK_TRACE_CTX(ctx) ((void)0)

// Stub implementations for exported functions
static inline uint32_t get_stack_buffer_ptr(void) { return 0; }
static inline int get_stack_depth(void) { return 0; }
static inline int get_max_stack_depth(void) { return 0; }

#endif
