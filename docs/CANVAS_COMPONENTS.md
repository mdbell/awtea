# Canvas Components: TCanvas vs THeavyCanvas

## Overview

awtea provides two distinct canvas implementations for different use cases. This document clarifies the differences, provides usage examples, and offers guidance on choosing the right component for your needs.

## Quick Comparison Matrix

| Feature | TCanvas (Lightweight) | THeavyCanvas (Heavyweight) |
|---------|----------------------|----------------------------|
| **Extends** | `TComponent` | Standalone class (not a Component) |
| **DOM Element** | ❌ None | ✅ `HTMLCanvasElement` |
| **Rendering Surface** | Uses parent's surface | Owns dedicated `Surface` |
| **Event Handling** | Through AWT event hierarchy | Direct browser events via `TEventManager` |
| **Focus Management** | Standard AWT focus | Configurable, can be focusable |
| **Layout Support** | ✅ Yes (setBounds, LayoutManagers) | ❌ No (sized manually) |
| **Memory Footprint** | Low (shared resources) | Higher (dedicated GPU/memory resources) |
| **Use Cases** | Embedded drawing components | Top-level windows, peer implementations |
| **Hardware Acceleration** | Via parent's backend | Direct WebGL/WASM access |
| **Event Performance** | Good for embedded components | Optimal for high-frequency events |
| **Typical Usage** | Custom widgets, game sprites | Frame peers, standalone canvases |

## TCanvas (Lightweight)

### Description

`TCanvas` is a lightweight AWT component designed for custom drawing within containers. It integrates seamlessly with the AWT component hierarchy, participating in layout management, event dispatch, and the standard paint lifecycle.

### Architecture

```
┌────────────────────────────────────┐
│           TFrame (TSurface)        │
│  ┌──────────────────────────────┐  │
│  │     TPanel (Container)       │  │
│  │  ┌────────────────────────┐  │  │
│  │  │   TCanvas              │  │  │
│  │  │   (renders here)       │  │  │
│  │  └────────────────────────┘  │  │
│  └──────────────────────────────┘  │
│                                    │
│  All render to Frame's <canvas>   │
└────────────────────────────────────┘
```

### When to Use

✅ **Use TCanvas when you need:**
- A custom drawing area within a panel, frame, or dialog
- Standard AWT component behavior (bounds, visibility, focus)
- Integration with AWT layout managers
- Multiple small drawing areas in one window
- Game sprites or UI elements within a larger application
- Minimal memory and resource overhead

❌ **Don't use TCanvas when you need:**
- A top-level window (use `TFrame` instead)
- Direct hardware acceleration control (use `THeavyCanvas`)
- Maximum event handling performance (use `THeavyCanvas`)
- Separate z-ordering from parent (use `THeavyCanvas`)

### Complete Example: Interactive Drawing Canvas

```java
import me.mdbell.awtea.classlib.java.awt.*;
import me.mdbell.awtea.classlib.java.awt.event.*;

/**
 * Interactive canvas that draws circles where the user clicks
 */
public class InteractiveCanvas extends TCanvas {
    private java.util.List<Point> points = new java.util.ArrayList<>();
    private Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE};
    private int colorIndex = 0;
    
    public InteractiveCanvas() {
        // Add mouse listener for click events
        addMouseListener(new TMouseAdapter() {
            @Override
            public void mousePressed(TMouseEvent e) {
                // Record click position
                points.add(new Point(e.getX(), e.getY()));
                colorIndex = (colorIndex + 1) % colors.length;
                
                // Trigger repaint
                repaint();
            }
        });
        
        // Optional: Add focus support for keyboard events
        setFocusable(true);
        
        addKeyListener(new TKeyAdapter() {
            @Override
            public void keyPressed(TKeyEvent e) {
                if (e.getKeyCode() == TKeyEvent.VK_SPACE) {
                    // Clear on spacebar
                    points.clear();
                    repaint();
                }
            }
        });
    }
    
    @Override
    public void paint(TGraphics g) {
        // Draw background
        g.setColor(new Color(240, 240, 255));
        g.fillRect(0, 0, getWidth(), getHeight());
        
        // Draw border
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        
        // Draw all circles
        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);
            g.setColor(colors[i % colors.length]);
            g.fillOval(p.x - 15, p.y - 15, 30, 30);
        }
        
        // Draw instructions
        g.setColor(Color.BLACK);
        g.drawString("Click to draw circles, press SPACE to clear", 10, 20);
    }
}

// Usage:
public class Example {
    public static void main(String[] args) {
        TFrame frame = new TFrame();
        frame.setTitle("TCanvas Example");
        frame.setSize(600, 400);
        
        InteractiveCanvas canvas = new InteractiveCanvas();
        canvas.setBounds(50, 50, 500, 300);
        
        frame.add(canvas);
        frame.setVisible(true);
    }
}
```

