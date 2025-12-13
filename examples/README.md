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

## Building and Running Examples

Each example is a self-contained Gradle project that can be built independently.

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

Navigate to the example directory and run:

```bash
cd hello-world
./gradlew build
```

This will:
1. Compile the Java source code
2. Run TeaVM to transpile Java bytecode to JavaScript/WebAssembly
3. Generate HTML files and assets in `build/dist/`

### Running an Example

After building, open the generated HTML file in a web browser:

```bash
# Open in default browser (Linux/macOS)
open build/dist/index.html

# Or on Linux with xdg-open
xdg-open build/dist/index.html

# Or on Windows
start build/dist/index.html
```

Alternatively, you can serve the files with a local web server:

```bash
cd build/dist
python3 -m http.server 8000
# Then open http://localhost:8000 in your browser
```

## Project Structure

Each example follows this structure:

```
example-name/
├── build.gradle.kts          # Build configuration with TeaVM plugin
├── src/
│   └── main/
│       ├── java/             # Java source code
│       └── webapp/           # HTML template and resources
│           └── index.html
└── README.md                 # Example-specific documentation
```

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
