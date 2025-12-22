# WASM Build Flags Documentation

This document describes the build configuration system for the AWT WASM rasterizer, including the master debug flag and individual feature flags.

## Overview

The WASM rasterizer uses a master `AWTEA_DEBUG_BUILD` flag to control all debug features. This provides a clear distinction between development and production builds while allowing fine-grained control when needed.

## Master Debug Flag

### `AWTEA_DEBUG_BUILD`

**Purpose:** Master switch that controls all debug features

**Default behavior:**
- `AWTEA_DEBUG_BUILD=1` when `NDEBUG` is not defined (debug builds)
- `AWTEA_DEBUG_BUILD=0` when `NDEBUG` is defined (release builds)

**Can be explicitly set at compile time:**
```bash
# Force debug mode
emcc -DAWTEA_DEBUG_BUILD=1 ...

# Force release mode
emcc -DAWTEA_DEBUG_BUILD=0 ...
```

## Individual Feature Flags

Each debug feature can be controlled individually, but by default they inherit from `AWTEA_DEBUG_BUILD`.

### `ENABLE_WASM_STACK_TRACKING`

**Purpose:** Call stack tracking for crash analysis

**Default:** Inherits from `AWTEA_DEBUG_BUILD`

**When enabled:**
- Maintains circular buffer of function calls (256 frames)
- Each frame includes function name, line number, timestamp, and context
- Can be read from host even after crash
- Memory overhead: ~8KB (256 frames × 32 bytes)
- Runtime overhead: ~1-2% (minimal)

**When disabled:**
- All `STACK_ENTER()` and `STACK_EXIT()` macros compile to no-ops
- Zero memory overhead
- Zero runtime overhead
- Stack exports return 0/null

**Header:** `awt_stack.h`

**Exports:**
- `get_stack_buffer_ptr()` - Pointer to stack buffer
- `get_stack_depth()` - Current stack depth
- `get_max_stack_depth()` - Maximum stack depth (256)

### `ENABLE_WASM_ASSERTIONS`

**Purpose:** Runtime assertion checking

**Default:** Inherits from `AWTEA_DEBUG_BUILD`

**When enabled:**
- `WASM_ASSERT(expr)` checks expression and calls `wasm_assertion_failed` callback if false
- Provides expression text, file, and line number to host
- Useful for catching bugs early in development

**When disabled:**
- `WASM_ASSERT(expr)` compiles to `((void)0)` (no-op)
- Zero overhead

**Header:** `awt_imports.h`

**Import required:**
```c
extern void wasm_assertion_failed(const char* expr, const char* file, int line);
```

### `ENABLE_WASM_LOGGING`

**Purpose:** Diagnostic logging to host

**Default:** Inherits from `AWTEA_DEBUG_BUILD`

**When enabled:**
- Provides `log_error()`, `log_warn()`, `log_info()`, `log_debug()`, `log_trace()` functions
- Messages sent to host via `wasm_log_callback` import
- Printf-style formatting supported

**When disabled:**
- All logging functions compile to no-ops
- Zero overhead

**Header:** `awt_log.h`

**Import required:**
```c
extern void wasm_log_callback(int level, const char* message_ptr, int message_len);
```

**Log levels:**
- 0: ERROR
- 1: WARN
- 2: INFO
- 3: DEBUG
- 4: TRACE

### `ENABLE_WASM_MEMORY_TRACKING`

**Purpose:** Track memory allocations for leak detection

**Default:** Inherits from `AWTEA_DEBUG_BUILD`

**When enabled:**
- `tracked_malloc()` and `tracked_free()` maintain allocation statistics
- Reports current allocated bytes, allocation count, and peak usage to host
- Helps identify memory leaks and optimize memory usage

**When disabled:**
- `tracked_malloc()` and `tracked_free()` are simple wrappers around standard `malloc()`/`free()`
- No tracking overhead

**Header:** `awt_memory.h`

**Import required:**
```c
extern void wasm_report_memory_usage(size_t allocated_bytes, size_t alloc_count, size_t peak_bytes);
```

## Build Modes

### Debug Mode (Development)

**Gradle command:**
```bash
./gradlew :awtea-graphics:buildAwtRasterWasm
# or explicitly:
./gradlew :awtea-graphics:buildAwtRasterWasm -PwasmBuildMode=debug
```

**Compiler flags:**
```bash
emcc \
  -O1 \                           # Minimal optimization
  -g \                            # Debug symbols
  -DAWTEA_DEBUG_BUILD=1 \         # Master debug flag ON
  -DAWTEA_BUILD_VERSION="0.1.0-dev" \
  ...
```

**All debug features enabled:**
- Stack tracking
- Assertions
- Logging (all levels)
- Memory tracking

**Use for:**
- Development
- Testing
- Debugging crashes
- Profiling memory usage
- QA validation

### Release Mode (Production)

**Gradle command:**
```bash
./gradlew :awtea-graphics:buildAwtRasterWasm -PwasmBuildMode=release
```

