# Class: `KeyboardFocusManager` ![Coverage](https://img.shields.io/badge/coverage-25.0%25-orange)

**Full Name:** `java.awt.KeyboardFocusManager`

**Coverage:** 16 / 64 (25.0%)

```
[████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 25.0%
```

## ✓ Implemented Methods

- `protected void setGlobalActiveWindow(java.awt.Window)`
- `protected void setGlobalFocusedWindow(java.awt.Window)`
- `public java.awt.Component getFocusOwner()`
- `public java.awt.Component getPermanentFocusOwner()`
- `public java.awt.FocusTraversalPolicy getDefaultFocusTraversalPolicy()`
- `public java.awt.Window getActiveWindow()`
- `public java.awt.Window getFocusedWindow()`
- `public java.util.Set getDefaultFocusTraversalKeys(int)`
- `public static java.awt.KeyboardFocusManager getCurrentKeyboardFocusManager()`
- `public static void setCurrentKeyboardFocusManager(java.awt.KeyboardFocusManager)`
- `public void setDefaultFocusTraversalKeys(int, java.util.Set)`
- `public void setDefaultFocusTraversalPolicy(java.awt.FocusTraversalPolicy)`

## ✗ Missing Methods

- `protected abstract void dequeueKeyEvents(long, java.awt.Component)`
- `protected abstract void discardKeyEvents(java.awt.Component)`
- `protected abstract void enqueueKeyEvents(long, java.awt.Component)`
- `protected java.awt.Component getGlobalFocusOwner()`
- `protected java.awt.Component getGlobalPermanentFocusOwner()`
- `protected java.awt.Container getGlobalCurrentFocusCycleRoot()`
- `protected java.awt.Window getGlobalActiveWindow()`
- `protected java.awt.Window getGlobalFocusedWindow()`
- `protected java.util.List getKeyEventDispatchers()`
- `protected java.util.List getKeyEventPostProcessors()`
- `protected void firePropertyChange(java.lang.String, java.lang.Object, java.lang.Object)`
- `protected void fireVetoableChange(java.lang.String, java.lang.Object, java.lang.Object)`
- `protected void setGlobalFocusOwner(java.awt.Component)`
- `protected void setGlobalPermanentFocusOwner(java.awt.Component)`
- `public abstract boolean dispatchEvent(java.awt.AWTEvent)`
- `public abstract boolean dispatchKeyEvent(java.awt.event.KeyEvent)`
- `public abstract boolean postProcessKeyEvent(java.awt.event.KeyEvent)`
- `public abstract void downFocusCycle(java.awt.Container)`
- `public abstract void focusNextComponent(java.awt.Component)`
- `public abstract void focusPreviousComponent(java.awt.Component)`
- `public abstract void processKeyEvent(java.awt.Component, java.awt.event.KeyEvent)`
- `public abstract void upFocusCycle(java.awt.Component)`
- `public final void downFocusCycle()`
- `public final void focusNextComponent()`
- `public final void focusPreviousComponent()`
- `public final void redispatchEvent(java.awt.Component, java.awt.AWTEvent)`
- `public final void upFocusCycle()`
- `public java.awt.Container getCurrentFocusCycleRoot()`
- `public java.beans.PropertyChangeListener[] getPropertyChangeListeners()`
- `public java.beans.PropertyChangeListener[] getPropertyChangeListeners(java.lang.String)`
- `public java.beans.VetoableChangeListener[] getVetoableChangeListeners()`
- `public java.beans.VetoableChangeListener[] getVetoableChangeListeners(java.lang.String)`
- `public void addKeyEventDispatcher(java.awt.KeyEventDispatcher)`
- `public void addKeyEventPostProcessor(java.awt.KeyEventPostProcessor)`
- `public void addPropertyChangeListener(java.beans.PropertyChangeListener)`
- `public void addPropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)`
- `public void addVetoableChangeListener(java.beans.VetoableChangeListener)`
- `public void addVetoableChangeListener(java.lang.String, java.beans.VetoableChangeListener)`
- `public void clearFocusOwner()`
- `public void clearGlobalFocusOwner()`
- `public void removeKeyEventDispatcher(java.awt.KeyEventDispatcher)`
- `public void removeKeyEventPostProcessor(java.awt.KeyEventPostProcessor)`
- `public void removePropertyChangeListener(java.beans.PropertyChangeListener)`
- `public void removePropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)`
- `public void removeVetoableChangeListener(java.beans.VetoableChangeListener)`
- `public void removeVetoableChangeListener(java.lang.String, java.beans.VetoableChangeListener)`
- `public void setGlobalCurrentFocusCycleRoot(java.awt.Container)`

## ✓ Implemented Fields

- `public static final int BACKWARD_TRAVERSAL_KEYS`
- `public static final int DOWN_CYCLE_TRAVERSAL_KEYS`
- `public static final int FORWARD_TRAVERSAL_KEYS`
- `public static final int UP_CYCLE_TRAVERSAL_KEYS`

## ✗ Missing Constructors

- `public java.awt.KeyboardFocusManager()`


[← Back to Package](index.md)
