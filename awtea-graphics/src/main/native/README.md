# Native C WASM Rasterizer

This directory contains the native C implementation of the AWT rasterizer, compiled to WebAssembly using Emscripten.

## Code Quality Standards

### Memory Management

**All dynamic memory allocations MUST use tracked allocation functions:**

```c
void* ptr = tracked_malloc(size);    // Instead of malloc()
ptr = tracked_realloc(ptr, new_size); // Instead of realloc()
tracked_free(ptr);                    // Instead of free()
```

**Rationale:** Tracked allocation provides memory usage statistics to the host and helps detect memory leaks during development.

**Exception:** Only deviate from tracked allocation if there's a specific technical requirement (e.g., interfacing with external libraries). Document the reason with a comment.

### Magic Constants

**All buffer sizes and capacity constants MUST be defined with explanatory comments:**

```c
// Command buffer size: 32KB = 8192 words (4-byte units)
// Rationale: Large enough for batching, small enough for WASM memory
// Overflow policy: Commands exceeding this size split across multiple calls
#define COMMAND_BUFFER_SIZE_WORDS 8192
```

**Key constants documented:**
- `COMMAND_BUFFER_SIZE_WORDS` (8192) - Command buffer size
- `MAX_STACK_DEPTH` (256) - Stack trace buffer depth
- `CONTEXT_BUFFER_SIZE` (128) - Context string formatting buffer
- `CONTEXT_BUFFER_POOL_SIZE` (8) - Number of context buffers
- `EDGE_TABLE_POOL_INITIAL_CAPACITY` (4) - Edge table pool size
- `EDGE_TABLE_ACTIVE_INITIAL_CAPACITY` (16) - Active edge list size
- `EDGE_TABLE_SCANLINE_INITIAL_CAPACITY` (4) - Per-scanline edge list size

### Stack Tracking

**All exported functions MUST use stack tracking macros:**

```c
int my_function(int param) {
    STACK_ENTER();  // At function entry
    
    if (error_condition) {
        STACK_EXIT_ERR(error_code);  // Before returning error
        return error_code;
    }
    
    STACK_EXIT();  // Before successful return
    return 0;
}
```

**Extended tracking for complex operations:**
```c
STACK_ENTER_EXT(context_string, surface_id, context_id, operation, cmd_idx, ref_count);
```

### Buffer Safety

**All string formatting MUST use bounds-checked functions:**

```c
int written = snprintf(buffer, BUFFER_SIZE, "format %d", value);

// Check for truncation
if (written < 0 || written >= BUFFER_SIZE) {
    log_warn("Buffer truncated: needed %d bytes", written);
    buffer[BUFFER_SIZE - 1] = '\0';  // Ensure null termination
}
```

**Never use unsafe functions:** `sprintf`, `strcpy`, `strcat` are prohibited.

### Assertion Usage

**Use WASM_ASSERT for runtime invariant checking:**

```c
WASM_ASSERT(ptr != NULL);
WASM_ASSERT(index >= 0 && index < capacity);
```

**Note:** Assertions are disabled in release builds (`-DNDEBUG`).

## Build Configuration

### Debug Build (Default)
```bash
./gradlew :awtea-graphics:buildAwtRasterWasm
# Or explicitly:
./gradlew -PwasmBuildMode=debug :awtea-graphics:buildAwtRasterWasm
```

**Flags:**
- `-O1` - Minimal optimization for faster builds and better debugging
- `-g` - Include debug symbols
- `-DAWTEA_DEBUG_BUILD=1` - Enable all debug features

### Release Build
```bash
./gradlew -PwasmBuildMode=release :awtea-graphics:buildAwtRasterWasm
```

**Flags:**
- `-O3` - Maximum optimization
- `-DNDEBUG` - Disable C assertions
- `-DAWTEA_DEBUG_BUILD=0` - Disable debug features

### Debug Feature Flags

Controlled by `AWTEA_DEBUG_BUILD` in `awt_build_info.h`:

| Flag | Description | Impact when Disabled |
|------|-------------|---------------------|
| `ENABLE_WASM_STACK_TRACKING` | Stack trace buffer for crash debugging | No stack traces, smaller binary |
| `ENABLE_WASM_ASSERTIONS` | Runtime assertion checks | No assertion overhead |
| `ENABLE_WASM_LOGGING` | Logging system | No log output, smaller binary |
| `ENABLE_WASM_MEMORY_TRACKING` | Memory allocation tracking | No memory stats, minimal overhead |

## Resource Lifecycle Management

### Edge Table Pool

The global edge table pool is lazily initialized on first use and persists for the module lifetime.

**Cleanup:** Call `cleanup_edge_table_pool()` during module unload or application shutdown:

```c
cleanup_edge_table_pool();  // Frees all pooled edge tables
```

**Monitoring:** Check pool size for debugging:

```c
int pool_size = get_edge_table_pool_size();  // Number of tables in pool
```

## Import/Export Conventions

### C → JavaScript/Deno Imports

All host-provided functions use consistent pointer+length pattern for strings:

```c
extern void wasm_log_callback(int level, const char* msg_ptr, int msg_len);
extern void wasm_assertion_failed(const char* expr_ptr, int expr_len,
                                   const char* file_ptr, int file_len,
                                   int line);
```

### WASM Exports

Functions exported to JavaScript/Deno use `__attribute__((export_name("name")))`:

```c
__attribute__((export_name("get_edge_table_pool_size")))
int get_edge_table_pool_size(void);
```

## Testing

### Deno Tests (TypeScript)
```bash
./gradlew :awtea-graphics:denoTest
```

Tests run in isolation using Deno, directly loading the compiled WASM module. No TeaVM or Java runtime required.

**Test Coverage:**
- WASM imports/exports verification
- Stack tracking functionality
- Memory tracking
- Assertion callback
- Surface and context operations
- Drawing primitives (fill, draw, blit)
- Composite operations

### Java Tests (Experimental)
```bash
./gradlew :awtea-graphics:buildDenoJavaTests
./gradlew :awtea-graphics:denoTestJava
```

Compiles Java tests to JavaScript via TeaVM, then executes in Deno.

## Common Patterns

### Creating New Drawing Primitives

1. Add handler function to `awt_draw.c` or appropriate file
2. Use `STACK_ENTER()` / `STACK_EXIT()` for tracking
3. Use `tracked_malloc()` / `tracked_free()` for allocations
4. Document any magic constants
5. Add Deno tests in `src/test/deno/*_test.ts`
6. Add visual demos in `src/test/deno/*_demo.ts`

### Error Handling Pattern

```c
int my_function(int param) {
    STACK_ENTER();
    
    // Validate inputs
    if (param < 0) {
        log_error("Invalid parameter: %d", param);
        STACK_EXIT_ERR(-1);
        return -1;
    }
    
    // Allocate resources
    void* buffer = tracked_malloc(size);
    if (!buffer) {
        log_error("Allocation failed: %zu bytes", size);
        STACK_EXIT_ERR(-2);
        return -2;
    }
    
    // Perform operation
    int result = do_work(buffer);
    
    // Clean up resources (even on error)
    tracked_free(buffer);
    
    if (result != 0) {
        log_error("Operation failed: %d", result);
        STACK_EXIT_ERR(result);
        return result;
    }
    
    STACK_EXIT();
    return 0;
}
```

## References

- **Emscripten Documentation:** https://emscripten.org/docs/
- **WASM Import/Export Guide:** `docs/WASM_IMPORTS.md`
- **Rendering Backend Architecture:** `docs/RENDERING_BACKENDS.md`
- **System Properties:** `docs/SYSTEM_PROPERTIES.md`
