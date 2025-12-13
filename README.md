# AWTea

AWTea is a Java AWT implementation for the browser using TeaVM.

## Project Structure

This project is organized into multiple modules for better maintainability and modularity:

### Core Modules

- **awtea-core**: Core runtime and basic implementations
  - Contains helper classes and core implementations
  - Includes virtual file system support (IndexedDB)
  
- **awtea-classlib**: Java AWT class library reimplementations
  - Reimplementations of `java.awt.*`, `javax.swing.*`, and related packages
  - Browser-compatible versions of AWT classes

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
