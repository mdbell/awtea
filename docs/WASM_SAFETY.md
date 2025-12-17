# WASM Surface/Context Safety and Best Practices

This document provides comprehensive guidance on safely using the WASM surface/context system in awtea, covering memory management, resource limits, concurrency, pooling, and debugging.

## Table of Contents

- [Memory Management](#memory-management)
- [Resource Limits](#resource-limits)
- [Concurrency](#concurrency)
- [Surface Pool Integration](#surface-pool-integration)
- [Diagnostics & Debugging](#diagnostics--debugging)
- [Error Handling](#error-handling)
- [Best Practices](#best-practices)
- [Common Pitfalls](#common-pitfalls)

## Memory Management

### Destroy Methods are Idempotent

All destroy operations are safe to call multiple times:

```java
WasmSurface surface = backend.createSurface(800, 600, Surface.FORMAT_INT_ARGB);
surface.destroy(); // First call - frees resources
surface.destroy(); // Second call - safe no-op
surface.destroy(); // Third call - also safe
```

```java
WasmRasterizer rasterizer = (WasmRasterizer) surface.createRasterizer();
rasterizer.dispose(); // First call - frees context
rasterizer.dispose(); // Second call - safe no-op
```

**Why idempotent?** This prevents double-free errors and makes cleanup code simpler in error-handling paths.

### Always Explicitly Destroy Resources

Surfaces and rasterizers must be explicitly destroyed to free WASM memory:

```java
// Good: Explicit cleanup
WasmSurface surface = backend.createSurface(800, 600, Surface.FORMAT_INT_ARGB);
try {
    // Use surface...
} finally {
    surface.destroy(); // Always called, even if exception occurs
}
```

```java
// Good: Try-with-resources style
WasmSurface surface = backend.createSurface(800, 600, Surface.FORMAT_INT_ARGB);
WasmRasterizer rasterizer = (WasmRasterizer) surface.createRasterizer();
try {
    // Use rasterizer...
} finally {
    rasterizer.dispose();
    surface.destroy();
}
```

**Note:** While surfaces and rasterizers have finalizers that will eventually free resources, relying on them can lead to resource exhaustion. Always call `destroy()` or `dispose()` explicitly.

### Leak Detection Warnings

If you forget to destroy a surface or rasterizer, you'll see warnings like:

```
[WARN] WasmSurface 1025 was finalized without explicit destroy() - possible resource leak. 
       Always call destroy() when done with a surface. Size: 800x600, Format: 0
```

```
[WARN] WasmRasterizer with context 1024 was finalized without explicit dispose() - possible resource leak. 
       Always call dispose() when done with a rasterizer.
```

These warnings indicate you're leaking resources. Fix them by adding proper cleanup code.

### Surface Reference Counting

Surfaces use reference counting to track how many contexts (rasterizers) are using them:

```java
WasmSurface surface = backend.createSurface(800, 600, Surface.FORMAT_INT_ARGB);
// surface.ref_count = 0

WasmRasterizer rast1 = (WasmRasterizer) surface.createRasterizer();
// surface.ref_count = 1

WasmRasterizer rast2 = (WasmRasterizer) surface.createRasterizer();
// surface.ref_count = 2

rast1.dispose();
// surface.ref_count = 1

rast2.dispose();
// surface.ref_count = 0

surface.destroy();
```

You can query reference counts via the diagnostics API:

```java
WasmDiagnostics diag = backend.getDiagnostics();
int refCount = diag.getSurfaceRefCount(surface.getId());
```

### WASM Memory Buffer Invalidation

**Important:** WASM memory can grow, invalidating existing ArrayBuffer views. If you hold references to `Uint8ClampedArray` obtained from `surface.getPixelData()`, they may become invalid after WASM allocates more memory.

**Best practice:** Get pixel data immediately before use, don't cache it:

```java
// Bad: Cached pixel data may become invalid
Uint8ClampedArray pixels = surface.getPixelData();
// ... do lots of operations that might allocate WASM memory ...
pixels.set(someData); // May fail if WASM memory grew!

// Good: Get pixel data immediately before use
doSomeWork();
Uint8ClampedArray pixels = surface.getPixelData();
pixels.set(someData); // Safe - just obtained
```

## Resource Limits

### Capacity Limits

The WASM module has fixed capacity limits:

- **MAX_SURFACES**: 1024 surfaces
- **MAX_CONTEXTS**: 2048 contexts

These limits are compiled into the WASM module and cannot be changed at runtime.

### Graceful Handling of Capacity Exhaustion

When capacity is reached, you'll get descriptive error messages:

```java
try {
    WasmSurface surface = backend.createSurface(800, 600, Surface.FORMAT_INT_ARGB);
} catch (IllegalStateException e) {
    // "No free surface IDs available (1024 / 1024 surfaces active). 
    //  Consider destroying unused surfaces or increasing MAX_SURFACES."
}
```

```java
try {
    WasmRasterizer rast = (WasmRasterizer) surface.createRasterizer();
} catch (IllegalStateException e) {
    // "Failed to create context for surface 1025: no free context IDs available 
    //  (2048 / 2048 contexts active). Consider disposing unused rasterizers or increasing MAX_CONTEXTS."
}
```

### Capacity Warnings

The backend automatically logs warnings when approaching capacity:

```
[WARN] Surface capacity at 92.3% (945 / 1024), approaching limit
```

You can check capacity programmatically:

```java
WasmDiagnostics diag = backend.getDiagnostics();

if (diag.isSurfaceCapacityWarning(0.8)) {
    log.warn("Surface capacity at {}%, consider cleanup", 
            diag.getSurfaceUtilization() * 100);
}

if (diag.isContextCapacityWarning(0.9)) {
    log.warn("Context capacity at {}%, consider cleanup", 
            diag.getContextUtilization() * 100);
}
```

### Error Codes

All WASM functions return error codes. Common codes:

| Code | Meaning |
|------|---------|
| 0    | Success |
| -1   | Generic failure (invalid ID, no free slots, etc.) |
| -2   | Invalid surface data |
| -3   | Invalid ID range |
| -4   | Incomplete command data |
| -5   | Invalid operation |
| -6   | Failed to skip invalid command |

Error codes are mapped to descriptive Java exceptions automatically.

## Concurrency

### Surfaces and Contexts are NOT Thread-Safe

**Critical:** `WasmSurface` and `WasmRasterizer` are NOT inherently thread-safe. Do not access them from multiple threads simultaneously.

```java
// BAD: Multiple threads using same rasterizer
WasmRasterizer rasterizer = (WasmRasterizer) surface.createRasterizer();

Thread t1 = new Thread(() -> {
    rasterizer.rasterizeCommands(commands1); // UNSAFE
});

Thread t2 = new Thread(() -> {
    rasterizer.rasterizeCommands(commands2); // UNSAFE - race condition!
});

t1.start();
t2.start();
```

### Multi-Threading Pattern: Create Per-Thread Rasterizers

Each thread should create its own rasterizer using `create()`, which clones the context:

```java
// GOOD: Each thread has its own rasterizer
WasmSurface surface = backend.createSurface(800, 600, Surface.FORMAT_INT_ARGB);
WasmRasterizer mainRasterizer = (WasmRasterizer) surface.createRasterizer();

Thread t1 = new Thread(() -> {
    WasmRasterizer rast = mainRasterizer.create(); // Clone context
    try {
        rast.rasterizeCommands(commands1); // Safe
    } finally {
        rast.dispose();
    }
});

Thread t2 = new Thread(() -> {
    WasmRasterizer rast = mainRasterizer.create(); // Clone context
    try {
        rast.rasterizeCommands(commands2); // Safe
    } finally {
        rast.dispose();
    }
});

t1.start();
t2.start();
t1.join();
t2.join();

mainRasterizer.dispose();
surface.destroy();
```

The `create()` method calls `cloneContext()` in WASM, which:
- Creates a new context with independent rendering state
- Increments the surface reference count
- Allocates a new command buffer

### Thread Safety via createReference()

For more advanced scenarios, you can use `createReference()` to share a surface across threads (though each thread should still have its own rasterizer):

```java
// Thread 1: Create and use surface
WasmSurface surface = backend.createSurface(800, 600, Surface.FORMAT_INT_ARGB);
int surfaceId = surface.getId();

// Thread 2: Create reference to same surface
int refSurfaceId = backend.exports.createReference(surfaceId);
// Now both threads can safely access the surface as long as each has its own rasterizer
```

**Remember:** Even with references, you still need separate rasterizers per thread.

## Surface Pool Integration

### Pooling is Transparent

The surface pool is completely transparent - just use `createSurface()` and `destroy()` as usual:

```java
// Pooling happens automatically
WasmSurface surface = backend.createSurface(800, 600, Surface.FORMAT_INT_ARGB);
// ... use surface ...
surface.destroy(); // Returns to pool

// Next creation may reuse the pooled surface
WasmSurface surface2 = backend.createSurface(800, 600, Surface.FORMAT_INT_ARGB);
```

### Ownership Semantics

**Important:** The pool NEVER returns the pooled instance directly. It always:

1. Takes a surface from the pool
2. Resets its state (via `resize()`)
3. Returns it as a fresh, clean surface

This ensures:
- No state leakage between reuses
- Reference counts are correct
- Pixel data is reallocated

### Disabling Pooling for Specific Surfaces

If you need a surface that should NOT be pooled (e.g., long-lived surfaces):

```java
WasmSurface surface = backend.createSurface(800, 600, Surface.FORMAT_INT_ARGB);
surface.setPoolable(false); // This surface will be destroyed immediately on destroy()
// ... use surface ...
surface.destroy(); // Direct destruction, not returned to pool
```

### Pool Cleanup

The pool automatically handles cleanup on eviction:

```java
WasmSurfacePool pool = backend.getSurfacePool();

// Clear entire pool
pool.clear(); // Destroys all pooled surfaces

// Trim pool (remove LRU surface from each key)
int removed = pool.trim();

// Evict old surfaces (older than 60 seconds)
int evicted = pool.evictOld(60000);
```

## Diagnostics & Debugging

### WasmDiagnostics API

Access diagnostics via the backend:

```java
WasmSurfaceBackend backend = new WasmSurfaceBackend();
WasmDiagnostics diag = backend.getDiagnostics();
```

### Query Active Counts

```java
// How many surfaces are currently active?
int activeSurfaces = diag.getActiveSurfaceCount();

// How many contexts are currently active?
int activeContexts = diag.getActiveContextCount();

// What's the reference count for a specific surface?
int refCount = diag.getSurfaceRefCount(surfaceId);
```

### Query Capacity Limits

```java
// Maximum surfaces supported
int maxSurfaces = diag.getMaxSurfaces(); // 1024

// Maximum contexts supported
int maxContexts = diag.getMaxContexts(); // 2048
```

### Utilization Metrics

```java
// Surface utilization (0.0 to 1.0)
double surfaceUtil = diag.getSurfaceUtilization();

// Context utilization (0.0 to 1.0)
double contextUtil = diag.getContextUtilization();

// Check if approaching capacity
if (diag.isSurfaceCapacityWarning(0.8)) {
    System.out.println("WARNING: Surface capacity exceeds 80%");
}

if (diag.isContextCapacityWarning(0.9)) {
    System.out.println("WARNING: Context capacity exceeds 90%");
}
```

### Diagnostic Reports

```java
// Get a formatted report
String report = diag.getReport();
System.out.println(report);

// Output:
// WASM Surface/Context Diagnostics:
//   Surfaces: 123 / 1024 (12.0%)
//   Contexts: 256 / 2048 (12.5%)
```

### WASM Stack Traces

When an abort or assertion failure occurs, the backend automatically logs the WASM call stack:

```
[ERROR] WASM module aborted execution
[ERROR] WASM call stack:
  #0: render_awt (line 85) [12.345ms] ERR=-1 surf=1025 ctx=2048
  #1: handle_fill_rect (line 234) [0.123ms] surf=1025 ctx=2048 op=7
  #2: fill_rect_internal (line 567) [0.089ms] surf=1025
```

This helps debug issues deep in the WASM module.

### Enabling Debug Logging

For detailed diagnostics:

```java
import me.mdbell.awtea.util.logging.LoggerFactory;
import me.mdbell.awtea.util.logging.LogLevel;

LoggerFactory.setGlobalLevel(LogLevel.DEBUG);
```

## Error Handling

### Invalid IDs

All WASM functions validate IDs before operations:

```c
SurfaceData* surface = get_surface_data(surface_id);
if (surface_id < START_SURFACE_ID || surface_id >= END_SURFACE_ID) {
    log_error("Invalid surface ID: %d", surface_id);
    return -3;
}
WASM_ASSERT(surface != NULL && "get_surface_data returned NULL");
```

### Assertions

The WASM module uses `WASM_ASSERT()` macros in critical paths:

```c
WASM_ASSERT(surface->width > 0 && surface->height > 0 && "Surface must have positive dimensions");
```

When an assertion fails, you'll see:

```
[ERROR] WASM assertion failed: surface->width > 0 && surface->height > 0 at awt_surface.c:198
[ERROR] WASM call stack: ...
```

Assertions can be disabled by rebuilding WASM with `-DENABLE_WASM_ASSERTIONS=0` for production builds.

## Best Practices

### 1. Always Use Try-Finally for Cleanup

```java
WasmSurface surface = backend.createSurface(800, 600, Surface.FORMAT_INT_ARGB);
WasmRasterizer rasterizer = null;
try {
    rasterizer = (WasmRasterizer) surface.createRasterizer();
    // ... use rasterizer ...
} finally {
    if (rasterizer != null) {
        rasterizer.dispose();
    }
    surface.destroy();
}
```

### 2. Monitor Capacity in Long-Running Applications

```java
// Periodic cleanup task
Timer timer = new Timer();
timer.scheduleAtFixedRate(new TimerTask() {
    @Override
    public void run() {
        WasmDiagnostics diag = backend.getDiagnostics();
        if (diag.isSurfaceCapacityWarning(0.8)) {
            log.warn("Surface capacity high, triggering cleanup");
            backend.getSurfacePool().trim();
        }
    }
}, 60000, 60000); // Every 60 seconds
```

### 3. Use Pooling for High-Frequency Creation

For text rendering, UI, or image processing with frequent surface creation:

```java
// Let pooling handle reuse automatically
for (int i = 0; i < 1000; i++) {
    WasmSurface surface = backend.createSurface(32, 32, Surface.FORMAT_INT_ARGB);
    // ... render glyph ...
    surface.destroy(); // Returns to pool
}
```

### 4. Disable Pooling for Long-Lived Surfaces

```java
// Main window surface - keep for duration of app
WasmSurface mainSurface = backend.createSurface(1920, 1080, Surface.FORMAT_INT_ARGB);
mainSurface.setPoolable(false); // Don't pool this one
```

### 5. One Rasterizer Per Thread

```java
// Each thread gets its own rasterizer
ExecutorService executor = Executors.newFixedThreadPool(4);
WasmSurface surface = backend.createSurface(800, 600, Surface.FORMAT_INT_ARGB);
WasmRasterizer baseRasterizer = (WasmRasterizer) surface.createRasterizer();

for (int i = 0; i < 4; i++) {
    executor.submit(() -> {
        WasmRasterizer threadRasterizer = baseRasterizer.create();
        try {
            // ... render ...
        } finally {
            threadRasterizer.dispose();
        }
    });
}

executor.shutdown();
executor.awaitTermination(1, TimeUnit.MINUTES);
baseRasterizer.dispose();
surface.destroy();
```

## Common Pitfalls

### Pitfall 1: Forgetting to Destroy

```java
// BAD: Leaks resources
void renderFrame() {
    WasmSurface surface = backend.createSurface(800, 600, Surface.FORMAT_INT_ARGB);
    // ... render ...
    // Oops, forgot surface.destroy()!
}
```

**Fix:** Always destroy in finally block.

### Pitfall 2: Caching Pixel Data

```java
// BAD: Pixel data may become invalid
Uint8ClampedArray pixels = surface.getPixelData();
doLotsOfWork(); // May trigger WASM memory growth
pixels.set(data); // May fail!
```

**Fix:** Get pixel data immediately before use.

### Pitfall 3: Sharing Rasterizers Between Threads

```java
// BAD: Race conditions!
WasmRasterizer rasterizer = (WasmRasterizer) surface.createRasterizer();
Thread t1 = new Thread(() -> rasterizer.rasterizeCommands(cmds1));
Thread t2 = new Thread(() -> rasterizer.rasterizeCommands(cmds2));
```

**Fix:** Each thread creates its own rasterizer via `create()`.

### Pitfall 4: Ignoring Capacity Warnings

```java
// BAD: Keep creating surfaces until crash
while (true) {
    WasmSurface surface = backend.createSurface(800, 600, Surface.FORMAT_INT_ARGB);
    // Never destroy, just create more and more...
}
```

**Fix:** Monitor diagnostics and implement cleanup strategies.

### Pitfall 5: Assuming Pool Returns Same Instance

```java
// BAD: Assuming pooling is just caching instances
WasmSurface s1 = backend.createSurface(800, 600, Surface.FORMAT_INT_ARGB);
// ... draw something on s1 ...
s1.destroy(); // Returns to pool

WasmSurface s2 = backend.createSurface(800, 600, Surface.FORMAT_INT_ARGB);
// s2 may reuse same surface ID, but pixels are CLEARED
// Don't expect s1's pixel data to still be there!
```

**Fix:** Understand that pooling resets state - each acquired surface is clean.

## Related Documentation

- [WasmSurfacePool.md](WasmSurfacePool.md) - Detailed pooling documentation
- [WASM_LOGGING.md](WASM_LOGGING.md) - WASM logging system
- [SYSTEM_PROPERTIES.md](SYSTEM_PROPERTIES.md) - Runtime configuration
- [RENDERING_BACKENDS.md](RENDERING_BACKENDS.md) - Backend selection and architecture

## See Also

- Issue [#56](https://github.com/mdbell/awtea/issues/56) - WASM surface/context safety (this issue)
- Issue [#53](https://github.com/mdbell/awtea/issues/53) - Multi-context architecture
- Issue [#55](https://github.com/mdbell/awtea/issues/55) - Surface pooling
