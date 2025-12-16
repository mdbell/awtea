# Applet Factory and Launcher System

The awtea applet factory and launcher system provides a low-code, compile-time safe way to launch Java applets in the browser without using reflection or dynamic class loading.

## Overview

The system consists of three main components:

1. **`AppletFactory`** - A functional interface for creating applet instances
2. **`AppletRegistry`** - A static registry for compile-time applet registration
3. **`AppletLauncher`** - An ES2015 module-compatible launcher with automatic discovery

## Quick Start

### Step 1: Create Your Applet

Create an applet class that extends `Applet` (or `TApplet`):

```java
package com.example.myapp;

import me.mdbell.awtea.classlib.java.applet.AppletRegistry;
import java.applet.Applet;
import java.awt.*;

public class MyApplet extends Applet {
    
    // Register the applet at class load time
    static {
        AppletRegistry.register("my-applet", MyApplet::new);
    }
    
    @Override
    public void init() {
        // Initialize your applet
        setSize(800, 600);
        
        Canvas canvas = new Canvas() {
            @Override
            public void paint(Graphics g) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                
                g.setColor(Color.BLACK);
                g.setFont(new Font("SansSerif", Font.BOLD, 32));
                g.drawString("Hello from MyApplet!", 50, 100);
            }
        };
        
        add(canvas);
    }
    
    @Override
    public void start() {
        // Start your applet (called after init)
        repaint();
    }
}
```

### Step 2: Create a Launcher Main Class

Create a main class that triggers the applet launcher:

```java
package com.example.myapp;

import me.mdbell.awtea.classlib.java.applet.AppletLauncher;

public class MyAppLauncher {
    
    public static void main(String[] args) {
        // Ensure your applet class is loaded (triggers static block)
        try {
            Class.forName("com.example.myapp.MyApplet");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load MyApplet", e);
        }
        
        // Launch applets using automatic discovery
        AppletLauncher.main(args);
    }
}
```

### Step 3: Update Your Build Configuration

Configure TeaVM to use your launcher as the main class:

```kotlin
// build.gradle.kts
teavm {
    js {
        mainClass = "com.example.myapp.MyAppLauncher"
        outputDir = layout.buildDirectory.dir("dist").get().asFile
        moduleType = JSModuleType.ES2015
        
        optimization = OptimizationLevel.NONE
        sourceMap = true
    }
}
```

### Step 4: Create Your HTML Page

Add a canvas with the `data-awtea-applet` attribute:

```html
<!DOCTYPE html>
<html>
<head>
    <title>My awtea App</title>
</head>
<body>
    <h1>My Applet Demo</h1>
    
    <!-- Canvas with data-awtea-applet attribute -->
    <canvas id="app-canvas" 
            data-awtea-applet="my-applet" 
            width="800" 
            height="600">
        Your browser does not support HTML5 canvas.
    </canvas>
    
    <!-- Load and run the applet -->
    <script type="module">
        import { main } from './js/my-app.js';
        main([]);
    </script>
</body>
</html>
```

### Step 5: Build and Run

```bash
./gradlew :your-project:build

# Serve the generated files
cd your-project/build/dist
python3 -m http.server 8000
```

Open `http://localhost:8000` in your browser!

## Usage Patterns

### Automatic Discovery (Recommended)

The launcher automatically finds all canvas elements with the `data-awtea-applet` attribute and launches the corresponding registered applets:

**HTML:**
```html
<canvas id="canvas1" data-awtea-applet="demo-app" width="800" height="600"></canvas>
<canvas id="canvas2" data-awtea-applet="another-app" width="400" height="300"></canvas>
```

**JavaScript (ES2015 module):**
```javascript
import { main } from './js/my-app.js';

// This discovers and launches all applets
main([]);
```

### Manual Launch from JavaScript

You can also manually launch applets from JavaScript:

```javascript
import { launchNamed } from './js/my-app.js';

// Launch a specific applet on a specific canvas
const success = launchNamed('my-canvas-id', 'my-applet-name');

if (success) {
    console.log('Applet launched successfully');
} else {
    console.error('Failed to launch applet');
}
```

### Multiple Applets in One Application

Register multiple applets in your launcher:

```java
public class MultiAppLauncher {
    
    public static void main(String[] args) {
        // Load all applet classes
        try {
            Class.forName("com.example.AppletOne");
            Class.forName("com.example.AppletTwo");
            Class.forName("com.example.AppletThree");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load applet classes", e);
        }
        
        // Launch all registered applets
        AppletLauncher.main(args);
    }
}
```

