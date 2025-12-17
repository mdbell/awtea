# awtea - Java AWT in the Browser via TeaVM

**awtea** is a clean-room implementation of Java AWT that runs in the browser using [TeaVM](https://teavm.org/) and WebAssembly. This is a multi-module Gradle project that compiles Java 11 bytecode to JavaScript/WASM for browser execution.

## Architecture Overview

### Module Structure (10 modules)
- **awtea-classlib**: Clean-room AWT/Swing API implementations (`java.awt.*`, `javax.swing.*`). Classes prefixed with `T` (e.g., `TFrame`, `TApplet`, `TGraphics`)
- **awtea-core**: Core runtime, helpers, virtual file system (IndexedDB)
- **awtea-graphics**: Multi-backend rendering (WebGL/WASM/Java software), font rendering with pluggable strategies, native C rasterization compiled to WASM
- **awtea-sound**: Audio subsystem (MIDI/PCM support)
- **awtea-input**: Browser input abstraction layer
- **awtea-net**: Browser-compatible networking
- **awtea-instrument**: TeaVM bytecode transformers for method detours, monitoring, and array workarounds
- **awtea-ui**: Developer tools (debug windows, monitoring UI)
- **awtea-util**: Utilities, JSO extensions, logging framework, coverage analysis
- **awtea-test-util**: Lightweight test framework for Deno execution (replaces JUnit for browser tests)

### Component Hierarchy Pattern
- **Heavyweight components** (e.g., `TFrame`, `TApplet`): Have DOM elements (`<canvas>`), use Peer classes, have their own Surface, receive native browser events
- **Lightweight components** (e.g., `TPanel`, `TCanvas`): Pure Java rendering into parent's surface, no DOM elements, events dispatched through hierarchy
- See `docs/COMPONENT_MAPPING.md` for complete mapping table

### Rendering Backend System
Three backends with automatic fallback (priority order):
1. **WebGL**: Hardware-accelerated, screen-only, requires `HTMLCanvasElement` + WebGL 2.0
2. **WASM**: High-performance offscreen rendering via native C code compiled to WASM, uses linear memory
3. **Java Software**: Pure Java fallback for compatibility

Backend selection: `System.setProperty("me.mdbell.awtea.gfx.backend", "wasm|software")`. See `docs/RENDERING_BACKENDS.md` and `docs/SYSTEM_PROPERTIES.md`.

### Font Rendering Architecture
Modular font rendering with `FontRenderer` interface and `FontPeer` bridge:
- **FontRenderer**: Strategy interface (current: raster renderer; future: SDF, canvas, vector)
- **FontPeer**: Bridge between logical fonts (`TFont`) and rendering implementation
- TrueType font support with multiple rendering strategies
- See `docs/FONT_RENDERING_ARCHITECTURE.md`

## Critical Build Workflows

### Standard Build Commands
```bash
./gradlew build                                    # Build all modules
./gradlew :awtea-graphics:build                    # Build specific module
./gradlew :examples:gui-demo:build                 # Build example (generates JS/HTML)
./gradlew :examples:gui-demo:generateJavaScript    # Compile Java → JS via TeaVM
```

### TeaVM Build Requirements
**IMPORTANT**: When building examples or any project using TeaVM's JavaScript generation, use the `--no-daemon` flag to avoid TeaVM plugin corruption:

```bash
./gradlew --no-daemon :examples:gui-demo:build
./gradlew --no-daemon :examples:layout-demo:build
./gradlew --no-daemon :examples:gui-demo:generateJavaScript
```

The Gradle daemon can cause TeaVM plugin state corruption, leading to build failures with errors like "Error loading plugins" or "ZipException: ZipFile invalid LOC header". Using `--no-daemon` ensures each build starts with a clean state.

This requirement only applies to tasks that invoke TeaVM's JavaScript/WASM generation (typically example builds). Library modules (`awtea-classlib`, `awtea-graphics`, etc.) can be built normally without this flag.

### WASM Compilation (Native C → WASM)
The `awtea-graphics` module compiles C rasterization code to WASM using Emscripten:
```bash
./gradlew :awtea-graphics:buildAwtRasterWasm  # Compile src/main/native/*.c → build/wasm/awt_raster.wasm
```
**Key points:**
- Uses `emcc` (Emscripten compiler) - requires dev container or local Emscripten SDK
- C sources in `awtea-graphics/src/main/native/`
- Output bundled into resources during `processResources` task
- WASM file loaded at runtime by `WasmSurfaceBackend`

### Code Generation (Required Before Build)
Enums are code-generated from YAML schemas before compilation:
```bash
./gradlew generateEnums  # Generate enums from schemas/ → C/Java/TypeScript
```
- Schemas in `schemas/` (e.g., `pixel-format.yaml`, `log-level.yaml`)
- Generator: `buildSrc/src/main/java/me/mdbell/awtea/codegen/EnumGenerator.java`
- Outputs:
  - C: `awtea-graphics/src/main/native/generated/`
  - Java: `awtea-graphics/src/main/java/me/mdbell/awtea/gfx/generated/`
  - TypeScript: `awtea-graphics/src/test/deno/generated/`
- Already hooked into `compileJava` dependency chain

### Testing with Deno
WASM rasterizer has isolated Deno tests (TypeScript):
```bash
./gradlew :awtea-graphics:denoTest  # Run src/test/deno/*.ts tests
```
Java tests compiled to JS for Deno execution (experimental):
```bash
./gradlew :awtea-graphics:buildDenoJavaTests  # Compile Java tests → JS via TeaVM
./gradlew :awtea-graphics:denoTestJava        # Run compiled tests in Deno
```

## Bytecode Instrumentation System

The `awtea-instrument` module provides TeaVM bytecode transformers registered via `CustomTransformersPlugin`:

### Method Detours
Redirect method/constructor calls to custom implementations using annotations:
```java
@DetourReceiver(target = Thread.class)
@NoDetours  // Prevent detour recursion
public class ThreadDetour {
    @DetourMethod("sleep")  // Instance method: static void sleep(Thread self, long millis)
    public static void sleep(Thread self, long millis) { /* custom impl */ }
    
    @DetourMethod("<init>")  // Constructor: static Thread factory(Runnable r)
    public static Thread factory(Runnable r) { /* custom impl */ }
}
```
- Detour classes loaded from `META-INF/awtea.detours` resource
- Three patterns: instance methods (first param = target), static methods, constructors (factory pattern)
- See `DetourHacks.java` for implementation

### Other Transformers
- **MonitorHacks**: Performance monitoring for annotated methods (`@MonitorMethod`)
- **ArrayHacks**: Workaround for TeaVM 3D array creation bug

## Project-Specific Conventions

### Naming Patterns
- AWT reimplementations: `T` prefix (`TFrame`, `TGraphics`, `TCanvas`)
- Native binding classes: Suffix `Binding` (e.g., for WASM imports)
- Peer classes: Suffix `Peer` (`TFrameFloatingPeer`, `TOffscreenBufferPeer`)
- Backend implementations: Suffix `Backend`/`Rasterizer`/`Surface`

### Logging
Unified logging framework in `awtea-util`:
```java
private static final Logger log = LoggerFactory.getLogger(MyClass.class);
log.info("Message with {} placeholder", value);
log.error("Exception occurred", exception);
```
- Log levels: `ERROR`, `WARN`, `INFO`, `DEBUG`
- Pluggable sinks: `ConsoleLogSink`, `LogFrameSink` (UI component)
- Configure via system properties (see `docs/LOGGING.md`)

### System Properties
Many runtime behaviors configurable via system properties:
- `me.mdbell.awtea.gfx.backend`: Force rendering backend
- `me.mdbell.awtea.font.renderer`: Font rendering strategy
- `me.mdbell.awtea.wasm.surface_pool_enabled`: Enable WASM surface pooling
- Full list in `docs/SYSTEM_PROPERTIES.md`

## Development Environment

### Dev Container (Recommended)
The `.devcontainer/` setup provides all tools pre-configured:
- Java 21 (VS Code default) + Java 11 (project builds via SDKMAN)
- Emscripten SDK (for C → WASM compilation)
- Deno (for TypeScript/WASM tests)
- Gradle wrapper
- **Usage**: Open in VS Code with Dev Containers extension → "Reopen in Container"

### Dependencies
- **TeaVM 0.13.0**: Core dependency for Java → JS/WASM transpilation
- **Lombok 1.18.36**: Compile-time annotation processing (use `@AllArgsConstructor`, `@Data`, etc.)
- TeaVM plugin applied in `build.gradle.kts` but not activated for libraries (only examples)

## Key Documentation
- `docs/RENDERING_BACKENDS.md`: Backend architecture and selection
- `docs/FONT_RENDERING_ARCHITECTURE.md`: Font system design
- `docs/COMPONENT_MAPPING.md`: AWT component → web mapping strategy
- `docs/SYSTEM_PROPERTIES.md`: Complete runtime configuration reference
- `docs/WasmSurfacePool.md`: WASM surface reuse pooling
- `docs/WASM_IMPORTS.md`: WASM module import/export conventions
- `buildSrc/src/main/java/me/mdbell/awtea/codegen/README.md`: Code generation system

* Use the following guidelines for the content of your reply:
    - Be concise and to the point. Avoid unnecessary details or explanations.
    - Do not restate or summarize the comment. Focus on addressing the specific request or question.
    - Use a friendly and professional tone. Do not thank the user or compliment their feedback or comments in your response.
    - **ALWAYS** include a screenshot of any UI changes so the user can see the impact of the change.

## Common Patterns

### Creating New AWT Components
1. Implement in `awtea-classlib/src/main/java/me/mdbell/awtea/classlib/java/awt/T*.java`
2. Decide heavyweight (needs Peer) vs lightweight (extends `TContainer`/`TComponent`)
3. If heavyweight: Create peer in `awtea-classlib/.../peer/` package
4. Reference `TFrame.java` (heavyweight) or component mapping docs

### Adding New Rendering Primitives
1. Add to appropriate backend's Rasterizer class (`WebGLRasterizer`, `WasmRasterizer`, `SoftwareRasterizer`)
2. For WASM: Add C implementation in `awtea-graphics/src/main/native/*.c`, rebuild WASM
3. For WASM imports: Update bindings and see `docs/WASM_IMPORTS.md`
4. **Add Deno tests**: Update `awtea-graphics/src/test/deno/*_test.ts` with tests covering the new primitive
5. **Add Deno demos**: Update or create demo files in `awtea-graphics/src/test/deno/*_demo.ts` to showcase the new functionality
6. **Update WasmRasterizer helper**: Add `write*Command()` static methods to `awtea-graphics/src/test/deno/wasm_rasterizer.ts` for easy test authoring

**Testing Requirements for WASM Rendering Changes:**
- Every new WASM rendering function MUST have corresponding Deno tests in `*_test.ts` files
- Every new WASM rendering function SHOULD have a visual demo in `demo.ts`
- Tests verify correctness using `Deno.test()`, demos showcase functionality visually
- **CRITICAL**: Always run ALL tests before committing ANY changes (not just WASM):
  ```bash
  ./gradlew :awtea-graphics:denoTest
  ```
- **REQUIRED**: Verify all tests pass before pushing commits
- **REQUIRED**: If ANY tests fail (even unrelated to your changes), investigate and fix them:
  - Read the test failure output carefully
  - Reproduce the failure locally if needed
  - Debug the root cause (don't just adjust test thresholds without understanding why)
  - Fix the underlying issue, not just the test
  - Run tests again to confirm the fix
- DO NOT commit if tests fail - fix the issues first
- Run visual demos manually to verify rendering: `deno run --allow-read src/test/deno/demo.ts`

### Extending Bytecode Transformation
1. Create transformer implementing `ClassHolderTransformer` in `awtea-instrument`
2. Register in `CustomTransformersPlugin.install()`
3. For detours: Use `@DetourReceiver`/`@DetourMethod` pattern, add class to `META-INF/awtea.detours`

## Testing Notes
- Examples currently have TeaVM compilation issues (under investigation)
- Example code demonstrates correct API usage and serves as reference
- WASM tests use Deno directly (isolated from TeaVM)
- Java test → Deno compilation is experimental via `awtea-test-util` framework

## When Editing Build Files
- Always check Gradle task dependencies (especially code generation → compilation chain)
- WASM compilation requires `emcc` on PATH (use dev container if not installed)
- Examples use TeaVM Gradle plugin with `teavm { js { ... } }` block configuration
- Module dependencies: Examples depend on library modules; instrument module is consumed by all
