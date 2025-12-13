# GUI Demo Example

A comprehensive awtea example demonstrating multiple AWT components, graphics primitives, event handling, and user interaction.

## Status

⚠️ **Note**: This example is currently experiencing TeaVM compilation issues that are being investigated in the awtea project. The code demonstrates correct API usage and will work once runtime compatibility issues are resolved.

## What This Example Demonstrates

- Creating a complex GUI with multiple containers
- Using `Canvas` for custom graphics
- Drawing primitives (rectangles, lines, arcs, polygons)
- Event handling (mouse clicks and movement)
- Color manipulation
- Font rendering
- Component positioning with setBounds
- Interactive components

## Building

From the root awtea directory:

```bash
./gradlew :examples:gui-demo:build
```

## Running

After building, open `examples/gui-demo/build/dist/index.html` in your web browser:

```bash
# From root directory (Linux/macOS)
xdg-open examples/gui-demo/build/dist/index.html

# Or serve with Python
cd examples/gui-demo/build/dist
python3 -m http.server 8000
# Then open http://localhost:8000
```

## Features

The demo includes:

1. **Drawing Canvas**: A canvas where you can click to draw colored squares
2. **Graphics Primitives**: Demonstrations of various shape drawing methods
3. **Text Rendering**: Different fonts and styles
4. **Color Palette**: Shows various colors
5. **Mouse Tracking**: Real-time display of mouse coordinates

## Interaction

- **Click** on the main canvas to draw a square at that location
- **Move the mouse** over the canvas to see coordinates update
- Observe the various graphics primitives and text rendering

## Code Structure

The example consists of:
- `GuiDemo.java`: Main application class with frame setup
- `DrawingCanvas.java`: Interactive canvas with mouse event handling
- `GraphicsDemoPanel.java`: Panel demonstrating various graphics operations

Uses standard `java.awt.*` classes which TeaVM automatically aliases to awtea's implementations.

## Next Steps

Use this example as a reference for building your own interactive awtea applications!
