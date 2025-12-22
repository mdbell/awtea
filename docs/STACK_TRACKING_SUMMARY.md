# WASM Stack Tracking System - Implementation Summary

## Overview
Added a complete call stack tracking system for the WASM rasterizer to enable debugging when crashes or assertion failures occur. The stack is readable externally from JavaScript/Java even after the module crashes, enabling post-mortem analysis.

**Enhanced Features:**
- **Timestamps**: Each frame captures the timestamp when the function was entered (in milliseconds)
- **Context strings**: Optional context information (e.g., memory allocations, surface dimensions)

## Key Features Implemented

### 1. Core C Implementation
- **`awt_stack.h`**: API header with stack frame structure and macros
- **`awt_stack.c`**: Circular buffer implementation (256 frames, ~6KB total)
- **Stack Frame**: 24 bytes per frame (function name pointer + line number + timestamp + context pointer + reserved)
- **Macros**: `STACK_ENTER()`, `STACK_ENTER_CTX(ctx)`, `STACK_EXIT()` for easy integration
- **Helper functions**: `stack_format_alloc_context()`, `stack_format_surface_context()` for context formatting

### 2. Integration Points
Added stack tracking to critical functions:
- `init_surface_system()` - System initialization
- `reset_surface()` - Surface creation/destruction
- `create_context()` - Context management
- `render_awt()` - Command rendering
- `draw_filled_rect()` - Drawing primitives
- `blit_image()` - Image blitting

### 3. WASM Exports
Three new exports added:
- `get_stack_buffer_ptr()` - Returns pointer to stack buffer in WASM memory
- `get_stack_depth()` - Returns current stack depth (0-256)
- `get_max_stack_depth()` - Returns max depth (256)

### 4. Java Integration
Updated `WasmSurfaceBackend.java`:
- `readWasmStackTrace()` - Reads stack frames from WASM memory
- `readNullTerminatedString()` - Helper for reading function names
- Integrated into `handleAbort()` and `handleAssertionFailure()`

### 5. Build Configuration
- Added `-DENABLE_WASM_STACK_TRACKING=1` to emcc compilation flags
- Can be disabled for production: `-DENABLE_WASM_STACK_TRACKING=0`
- Zero overhead when disabled (macros expand to no-ops)

### 6. Documentation
Updated `docs/WASM_IMPORTS.md` with comprehensive documentation:
- API reference
- Usage examples
- Performance impact
- Best practices
- Troubleshooting guide

### 7. Testing
Created comprehensive Deno tests (`stack_test.ts`):
- ✅ Verify exports are available
- ✅ Test stack depth tracking
- ✅ Test stack trace reading
- ✅ Test circular buffer overflow behavior

Created demonstration script (`stack_demo.ts`):
- Shows stack tracking initialization
- Demonstrates normal operation
- Shows stack cleanup after function returns

## Performance Impact

### Memory Overhead
- **~6144 bytes** (~6KB) for 256-frame circular buffer (24 bytes per frame)
- **1024 bytes** (1KB) for context string buffer pool (8 × 128 bytes)
- **Total: ~7KB** static allocation during initialization
- No dynamic memory allocation

### Runtime Overhead
- ~3-4 instructions per function call (push with timestamp + context)
- ~1 instruction per function return (pop)
- Timestamp capture via `wasm_get_time_ms()` import
- Estimated **1-2%** impact for typical workloads
- Can be completely disabled for production builds

## Output Example

When an assertion fails or the module aborts:

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

**New information provided:**
- **Timestamps**: Shows when each function was entered, useful for identifying performance bottlenecks
- **Context**: Shows relevant information like surface dimensions, helping identify which specific surface caused the issue

## Technical Details

### Circular Buffer Design
- Fixed-size array of 256 StackFrame structures
- Each frame: 24 bytes (function pointer + line + timestamp + context + reserved)
- Overwrites oldest entries when full
- Stack pointer wraps at MAX_STACK_DEPTH
- Overflow protection prevents integer overflow

### Timestamp Capture
- Uses `wasm_get_time_ms()` import (already available in the system)
- Millisecond precision via JavaScript's `performance.now()`
- Captured at function entry via `STACK_ENTER()`
- Stored as double (8 bytes) for high precision

### Context String Storage
- 8-buffer pool, each 128 bytes
- Circular reuse prevents memory exhaustion
- Helper functions format common contexts:
  - `stack_format_alloc_context(bytes)` - for memory allocations
  - `stack_format_surface_context(id, w, h)` - for surface operations
- Context strings are optional (can be NULL)

### Function Name Storage
- Uses `__FUNCTION__` macro (compile-time constant)
- Function names are pointers to static strings
- No string copying or allocation needed
- Line numbers captured via `__LINE__` macro

### Memory Safety
- All exports check for null/zero pointers
- String reading has 256-byte safety limit
- Defensive bounds checking in all operations

## Files Modified/Created

### New Files
- `awtea-graphics/src/main/native/awt_stack.h`
- `awtea-graphics/src/main/native/awt_stack.c`
- `awtea-graphics/src/test/deno/stack_test.ts`
- `awtea-graphics/src/test/deno/stack_demo.ts`

### Modified Files
- `awtea-graphics/src/main/native/awt_surface.c`
- `awtea-graphics/src/main/native/awt_draw.c`
- `awtea-graphics/src/main/native/awt_commands.c`
- `awtea-graphics/src/main/java/me/mdbell/awtea/gfx/wasm/WasmAwtRasterizerExports.java`
- `awtea-graphics/src/main/java/me/mdbell/awtea/gfx/wasm/WasmSurfaceBackend.java`
- `awtea-graphics/src/test/deno/wasm_rasterizer.ts`
- `awtea-graphics/build.gradle.kts`
- `docs/WASM_IMPORTS.md`

## Testing Results

All tests passing:
```
Stack tracking - exports are available ... ok
Stack tracking - depth increases during operations ... ok
Stack tracking - read stack frames after operations ... ok
Stack tracking - circular buffer overflow ... ok
```

Build successful:
```
BUILD SUCCESSFUL in 6s
22 actionable tasks: 8 executed, 14 up-to-date
```

## Usage Guide

### For Debugging
1. Run code normally - stack tracking is enabled by default
2. When assertion fails, check logs for stack trace
3. Stack trace shows call chain leading to failure
4. Use function names and line numbers to locate issue

### For Production
Disable stack tracking for zero overhead:
```bash
emcc -DENABLE_WASM_STACK_TRACKING=0 ...
```

### Adding to New Functions
```c
void my_function(int arg) {
    STACK_ENTER();
    
    // Function body
    if (error_condition) {
        STACK_EXIT();
        return;
    }
    
    do_work();
    
    STACK_EXIT();
}
```

### Adding with Context
```c
void allocate_buffer(size_t bytes) {
    STACK_ENTER_CTX(stack_format_alloc_context(bytes));
    
    void* ptr = malloc(bytes);
    // ... use ptr ...
    
    STACK_EXIT();
}

void process_surface(int id, int width, int height) {
    STACK_ENTER_CTX(stack_format_surface_context(id, width, height));
    
    // Process surface
    
    STACK_EXIT();
}
```

## Future Enhancements

Potential improvements (not in scope for this PR):
- Add frame timestamps for performance profiling
- Support for nested function arguments/locals
- Integration with source maps for minified builds
- Optional compression for very deep stacks
- Web debugger UI for visualizing stack traces

## Conclusion

The stack tracking system is fully implemented, tested, and documented. It provides valuable debugging information when WASM crashes occur, with minimal performance overhead and the ability to disable completely for production builds.
