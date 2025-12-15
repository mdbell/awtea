# Toolkit Implementation

This document describes the implementation of the `TToolkit` and `TAWTeaToolkit` classes in awtea, which provide the bridge between Java AWT's abstract toolkit interface and browser-based implementations.

## Overview

The `TToolkit` class is the abstract base class that defines the AWT toolkit interface, while `TAWTeaToolkit` provides the concrete browser-based implementation. These classes have been improved from their initial stubbed-out state to provide functional implementations of core toolkit methods.

## Implemented Methods

### Input Event Modifiers

The `TInputEvent` class now defines all standard AWT modifier mask constants:

#### Old-style Masks (for backward compatibility)
- `SHIFT_MASK` (1 << 0)
- `CTRL_MASK` (1 << 1)
- `META_MASK` (1 << 2)
- `ALT_MASK` (1 << 3)
- `BUTTON1_MASK` (1 << 4)
- `BUTTON2_MASK` (1 << 3) - Intentionally same as ALT_MASK for AWT compatibility
- `BUTTON3_MASK` (1 << 2) - Intentionally same as META_MASK for AWT compatibility
- `ALT_GRAPH_MASK` (1 << 5)

#### Extended Masks (preferred)
- `SHIFT_DOWN_MASK` (1 << 6)
- `CTRL_DOWN_MASK` (1 << 7)
- `META_DOWN_MASK` (1 << 8)
- `ALT_DOWN_MASK` (1 << 9)
- `BUTTON1_DOWN_MASK` (1 << 10)
- `BUTTON2_DOWN_MASK` (1 << 11)
- `BUTTON3_DOWN_MASK` (1 << 12)
- `ALT_GRAPH_DOWN_MASK` (1 << 13)

### Menu Shortcut Keys

```java
public int getMenuShortcutKeyMask()
public int getMenuShortcutKeyMaskEx()
```

Both methods return the Control key modifier (`CTRL_MASK` and `CTRL_DOWN_MASK` respectively), which is the standard menu shortcut key in most desktop environments. This matches typical browser keyboard shortcuts.

### Audio

```java
public void beep()
```

Plays a system beep sound using the Web Audio API. The implementation:
- Uses a 440Hz sine wave (standard musical note A4)
- Plays for 100ms duration
- Volume set to 30% to avoid being too loud
- Gracefully handles environments without Web Audio API support

**Browser Compatibility**: Requires Web Audio API support (available in all modern browsers).

### Graphics Synchronization

```java
public void sync()
```

Synchronizes rendering operations by requesting an animation frame. This ensures all pending DOM and canvas operations are flushed before continuing. In traditional AWT, this would flush X11 operations; in awtea, it uses `requestAnimationFrame` to ensure browser rendering is complete.

**Browser Limitation**: Unlike traditional AWT's `sync()` which blocks until rendering is complete, the browser implementation is asynchronous. It schedules a synchronization via `requestAnimationFrame` but returns immediately without blocking. This is a necessary deviation from the AWT specification due to JavaScript's single-threaded, event-driven nature where blocking operations would freeze the UI.

### Font Management

```java
public String[] getFontList()
```

Returns the list of available font families, including:
- **Logical fonts** (AWT standard): Dialog, DialogInput, Serif, SansSerif, Monospaced
- **Physical fonts** (awtea-specific): NotoSans, Helvetica

These fonts correspond to the TrueType font files available in the `webapp-common/fonts/` directory.

**Extensibility**: Additional fonts can be added by:
1. Placing `.ttf` files in the fonts directory
2. Updating the `getFontList()` method to include them
3. Ensuring the font loader can access them via the configured base URL

### Image Loading

```java
public boolean prepareImage(TImage img, int w, int h, TImageObserver obs)
public int checkImage(TImage img, int w, int h, TImageObserver obs)
```

Both methods account for awtea's synchronous image loading model:

- **`prepareImage()`**: Always returns `true` because images are loaded synchronously in awtea. If an observer is provided, it receives immediate notification with `ALLBITS | WIDTH | HEIGHT` flags.

- **`checkImage()`**: Always returns `ALLBITS | WIDTH | HEIGHT | PROPERTIES` flags, indicating the image is fully loaded and all information is available.

This differs from traditional AWT where image loading is asynchronous, but matches the browser's synchronous image decoding model used in awtea.

### Cursor Properties

```java
public int getMaximumCursorColors()
```

Returns `0x1000000` (16,777,216 colors), indicating true color support. Browser environments support full-color cursors via CSS cursor properties and data URLs.

```java
public TDimension getBestCursorSize(int preferredWidth, int preferredHeight)
```

Returns the requested dimensions as-is. Browsers can support arbitrary cursor sizes (within reasonable limits), so the preferred size is always the best size.

### Desktop Properties

```java
public Object getDesktopProperty(String name)
```

Returns `null` for all property queries. Desktop properties (like `awt.font.desktophints`, `DnD.gestureMotionThreshold`) are desktop environment-specific and don't have direct browser equivalents. Applications should use sensible defaults when properties are not available.

### System Properties

```java
public static String getProperty(String key, String defaultValue)
```

Delegates to `System.getProperty()` for consistency with the Java platform. This allows toolkit properties to be configured via system properties, which can be set in TeaVM through various mechanisms.

## Browser-Specific Considerations

### Web Audio API
The `beep()` method requires Web Audio API support. In environments without it (e.g., Node.js, very old browsers), the beep call will fail silently with a console warning.

### RequestAnimationFrame
The `sync()` method uses `requestAnimationFrame`, which is available in all modern browsers but may not be available in some non-browser JavaScript environments.

### Font Loading
Fonts are loaded asynchronously via fetch API from the configured base URL (default: `fonts/`). The base URL can be configured via the system property `me.mdbell.awtea.font.base_url`.

### Image Loading
While traditional AWT uses asynchronous image loading with producer/consumer patterns, awtea loads images synchronously using browser APIs. This simplifies the implementation but means the `prepareImage` and `checkImage` methods don't provide progressive loading feedback.

## Testing

To verify the toolkit implementations work correctly, you can use code like:

```java
Toolkit toolkit = Toolkit.getDefaultToolkit();

// Test font list
String[] fonts = toolkit.getFontList();
System.out.println("Available fonts: " + String.join(", ", fonts));

// Test screen properties  
Dimension screen = toolkit.getScreenSize();
int dpi = toolkit.getScreenResolution();
System.out.println("Screen: " + screen.width + "x" + screen.height + " @ " + dpi + " DPI");

// Test beep (will play sound in browser)
toolkit.beep();

// Test synchronization
toolkit.sync();

// Test menu shortcuts
int mask = toolkit.getMenuShortcutKeyMask();
System.out.println("Menu shortcut mask: " + mask);
```

## Future Improvements

Potential areas for enhancement:

1. **Desktop Properties**: Implement browser-specific equivalents for common desktop properties
2. **Font Discovery**: Dynamically detect available fonts rather than hardcoding the list
3. **Cursor Support**: Implement custom cursor creation when that functionality is added
4. **Screen Insets**: Calculate actual screen insets accounting for browser chrome/toolbars
5. **Multi-Monitor**: Support for multi-monitor configurations via Screen Orientation API

## Related Documentation

- `FONT_LOADING.md` - Details on font loading mechanism
- `SYSTEM_PROPERTIES.md` - Available system properties for configuration
- `COMPONENT_MAPPING.md` - How AWT components map to browser elements
