# Focus Traversal System Documentation

## Overview

The Focus Traversal System provides comprehensive keyboard navigation support for awtea applications, matching the behavior and API of Java AWT/Swing's focus management system. This enables users to navigate between interactive components using the keyboard (typically TAB and Shift-TAB keys).

## Core Concepts

### Focus Owner
The **focus owner** is the Component that currently has keyboard focus and will receive keyboard events. Only one component can have focus at a time within an application.

### Focus Cycle Root
A **focus cycle root** is a Container that defines a boundary for focus traversal. When focus traverses past the last component in a cycle, it wraps around to the first component. Windows are focus cycle roots by default.

### Focus Traversal Policy
A **focus traversal policy** defines the order in which components are traversed within a focus cycle. Different policies can create different traversal orders (e.g., by addition order, by layout position, etc.).

### Focus Traversal Keys
**Focus traversal keys** are keyboard shortcuts that trigger focus traversal:
- **TAB**: Move focus to the next component (forward)
- **Shift-TAB**: Move focus to the previous component (backward)
- **Ctrl-TAB**: Move focus down to a nested focus cycle
- **Ctrl-Shift-TAB**: Move focus up to the parent focus cycle

## API Classes

### TKeyboardFocusManager

The central manager for keyboard focus in an application. Provides access to focus state and controls focus traversal.

```java
// Get the current focus manager
TKeyboardFocusManager manager = TKeyboardFocusManager.getCurrentKeyboardFocusManager();

// Query focus state
TComponent focusOwner = manager.getFocusOwner();
TComponent permanentFocusOwner = manager.getPermanentFocusOwner();
TWindow focusedWindow = manager.getFocusedWindow();

// Programmatically change focus
manager.setGlobalFocusOwner(myComponent);

// Trigger focus traversal
manager.focusNextComponent();          // TAB
manager.focusPreviousComponent();      // Shift-TAB
manager.upFocusCycle();                // Ctrl-Shift-TAB
manager.downFocusCycle();              // Ctrl-TAB

// Configure default focus traversal policy
TFocusTraversalPolicy policy = new TDefaultFocusTraversalPolicy();
manager.setDefaultFocusTraversalPolicy(policy);
```

**Key Methods:**
- `getFocusOwner()`: Returns the component that currently has focus
- `setGlobalFocusOwner(TComponent)`: Sets the focus owner
- `focusNextComponent()`: Moves focus to the next component
- `focusPreviousComponent()`: Moves focus to the previous component
- `getDefaultFocusTraversalPolicy()`: Returns the default traversal policy
- `setDefaultFocusTraversalPolicy(TFocusTraversalPolicy)`: Sets the default policy

### TFocusTraversalPolicy

Abstract base class that defines how focus traverses through components.

```java
public abstract class TFocusTraversalPolicy {
    // Get the component after aComponent in the traversal order
    public abstract TComponent getComponentAfter(TContainer aContainer, TComponent aComponent);
    
    // Get the component before aComponent in the traversal order
    public abstract TComponent getComponentBefore(TContainer aContainer, TComponent aComponent);
    
    // Get the first component in the traversal order
    public abstract TComponent getFirstComponent(TContainer aContainer);
    
    // Get the last component in the traversal order
    public abstract TComponent getLastComponent(TContainer aContainer);
    
    // Get the default component (usually the first)
    public abstract TComponent getDefaultComponent(TContainer aContainer);
    
    // Get the initial component when a window first appears
    public TComponent getInitialComponent(TWindow window);
}
```

### TDefaultFocusTraversalPolicy

The default focus traversal policy implementation. Traverses components in the order they were added to their containers (container order).

```java
// Create and set a default policy
TFocusTraversalPolicy policy = new TDefaultFocusTraversalPolicy();
container.setFocusTraversalPolicy(policy);
```

**Behavior:**
- Traverses in a pre-order depth-first manner through the component hierarchy
- Respects focus cycle root boundaries
- Skips non-focusable and invisible components
- Wraps around at the end of a focus cycle

### TContainerOrderFocusTraversalPolicy

A simpler policy that traverses components strictly in the order they were added to containers.

```java
TContainerOrderFocusTraversalPolicy policy = new TContainerOrderFocusTraversalPolicy();

// Configure down-cycle traversal behavior
policy.setImplicitDownCycleTraversal(true);  // Default: true
```

**Properties:**
- `implicitDownCycleTraversal`: If true, automatically traverses into nested focus cycle roots

## Component Integration

### TComponent Methods

Components have several methods for focus management:

