#include "awt_stack.h"
#include "awt_log.h"
#include "awt_imports.h"
#include <stdio.h>

#if ENABLE_WASM_STACK_TRACKING

// Context string formatting buffer pool configuration
// Pool of small buffers for temporary context string formatting to avoid allocations
// Buffer size: 128 bytes - sufficient for typical context strings like "alloc 1024 bytes"
// Pool size: 8 buffers - allows 8 concurrent context strings in circular fashion
// Overflow policy: Buffers reused in circular order, caller must copy if persistence needed
#define CONTEXT_BUFFER_POOL_SIZE 8
#define CONTEXT_BUFFER_SIZE 128
static char g_context_buffers[CONTEXT_BUFFER_POOL_SIZE][CONTEXT_BUFFER_SIZE];
static int g_context_buffer_index = 0;

// Get a temporary buffer for context string formatting
// Note: Buffer is reused in circular fashion, so copy if you need to keep it
static char* get_context_buffer(void) {
    char* buf = g_context_buffers[g_context_buffer_index];
    g_context_buffer_index = (g_context_buffer_index + 1) % CONTEXT_BUFFER_POOL_SIZE;
    buf[0] = '\0';  // Clear buffer
    return buf;
}

// Global stack tracking state
static StackFrame g_stack[MAX_STACK_DEPTH];
static int g_stack_pointer = 0;  // Current stack depth (also index for next push)
static uint32_t g_stack_buffer_ptr = 0;  // Pointer to g_stack buffer

void init_stack_tracking(void) {
    // Clear the stack
    for (int i = 0; i < MAX_STACK_DEPTH; i++) {
        g_stack[i].function_name = NULL;
        g_stack[i].line_number = 0;
        g_stack[i].timestamp_ms = 0.0;
        g_stack[i].context = NULL;
        g_stack[i].error_code = 0;
        g_stack[i].surface_id = -1;
        g_stack[i].context_id = -1;
        g_stack[i].operation_type = 0;
        g_stack[i].command_index = 0;
        g_stack[i].ref_count = 0;
        g_stack[i].flags = 0;
    }
    g_stack_pointer = 0;
    
    // Clear context buffers
    for (int i = 0; i < CONTEXT_BUFFER_POOL_SIZE; i++) {
        g_context_buffers[i][0] = '\0';
    }
    g_context_buffer_index = 0;
    
    // Store pointer to stack buffer for external access
    g_stack_buffer_ptr = (uint32_t)(uintptr_t)&g_stack[0];
    
    log_info("Stack tracking initialized: %d max frames, buffer at 0x%08X", 
             MAX_STACK_DEPTH, g_stack_buffer_ptr);
}

void stack_push_extended(const char* function_name, int line, const char* context,
                        int32_t surface_id, int32_t context_id, 
                        uint16_t operation_type, uint16_t command_index, uint16_t ref_count) {
    if (g_stack_pointer < 0) {
        g_stack_pointer = 0;  // safety check
    }
    
    // Write to circular buffer (overwrites oldest entry when full)
    int index = g_stack_pointer % MAX_STACK_DEPTH;
    g_stack[index].function_name = function_name;
    g_stack[index].line_number = line;
    g_stack[index].timestamp_ms = wasm_get_time_ms();
    g_stack[index].context = context;
    g_stack[index].error_code = 0;
    g_stack[index].surface_id = surface_id;
    g_stack[index].context_id = context_id;
    g_stack[index].operation_type = operation_type;
    g_stack[index].command_index = command_index;
    g_stack[index].ref_count = ref_count;
    g_stack[index].flags = 0;
    
    g_stack_pointer++;
    
    // Prevent integer overflow on very long-running programs
    if (g_stack_pointer > MAX_STACK_DEPTH * 1000) {
        g_stack_pointer = MAX_STACK_DEPTH;
    }
}

void stack_push(const char* function_name, int line) {
    stack_push_extended(function_name, line, NULL, -1, -1, 0, 0, 0);
}

void stack_push_with_context(const char* function_name, int line, const char* context) {
    stack_push_extended(function_name, line, context, -1, -1, 0, 0, 0);
}

void stack_pop(void) {
    if (g_stack_pointer > 0) {
        g_stack_pointer--;
    }
}

void stack_set_error(int32_t error_code) {
    if (g_stack_pointer > 0) {
        int index = (g_stack_pointer - 1) % MAX_STACK_DEPTH;
        g_stack[index].error_code = error_code;
    }
}

uint32_t get_stack_buffer_ptr(void) {
    return g_stack_buffer_ptr;
}

int get_stack_depth(void) {
    // Return actual depth (capped at MAX_STACK_DEPTH)
    return (g_stack_pointer > MAX_STACK_DEPTH) ? MAX_STACK_DEPTH : g_stack_pointer;
}

int get_max_stack_depth(void) {
    return MAX_STACK_DEPTH;
}

// Helper function to format context string for memory allocations
// Uses snprintf for safe truncation - no overflow possible
const char* stack_format_alloc_context(size_t bytes) {
    char* buf = get_context_buffer();
    int written = snprintf(buf, CONTEXT_BUFFER_SIZE, "alloc %zu bytes", bytes);
    
    // Check for truncation (should be rare, but log if it happens)
    if (written < 0 || written >= CONTEXT_BUFFER_SIZE) {
        log_warn("stack_format_alloc_context: truncated output (needed %d bytes)", written);
        // Ensure null termination (snprintf guarantees this, but be explicit)
        buf[CONTEXT_BUFFER_SIZE - 1] = '\0';
    }
    
    return buf;
}

// Helper function to format context string for surface operations
// Uses snprintf for safe truncation - no overflow possible
const char* stack_format_surface_context(int id, int width, int height) {
    char* buf = get_context_buffer();
    int written = snprintf(buf, CONTEXT_BUFFER_SIZE, "surface %d (%dx%d)", id, width, height);
    
    // Check for truncation (should be rare, but log if it happens)
    if (written < 0 || written >= CONTEXT_BUFFER_SIZE) {
        log_warn("stack_format_surface_context: truncated output (needed %d bytes)", written);
        // Ensure null termination (snprintf guarantees this, but be explicit)
        buf[CONTEXT_BUFFER_SIZE - 1] = '\0';
    }
    
    return buf;
}

#endif  // ENABLE_WASM_STACK_TRACKING