Each applet registers itself:

```java
public class AppletOne extends Applet {
    static { AppletRegistry.register("app-one", AppletOne::new); }
    // ... implementation
}

public class AppletTwo extends Applet {
    static { AppletRegistry.register("app-two", AppletTwo::new); }
    // ... implementation
}
```

Then use them in HTML:

```html
<canvas id="c1" data-awtea-applet="app-one" width="400" height="300"></canvas>
<canvas id="c2" data-awtea-applet="app-two" width="400" height="300"></canvas>
```

## API Reference

### `AppletFactory`

Functional interface for creating applet instances.

```java
@FunctionalInterface
public interface AppletFactory {
    TApplet createApplet();
}
```

**Usage:**
```java
AppletFactory factory = MyApplet::new;
TApplet applet = factory.createApplet();
```

### `AppletRegistry`

Static registry for compile-time applet registration.

#### Methods

- **`register(String name, AppletFactory factory)`**
  - Registers an applet factory with a unique name
  - Throws `IllegalArgumentException` if name is null/empty
  - Throws `IllegalStateException` if name is already registered

- **`createApplet(String name)`**
  - Creates a new instance of the registered applet
  - Throws `IllegalArgumentException` if applet is not registered

- **`isRegistered(String name)`**
  - Checks if an applet with the given name is registered
  - Returns `true` if registered, `false` otherwise

- **`getRegisteredNames()`**
  - Returns an unmodifiable set of all registered applet names

- **`clear()`**
  - Clears all registered applets (primarily for testing)

**Example:**
```java
// Registration
AppletRegistry.register("my-app", MyApplet::new);

// Check registration
if (AppletRegistry.isRegistered("my-app")) {
    TApplet applet = AppletRegistry.createApplet("my-app");
}

// List all registered applets
Set<String> names = AppletRegistry.getRegisteredNames();
System.out.println("Available applets: " + names);
```

### `AppletLauncher`

Launcher with ES2015 module exports for browser integration.

#### Methods

- **`main(String[] args)`** - `@JSExport`
  - Automatically discovers and launches applets on canvases with `data-awtea-applet`
  - Scans the DOM for matching canvases
  - Logs discovery and launch results

- **`launchNamed(String canvasId, String appletName)`** - `@JSExport`
  - Manually launches a specific applet on a specific canvas
  - Returns `true` if successful, `false` otherwise

**JavaScript Usage:**
```javascript
import { main, launchNamed } from './js/my-app.js';

// Automatic discovery
main([]);

// Manual launch
const success = launchNamed('canvas-id', 'applet-name');
```

## Canvas Configuration

### Canvas Attributes

The launcher recognizes these canvas attributes:

- **`id`** (required) - Unique identifier for the canvas
- **`data-awtea-applet`** (required) - Name of the registered applet to launch
- **`width`** (optional) - Canvas width in pixels (default: 800)
- **`height`** (optional) - Canvas height in pixels (default: 600)

**Example:**
```html
<canvas id="my-canvas" 
        data-awtea-applet="demo" 
        width="1024" 
        height="768">
</canvas>
```

### Default Dimensions

If `width` or `height` are not specified or are zero, the launcher uses defaults:
- Default width: 800px
- Default height: 600px

## Error Handling

### Common Errors

#### "No applet registered with name: X"

**Problem:** The applet name in HTML doesn't match a registered applet.

**Solution:** 
- Check the `data-awtea-applet` value matches the registered name
- Ensure the applet class is loaded in your launcher's `main()`
- Verify the static block registration code runs

#### "Canvas element not found: X"

**Problem:** The canvas ID doesn't exist in the DOM.

**Solution:** Verify the `id` attribute matches the value passed to `launchNamed()`

#### "Element is not a canvas: X"

**Problem:** The element with the given ID is not a canvas element.

**Solution:** Ensure you're using `<canvas>` elements, not `<div>` or other tags

### Logging

Enable debug logging to troubleshoot launcher issues:

```java
import me.mdbell.awtea.util.logging.LogLevel;
import me.mdbell.awtea.util.logging.LoggerFactory;

public class MyLauncher {
    public static void main(String[] args) {
        // Enable debug logging
        LoggerFactory.setGlobalLevel(LogLevel.DEBUG);
        
        // ... rest of launcher code
    }
}
```

The launcher logs:
- Discovery of canvases with `data-awtea-applet`
- Applet launch attempts (success/failure)
- Available registered applets
- Dimension defaults applied

## Best Practices

### 1. Use Descriptive Applet Names