### Animation Example

```java
public class AnimatedCanvas extends TCanvas implements Runnable {
    private int x = 0;
    private int direction = 1;
    private Thread animator;
    
    public AnimatedCanvas() {
        animator = new Thread(this);
        animator.start();
    }
    
    @Override
    public void paint(TGraphics g) {
        // Clear background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        // Draw moving ball
        g.setColor(Color.RED);
        g.fillOval(x, getHeight() / 2 - 20, 40, 40);
    }
    
    @Override
    public void run() {
        while (true) {
            // Update position
            x += direction * 2;
            
            // Bounce at edges
            if (x < 0 || x > getWidth() - 40) {
                direction *= -1;
            }
            
            // Repaint
            repaint();
            
            try {
                Thread.sleep(16); // ~60 FPS
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
```

## THeavyCanvas (Heavyweight)

### Description

`THeavyCanvas` is a heavyweight canvas component that manages its own HTML canvas element, rendering surface, and event handling. It's designed for high-performance scenarios and is primarily used internally by heavyweight peers like `TFrameFloatingPeer`.

### Architecture

```
┌─────────────────────────────────────┐
│        THeavyCanvas                 │
│  ┌───────────────────────────────┐  │
│  │   HTMLCanvasElement           │  │
│  │   (DOM in browser)            │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │   Surface (WebGL/WASM)        │  │
│  │   (rendering backend)         │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │   TEventManager               │  │
│  │   (direct browser events)     │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

### When to Use

✅ **Use THeavyCanvas when you need:**
- Implementation of heavyweight window peers (Frame, Dialog, Applet)
- Maximum rendering performance with direct hardware access
- High-frequency event handling (real-time games, drawing apps)
- Direct control over the canvas element and surface lifecycle
- Separate rendering context from other components
- Advanced rendering scenarios (custom shaders, WebGL features)

❌ **Don't use THeavyCanvas when you need:**
- Integration with AWT layout managers (use `TCanvas`)
- Multiple small canvases in one window (use `TCanvas`)
- Standard AWT component behavior without DOM overhead (use `TCanvas`)
- Simpler embedded drawing areas (use `TCanvas`)

### Complete Example: Standalone Canvas

```java
import me.mdbell.awtea.classlib.java.awt.*;
import me.mdbell.awtea.classlib.java.awt.THeavyCanvas;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;

/**
 * Example of using THeavyCanvas for a high-performance drawing surface
 */
public class StandaloneCanvasExample {
    
    public static void createCanvas() {
        HTMLDocument document = Window.current().getDocument();
        
        // Create a container for the canvas
        TContainer container = new TContainer();
        
        // Create heavyweight canvas
        THeavyCanvas canvas = new THeavyCanvas(document, container, 800, 600);
        
        // Configure standard event handling
        canvas.configureStandardEvents();
        
        // Get the canvas element and attach to DOM
        HTMLCanvasElement canvasElement = canvas.getCanvasElement();
        document.getBody().appendChild(canvasElement);
        
        // Render initial content
        renderContent(canvas);
        
        // Set up animation loop
        startAnimationLoop(canvas);
    }
    
    private static void renderContent(THeavyCanvas canvas) {
        TGraphics g = canvas.getGraphics();
        
        // Clear background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // Draw content
        g.setColor(Color.CYAN);
        g.fillRect(100, 100, 200, 150);
        
        g.setColor(Color.WHITE);
        g.drawString("THeavyCanvas - Direct Hardware Rendering", 50, 50);
    }
    
    private static void startAnimationLoop(THeavyCanvas canvas) {
        // Use browser's requestAnimationFrame for smooth rendering
        Window.requestAnimationFrame(timestamp -> {
            renderContent(canvas);
            startAnimationLoop(canvas);
        });
    }
}
```

### Example: Peer Implementation

```java
/**
 * Example of how THeavyCanvas is used in heavyweight peer implementations
 * (Simplified version of TFrameFloatingPeer)
 */
public class CustomWindowPeer {
    private final THeavyCanvas heavyCanvas;
    private final HTMLElement containerElement;
    
