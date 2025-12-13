# Hello World Example

This is the simplest possible awtea application - a minimal AWT program that displays "Hello, awtea!" in a window.

## Status

⚠️ **Note**: This example is currently experiencing TeaVM compilation issues that are being investigated in the awtea project. The code demonstrates correct API usage and will work once runtime compatibility issues are resolved.

## What This Example Demonstrates

- Creating a basic `TFrame` window
- Using `TCanvas` for custom drawing
- Rendering text with `Graphics.drawString()`
- Setting up the basic structure of an awtea application

## Building

```bash
./gradlew build
```

## Running

After building, open `build/dist/index.html` in your web browser:

```bash
# Linux/macOS
xdg-open build/dist/index.html

# Or serve with Python
cd build/dist
python3 -m http.server 8000
# Then open http://localhost:8000
```

## Code Overview

The example consists of a single class `HelloWorld` that:
1. Creates a `TFrame` (top-level window)
2. Adds a `TCanvas` for custom drawing
3. Draws "Hello, awtea!" text in the center of the canvas
4. Displays the window

## Next Steps

After running this example, check out the `gui-demo` example for more advanced features like:
- Event handling
- Multiple components
- Graphics primitives
- User interaction
