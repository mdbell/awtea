#include "awt_imports.h"
#include "awt_log.h"
#include <stdlib.h>

// Example: Measure performance of an operation
void example_timed_operation(void) {
    double start = wasm_get_time_ms();
    
    // Do some work
    for (int i = 0; i < 1000000; i++) {
        // simulate work
    }
    
    double elapsed = wasm_get_time_ms() - start;
    log_info("Operation took %.2f ms", elapsed);
}

// Example: Report memory usage
static int total_allocated = 0;
static int peak_allocated = 0;

void* tracked_malloc(size_t size) {
    void* ptr = malloc(size);
    if (ptr) {
        total_allocated += size;
        if (total_allocated > peak_allocated) {
            peak_allocated = total_allocated;
        }
        wasm_report_memory_usage(total_allocated, peak_allocated);
    }
    return ptr;
}

void tracked_free(void* ptr, size_t size) {
    if (ptr) {
        free(ptr);
        total_allocated -= size;
        wasm_report_memory_usage(total_allocated, peak_allocated);
    }
}

// Example: Use assertions
void example_with_assertions(int* buffer, int size) {
    WASM_ASSERT(buffer != NULL);
    WASM_ASSERT(size > 0);
    
    for (int i = 0; i < size; i++) {
        WASM_ASSERT(i >= 0 && i < size);
        buffer[i] = i;
    }
}
