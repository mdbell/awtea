# awtea Examples

This directory contains example applications demonstrating how to use awtea to run Java AWT applications in the browser using TeaVM.

## Available Examples

### 1. Hello World (`hello-world/`)
A minimal AWT applet that displays "Hello, awtea!" in a window. This is the simplest possible example to get started.

**Features demonstrated:**
- Basic Frame usage
- Simple graphics rendering with drawString
- Setting up a basic AWT application structure

### 2. GUI Demo (`gui-demo/`)
A more comprehensive example showcasing multiple AWT components, layouts, graphics primitives, and event handling.

**Features demonstrated:**
- Multiple component types (containers, panels, canvas)
- Event handling (mouse click and movement events)
- Graphics primitives (lines, rectangles, arcs, polygons, colors)
- Text rendering with different fonts
- User interaction

### 3. Animation Demo (`animation-demo/`)
An advanced example demonstrating real-time animation, physics simulation, and interactive controls.

**Features demonstrated:**
- Frame-based animation loop (~60 FPS)
- Physics simulation with gravity and collision detection
- FPS monitoring and performance tracking
- Double buffering for smooth rendering
- Mouse interaction (add balls with clicks)
- Keyboard controls (pause, reset, visual effects)
- Delta-time based physics for frame-independent movement
- Motion trails and velocity vector visualization

### 4. Layout Demo (`layout-demo/`)
A comprehensive demonstration of AWT layout managers, showing how components can be automatically positioned and sized.

**Features demonstrated:**
- BorderLayout (5-region layout: NORTH, SOUTH, EAST, WEST, CENTER)
- FlowLayout (horizontal flow with automatic wrapping)
- GridLayout (equal-sized grid cells)
- CardLayout (stack of panels showing one at a time)
- Automatic component sizing with preferred dimensions
- Layout validation and component hierarchy

## Building and Running Examples

The examples are integrated as subprojects of the main awtea build.

### Prerequisites
- Java 11 or newer
- Gradle (or use the included wrapper)

### Building Examples

**Important:** When building examples, use the `--no-daemon` flag to avoid TeaVM plugin corruption:

```bash
# Build hello-world example
./gradlew --no-daemon :examples:hello-world:build

# Build gui-demo example
./gradlew --no-daemon :examples:gui-demo:build

# Build animation-demo example
./gradlew --no-daemon :examples:animation-demo:build

# Build layout-demo example
./gradlew --no-daemon :examples:layout-demo:build
```

The Gradle daemon can cause TeaVM plugin state corruption, leading to build failures. Using `--no-daemon` ensures each build starts with a clean state. This requirement only applies to example builds that use TeaVM JavaScript generation.

This will:
1. Compile the Java source code
2. Run TeaVM to transpile Java bytecode to JavaScript/WebAssembly
3. Generate HTML files and assets in `examples/*/build/dist/`

### Running an Example

After building, open the generated HTML file in a web browser:

```bash
# Open hello-world in default browser (Linux/macOS)
open examples/hello-world/build/dist/index.html

# Or on Linux with xdg-open
xdg-open examples/hello-world/build/dist/index.html

# Or on Windows
start examples\hello-world\build\dist\index.html
```

Alternatively, you can serve the files with a local web server:

```bash
cd examples/hello-world/build/dist
python3 -m http.server 8000
# Then open http://localhost:8000 in your browser
```

## Project Structure

The examples are integrated as subprojects under the main awtea build. Each example follows this structure:

```
examples/example-name/
├── build.gradle.kts          # Build configuration with TeaVM plugin
├── src/
│   └── main/
│       ├── java/             # Java source code
│       └── webapp/           # HTML template and resources
│           └── index.html
└── README.md                 # Example-specific documentation
```

Examples use `project(":awtea-classlib")` dependencies to reference the parent awtea modules.

## Known Limitations

- Some AWT features are not yet implemented in awtea (see main project documentation)
- Browser compatibility may vary (modern browsers with WebAssembly support recommended)
- Some heavyweight components (Dialog, Window) are not yet available
- Text input components (TextField, TextArea) have limited implementation

## Tips for Creating Your Own Applications

1. **Start Simple**: Begin with the hello-world example and gradually add features
2. **Use Standard AWT Classes**: Use standard `java.awt.*` classes (Frame, Canvas, Graphics, etc.) - TeaVM will automatically alias them to awtea's implementations
3. **Lightweight Components**: Most UI components should be lightweight (no peers)
4. **Canvas for Custom Drawing**: Use `Canvas` for custom graphics
5. **Check Documentation**: Review the main project docs for component mapping and available features

## Contributing

Found a bug or want to add more examples? Please file an issue or submit a pull request!
