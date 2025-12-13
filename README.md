# awtea

**Bringing Java back to the browser**

awtea is a project that implements a Java AWT (Abstract Window Toolkit) runtime using [TeaVM](https://teavm.org/) and WebAssembly, enabling legacy and modern Java graphical applications to run natively in the browser.

## Features
- AWT event and component emulation
- Multi-backend rendering: WebGL, WASM, and Java software renderer
- Bytecode instrumentation for performance and extensibility
- Zero-copy pixel buffers
- Comprehensive debugging and monitoring tools

## Getting Started

### Prerequisites
- Java 11 or newer
- Gradle

### Building
```sh
./gradlew build
```

### Running
Instructions for running in the browser (via TeaVM) coming soon!

## Project Structure

This project is organized into multiple modules for better maintainability and modularity:

### Core Modules

- **awtea-core**: Core runtime and basic implementations
  - Contains helper classes and core implementations
  - Includes virtual file system support (IndexedDB)
  
- **awtea-classlib**: Java AWT class library reimplementations
  - Reimplementations of `java.awt.*`, `javax.swing.*`, and related packages
  - Browser-compatible versions of AWT classes
  - Canvas components:
    - `TCanvas`: Lightweight canvas for embedding in AWT containers
    - `THeavyCanvas`: Heavyweight hardware-backed canvas for top-level windows

### Specialized Modules

- **awtea-graphics**: Graphics and rendering system
  - Graphics rendering (software, WebGL, WASM)
  - Font handling and TrueType font support
  - OpenGL bindings
  - Native code for rasterization

- **awtea-sound**: Audio subsystem
  - MIDI and PCM audio support
  - Browser audio integration

- **awtea-input**: Input handling
  - Keyboard and mouse event mapping
  - Browser input abstraction

- **awtea-net**: Networking stack
  - Socket implementations for the browser

- **awtea-instrument**: Instrumentation and bytecode transformation
  - TeaVM bytecode transformers
  - Method detours and monitoring
  - Performance monitoring tools

- **awtea-ui**: UI widgets and development tools
  - Debug and monitoring windows
  - Developer tools UI
  - Floating windows

- **awtea-util**: Common utilities and support
  - Utility classes and helpers
  - JSO extensions
  - Coverage analysis tools

## Building

Build all modules:
```bash
./gradlew build
```

Build a specific module:
```bash
./gradlew :awtea-graphics:build
```

## Using as a Dependency

### In Another Gradle Project

To use AWTea modules in your project, you can include them as a composite build or submodule:

**As a composite build:**
```kotlin
// settings.gradle.kts
includeBuild("path/to/awtea")
```

**As a submodule:**
```kotlin
// settings.gradle.kts
includeBuild("awtea")  // if awtea is in a subdirectory
```

Then declare dependencies on the specific modules you need:
```kotlin
// build.gradle.kts
dependencies {
    implementation("me.mdbell:awtea-classlib:0.1.0")
    // Add other modules as needed
}
```

Most projects will primarily need `awtea-classlib`, which transitively includes the other required modules.

## Module Dependencies

```
awtea-util (base utilities)
  ↓
awtea-core → awtea-instrument → awtea-ui
  ↓              ↓
awtea-graphics   ↓
  ↓              ↓
awtea-input      ↓
awtea-sound      ↓
awtea-net        ↓
  ↓              ↓
awtea-classlib (depends on most modules)
```

## Documentation

### Canvas Components

AWTea provides two canvas implementations for different use cases:

#### TCanvas (Lightweight)
- For embedding within AWT containers (panels, frames' content panes, etc.)
- Participates in AWT's component hierarchy and layout management
- Suitable for custom drawing components within an application

#### THeavyCanvas (Heavyweight)
- For top-level heavyweight windows (Frame, Dialog, Applet windows)
- Manages its own HTML canvas element and hardware rendering surface
- Handles native events directly via TEventManager
- Used internally by heavyweight peers like `TFrameFloatingPeer`
- Can be used for advanced use cases requiring direct canvas control (games, visualization, etc.)

**Example usage of THeavyCanvas:**
```java
// Create a heavyweight canvas for a container
THeavyCanvas canvas = new THeavyCanvas(document, container);
canvas.configureStandardEvents();

// Get the canvas element to attach to DOM
HTMLCanvasElement element = canvas.getCanvasElement();

// Resize the canvas
canvas.resize(800, 600);

// Get graphics context for rendering
TGraphics graphics = canvas.getGraphics();

// Clean up when done
canvas.destroy();
```

### Architecture Documentation
- [Component Mapping Strategy](docs/COMPONENT_MAPPING.md) - How AWT components map to web technologies
- [Rendering Backends](docs/RENDERING_BACKENDS.md) - WebGL, WASM, and Software rendering systems
- [CSS Embedding](docs/CSS_EMBEDDING.md) - Embedding CSS files with CSS custom properties for theming

### Development Tools

Generate API coverage reports:
```bash
./gradlew generateDocs
```

Find missing classes:
```bash
./gradlew findMissingClasses
```

## Native Components

The `awtea-graphics` module includes native C code compiled to WebAssembly for high-performance rasterization. The WASM module is automatically built using Docker and the Emscripten SDK when building the graphics module.

## Contributing
Contributions are welcome! Please file issues or submit pull requests.

## License
Specify license here (currently missing).
