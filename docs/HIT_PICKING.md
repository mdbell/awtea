# GPU-Based Hit Picking (Picking Buffer)

## Overview

awtea implements a **GPU-based hit-picking system** (also called "picking buffer" or "color picking") to provide fast, O(1) component hit-testing for mouse events. This is an optional optimization that can significantly improve performance for complex UIs with deep component hierarchies.

## Architecture

### Strategy Pattern

Hit-testing is abstracted through the `HitTestStrategy` interface, allowing different implementations:

1. **TreeWalkHitTestStrategy** (default): Traditional recursive tree traversal - O(n) complexity
2. **PickingBufferHitTestStrategy** (WebGL only): GPU-accelerated picking - O(1) complexity

The strategy can be selected via system property or programmatically.

### How GPU Picking Works

#### Component ID Encoding

Each `TComponent` is assigned a unique integer ID at construction time. This ID is encoded as an RGB color:

```java
// Component ID 123456 becomes:
int r = (id >> 16) & 0xFF;  // Red channel
int g = (id >> 8) & 0xFF;   // Green channel  
int b = id & 0xFF;          // Blue channel
```

This allows encoding up to 16,777,215 (2^24 - 1) unique component IDs.

#### Off-Screen Rendering

The picking buffer maintains an off-screen WebGL framebuffer where:
- Each visible component is rendered as a filled rectangle
- The rectangle color encodes the component's unique ID
- Z-ordering is preserved through depth buffering
- The buffer size matches the canvas size

#### Hit-Testing

When a mouse event occurs at coordinates (x, y):

1. If the picking buffer is dirty (layout/hierarchy changed), rebuild it
2. Read the pixel at (x, y) from the picking buffer using `gl.readPixels()`
3. Decode the RGB color back to a component ID
4. Look up the component in the global component registry
5. Return the component (O(1) operation)

### Lazy Invalidation

The picking buffer is **lazily rebuilt** to minimize overhead:

- **Invalidation triggers**: Component add/remove, layout changes, visibility changes, resize
- **Rebuild timing**: Only when the next hit-test is requested AND the buffer is dirty
- **Performance**: Amortizes rebuild cost across multiple events

## Configuration

### System Property

Set the hit-test strategy via system property:

```bash
# Use GPU-based picking buffer (WebGL only)
-Dme.mdbell.awtea.hit_test.strategy=picking_buffer

# Use traditional tree-walk (always available)
-Dme.mdbell.awtea.hit_test.strategy=tree_walk

# Auto-select: Use picking buffer if WebGL available, else tree-walk (default)
-Dme.mdbell.awtea.hit_test.strategy=auto
```

### Programmatic Configuration

You can also set the strategy programmatically:

```java
// Get the event manager from a TFrame or other component
TEventManager eventManager = frame.getEventManager();

// Switch to GPU picking (requires WebGL context)
WebGL2RenderingContext gl = ...; // from WebGLSurfaceBackend
PickingBufferHitTestStrategy strategy = new PickingBufferHitTestStrategy(
    gl, rootContainer, width, height
);
eventManager.setHitTestStrategy(strategy);
```

## Performance Characteristics

### Tree-Walk Strategy

- **Hit-test complexity**: O(n) where n = number of components
- **Memory**: Minimal (no additional buffers)
- **Availability**: Always available (pure Java)
- **Best for**: Simple UIs, small component counts (<100 components)

### Picking Buffer Strategy

- **Hit-test complexity**: O(1) per event
- **Memory**: Width × Height × 4 bytes (off-screen framebuffer)
- **Rebuild cost**: O(n) where n = number of visible components
- **Availability**: WebGL 2.0 required
- **Best for**: Complex UIs, deep hierarchies, high event rates (>1000 components)

### Performance Comparison

For a UI with 1000 components:

| Operation | Tree-Walk | Picking Buffer |
|-----------|-----------|----------------|
| Hit-test | ~1000 iterations | 1 pixel read + 1 map lookup |
| Layout change | Instant | Buffer invalidate (instant) |
| Next hit-test after change | ~1000 iterations | Rebuild (1000 rects) + 1 pixel read |

**Key insight**: Picking buffer amortizes rebuild cost across multiple hit-tests. For mouse-move events (typically >30/sec), this provides significant savings.

## Implementation Details

### Component Registry

`TComponent` maintains a static registry mapping IDs to component instances:

```java
private static final Map<Integer, TComponent> componentRegistry = new ConcurrentHashMap<>();
```

Components automatically register on construction and should be unregistered on destruction (not yet implemented).

### WebGL Integration

The picking buffer integrates with the existing WebGL rendering pipeline:

- Uses the same `WebGL2RenderingContext` as the main rendering backend
- Shares shader infrastructure (simplified shaders for ID rendering)
- Respects viewport transforms and clipping

### Limitations

1. **WebGL 2.0 Required**: Picking buffer only works with WebGL backend (not WASM or Software)
2. **Screen-Space Only**: Picking buffer matches canvas size (no sub-pixel precision)
3. **Component Limit**: Maximum 16,777,215 components (RGB encoding limit)
4. **Memory**: Requires VRAM for off-screen framebuffer (4 bytes per pixel)

## Future Enhancements

### Potential Optimizations

1. **Incremental Updates**: Instead of rebuilding entire buffer, only redraw changed regions
2. **Hierarchical Culling**: Skip invisible or fully-occluded component subtrees
3. **Dirty Region Tracking**: Only rebuild affected screen regions
4. **Texture Compression**: Use compressed texture formats to reduce memory

### Extended Hit-Testing

1. **Non-Rectangular Shapes**: Support arbitrary component shapes (already pixel-precise)
2. **Transparency**: Handle semi-transparent components with alpha testing
3. **Sub-Component Picking**: Encode sub-regions of components (e.g., button parts)

## Code Locations

- **Interface**: `awtea-classlib/java/awt/awtea/HitTestStrategy.java`
- **Tree-Walk**: `awtea-classlib/java/awt/awtea/TreeWalkHitTestStrategy.java`
- **Picking Buffer**: `awtea-graphics/gfx/webgl/PickingBufferHitTestStrategy.java`
- **Color Encoding**: `awtea-graphics/gfx/webgl/PickingColorEncoder.java`
- **Buffer Management**: `awtea-graphics/gfx/webgl/PickingBuffer.java`
- **Integration**: `awtea-classlib/java/awt/awtea/TEventManager.java`

## References

- **OpenGL Picking Tutorial**: https://www.opengl-tutorial.org/miscellaneous/clicking-on-objects/picking-with-an-opengl-hack/
- **WebGL Picking Examples**: https://webglfundamentals.org/webgl/lessons/webgl-picking.html
- **AWT Hit-Testing**: https://docs.oracle.com/javase/8/docs/api/java/awt/Container.html#getComponentAt-int-int-

## See Also

- [RENDERING_BACKENDS.md](RENDERING_BACKENDS.md) - WebGL backend architecture
- [SYSTEM_PROPERTIES.md](SYSTEM_PROPERTIES.md) - Configuration properties
- [COMPONENT_MAPPING.md](COMPONENT_MAPPING.md) - Component hierarchy design
