# Focus Traversal Demo

This demo showcases the Focus Traversal System implemented for awtea, demonstrating:

- **TAB key navigation**: Press TAB to move forward through focusable components
- **Shift-TAB navigation**: Press Shift-TAB to move backward through focusable components
- **Visual focus indicators**: Focused components are highlighted with a yellow background
- **Focus event handling**: Status updates show which component gains focus
- **Keyboard event handling**: Demonstrates key events on focused components

## Features Demonstrated

1. **Automatic Focus Traversal**: Components are automatically ordered for keyboard navigation
2. **Focus Events**: Visual feedback and status updates when focus changes
3. **Focus Cycle Roots**: The window acts as a focus cycle root, containing all traversable components
4. **Focusable Components**: All buttons are focusable and participate in the traversal order
5. **Focus Manager Integration**: Uses `TKeyboardFocusManager` for centralized focus control

## How to Build and Run

```bash
# Build the demo
./gradlew :examples:focus-demo:build

# The output will be in examples/focus-demo/build/dist/
# Open examples/focus-demo/build/dist/index.html in a web browser
```

## Implementation Details

The demo uses the following focus traversal classes:
- `TKeyboardFocusManager`: Central manager for focus state and traversal
- `TFocusTraversalPolicy`: Defines the order of traversal (default: container order)
- `TDefaultFocusTraversalPolicy`: Default policy that traverses in child addition order
- Focus events (`TFocusEvent`, `TFocusListener`): For responding to focus changes

## Architecture

The demo creates a simple form with multiple focusable buttons arranged in a grid layout. Each button:
- Registers focus listeners to update its appearance when focused/unfocused
- Registers key listeners to demonstrate keyboard interaction
- Participates in the automatic focus traversal system

The focus traversal system automatically:
- Identifies focusable components
- Creates a traversal order based on the container hierarchy
- Handles TAB/Shift-TAB key presses to navigate between components
- Fires focus events when focus changes
- Skips non-focusable or invisible components

## API Compatibility

This implementation provides 1-to-1 API compatibility with Java AWT/Swing's focus traversal system:
- `KeyboardFocusManager` → `TKeyboardFocusManager`
- `FocusTraversalPolicy` → `TFocusTraversalPolicy`
- `DefaultFocusTraversalPolicy` → `TDefaultFocusTraversalPolicy`
- `ContainerOrderFocusTraversalPolicy` → `TContainerOrderFocusTraversalPolicy`

All method signatures and behaviors match the official Java documentation.