```java
// Request focus
component.requestFocus();

// Transfer focus
component.transferFocus();              // Move to next component
component.transferFocusBackward();      // Move to previous component
component.transferFocusUpCycle();       // Move to parent focus cycle

// Query focus state
boolean hasFocus = (component == manager.getFocusOwner());
boolean isFocusable = component.isFocusable();
component.setFocusable(true);

// Focus traversal keys
boolean enabled = component.isFocusTraversalKeysEnabled();
component.setFocusTraversalKeysEnabled(false);  // Disable automatic TAB handling

// Find focus cycle root
TContainer cycleRoot = component.getFocusCycleRootAncestor();
```

### TContainer Methods

Containers can be configured as focus cycle roots and have their own traversal policies:

```java
// Configure as a focus cycle root
container.setFocusCycleRoot(true);
boolean isRoot = container.isFocusCycleRoot();

// Set a custom focus traversal policy
TFocusTraversalPolicy policy = new TDefaultFocusTraversalPolicy();
container.setFocusTraversalPolicy(policy);

// Query policy
TFocusTraversalPolicy currentPolicy = container.getFocusTraversalPolicy();
boolean hasCustomPolicy = container.isFocusTraversalPolicySet();

// Focus traversal policy provider
container.setFocusTraversalPolicyProvider(true);

// Find parent cycle root
TContainer parentRoot = container.getFocusCycleRootAncestor();
```

## Focus Events

Components can listen for focus changes using `TFocusListener`:

```java
component.addFocusListener(new TFocusListener() {
    @Override
    public void focusGained(TFocusEvent e) {
        // Component gained focus
        System.out.println("Component gained focus");
        component.setBackground(Color.YELLOW);
    }
    
    @Override
    public void focusLost(TFocusEvent e) {
        // Component lost focus
        System.out.println("Component lost focus");
        component.setBackground(Color.WHITE);
    }
});
```

**TFocusEvent Properties:**
- `getID()`: Either `FOCUS_GAINED` or `FOCUS_LOST`
- `getComponent()`: The component that fired the event

## Usage Examples

### Basic Focus Traversal

```java
// Create a form with focusable components
TFrame frame = new TFrame("My Application");
TPanel panel = new TPanel();
panel.setLayout(new TFlowLayout());

TButton button1 = new TButton("First");
TButton button2 = new TButton("Second");
TButton button3 = new TButton("Third");

panel.add(button1);
panel.add(button2);
panel.add(button3);

frame.add(panel);
frame.setVisible(true);

// Focus will automatically traverse: button1 -> button2 -> button3 -> button1
// User can press TAB to move forward, Shift-TAB to move backward
```

### Custom Focus Traversal Policy

```java
// Create a custom policy that reverses the traversal order
class ReversedPolicy extends TContainerOrderFocusTraversalPolicy {
    @Override
    public TComponent getComponentAfter(TContainer container, TComponent component) {
        // Go backward instead of forward
        return super.getComponentBefore(container, component);
    }
    
    @Override
    public TComponent getComponentBefore(TContainer container, TComponent component) {
        // Go forward instead of backward
        return super.getComponentAfter(container, component);
    }
}

// Apply the custom policy
container.setFocusTraversalPolicy(new ReversedPolicy());
```

### Focus Cycle Roots

```java
// Create a nested focus cycle
TFrame frame = new TFrame();
TPanel mainPanel = new TPanel();

// Create a nested panel that is a focus cycle root
TPanel nestedPanel = new TPanel();
nestedPanel.setFocusCycleRoot(true);
nestedPanel.add(new TButton("Nested 1"));
nestedPanel.add(new TButton("Nested 2"));

mainPanel.add(new TButton("Main 1"));
mainPanel.add(nestedPanel);
mainPanel.add(new TButton("Main 2"));

frame.add(mainPanel);

// TAB will traverse: Main 1 -> Nested 1 -> Nested 2 -> Main 2 -> Main 1
// The nested panel forms a separate focus cycle
```

### Programmatic Focus Control

```java
// Set initial focus
TButton submitButton = new TButton("Submit");
panel.add(submitButton);
submitButton.requestFocus();

// React to button clicks by moving focus
button.addActionListener(e -> {
    // Move to next field
    button.transferFocus();
});

// Prevent a component from being focusable
label.setFocusable(false);  // Labels don't need focus

// Disable TAB key handling for a text area (let it insert tabs)
textArea.setFocusTraversalKeysEnabled(false);
```

## Integration with Existing Code

### Backward Compatibility

The focus traversal system is fully integrated with the existing `TFocusManager`:

