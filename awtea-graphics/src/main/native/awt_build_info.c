#include "awt_build_info.h"
#include "awt_stack.h"
#include <stdio.h>
#include <string.h>

// Build version - can be overridden at compile time
#ifndef AWTEA_BUILD_VERSION
#define AWTEA_BUILD_VERSION "0.1.0-dev"
#endif

// Build date and time - set by compiler
#ifndef AWTEA_BUILD_DATE
#define AWTEA_BUILD_DATE __DATE__
#endif

#ifndef AWTEA_BUILD_TIME
#define AWTEA_BUILD_TIME __TIME__
#endif

// Static strings for build metadata
static const char BUILD_VERSION[] = AWTEA_BUILD_VERSION;
static const char BUILD_DATE[] = AWTEA_BUILD_DATE;
static const char BUILD_TIME[] = AWTEA_BUILD_TIME;

// Static buffer for flags description (allocated once, filled on first access)
static char BUILD_FLAGS_DESC[256] = {0};
static int flags_desc_initialized = 0;

/**
 * Compute build flags bit-packed integer
 */
uint32_t get_build_flags(void) {
    uint32_t flags = 0;
    
#if AWTEA_DEBUG_BUILD
    flags |= BUILD_FLAG_DEBUG;
#endif

#if ENABLE_WASM_STACK_TRACKING
    flags |= BUILD_FLAG_STACK_TRACKING;
#endif

#if ENABLE_WASM_ASSERTIONS
    flags |= BUILD_FLAG_ASSERTIONS;
#endif

#if ENABLE_WASM_LOGGING
    flags |= BUILD_FLAG_LOGGING;
#endif

#if ENABLE_WASM_MEMORY_TRACKING
    flags |= BUILD_FLAG_MEMORY_TRACKING;
#endif

    return flags;
}

/**
 * Helper macro to append a flag description if the flag is set
 * This avoids hardcoding flag names as strings
 */
#define APPEND_FLAG_IF_SET(flags, flag_constant, flag_name) \
    do { \
        if ((flags) & (flag_constant)) { \
            int written = snprintf(ptr, remaining, " +%s", #flag_name); \
            ptr += written; \
            remaining -= written; \
        } \
    } while(0)

/**
 * Generate human-readable description of build flags
 * Uses macro-based string generation to avoid hardcoding flag names
 */
static void init_build_flags_desc(void) {
    if (flags_desc_initialized) {
        return;
    }
    
    uint32_t flags = get_build_flags();
    char* ptr = BUILD_FLAGS_DESC;
    int remaining = sizeof(BUILD_FLAGS_DESC) - 1;
    
    // First part: DEBUG or RELEASE
    if (flags & BUILD_FLAG_DEBUG) {
        int written = snprintf(ptr, remaining, "DEBUG");
        ptr += written;
        remaining -= written;
    } else {
        int written = snprintf(ptr, remaining, "RELEASE");
        ptr += written;
        remaining -= written;
    }
    
    // Append all enabled flags using macro (flag name auto-extracted from constant name)
    APPEND_FLAG_IF_SET(flags, BUILD_FLAG_STACK_TRACKING, STACK);
    APPEND_FLAG_IF_SET(flags, BUILD_FLAG_ASSERTIONS, ASSERT);
    APPEND_FLAG_IF_SET(flags, BUILD_FLAG_LOGGING, LOG);
    APPEND_FLAG_IF_SET(flags, BUILD_FLAG_MEMORY_TRACKING, MEMTRACK);
    
    // If no flags set (minimal production build)
    if (flags == 0) {
        snprintf(ptr, remaining, " (minimal)");
    }
    
    flags_desc_initialized = 1;
}

/**
 * Get pointer to build version string
 */
uint32_t get_build_version_ptr(void) {
    return (uint32_t)(uintptr_t)BUILD_VERSION;
}

/**
 * Get pointer to build date string
 */
uint32_t get_build_date_ptr(void) {
    return (uint32_t)(uintptr_t)BUILD_DATE;
}

/**
 * Get pointer to build time string
 */
uint32_t get_build_time_ptr(void) {
    return (uint32_t)(uintptr_t)BUILD_TIME;
}

/**
 * Get pointer to build flags description string
 */
uint32_t get_build_flags_string_ptr(void) {
    init_build_flags_desc();
    return (uint32_t)(uintptr_t)BUILD_FLAGS_DESC;
}

/**
 * Get stack trace buffer pointer (safe to query after crash)
 * This provides initialization-time access to stack info
 */
uint32_t get_stack_info_ptr(void) {
#if ENABLE_WASM_STACK_TRACKING
    return get_stack_buffer_ptr();
#else
    return 0;
#endif
}

/**
 * Get stack trace buffer count (safe to query after crash)
 */
int get_stack_info_count(void) {
#if ENABLE_WASM_STACK_TRACKING
    return get_max_stack_depth();
#else
    return 0;
#endif
}
