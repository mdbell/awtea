# awtea Examples

This directory contains example applications demonstrating how to use awtea to run Java AWT applications in the browser using TeaVM.

## Available Examples

### 1. Hello World (`hello-world/`)
A minimal AWT applet that displays "Hello, awtea!" in a canvas. This demonstrates the new applet factory and launcher pattern.

**Features demonstrated:**
- Applet lifecycle (init/start)
- Applet factory registration pattern
- Automatic applet discovery from HTML
- Simple graphics rendering with drawString
- ES2015 module integration

### 2. GUI Demo (`gui-demo/`)
A more comprehensive example showcasing multiple AWT components, layouts, graphics primitives, and event handling using the applet launcher.

**Features demonstrated:**
- Multiple component types (containers, panels, canvas)
- Event handling (mouse click and movement events)
- Graphics primitives (lines, rectangles, arcs, polygons, colors)
- Text rendering with different fonts
- User interaction
- Complex applet structure

## Building and Running Examples

The examples are integrated as subprojects of the main awtea build.

### Prerequisites
- Java 11 or newer
- Gradle (or use the included wrapper)

### Current Status

⚠️ **Note**: The examples are currently experiencing TeaVM compilation issues that are being investigated. The example code demonstrates the correct API usage and structure for awtea applications. Once the underlying runtime compatibility issues are resolved, these examples will build and run successfully.

The examples serve as:
- Reference implementations showing proper awtea API usage
- Templates for your own applications
- Documentation of component interactions and patterns

### Building an Example

From the root awtea directory, you can build individual examples:

```bash
# Build hello-world example
./gradlew :examples:hello-world:build

# Build gui-demo example
./gradlew :examples:gui-demo:build

# Build all examples
./gradlew :examples:hello-world:build :examples:gui-demo:build
```

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
2. **Use the Applet Pattern**: Extend `Applet` and use `init()` and `start()` lifecycle methods
3. **Register Your Applet**: Use `AppletRegistry.register("name", YourApplet::new)` in a static block
4. **Create a Launcher**: Make a launcher class that loads your applet and calls `AppletLauncher.main()`
5. **Use Canvas Elements**: Add `<canvas>` elements with `data-awtea-applet="name"` in your HTML
6. **Lightweight Components**: Most UI components should be lightweight (no peers)
7. **Canvas for Custom Drawing**: Use `Canvas` for custom graphics
8. **Check Documentation**: Review the [Applet Launcher Guide](../docs/APPLET_LAUNCHER.md) for detailed usage

For a complete guide on the applet factory and launcher system, see [APPLET_LAUNCHER.md](../docs/APPLET_LAUNCHER.md).

## Contributing

Found a bug or want to add more examples? Please file an issue or submit a pull request!