```java
// Old code using TFocusManager still works
TFocusManager.get().setGlobalFocusOwner(component);

// New code uses TKeyboardFocusManager (recommended)
TKeyboardFocusManager.getCurrentKeyboardFocusManager().setGlobalFocusOwner(component);

// Both approaches keep focus state synchronized
```

### Automatic TAB Handling

TAB key presses are automatically intercepted and converted to focus traversal:

```java
// In TComponent.dispatchKeyEvent():
// TAB key is checked first before dispatching to key listeners
// If focus traversal keys are enabled, TAB triggers traversal
// Otherwise, TAB is dispatched as a normal key event
```

## Best Practices

1. **Make Interactive Components Focusable**
   ```java
   button.setFocusable(true);  // Buttons should be focusable
   label.setFocusable(false);  // Labels typically don't need focus
   ```

2. **Provide Visual Focus Indicators**
   ```java
   component.addFocusListener(new TFocusListener() {
       public void focusGained(TFocusEvent e) {
           component.setBorder(focusedBorder);
       }
       public void focusLost(TFocusEvent e) {
           component.setBorder(normalBorder);
       }
   });
   ```

3. **Set Logical Tab Order**
   ```java
   // Add components in the order you want them to be traversed
   panel.add(nameField);
   panel.add(emailField);
   panel.add(submitButton);
   ```

4. **Use Focus Cycle Roots for Complex UIs**
   ```java
   // Group related components in a focus cycle root
   TPanel formPanel = new TPanel();
   formPanel.setFocusCycleRoot(true);
   // Now TAB won't escape the form until Ctrl-TAB is pressed
   ```

5. **Set Initial Focus**
   ```java
   // In window initialization or start() method
   firstField.requestFocus();
   ```

## Migration from Legacy Focus System

If you have existing code using `TFocusManager`:

```java
// Old code:
TFocusManager.get().setGlobalFocusOwner(component);
TComponent owner = TFocusManager.get().getGlobalFocusOwner();

// New code (recommended):
TKeyboardFocusManager.getCurrentKeyboardFocusManager().setGlobalFocusOwner(component);
TComponent owner = TKeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();

// Both work and stay synchronized, but TKeyboardFocusManager provides more features
```

## Troubleshooting

### Focus Not Moving with TAB

**Check:**
1. Are components focusable? `component.isFocusable()`
2. Are components visible? `component.isVisible()`
3. Is there a focus cycle root? `component.getFocusCycleRootAncestor() != null`
4. Are focus traversal keys enabled? `component.isFocusTraversalKeysEnabled()`

### TAB Inserting Characters Instead of Traversing

**Solution:** Make sure focus traversal keys are enabled:
```java
component.setFocusTraversalKeysEnabled(true);  // Default is true
```

For components that should receive TAB characters (like text areas), disable it:
```java
textArea.setFocusTraversalKeysEnabled(false);
```

### Custom Policy Not Working

**Check:**
1. Is the policy set on a focus cycle root?
2. Does the policy return non-null components?
3. Are the returned components focusable and visible?

```java
// Debug focus traversal
TFocusTraversalPolicy policy = container.getFocusTraversalPolicy();
TComponent first = policy.getFirstComponent(container);
System.out.println("First component: " + first);
System.out.println("Is focusable: " + (first != null && first.isFocusable()));
```

## Performance Considerations

- Focus traversal is efficient for typical UI sizes (hundreds of components)
- For very large UIs, consider breaking into multiple focus cycle roots
- Focus event listeners are invoked synchronously, so keep them lightweight

## API Compatibility

This implementation provides **1-to-1 API compatibility** with Java AWT/Swing:

| Java AWT/Swing | awtea |
|---|---|
| `java.awt.KeyboardFocusManager` | `TKeyboardFocusManager` |
| `java.awt.FocusTraversalPolicy` | `TFocusTraversalPolicy` |
| `java.awt.DefaultFocusTraversalPolicy` | `TDefaultFocusTraversalPolicy` |
| `java.awt.ContainerOrderFocusTraversalPolicy` | `TContainerOrderFocusTraversalPolicy` |
| `java.awt.event.FocusEvent` | `TFocusEvent` |
| `java.awt.event.FocusListener` | `TFocusListener` |

All method signatures, behaviors, and semantics match the official Java documentation.

## See Also

- [Java AWT KeyboardFocusManager](https://docs.oracle.com/javase/8/docs/api/java/awt/KeyboardFocusManager.html)
- [Java AWT FocusTraversalPolicy](https://docs.oracle.com/javase/8/docs/api/java/awt/FocusTraversalPolicy.html)
- [Focus Demo Example](../examples/focus-demo/README.md)
