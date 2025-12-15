#include "awt_stack.h"
#include "awt_log.h"

#if ENABLE_WASM_STACK_TRACKING

// Global stack tracking state
static StackFrame g_stack[MAX_STACK_DEPTH];
static int g_stack_pointer = 0;  // Current stack depth (also index for next push)
static uint32_t g_stack_buffer_ptr = 0;  // Pointer to g_stack buffer

void init_stack_tracking(void) {
    // Clear the stack
    for (int i = 0; i < MAX_STACK_DEPTH; i++) {
        g_stack[i].function_name = NULL;
        g_stack[i].line_number = 0;
    }
    g_stack_pointer = 0;
    
    // Store pointer to stack buffer for external access
    g_stack_buffer_ptr = (uint32_t)(uintptr_t)&g_stack[0];
    
    log_info("Stack tracking initialized: %d max frames, buffer at 0x%08X", 
             MAX_STACK_DEPTH, g_stack_buffer_ptr);
}

void stack_push(const char* function_name, int line) {
    if (g_stack_pointer < 0) {
        g_stack_pointer = 0;  // safety check
    }
    
    // Write to circular buffer (overwrites oldest entry when full)
    int index = g_stack_pointer % MAX_STACK_DEPTH;
    g_stack[index].function_name = function_name;
    g_stack[index].line_number = line;
    
    g_stack_pointer++;
    
    // Prevent integer overflow on very long-running programs
    if (g_stack_pointer > MAX_STACK_DEPTH * 1000) {
        g_stack_pointer = MAX_STACK_DEPTH;
    }
}

void stack_pop(void) {
    if (g_stack_pointer > 0) {
        g_stack_pointer--;
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

#endif  // ENABLE_WASM_STACK_TRACKING
