# Synthesized Mouse Enter/Exit Events Implementation

## Overview

This document describes the implementation of synthesized MOUSE_ENTERED and MOUSE_EXITED events for individual AWT components rendered to the HTML canvas. Prior to this implementation, these events only fired for the canvas DOM element itself, not for individual Java components painted within it.

## Problem Statement

In standard AWT/Swing applications, components receive `mouseEntered` and `mouseExited` events when the mouse cursor moves into or out of their boundaries. This enables important UI behaviors such as:
- Dynamic cursor changes based on the hovered component
- Button hover effects and visual feedback
- Text field cursor switching
- Tooltips and context-sensitive help
- Drag-and-drop visual cues

In awtea, all components are rendered to a single HTML canvas element. The browser only fires mouse events for the canvas itself, not for individual painted components. This implementation synthesizes these events by tracking which component is under the mouse cursor and dispatching appropriate events as the mouse moves.

## Implementation Details

### Core Logic (TEventManager.java)

The `TEventManager` class is responsible for converting browser DOM events into AWT events. The key additions:

1. **Component Tracking**
   ```java
   private TComponent componentUnderMouse = null;
   ```
   Tracks the component currently under the mouse cursor.

2. **Event Synthesis**
   On every mouse event (move, press, release, etc.):
   - Use hit-testing (`getComponentAt`) to determine which component is under the cursor
   - Compare with the previously tracked component
   - If different:
     - Fire `MOUSE_EXITED` to the previous component
     - Fire `MOUSE_ENTERED` to the new component
     - Update tracking

3. **Canvas Exit Handling**
   When the mouse leaves the canvas entirely (via the browser's `mouseout` event):
   - Fire `MOUSE_EXITED` to the last tracked component
   - Clear the tracking state

### Hit-Testing Improvements (TContainer.java)

Enhanced the `getComponentAt()` method to respect component visibility:

```java
// Skip invisible components for hit-testing
if (!child.isVisible()) {
    continue;
}
```

This ensures that invisible components don't receive mouse events and don't interfere with mouse event delivery to visible components behind them.

### Coordinate System

Mouse event coordinates are maintained relative to each component:
- Browser events provide coordinates relative to the viewport
- `translatePoint()` converts to canvas-relative coordinates
- `getLocationOnScreen()` provides component position in canvas coordinates
- Final event coordinates are relative to the target component's origin

For MOUSE_EXITED events, coordinates are calculated relative to the exiting component using the current mouse position, ensuring accurate event information.

## Event Flow Example

Consider a user moving their mouse over a button:

1. **Mouse enters canvas** → Canvas receives native browser `mouseenter`
   - Hit-test determines root container is under mouse
   - `MOUSE_ENTERED` synthesized for root container

2. **Mouse moves to button area**
   - Hit-test determines button is now under mouse
   - `MOUSE_EXITED` synthesized for root container
   - `MOUSE_ENTERED` synthesized for button
   - Button can now show hover effects

3. **Mouse moves within button**
   - Hit-test still returns button
   - No enter/exit events (component unchanged)
   - Only `MOUSE_MOVED` dispatched

4. **Mouse moves to adjacent label**
   - Hit-test determines label is now under mouse
   - `MOUSE_EXITED` synthesized for button
   - `MOUSE_ENTERED` synthesized for label

5. **Mouse leaves canvas** → Canvas receives native browser `mouseout`
   - `MOUSE_EXITED` synthesized for label
   - Tracking cleared

## API Usage

### Adding Mouse Listeners

Components can now receive enter/exit events like standard AWT:

```java
button.addMouseListener(new MouseListener() {
    public void mouseEntered(MouseEvent e) {
        System.out.println("Mouse entered button");
        // Change cursor, show hover effect, etc.
    }
    
    public void mouseExited(MouseEvent e) {
        System.out.println("Mouse exited button");
        // Restore normal appearance
    }
    
    // Other methods...
});
```

### Using TMouseAdapter

For convenience, use `TMouseAdapter` to avoid implementing all methods:

```java
component.addMouseListener(new TMouseAdapter() {
    @Override
    public void mouseEntered(TMouseEvent e) {
        // Handle enter
    }
    
    @Override
    public void mouseExited(TMouseEvent e) {
        // Handle exit
    }
});
```

Note: When using standard `java.awt` API (e.g., `java.awt.Button`), you must use `java.awt.event.MouseListener`, not `TMouseAdapter`.

## Edge Cases Handled

1. **Invisible Components**: Excluded from hit-testing via visibility check
2. **Overlapping Components**: Z-order handled by component tree traversal
3. **Canvas Exit**: Properly fires exit event for last component
4. **Rapid Movement**: Events fire only when component changes (no redundant events)
5. **Component Hierarchy**: Hit-testing descends through container hierarchy to find deepest component

## Testing

The implementation was validated using the `gui-demo` example application:

### Test Scenarios
1. ✅ Moving mouse from canvas background to button → enter event fires
2. ✅ Moving mouse from button to canvas background → exit event fires
3. ✅ Moving mouse between buttons → exit + enter events fire in sequence
4. ✅ Moving mouse off canvas → exit event fires for last component
5. ✅ Rapid mouse movement → no duplicate events

### Console Output Example
```
Mouse entered DrawingCanvas
Mouse exited DrawingCanvas
Mouse entered button: Change Color
Mouse exited button: Change Color
Mouse entered button: Clear Canvas
Mouse exited button: Clear Canvas
```

## Performance Considerations

- **Hit-testing cost**: Performed on every mouse event, but optimized by early termination
- **Event overhead**: Two additional events per component transition (exit + enter)
- **Memory**: Single reference to track current component (minimal overhead)

The implementation is efficient for typical UI hierarchies (dozens to hundreds of components). For extremely complex UIs with thousands of components, consider spatial indexing if hit-testing becomes a bottleneck.

## Future Enhancements

Possible improvements for future work:

1. **Enter/Exit for disabled components**: Currently disabled state not checked
2. **Event capture phase**: Support capturing enter/exit events at container level
3. **Hover delay**: Add configurable delay before firing enter events
4. **Component ordering**: Consider explicit z-order for overlapping components
5. **Event coalescing**: Batch rapid transitions through multiple components

## Compatibility

This implementation maintains compatibility with standard AWT/Swing behavior:
- Event order matches Java AWT (exit before enter)
- Event coordinates are component-relative
- Events bubble through component hierarchy
- Visibility is respected

Applications written for standard AWT can use these events without modification when running on awtea.

## References

- Java AWT MouseEvent documentation: https://docs.oracle.com/javase/8/docs/api/java/awt/event/MouseEvent.html
- Issue discussion: awtea issue agent chat, 2025-12-18
- Related files:
  - `awtea-classlib/src/main/java/me/mdbell/awtea/classlib/java/awt/awtea/TEventManager.java`
  - `awtea-classlib/src/main/java/me/mdbell/awtea/classlib/java/awt/TContainer.java`
  - `awtea-classlib/src/main/java/me/mdbell/awtea/classlib/java/awt/event/TMouseAdapter.java`
