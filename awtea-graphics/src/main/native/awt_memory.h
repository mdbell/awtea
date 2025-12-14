#pragma once
#include "awt_imports.h"
#include <stdlib.h>
#include <stdint.h>

#ifndef ENABLE_WASM_MEMORY_TRACKING
#define ENABLE_WASM_MEMORY_TRACKING 1
#endif

#if ENABLE_WASM_MEMORY_TRACKING

__attribute__((weak)) size_t total_allocated_memory = 0;
__attribute__((weak)) size_t total_allocation_count = 0;
__attribute__((weak)) size_t peak_allocated_memory = 0;

typedef struct {
    size_t size;
} alloc_header_t;

static inline void* tracked_malloc(size_t size) {
    void* raw_ptr = malloc(sizeof(alloc_header_t) + size);
    if (!raw_ptr) {
        return NULL;
    }
    alloc_header_t* header = (alloc_header_t*)raw_ptr;
    header->size = size;
    total_allocated_memory += size;
    total_allocation_count++;
    if (total_allocated_memory > peak_allocated_memory) {
        peak_allocated_memory = total_allocated_memory;
    }
    wasm_report_memory_usage(total_allocated_memory, total_allocation_count, peak_allocated_memory);
    return (void*)((uint8_t*)raw_ptr + sizeof(alloc_header_t));
}

static inline void tracked_free(void* ptr) {
    if (ptr) {
        alloc_header_t* header = (alloc_header_t*)((uint8_t*)ptr - sizeof(alloc_header_t));
        size_t size = header->size;
        total_allocated_memory -= size;
        total_allocation_count--;
        wasm_report_memory_usage(total_allocated_memory, total_allocation_count, peak_allocated_memory);
        free(header);
    }
}

#else

static inline void* tracked_malloc(size_t size) {
    return malloc(size);
}

static inline void tracked_free(void* ptr) {
    free(ptr);
}

#endif