**Compiler flags:**
```bash
emcc \
  -O3 \                           # Maximum optimization
  -DNDEBUG \                      # Standard C assertion disable
  -DAWTEA_DEBUG_BUILD=0 \         # Master debug flag OFF
  -DAWTEA_BUILD_VERSION="0.1.0" \
  ...
```

**All debug features disabled:**
- No stack tracking (zero overhead)
- No assertions (zero overhead)
- No logging (zero overhead)
- No memory tracking (zero overhead)

**Use for:**
- Production deployment
- Performance benchmarking
- Minimal binary size

**Performance characteristics:**
- Smaller WASM binary (debug symbols removed)
- Faster execution (O3 optimization)
- Lower memory usage (no tracking buffers)

## Advanced: Custom Build Configurations

You can create custom builds with specific features enabled/disabled:

### Example: Release with Stack Tracking

Useful for production diagnostics while keeping other overhead minimal:

```bash
./gradlew :awtea-graphics:buildAwtRasterWasm -PwasmBuildMode=release

# Then manually rebuild with custom flags:
emcc \
  -O3 \
  -DAWTEA_DEBUG_BUILD=0 \              # Release mode
  -DENABLE_WASM_STACK_TRACKING=1 \     # But keep stack tracking
  -DAWTEA_BUILD_VERSION="0.1.0" \
  ...
```

### Example: Debug without Memory Tracking

Useful when memory tracking overhead affects the behavior being debugged:

```bash
emcc \
  -O1 \
  -g \
  -DAWTEA_DEBUG_BUILD=1 \
  -DENABLE_WASM_MEMORY_TRACKING=0 \    # Explicitly disable
  ...
```

## Querying Build Configuration at Runtime

The host can query the build configuration using the build info exports:

**TypeScript/JavaScript:**
```typescript
const flags = wasmExports.get_build_flags();

const isDebug = (flags & 0x01) !== 0;
const hasStackTracking = (flags & 0x02) !== 0;
const hasAssertions = (flags & 0x04) !== 0;
const hasLogging = (flags & 0x08) !== 0;
const hasMemoryTracking = (flags & 0x10) !== 0;

console.log(`Debug build: ${isDebug}`);
console.log(`Stack tracking: ${hasStackTracking}`);
console.log(`Assertions: ${hasAssertions}`);
console.log(`Logging: ${hasLogging}`);
console.log(`Memory tracking: ${hasMemoryTracking}`);

// Or get human-readable description:
const flagsStrPtr = wasmExports.get_build_flags_string_ptr();
const flagsDesc = decodeNullTerminatedString(memory, flagsStrPtr);
console.log(`Build: ${flagsDesc}`); // e.g., "DEBUG +STACK +ASSERT +LOG +MEMTRACK"
```

**Java:**
```java
// In WasmSurfaceBackend initialization:
int flags = wasmModule.getExports().getBuildFlags();
String version = readStringFromWasm(wasmModule.getExports().getBuildVersionPtr());
String flagsDesc = readStringFromWasm(wasmModule.getExports().getBuildFlagsStringPtr());

logger.info("Loaded WASM rasterizer: {} (flags: {})", version, flagsDesc);

if ((flags & BUILD_FLAG_DEBUG) != 0) {
    logger.warn("Running DEBUG build in production - performance may be impacted");
}
```

## Best Practices

### Development

1. **Use debug mode by default** - catches bugs early with assertions and provides diagnostic information
2. **Keep all features enabled** - stack tracking is invaluable when crashes occur
3. **Use logging liberally** - helps understand runtime behavior
4. **Monitor memory usage** - detect leaks before they become problems

### Testing

1. **Test both modes** - ensure release builds work correctly
2. **Verify performance** - compare debug vs release to understand overhead
3. **Check binary size** - ensure release builds are optimized

### Production

1. **Use release mode** - zero overhead from debug features
2. **Consider custom builds** - if you need specific diagnostics in production
3. **Version checking** - verify loaded module matches expected version
4. **Flag validation** - warn if debug build is accidentally deployed to production

## Flag Dependency Matrix

| Feature | Depends On | Can Override |
|---------|------------|--------------|
| `AWTEA_DEBUG_BUILD` | `NDEBUG` | Yes |
| `ENABLE_WASM_STACK_TRACKING` | `AWTEA_DEBUG_BUILD` | Yes |
| `ENABLE_WASM_ASSERTIONS` | `AWTEA_DEBUG_BUILD` | Yes |
| `ENABLE_WASM_LOGGING` | `AWTEA_DEBUG_BUILD` | Yes |
| `ENABLE_WASM_MEMORY_TRACKING` | `AWTEA_DEBUG_BUILD` | Yes |

## See Also

- [WASM_IMPORTS.md](WASM_IMPORTS.md) - WASM import/export reference
- [WASM_LOGGING.md](WASM_LOGGING.md) - Logging system details
- `awt_build_info.h` - Build info API header
- `awt_stack.h` - Stack tracking API
- `awt_log.h` - Logging API
- `awt_memory.h` - Memory tracking API
