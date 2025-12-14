# WASM Logging Support

## Overview

The WASM rasterizer module includes integrated logging support that routes C-side log messages through awtea's existing Java logging infrastructure via callback mechanism. This enables better debugging and diagnostics of rendering operations, with logs appearing immediately as they are emitted.

## Features

- **Multiple log levels**: DEBUG, INFO, WARN, ERROR
- **Printf-style formatting**: Full support for format strings and arguments in C
- **Java integration**: Logs appear in Java console via `LoggerFactory.getLogger("wasm.rasterizer")`
- **Immediate delivery**: Callback-based approach delivers logs instantly (no polling)
- **Compile-time control**: Can be disabled via `ENABLE_WASM_LOGGING=0` flag
- **Zero overhead**: When disabled, logging becomes no-op macros with no performance impact

## Architecture

The implementation uses a **callback-based** approach (not polling):

1. **C-side**: Log messages are formatted using `vsnprintf` and immediately passed to JavaScript via `wasm_log_callback`
2. **JavaScript bridge**: The callback reads the message from WASM memory and calls Java's `logFromWasm` method
3. **Java-side**: Messages are routed to the appropriate log level via `LoggerFactory.getLogger("wasm.rasterizer")`
4. **No buffer**: No shared buffer or polling—logs appear immediately when emitted

## Disabling Logging

To disable logging in production builds, modify `build.gradle.kts`:

```kotlin
exec {
    commandLine(
        "docker", "run", "--rm",
        "-v", "${projectDir}:/src",
        "-w", "/src",
        "emscripten/emsdk",
        "emcc",
        "-Isrc/main/native",
        "-DENABLE_WASM_LOGGING=0",  // Add this line
        * sourceList.toTypedArray(),
        "-O2",
        // ... rest of flags
    )
}
```

When disabled:
- All log functions become no-op macros
- Zero performance overhead
- WASM module size is not increased
- JavaScript callback is never invoked

## Testing

To verify logging works:

1. Set log level to DEBUG:
   ```java
   LoggerFactory.setGlobalLevel(LogLevel.DEBUG);
   ```

2. Use the WASM backend:
   ```java
   System.setProperty("me.mdbell.awtea.gfx.backend", "wasm");
   ```

3. Perform rendering operations that trigger log statements

4. Check console output for messages prefixed with `[wasm.rasterizer]`

## Example: HelloWorld with WASM Logging

```java
package me.mdbell.awtea.examples;

import me.mdbell.awtea.util.logging.LogLevel;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.awt.*;

public class WasmLoggingDemo {
    public static void main(String[] args) {
        // Enable debug logging to see WASM messages
        LoggerFactory.setGlobalLevel(LogLevel.DEBUG);
        
        // Use WASM backend (default is Java backend)
        System.setProperty("me.mdbell.awtea.gfx.backend", "wasm");
        
        Frame frame = new Frame();
        frame.setTitle("WASM Logging Demo");
        frame.setSize(400, 300);
        
        Canvas canvas = new Canvas() {
            @Override
            public void paint(Graphics g) {
                // These operations will trigger WASM logs
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                
                g.setColor(Color.BLUE);
                g.fillRect(50, 50, 100, 100);
                
                g.setColor(Color.RED);
                g.drawRect(200, 50, 100, 100);
            }
        };
        
        frame.add(canvas);
        frame.setVisible(true);
        
        // Check console for messages like:
        // [DEBUG] [wasm.rasterizer] reset_surface: id=1024, layer=0, size=400x300, format=0
        // [INFO]  [wasm.rasterizer] Created surface 1024: 400x300, 480000 bytes
    }
}
```

## Troubleshooting

### Logs not appearing

1. **Check log level**: Ensure global log level is set to appropriate level
   ```java
   LoggerFactory.setGlobalLevel(LogLevel.DEBUG);
   ```

2. **Verify backend**: Confirm WASM backend is being used
   ```java
   System.setProperty("me.mdbell.awtea.gfx.backend", "wasm");
   ```

3. **Check build**: Ensure WASM module was built with logging enabled (default)

4. **Force rebuild**: Clean and rebuild WASM module
   ```bash
   ./gradlew :awtea-graphics:clean :awtea-graphics:buildAwtRasterWasm
   ```

5. **Check JavaScript console**: Errors in the callback will appear in browser console

### Messages truncated

If you see truncated messages:
- Increase `LOG_MESSAGE_MAX_SIZE` in `awt_log.h` (default: 512)
- Rebuild WASM module

### Performance issues

If logging impacts performance:
- Set log level to INFO or WARN in production
- Disable debug logging: `LoggerFactory.setGlobalLevel(LogLevel.INFO)`
- For maximum performance, rebuild with `ENABLE_WASM_LOGGING=0`

## Related Issues

- [#57](https://github.com/mdbell/awtea/issues/57) - Test harness
- [#56](https://github.com/mdbell/awtea/issues/56) - Safety practices
