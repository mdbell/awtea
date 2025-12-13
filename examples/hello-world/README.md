# Hello World Example

This is the simplest possible awtea application - a minimal AWT program that displays "Hello, awtea!" in a window.

## Status

⚠️ **Note**: This example is currently experiencing TeaVM compilation issues that are being investigated in the awtea project. The code demonstrates correct API usage and will work once runtime compatibility issues are resolved.

## What This Example Demonstrates

- Creating a basic `Frame` window
- Using `Canvas` for custom drawing
- Rendering text with `Graphics.drawString()`
- Setting up the basic structure of an awtea application

## Building

From the root awtea directory:

```bash
./gradlew :examples:hello-world:build
```

## Running

After building, open `examples/hello-world/build/dist/index.html` in your web browser:

```bash
# From root directory (Linux/macOS)
xdg-open examples/hello-world/build/dist/index.html

# Or serve with Python
cd examples/hello-world/build/dist
python3 -m http.server 8000
# Then open http://localhost:8000
```

## Code Overview

The example consists of a single class `HelloWorld` that:
1. Creates a `Frame` (top-level window)
2. Adds a `Canvas` for custom drawing
3. Draws "Hello, awtea!" text and a border
4. Displays the window

Uses standard `java.awt.*` classes which TeaVM automatically aliases to awtea's implementations.

## Next Steps

After running this example, check out the `gui-demo` example for more advanced features like:
- Event handling
- Multiple components
- Graphics primitives
- User interaction
