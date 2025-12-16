# Applet Factory and Launcher Implementation Summary

This document summarizes the implementation of the low-code applet factory and ES2015 module launcher system for awtea.

## What Was Implemented

### Core Infrastructure (`awtea-classlib`)

#### 1. AppletFactory Interface
- **Location**: `awtea-classlib/src/main/java/me/mdbell/awtea/classlib/java/applet/AppletFactory.java`
- **Purpose**: Functional interface for creating applet instances without reflection
- **Signature**: `Applet createApplet()`
- Uses standard `java.applet.Applet` for compatibility with both JDK compilation and TeaVM runtime

#### 2. AppletRegistry Class  
- **Location**: `awtea-classlib/src/main/java/me/mdbell/awtea/classlib/java/applet/AppletRegistry.java`
- **Purpose**: Static registry for compile-time applet registration
- **Key Methods**:
  - `register(String name, AppletFactory factory)` - Register an applet with a unique name
  - `createApplet(String name)` - Instantiate a registered applet by name
  - `isRegistered(String name)` - Check if an applet is registered
  - `getRegisteredNames()` - Get all registered applet names
- Thread-safe and prevents duplicate registration

#### 3. AppletLauncher Class
- **Location**: `awtea-classlib/src/main/java/me/mdbell/awtea/classlib/java/applet/AppletLauncher.java`
- **Purpose**: ES2015 module-compatible launcher with automatic DOM discovery
- **Key Features**:
  - `@JSExport main(String[] args)` - Auto-discovers canvases with `data-awtea-applet` attribute
  - `@JSExport launchNamed(String canvasId, String appletName)` - Manual applet launch API
  - Automatic canvas dimension defaults (800x600 if not specified)
  - Comprehensive logging for debugging
- **Process**: Scans DOM → finds canvases → creates applets → sets up stubs → calls init() → calls start()

#### 4. TeaAppletStubAdapter Class
- **Location**: `awtea-classlib/src/main/java/me/mdbell/awtea/classlib/java/applet/TeaAppletStubAdapter.java`
- **Purpose**: Bridges `TeaAppletStub` (awtea-core) to `TAppletStub` (awtea-classlib)
- **Rationale**: Avoids circular dependency between awtea-core and awtea-classlib

#### 5. TeaAppletStub Enhancement
- **Location**: `awtea-core/src/main/java/me/mdbell/awtea/impl/TeaAppletStub.java`
- **Changes**:
  - Added `HTMLCanvasElement canvas` field with getter
  - Added constructor overload that accepts canvas
  - Enabled canvas data-attribute parameter lookup
- **Purpose**: Allows applet stub to access canvas and its data attributes

## Example Updates

### Hello World Example
- **New Files**:
  - `HelloWorldApplet.java` - Applet extending `java.applet.Applet`
  - `HelloWorldLauncher.java` - Main class that uses `AppletLauncher`
- **Modified Files**:
  - `build.gradle.kts` - Changed `mainClass` to `HelloWorldLauncher`
  - `index.html` - Added `<canvas data-awtea-applet="hello-world">` with dimensions
- **Registration**: Static block calls `AppletRegistry.register("hello-world", HelloWorldApplet::new)`

### GUI Demo Example
- **New Files**:
  - `GuiDemoApplet.java` - Applet extending `java.applet.Applet`
  - `GuiDemoLauncher.java` - Main class that uses `AppletLauncher`
- **Modified Files**:
  - `build.gradle.kts` - Changed `mainClass` to `GuiDemoLauncher`
  - `index.html` - Added `<canvas data-awtea-applet="gui-demo">` with dimensions
- **Registration**: Static block calls `AppletRegistry.register("gui-demo", GuiDemoApplet::new)`

## Usage Pattern

### 1. Define Your Applet
```java
public class MyApplet extends java.applet.Applet {
    static {
        AppletRegistry.register("my-applet", MyApplet::new);
    }
    
    @Override
    public void init() {
        // Initialize UI
    }
    
    @Override
    public void start() {
        // Start animation/logic
    }
}
```

### 2. Create a Launcher
```java
public class MyLauncher {
    public static void main(String[] args) {
        // Configure system properties
        LoggerFactory.setGlobalLevel(LogLevel.DEBUG);
        
        // Applets are automatically registered via their static blocks
        // No need for explicit Class.forName() calls
        
        // Launch applets
        AppletLauncher.main(args);
    }
}
```

