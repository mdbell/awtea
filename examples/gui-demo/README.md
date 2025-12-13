# GUI Demo Example

A comprehensive awtea example demonstrating multiple AWT components, graphics primitives, event handling, and user interaction.

## Status

⚠️ **Note**: This example is currently experiencing TeaVM compilation issues that are being investigated in the awtea project. The code demonstrates correct API usage and will work once runtime compatibility issues are resolved.

## What This Example Demonstrates

- Creating a complex GUI with multiple panels
- Using `TCanvas` for custom graphics
- Drawing primitives (rectangles, ovals, lines, arcs)
- Event handling (mouse clicks and movement)
- Color manipulation
- Font rendering
- Layout management
- Interactive components

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

## Features

The demo includes:

1. **Drawing Canvas**: A canvas where you can click to draw colored circles
2. **Graphics Primitives**: Demonstrations of various shape drawing methods
3. **Text Rendering**: Different fonts, sizes, and styles
4. **Color Palette**: Shows various colors and gradients
5. **Mouse Tracking**: Real-time display of mouse coordinates

## Interaction

- **Click** on the main canvas to draw a circle at that location
- **Move the mouse** over the canvas to see coordinates update
- Observe the various graphics primitives and text rendering

## Code Structure

The example consists of:
- `GuiDemo.java`: Main application class with frame setup
- `DrawingCanvas.java`: Interactive canvas with mouse event handling
- `GraphicsDemoPanel.java`: Panel demonstrating various graphics operations

## Next Steps

Use this example as a reference for building your own interactive awtea applications!
