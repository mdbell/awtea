# WASM Stack Tracking System - Implementation Summary

## Overview
Added a complete call stack tracking system for the WASM rasterizer to enable debugging when crashes or assertion failures occur. The stack is readable externally from JavaScript/Java even after the module crashes, enabling post-mortem analysis.

## Key Features Implemented

### 1. Core C Implementation
- **`awt_stack.h`**: API header with stack frame structure and macros
- **`awt_stack.c`**: Circular buffer implementation (256 frames, 2KB total)
- **Stack Frame**: 8 bytes per frame (function name pointer + line number)
- **Macros**: `STACK_ENTER()`, `STACK_EXIT()` for easy integration

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
- **2048 bytes** (2KB) for 256-frame circular buffer
- Static allocation during initialization
- No dynamic memory allocation

### Runtime Overhead
- ~2 instructions per function call (push)
- ~1 instruction per function return (pop)
- Estimated **< 1%** impact for typical workloads
- Can be completely disabled for production builds

## Output Example

When an assertion fails or the module aborts:

```
[ERROR] WASM assertion failed: buffer != NULL at awt_draw.c:45
[ERROR] WASM call stack:
Call stack (depth=5):
  #0: draw_filled_rect (line 23)
  #1: render_awt (line 67)
  #2: blit_image (line 156)
  #3: create_context (line 178)
  #4: init_surface_system (line 12)
```

## Technical Details

### Circular Buffer Design
- Fixed-size array of 256 StackFrame structures
- Overwrites oldest entries when full
- Stack pointer wraps at MAX_STACK_DEPTH
- Overflow protection prevents integer overflow

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

## Future Enhancements

Potential improvements (not in scope for this PR):
- Add frame timestamps for performance profiling
- Support for nested function arguments/locals
- Integration with source maps for minified builds
- Optional compression for very deep stacks
- Web debugger UI for visualizing stack traces

## Conclusion

The stack tracking system is fully implemented, tested, and documented. It provides valuable debugging information when WASM crashes occur, with minimal performance overhead and the ability to disable completely for production builds.
