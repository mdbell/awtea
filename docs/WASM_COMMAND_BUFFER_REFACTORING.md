# WASM Command Buffer Refactoring

## Overview

This document describes the refactoring of command buffers in the WASM rasterizer backend from dynamically allocated buffers to fixed-size per-context buffers.

## Motivation

Previously, command buffers were dynamically allocated on every render operation:
- `SurfaceCommandBuffer` constructor called `exports.requestCommandBuffer(maxCommands)` which performed a `tracked_malloc()`
- Each buffer had to be manually freed by calling the `free()` method
- This created allocation overhead on every render operation

## Changes

### C/WASM Side

#### 1. Added `MAX_CONTEXT_COMMANDS` Constant
- **File**: `awtea-graphics/src/main/native/awt_raster_internal.h`
- **Value**: 512 commands per context
- **Purpose**: Defines the fixed size of each context's command buffer

#### 2. Updated `SurfaceContext` Structure
- **File**: `awtea-graphics/src/main/native/awt_raster_internal.h`
- **Added fields**:
  - `SurfaceCommand* command_buffer`: Pointer to the fixed-size command buffer
  - `int max_commands`: Size of the buffer (always `MAX_CONTEXT_COMMANDS`)

#### 3. Updated Context Management Functions
- **File**: `awtea-graphics/src/main/native/awt_surface.c`

**`create_context()`**:
- Now allocates a fixed-size command buffer when creating a context
- Buffer is allocated with `tracked_malloc()` and initialized to zero
- Returns error if allocation fails

**`clone_context()`**:
- Allocates a new command buffer for the cloned context
- Each cloned context gets its own independent buffer

**`destroy_context()`**:
- Frees the command buffer with `tracked_free()` before marking context as unused
- Sets `command_buffer` to NULL and `max_commands` to 0

#### 4. New WASM Exports
- **File**: `awtea-graphics/src/main/native/awt_surface.h` and `awt_surface.c`

**`get_max_context_commands()`**:
- Returns `MAX_CONTEXT_COMMANDS` constant
- Allows Java/TypeScript code to query the buffer size

**`get_context_command_buffer_ptr(int context_id)`**:
- Returns the pointer to a context's command buffer
- Returns 0 if context is invalid or has no buffer

#### 5. Updated `render_awt()` Function
- **File**: `awtea-graphics/src/main/native/awt_commands.c`
- When `cmdPtr` parameter is 0, uses the context's internal buffer instead
- Falls back to using the provided pointer for backward compatibility

### Java Side

#### 1. Added New Exports
- **File**: `WasmAwtRasterizerExports.java`
- Added `getMaxContextCommands()` method
- Added `getContextCommandBufferPtr(int contextId)` method

#### 2. Refactored `SurfaceCommandBuffer`
- **File**: `SurfaceCommandBuffer.java`

**Constructor Changes**:
- If `contextId != -1`, gets buffer from context via `getContextCommandBufferPtr()`
- If `contextId == -1`, uses legacy path (allocates temporary buffer)
- Buffer size comes from `getMaxContextCommands()` when using context buffer

**`free()` Method**:
- Now checks if buffer is context-owned (contextId != -1)
- Context-owned buffers are not freed (no-op) - they're freed when context is destroyed
- Legacy allocated buffers are still freed normally

#### 3. Updated `WasmSurface`
- **File**: `WasmSurface.java`
- Added `createBufferForContext(int contextId)` method
- This method creates a `SurfaceCommandBuffer` that uses the context's internal buffer
- Existing `createBuffer()` methods remain for backward compatibility

#### 4. Updated `WasmRasterizer`
- **File**: `WasmRasterizer.java`
- Changed to use `surface.createBufferForContext(contextId)` instead of `createBuffer()`
- Eliminates the need to call `setContextId()` after creation
- The `commandBuffer.free()` call in `dispose()` is now a no-op (buffer freed with context)

### TypeScript Test Side

#### 1. Added New Methods to Test Harness
- **File**: `wasm_rasterizer.ts`

**`getMaxContextCommands()`**:
- Queries the maximum buffer size from WASM

**`getContextCommandBufferPtr(contextId: number)`**:
- Gets the pointer to a context's command buffer