### 3. Update build.gradle.kts
```kotlin
teavm {
    js {
        mainClass = "com.example.MyLauncher"
        outputDir = layout.buildDirectory.dir("dist").get().asFile
        moduleType = JSModuleType.ES2015
    }
}
```

### 4. Add HTML Canvas
```html
<canvas id="app-canvas" 
        data-awtea-applet="my-applet" 
        width="800" 
        height="600">
</canvas>

<script type="module">
    import { main } from './js/my-app.js';
    main([]);
</script>
```

## Key Design Decisions

### 1. Use Standard `java.applet.Applet` Not `TApplet`
**Rationale**: During standard Java compilation, using `java.awt.*` imports refers to JDK classes. Using awtea's `T`-prefixed classes directly causes type mismatches. By using standard `java.applet.Applet`, the code compiles with the JDK, and TeaVM's aliasing handles the runtime mapping to awtea implementations.

### 2. Static Registration in Static Blocks
**Rationale**: Ensures applets are registered before `main()` runs. The launcher explicitly loads applet classes via `Class.forName()` to trigger these static blocks.

### 3. ES2015 Module Exports with `@JSExport`
**Rationale**: Modern JavaScript ecosystem uses ES2015 modules. The `@JSExport` annotation makes launcher methods available as module exports, enabling clean import/export syntax.

### 4. Canvas-Based Architecture
**Rationale**: HTML5 canvases are the rendering surface for AWT content. The `data-awtea-applet` attribute provides declarative binding between canvas elements and applet instances.

### 5. No Reflection
**Rationale**: TeaVM does not support Java reflection. The factory pattern with static registration provides compile-time safety while avoiding reflection.

## Build Verification

Both examples build successfully:
```bash
$ ./gradlew :examples:hello-world:build
BUILD SUCCESSFUL

$ ./gradlew :examples:gui-demo:build  
BUILD SUCCESSFUL
```

Generated artifacts:
- `build/dist/index.html` - Updated HTML with canvas and data attributes
- `build/dist/js/hello-world.js` - ES2015 module with exported functions
- `build/dist/js/hello-world.js.map` - Source maps for debugging

## Documentation

- **Comprehensive Guide**: `docs/APPLET_LAUNCHER.md`
  - API reference
  - Usage patterns
  - Migration guide
  - Troubleshooting
  - Best practices
  
- **Updated Examples README**: `examples/README.md`
  - References new launcher pattern
  - Links to detailed documentation

## Known Limitations

1. **Runtime Testing**: Build succeeds, but browser runtime testing is needed to verify:
   - Auto-discovery actually finds and launches applets
   - Manual `launchNamed()` API works correctly
   - Applet lifecycle (init/start) executes properly
   - Canvas rendering works as expected

2. **Type System Complexity**: The dual compilation/runtime model (JDK for compilation, TeaVM aliases at runtime) requires careful attention to imports. Standard `java.awt.*` imports work, but mixing T-prefixed and standard classes can cause issues.

3. **TeaVM Compilation Issues**: The examples README notes that TeaVM compilation issues were previously being investigated. The new launcher approach builds successfully, but full runtime behavior needs verification.

## Future Enhancements

Potential improvements:
- Add `stop()` and `destroy()` lifecycle support
- Support multiple applets per page
- Add applet parameter passing from HTML data attributes
- Provide TypeScript definitions for ES2015 exports
- Add browser-based test suite for runtime validation
- Support dynamic applet loading/unloading

## Testing Checklist

- [x] Applet factory interface compiles
- [x] Applet registry compiles and has basic validation
- [x] Applet launcher compiles with ES2015 exports
- [x] Hello-world example builds successfully
- [x] GUI-demo example builds successfully
- [x] HTML has correct canvas elements with data attributes
- [x] JS files are generated as ES2015 modules
- [ ] Auto-discovery works in browser runtime
- [ ] Manual launch API works in browser runtime
- [ ] Applet init() and start() are called correctly
- [ ] Canvas rendering works as expected
- [ ] Multiple canvases on same page work
- [ ] Error handling and logging work correctly

## Conclusion

The applet factory and launcher system successfully implements a low-code pattern for launching Java applets in the browser without reflection. The implementation:

✅ Provides compile-time type safety  
✅ Uses standard Java applet API  
✅ Exports ES2015 module functions  
✅ Supports both automatic discovery and manual launch  
✅ Includes comprehensive documentation  
✅ Updates both example applications  
✅ Builds successfully with Gradle + TeaVM  

The system is ready for runtime testing in a browser environment to validate the full end-to-end workflow.
