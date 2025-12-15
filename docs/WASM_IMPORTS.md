# WASM Imports Documentation

This document describes the WASM import functions available for debugging, profiling, and diagnostics in the AWT rasterizer module.

## Overview

The AWT rasterizer WASM module uses host-provided imports for various runtime features. All WASM imports are declared in `awt_imports.h`:

1. **Logging** - Send log messages to the host environment
2. **Performance Timing** - High-resolution timestamps for profiling
3. **Memory Tracking** - Manual memory usage reporting
4. **Assertion Handling** - Better C-side debugging with assertion macros

All features are independent and can be used together or separately as needed.

## Logging

### Function: `wasm_log_callback()`

Sends log messages to the host environment for display or recording.

**C Declaration:**
```c
extern void wasm_log_callback(int level, const char* message_ptr, int message_len);
```

**Parameters:**
- `level` - Log level (0=ERROR, 1=WARN, 2=INFO, 3=DEBUG)
- `message_ptr` - Pointer to message string in WASM memory
- `message_len` - Length of message in bytes

**Usage:**
The logging import is typically used through the higher-level helper functions in `awt_log.h`:
```c
#include "awt_log.h"

log_info("Processing %d items", count);
log_error("Failed to allocate buffer");
```

See [WASM_LOGGING.md](WASM_LOGGING.md) for more details on the logging system.

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

In the test harness (`wasm_rasterizer.ts`), all imports are provided as:

```typescript
const imports = {
  env: {
    wasm_log_callback: (level: number, messagePtr: number, messageLen: number) => {
      const message = decodeString(memory, messagePtr, messageLen);
      console.log(`[WASM ${levelName}] ${message}`);
    },
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

All WASM imports are declared in `awt_imports.h` and follow a consistent pattern:

1. **C Side:** Declare extern functions in `awt_imports.h`
2. **C Side:** Call imported functions when needed (directly or through helper functions)
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

## Stack Tracking

The WASM rasterizer includes a call stack tracking system for debugging crashes and assertion failures.

### Overview

When enabled, the rasterizer maintains a circular buffer of function calls in WASM linear memory. This stack is readable from JavaScript/Java even after the module crashes, enabling post-mortem analysis.

### Exported Functions

The following functions are exported from the WASM module:

**`get_stack_buffer_ptr()`**
```c
uint32_t get_stack_buffer_ptr(void);
```
Returns a pointer to the stack buffer in WASM linear memory. Returns 0 if stack tracking is disabled.

**`get_stack_depth()`**
```c
int get_stack_depth(void);
```
Returns the current stack depth (number of frames). Capped at MAX_STACK_DEPTH (256).

**`get_max_stack_depth()`**
```c
int get_max_stack_depth(void);
```
Returns the maximum stack depth (circular buffer size), currently 256 frames.

### Stack Frame Structure

Each stack frame is 8 bytes:
```c
typedef struct {
    const char* function_name;  // 4 bytes: pointer to static string
    int line_number;            // 4 bytes: line number
} StackFrame;
```

Frames are stored contiguously in a circular buffer. When the buffer is full, new frames overwrite the oldest entries.

### C-Side Usage

Include the header:
```c
#include "awt_stack.h"
```

Track function entry/exit:
```c
void my_function(int arg) {
    STACK_ENTER();
    
    // Function body
    do_work(arg);
    
    STACK_EXIT();
}
```

For functions with multiple return paths:
```c
int validate_input(int* buffer, int size) {
    STACK_ENTER();
    
    if (buffer == NULL) {
        STACK_EXIT();
        return -1;
    }
    
    if (size <= 0) {
        STACK_EXIT();
        return -2;
    }
    
    // Success
    STACK_EXIT();
    return 0;
}
```

### Java-Side Reading

The `WasmSurfaceBackend` class automatically reads and logs the stack trace when:
- An assertion fails (via `wasm_assertion_failed` callback)
- The module aborts (via `abort` callback)

Example log output:
```
[ERROR] WASM assertion failed: buffer != NULL at awt_draw.c:45
[ERROR] WASM call stack:
Call stack (depth=5):
  #0: draw_filled_rect (line 23) [1234.567ms]
  #1: render_awt (line 67) [1234.123ms] 
  #2: blit_image (line 156) [1233.890ms]
  #3: create_context (line 178) [1233.456ms]
  #4: reset_surface (line 48) [1233.012ms] - surface 0 (100x100)
```

With context information included, you can see not only the call chain but also:
- Timestamps showing when each function was entered
- Additional context like surface dimensions or memory allocation sizes

### Compile-Time Configuration

Stack tracking can be disabled at compile time for production builds:

```bash
# Disable stack tracking (zero overhead)
emcc -DENABLE_WASM_STACK_TRACKING=0 ...
```

By default, `ENABLE_WASM_STACK_TRACKING` is set to `1` (enabled).

When disabled:
- `STACK_ENTER()` and `STACK_EXIT()` expand to `((void)0)` (no-op)
- Export functions return 0
- No memory overhead
- No runtime overhead

### Performance Impact

When enabled:
- **Memory**: ~6KB for 256 frames (24 bytes per frame)
- **Runtime**: ~3-4 instructions per function call/return including timestamp capture
- **Overhead**: ~1-2% for typical workloads

For performance-critical production builds, disable stack tracking after debugging is complete.

### Implementation Details

- **Circular buffer**: When full, overwrites oldest entries
- **Thread-safe**: Single-threaded WASM environment
- **Static strings**: Function names are pointers to static `__FUNCTION__` strings
- **Line numbers**: Captured at `STACK_ENTER()` via `__LINE__` macro
- **Timestamps**: Captured via `wasm_get_time_ms()` import (millisecond precision)
- **Context strings**: Stored in small buffer pool (8 buffers × 128 bytes)
- **No dynamic allocation**: Fixed-size buffers allocated at init time
- **Overflow protection**: Stack pointer capped to prevent integer overflow

### Best Practices

1. **Add tracking to key functions**:
   - Public API entry points
   - Complex algorithms
   - Functions that allocate memory
   - Functions that validate input

2. **Don't track trivial functions**:
   - Getters/setters
   - Simple arithmetic helpers
   - Functions called in tight loops (use sparingly)

3. **Balance between detail and overhead**:
   - Track more in debug builds
   - Track less in release builds
   - Consider disabling entirely for production

4. **Always match STACK_ENTER/STACK_EXIT**:
   - Add STACK_EXIT before every return
   - Use RAII-style `STACK_TRACE()` macro for automatic cleanup (C11+)

### Troubleshooting

**Stack trace shows empty or garbled function names:**
- Ensure you're using `STACK_ENTER()` at function entry
- Check that `__FUNCTION__` is supported by your compiler (it is in Emscripten)

**Stack depth is always 256:**
- The buffer wrapped around; oldest frames were overwritten
- Consider increasing MAX_STACK_DEPTH or reducing tracked functions

**No stack trace appears:**
- Verify ENABLE_WASM_STACK_TRACKING=1 in build configuration
- Check that `init_stack_tracking()` is called during initialization
- Ensure assertion/abort callbacks are properly configured