**`renderCommandsToContext(contextId: number, commands: SurfaceCommand[])`**:
- Helper method that writes commands to the context buffer and renders
- Uses `render_awt` with `cmdPtr=0` to use the context buffer
- More convenient than manual buffer management

#### 2. Added New Test File
- **File**: `context_buffer_test.ts`
- Tests basic buffer allocation and usage
- Tests rendering with context buffer (cmdPtr=0)
- Tests helper method `renderCommandsToContext()`
- Tests that multiple contexts have independent buffers
- Tests that cloned contexts get their own buffers
- Tests backward compatibility with explicit buffers

## Benefits

1. **No Allocation Overhead**: Command buffers are allocated once per context, not per render operation
2. **Predictable Memory Usage**: Each context has exactly 512 commands worth of buffer space
3. **Simpler API**: No manual `free()` calls needed for context buffers
4. **Thread-Safety Ready**: Each context has its own isolated buffer
5. **Flexible**: Buffer size is exported for introspection
6. **Backward Compatible**: Old code using explicit buffers still works

## Usage Examples

### Java Usage

```java
// Old way (still works, but allocates temporarily)
SurfaceCommandBuffer buffer = surface.createBuffer();
buffer.setContextId(contextId);
// ... use buffer ...
buffer.free(); // Must free manually

// New way (uses context's buffer)
SurfaceCommandBuffer buffer = surface.createBufferForContext(contextId);
// ... use buffer ...
buffer.free(); // No-op, buffer is freed with context
```

### TypeScript Usage

```typescript
// New helper method (recommended)
const commands = [
  WasmRasterizer.setColorCommand(red),
  WasmRasterizer.fillRectCommand(0, 0, 10, 10),
];
rasterizer.renderCommandsToContext(contextId, commands);

// Or manually using context buffer
const bufferPtr = rasterizer.getContextCommandBufferPtr(contextId);
rasterizer.writeCommand(bufferPtr, 0, WasmRasterizer.setColorCommand(red));
rasterizer.writeCommand(bufferPtr, 1, WasmRasterizer.fillRectCommand(0, 0, 10, 10));
const wasm = rasterizer.getExports();
wasm.render_awt(contextId, 0, 2); // 0 = use context buffer

// Old way still works (allocates temporary buffer)
const tempBuffer = rasterizer.createCommandBuffer(10);
rasterizer.writeCommand(tempBuffer, 0, ...);
rasterizer.renderCommands(contextId, tempBuffer, count);
```

## Migration Guide

### For Existing Code

Existing code will continue to work without changes because:
1. `WasmRasterizer` automatically uses the new context buffer approach
2. Legacy `SurfaceCommandBuffer` creation (without contextId) still allocates temporary buffers
3. The `free()` method intelligently handles both context-owned and temporary buffers

### Recommended Changes

To take full advantage of the new system:

1. **Java**: Use `surface.createBufferForContext(contextId)` instead of `surface.createBuffer()` + `setContextId()`
2. **TypeScript**: Use `renderCommandsToContext()` helper method for simpler code
3. **Remove explicit `free()` calls** when using context buffers (they're no-ops anyway)

## Building

### WASM Module Compilation

The WASM module must be recompiled for the C changes to take effect:

```bash
./gradlew :awtea-graphics:buildAwtRasterWasm
```

**Requirements**:
- Emscripten SDK must be installed and on PATH
- Use the dev container for a pre-configured environment

### Java Compilation

```bash
./gradlew :awtea-graphics:compileJava
```

### Running Tests

```bash
./gradlew :awtea-graphics:denoTest
```

## Performance Impact

Expected improvements:
- **Reduced allocation calls**: 1 allocation per context vs N allocations per render
- **Reduced memory fragmentation**: Fixed-size buffers reused throughout context lifetime
- **Cache-friendly**: Same buffer memory reused improves cache locality

Measured impact: (To be measured after WASM rebuild)

## Future Considerations

1. **Buffer Size Tuning**: The 512 command limit may need adjustment based on real usage patterns
2. **Overflow Handling**: Consider adding automatic flush-on-overflow in `SurfaceCommandBuffer`
3. **Statistics**: Add tracking for buffer usage to identify optimal buffer size
4. **Buffer Pooling**: Consider pooling temporary buffers (legacy path) to reduce allocation overhead