Use lowercase-with-dashes naming convention:

```java
// Good
AppletRegistry.register("drawing-canvas", DrawingCanvas::new);
AppletRegistry.register("chart-viewer", ChartViewer::new);

// Avoid
AppletRegistry.register("DC", DrawingCanvas::new);
AppletRegistry.register("applet1", ChartViewer::new);
```

### 2. Register in Static Blocks

Register applets in static initialization blocks to ensure they're available before `main()` runs:

```java
public class MyApplet extends Applet {
    static {
        AppletRegistry.register("my-applet", MyApplet::new);
    }
    // ... implementation
}
```

### 3. Explicit Class Loading

Always explicitly load applet classes in your launcher to ensure static blocks run:

```java
public static void main(String[] args) {
    try {
        Class.forName("com.example.MyApplet");
    } catch (ClassNotFoundException e) {
        throw new RuntimeException("Failed to load applet", e);
    }
    
    AppletLauncher.main(args);
}
```

### 4. Set Canvas Dimensions

Always specify canvas dimensions explicitly in HTML:

```html
<!-- Good -->
<canvas id="app" data-awtea-applet="my-app" width="800" height="600"></canvas>

<!-- Works but uses defaults -->
<canvas id="app" data-awtea-applet="my-app"></canvas>
```

### 5. One Applet per Canvas

Each canvas should have exactly one applet. If you need multiple applets, use multiple canvases:

```html
<canvas id="canvas1" data-awtea-applet="app1" width="400" height="300"></canvas>
<canvas id="canvas2" data-awtea-applet="app2" width="400" height="300"></canvas>
```

## Migration Guide

### Migrating from Direct `main()` Pattern

**Old Pattern (Frame-based):**
```java
public class OldApp {
    public static void main(String[] args) {
        Frame frame = new Frame();
        frame.setTitle("My App");
        frame.setSize(800, 600);
        // ... add components
        frame.setVisible(true);
    }
}
```

**New Pattern (Applet-based):**
```java
public class NewApp extends Applet {
    static {
        AppletRegistry.register("new-app", NewApp::new);
    }
    
    @Override
    public void init() {
        setSize(800, 600);
        // ... add components (no Frame needed)
    }
    
    @Override
    public void start() {
        // Start logic here
    }
}

// Launcher class
public class NewAppLauncher {
    public static void main(String[] args) {
        try {
            Class.forName("com.example.NewApp");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        AppletLauncher.main(args);
    }
}
```

**HTML:**
```html
<canvas id="app" data-awtea-applet="new-app" width="800" height="600"></canvas>
<script type="module">
    import { main } from './js/new-app.js';
    main([]);
</script>
```

### Key Changes

1. **Extend `Applet`** instead of creating a `Frame`
2. **Use `init()` and `start()`** lifecycle methods instead of `main()`
3. **Register in static block** with a unique name
4. **Create a launcher class** that loads applet classes and calls `AppletLauncher.main()`
5. **Update HTML** to use `<canvas>` with `data-awtea-applet` attribute
6. **Update build config** to use the launcher as the main class

## Examples

See the following examples in the `examples/` directory:

- **`hello-world`** - Minimal applet with text rendering
- **`gui-demo`** - Multi-component applet with interactive features

## Troubleshooting

### Applet Not Launching

1. **Check browser console** for JavaScript errors
2. **Enable debug logging** to see discovery and launch messages
3. **Verify registration** using `AppletRegistry.getRegisteredNames()`
4. **Check canvas HTML** - ensure `id` and `data-awtea-applet` are set
5. **Verify build output** - ensure TeaVM generated JS with ES2015 modules

### TeaVM Compilation Errors

If you see errors about missing classes:

1. **Add dependencies** - ensure `awtea-classlib` is in your dependencies
2. **Check imports** - use `java.applet.Applet` (aliased to `TApplet`)
3. **Verify module type** - set `moduleType = JSModuleType.ES2015` in build config

### Canvas Not Found

If you see "Canvas element not found" errors:

1. **Check DOM timing** - ensure canvas exists before calling `main()`
2. **Verify ID** - canvas ID must match what's in HTML
3. **Check querySelectorAll** - the launcher uses `canvas[data-awtea-applet]`

## See Also

- [Component Mapping](COMPONENT_MAPPING.md) - AWT component implementation details
- [System Properties](SYSTEM_PROPERTIES.md) - Runtime configuration options
- [Logging](LOGGING.md) - Logging framework documentation
- [Examples README](../examples/README.md) - Example applications