    public CustomWindowPeer(TContainer component) {
        HTMLDocument document = Window.current().getDocument();
        
        // Create heavyweight canvas with standard events
        heavyCanvas = new THeavyCanvas(document, component);
        heavyCanvas.configureStandardEvents();
        
        // Create container for windowing
        containerElement = document.createElement("div");
        containerElement.getStyle().setProperty("position", "absolute");
        containerElement.appendChild(heavyCanvas.getCanvasElement());
        
        document.getBody().appendChild(containerElement);
    }
    
    public void resize(int width, int height) {
        heavyCanvas.resize(width, height);
    }
    
    public TGraphics getGraphics() {
        return heavyCanvas.getGraphics();
    }
    
    public void destroy() {
        heavyCanvas.destroy();
        containerElement.getParentNode().removeChild(containerElement);
    }
}
```

### Direct Event Handling Example

```java
// Custom event configuration for specific needs
THeavyCanvas canvas = new THeavyCanvas(document, container, 1024, 768);

// Configure only the events you need
canvas.getEventManager()
    .disableContextMenu()
    .withMouse()
    .withMouseWheel()
    .withKeyboard();

// Or use standard configuration
canvas.configureStandardEvents();

// Access the underlying Surface for advanced operations
Surface surface = canvas.getSurface();
// ... perform advanced rendering operations
```

## Migration Guide

### From TCanvas to THeavyCanvas

If you need to migrate from a lightweight canvas to heavyweight (e.g., for performance reasons):

**Before (TCanvas):**
```java
public class MyCanvas extends TCanvas {
    @Override
    public void paint(TGraphics g) {
        // Drawing code
        g.fillRect(10, 10, 100, 100);
    }
}

// Usage
TFrame frame = new TFrame();
MyCanvas canvas = new MyCanvas();
canvas.setBounds(0, 0, 400, 300);
frame.add(canvas);
frame.setVisible(true);
```

**After (THeavyCanvas):**
```java
// THeavyCanvas is not a Component, so you manage it directly
public class MyHeavyCanvasWrapper {
    private THeavyCanvas canvas;
    
    public MyHeavyCanvasWrapper(HTMLDocument document, TContainer container) {
        canvas = new THeavyCanvas(document, container, 400, 300);
        canvas.configureStandardEvents();
    }
    
    public void render() {
        TGraphics g = canvas.getGraphics();
        // Drawing code
        g.fillRect(10, 10, 100, 100);
    }
    
    public HTMLCanvasElement getElement() {
        return canvas.getCanvasElement();
    }
}

// Usage - you need to manually attach to DOM
HTMLDocument document = Window.current().getDocument();
TContainer container = new TContainer();
MyHeavyCanvasWrapper wrapper = new MyHeavyCanvasWrapper(document, container);
document.getBody().appendChild(wrapper.getElement());
```

**Key Changes:**
1. THeavyCanvas is not a Component - no `add()`, `setBounds()`, or layout managers
2. You must manually attach the canvas element to the DOM
3. You must manually call rendering methods (no automatic `paint()` callback)
4. You're responsible for lifecycle management (calling `destroy()`)

### From THeavyCanvas to TCanvas

If you started with THeavyCanvas but want to embed it in an AWT container:

**Before (THeavyCanvas):**
```java
THeavyCanvas canvas = new THeavyCanvas(document, container, 400, 300);
canvas.configureStandardEvents();
document.getBody().appendChild(canvas.getCanvasElement());

// Manual rendering
TGraphics g = canvas.getGraphics();
g.fillRect(0, 0, 400, 300);
```

**After (TCanvas):**
```java
TCanvas canvas = new TCanvas() {
    @Override
    public void paint(TGraphics g) {
        g.fillRect(0, 0, getWidth(), getHeight());
    }
};

// Add to container with layout support
frame.add(canvas);
canvas.setBounds(0, 0, 400, 300);

// Automatic rendering through repaint()
canvas.repaint();
```

**Key Changes:**
1. Rendering is automatic through the paint lifecycle
2. No need to manage DOM elements
3. Automatic integration with AWT event system
4. Support for layout managers
5. Lower memory overhead

## Performance Considerations

### TCanvas Performance

**Pros:**
- Low memory overhead (shared surface)
- Efficient for multiple small canvases
- Good for UI elements and widgets
- Automatic batching with parent's rendering

**Cons:**
- Events go through parent hierarchy (minimal overhead)
- Shares parent's rendering backend choice
- All rendering serialized through parent's surface

**Best for:**
- UI components with moderate rendering frequency (<60 FPS animations)
- Multiple small drawing areas
- Applications with many components

### THeavyCanvas Performance

**Pros:**
- Direct hardware acceleration
- Maximum rendering performance
- Independent rendering pipeline
- Direct event handling (minimal latency)
- Can choose specific backend (WebGL, WASM, etc.)

**Cons:**
- Higher memory overhead (dedicated resources)
- GPU context switching between multiple canvases
- More complex lifecycle management

**Best for:**
- High-frequency rendering (games, real-time visualizations)
- Top-level windows (Frame, Dialog)
- Applications needing maximum performance
- Full-screen or large canvases

## Integration Notes

### Using Multiple TCanvas Components

```java
TFrame frame = new TFrame();
frame.setLayout(new GridLayout(2, 2));

