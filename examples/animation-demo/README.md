# Animation Demo Example

A comprehensive awtea example demonstrating real-time animation, physics simulation, and interactive controls.

## Status

⚠️ **Note**: This example is currently experiencing TeaVM compilation issues that are being investigated in the awtea project. The code demonstrates correct API usage and will work once runtime compatibility issues are resolved.

## What This Example Demonstrates

- **Frame-based animation loop**: Consistent ~60 FPS rendering with `Runnable` implementation
- **Physics simulation**: Gravity (400 px/s²) and velocity-based movement
- **Collision detection**: Bouncing off walls with damping (0.8 coefficient)
- **FPS monitoring**: Real-time performance tracking with rolling window
- **Double buffering**: Offscreen rendering for smooth animations
- **Mouse interaction**: Add balls via left/right click with different behaviors
- **Keyboard controls**: Pause, reset, clear, and visual effects
- **Delta-time physics**: Frame-independent physics calculations
- **Motion trails**: Optional visual effect showing ball paths
- **Velocity vectors**: Optional visualization of ball velocities

## Building

From the root awtea directory:

```bash
./gradlew :examples:animation-demo:build
```

This will:
1. Compile the Java source code
2. Run TeaVM to transpile to JavaScript/WebAssembly
3. Generate HTML and assets in `examples/animation-demo/build/dist/`

## Running

After building, open `examples/animation-demo/build/dist/index.html` in your web browser:

```bash
# From root directory (Linux/macOS)
xdg-open examples/animation-demo/build/dist/index.html

# Or serve with Python
cd examples/animation-demo/build/dist
python3 -m http.server 8000
# Then open http://localhost:8000
```

## Controls Reference

### Mouse Controls
- **Left Click**: Add a ball at the mouse position with random size/color
- **Right Click**: Add a ball with random velocity (gets thrown upward)

### Keyboard Controls
- **SPACE**: Pause/unpause the animation
- **C**: Clear all balls from the canvas
- **R**: Reset to 15 randomly positioned balls
- **T**: Toggle motion trails effect
- **V**: Toggle velocity vector visualization
- **+ / =**: Add one random ball
- **-**: Remove the last ball

## Code Structure

The example consists of three main classes:

### AnimationDemo.java
Main entry point that:
- Creates the `Frame` window (800x600)
- Initializes the `AnimationCanvas`
- Starts the animation loop

### AnimationCanvas (inner class)
Custom `Canvas` that implements `Runnable` for the animation thread:
- **Animation Loop**: Runs at ~60 FPS target using `Thread.sleep()`
- **Double Buffering**: Creates offscreen `Image` for smooth rendering
- **Physics Update**: Calculates ball positions using delta-time
- **Event Handling**: Responds to mouse and keyboard events
- **Rendering**: Draws balls, UI elements, and optional effects

Key features:
- Uses `System.currentTimeMillis()` for timing
- Delta-time calculation for frame-independent physics
- Offscreen buffer automatically sized to canvas dimensions
- FPS counter with 60-frame rolling window

### Ball.java
Physics entity representing a bouncing ball:
- **Properties**: Position (x, y), velocity (vx, vy), radius, color
- **Physics**: Gravity acceleration, velocity integration
- **Collision**: Wall bounce detection with damping
- **Rendering**: Draws filled circle with outline
- **Velocity Vectors**: Optional arrow visualization

Physics constants:
- Gravity: 400 pixels/second²
- Damping: 0.8 (80% velocity retention on bounce)

### FPSCounter.java
Performance monitoring utility:
- **Rolling Window**: Tracks last 60 frame times
- **Average Calculation**: Computes FPS from time samples
- **Methods**: `frame()` to record timing, `getFPS()` to get current rate

## Implementation Notes

### Animation Loop
The animation uses a dedicated thread that:
1. Calculates elapsed time since last frame
2. Updates physics simulation (if not paused)
3. Calls `repaint()` to trigger rendering
4. Records frame timing for FPS counter
5. Sleeps to maintain ~60 FPS target

### Physics Simulation
- **Delta Time**: Each frame calculates elapsed time in seconds
- **Gravity**: Applied as acceleration to Y velocity
- **Integration**: Euler method for position updates
- **Collisions**: Check boundaries and reverse velocity with damping
- **Stability**: Prevents balls from sticking at bottom boundary

### Double Buffering
The canvas uses an offscreen `Image` to:
1. Render the entire frame to the offscreen buffer
2. Copy the complete frame to the screen in one operation
3. Eliminate flicker and tearing artifacts

The buffer is automatically recreated if the canvas is resized.

### Keyboard Input
- Canvas is set to `focusable(true)` to receive key events
- `requestFocus()` called during initialization
- `KeyListener` handles key press events
- Uses `KeyEvent` constants for key codes

## Visual Effects

### Motion Trails
When enabled (press 'T'):
- Background is drawn with semi-transparent overlay
- Previous frames remain partially visible
- Creates a motion blur/trail effect

### Velocity Vectors
When enabled (press 'V'):
- Red arrows drawn from ball center
- Arrow length proportional to velocity magnitude
- Includes arrowhead for direction indication

## Performance

- **Target**: 60 FPS with smooth animation
- **Monitoring**: Real-time FPS display in top-left
- **Optimization**: Double buffering reduces redraw overhead
- **Scaling**: Performance depends on ball count and browser

## Next Steps

Use this example as a reference for:
- Building real-time interactive applications
- Implementing physics-based simulations
- Creating game-like experiences in awtea
- Learning animation and timing patterns
- Understanding event handling and user interaction
