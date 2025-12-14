# WASM Imports Documentation

This document describes the WASM import functions available for debugging, profiling, and diagnostics in the AWT rasterizer module.

## Overview

The AWT rasterizer WASM module provides three categories of debugging/profiling imports in addition to the existing logging functionality:

1. **Performance Timing** - High-resolution timestamps for profiling
2. **Memory Tracking** - Manual memory usage reporting
3. **Assertion Handling** - Better C-side debugging with assertion macros

All three features are independent and can be used together or separately as needed.

## Performance Timing

### Function: `wasm_get_time_ms()`

Returns a high-resolution timestamp in milliseconds using JavaScript's `performance.now()`.

**C Declaration:**
```c
extern double wasm_get_time_ms(void);
```

**Usage Example:**
```c
#include "awt_imports.h"
#include "awt_log.h"

void profile_rendering_operation(void) {
    double start = wasm_get_time_ms();
    
    // Perform rendering operations
    render_complex_scene();
    
    double elapsed = wasm_get_time_ms() - start;
    log_info("Rendering took %.2f ms", elapsed);
}
```

**Benefits:**
- Sub-millisecond precision for accurate profiling
- No platform-specific timing code needed
- Works consistently across all WASM environments

## Memory Tracking

### Function: `wasm_report_memory_usage()`

Reports memory usage statistics to the host environment for debugging and leak detection.

**C Declaration:**
```c
extern void wasm_report_memory_usage(int allocated_bytes, int peak_bytes);
```

**Parameters:**
- `allocated_bytes` - Current total allocated memory in bytes
- `peak_bytes` - Peak memory usage in bytes

**Usage Example:**
```c
#include "awt_imports.h"
#include <stdlib.h>

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
```

**Benefits:**
- Track memory usage patterns
- Detect memory leaks early
- Monitor peak memory usage for optimization
- Manual invocation provides fine-grained control

**Note:** This is a manual reporting mechanism. The caller is responsible for tracking allocations and invoking the function at appropriate times.

## Assertion Handling

### Function: `wasm_assertion_failed()`

Called when an assertion fails, providing detailed error information to the host.

**C Declaration:**
```c
extern void wasm_assertion_failed(const char* expr, const char* file, int line);
```

**Parameters:**
- `expr` - The assertion expression that failed (as string)
- `file` - Source file where assertion failed
- `line` - Line number where assertion failed

### Macro: `WASM_ASSERT()`

Convenience macro for checking assertions.

**Usage Example:**
```c
#include "awt_imports.h"

void process_buffer(int* buffer, int size) {
    WASM_ASSERT(buffer != NULL);
    WASM_ASSERT(size > 0);
    WASM_ASSERT(size <= MAX_BUFFER_SIZE);
    
    for (int i = 0; i < size; i++) {
        WASM_ASSERT(i >= 0 && i < size);
        buffer[i] = process_element(buffer[i]);
    }
}
```

**Benefits:**
- Better error messages than generic crashes
- File and line number information
- Expression text included in error
- Can be disabled at compile time

### Compile-Time Configuration

Assertions can be disabled during compilation for production builds:

```bash
# Build with assertions disabled
clang -DENABLE_WASM_ASSERTIONS=0 ...
```

By default, `ENABLE_WASM_ASSERTIONS` is set to `1` (enabled).

When disabled, `WASM_ASSERT(expr)` expands to `((void)0)`, which has no runtime overhead.

## Host Implementation

### Java/TeaVM

In `WasmSurfaceBackend.java`, the callbacks are implemented as:

```java
private double getTimeMs() {
    return JSPerformance.now();
}

private void reportMemoryUsage(int allocatedBytes, int peakBytes) {
    memoryLogger.debug("WASM memory: allocated={} bytes, peak={} bytes", 
        allocatedBytes, peakBytes);
}

private void handleAssertionFailure(int exprPtr, int exprLen, int filePtr, int fileLen, int line) {
    // Decode strings from WASM memory
    String expr = decodeString(exprPtr, exprLen);
    String file = decodeString(filePtr, fileLen);
    
    wasmLogger.error("WASM assertion failed: {} at {}:{}", expr, file, line);
}
```

### TypeScript/Deno

In the test harness (`wasm_rasterizer.ts`), the imports are provided as:

```typescript
const imports = {
  env: {
    wasm_get_time_ms: (): number => {
      return performance.now();
    },
    wasm_report_memory_usage: (allocatedBytes: number, peakBytes: number) => {
      console.log(`[WASM MEMORY] allocated=${allocatedBytes} bytes, peak=${peakBytes} bytes`);
    },
    wasm_assertion_failed: (exprPtr: number, exprLen: number, filePtr: number, fileLen: number, line: number) => {
      const expr = decodeString(memory, exprPtr, exprLen);
      const file = decodeString(memory, filePtr, fileLen);
      console.error(`[WASM ASSERTION] ${expr} failed at ${file}:${line}`);
    }
  }
};
```

## Integration Pattern

These imports follow the same pattern as the existing `wasm_log_callback`:

1. **C Side:** Declare extern functions in header
2. **C Side:** Call imported functions when needed
3. **Host Side:** Provide implementations via WASM imports object
4. **Host Side:** Handle callbacks with appropriate logging/actions

This design keeps the C code simple while providing full control on the host side.

## Logging Categories

The Java implementation uses separate loggers for different aspects:

- `wasm.rasterizer` - General WASM and assertion logs
- `wasm.memory` - Memory tracking logs

Configure logging levels in your application to control verbosity.

## Example: Complete Integration

Here's a complete example showing all three features together:

```c
#include "awt_imports.h"
#include "awt_log.h"
#include <stdlib.h>

// Memory tracking state
static int allocated = 0;
static int peak = 0;

void* my_malloc(size_t size) {
    void* ptr = malloc(size);
    if (ptr) {
        allocated += size;
        if (allocated > peak) peak = allocated;
        wasm_report_memory_usage(allocated, peak);
    }
    return ptr;
}

void my_free(void* ptr, size_t size) {
    if (ptr) {
        free(ptr);
        allocated -= size;
        wasm_report_memory_usage(allocated, peak);
    }
}

// Profiled and checked operation
void complex_operation(int* data, int count) {
    double start = wasm_get_time_ms();
    
    WASM_ASSERT(data != NULL);
    WASM_ASSERT(count > 0);
    
    int* temp = my_malloc(count * sizeof(int));
    WASM_ASSERT(temp != NULL);
    
    // Do work...
    for (int i = 0; i < count; i++) {
        WASM_ASSERT(i < count);
        temp[i] = data[i] * 2;
    }
    
    my_free(temp, count * sizeof(int));
    
    double elapsed = wasm_get_time_ms() - start;
    log_info("Operation took %.2f ms", elapsed);
}
```

## Best Practices

1. **Performance Timing:**
   - Use for coarse-grained profiling (operations > 1ms)
   - Don't call in tight loops (overhead adds up)
   - Log results rather than returning to caller

2. **Memory Tracking:**
   - Implement wrapper functions for malloc/free
   - Update counts immediately after allocation/deallocation
   - Consider using only in debug builds if overhead is concern

3. **Assertions:**
   - Use liberally in debug builds
   - Disable in production builds (-DENABLE_WASM_ASSERTIONS=0)
   - Check preconditions at function entry
   - Check invariants in loops

## See Also

- [WASM_LOGGING.md](WASM_LOGGING.md) - General WASM logging documentation
- [LOGGING.md](LOGGING.md) - Application-wide logging configuration
- `awt_imports.h` - C header with declarations
- `example_usage.c` - Complete usage examples