// Create multiple lightweight canvases efficiently
for (int i = 0; i < 4; i++) {
    final int index = i;
    TCanvas canvas = new TCanvas() {
        @Override
        public void paint(TGraphics g) {
            g.setColor(new Color(index * 60, 100, 200));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.WHITE);
            g.drawString("Canvas " + index, 10, 20);
        }
    };
    frame.add(canvas);
}

frame.setSize(600, 600);
frame.setVisible(true);
```

### Using THeavyCanvas with Custom Peers

```java
// This is how heavyweight peers should use THeavyCanvas
public class MyCustomPeer extends FloatingWindow {
    private final THeavyCanvas heavyCanvas;
    
    public MyCustomPeer(TComponent component) {
        super("my-custom-peer");
        
        heavyCanvas = new THeavyCanvas(
            Window.current().getDocument(),
            (TContainer) component,
            800,
            600
        );
        heavyCanvas.configureStandardEvents();
    }
    
    @Override
    protected HTMLElement buildBodyContent() {
        return heavyCanvas.getCanvasElement();
    }
    
    public TGraphics getGraphics() {
        return heavyCanvas.getGraphics();
    }
}
```

## Gotchas and Best Practices

### TCanvas Gotchas

❌ **Don't:**
- Forget to call `repaint()` after state changes
- Create very large TCanvas instances (use THeavyCanvas instead)
- Expect direct DOM manipulation
- Assume immediate rendering (it's event-driven)

✅ **Do:**
- Override `paint()` to provide custom drawing
- Use `repaint()` to trigger redraws
- Leverage AWT layout managers
- Keep rendering logic efficient (called frequently)

### THeavyCanvas Gotchas

❌ **Don't:**
- Forget to call `destroy()` when done
- Expect automatic paint() callbacks
- Use for small embedded components
- Create many instances unnecessarily

✅ **Do:**
- Call `configureStandardEvents()` for typical event handling
- Manage canvas lifecycle explicitly
- Use for top-level windows and peers
- Consider surface backend requirements

## API Reference Summary

### TCanvas

```java
// Class hierarchy
public class TCanvas extends TComponent

// Key methods (inherited)
void paint(TGraphics g)        // Override for custom drawing
void repaint()                 // Request repaint
void setBounds(int x, int y, int w, int h)
void setSize(int w, int h)
void addMouseListener(MouseListener l)
void addKeyListener(KeyListener l)
```

### THeavyCanvas

```java
// Standalone class (not a Component)
public class THeavyCanvas

// Constructors
THeavyCanvas(HTMLDocument doc, TContainer container)
THeavyCanvas(HTMLDocument doc, TContainer container, int width, int height)

// Configuration
THeavyCanvas configureStandardEvents()

// Access
HTMLCanvasElement getCanvasElement()
TGraphics getGraphics()
TBufferedImage getBufferedImage()
TEventManager getEventManager()
Surface getSurface()

// Lifecycle
void resize(int width, int height)
int getWidth()
int getHeight()
void destroy()
```

## Related Documentation

- [Component Mapping Strategy](COMPONENT_MAPPING.md) - Complete AWT component architecture
- [Rendering Backends](RENDERING_BACKENDS.md) - WebGL, WASM, and Software rendering
- [System Properties](SYSTEM_PROPERTIES.md) - Configuration options
- [Alpha Blending](ALPHA_BLENDING.md) - Transparency and compositing

## Source Files

- [`TCanvas.java`](../awtea-classlib/src/main/java/me/mdbell/awtea/classlib/java/awt/TCanvas.java)
- [`THeavyCanvas.java`](../awtea-classlib/src/main/java/me/mdbell/awtea/classlib/java/awt/THeavyCanvas.java)
- [`TFrameFloatingPeer.java`](../awtea-classlib/src/main/java/me/mdbell/awtea/classlib/java/awt/awtea/peer/TFrameFloatingPeer.java) - Example peer usage
