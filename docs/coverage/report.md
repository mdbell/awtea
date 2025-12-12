# AWT API Coverage Report

**Generated:** 2025-12-12 21:17:08

## Summary

- **Total Coverage**: 1327 / 2820 (47.1%)
- **Packages**: 12
- **Classes**: 118

```
[███████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░] 47.1%
```

## Package: `java.applet`

**Coverage:** 14 / 42 (33.3%)

```
[████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 33.3%
```

### Class: `Applet` ![Coverage](https://img.shields.io/badge/coverage-30.8%25-orange)

**Coverage:** 8 / 26 (30.8%)

```
[███████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 30.8%
```

#### ✓ Implemented Methods

- `public final void setStub(java.applet.AppletStub)`
- `public java.applet.AppletContext getAppletContext()`
- `public java.lang.String getParameter(java.lang.String)`
- `public java.net.URL getCodeBase()`
- `public java.net.URL getDocumentBase()`
- `public void init()`
- `public void start()`

#### ✗ Missing Methods

- `public boolean isActive()`
- `public boolean isValidateRoot()`
- `public java.applet.AudioClip getAudioClip(java.net.URL)`
- `public java.applet.AudioClip getAudioClip(java.net.URL, java.lang.String)`
- `public java.awt.Image getImage(java.net.URL)`
- `public java.awt.Image getImage(java.net.URL, java.lang.String)`
- `public java.lang.String getAppletInfo()`
- `public java.lang.String[][] getParameterInfo()`
- `public java.util.Locale getLocale()`
- `public javax.accessibility.AccessibleContext getAccessibleContext()`
- `public static final java.applet.AudioClip newAudioClip(java.net.URL)`
- `public void destroy()`
- `public void play(java.net.URL)`
- `public void play(java.net.URL, java.lang.String)`
- `public void resize(int, int)`
- `public void resize(java.awt.Dimension)`
- `public void showStatus(java.lang.String)`
- `public void stop()`

#### ✓ Implemented Constructors

- `public java.applet.Applet()`

---

### Class: `AppletContext` ![Coverage](https://img.shields.io/badge/coverage-0.0%25-red)

**Coverage:** 0 / 10 (0.0%)

```
[░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 0.0%
```

#### ✗ Missing Methods

- `public abstract java.applet.Applet getApplet(java.lang.String)`
- `public abstract java.applet.AudioClip getAudioClip(java.net.URL)`
- `public abstract java.awt.Image getImage(java.net.URL)`
- `public abstract java.io.InputStream getStream(java.lang.String)`
- `public abstract java.util.Enumeration getApplets()`
- `public abstract java.util.Iterator getStreamKeys()`
- `public abstract void setStream(java.lang.String, java.io.InputStream)`
- `public abstract void showDocument(java.net.URL)`
- `public abstract void showDocument(java.net.URL, java.lang.String)`
- `public abstract void showStatus(java.lang.String)`

---

### Class: `AppletStub` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 6 / 6 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract boolean isActive()`
- `public abstract java.applet.AppletContext getAppletContext()`
- `public abstract java.lang.String getParameter(java.lang.String)`
- `public abstract java.net.URL getCodeBase()`
- `public abstract java.net.URL getDocumentBase()`
- `public abstract void appletResize(int, int)`

---

## Package: `java.awt`

**Coverage:** 326 / 918 (35.5%)

```
[█████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 35.5%
```

### Class: `AWTEvent` ![Coverage](https://img.shields.io/badge/coverage-16.7%25-red)

**Coverage:** 5 / 30 (16.7%)

```
[████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 16.7%
```

#### ✓ Implemented Methods

- `public int getID()`
- `public java.lang.String toString()`

#### ✗ Missing Methods

- `protected boolean isConsumed()`
- `protected void consume()`
- `public java.lang.String paramString()`
- `public void setSource(java.lang.Object)`

#### ✓ Implemented Fields

- `protected boolean consumed`
- `public static final int RESERVED_ID_MAX`

#### ✗ Missing Fields

- `protected int id`
- `public static final long ACTION_EVENT_MASK`
- `public static final long ADJUSTMENT_EVENT_MASK`
- `public static final long COMPONENT_EVENT_MASK`
- `public static final long CONTAINER_EVENT_MASK`
- `public static final long FOCUS_EVENT_MASK`
- `public static final long HIERARCHY_BOUNDS_EVENT_MASK`
- `public static final long HIERARCHY_EVENT_MASK`
- `public static final long INPUT_METHOD_EVENT_MASK`
- `public static final long INVOCATION_EVENT_MASK`
- `public static final long ITEM_EVENT_MASK`
- `public static final long KEY_EVENT_MASK`
- `public static final long MOUSE_EVENT_MASK`
- `public static final long MOUSE_MOTION_EVENT_MASK`
- `public static final long MOUSE_WHEEL_EVENT_MASK`
- `public static final long PAINT_EVENT_MASK`
- `public static final long TEXT_EVENT_MASK`
- `public static final long WINDOW_EVENT_MASK`
- `public static final long WINDOW_FOCUS_EVENT_MASK`
- `public static final long WINDOW_STATE_EVENT_MASK`

#### ✓ Implemented Constructors

- `public java.awt.AWTEvent(java.lang.Object, int)`

#### ✗ Missing Constructors

- `public java.awt.AWTEvent(java.awt.Event)`

---

### Class: `Canvas` ![Coverage](https://img.shields.io/badge/coverage-22.2%25-red)

**Coverage:** 2 / 9 (22.2%)

```
[███████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 22.2%
```

#### ✓ Implemented Methods

- `public void paint(java.awt.Graphics)`

#### ✗ Missing Methods

- `public java.awt.image.BufferStrategy getBufferStrategy()`
- `public javax.accessibility.AccessibleContext getAccessibleContext()`
- `public void addNotify()`
- `public void createBufferStrategy(int)`
- `public void createBufferStrategy(int, java.awt.BufferCapabilities)`
- `public void update(java.awt.Graphics)`

#### ✓ Implemented Constructors

- `public java.awt.Canvas()`

#### ✗ Missing Constructors

- `public java.awt.Canvas(java.awt.GraphicsConfiguration)`

---

### Class: `Component` ![Coverage](https://img.shields.io/badge/coverage-20.7%25-red)

**Coverage:** 48 / 232 (20.7%)

```
[██████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 20.7%
```

#### ✓ Implemented Methods

- `protected void firePropertyChange(java.lang.String, java.lang.Object, java.lang.Object)`
- `public boolean imageUpdate(java.awt.Image, int, int, int, int, int)`
- `public boolean isFocusable()`
- `public boolean isValid()`
- `public boolean prepareImage(java.awt.Image, int, int, java.awt.image.ImageObserver)`
- `public boolean prepareImage(java.awt.Image, java.awt.image.ImageObserver)`
- `public int checkImage(java.awt.Image, int, int, java.awt.image.ImageObserver)`
- `public int checkImage(java.awt.Image, java.awt.image.ImageObserver)`
- `public int getHeight()`
- `public int getWidth()`
- `public int getX()`
- `public int getY()`
- `public java.awt.Color getBackground()`
- `public java.awt.Color getForeground()`
- `public java.awt.Container getParent()`
- `public java.awt.Dimension getPreferredSize()`
- `public java.awt.FontMetrics getFontMetrics(java.awt.Font)`
- `public java.awt.Graphics getGraphics()`
- `public java.awt.Image createImage(int, int)`
- `public java.awt.Image createImage(java.awt.image.ImageProducer)`
- `public java.awt.Point getLocationOnScreen()`
- `public void addFocusListener(java.awt.event.FocusListener)`
- `public void addKeyListener(java.awt.event.KeyListener)`
- `public void addMouseListener(java.awt.event.MouseListener)`
- `public void addMouseMotionListener(java.awt.event.MouseMotionListener)`
- `public void addMouseWheelListener(java.awt.event.MouseWheelListener)`
- `public void invalidate()`
- `public void paint(java.awt.Graphics)`
- `public void removeFocusListener(java.awt.event.FocusListener)`
- `public void removeKeyListener(java.awt.event.KeyListener)`
- `public void removeMouseListener(java.awt.event.MouseListener)`
- `public void removeMouseMotionListener(java.awt.event.MouseMotionListener)`
- `public void removeMouseWheelListener(java.awt.event.MouseWheelListener)`
- `public void repaint()`
- `public void repaint(int, int, int, int)`
- `public void repaint(long, int, int, int, int)`
- `public void requestFocus()`
- `public void revalidate()`
- `public void setBackground(java.awt.Color)`
- `public void setFocusTraversalKeysEnabled(boolean)`
- `public void setFocusable(boolean)`
- `public void setForeground(java.awt.Color)`
- `public void setLocation(int, int)`
- `public void setPreferredSize(java.awt.Dimension)`
- `public void setSize(int, int)`
- `public void setVisible(boolean)`
- `public void update(java.awt.Graphics)`
- `public void validate()`

#### ✗ Missing Methods

- `protected boolean requestFocus(boolean)`
- `protected boolean requestFocus(boolean, java.awt.event.FocusEvent$Cause)`
- `protected boolean requestFocusInWindow(boolean)`
- `protected final void disableEvents(long)`
- `protected final void enableEvents(long)`
- `protected java.awt.AWTEvent coalesceEvents(java.awt.AWTEvent, java.awt.AWTEvent)`
- `protected java.lang.String paramString()`
- `protected void firePropertyChange(java.lang.String, boolean, boolean)`
- `protected void firePropertyChange(java.lang.String, int, int)`
- `protected void processComponentEvent(java.awt.event.ComponentEvent)`
- `protected void processEvent(java.awt.AWTEvent)`
- `protected void processFocusEvent(java.awt.event.FocusEvent)`
- `protected void processHierarchyBoundsEvent(java.awt.event.HierarchyEvent)`
- `protected void processHierarchyEvent(java.awt.event.HierarchyEvent)`
- `protected void processInputMethodEvent(java.awt.event.InputMethodEvent)`
- `protected void processKeyEvent(java.awt.event.KeyEvent)`
- `protected void processMouseEvent(java.awt.event.MouseEvent)`
- `protected void processMouseMotionEvent(java.awt.event.MouseEvent)`
- `protected void processMouseWheelEvent(java.awt.event.MouseWheelEvent)`
- `public boolean action(java.awt.Event, java.lang.Object)`
- `public boolean areFocusTraversalKeysSet(int)`
- `public boolean contains(int, int)`
- `public boolean contains(java.awt.Point)`
- `public boolean getFocusTraversalKeysEnabled()`
- `public boolean getIgnoreRepaint()`
- `public boolean gotFocus(java.awt.Event, java.lang.Object)`
- `public boolean handleEvent(java.awt.Event)`
- `public boolean hasFocus()`
- `public boolean inside(int, int)`
- `public boolean isBackgroundSet()`
- `public boolean isCursorSet()`
- `public boolean isDisplayable()`
- `public boolean isDoubleBuffered()`
- `public boolean isEnabled()`
- `public boolean isFocusCycleRoot(java.awt.Container)`
- `public boolean isFocusOwner()`
- `public boolean isFocusTraversable()`
- `public boolean isFontSet()`
- `public boolean isForegroundSet()`
- `public boolean isLightweight()`
- `public boolean isMaximumSizeSet()`
- `public boolean isMinimumSizeSet()`
- `public boolean isOpaque()`
- `public boolean isPreferredSizeSet()`
- `public boolean isShowing()`
- `public boolean isVisible()`
- `public boolean keyDown(java.awt.Event, int)`
- `public boolean keyUp(java.awt.Event, int)`
- `public boolean lostFocus(java.awt.Event, java.lang.Object)`
- `public boolean mouseDown(java.awt.Event, int, int)`
- `public boolean mouseDrag(java.awt.Event, int, int)`
- `public boolean mouseEnter(java.awt.Event, int, int)`
- `public boolean mouseExit(java.awt.Event, int, int)`
- `public boolean mouseMove(java.awt.Event, int, int)`
- `public boolean mouseUp(java.awt.Event, int, int)`
- `public boolean postEvent(java.awt.Event)`
- `public boolean requestFocusInWindow()`
- `public boolean requestFocusInWindow(java.awt.event.FocusEvent$Cause)`
- `public final java.lang.Object getTreeLock()`
- `public final void dispatchEvent(java.awt.AWTEvent)`
- `public float getAlignmentX()`
- `public float getAlignmentY()`
- `public int getBaseline(int, int)`
- `public java.awt.Component getComponentAt(int, int)`
- `public java.awt.Component getComponentAt(java.awt.Point)`
- `public java.awt.Component locate(int, int)`
- `public java.awt.Component$BaselineResizeBehavior getBaselineResizeBehavior()`
- `public java.awt.ComponentOrientation getComponentOrientation()`
- `public java.awt.Container getFocusCycleRootAncestor()`
- `public java.awt.Cursor getCursor()`
- `public java.awt.Dimension getMaximumSize()`
- `public java.awt.Dimension getMinimumSize()`
- `public java.awt.Dimension getSize()`
- `public java.awt.Dimension getSize(java.awt.Dimension)`
- `public java.awt.Dimension minimumSize()`
- `public java.awt.Dimension preferredSize()`
- `public java.awt.Dimension size()`
- `public java.awt.Font getFont()`
- `public java.awt.GraphicsConfiguration getGraphicsConfiguration()`
- `public java.awt.Point getLocation()`
- `public java.awt.Point getLocation(java.awt.Point)`
- `public java.awt.Point getMousePosition()`
- `public java.awt.Point location()`
- `public java.awt.Rectangle bounds()`
- `public java.awt.Rectangle getBounds()`
- `public java.awt.Rectangle getBounds(java.awt.Rectangle)`
- `public java.awt.Toolkit getToolkit()`
- `public java.awt.dnd.DropTarget getDropTarget()`
- `public java.awt.event.ComponentListener[] getComponentListeners()`
- `public java.awt.event.FocusListener[] getFocusListeners()`
- `public java.awt.event.HierarchyBoundsListener[] getHierarchyBoundsListeners()`
- `public java.awt.event.HierarchyListener[] getHierarchyListeners()`
- `public java.awt.event.InputMethodListener[] getInputMethodListeners()`
- `public java.awt.event.KeyListener[] getKeyListeners()`
- `public java.awt.event.MouseListener[] getMouseListeners()`
- `public java.awt.event.MouseMotionListener[] getMouseMotionListeners()`
- `public java.awt.event.MouseWheelListener[] getMouseWheelListeners()`
- `public java.awt.im.InputContext getInputContext()`
- `public java.awt.im.InputMethodRequests getInputMethodRequests()`
- `public java.awt.image.ColorModel getColorModel()`
- `public java.awt.image.VolatileImage createVolatileImage(int, int)`
- `public java.awt.image.VolatileImage createVolatileImage(int, int, java.awt.ImageCapabilities)`
- `public java.beans.PropertyChangeListener[] getPropertyChangeListeners()`
- `public java.beans.PropertyChangeListener[] getPropertyChangeListeners(java.lang.String)`
- `public java.lang.String getName()`
- `public java.lang.String toString()`
- `public java.util.EventListener[] getListeners(java.lang.Class)`
- `public java.util.Locale getLocale()`
- `public java.util.Set getFocusTraversalKeys(int)`
- `public javax.accessibility.AccessibleContext getAccessibleContext()`
- `public void add(java.awt.PopupMenu)`
- `public void addComponentListener(java.awt.event.ComponentListener)`
- `public void addHierarchyBoundsListener(java.awt.event.HierarchyBoundsListener)`
- `public void addHierarchyListener(java.awt.event.HierarchyListener)`
- `public void addInputMethodListener(java.awt.event.InputMethodListener)`
- `public void addNotify()`
- `public void addPropertyChangeListener(java.beans.PropertyChangeListener)`
- `public void addPropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)`
- `public void applyComponentOrientation(java.awt.ComponentOrientation)`
- `public void deliverEvent(java.awt.Event)`
- `public void disable()`
- `public void doLayout()`
- `public void enable()`
- `public void enable(boolean)`
- `public void enableInputMethods(boolean)`
- `public void firePropertyChange(java.lang.String, byte, byte)`
- `public void firePropertyChange(java.lang.String, char, char)`
- `public void firePropertyChange(java.lang.String, double, double)`
- `public void firePropertyChange(java.lang.String, float, float)`
- `public void firePropertyChange(java.lang.String, long, long)`
- `public void firePropertyChange(java.lang.String, short, short)`
- `public void hide()`
- `public void layout()`
- `public void list()`
- `public void list(java.io.PrintStream)`
- `public void list(java.io.PrintStream, int)`
- `public void list(java.io.PrintWriter)`
- `public void list(java.io.PrintWriter, int)`
- `public void move(int, int)`
- `public void nextFocus()`
- `public void paintAll(java.awt.Graphics)`
- `public void print(java.awt.Graphics)`
- `public void printAll(java.awt.Graphics)`
- `public void remove(java.awt.MenuComponent)`
- `public void removeComponentListener(java.awt.event.ComponentListener)`
- `public void removeHierarchyBoundsListener(java.awt.event.HierarchyBoundsListener)`
- `public void removeHierarchyListener(java.awt.event.HierarchyListener)`
- `public void removeInputMethodListener(java.awt.event.InputMethodListener)`
- `public void removeNotify()`
- `public void removePropertyChangeListener(java.beans.PropertyChangeListener)`
- `public void removePropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)`
- `public void repaint(long)`
- `public void requestFocus(java.awt.event.FocusEvent$Cause)`
- `public void reshape(int, int, int, int)`
- `public void resize(int, int)`
- `public void resize(java.awt.Dimension)`
- `public void setBounds(int, int, int, int)`
- `public void setBounds(java.awt.Rectangle)`
- `public void setComponentOrientation(java.awt.ComponentOrientation)`
- `public void setCursor(java.awt.Cursor)`
- `public void setDropTarget(java.awt.dnd.DropTarget)`
- `public void setEnabled(boolean)`
- `public void setFocusTraversalKeys(int, java.util.Set)`
- `public void setFont(java.awt.Font)`
- `public void setIgnoreRepaint(boolean)`
- `public void setLocale(java.util.Locale)`
- `public void setLocation(java.awt.Point)`
- `public void setMaximumSize(java.awt.Dimension)`
- `public void setMinimumSize(java.awt.Dimension)`
- `public void setMixingCutoutShape(java.awt.Shape)`
- `public void setName(java.lang.String)`
- `public void setSize(java.awt.Dimension)`
- `public void show()`
- `public void show(boolean)`
- `public void transferFocus()`
- `public void transferFocusBackward()`
- `public void transferFocusUpCycle()`

#### ✗ Missing Fields

- `protected javax.accessibility.AccessibleContext accessibleContext`
- `public static final float BOTTOM_ALIGNMENT`
- `public static final float CENTER_ALIGNMENT`
- `public static final float LEFT_ALIGNMENT`
- `public static final float RIGHT_ALIGNMENT`
- `public static final float TOP_ALIGNMENT`

#### ✗ Missing Constructors

- `protected java.awt.Component()`

---

### Class: `Container` ![Coverage](https://img.shields.io/badge/coverage-12.3%25-red)

**Coverage:** 9 / 73 (12.3%)

```
[██████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 12.3%
```

#### ✓ Implemented Methods

- `public boolean isFocusCycleRoot()`
- `public boolean isValidateRoot()`
- `public java.awt.Component add(java.awt.Component)`
- `public java.awt.Component getComponentAt(int, int)`
- `public java.awt.Component[] getComponents()`
- `public void paint(java.awt.Graphics)`
- `public void remove(java.awt.Component)`
- `public void setFocusCycleRoot(boolean)`

#### ✗ Missing Methods

- `protected java.lang.String paramString()`
- `protected void addImpl(java.awt.Component, java.lang.Object, int)`
- `protected void processContainerEvent(java.awt.event.ContainerEvent)`
- `protected void processEvent(java.awt.AWTEvent)`
- `protected void validateTree()`
- `public boolean areFocusTraversalKeysSet(int)`
- `public boolean isAncestorOf(java.awt.Component)`
- `public boolean isFocusCycleRoot(java.awt.Container)`
- `public boolean isFocusTraversalPolicySet()`
- `public final boolean isFocusTraversalPolicyProvider()`
- `public final void setFocusTraversalPolicyProvider(boolean)`
- `public float getAlignmentX()`
- `public float getAlignmentY()`
- `public int countComponents()`
- `public int getComponentCount()`
- `public int getComponentZOrder(java.awt.Component)`
- `public java.awt.Component add(java.awt.Component, int)`
- `public java.awt.Component add(java.lang.String, java.awt.Component)`
- `public java.awt.Component findComponentAt(int, int)`
- `public java.awt.Component findComponentAt(java.awt.Point)`
- `public java.awt.Component getComponent(int)`
- `public java.awt.Component getComponentAt(java.awt.Point)`
- `public java.awt.Component locate(int, int)`
- `public java.awt.Dimension getMaximumSize()`
- `public java.awt.Dimension getMinimumSize()`
- `public java.awt.Dimension getPreferredSize()`
- `public java.awt.Dimension minimumSize()`
- `public java.awt.Dimension preferredSize()`
- `public java.awt.FocusTraversalPolicy getFocusTraversalPolicy()`
- `public java.awt.Insets getInsets()`
- `public java.awt.Insets insets()`
- `public java.awt.LayoutManager getLayout()`
- `public java.awt.Point getMousePosition(boolean)`
- `public java.awt.event.ContainerListener[] getContainerListeners()`
- `public java.util.EventListener[] getListeners(java.lang.Class)`
- `public java.util.Set getFocusTraversalKeys(int)`
- `public void add(java.awt.Component, java.lang.Object)`
- `public void add(java.awt.Component, java.lang.Object, int)`
- `public void addContainerListener(java.awt.event.ContainerListener)`
- `public void addNotify()`
- `public void addPropertyChangeListener(java.beans.PropertyChangeListener)`
- `public void addPropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)`
- `public void applyComponentOrientation(java.awt.ComponentOrientation)`
- `public void deliverEvent(java.awt.Event)`
- `public void doLayout()`
- `public void invalidate()`
- `public void layout()`
- `public void list(java.io.PrintStream, int)`
- `public void list(java.io.PrintWriter, int)`
- `public void paintComponents(java.awt.Graphics)`
- `public void print(java.awt.Graphics)`
- `public void printComponents(java.awt.Graphics)`
- `public void remove(int)`
- `public void removeAll()`
- `public void removeContainerListener(java.awt.event.ContainerListener)`
- `public void removeNotify()`
- `public void setComponentZOrder(java.awt.Component, int)`
- `public void setFocusTraversalKeys(int, java.util.Set)`
- `public void setFocusTraversalPolicy(java.awt.FocusTraversalPolicy)`
- `public void setFont(java.awt.Font)`
- `public void setLayout(java.awt.LayoutManager)`
- `public void transferFocusDownCycle()`
- `public void update(java.awt.Graphics)`
- `public void validate()`

#### ✓ Implemented Constructors

- `public java.awt.Container()`

---

### Class: `DisplayMode` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 11 / 11 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public boolean equals(java.awt.DisplayMode)`
- `public boolean equals(java.lang.Object)`
- `public int getBitDepth()`
- `public int getHeight()`
- `public int getRefreshRate()`
- `public int getWidth()`
- `public int hashCode()`
- `public java.lang.String toString()`

#### ✓ Implemented Fields

- `public static final int BIT_DEPTH_MULTI`
- `public static final int REFRESH_RATE_UNKNOWN`

#### ✓ Implemented Constructors

- `public java.awt.DisplayMode(int, int, int, int)`

---

### Class: `EventDispatchThread` ![Coverage](https://img.shields.io/badge/coverage-25.0%25-orange)

**Coverage:** 1 / 4 (25.0%)

```
[████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 25.0%
```

#### ✓ Implemented Methods

- `public void run()`

#### ✗ Missing Methods

- `public java.awt.EventQueue getEventQueue()`
- `public void setEventQueue(java.awt.EventQueue)`
- `public void stopDispatching()`

---

### Class: `EventQueue` ![Coverage](https://img.shields.io/badge/coverage-78.6%25-green)

**Coverage:** 11 / 14 (78.6%)

```
[███████████████████████████████████████░░░░░░░░░░░] 78.6%
```

#### ✓ Implemented Methods

- `protected void dispatchEvent(java.awt.AWTEvent)`
- `protected void pop()`
- `public java.awt.AWTEvent peekEvent()`
- `public java.awt.AWTEvent peekEvent(int)`
- `public static boolean isDispatchThread()`
- `public static java.awt.AWTEvent getCurrentEvent()`
- `public static void invokeAndWait(java.lang.Runnable)`
- `public static void invokeLater(java.lang.Runnable)`
- `public void postEvent(java.awt.AWTEvent)`
- `public void push(java.awt.EventQueue)`

#### ✗ Missing Methods

- `public java.awt.AWTEvent getNextEvent()`
- `public java.awt.SecondaryLoop createSecondaryLoop()`
- `public static long getMostRecentEventTime()`

#### ✓ Implemented Constructors

- `public java.awt.EventQueue()`

---

### Class: `Font` ![Coverage](https://img.shields.io/badge/coverage-45.8%25-orange)

**Coverage:** 38 / 83 (45.8%)

```
[██████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 45.8%
```

#### ✓ Implemented Methods

- `public boolean equals(java.lang.Object)`
- `public boolean isBold()`
- `public boolean isItalic()`
- `public boolean isPlain()`
- `public int getSize()`
- `public int getStyle()`
- `public int hashCode()`
- `public java.awt.Font deriveFont(float)`
- `public java.awt.Font deriveFont(int)`
- `public java.awt.Font deriveFont(int, float)`
- `public java.lang.String getFamily()`
- `public java.lang.String getFontName()`
- `public java.lang.String getName()`
- `public java.lang.String getPSName()`
- `public static java.awt.Font createFont(int, java.io.File)`
- `public static java.awt.Font createFont(int, java.io.InputStream)`
- `public static java.awt.Font decode(java.lang.String)`
- `public static java.awt.Font getFont(java.lang.String)`
- `public static java.awt.Font getFont(java.lang.String, java.awt.Font)`

#### ✗ Missing Methods

- `public boolean canDisplay(char)`
- `public boolean canDisplay(int)`
- `public boolean hasLayoutAttributes()`
- `public boolean hasUniformLineMetrics()`
- `public boolean isTransformed()`
- `public byte getBaselineFor(char)`
- `public float getItalicAngle()`
- `public float getSize2D()`
- `public int canDisplayUpTo(char[], int, int)`
- `public int canDisplayUpTo(java.lang.String)`
- `public int canDisplayUpTo(java.text.CharacterIterator, int, int)`
- `public int getMissingGlyphCode()`
- `public int getNumGlyphs()`
- `public java.awt.Font deriveFont(int, java.awt.geom.AffineTransform)`
- `public java.awt.Font deriveFont(java.awt.geom.AffineTransform)`
- `public java.awt.Font deriveFont(java.util.Map)`
- `public java.awt.font.GlyphVector createGlyphVector(java.awt.font.FontRenderContext, char[])`
- `public java.awt.font.GlyphVector createGlyphVector(java.awt.font.FontRenderContext, int[])`
- `public java.awt.font.GlyphVector createGlyphVector(java.awt.font.FontRenderContext, java.lang.String)`
- `public java.awt.font.GlyphVector createGlyphVector(java.awt.font.FontRenderContext, java.text.CharacterIterator)`
- `public java.awt.font.GlyphVector layoutGlyphVector(java.awt.font.FontRenderContext, char[], int, int, int)`
- `public java.awt.font.LineMetrics getLineMetrics(char[], int, int, java.awt.font.FontRenderContext)`
- `public java.awt.font.LineMetrics getLineMetrics(java.lang.String, int, int, java.awt.font.FontRenderContext)`
- `public java.awt.font.LineMetrics getLineMetrics(java.lang.String, java.awt.font.FontRenderContext)`
- `public java.awt.font.LineMetrics getLineMetrics(java.text.CharacterIterator, int, int, java.awt.font.FontRenderContext)`
- `public java.awt.geom.AffineTransform getTransform()`
- `public java.awt.geom.Rectangle2D getMaxCharBounds(java.awt.font.FontRenderContext)`
- `public java.awt.geom.Rectangle2D getStringBounds(char[], int, int, java.awt.font.FontRenderContext)`
- `public java.awt.geom.Rectangle2D getStringBounds(java.lang.String, int, int, java.awt.font.FontRenderContext)`
- `public java.awt.geom.Rectangle2D getStringBounds(java.lang.String, java.awt.font.FontRenderContext)`
- `public java.awt.geom.Rectangle2D getStringBounds(java.text.CharacterIterator, int, int, java.awt.font.FontRenderContext)`
- `public java.lang.String getFamily(java.util.Locale)`
- `public java.lang.String getFontName(java.util.Locale)`
- `public java.lang.String toString()`
- `public java.text.AttributedCharacterIterator$Attribute[] getAvailableAttributes()`
- `public java.util.Map getAttributes()`
- `public static boolean textRequiresLayout(char[], int, int)`
- `public static java.awt.Font getFont(java.util.Map)`
- `public static java.awt.Font[] createFonts(java.io.File)`
- `public static java.awt.Font[] createFonts(java.io.InputStream)`

#### ✓ Implemented Fields

- `public static final int BOLD`
- `public static final int CENTER_BASELINE`
- `public static final int HANGING_BASELINE`
- `public static final int ITALIC`
- `public static final int LAYOUT_LEFT_TO_RIGHT`
- `public static final int LAYOUT_NO_LIMIT_CONTEXT`
- `public static final int LAYOUT_NO_START_CONTEXT`
- `public static final int LAYOUT_RIGHT_TO_LEFT`
- `public static final int PLAIN`
- `public static final int ROMAN_BASELINE`
- `public static final int TRUETYPE_FONT`
- `public static final int TYPE1_FONT`
- `public static final java.lang.String DIALOG`
- `public static final java.lang.String DIALOG_INPUT`
- `public static final java.lang.String MONOSPACED`
- `public static final java.lang.String SANS_SERIF`
- `public static final java.lang.String SERIF`

#### ✗ Missing Fields

- `protected float pointSize`
- `protected int size`
- `protected int style`
- `protected java.lang.String name`

#### ✓ Implemented Constructors

- `public java.awt.Font(java.lang.String, int, int)`
- `public java.awt.Font(java.util.Map)`

#### ✗ Missing Constructors

- `protected java.awt.Font(java.awt.Font)`

---

### Class: `FontMetrics` ![Coverage](https://img.shields.io/badge/coverage-17.2%25-red)

**Coverage:** 5 / 29 (17.2%)

```
[████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 17.2%
```

#### ✓ Implemented Methods

- `public int getAscent()`
- `public int getDescent()`
- `public int getLeading()`
- `public int stringWidth(java.lang.String)`
- `public java.awt.Font getFont()`

#### ✗ Missing Methods

- `public boolean hasUniformLineMetrics()`
- `public int bytesWidth(byte[], int, int)`
- `public int charWidth(char)`
- `public int charWidth(int)`
- `public int charsWidth(char[], int, int)`
- `public int getHeight()`
- `public int getMaxAdvance()`
- `public int getMaxAscent()`
- `public int getMaxDecent()`
- `public int getMaxDescent()`
- `public int[] getWidths()`
- `public java.awt.font.FontRenderContext getFontRenderContext()`
- `public java.awt.font.LineMetrics getLineMetrics(char[], int, int, java.awt.Graphics)`
- `public java.awt.font.LineMetrics getLineMetrics(java.lang.String, int, int, java.awt.Graphics)`
- `public java.awt.font.LineMetrics getLineMetrics(java.lang.String, java.awt.Graphics)`
- `public java.awt.font.LineMetrics getLineMetrics(java.text.CharacterIterator, int, int, java.awt.Graphics)`
- `public java.awt.geom.Rectangle2D getMaxCharBounds(java.awt.Graphics)`
- `public java.awt.geom.Rectangle2D getStringBounds(char[], int, int, java.awt.Graphics)`
- `public java.awt.geom.Rectangle2D getStringBounds(java.lang.String, int, int, java.awt.Graphics)`
- `public java.awt.geom.Rectangle2D getStringBounds(java.lang.String, java.awt.Graphics)`
- `public java.awt.geom.Rectangle2D getStringBounds(java.text.CharacterIterator, int, int, java.awt.Graphics)`
- `public java.lang.String toString()`

#### ✗ Missing Fields

- `protected java.awt.Font font`

#### ✗ Missing Constructors

- `protected java.awt.FontMetrics(java.awt.Font)`

---

### Class: `Frame` ![Coverage](https://img.shields.io/badge/coverage-6.0%25-red)

**Coverage:** 3 / 50 (6.0%)

```
[███░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 6.0%
```

#### ✓ Implemented Methods

- `public void setResizable(boolean)`
- `public void setTitle(java.lang.String)`

#### ✗ Missing Methods

- `protected java.lang.String paramString()`
- `public boolean isResizable()`
- `public boolean isUndecorated()`
- `public int getCursorType()`
- `public int getExtendedState()`
- `public int getState()`
- `public java.awt.Image getIconImage()`
- `public java.awt.MenuBar getMenuBar()`
- `public java.awt.Rectangle getMaximizedBounds()`
- `public java.lang.String getTitle()`
- `public javax.accessibility.AccessibleContext getAccessibleContext()`
- `public static java.awt.Frame[] getFrames()`
- `public void addNotify()`
- `public void remove(java.awt.MenuComponent)`
- `public void removeNotify()`
- `public void setBackground(java.awt.Color)`
- `public void setCursor(int)`
- `public void setExtendedState(int)`
- `public void setIconImage(java.awt.Image)`
- `public void setMaximizedBounds(java.awt.Rectangle)`
- `public void setMenuBar(java.awt.MenuBar)`
- `public void setOpacity(float)`
- `public void setShape(java.awt.Shape)`
- `public void setState(int)`
- `public void setUndecorated(boolean)`

#### ✗ Missing Fields

- `public static final int CROSSHAIR_CURSOR`
- `public static final int DEFAULT_CURSOR`
- `public static final int E_RESIZE_CURSOR`
- `public static final int HAND_CURSOR`
- `public static final int ICONIFIED`
- `public static final int MAXIMIZED_BOTH`
- `public static final int MAXIMIZED_HORIZ`
- `public static final int MAXIMIZED_VERT`
- `public static final int MOVE_CURSOR`
- `public static final int NE_RESIZE_CURSOR`
- `public static final int NORMAL`
- `public static final int NW_RESIZE_CURSOR`
- `public static final int N_RESIZE_CURSOR`
- `public static final int SE_RESIZE_CURSOR`
- `public static final int SW_RESIZE_CURSOR`
- `public static final int S_RESIZE_CURSOR`
- `public static final int TEXT_CURSOR`
- `public static final int WAIT_CURSOR`
- `public static final int W_RESIZE_CURSOR`

#### ✓ Implemented Constructors

- `public java.awt.Frame()`

#### ✗ Missing Constructors

- `public java.awt.Frame(java.awt.GraphicsConfiguration)`
- `public java.awt.Frame(java.lang.String)`
- `public java.awt.Frame(java.lang.String, java.awt.GraphicsConfiguration)`

---

### Class: `Graphics` ![Coverage](https://img.shields.io/badge/coverage-56.9%25-yellow)

**Coverage:** 29 / 51 (56.9%)

```
[████████████████████████████░░░░░░░░░░░░░░░░░░░░░░] 56.9%
```

#### ✓ Implemented Methods

- `public abstract boolean drawImage(java.awt.Image, int, int, int, int, java.awt.image.ImageObserver)`
- `public abstract boolean drawImage(java.awt.Image, int, int, java.awt.image.ImageObserver)`
- `public abstract java.awt.Color getColor()`
- `public abstract java.awt.Font getFont()`
- `public abstract java.awt.FontMetrics getFontMetrics(java.awt.Font)`
- `public abstract java.awt.Graphics create()`
- `public abstract java.awt.Rectangle getClipBounds()`
- `public abstract java.awt.Shape getClip()`
- `public abstract void clearRect(int, int, int, int)`
- `public abstract void clipRect(int, int, int, int)`
- `public abstract void drawArc(int, int, int, int, int, int)`
- `public abstract void drawLine(int, int, int, int)`
- `public abstract void drawPolygon(int[], int[], int)`
- `public abstract void drawRoundRect(int, int, int, int, int, int)`
- `public abstract void drawString(java.lang.String, int, int)`
- `public abstract void fillPolygon(int[], int[], int)`
- `public abstract void fillRect(int, int, int, int)`
- `public abstract void fillRoundRect(int, int, int, int, int, int)`
- `public abstract void setClip(int, int, int, int)`
- `public abstract void setClip(java.awt.Shape)`
- `public abstract void setColor(java.awt.Color)`
- `public abstract void setFont(java.awt.Font)`
- `public abstract void setPaintMode()`
- `public abstract void setXORMode(java.awt.Color)`
- `public abstract void translate(int, int)`
- `public java.awt.FontMetrics getFontMetrics()`
- `public java.awt.Graphics create(int, int, int, int)`
- `public java.lang.String toString()`

#### ✗ Missing Methods

- `public abstract boolean drawImage(java.awt.Image, int, int, int, int, int, int, int, int, java.awt.Color, java.awt.image.ImageObserver)`
- `public abstract boolean drawImage(java.awt.Image, int, int, int, int, int, int, int, int, java.awt.image.ImageObserver)`
- `public abstract boolean drawImage(java.awt.Image, int, int, int, int, java.awt.Color, java.awt.image.ImageObserver)`
- `public abstract boolean drawImage(java.awt.Image, int, int, java.awt.Color, java.awt.image.ImageObserver)`
- `public abstract void copyArea(int, int, int, int, int, int)`
- `public abstract void dispose()`
- `public abstract void drawOval(int, int, int, int)`
- `public abstract void drawPolyline(int[], int[], int)`
- `public abstract void drawString(java.text.AttributedCharacterIterator, int, int)`
- `public abstract void fillArc(int, int, int, int, int, int)`
- `public abstract void fillOval(int, int, int, int)`
- `public boolean hitClip(int, int, int, int)`
- `public java.awt.Rectangle getClipBounds(java.awt.Rectangle)`
- `public java.awt.Rectangle getClipRect()`
- `public void draw3DRect(int, int, int, int, boolean)`
- `public void drawBytes(byte[], int, int, int, int)`
- `public void drawChars(char[], int, int, int, int)`
- `public void drawPolygon(java.awt.Polygon)`
- `public void drawRect(int, int, int, int)`
- `public void fill3DRect(int, int, int, int, boolean)`
- `public void fillPolygon(java.awt.Polygon)`
- `public void finalize()`

#### ✓ Implemented Constructors

- `protected java.awt.Graphics()`

---

### Class: `Graphics2D` ![Coverage](https://img.shields.io/badge/coverage-57.5%25-yellow)

**Coverage:** 23 / 40 (57.5%)

```
[████████████████████████████░░░░░░░░░░░░░░░░░░░░░░] 57.5%
```

#### ✓ Implemented Methods

- `public abstract boolean hit(java.awt.Rectangle, java.awt.Shape, boolean)`
- `public abstract java.awt.Color getBackground()`
- `public abstract java.awt.Paint getPaint()`
- `public abstract java.awt.RenderingHints getRenderingHints()`
- `public abstract java.awt.geom.AffineTransform getTransform()`
- `public abstract void draw(java.awt.Shape)`
- `public abstract void drawString(java.lang.String, int, int)`
- `public abstract void drawString(java.text.AttributedCharacterIterator, float, float)`
- `public abstract void drawString(java.text.AttributedCharacterIterator, int, int)`
- `public abstract void fill(java.awt.Shape)`
- `public abstract void rotate(double)`
- `public abstract void rotate(double, double, double)`
- `public abstract void scale(double, double)`
- `public abstract void setBackground(java.awt.Color)`
- `public abstract void setPaint(java.awt.Paint)`
- `public abstract void setTransform(java.awt.geom.AffineTransform)`
- `public abstract void shear(double, double)`
- `public abstract void transform(java.awt.geom.AffineTransform)`
- `public abstract void translate(double, double)`
- `public abstract void translate(int, int)`
- `public void draw3DRect(int, int, int, int, boolean)`
- `public void fill3DRect(int, int, int, int, boolean)`

#### ✗ Missing Methods

- `public abstract boolean drawImage(java.awt.Image, java.awt.geom.AffineTransform, java.awt.image.ImageObserver)`
- `public abstract java.awt.Composite getComposite()`
- `public abstract java.awt.GraphicsConfiguration getDeviceConfiguration()`
- `public abstract java.awt.Stroke getStroke()`
- `public abstract java.awt.font.FontRenderContext getFontRenderContext()`
- `public abstract java.lang.Object getRenderingHint(java.awt.RenderingHints$Key)`
- `public abstract void addRenderingHints(java.util.Map)`
- `public abstract void clip(java.awt.Shape)`
- `public abstract void drawGlyphVector(java.awt.font.GlyphVector, float, float)`
- `public abstract void drawImage(java.awt.image.BufferedImage, java.awt.image.BufferedImageOp, int, int)`
- `public abstract void drawRenderableImage(java.awt.image.renderable.RenderableImage, java.awt.geom.AffineTransform)`
- `public abstract void drawRenderedImage(java.awt.image.RenderedImage, java.awt.geom.AffineTransform)`
- `public abstract void drawString(java.lang.String, float, float)`
- `public abstract void setComposite(java.awt.Composite)`
- `public abstract void setRenderingHint(java.awt.RenderingHints$Key, java.lang.Object)`
- `public abstract void setRenderingHints(java.util.Map)`
- `public abstract void setStroke(java.awt.Stroke)`

#### ✓ Implemented Constructors

- `protected java.awt.Graphics2D()`

---

### Class: `GraphicsConfiguration` ![Coverage](https://img.shields.io/badge/coverage-62.5%25-yellow)

**Coverage:** 10 / 16 (62.5%)

```
[███████████████████████████████░░░░░░░░░░░░░░░░░░░] 62.5%
```

#### ✓ Implemented Methods

- `public abstract java.awt.GraphicsDevice getDevice()`
- `public abstract java.awt.Rectangle getBounds()`
- `public abstract java.awt.geom.AffineTransform getDefaultTransform()`
- `public abstract java.awt.geom.AffineTransform getNormalizingTransform()`
- `public abstract java.awt.image.ColorModel getColorModel()`
- `public abstract java.awt.image.ColorModel getColorModel(int)`
- `public boolean isTranslucencyCapable()`
- `public java.awt.image.BufferedImage createCompatibleImage(int, int)`
- `public java.awt.image.BufferedImage createCompatibleImage(int, int, int)`

#### ✗ Missing Methods

- `public java.awt.BufferCapabilities getBufferCapabilities()`
- `public java.awt.ImageCapabilities getImageCapabilities()`
- `public java.awt.image.VolatileImage createCompatibleVolatileImage(int, int)`
- `public java.awt.image.VolatileImage createCompatibleVolatileImage(int, int, int)`
- `public java.awt.image.VolatileImage createCompatibleVolatileImage(int, int, java.awt.ImageCapabilities)`
- `public java.awt.image.VolatileImage createCompatibleVolatileImage(int, int, java.awt.ImageCapabilities, int)`

#### ✓ Implemented Constructors

- `protected java.awt.GraphicsConfiguration()`

---

### Class: `GraphicsDevice` ![Coverage](https://img.shields.io/badge/coverage-77.8%25-green)

**Coverage:** 14 / 18 (77.8%)

```
[██████████████████████████████████████░░░░░░░░░░░░] 77.8%
```

#### ✓ Implemented Methods

- `public abstract int getType()`
- `public abstract java.awt.GraphicsConfiguration getDefaultConfiguration()`
- `public abstract java.awt.GraphicsConfiguration[] getConfigurations()`
- `public abstract java.lang.String getIDstring()`
- `public boolean isDisplayChangeSupported()`
- `public boolean isFullScreenSupported()`
- `public int getAvailableAcceleratedMemory()`
- `public java.awt.DisplayMode getDisplayMode()`
- `public java.awt.DisplayMode[] getDisplayModes()`
- `public void setDisplayMode(java.awt.DisplayMode)`

#### ✗ Missing Methods

- `public boolean isWindowTranslucencySupported(java.awt.GraphicsDevice$WindowTranslucency)`
- `public java.awt.GraphicsConfiguration getBestConfiguration(java.awt.GraphicsConfigTemplate)`
- `public java.awt.Window getFullScreenWindow()`
- `public void setFullScreenWindow(java.awt.Window)`

#### ✓ Implemented Fields

- `public static final int TYPE_IMAGE_BUFFER`
- `public static final int TYPE_PRINTER`
- `public static final int TYPE_RASTER_SCREEN`

#### ✓ Implemented Constructors

- `protected java.awt.GraphicsDevice()`

---

### Class: `GraphicsEnvironment` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 15 / 15 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract java.awt.Font[] getAllFonts()`
- `public abstract java.awt.Graphics2D createGraphics(java.awt.image.BufferedImage)`
- `public abstract java.awt.GraphicsDevice getDefaultScreenDevice()`
- `public abstract java.awt.GraphicsDevice[] getScreenDevices()`
- `public abstract java.lang.String[] getAvailableFontFamilyNames()`
- `public abstract java.lang.String[] getAvailableFontFamilyNames(java.util.Locale)`
- `public boolean isHeadlessInstance()`
- `public boolean registerFont(java.awt.Font)`
- `public java.awt.Point getCenterPoint()`
- `public java.awt.Rectangle getMaximumWindowBounds()`
- `public static boolean isHeadless()`
- `public static java.awt.GraphicsEnvironment getLocalGraphicsEnvironment()`
- `public void preferLocaleFonts()`
- `public void preferProportionalFonts()`

#### ✓ Implemented Constructors

- `protected java.awt.GraphicsEnvironment()`

---

### Class: `Image` ![Coverage](https://img.shields.io/badge/coverage-77.8%25-green)

**Coverage:** 14 / 18 (77.8%)

```
[██████████████████████████████████████░░░░░░░░░░░░] 77.8%
```

#### ✓ Implemented Methods

- `public abstract int getHeight(java.awt.image.ImageObserver)`
- `public abstract int getWidth(java.awt.image.ImageObserver)`
- `public abstract java.awt.Graphics getGraphics()`
- `public abstract java.awt.image.ImageProducer getSource()`
- `public abstract java.lang.Object getProperty(java.lang.String, java.awt.image.ImageObserver)`
- `public float getAccelerationPriority()`
- `public java.awt.Image getScaledInstance(int, int, int)`
- `public void setAccelerationPriority(float)`

#### ✗ Missing Methods

- `public java.awt.ImageCapabilities getCapabilities(java.awt.GraphicsConfiguration)`
- `public void flush()`

#### ✓ Implemented Fields

- `public static final int SCALE_AREA_AVERAGING`
- `public static final int SCALE_DEFAULT`
- `public static final int SCALE_FAST`
- `public static final int SCALE_REPLICATE`
- `public static final int SCALE_SMOOTH`
- `public static final java.lang.Object UndefinedProperty`

#### ✗ Missing Fields

- `protected float accelerationPriority`

#### ✗ Missing Constructors

- `protected java.awt.Image()`

---

### Class: `Insets` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 10 / 10 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public boolean equals(java.lang.Object)`
- `public int hashCode()`
- `public java.lang.Object clone()`
- `public java.lang.String toString()`
- `public void set(int, int, int, int)`

#### ✓ Implemented Fields

- `public int bottom`
- `public int left`
- `public int right`
- `public int top`

#### ✓ Implemented Constructors

- `public java.awt.Insets(int, int, int, int)`

---

### Class: `MediaEntry` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 0 / 0 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

---

### Class: `MediaTracker` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 24 / 24 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public boolean checkAll()`
- `public boolean checkAll(boolean)`
- `public boolean checkID(int)`
- `public boolean checkID(int, boolean)`
- `public boolean isErrorAny()`
- `public boolean isErrorID(int)`
- `public boolean waitForAll(long)`
- `public boolean waitForID(int, long)`
- `public int statusAll(boolean)`
- `public int statusID(int, boolean)`
- `public java.lang.Object[] getErrorsAny()`
- `public java.lang.Object[] getErrorsID(int)`
- `public void addImage(java.awt.Image, int)`
- `public void addImage(java.awt.Image, int, int, int)`
- `public void removeImage(java.awt.Image)`
- `public void removeImage(java.awt.Image, int)`
- `public void removeImage(java.awt.Image, int, int, int)`
- `public void waitForAll()`
- `public void waitForID(int)`

#### ✓ Implemented Fields

- `public static final int ABORTED`
- `public static final int COMPLETE`
- `public static final int ERRORED`
- `public static final int LOADING`

#### ✓ Implemented Constructors

- `public java.awt.MediaTracker(java.awt.Component)`

---

### Class: `Paint` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 1 / 1 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract java.awt.PaintContext createContext(java.awt.image.ColorModel, java.awt.Rectangle, java.awt.geom.Rectangle2D, java.awt.geom.AffineTransform, java.awt.RenderingHints)`

---

### Class: `PaintContext` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 3 / 3 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract java.awt.image.ColorModel getColorModel()`
- `public abstract java.awt.image.Raster getRaster(int, int, int, int)`
- `public abstract void dispose()`

---

### Class: `Rectangle` ![Coverage](https://img.shields.io/badge/coverage-25.0%25-orange)

**Coverage:** 12 / 48 (25.0%)

```
[████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 25.0%
```

#### ✓ Implemented Methods

- `public boolean contains(int, int)`
- `public boolean contains(int, int, int, int)`
- `public java.awt.Rectangle getBounds()`
- `public java.awt.Rectangle intersection(java.awt.Rectangle)`
- `public java.awt.geom.Rectangle2D getBounds2D()`

#### ✗ Missing Methods

- `public boolean contains(java.awt.Point)`
- `public boolean contains(java.awt.Rectangle)`
- `public boolean equals(java.lang.Object)`
- `public boolean inside(int, int)`
- `public boolean intersects(java.awt.Rectangle)`
- `public boolean isEmpty()`
- `public double getHeight()`
- `public double getWidth()`
- `public double getX()`
- `public double getY()`
- `public int outcode(double, double)`
- `public java.awt.Dimension getSize()`
- `public java.awt.Point getLocation()`
- `public java.awt.Rectangle union(java.awt.Rectangle)`
- `public java.awt.geom.Rectangle2D createIntersection(java.awt.geom.Rectangle2D)`
- `public java.awt.geom.Rectangle2D createUnion(java.awt.geom.Rectangle2D)`
- `public java.lang.String toString()`
- `public void add(int, int)`
- `public void add(java.awt.Point)`
- `public void add(java.awt.Rectangle)`
- `public void grow(int, int)`
- `public void move(int, int)`
- `public void reshape(int, int, int, int)`
- `public void resize(int, int)`
- `public void setBounds(int, int, int, int)`
- `public void setBounds(java.awt.Rectangle)`
- `public void setLocation(int, int)`
- `public void setLocation(java.awt.Point)`
- `public void setRect(double, double, double, double)`
- `public void setSize(int, int)`
- `public void setSize(java.awt.Dimension)`
- `public void translate(int, int)`

#### ✓ Implemented Fields

- `public int height`
- `public int width`
- `public int x`
- `public int y`

#### ✓ Implemented Constructors

- `public java.awt.Rectangle()`
- `public java.awt.Rectangle(int, int)`
- `public java.awt.Rectangle(int, int, int, int)`

#### ✗ Missing Constructors

- `public java.awt.Rectangle(java.awt.Dimension)`
- `public java.awt.Rectangle(java.awt.Point)`
- `public java.awt.Rectangle(java.awt.Point, java.awt.Dimension)`
- `public java.awt.Rectangle(java.awt.Rectangle)`

---

### Class: `RenderingHints` ![Coverage](https://img.shields.io/badge/coverage-0.0%25-red)

**Coverage:** 0 / 66 (0.0%)

```
[░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 0.0%
```

#### ✗ Missing Methods

- `public boolean containsKey(java.lang.Object)`
- `public boolean containsValue(java.lang.Object)`
- `public boolean equals(java.lang.Object)`
- `public boolean isEmpty()`
- `public int hashCode()`
- `public int size()`
- `public java.lang.Object clone()`
- `public java.lang.Object get(java.lang.Object)`
- `public java.lang.Object put(java.lang.Object, java.lang.Object)`
- `public java.lang.Object remove(java.lang.Object)`
- `public java.lang.String toString()`
- `public java.util.Collection values()`
- `public java.util.Set entrySet()`
- `public java.util.Set keySet()`
- `public void add(java.awt.RenderingHints)`
- `public void clear()`
- `public void putAll(java.util.Map)`

#### ✗ Missing Fields

- `public static final java.awt.RenderingHints$Key KEY_ALPHA_INTERPOLATION`
- `public static final java.awt.RenderingHints$Key KEY_ANTIALIASING`
- `public static final java.awt.RenderingHints$Key KEY_COLOR_RENDERING`
- `public static final java.awt.RenderingHints$Key KEY_DITHERING`
- `public static final java.awt.RenderingHints$Key KEY_FRACTIONALMETRICS`
- `public static final java.awt.RenderingHints$Key KEY_INTERPOLATION`
- `public static final java.awt.RenderingHints$Key KEY_RENDERING`
- `public static final java.awt.RenderingHints$Key KEY_RESOLUTION_VARIANT`
- `public static final java.awt.RenderingHints$Key KEY_STROKE_CONTROL`
- `public static final java.awt.RenderingHints$Key KEY_TEXT_ANTIALIASING`
- `public static final java.awt.RenderingHints$Key KEY_TEXT_LCD_CONTRAST`
- `public static final java.lang.Object VALUE_ALPHA_INTERPOLATION_DEFAULT`
- `public static final java.lang.Object VALUE_ALPHA_INTERPOLATION_QUALITY`
- `public static final java.lang.Object VALUE_ALPHA_INTERPOLATION_SPEED`
- `public static final java.lang.Object VALUE_ANTIALIAS_DEFAULT`
- `public static final java.lang.Object VALUE_ANTIALIAS_OFF`
- `public static final java.lang.Object VALUE_ANTIALIAS_ON`
- `public static final java.lang.Object VALUE_COLOR_RENDER_DEFAULT`
- `public static final java.lang.Object VALUE_COLOR_RENDER_QUALITY`
- `public static final java.lang.Object VALUE_COLOR_RENDER_SPEED`
- `public static final java.lang.Object VALUE_DITHER_DEFAULT`
- `public static final java.lang.Object VALUE_DITHER_DISABLE`
- `public static final java.lang.Object VALUE_DITHER_ENABLE`
- `public static final java.lang.Object VALUE_FRACTIONALMETRICS_DEFAULT`
- `public static final java.lang.Object VALUE_FRACTIONALMETRICS_OFF`
- `public static final java.lang.Object VALUE_FRACTIONALMETRICS_ON`
- `public static final java.lang.Object VALUE_INTERPOLATION_BICUBIC`
- `public static final java.lang.Object VALUE_INTERPOLATION_BILINEAR`
- `public static final java.lang.Object VALUE_INTERPOLATION_NEAREST_NEIGHBOR`
- `public static final java.lang.Object VALUE_RENDER_DEFAULT`
- `public static final java.lang.Object VALUE_RENDER_QUALITY`
- `public static final java.lang.Object VALUE_RENDER_SPEED`
- `public static final java.lang.Object VALUE_RESOLUTION_VARIANT_BASE`
- `public static final java.lang.Object VALUE_RESOLUTION_VARIANT_DEFAULT`
- `public static final java.lang.Object VALUE_RESOLUTION_VARIANT_DPI_FIT`
- `public static final java.lang.Object VALUE_RESOLUTION_VARIANT_SIZE_FIT`
- `public static final java.lang.Object VALUE_STROKE_DEFAULT`
- `public static final java.lang.Object VALUE_STROKE_NORMALIZE`
- `public static final java.lang.Object VALUE_STROKE_PURE`
- `public static final java.lang.Object VALUE_TEXT_ANTIALIAS_DEFAULT`
- `public static final java.lang.Object VALUE_TEXT_ANTIALIAS_GASP`
- `public static final java.lang.Object VALUE_TEXT_ANTIALIAS_LCD_HBGR`
- `public static final java.lang.Object VALUE_TEXT_ANTIALIAS_LCD_HRGB`
- `public static final java.lang.Object VALUE_TEXT_ANTIALIAS_LCD_VBGR`
- `public static final java.lang.Object VALUE_TEXT_ANTIALIAS_LCD_VRGB`
- `public static final java.lang.Object VALUE_TEXT_ANTIALIAS_OFF`
- `public static final java.lang.Object VALUE_TEXT_ANTIALIAS_ON`

#### ✗ Missing Constructors

- `public java.awt.RenderingHints(java.awt.RenderingHints$Key, java.lang.Object)`
- `public java.awt.RenderingHints(java.util.Map)`

---

### Class: `Shape` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 10 / 10 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract boolean contains(double, double)`
- `public abstract boolean contains(double, double, double, double)`
- `public abstract boolean contains(java.awt.geom.Point2D)`
- `public abstract boolean contains(java.awt.geom.Rectangle2D)`
- `public abstract boolean intersects(double, double, double, double)`
- `public abstract boolean intersects(java.awt.geom.Rectangle2D)`
- `public abstract java.awt.Rectangle getBounds()`
- `public abstract java.awt.geom.PathIterator getPathIterator(java.awt.geom.AffineTransform)`
- `public abstract java.awt.geom.PathIterator getPathIterator(java.awt.geom.AffineTransform, double)`
- `public abstract java.awt.geom.Rectangle2D getBounds2D()`

---

### Class: `Toolkit` ![Coverage](https://img.shields.io/badge/coverage-40.7%25-orange)

**Coverage:** 24 / 59 (40.7%)

```
[████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 40.7%
```

#### ✓ Implemented Methods

- `protected abstract java.awt.EventQueue getSystemEventQueueImpl()`
- `public abstract boolean prepareImage(java.awt.Image, int, int, java.awt.image.ImageObserver)`
- `public abstract int checkImage(java.awt.Image, int, int, java.awt.image.ImageObserver)`
- `public abstract int getScreenResolution()`
- `public abstract java.awt.FontMetrics getFontMetrics(java.awt.Font)`
- `public abstract java.awt.Image createImage(byte[], int, int)`
- `public abstract java.awt.Image createImage(java.awt.image.ImageProducer)`
- `public abstract java.awt.Image createImage(java.lang.String)`
- `public abstract java.awt.Image createImage(java.net.URL)`
- `public abstract java.awt.Image getImage(java.lang.String)`
- `public abstract java.awt.Image getImage(java.net.URL)`
- `public abstract java.awt.image.ColorModel getColorModel()`
- `public abstract java.lang.String[] getFontList()`
- `public abstract void beep()`
- `public abstract void sync()`
- `public final java.awt.EventQueue getSystemEventQueue()`
- `public final java.lang.Object getDesktopProperty(java.lang.String)`
- `public int getMaximumCursorColors()`
- `public int getMenuShortcutKeyMask()`
- `public int getMenuShortcutKeyMaskEx()`
- `public java.awt.Image createImage(byte[])`
- `public java.awt.Insets getScreenInsets(java.awt.GraphicsConfiguration)`
- `public static java.awt.Toolkit getDefaultToolkit()`
- `public static java.lang.String getProperty(java.lang.String, java.lang.String)`

#### ✗ Missing Methods

- `protected boolean isDynamicLayoutSet()`
- `protected final void setDesktopProperty(java.lang.String, java.lang.Object)`
- `protected java.lang.Object lazilyLoadDesktopProperty(java.lang.String)`
- `protected static java.awt.Container getNativeContainer(java.awt.Component)`
- `protected void initializeDesktopProperties()`
- `protected void loadSystemColors(int[])`
- `public abstract boolean isModalExclusionTypeSupported(java.awt.Dialog$ModalExclusionType)`
- `public abstract boolean isModalityTypeSupported(java.awt.Dialog$ModalityType)`
- `public abstract java.awt.Dimension getScreenSize()`
- `public abstract java.awt.PrintJob getPrintJob(java.awt.Frame, java.lang.String, java.util.Properties)`
- `public abstract java.awt.datatransfer.Clipboard getSystemClipboard()`
- `public abstract java.util.Map mapInputMethodHighlight(java.awt.im.InputMethodHighlight)`
- `public boolean areExtraMouseButtonsEnabled()`
- `public boolean getLockingKeyState(int)`
- `public boolean isAlwaysOnTopSupported()`
- `public boolean isDynamicLayoutActive()`
- `public boolean isFrameStateSupported(int)`
- `public java.awt.Cursor createCustomCursor(java.awt.Image, java.awt.Point, java.lang.String)`
- `public java.awt.Dimension getBestCursorSize(int, int)`
- `public java.awt.PrintJob getPrintJob(java.awt.Frame, java.lang.String, java.awt.JobAttributes, java.awt.PageAttributes)`
- `public java.awt.datatransfer.Clipboard getSystemSelection()`
- `public java.awt.dnd.DragGestureRecognizer createDragGestureRecognizer(java.lang.Class, java.awt.dnd.DragSource, java.awt.Component, int, java.awt.dnd.DragGestureListener)`
- `public java.awt.event.AWTEventListener[] getAWTEventListeners()`
- `public java.awt.event.AWTEventListener[] getAWTEventListeners(long)`
- `public java.beans.PropertyChangeListener[] getPropertyChangeListeners()`
- `public java.beans.PropertyChangeListener[] getPropertyChangeListeners(java.lang.String)`
- `public void addAWTEventListener(java.awt.event.AWTEventListener, long)`
- `public void addPropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)`
- `public void removeAWTEventListener(java.awt.event.AWTEventListener)`
- `public void removePropertyChangeListener(java.lang.String, java.beans.PropertyChangeListener)`
- `public void setDynamicLayout(boolean)`
- `public void setLockingKeyState(int, boolean)`

#### ✗ Missing Fields

- `protected final java.beans.PropertyChangeSupport desktopPropsSupport`
- `protected final java.util.Map desktopProperties`

#### ✗ Missing Constructors

- `protected java.awt.Toolkit()`

---

### Class: `Transparency` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 4 / 4 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract int getTransparency()`

#### ✓ Implemented Fields

- `public static final int BITMASK`
- `public static final int OPAQUE`
- `public static final int TRANSLUCENT`

---

## Package: `java.awt.color`

**Coverage:** 12 / 42 (28.6%)

```
[██████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 28.6%
```

### Class: `ColorSpace` ![Coverage](https://img.shields.io/badge/coverage-28.6%25-orange)

**Coverage:** 12 / 42 (28.6%)

```
[██████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 28.6%
```

#### ✓ Implemented Methods

- `public abstract float[] fromCIEXYZ(float[])`
- `public abstract float[] fromRGB(float[])`
- `public abstract float[] toCIEXYZ(float[])`
- `public abstract float[] toRGB(float[])`
- `public int getNumComponents()`
- `public int getType()`
- `public static java.awt.color.ColorSpace getInstance(int)`

#### ✗ Missing Methods

- `public boolean isCS_sRGB()`
- `public float getMaxValue(int)`
- `public float getMinValue(int)`
- `public java.lang.String getName(int)`

#### ✓ Implemented Fields

- `public static final int CS_GRAY`
- `public static final int CS_sRGB`
- `public static final int TYPE_GRAY`
- `public static final int TYPE_RGB`

#### ✗ Missing Fields

- `public static final int CS_CIEXYZ`
- `public static final int CS_LINEAR_RGB`
- `public static final int CS_PYCC`
- `public static final int TYPE_2CLR`
- `public static final int TYPE_3CLR`
- `public static final int TYPE_4CLR`
- `public static final int TYPE_5CLR`
- `public static final int TYPE_6CLR`
- `public static final int TYPE_7CLR`
- `public static final int TYPE_8CLR`
- `public static final int TYPE_9CLR`
- `public static final int TYPE_ACLR`
- `public static final int TYPE_BCLR`
- `public static final int TYPE_CCLR`
- `public static final int TYPE_CMY`
- `public static final int TYPE_CMYK`
- `public static final int TYPE_DCLR`
- `public static final int TYPE_ECLR`
- `public static final int TYPE_FCLR`
- `public static final int TYPE_HLS`
- `public static final int TYPE_HSV`
- `public static final int TYPE_Lab`
- `public static final int TYPE_Luv`
- `public static final int TYPE_XYZ`
- `public static final int TYPE_YCbCr`
- `public static final int TYPE_Yxy`

#### ✓ Implemented Constructors

- `protected java.awt.color.ColorSpace(int, int)`

---

## Package: `java.awt.event`

**Coverage:** 291 / 393 (74.0%)

```
[█████████████████████████████████████░░░░░░░░░░░░░] 74.0%
```

### Class: `ActionEvent` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 14 / 14 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public int getModifiers()`
- `public java.lang.String getActionCommand()`
- `public java.lang.String paramString()`
- `public long getWhen()`

#### ✓ Implemented Fields

- `public static final int ACTION_FIRST`
- `public static final int ACTION_LAST`
- `public static final int ACTION_PERFORMED`
- `public static final int ALT_MASK`
- `public static final int CTRL_MASK`
- `public static final int META_MASK`
- `public static final int SHIFT_MASK`

#### ✓ Implemented Constructors

- `public java.awt.event.ActionEvent(java.lang.Object, int, java.lang.String)`
- `public java.awt.event.ActionEvent(java.lang.Object, int, java.lang.String, int)`
- `public java.awt.event.ActionEvent(java.lang.Object, int, java.lang.String, long, int)`

---

### Class: `ComponentAdapter` ![Coverage](https://img.shields.io/badge/coverage-0.0%25-red)

**Coverage:** 0 / 5 (0.0%)

```
[░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 0.0%
```

#### ✗ Missing Methods

- `public void componentHidden(java.awt.event.ComponentEvent)`
- `public void componentMoved(java.awt.event.ComponentEvent)`
- `public void componentResized(java.awt.event.ComponentEvent)`
- `public void componentShown(java.awt.event.ComponentEvent)`

#### ✗ Missing Constructors

- `protected java.awt.event.ComponentAdapter()`

---

### Class: `ComponentEvent` ![Coverage](https://img.shields.io/badge/coverage-22.2%25-red)

**Coverage:** 2 / 9 (22.2%)

```
[███████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 22.2%
```

#### ✓ Implemented Methods

- `public java.awt.Component getComponent()`

#### ✗ Missing Methods

- `public java.lang.String paramString()`

#### ✗ Missing Fields

- `public static final int COMPONENT_FIRST`
- `public static final int COMPONENT_HIDDEN`
- `public static final int COMPONENT_LAST`
- `public static final int COMPONENT_MOVED`
- `public static final int COMPONENT_RESIZED`
- `public static final int COMPONENT_SHOWN`

#### ✓ Implemented Constructors

- `public java.awt.event.ComponentEvent(java.awt.Component, int)`

---

### Class: `ComponentListener` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 4 / 4 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract void componentHidden(java.awt.event.ComponentEvent)`
- `public abstract void componentMoved(java.awt.event.ComponentEvent)`
- `public abstract void componentResized(java.awt.event.ComponentEvent)`
- `public abstract void componentShown(java.awt.event.ComponentEvent)`

---

### Class: `FocusEvent` ![Coverage](https://img.shields.io/badge/coverage-41.7%25-orange)

**Coverage:** 5 / 12 (41.7%)

```
[████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 41.7%
```

#### ✗ Missing Methods

- `public boolean isTemporary()`
- `public final java.awt.event.FocusEvent$Cause getCause()`
- `public java.awt.Component getOppositeComponent()`
- `public java.lang.String paramString()`

#### ✓ Implemented Fields

- `public static final int FOCUS_FIRST`
- `public static final int FOCUS_GAINED`
- `public static final int FOCUS_LAST`
- `public static final int FOCUS_LOST`

#### ✓ Implemented Constructors

- `public java.awt.event.FocusEvent(java.awt.Component, int)`

#### ✗ Missing Constructors

- `public java.awt.event.FocusEvent(java.awt.Component, int, boolean)`
- `public java.awt.event.FocusEvent(java.awt.Component, int, boolean, java.awt.Component)`
- `public java.awt.event.FocusEvent(java.awt.Component, int, boolean, java.awt.Component, java.awt.event.FocusEvent$Cause)`

---

### Class: `FocusListener` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 2 / 2 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract void focusGained(java.awt.event.FocusEvent)`
- `public abstract void focusLost(java.awt.event.FocusEvent)`

---

### Class: `InputEvent` ![Coverage](https://img.shields.io/badge/coverage-7.1%25-red)

**Coverage:** 2 / 28 (7.1%)

```
[███░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 7.1%
```

#### ✓ Implemented Methods

- `public int getModifiers()`
- `public long getWhen()`

#### ✗ Missing Methods

- `public boolean isAltDown()`
- `public boolean isAltGraphDown()`
- `public boolean isConsumed()`
- `public boolean isControlDown()`
- `public boolean isMetaDown()`
- `public boolean isShiftDown()`
- `public int getModifiersEx()`
- `public static int getMaskForButton(int)`
- `public static java.lang.String getModifiersExText(int)`
- `public void consume()`

#### ✗ Missing Fields

- `public static final int ALT_DOWN_MASK`
- `public static final int ALT_GRAPH_DOWN_MASK`
- `public static final int ALT_GRAPH_MASK`
- `public static final int ALT_MASK`
- `public static final int BUTTON1_DOWN_MASK`
- `public static final int BUTTON1_MASK`
- `public static final int BUTTON2_DOWN_MASK`
- `public static final int BUTTON2_MASK`
- `public static final int BUTTON3_DOWN_MASK`
- `public static final int BUTTON3_MASK`
- `public static final int CTRL_DOWN_MASK`
- `public static final int CTRL_MASK`
- `public static final int META_DOWN_MASK`
- `public static final int META_MASK`
- `public static final int SHIFT_DOWN_MASK`
- `public static final int SHIFT_MASK`

---

### Class: `InvocationEvent` ![Coverage](https://img.shields.io/badge/coverage-68.8%25-yellow)

**Coverage:** 11 / 16 (68.8%)

```
[██████████████████████████████████░░░░░░░░░░░░░░░░] 68.8%
```

#### ✓ Implemented Methods

- `public boolean isDispatched()`
- `public java.lang.Exception getException()`
- `public java.lang.Throwable getThrowable()`
- `public long getWhen()`
- `public void dispatch()`

#### ✗ Missing Methods

- `public java.lang.String paramString()`

#### ✓ Implemented Fields

- `public static final int INVOCATION_DEFAULT`
- `public static final int INVOCATION_FIRST`
- `public static final int INVOCATION_LAST`

#### ✗ Missing Fields

- `protected boolean catchExceptions`
- `protected java.lang.Object notifier`
- `protected java.lang.Runnable runnable`

#### ✓ Implemented Constructors

- `protected java.awt.event.InvocationEvent(java.lang.Object, int, java.lang.Runnable, java.lang.Object, boolean)`
- `public java.awt.event.InvocationEvent(java.lang.Object, java.lang.Runnable)`
- `public java.awt.event.InvocationEvent(java.lang.Object, java.lang.Runnable, java.lang.Object, boolean)`

#### ✗ Missing Constructors

- `public java.awt.event.InvocationEvent(java.lang.Object, java.lang.Runnable, java.lang.Runnable, boolean)`

---

### Class: `KeyEvent` ![Coverage](https://img.shields.io/badge/coverage-94.0%25-green)

**Coverage:** 202 / 215 (94.0%)

```
[██████████████████████████████████████████████░░░░] 94.0%
```

#### ✓ Implemented Methods

- `public char getKeyChar()`
- `public int getKeyCode()`

#### ✗ Missing Methods

- `public boolean isActionKey()`
- `public int getExtendedKeyCode()`
- `public int getKeyLocation()`
- `public java.lang.String paramString()`
- `public static int getExtendedKeyCodeForChar(int)`
- `public static java.lang.String getKeyModifiersText(int)`
- `public static java.lang.String getKeyText(int)`
- `public void setKeyChar(char)`
- `public void setKeyCode(int)`
- `public void setModifiers(int)`

#### ✓ Implemented Fields

- `public static final char CHAR_UNDEFINED`
- `public static final int KEY_FIRST`
- `public static final int KEY_LAST`
- `public static final int KEY_LOCATION_LEFT`
- `public static final int KEY_LOCATION_NUMPAD`
- `public static final int KEY_LOCATION_RIGHT`
- `public static final int KEY_LOCATION_STANDARD`
- `public static final int KEY_LOCATION_UNKNOWN`
- `public static final int KEY_PRESSED`
- `public static final int KEY_RELEASED`
- `public static final int KEY_TYPED`
- `public static final int VK_0`
- `public static final int VK_1`
- `public static final int VK_2`
- `public static final int VK_3`
- `public static final int VK_4`
- `public static final int VK_5`
- `public static final int VK_6`
- `public static final int VK_7`
- `public static final int VK_8`
- `public static final int VK_9`
- `public static final int VK_A`
- `public static final int VK_ACCEPT`
- `public static final int VK_ADD`
- `public static final int VK_AGAIN`
- `public static final int VK_ALL_CANDIDATES`
- `public static final int VK_ALPHANUMERIC`
- `public static final int VK_ALT`
- `public static final int VK_ALT_GRAPH`
- `public static final int VK_AMPERSAND`
- `public static final int VK_ASTERISK`
- `public static final int VK_AT`
- `public static final int VK_B`
- `public static final int VK_BACK_QUOTE`
- `public static final int VK_BACK_SLASH`
- `public static final int VK_BACK_SPACE`
- `public static final int VK_BEGIN`
- `public static final int VK_BRACELEFT`
- `public static final int VK_BRACERIGHT`
- `public static final int VK_C`
- `public static final int VK_CANCEL`
- `public static final int VK_CAPS_LOCK`
- `public static final int VK_CIRCUMFLEX`
- `public static final int VK_CLEAR`
- `public static final int VK_CLOSE_BRACKET`
- `public static final int VK_CODE_INPUT`
- `public static final int VK_COLON`
- `public static final int VK_COMMA`
- `public static final int VK_COMPOSE`
- `public static final int VK_CONTEXT_MENU`
- `public static final int VK_CONTROL`
- `public static final int VK_CONVERT`
- `public static final int VK_COPY`
- `public static final int VK_CUT`
- `public static final int VK_D`
- `public static final int VK_DEAD_ABOVEDOT`
- `public static final int VK_DEAD_ABOVERING`
- `public static final int VK_DEAD_ACUTE`
- `public static final int VK_DEAD_BREVE`
- `public static final int VK_DEAD_CARON`
- `public static final int VK_DEAD_CEDILLA`
- `public static final int VK_DEAD_CIRCUMFLEX`
- `public static final int VK_DEAD_DIAERESIS`
- `public static final int VK_DEAD_DOUBLEACUTE`
- `public static final int VK_DEAD_GRAVE`
- `public static final int VK_DEAD_IOTA`
- `public static final int VK_DEAD_MACRON`
- `public static final int VK_DEAD_OGONEK`
- `public static final int VK_DEAD_SEMIVOICED_SOUND`
- `public static final int VK_DEAD_TILDE`
- `public static final int VK_DEAD_VOICED_SOUND`
- `public static final int VK_DECIMAL`
- `public static final int VK_DELETE`
- `public static final int VK_DIVIDE`
- `public static final int VK_DOLLAR`
- `public static final int VK_DOWN`
- `public static final int VK_E`
- `public static final int VK_END`
- `public static final int VK_ENTER`
- `public static final int VK_EQUALS`
- `public static final int VK_ESCAPE`
- `public static final int VK_EURO_SIGN`
- `public static final int VK_EXCLAMATION_MARK`
- `public static final int VK_F`
- `public static final int VK_F1`
- `public static final int VK_F10`
- `public static final int VK_F11`
- `public static final int VK_F12`
- `public static final int VK_F13`
- `public static final int VK_F14`
- `public static final int VK_F15`
- `public static final int VK_F16`
- `public static final int VK_F17`
- `public static final int VK_F18`
- `public static final int VK_F19`
- `public static final int VK_F2`
- `public static final int VK_F20`
- `public static final int VK_F21`
- `public static final int VK_F22`
- `public static final int VK_F23`
- `public static final int VK_F24`
- `public static final int VK_F3`
- `public static final int VK_F4`
- `public static final int VK_F5`
- `public static final int VK_F6`
- `public static final int VK_F7`
- `public static final int VK_F8`
- `public static final int VK_F9`
- `public static final int VK_FINAL`
- `public static final int VK_FIND`
- `public static final int VK_FULL_WIDTH`
- `public static final int VK_G`
- `public static final int VK_GREATER`
- `public static final int VK_H`
- `public static final int VK_HALF_WIDTH`
- `public static final int VK_HELP`
- `public static final int VK_HIRAGANA`
- `public static final int VK_HOME`
- `public static final int VK_I`
- `public static final int VK_INPUT_METHOD_ON_OFF`
- `public static final int VK_INSERT`
- `public static final int VK_INVERTED_EXCLAMATION_MARK`
- `public static final int VK_J`
- `public static final int VK_JAPANESE_HIRAGANA`
- `public static final int VK_JAPANESE_KATAKANA`
- `public static final int VK_JAPANESE_ROMAN`
- `public static final int VK_K`
- `public static final int VK_KANA`
- `public static final int VK_KANA_LOCK`
- `public static final int VK_KANJI`
- `public static final int VK_KATAKANA`
- `public static final int VK_KP_DOWN`
- `public static final int VK_KP_LEFT`
- `public static final int VK_KP_RIGHT`
- `public static final int VK_KP_UP`
- `public static final int VK_L`
- `public static final int VK_LEFT`
- `public static final int VK_LEFT_PARENTHESIS`
- `public static final int VK_LESS`
- `public static final int VK_M`
- `public static final int VK_META`
- `public static final int VK_MINUS`
- `public static final int VK_MODECHANGE`
- `public static final int VK_MULTIPLY`
- `public static final int VK_N`
- `public static final int VK_NONCONVERT`
- `public static final int VK_NUMBER_SIGN`
- `public static final int VK_NUMPAD0`
- `public static final int VK_NUMPAD1`
- `public static final int VK_NUMPAD2`
- `public static final int VK_NUMPAD3`
- `public static final int VK_NUMPAD4`
- `public static final int VK_NUMPAD5`
- `public static final int VK_NUMPAD6`
- `public static final int VK_NUMPAD7`
- `public static final int VK_NUMPAD8`
- `public static final int VK_NUMPAD9`
- `public static final int VK_NUM_LOCK`
- `public static final int VK_O`
- `public static final int VK_OPEN_BRACKET`
- `public static final int VK_P`
- `public static final int VK_PAGE_DOWN`
- `public static final int VK_PAGE_UP`
- `public static final int VK_PASTE`
- `public static final int VK_PAUSE`
- `public static final int VK_PERIOD`
- `public static final int VK_PLUS`
- `public static final int VK_PREVIOUS_CANDIDATE`
- `public static final int VK_PRINTSCREEN`
- `public static final int VK_PROPS`
- `public static final int VK_Q`
- `public static final int VK_QUOTE`
- `public static final int VK_QUOTEDBL`
- `public static final int VK_R`
- `public static final int VK_RIGHT`
- `public static final int VK_RIGHT_PARENTHESIS`
- `public static final int VK_ROMAN_CHARACTERS`
- `public static final int VK_S`
- `public static final int VK_SCROLL_LOCK`
- `public static final int VK_SEMICOLON`
- `public static final int VK_SEPARATER`
- `public static final int VK_SEPARATOR`
- `public static final int VK_SHIFT`
- `public static final int VK_SLASH`
- `public static final int VK_SPACE`
- `public static final int VK_STOP`
- `public static final int VK_SUBTRACT`
- `public static final int VK_T`
- `public static final int VK_TAB`
- `public static final int VK_U`
- `public static final int VK_UNDEFINED`
- `public static final int VK_UNDERSCORE`
- `public static final int VK_UNDO`
- `public static final int VK_UP`
- `public static final int VK_V`
- `public static final int VK_W`
- `public static final int VK_WINDOWS`
- `public static final int VK_X`
- `public static final int VK_Y`
- `public static final int VK_Z`

#### ✗ Missing Constructors

- `public java.awt.event.KeyEvent(java.awt.Component, int, long, int, int)`
- `public java.awt.event.KeyEvent(java.awt.Component, int, long, int, int, char)`
- `public java.awt.event.KeyEvent(java.awt.Component, int, long, int, int, char, int)`

---

### Class: `KeyListener` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 3 / 3 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract void keyPressed(java.awt.event.KeyEvent)`
- `public abstract void keyReleased(java.awt.event.KeyEvent)`
- `public abstract void keyTyped(java.awt.event.KeyEvent)`

---

### Class: `MouseEvent` ![Coverage](https://img.shields.io/badge/coverage-60.0%25-yellow)

**Coverage:** 18 / 30 (60.0%)

```
[██████████████████████████████░░░░░░░░░░░░░░░░░░░░] 60.0%
```

#### ✓ Implemented Methods

- `public boolean isPopupTrigger()`
- `public int getButton()`
- `public int getX()`
- `public int getY()`

#### ✗ Missing Methods

- `public int getClickCount()`
- `public int getModifiersEx()`
- `public int getXOnScreen()`
- `public int getYOnScreen()`
- `public java.awt.Point getLocationOnScreen()`
- `public java.awt.Point getPoint()`
- `public java.lang.String paramString()`
- `public static java.lang.String getMouseModifiersText(int)`
- `public void translatePoint(int, int)`

#### ✓ Implemented Fields

- `public static final int BUTTON1`
- `public static final int BUTTON2`
- `public static final int BUTTON3`
- `public static final int MOUSE_CLICKED`
- `public static final int MOUSE_DRAGGED`
- `public static final int MOUSE_ENTERED`
- `public static final int MOUSE_EXITED`
- `public static final int MOUSE_FIRST`
- `public static final int MOUSE_LAST`
- `public static final int MOUSE_MOVED`
- `public static final int MOUSE_PRESSED`
- `public static final int MOUSE_RELEASED`
- `public static final int MOUSE_WHEEL`
- `public static final int NOBUTTON`

#### ✗ Missing Constructors

- `public java.awt.event.MouseEvent(java.awt.Component, int, long, int, int, int, int, boolean)`
- `public java.awt.event.MouseEvent(java.awt.Component, int, long, int, int, int, int, boolean, int)`
- `public java.awt.event.MouseEvent(java.awt.Component, int, long, int, int, int, int, int, int, boolean, int)`

---

### Class: `MouseListener` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 5 / 5 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract void mouseClicked(java.awt.event.MouseEvent)`
- `public abstract void mouseEntered(java.awt.event.MouseEvent)`
- `public abstract void mouseExited(java.awt.event.MouseEvent)`
- `public abstract void mousePressed(java.awt.event.MouseEvent)`
- `public abstract void mouseReleased(java.awt.event.MouseEvent)`

---

### Class: `MouseMotionListener` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 2 / 2 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract void mouseDragged(java.awt.event.MouseEvent)`
- `public abstract void mouseMoved(java.awt.event.MouseEvent)`

---

### Class: `MouseWheelEvent` ![Coverage](https://img.shields.io/badge/coverage-54.5%25-yellow)

**Coverage:** 6 / 11 (54.5%)

```
[███████████████████████████░░░░░░░░░░░░░░░░░░░░░░░] 54.5%
```

#### ✓ Implemented Methods

- `public double getPreciseWheelRotation()`
- `public int getScrollAmount()`
- `public int getScrollType()`
- `public int getUnitsToScroll()`
- `public int getWheelRotation()`
- `public java.lang.String paramString()`

#### ✗ Missing Fields

- `public static final int WHEEL_BLOCK_SCROLL`
- `public static final int WHEEL_UNIT_SCROLL`

#### ✗ Missing Constructors

- `public java.awt.event.MouseWheelEvent(java.awt.Component, int, long, int, int, int, int, boolean, int, int, int)`
- `public java.awt.event.MouseWheelEvent(java.awt.Component, int, long, int, int, int, int, int, int, boolean, int, int, int)`
- `public java.awt.event.MouseWheelEvent(java.awt.Component, int, long, int, int, int, int, int, int, boolean, int, int, int, double)`

---

### Class: `MouseWheelListener` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 1 / 1 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract void mouseWheelMoved(java.awt.event.MouseWheelEvent)`

---

### Class: `PaintEvent` ![Coverage](https://img.shields.io/badge/coverage-87.5%25-green)

**Coverage:** 7 / 8 (87.5%)

```
[███████████████████████████████████████████░░░░░░░] 87.5%
```

#### ✓ Implemented Methods

- `public java.awt.Rectangle getUpdateRect()`
- `public void setUpdateRect(java.awt.Rectangle)`

#### ✗ Missing Methods

- `public java.lang.String paramString()`

#### ✓ Implemented Fields

- `public static final int PAINT`
- `public static final int PAINT_FIRST`
- `public static final int PAINT_LAST`
- `public static final int UPDATE`

#### ✓ Implemented Constructors

- `public java.awt.event.PaintEvent(java.awt.Component, int, java.awt.Rectangle)`

---

### Class: `WindowEvent` ![Coverage](https://img.shields.io/badge/coverage-0.0%25-red)

**Coverage:** 0 / 21 (0.0%)

```
[░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 0.0%
```

#### ✗ Missing Methods

- `public int getNewState()`
- `public int getOldState()`
- `public java.awt.Window getOppositeWindow()`
- `public java.awt.Window getWindow()`
- `public java.lang.String paramString()`

#### ✗ Missing Fields

- `public static final int WINDOW_ACTIVATED`
- `public static final int WINDOW_CLOSED`
- `public static final int WINDOW_CLOSING`
- `public static final int WINDOW_DEACTIVATED`
- `public static final int WINDOW_DEICONIFIED`
- `public static final int WINDOW_FIRST`
- `public static final int WINDOW_GAINED_FOCUS`
- `public static final int WINDOW_ICONIFIED`
- `public static final int WINDOW_LAST`
- `public static final int WINDOW_LOST_FOCUS`
- `public static final int WINDOW_OPENED`
- `public static final int WINDOW_STATE_CHANGED`

#### ✗ Missing Constructors

- `public java.awt.event.WindowEvent(java.awt.Window, int)`
- `public java.awt.event.WindowEvent(java.awt.Window, int, int, int)`
- `public java.awt.event.WindowEvent(java.awt.Window, int, java.awt.Window)`
- `public java.awt.event.WindowEvent(java.awt.Window, int, java.awt.Window, int, int)`

---

### Class: `WindowListener` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 7 / 7 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract void windowActivated(java.awt.event.WindowEvent)`
- `public abstract void windowClosed(java.awt.event.WindowEvent)`
- `public abstract void windowClosing(java.awt.event.WindowEvent)`
- `public abstract void windowDeactivated(java.awt.event.WindowEvent)`
- `public abstract void windowDeiconified(java.awt.event.WindowEvent)`
- `public abstract void windowIconified(java.awt.event.WindowEvent)`
- `public abstract void windowOpened(java.awt.event.WindowEvent)`

---

## Package: `java.awt.geom`

**Coverage:** 290 / 303 (95.7%)

```
[███████████████████████████████████████████████░░░] 95.7%
```

### Class: `AffineTransform` ![Coverage](https://img.shields.io/badge/coverage-98.7%25-green)

**Coverage:** 74 / 75 (98.7%)

```
[█████████████████████████████████████████████████░] 98.7%
```

#### ✓ Implemented Methods

- `public boolean equals(java.lang.Object)`
- `public boolean isIdentity()`
- `public double getDeterminant()`
- `public double getScaleX()`
- `public double getScaleY()`
- `public double getShearX()`
- `public double getShearY()`
- `public double getTranslateX()`
- `public double getTranslateY()`
- `public int getType()`
- `public int hashCode()`
- `public java.awt.geom.AffineTransform createInverse()`
- `public java.awt.geom.Point2D deltaTransform(java.awt.geom.Point2D, java.awt.geom.Point2D)`
- `public java.awt.geom.Point2D inverseTransform(java.awt.geom.Point2D, java.awt.geom.Point2D)`
- `public java.awt.geom.Point2D transform(java.awt.geom.Point2D, java.awt.geom.Point2D)`
- `public java.lang.Object clone()`
- `public java.lang.String toString()`
- `public static java.awt.geom.AffineTransform getQuadrantRotateInstance(int)`
- `public static java.awt.geom.AffineTransform getQuadrantRotateInstance(int, double, double)`
- `public static java.awt.geom.AffineTransform getRotateInstance(double)`
- `public static java.awt.geom.AffineTransform getRotateInstance(double, double)`
- `public static java.awt.geom.AffineTransform getRotateInstance(double, double, double)`
- `public static java.awt.geom.AffineTransform getRotateInstance(double, double, double, double)`
- `public static java.awt.geom.AffineTransform getScaleInstance(double, double)`
- `public static java.awt.geom.AffineTransform getShearInstance(double, double)`
- `public static java.awt.geom.AffineTransform getTranslateInstance(double, double)`
- `public void concatenate(java.awt.geom.AffineTransform)`
- `public void deltaTransform(double[], int, double[], int, int)`
- `public void getMatrix(double[])`
- `public void inverseTransform(double[], int, double[], int, int)`
- `public void invert()`
- `public void preConcatenate(java.awt.geom.AffineTransform)`
- `public void quadrantRotate(int)`
- `public void quadrantRotate(int, double, double)`
- `public void rotate(double)`
- `public void rotate(double, double)`
- `public void rotate(double, double, double)`
- `public void rotate(double, double, double, double)`
- `public void scale(double, double)`
- `public void setToIdentity()`
- `public void setToQuadrantRotation(int)`
- `public void setToQuadrantRotation(int, double, double)`
- `public void setToRotation(double)`
- `public void setToRotation(double, double)`
- `public void setToRotation(double, double, double)`
- `public void setToRotation(double, double, double, double)`
- `public void setToScale(double, double)`
- `public void setToShear(double, double)`
- `public void setToTranslation(double, double)`
- `public void setTransform(double, double, double, double, double, double)`
- `public void setTransform(java.awt.geom.AffineTransform)`
- `public void shear(double, double)`
- `public void transform(double[], int, double[], int, int)`
- `public void transform(double[], int, float[], int, int)`
- `public void transform(float[], int, double[], int, int)`
- `public void transform(float[], int, float[], int, int)`
- `public void transform(java.awt.geom.Point2D[], int, java.awt.geom.Point2D[], int, int)`
- `public void translate(double, double)`

#### ✗ Missing Methods

- `public java.awt.Shape createTransformedShape(java.awt.Shape)`

#### ✓ Implemented Fields

- `public static final int TYPE_FLIP`
- `public static final int TYPE_GENERAL_ROTATION`
- `public static final int TYPE_GENERAL_SCALE`
- `public static final int TYPE_GENERAL_TRANSFORM`
- `public static final int TYPE_IDENTITY`
- `public static final int TYPE_MASK_ROTATION`
- `public static final int TYPE_MASK_SCALE`
- `public static final int TYPE_QUADRANT_ROTATION`
- `public static final int TYPE_TRANSLATION`
- `public static final int TYPE_UNIFORM_SCALE`

#### ✓ Implemented Constructors

- `public java.awt.geom.AffineTransform()`
- `public java.awt.geom.AffineTransform(double, double, double, double, double, double)`
- `public java.awt.geom.AffineTransform(double[])`
- `public java.awt.geom.AffineTransform(float, float, float, float, float, float)`
- `public java.awt.geom.AffineTransform(float[])`
- `public java.awt.geom.AffineTransform(java.awt.geom.AffineTransform)`

---

### Class: `Dimension2D` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 6 / 6 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract double getHeight()`
- `public abstract double getWidth()`
- `public abstract void setSize(double, double)`
- `public java.lang.Object clone()`
- `public void setSize(java.awt.geom.Dimension2D)`

#### ✓ Implemented Constructors

- `protected java.awt.geom.Dimension2D()`

---

### Class: `FlatteningPathIterator` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 9 / 9 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public boolean isDone()`
- `public double getFlatness()`
- `public int currentSegment(double[])`
- `public int currentSegment(float[])`
- `public int getRecursionLimit()`
- `public int getWindingRule()`
- `public void next()`

#### ✓ Implemented Constructors

- `public java.awt.geom.FlatteningPathIterator(java.awt.geom.PathIterator, double)`
- `public java.awt.geom.FlatteningPathIterator(java.awt.geom.PathIterator, double, int)`

---

### Class: `Line2D` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 38 / 38 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract double getX1()`
- `public abstract double getX2()`
- `public abstract double getY1()`
- `public abstract double getY2()`
- `public abstract java.awt.geom.Point2D getP1()`
- `public abstract java.awt.geom.Point2D getP2()`
- `public abstract void setLine(double, double, double, double)`
- `public boolean contains(double, double)`
- `public boolean contains(double, double, double, double)`
- `public boolean contains(java.awt.geom.Point2D)`
- `public boolean contains(java.awt.geom.Rectangle2D)`
- `public boolean intersects(double, double, double, double)`
- `public boolean intersects(java.awt.geom.Rectangle2D)`
- `public boolean intersectsLine(double, double, double, double)`
- `public boolean intersectsLine(java.awt.geom.Line2D)`
- `public double ptLineDist(double, double)`
- `public double ptLineDist(java.awt.geom.Point2D)`
- `public double ptLineDistSq(double, double)`
- `public double ptLineDistSq(java.awt.geom.Point2D)`
- `public double ptSegDist(double, double)`
- `public double ptSegDist(java.awt.geom.Point2D)`
- `public double ptSegDistSq(double, double)`
- `public double ptSegDistSq(java.awt.geom.Point2D)`
- `public int relativeCCW(double, double)`
- `public int relativeCCW(java.awt.geom.Point2D)`
- `public java.awt.Rectangle getBounds()`
- `public java.awt.geom.PathIterator getPathIterator(java.awt.geom.AffineTransform)`
- `public java.awt.geom.PathIterator getPathIterator(java.awt.geom.AffineTransform, double)`
- `public java.lang.Object clone()`
- `public static boolean linesIntersect(double, double, double, double, double, double, double, double)`
- `public static double ptLineDist(double, double, double, double, double, double)`
- `public static double ptLineDistSq(double, double, double, double, double, double)`
- `public static double ptSegDist(double, double, double, double, double, double)`
- `public static double ptSegDistSq(double, double, double, double, double, double)`
- `public static int relativeCCW(double, double, double, double, double, double)`
- `public void setLine(java.awt.geom.Line2D)`
- `public void setLine(java.awt.geom.Point2D, java.awt.geom.Point2D)`

#### ✓ Implemented Constructors

- `protected java.awt.geom.Line2D()`

---

### Class: `Line2D$Double` ![Coverage](https://img.shields.io/badge/coverage-93.3%25-green)

**Coverage:** 14 / 15 (93.3%)

```
[██████████████████████████████████████████████░░░░] 93.3%
```

#### ✓ Implemented Methods

- `public double getX1()`
- `public double getX2()`
- `public double getY1()`
- `public double getY2()`
- `public java.awt.geom.Point2D getP1()`
- `public java.awt.geom.Point2D getP2()`
- `public java.awt.geom.Rectangle2D getBounds2D()`
- `public void setLine(double, double, double, double)`

#### ✓ Implemented Fields

- `public double x1`
- `public double x2`
- `public double y1`
- `public double y2`

#### ✓ Implemented Constructors

- `public java.awt.geom.Line2D$Double()`
- `public java.awt.geom.Line2D$Double(double, double, double, double)`

#### ✗ Missing Constructors

- `public java.awt.geom.Line2D$Double(java.awt.geom.Point2D, java.awt.geom.Point2D)`

---

### Class: `Line2D$Float` ![Coverage](https://img.shields.io/badge/coverage-87.5%25-green)

**Coverage:** 14 / 16 (87.5%)

```
[███████████████████████████████████████████░░░░░░░] 87.5%
```

#### ✓ Implemented Methods

- `public double getX1()`
- `public double getX2()`
- `public double getY1()`
- `public double getY2()`
- `public java.awt.geom.Point2D getP1()`
- `public java.awt.geom.Point2D getP2()`
- `public java.awt.geom.Rectangle2D getBounds2D()`
- `public void setLine(double, double, double, double)`

#### ✗ Missing Methods

- `public void setLine(float, float, float, float)`

#### ✓ Implemented Fields

- `public float x1`
- `public float x2`
- `public float y1`
- `public float y2`

#### ✓ Implemented Constructors

- `public java.awt.geom.Line2D$Float()`
- `public java.awt.geom.Line2D$Float(float, float, float, float)`

#### ✗ Missing Constructors

- `public java.awt.geom.Line2D$Float(java.awt.geom.Point2D, java.awt.geom.Point2D)`

---

### Class: `LineIterator` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 5 / 5 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public boolean isDone()`
- `public int currentSegment(double[])`
- `public int currentSegment(float[])`
- `public int getWindingRule()`
- `public void next()`

---

### Class: `NoninvertibleTransformException` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 1 / 1 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Constructors

- `public java.awt.geom.NoninvertibleTransformException(java.lang.String)`

---

### Class: `PathIterator` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 12 / 12 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract boolean isDone()`
- `public abstract int currentSegment(double[])`
- `public abstract int currentSegment(float[])`
- `public abstract int getWindingRule()`
- `public abstract void next()`

#### ✓ Implemented Fields

- `public static final int SEG_CLOSE`
- `public static final int SEG_CUBICTO`
- `public static final int SEG_LINETO`
- `public static final int SEG_MOVETO`
- `public static final int SEG_QUADTO`
- `public static final int WIND_EVEN_ODD`
- `public static final int WIND_NON_ZERO`

---

### Class: `Point2D` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 14 / 14 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract double getX()`
- `public abstract double getY()`
- `public abstract void setLocation(double, double)`
- `public boolean equals(java.lang.Object)`
- `public double distance(double, double)`
- `public double distance(java.awt.geom.Point2D)`
- `public double distanceSq(double, double)`
- `public double distanceSq(java.awt.geom.Point2D)`
- `public int hashCode()`
- `public java.lang.Object clone()`
- `public static double distance(double, double, double, double)`
- `public static double distanceSq(double, double, double, double)`
- `public void setLocation(java.awt.geom.Point2D)`

#### ✓ Implemented Constructors

- `protected java.awt.geom.Point2D()`

---

### Class: `Point2D$Double` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 8 / 8 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public double getX()`
- `public double getY()`
- `public java.lang.String toString()`
- `public void setLocation(double, double)`

#### ✓ Implemented Fields

- `public double x`
- `public double y`

#### ✓ Implemented Constructors

- `public java.awt.geom.Point2D$Double()`
- `public java.awt.geom.Point2D$Double(double, double)`

---

### Class: `Point2D$Float` ![Coverage](https://img.shields.io/badge/coverage-66.7%25-yellow)

**Coverage:** 6 / 9 (66.7%)

```
[█████████████████████████████████░░░░░░░░░░░░░░░░░] 66.7%
```

#### ✓ Implemented Methods

- `public double getX()`
- `public double getY()`
- `public java.lang.String toString()`
- `public void setLocation(double, double)`

#### ✗ Missing Methods

- `public void setLocation(float, float)`

#### ✗ Missing Fields

- `public float x`
- `public float y`

#### ✓ Implemented Constructors

- `public java.awt.geom.Point2D$Float()`
- `public java.awt.geom.Point2D$Float(float, float)`

---

### Class: `RectIterator` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 5 / 5 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public boolean isDone()`
- `public int currentSegment(double[])`
- `public int currentSegment(float[])`
- `public int getWindingRule()`
- `public void next()`

---

### Class: `Rectangle2D` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 27 / 27 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract int outcode(double, double)`
- `public abstract java.awt.geom.Rectangle2D createIntersection(java.awt.geom.Rectangle2D)`
- `public abstract java.awt.geom.Rectangle2D createUnion(java.awt.geom.Rectangle2D)`
- `public abstract void setRect(double, double, double, double)`
- `public boolean contains(double, double)`
- `public boolean contains(double, double, double, double)`
- `public boolean equals(java.lang.Object)`
- `public boolean intersects(double, double, double, double)`
- `public boolean intersectsLine(double, double, double, double)`
- `public boolean intersectsLine(java.awt.geom.Line2D)`
- `public int hashCode()`
- `public int outcode(java.awt.geom.Point2D)`
- `public java.awt.geom.PathIterator getPathIterator(java.awt.geom.AffineTransform)`
- `public java.awt.geom.PathIterator getPathIterator(java.awt.geom.AffineTransform, double)`
- `public java.awt.geom.Rectangle2D getBounds2D()`
- `public static void intersect(java.awt.geom.Rectangle2D, java.awt.geom.Rectangle2D, java.awt.geom.Rectangle2D)`
- `public static void union(java.awt.geom.Rectangle2D, java.awt.geom.Rectangle2D, java.awt.geom.Rectangle2D)`
- `public void add(double, double)`
- `public void add(java.awt.geom.Point2D)`
- `public void add(java.awt.geom.Rectangle2D)`
- `public void setFrame(double, double, double, double)`
- `public void setRect(java.awt.geom.Rectangle2D)`

#### ✓ Implemented Fields

- `public static final int OUT_BOTTOM`
- `public static final int OUT_LEFT`
- `public static final int OUT_RIGHT`
- `public static final int OUT_TOP`

#### ✓ Implemented Constructors

- `protected java.awt.geom.Rectangle2D()`

---

### Class: `Rectangle2D$Double` ![Coverage](https://img.shields.io/badge/coverage-88.9%25-green)

**Coverage:** 16 / 18 (88.9%)

```
[████████████████████████████████████████████░░░░░░] 88.9%
```

#### ✓ Implemented Methods

- `public boolean isEmpty()`
- `public double getHeight()`
- `public double getWidth()`
- `public double getX()`
- `public double getY()`
- `public int outcode(double, double)`
- `public java.awt.geom.Rectangle2D createIntersection(java.awt.geom.Rectangle2D)`
- `public java.awt.geom.Rectangle2D createUnion(java.awt.geom.Rectangle2D)`
- `public java.lang.String toString()`
- `public void setRect(double, double, double, double)`

#### ✗ Missing Methods

- `public java.awt.geom.Rectangle2D getBounds2D()`
- `public void setRect(java.awt.geom.Rectangle2D)`

#### ✓ Implemented Fields

- `public double height`
- `public double width`
- `public double x`
- `public double y`

#### ✓ Implemented Constructors

- `public java.awt.geom.Rectangle2D$Double()`
- `public java.awt.geom.Rectangle2D$Double(double, double, double, double)`

---

### Class: `Rectangle2D$Float` ![Coverage](https://img.shields.io/badge/coverage-84.2%25-green)

**Coverage:** 16 / 19 (84.2%)

```
[██████████████████████████████████████████░░░░░░░░] 84.2%
```

#### ✓ Implemented Methods

- `public boolean isEmpty()`
- `public double getHeight()`
- `public double getWidth()`
- `public double getX()`
- `public double getY()`
- `public int outcode(double, double)`
- `public java.awt.geom.Rectangle2D createIntersection(java.awt.geom.Rectangle2D)`
- `public java.awt.geom.Rectangle2D createUnion(java.awt.geom.Rectangle2D)`
- `public java.lang.String toString()`
- `public void setRect(double, double, double, double)`

#### ✗ Missing Methods

- `public java.awt.geom.Rectangle2D getBounds2D()`
- `public void setRect(float, float, float, float)`
- `public void setRect(java.awt.geom.Rectangle2D)`

#### ✓ Implemented Fields

- `public float height`
- `public float width`
- `public float x`
- `public float y`

#### ✓ Implemented Constructors

- `public java.awt.geom.Rectangle2D$Float()`
- `public java.awt.geom.Rectangle2D$Float(float, float, float, float)`

---

### Class: `RectangularShape` ![Coverage](https://img.shields.io/badge/coverage-96.2%25-green)

**Coverage:** 25 / 26 (96.2%)

```
[████████████████████████████████████████████████░░] 96.2%
```

#### ✓ Implemented Methods

- `public abstract boolean isEmpty()`
- `public abstract double getHeight()`
- `public abstract double getWidth()`
- `public abstract double getX()`
- `public abstract double getY()`
- `public abstract void setFrame(double, double, double, double)`
- `public boolean contains(java.awt.geom.Point2D)`
- `public boolean contains(java.awt.geom.Rectangle2D)`
- `public boolean intersects(java.awt.geom.Rectangle2D)`
- `public double getCenterX()`
- `public double getCenterY()`
- `public double getMaxX()`
- `public double getMaxY()`
- `public double getMinX()`
- `public double getMinY()`
- `public java.awt.Rectangle getBounds()`
- `public java.awt.geom.Rectangle2D getFrame()`
- `public java.lang.Object clone()`
- `public void setFrame(java.awt.geom.Point2D, java.awt.geom.Dimension2D)`
- `public void setFrame(java.awt.geom.Rectangle2D)`
- `public void setFrameFromCenter(double, double, double, double)`
- `public void setFrameFromCenter(java.awt.geom.Point2D, java.awt.geom.Point2D)`
- `public void setFrameFromDiagonal(double, double, double, double)`
- `public void setFrameFromDiagonal(java.awt.geom.Point2D, java.awt.geom.Point2D)`

#### ✗ Missing Methods

- `public java.awt.geom.PathIterator getPathIterator(java.awt.geom.AffineTransform, double)`

#### ✓ Implemented Constructors

- `protected java.awt.geom.RectangularShape()`

---

## Package: `java.awt.image`

**Coverage:** 240 / 464 (51.7%)

```
[█████████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░] 51.7%
```

### Class: `BufferedImage` ![Coverage](https://img.shields.io/badge/coverage-32.8%25-orange)

**Coverage:** 21 / 64 (32.8%)

```
[████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 32.8%
```

#### ✓ Implemented Methods

- `public boolean isAlphaPremultiplied()`
- `public int getHeight()`
- `public int getHeight(java.awt.image.ImageObserver)`
- `public int getRGB(int, int)`
- `public int getWidth()`
- `public int getWidth(java.awt.image.ImageObserver)`
- `public int[] getRGB(int, int, int, int, int[], int, int)`
- `public java.awt.Graphics getGraphics()`
- `public java.awt.image.ColorModel getColorModel()`
- `public java.awt.image.ImageProducer getSource()`
- `public java.awt.image.WritableRaster getRaster()`
- `public java.lang.Object getProperty(java.lang.String, java.awt.image.ImageObserver)`
- `public void setRGB(int, int, int)`
- `public void setRGB(int, int, int, int, int[], int, int)`

#### ✗ Missing Methods

- `public boolean hasTileWriters()`
- `public boolean isTileWritable(int, int)`
- `public int getMinTileX()`
- `public int getMinTileY()`
- `public int getMinX()`
- `public int getMinY()`
- `public int getNumXTiles()`
- `public int getNumYTiles()`
- `public int getTileGridXOffset()`
- `public int getTileGridYOffset()`
- `public int getTileHeight()`
- `public int getTileWidth()`
- `public int getTransparency()`
- `public int getType()`
- `public java.awt.Graphics2D createGraphics()`
- `public java.awt.Point[] getWritableTileIndices()`
- `public java.awt.image.BufferedImage getSubimage(int, int, int, int)`
- `public java.awt.image.Raster getData()`
- `public java.awt.image.Raster getData(java.awt.Rectangle)`
- `public java.awt.image.Raster getTile(int, int)`
- `public java.awt.image.SampleModel getSampleModel()`
- `public java.awt.image.WritableRaster copyData(java.awt.image.WritableRaster)`
- `public java.awt.image.WritableRaster getAlphaRaster()`
- `public java.awt.image.WritableRaster getWritableTile(int, int)`
- `public java.lang.Object getProperty(java.lang.String)`
- `public java.lang.String toString()`
- `public java.lang.String[] getPropertyNames()`
- `public java.util.Vector getSources()`
- `public void addTileObserver(java.awt.image.TileObserver)`
- `public void coerceData(boolean)`
- `public void releaseWritableTile(int, int)`
- `public void removeTileObserver(java.awt.image.TileObserver)`
- `public void setData(java.awt.image.Raster)`

#### ✓ Implemented Fields

- `public static final int TYPE_CUSTOM`
- `public static final int TYPE_INT_ARGB`
- `public static final int TYPE_INT_ARGB_PRE`
- `public static final int TYPE_INT_BGR`
- `public static final int TYPE_INT_RGB`

#### ✗ Missing Fields

- `public static final int TYPE_3BYTE_BGR`
- `public static final int TYPE_4BYTE_ABGR`
- `public static final int TYPE_4BYTE_ABGR_PRE`
- `public static final int TYPE_BYTE_BINARY`
- `public static final int TYPE_BYTE_GRAY`
- `public static final int TYPE_BYTE_INDEXED`
- `public static final int TYPE_USHORT_555_RGB`
- `public static final int TYPE_USHORT_565_RGB`
- `public static final int TYPE_USHORT_GRAY`

#### ✓ Implemented Constructors

- `public java.awt.image.BufferedImage(int, int, int)`
- `public java.awt.image.BufferedImage(java.awt.image.ColorModel, java.awt.image.WritableRaster, boolean, java.util.Hashtable)`

#### ✗ Missing Constructors

- `public java.awt.image.BufferedImage(int, int, int, java.awt.image.IndexColorModel)`

---

### Class: `ColorModel` ![Coverage](https://img.shields.io/badge/coverage-33.3%25-orange)

**Coverage:** 15 / 45 (33.3%)

```
[████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 33.3%
```

#### ✓ Implemented Methods

- `public abstract int getAlpha(int)`
- `public abstract int getBlue(int)`
- `public abstract int getGreen(int)`
- `public abstract int getRed(int)`
- `public int getDataElement(int[], int)`
- `public int getNumColorComponents()`
- `public int getNumComponents()`
- `public int getPixelSize()`
- `public int getRGB(int)`
- `public int getTransparency()`
- `public int[] getComponents(int, int[], int)`
- `public java.awt.image.WritableRaster createCompatibleWritableRaster(int, int)`
- `public java.lang.Object getDataElements(int, java.lang.Object)`
- `public static java.awt.image.ColorModel getRGBdefault()`

#### ✗ Missing Methods

- `public boolean equals(java.lang.Object)`
- `public boolean isCompatibleRaster(java.awt.image.Raster)`
- `public boolean isCompatibleSampleModel(java.awt.image.SampleModel)`
- `public final boolean hasAlpha()`
- `public final boolean isAlphaPremultiplied()`
- `public final int getTransferType()`
- `public final java.awt.color.ColorSpace getColorSpace()`
- `public float[] getNormalizedComponents(int[], int, float[], int)`
- `public float[] getNormalizedComponents(java.lang.Object, float[], int)`
- `public int getAlpha(java.lang.Object)`
- `public int getBlue(java.lang.Object)`
- `public int getComponentSize(int)`
- `public int getDataElement(float[], int)`
- `public int getGreen(java.lang.Object)`
- `public int getRGB(java.lang.Object)`
- `public int getRed(java.lang.Object)`
- `public int hashCode()`
- `public int[] getComponentSize()`
- `public int[] getComponents(java.lang.Object, int[], int)`
- `public int[] getUnnormalizedComponents(float[], int, int[], int)`
- `public java.awt.image.ColorModel coerceData(java.awt.image.WritableRaster, boolean)`
- `public java.awt.image.SampleModel createCompatibleSampleModel(int, int)`
- `public java.awt.image.WritableRaster getAlphaRaster(java.awt.image.WritableRaster)`
- `public java.lang.Object getDataElements(float[], int, java.lang.Object)`
- `public java.lang.Object getDataElements(int[], int, java.lang.Object)`
- `public java.lang.String toString()`
- `public void finalize()`

#### ✓ Implemented Fields

- `protected int transferType`

#### ✗ Missing Fields

- `protected int pixel_bits`

#### ✗ Missing Constructors

- `protected java.awt.image.ColorModel(int, int[], java.awt.color.ColorSpace, boolean, boolean, int, int)`
- `public java.awt.image.ColorModel(int)`

---

### Class: `DataBuffer` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 34 / 34 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract int getElem(int, int)`
- `public abstract void setElem(int, int, int)`
- `public double getElemDouble(int)`
- `public double getElemDouble(int, int)`
- `public float getElemFloat(int)`
- `public float getElemFloat(int, int)`
- `public int getDataType()`
- `public int getElem(int)`
- `public int getNumBanks()`
- `public int getOffset()`
- `public int getSize()`
- `public int[] getOffsets()`
- `public static int getDataTypeSize(int)`
- `public void setElem(int, int)`
- `public void setElemDouble(int, double)`
- `public void setElemDouble(int, int, double)`
- `public void setElemFloat(int, float)`
- `public void setElemFloat(int, int, float)`

#### ✓ Implemented Fields

- `protected int banks`
- `protected int dataType`
- `protected int offset`
- `protected int size`
- `protected int[] offsets`
- `public static final int TYPE_BYTE`
- `public static final int TYPE_DOUBLE`
- `public static final int TYPE_FLOAT`
- `public static final int TYPE_INT`
- `public static final int TYPE_SHORT`
- `public static final int TYPE_UNDEFINED`
- `public static final int TYPE_USHORT`

#### ✓ Implemented Constructors

- `protected java.awt.image.DataBuffer(int, int)`
- `protected java.awt.image.DataBuffer(int, int, int)`
- `protected java.awt.image.DataBuffer(int, int, int, int)`
- `protected java.awt.image.DataBuffer(int, int, int, int[])`

---

### Class: `DataBufferByte` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 13 / 13 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public byte[] getData()`
- `public byte[] getData(int)`
- `public byte[][] getBankData()`
- `public int getElem(int)`
- `public int getElem(int, int)`
- `public void setElem(int, int)`
- `public void setElem(int, int, int)`

#### ✓ Implemented Constructors

- `public java.awt.image.DataBufferByte(byte[], int)`
- `public java.awt.image.DataBufferByte(byte[], int, int)`
- `public java.awt.image.DataBufferByte(byte[][], int)`
- `public java.awt.image.DataBufferByte(byte[][], int, int[])`
- `public java.awt.image.DataBufferByte(int)`
- `public java.awt.image.DataBufferByte(int, int)`

---

### Class: `DataBufferDouble` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 21 / 21 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public double getElemDouble(int)`
- `public double getElemDouble(int, int)`
- `public double[] getData()`
- `public double[] getData(int)`
- `public double[][] getBankData()`
- `public float getElemFloat(int)`
- `public float getElemFloat(int, int)`
- `public int getElem(int)`
- `public int getElem(int, int)`
- `public void setElem(int, int)`
- `public void setElem(int, int, int)`
- `public void setElemDouble(int, double)`
- `public void setElemDouble(int, int, double)`
- `public void setElemFloat(int, float)`
- `public void setElemFloat(int, int, float)`

#### ✓ Implemented Constructors

- `public java.awt.image.DataBufferDouble(double[], int)`
- `public java.awt.image.DataBufferDouble(double[], int, int)`
- `public java.awt.image.DataBufferDouble(double[][], int)`
- `public java.awt.image.DataBufferDouble(double[][], int, int[])`
- `public java.awt.image.DataBufferDouble(int)`
- `public java.awt.image.DataBufferDouble(int, int)`

---

### Class: `DataBufferFloat` ![Coverage](https://img.shields.io/badge/coverage-81.0%25-green)

**Coverage:** 17 / 21 (81.0%)

```
[████████████████████████████████████████░░░░░░░░░░] 81.0%
```

#### ✓ Implemented Methods

- `public float getElemFloat(int)`
- `public float getElemFloat(int, int)`
- `public float[] getData()`
- `public float[] getData(int)`
- `public float[][] getBankData()`
- `public int getElem(int)`
- `public int getElem(int, int)`
- `public void setElem(int, int)`
- `public void setElem(int, int, int)`
- `public void setElemFloat(int, float)`
- `public void setElemFloat(int, int, float)`

#### ✗ Missing Methods

- `public double getElemDouble(int)`
- `public double getElemDouble(int, int)`
- `public void setElemDouble(int, double)`
- `public void setElemDouble(int, int, double)`

#### ✓ Implemented Constructors

- `public java.awt.image.DataBufferFloat(float[], int)`
- `public java.awt.image.DataBufferFloat(float[], int, int)`
- `public java.awt.image.DataBufferFloat(float[][], int)`
- `public java.awt.image.DataBufferFloat(float[][], int, int[])`
- `public java.awt.image.DataBufferFloat(int)`
- `public java.awt.image.DataBufferFloat(int, int)`

---

### Class: `DataBufferInt` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 13 / 13 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public int getElem(int)`
- `public int getElem(int, int)`
- `public int[] getData()`
- `public int[] getData(int)`
- `public int[][] getBankData()`
- `public void setElem(int, int)`
- `public void setElem(int, int, int)`

#### ✓ Implemented Constructors

- `public java.awt.image.DataBufferInt(int)`
- `public java.awt.image.DataBufferInt(int, int)`
- `public java.awt.image.DataBufferInt(int[], int)`
- `public java.awt.image.DataBufferInt(int[], int, int)`
- `public java.awt.image.DataBufferInt(int[][], int)`
- `public java.awt.image.DataBufferInt(int[][], int, int[])`

---

### Class: `DataBufferShort` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 13 / 13 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public int getElem(int)`
- `public int getElem(int, int)`
- `public short[] getData()`
- `public short[] getData(int)`
- `public short[][] getBankData()`
- `public void setElem(int, int)`
- `public void setElem(int, int, int)`

#### ✓ Implemented Constructors

- `public java.awt.image.DataBufferShort(int)`
- `public java.awt.image.DataBufferShort(int, int)`
- `public java.awt.image.DataBufferShort(short[], int)`
- `public java.awt.image.DataBufferShort(short[], int, int)`
- `public java.awt.image.DataBufferShort(short[][], int)`
- `public java.awt.image.DataBufferShort(short[][], int, int[])`

---

### Class: `DataBufferUShort` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 13 / 13 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public int getElem(int)`
- `public int getElem(int, int)`
- `public short[] getData()`
- `public short[] getData(int)`
- `public short[][] getBankData()`
- `public void setElem(int, int)`
- `public void setElem(int, int, int)`

#### ✓ Implemented Constructors

- `public java.awt.image.DataBufferUShort(int)`
- `public java.awt.image.DataBufferUShort(int, int)`
- `public java.awt.image.DataBufferUShort(short[], int)`
- `public java.awt.image.DataBufferUShort(short[], int, int)`
- `public java.awt.image.DataBufferUShort(short[][], int)`
- `public java.awt.image.DataBufferUShort(short[][], int, int[])`

---

### Class: `DirectColorModel` ![Coverage](https://img.shields.io/badge/coverage-15.4%25-red)

**Coverage:** 4 / 26 (15.4%)

```
[███████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 15.4%
```

#### ✓ Implemented Methods

- `public int getDataElement(int[], int)`
- `public java.lang.Object getDataElements(int, java.lang.Object)`

#### ✗ Missing Methods

- `public boolean isCompatibleRaster(java.awt.image.Raster)`
- `public final int getAlpha(int)`
- `public final int getAlphaMask()`
- `public final int getBlue(int)`
- `public final int getBlueMask()`
- `public final int getGreen(int)`
- `public final int getGreenMask()`
- `public final int getRGB(int)`
- `public final int getRed(int)`
- `public final int getRedMask()`
- `public final int[] getComponents(int, int[], int)`
- `public final int[] getComponents(java.lang.Object, int[], int)`
- `public final java.awt.image.ColorModel coerceData(java.awt.image.WritableRaster, boolean)`
- `public final java.awt.image.WritableRaster createCompatibleWritableRaster(int, int)`
- `public int getAlpha(java.lang.Object)`
- `public int getBlue(java.lang.Object)`
- `public int getGreen(java.lang.Object)`
- `public int getRGB(java.lang.Object)`
- `public int getRed(java.lang.Object)`
- `public java.lang.Object getDataElements(int[], int, java.lang.Object)`
- `public java.lang.String toString()`

#### ✓ Implemented Constructors

- `public java.awt.image.DirectColorModel(int, int, int, int)`
- `public java.awt.image.DirectColorModel(int, int, int, int, int)`

#### ✗ Missing Constructors

- `public java.awt.image.DirectColorModel(java.awt.color.ColorSpace, int, int, int, int, int, boolean, int)`

---

### Class: `ImageConsumer` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 16 / 16 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract void imageComplete(int)`
- `public abstract void setColorModel(java.awt.image.ColorModel)`
- `public abstract void setDimensions(int, int)`
- `public abstract void setHints(int)`
- `public abstract void setPixels(int, int, int, int, java.awt.image.ColorModel, byte[], int, int)`
- `public abstract void setPixels(int, int, int, int, java.awt.image.ColorModel, int[], int, int)`
- `public abstract void setProperties(java.util.Hashtable)`

#### ✓ Implemented Fields

- `public static final int COMPLETESCANLINES`
- `public static final int IMAGEABORTED`
- `public static final int IMAGEERROR`
- `public static final int RANDOMPIXELORDER`
- `public static final int SINGLEFRAME`
- `public static final int SINGLEFRAMEDONE`
- `public static final int SINGLEPASS`
- `public static final int STATICIMAGEDONE`
- `public static final int TOPDOWNLEFTRIGHT`

---

### Class: `ImageObserver` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 9 / 9 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract boolean imageUpdate(java.awt.Image, int, int, int, int, int)`

#### ✓ Implemented Fields

- `public static final int ABORT`
- `public static final int ALLBITS`
- `public static final int ERROR`
- `public static final int FRAMEBITS`
- `public static final int HEIGHT`
- `public static final int PROPERTIES`
- `public static final int SOMEBITS`
- `public static final int WIDTH`

---

### Class: `ImageProducer` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 5 / 5 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract boolean isConsumer(java.awt.image.ImageConsumer)`
- `public abstract void addConsumer(java.awt.image.ImageConsumer)`
- `public abstract void removeConsumer(java.awt.image.ImageConsumer)`
- `public abstract void requestTopDownLeftRightResend(java.awt.image.ImageConsumer)`
- `public abstract void startProduction(java.awt.image.ImageConsumer)`

---

### Class: `PixelGrabber` ![Coverage](https://img.shields.io/badge/coverage-10.0%25-red)

**Coverage:** 2 / 20 (10.0%)

```
[█████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 10.0%
```

#### ✓ Implemented Methods

- `public boolean grabPixels()`

#### ✗ Missing Methods

- `public boolean grabPixels(long)`
- `public int getHeight()`
- `public int getStatus()`
- `public int getWidth()`
- `public int status()`
- `public java.awt.image.ColorModel getColorModel()`
- `public java.lang.Object getPixels()`
- `public void abortGrabbing()`
- `public void imageComplete(int)`
- `public void setColorModel(java.awt.image.ColorModel)`
- `public void setDimensions(int, int)`
- `public void setHints(int)`
- `public void setPixels(int, int, int, int, java.awt.image.ColorModel, byte[], int, int)`
- `public void setPixels(int, int, int, int, java.awt.image.ColorModel, int[], int, int)`
- `public void setProperties(java.util.Hashtable)`
- `public void startGrabbing()`

#### ✓ Implemented Constructors

- `public java.awt.image.PixelGrabber(java.awt.Image, int, int, int, int, int[], int, int)`

#### ✗ Missing Constructors

- `public java.awt.image.PixelGrabber(java.awt.Image, int, int, int, int, boolean)`
- `public java.awt.image.PixelGrabber(java.awt.image.ImageProducer, int, int, int, int, int[], int, int)`

---

### Class: `Raster` ![Coverage](https://img.shields.io/badge/coverage-23.3%25-red)

**Coverage:** 14 / 60 (23.3%)

```
[███████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 23.3%
```

#### ✓ Implemented Methods

- `public int getSample(int, int, int)`
- `public int[] getPixel(int, int, int[])`
- `public java.awt.Rectangle getBounds()`
- `public java.awt.image.Raster createChild(int, int, int, int, int, int, int[])`
- `public java.awt.image.Raster createTranslatedChild(int, int)`
- `public java.awt.image.SampleModel getSampleModel()`
- `public java.lang.Object getDataElements(int, int, java.lang.Object)`

#### ✗ Missing Methods

- `public double getSampleDouble(int, int, int)`
- `public double[] getPixel(int, int, double[])`
- `public double[] getPixels(int, int, int, int, double[])`
- `public double[] getSamples(int, int, int, int, int, double[])`
- `public final int getHeight()`
- `public final int getMinX()`
- `public final int getMinY()`
- `public final int getNumBands()`
- `public final int getNumDataElements()`
- `public final int getSampleModelTranslateX()`
- `public final int getSampleModelTranslateY()`
- `public final int getTransferType()`
- `public final int getWidth()`
- `public float getSampleFloat(int, int, int)`
- `public float[] getPixel(int, int, float[])`
- `public float[] getPixels(int, int, int, int, float[])`
- `public float[] getSamples(int, int, int, int, int, float[])`
- `public int[] getPixels(int, int, int, int, int[])`
- `public int[] getSamples(int, int, int, int, int, int[])`
- `public java.awt.image.DataBuffer getDataBuffer()`
- `public java.awt.image.Raster getParent()`
- `public java.awt.image.WritableRaster createCompatibleWritableRaster()`
- `public java.awt.image.WritableRaster createCompatibleWritableRaster(int, int)`
- `public java.awt.image.WritableRaster createCompatibleWritableRaster(int, int, int, int)`
- `public java.awt.image.WritableRaster createCompatibleWritableRaster(java.awt.Rectangle)`
- `public java.lang.Object getDataElements(int, int, int, int, java.lang.Object)`
- `public static java.awt.image.Raster createRaster(java.awt.image.SampleModel, java.awt.image.DataBuffer, java.awt.Point)`
- `public static java.awt.image.WritableRaster createBandedRaster(int, int, int, int, int[], int[], java.awt.Point)`
- `public static java.awt.image.WritableRaster createBandedRaster(int, int, int, int, java.awt.Point)`
- `public static java.awt.image.WritableRaster createBandedRaster(java.awt.image.DataBuffer, int, int, int, int[], int[], java.awt.Point)`
- `public static java.awt.image.WritableRaster createInterleavedRaster(int, int, int, int, int, int[], java.awt.Point)`
- `public static java.awt.image.WritableRaster createInterleavedRaster(int, int, int, int, java.awt.Point)`
- `public static java.awt.image.WritableRaster createInterleavedRaster(java.awt.image.DataBuffer, int, int, int, int, int[], java.awt.Point)`
- `public static java.awt.image.WritableRaster createPackedRaster(int, int, int, int, int, java.awt.Point)`
- `public static java.awt.image.WritableRaster createPackedRaster(int, int, int, int[], java.awt.Point)`
- `public static java.awt.image.WritableRaster createPackedRaster(java.awt.image.DataBuffer, int, int, int, int[], java.awt.Point)`
- `public static java.awt.image.WritableRaster createPackedRaster(java.awt.image.DataBuffer, int, int, int, java.awt.Point)`
- `public static java.awt.image.WritableRaster createWritableRaster(java.awt.image.SampleModel, java.awt.Point)`
- `public static java.awt.image.WritableRaster createWritableRaster(java.awt.image.SampleModel, java.awt.image.DataBuffer, java.awt.Point)`

#### ✓ Implemented Fields

- `protected int height`
- `protected int minX`
- `protected int minY`
- `protected int sampleModelTranslateX`
- `protected int sampleModelTranslateY`
- `protected int width`
- `protected java.awt.image.SampleModel sampleModel`

#### ✗ Missing Fields

- `protected int numBands`
- `protected int numDataElements`
- `protected java.awt.image.DataBuffer dataBuffer`
- `protected java.awt.image.Raster parent`

#### ✗ Missing Constructors

- `protected java.awt.image.Raster(java.awt.image.SampleModel, java.awt.Point)`
- `protected java.awt.image.Raster(java.awt.image.SampleModel, java.awt.image.DataBuffer, java.awt.Point)`
- `protected java.awt.image.Raster(java.awt.image.SampleModel, java.awt.image.DataBuffer, java.awt.Rectangle, java.awt.Point, java.awt.image.Raster)`

---

### Class: `SampleModel` ![Coverage](https://img.shields.io/badge/coverage-25.0%25-orange)

**Coverage:** 11 / 44 (25.0%)

```
[████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 25.0%
```

#### ✓ Implemented Methods

- `public abstract int getNumDataElements()`
- `public abstract int getSample(int, int, int, java.awt.image.DataBuffer)`
- `public abstract java.awt.image.DataBuffer createDataBuffer()`
- `public abstract java.awt.image.SampleModel createCompatibleSampleModel(int, int)`
- `public abstract java.lang.Object getDataElements(int, int, java.lang.Object, java.awt.image.DataBuffer)`
- `public abstract void setDataElements(int, int, java.lang.Object, java.awt.image.DataBuffer)`
- `public abstract void setSample(int, int, int, int, java.awt.image.DataBuffer)`

#### ✗ Missing Methods

- `public abstract int getSampleSize(int)`
- `public abstract int[] getSampleSize()`
- `public abstract java.awt.image.SampleModel createSubsetSampleModel(int[])`
- `public double getSampleDouble(int, int, int, java.awt.image.DataBuffer)`
- `public double[] getPixel(int, int, double[], java.awt.image.DataBuffer)`
- `public double[] getPixels(int, int, int, int, double[], java.awt.image.DataBuffer)`
- `public double[] getSamples(int, int, int, int, int, double[], java.awt.image.DataBuffer)`
- `public final int getDataType()`
- `public final int getHeight()`
- `public final int getNumBands()`
- `public final int getWidth()`
- `public float getSampleFloat(int, int, int, java.awt.image.DataBuffer)`
- `public float[] getPixel(int, int, float[], java.awt.image.DataBuffer)`
- `public float[] getPixels(int, int, int, int, float[], java.awt.image.DataBuffer)`
- `public float[] getSamples(int, int, int, int, int, float[], java.awt.image.DataBuffer)`
- `public int getTransferType()`
- `public int[] getPixel(int, int, int[], java.awt.image.DataBuffer)`
- `public int[] getPixels(int, int, int, int, int[], java.awt.image.DataBuffer)`
- `public int[] getSamples(int, int, int, int, int, int[], java.awt.image.DataBuffer)`
- `public java.lang.Object getDataElements(int, int, int, int, java.lang.Object, java.awt.image.DataBuffer)`
- `public void setDataElements(int, int, int, int, java.lang.Object, java.awt.image.DataBuffer)`
- `public void setPixel(int, int, double[], java.awt.image.DataBuffer)`
- `public void setPixel(int, int, float[], java.awt.image.DataBuffer)`
- `public void setPixel(int, int, int[], java.awt.image.DataBuffer)`
- `public void setPixels(int, int, int, int, double[], java.awt.image.DataBuffer)`
- `public void setPixels(int, int, int, int, float[], java.awt.image.DataBuffer)`
- `public void setPixels(int, int, int, int, int[], java.awt.image.DataBuffer)`
- `public void setSample(int, int, int, double, java.awt.image.DataBuffer)`
- `public void setSample(int, int, int, float, java.awt.image.DataBuffer)`
- `public void setSamples(int, int, int, int, int, double[], java.awt.image.DataBuffer)`
- `public void setSamples(int, int, int, int, int, float[], java.awt.image.DataBuffer)`
- `public void setSamples(int, int, int, int, int, int[], java.awt.image.DataBuffer)`

#### ✓ Implemented Fields

- `protected int dataType`
- `protected int height`
- `protected int numBands`
- `protected int width`

#### ✗ Missing Constructors

- `public java.awt.image.SampleModel(int, int, int, int)`

---

### Class: `SinglePixelPackedSampleModel` ![Coverage](https://img.shields.io/badge/coverage-58.3%25-yellow)

**Coverage:** 14 / 24 (58.3%)

```
[█████████████████████████████░░░░░░░░░░░░░░░░░░░░░] 58.3%
```

#### ✓ Implemented Methods

- `public int getNumDataElements()`
- `public int getSample(int, int, int, java.awt.image.DataBuffer)`
- `public int getScanlineStride()`
- `public int[] getBitMasks()`
- `public int[] getBitOffsets()`
- `public int[] getPixel(int, int, int[], java.awt.image.DataBuffer)`
- `public java.awt.image.DataBuffer createDataBuffer()`
- `public java.awt.image.SampleModel createCompatibleSampleModel(int, int)`
- `public java.lang.Object getDataElements(int, int, java.lang.Object, java.awt.image.DataBuffer)`
- `public void setDataElements(int, int, java.lang.Object, java.awt.image.DataBuffer)`
- `public void setPixel(int, int, int[], java.awt.image.DataBuffer)`
- `public void setSample(int, int, int, int, java.awt.image.DataBuffer)`

#### ✗ Missing Methods

- `public boolean equals(java.lang.Object)`
- `public int getOffset(int, int)`
- `public int getSampleSize(int)`
- `public int hashCode()`
- `public int[] getPixels(int, int, int, int, int[], java.awt.image.DataBuffer)`
- `public int[] getSampleSize()`
- `public int[] getSamples(int, int, int, int, int, int[], java.awt.image.DataBuffer)`
- `public java.awt.image.SampleModel createSubsetSampleModel(int[])`
- `public void setPixels(int, int, int, int, int[], java.awt.image.DataBuffer)`
- `public void setSamples(int, int, int, int, int, int[], java.awt.image.DataBuffer)`

#### ✓ Implemented Constructors

- `public java.awt.image.SinglePixelPackedSampleModel(int, int, int, int, int[])`
- `public java.awt.image.SinglePixelPackedSampleModel(int, int, int, int[])`

---

### Class: `WritableRaster` ![Coverage](https://img.shields.io/badge/coverage-21.7%25-red)

**Coverage:** 5 / 23 (21.7%)

```
[██████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 21.7%
```

#### ✓ Implemented Methods

- `public java.awt.image.WritableRaster createWritableChild(int, int, int, int, int, int, int[])`
- `public java.awt.image.WritableRaster createWritableTranslatedChild(int, int)`
- `public void setDataElements(int, int, java.lang.Object)`
- `public void setPixel(int, int, int[])`
- `public void setSample(int, int, int, int)`

#### ✗ Missing Methods

- `public java.awt.image.WritableRaster getWritableParent()`
- `public void setDataElements(int, int, int, int, java.lang.Object)`
- `public void setDataElements(int, int, java.awt.image.Raster)`
- `public void setPixel(int, int, double[])`
- `public void setPixel(int, int, float[])`
- `public void setPixels(int, int, int, int, double[])`
- `public void setPixels(int, int, int, int, float[])`
- `public void setPixels(int, int, int, int, int[])`
- `public void setRect(int, int, java.awt.image.Raster)`
- `public void setRect(java.awt.image.Raster)`
- `public void setSample(int, int, int, double)`
- `public void setSample(int, int, int, float)`
- `public void setSamples(int, int, int, int, int, double[])`
- `public void setSamples(int, int, int, int, int, float[])`
- `public void setSamples(int, int, int, int, int, int[])`

#### ✗ Missing Constructors

- `protected java.awt.image.WritableRaster(java.awt.image.SampleModel, java.awt.Point)`
- `protected java.awt.image.WritableRaster(java.awt.image.SampleModel, java.awt.image.DataBuffer, java.awt.Point)`
- `protected java.awt.image.WritableRaster(java.awt.image.SampleModel, java.awt.image.DataBuffer, java.awt.Rectangle, java.awt.Point, java.awt.image.WritableRaster)`

---

## Package: `java.io`

**Coverage:** 2 / 35 (5.7%)

```
[██░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 5.7%
```

### Class: `ObjectInputStream` ![Coverage](https://img.shields.io/badge/coverage-5.7%25-red)

**Coverage:** 2 / 35 (5.7%)

```
[██░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 5.7%
```

#### ✓ Implemented Methods

- `public final java.lang.Object readObject()`

#### ✗ Missing Methods

- `protected boolean enableResolveObject(boolean)`
- `protected java.io.ObjectStreamClass readClassDescriptor()`
- `protected java.lang.Class resolveClass(java.io.ObjectStreamClass)`
- `protected java.lang.Class resolveProxyClass(java.lang.String[])`
- `protected java.lang.Object readObjectOverride()`
- `protected java.lang.Object resolveObject(java.lang.Object)`
- `protected void readStreamHeader()`
- `public boolean readBoolean()`
- `public byte readByte()`
- `public char readChar()`
- `public double readDouble()`
- `public final java.io.ObjectInputFilter getObjectInputFilter()`
- `public final void setObjectInputFilter(java.io.ObjectInputFilter)`
- `public float readFloat()`
- `public int available()`
- `public int read()`
- `public int read(byte[], int, int)`
- `public int readInt()`
- `public int readUnsignedByte()`
- `public int readUnsignedShort()`
- `public int skipBytes(int)`
- `public java.io.ObjectInputStream$GetField readFields()`
- `public java.lang.Object readUnshared()`
- `public java.lang.String readLine()`
- `public java.lang.String readUTF()`
- `public long readLong()`
- `public short readShort()`
- `public void close()`
- `public void defaultReadObject()`
- `public void readFully(byte[])`
- `public void readFully(byte[], int, int)`
- `public void registerValidation(java.io.ObjectInputValidation, int)`

#### ✓ Implemented Constructors

- `public java.io.ObjectInputStream(java.io.InputStream)`

#### ✗ Missing Constructors

- `protected java.io.ObjectInputStream()`

---

## Package: `java.net`

**Coverage:** 20 / 92 (21.7%)

```
[██████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 21.7%
```

### Class: `InetAddress` ![Coverage](https://img.shields.io/badge/coverage-8.0%25-red)

**Coverage:** 2 / 25 (8.0%)

```
[████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 8.0%
```

#### ✓ Implemented Methods

- `public java.lang.String getHostName()`
- `public static java.net.InetAddress getByName(java.lang.String)`

#### ✗ Missing Methods

- `public boolean equals(java.lang.Object)`
- `public boolean isAnyLocalAddress()`
- `public boolean isLinkLocalAddress()`
- `public boolean isLoopbackAddress()`
- `public boolean isMCGlobal()`
- `public boolean isMCLinkLocal()`
- `public boolean isMCNodeLocal()`
- `public boolean isMCOrgLocal()`
- `public boolean isMCSiteLocal()`
- `public boolean isMulticastAddress()`
- `public boolean isReachable(int)`
- `public boolean isReachable(java.net.NetworkInterface, int, int)`
- `public boolean isSiteLocalAddress()`
- `public byte[] getAddress()`
- `public int hashCode()`
- `public java.lang.String getCanonicalHostName()`
- `public java.lang.String getHostAddress()`
- `public java.lang.String toString()`
- `public static java.net.InetAddress getByAddress(byte[])`
- `public static java.net.InetAddress getByAddress(java.lang.String, byte[])`
- `public static java.net.InetAddress getLocalHost()`
- `public static java.net.InetAddress getLoopbackAddress()`
- `public static java.net.InetAddress[] getAllByName(java.lang.String)`

---

### Class: `Socket` ![Coverage](https://img.shields.io/badge/coverage-13.0%25-red)

**Coverage:** 7 / 54 (13.0%)

```
[██████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 13.0%
```

#### ✓ Implemented Methods

- `public int getSoTimeout()`
- `public java.io.InputStream getInputStream()`
- `public java.io.OutputStream getOutputStream()`
- `public void close()`
- `public void setSoTimeout(int)`
- `public void setTcpNoDelay(boolean)`

#### ✗ Missing Methods

- `public boolean getKeepAlive()`
- `public boolean getOOBInline()`
- `public boolean getReuseAddress()`
- `public boolean getTcpNoDelay()`
- `public boolean isBound()`
- `public boolean isClosed()`
- `public boolean isConnected()`
- `public boolean isInputShutdown()`
- `public boolean isOutputShutdown()`
- `public int getLocalPort()`
- `public int getPort()`
- `public int getReceiveBufferSize()`
- `public int getSendBufferSize()`
- `public int getSoLinger()`
- `public int getTrafficClass()`
- `public java.lang.Object getOption(java.net.SocketOption)`
- `public java.lang.String toString()`
- `public java.net.InetAddress getInetAddress()`
- `public java.net.InetAddress getLocalAddress()`
- `public java.net.Socket setOption(java.net.SocketOption, java.lang.Object)`
- `public java.net.SocketAddress getLocalSocketAddress()`
- `public java.net.SocketAddress getRemoteSocketAddress()`
- `public java.nio.channels.SocketChannel getChannel()`
- `public java.util.Set supportedOptions()`
- `public static void setSocketImplFactory(java.net.SocketImplFactory)`
- `public void bind(java.net.SocketAddress)`
- `public void connect(java.net.SocketAddress)`
- `public void connect(java.net.SocketAddress, int)`
- `public void sendUrgentData(int)`
- `public void setKeepAlive(boolean)`
- `public void setOOBInline(boolean)`
- `public void setPerformancePreferences(int, int, int)`
- `public void setReceiveBufferSize(int)`
- `public void setReuseAddress(boolean)`
- `public void setSendBufferSize(int)`
- `public void setSoLinger(boolean, int)`
- `public void setTrafficClass(int)`
- `public void shutdownInput()`
- `public void shutdownOutput()`

#### ✓ Implemented Constructors

- `public java.net.Socket(java.net.InetAddress, int)`

#### ✗ Missing Constructors

- `protected java.net.Socket(java.net.SocketImpl)`
- `public java.net.Socket()`
- `public java.net.Socket(java.lang.String, int)`
- `public java.net.Socket(java.lang.String, int, boolean)`
- `public java.net.Socket(java.lang.String, int, java.net.InetAddress, int)`
- `public java.net.Socket(java.net.InetAddress, int, boolean)`
- `public java.net.Socket(java.net.InetAddress, int, java.net.InetAddress, int)`
- `public java.net.Socket(java.net.Proxy)`

---

### Class: `Socket$SocketInputStream` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 4 / 4 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public int available()`
- `public int read()`
- `public int read(byte[], int, int)`
- `public void close()`

---

### Class: `Socket$SocketOutputStream` ![Coverage](https://img.shields.io/badge/coverage-66.7%25-yellow)

**Coverage:** 2 / 3 (66.7%)

```
[█████████████████████████████████░░░░░░░░░░░░░░░░░] 66.7%
```

#### ✓ Implemented Methods

- `public void write(byte[], int, int)`
- `public void write(int)`

#### ✗ Missing Methods

- `public void close()`

---

### Class: `SocketException` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 2 / 2 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Constructors

- `public java.net.SocketException()`
- `public java.net.SocketException(java.lang.String)`

---

### Class: `SocketTimeoutException` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 2 / 2 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Constructors

- `public java.net.SocketTimeoutException()`
- `public java.net.SocketTimeoutException(java.lang.String)`

---

### Class: `UnknownHostException` ![Coverage](https://img.shields.io/badge/coverage-50.0%25-yellow)

**Coverage:** 1 / 2 (50.0%)

```
[█████████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░] 50.0%
```

#### ✓ Implemented Constructors

- `public java.net.UnknownHostException(java.lang.String)`

#### ✗ Missing Constructors

- `public java.net.UnknownHostException()`

---

## Package: `java.nio.file`

**Coverage:** 2 / 72 (2.8%)

```
[█░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 2.8%
```

### Class: `Files` ![Coverage](https://img.shields.io/badge/coverage-1.4%25-red)

**Coverage:** 1 / 70 (1.4%)

```
[░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 1.4%
```

#### ✓ Implemented Methods

- `public static java.io.BufferedReader newBufferedReader(java.nio.file.Path)`

#### ✗ Missing Methods

- `public static boolean deleteIfExists(java.nio.file.Path)`
- `public static boolean exists(java.nio.file.Path, java.nio.file.LinkOption[])`
- `public static boolean isDirectory(java.nio.file.Path, java.nio.file.LinkOption[])`
- `public static boolean isExecutable(java.nio.file.Path)`
- `public static boolean isHidden(java.nio.file.Path)`
- `public static boolean isReadable(java.nio.file.Path)`
- `public static boolean isRegularFile(java.nio.file.Path, java.nio.file.LinkOption[])`
- `public static boolean isSameFile(java.nio.file.Path, java.nio.file.Path)`
- `public static boolean isSymbolicLink(java.nio.file.Path)`
- `public static boolean isWritable(java.nio.file.Path)`
- `public static boolean notExists(java.nio.file.Path, java.nio.file.LinkOption[])`
- `public static byte[] readAllBytes(java.nio.file.Path)`
- `public static java.io.BufferedReader newBufferedReader(java.nio.file.Path, java.nio.charset.Charset)`
- `public static java.io.BufferedWriter newBufferedWriter(java.nio.file.Path, java.nio.charset.Charset, java.nio.file.OpenOption[])`
- `public static java.io.BufferedWriter newBufferedWriter(java.nio.file.Path, java.nio.file.OpenOption[])`
- `public static java.io.InputStream newInputStream(java.nio.file.Path, java.nio.file.OpenOption[])`
- `public static java.io.OutputStream newOutputStream(java.nio.file.Path, java.nio.file.OpenOption[])`
- `public static java.lang.Object getAttribute(java.nio.file.Path, java.lang.String, java.nio.file.LinkOption[])`
- `public static java.lang.String probeContentType(java.nio.file.Path)`
- `public static java.lang.String readString(java.nio.file.Path)`
- `public static java.lang.String readString(java.nio.file.Path, java.nio.charset.Charset)`
- `public static java.nio.channels.SeekableByteChannel newByteChannel(java.nio.file.Path, java.nio.file.OpenOption[])`
- `public static java.nio.channels.SeekableByteChannel newByteChannel(java.nio.file.Path, java.util.Set, java.nio.file.attribute.FileAttribute[])`
- `public static java.nio.file.DirectoryStream newDirectoryStream(java.nio.file.Path)`
- `public static java.nio.file.DirectoryStream newDirectoryStream(java.nio.file.Path, java.lang.String)`
- `public static java.nio.file.DirectoryStream newDirectoryStream(java.nio.file.Path, java.nio.file.DirectoryStream$Filter)`
- `public static java.nio.file.FileStore getFileStore(java.nio.file.Path)`
- `public static java.nio.file.Path copy(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption[])`
- `public static java.nio.file.Path createDirectories(java.nio.file.Path, java.nio.file.attribute.FileAttribute[])`
- `public static java.nio.file.Path createDirectory(java.nio.file.Path, java.nio.file.attribute.FileAttribute[])`
- `public static java.nio.file.Path createFile(java.nio.file.Path, java.nio.file.attribute.FileAttribute[])`
- `public static java.nio.file.Path createLink(java.nio.file.Path, java.nio.file.Path)`
- `public static java.nio.file.Path createSymbolicLink(java.nio.file.Path, java.nio.file.Path, java.nio.file.attribute.FileAttribute[])`
- `public static java.nio.file.Path createTempDirectory(java.lang.String, java.nio.file.attribute.FileAttribute[])`
- `public static java.nio.file.Path createTempDirectory(java.nio.file.Path, java.lang.String, java.nio.file.attribute.FileAttribute[])`
- `public static java.nio.file.Path createTempFile(java.lang.String, java.lang.String, java.nio.file.attribute.FileAttribute[])`
- `public static java.nio.file.Path createTempFile(java.nio.file.Path, java.lang.String, java.lang.String, java.nio.file.attribute.FileAttribute[])`
- `public static java.nio.file.Path move(java.nio.file.Path, java.nio.file.Path, java.nio.file.CopyOption[])`
- `public static java.nio.file.Path readSymbolicLink(java.nio.file.Path)`
- `public static java.nio.file.Path setAttribute(java.nio.file.Path, java.lang.String, java.lang.Object, java.nio.file.LinkOption[])`
- `public static java.nio.file.Path setLastModifiedTime(java.nio.file.Path, java.nio.file.attribute.FileTime)`
- `public static java.nio.file.Path setOwner(java.nio.file.Path, java.nio.file.attribute.UserPrincipal)`
- `public static java.nio.file.Path setPosixFilePermissions(java.nio.file.Path, java.util.Set)`
- `public static java.nio.file.Path walkFileTree(java.nio.file.Path, java.nio.file.FileVisitor)`
- `public static java.nio.file.Path walkFileTree(java.nio.file.Path, java.util.Set, int, java.nio.file.FileVisitor)`
- `public static java.nio.file.Path write(java.nio.file.Path, byte[], java.nio.file.OpenOption[])`
- `public static java.nio.file.Path write(java.nio.file.Path, java.lang.Iterable, java.nio.charset.Charset, java.nio.file.OpenOption[])`
- `public static java.nio.file.Path write(java.nio.file.Path, java.lang.Iterable, java.nio.file.OpenOption[])`
- `public static java.nio.file.Path writeString(java.nio.file.Path, java.lang.CharSequence, java.nio.charset.Charset, java.nio.file.OpenOption[])`
- `public static java.nio.file.Path writeString(java.nio.file.Path, java.lang.CharSequence, java.nio.file.OpenOption[])`
- `public static java.nio.file.attribute.BasicFileAttributes readAttributes(java.nio.file.Path, java.lang.Class, java.nio.file.LinkOption[])`
- `public static java.nio.file.attribute.FileAttributeView getFileAttributeView(java.nio.file.Path, java.lang.Class, java.nio.file.LinkOption[])`
- `public static java.nio.file.attribute.FileTime getLastModifiedTime(java.nio.file.Path, java.nio.file.LinkOption[])`
- `public static java.nio.file.attribute.UserPrincipal getOwner(java.nio.file.Path, java.nio.file.LinkOption[])`
- `public static java.util.List readAllLines(java.nio.file.Path)`
- `public static java.util.List readAllLines(java.nio.file.Path, java.nio.charset.Charset)`
- `public static java.util.Map readAttributes(java.nio.file.Path, java.lang.String, java.nio.file.LinkOption[])`
- `public static java.util.Set getPosixFilePermissions(java.nio.file.Path, java.nio.file.LinkOption[])`
- `public static java.util.stream.Stream find(java.nio.file.Path, int, java.util.function.BiPredicate, java.nio.file.FileVisitOption[])`
- `public static java.util.stream.Stream lines(java.nio.file.Path)`
- `public static java.util.stream.Stream lines(java.nio.file.Path, java.nio.charset.Charset)`
- `public static java.util.stream.Stream list(java.nio.file.Path)`
- `public static java.util.stream.Stream walk(java.nio.file.Path, int, java.nio.file.FileVisitOption[])`
- `public static java.util.stream.Stream walk(java.nio.file.Path, java.nio.file.FileVisitOption[])`
- `public static long copy(java.io.InputStream, java.nio.file.Path, java.nio.file.CopyOption[])`
- `public static long copy(java.nio.file.Path, java.io.OutputStream)`
- `public static long mismatch(java.nio.file.Path, java.nio.file.Path)`
- `public static long size(java.nio.file.Path)`
- `public static void delete(java.nio.file.Path)`

---

### Class: `Paths` ![Coverage](https://img.shields.io/badge/coverage-50.0%25-yellow)

**Coverage:** 1 / 2 (50.0%)

```
[█████████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░] 50.0%
```

#### ✓ Implemented Methods

- `public static java.nio.file.Path get(java.lang.String, java.lang.String[])`

#### ✗ Missing Methods

- `public static java.nio.file.Path get(java.net.URI)`

---

## Package: `javax.sound.midi`

**Coverage:** 50 / 134 (37.3%)

```
[██████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 37.3%
```

### Class: `MidiMessage` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 8 / 8 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `protected void setMessage(byte[], int)`
- `public abstract java.lang.Object clone()`
- `public byte[] getMessage()`
- `public int getLength()`
- `public int getStatus()`

#### ✓ Implemented Fields

- `protected byte[] data`
- `protected int length`

#### ✓ Implemented Constructors

- `protected javax.sound.midi.MidiMessage(byte[])`

---

### Class: `MidiSystem` ![Coverage](https://img.shields.io/badge/coverage-13.6%25-red)

**Coverage:** 3 / 22 (13.6%)

```
[██████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 13.6%
```

#### ✓ Implemented Methods

- `public static javax.sound.midi.Receiver getReceiver()`
- `public static javax.sound.midi.Sequence getSequence(java.io.InputStream)`
- `public static javax.sound.midi.Sequencer getSequencer(boolean)`

#### ✗ Missing Methods

- `public static boolean isFileTypeSupported(int)`
- `public static boolean isFileTypeSupported(int, javax.sound.midi.Sequence)`
- `public static int write(javax.sound.midi.Sequence, int, java.io.File)`
- `public static int write(javax.sound.midi.Sequence, int, java.io.OutputStream)`
- `public static int[] getMidiFileTypes()`
- `public static int[] getMidiFileTypes(javax.sound.midi.Sequence)`
- `public static javax.sound.midi.MidiDevice getMidiDevice(javax.sound.midi.MidiDevice$Info)`
- `public static javax.sound.midi.MidiDevice$Info[] getMidiDeviceInfo()`
- `public static javax.sound.midi.MidiFileFormat getMidiFileFormat(java.io.File)`
- `public static javax.sound.midi.MidiFileFormat getMidiFileFormat(java.io.InputStream)`
- `public static javax.sound.midi.MidiFileFormat getMidiFileFormat(java.net.URL)`
- `public static javax.sound.midi.Sequence getSequence(java.io.File)`
- `public static javax.sound.midi.Sequence getSequence(java.net.URL)`
- `public static javax.sound.midi.Sequencer getSequencer()`
- `public static javax.sound.midi.Soundbank getSoundbank(java.io.File)`
- `public static javax.sound.midi.Soundbank getSoundbank(java.io.InputStream)`
- `public static javax.sound.midi.Soundbank getSoundbank(java.net.URL)`
- `public static javax.sound.midi.Synthesizer getSynthesizer()`
- `public static javax.sound.midi.Transmitter getTransmitter()`

---

### Class: `Receiver` ![Coverage](https://img.shields.io/badge/coverage-50.0%25-yellow)

**Coverage:** 1 / 2 (50.0%)

```
[█████████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░] 50.0%
```

#### ✓ Implemented Methods

- `public abstract void send(javax.sound.midi.MidiMessage, long)`

#### ✗ Missing Methods

- `public abstract void close()`

---

### Class: `Sequence` ![Coverage](https://img.shields.io/badge/coverage-0.0%25-red)

**Coverage:** 0 / 18 (0.0%)

```
[░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 0.0%
```

#### ✗ Missing Methods

- `public boolean deleteTrack(javax.sound.midi.Track)`
- `public float getDivisionType()`
- `public int getResolution()`
- `public javax.sound.midi.Patch[] getPatchList()`
- `public javax.sound.midi.Track createTrack()`
- `public javax.sound.midi.Track[] getTracks()`
- `public long getMicrosecondLength()`
- `public long getTickLength()`

#### ✗ Missing Fields

- `protected float divisionType`
- `protected int resolution`
- `protected java.util.Vector tracks`
- `public static final float PPQ`
- `public static final float SMPTE_24`
- `public static final float SMPTE_25`
- `public static final float SMPTE_30`
- `public static final float SMPTE_30DROP`

#### ✗ Missing Constructors

- `public javax.sound.midi.Sequence(float, int)`
- `public javax.sound.midi.Sequence(float, int, int)`

---

### Class: `Sequencer` ![Coverage](https://img.shields.io/badge/coverage-11.4%25-red)

**Coverage:** 5 / 44 (11.4%)

```
[█████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 11.4%
```

#### ✓ Implemented Methods

- `public abstract void setLoopCount(int)`
- `public abstract void setSequence(javax.sound.midi.Sequence)`
- `public abstract void start()`
- `public abstract void stop()`

#### ✗ Missing Methods

- `public abstract boolean addMetaEventListener(javax.sound.midi.MetaEventListener)`
- `public abstract boolean getTrackMute(int)`
- `public abstract boolean getTrackSolo(int)`
- `public abstract boolean isRecording()`
- `public abstract boolean isRunning()`
- `public abstract float getTempoFactor()`
- `public abstract float getTempoInBPM()`
- `public abstract float getTempoInMPQ()`
- `public abstract int getLoopCount()`
- `public abstract int[] addControllerEventListener(javax.sound.midi.ControllerEventListener, int[])`
- `public abstract int[] removeControllerEventListener(javax.sound.midi.ControllerEventListener, int[])`
- `public abstract javax.sound.midi.Sequence getSequence()`
- `public abstract javax.sound.midi.Sequencer$SyncMode getMasterSyncMode()`
- `public abstract javax.sound.midi.Sequencer$SyncMode getSlaveSyncMode()`
- `public abstract javax.sound.midi.Sequencer$SyncMode[] getMasterSyncModes()`
- `public abstract javax.sound.midi.Sequencer$SyncMode[] getSlaveSyncModes()`
- `public abstract long getLoopEndPoint()`
- `public abstract long getLoopStartPoint()`
- `public abstract long getMicrosecondLength()`
- `public abstract long getMicrosecondPosition()`
- `public abstract long getTickLength()`
- `public abstract long getTickPosition()`
- `public abstract void recordDisable(javax.sound.midi.Track)`
- `public abstract void recordEnable(javax.sound.midi.Track, int)`
- `public abstract void removeMetaEventListener(javax.sound.midi.MetaEventListener)`
- `public abstract void setLoopEndPoint(long)`
- `public abstract void setLoopStartPoint(long)`
- `public abstract void setMasterSyncMode(javax.sound.midi.Sequencer$SyncMode)`
- `public abstract void setMicrosecondPosition(long)`
- `public abstract void setSequence(java.io.InputStream)`
- `public abstract void setSlaveSyncMode(javax.sound.midi.Sequencer$SyncMode)`
- `public abstract void setTempoFactor(float)`
- `public abstract void setTempoInBPM(float)`
- `public abstract void setTempoInMPQ(float)`
- `public abstract void setTickPosition(long)`
- `public abstract void setTrackMute(int, boolean)`
- `public abstract void setTrackSolo(int, boolean)`
- `public abstract void startRecording()`
- `public abstract void stopRecording()`

#### ✓ Implemented Fields

- `public static final int LOOP_CONTINUOUSLY`

---

### Class: `ShortMessage` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 32 / 32 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `protected final int getDataLength(int)`
- `public int getChannel()`
- `public int getCommand()`
- `public int getData1()`
- `public int getData2()`
- `public java.lang.Object clone()`
- `public void setMessage(int)`
- `public void setMessage(int, int, int)`
- `public void setMessage(int, int, int, int)`

#### ✓ Implemented Fields

- `public static final int ACTIVE_SENSING`
- `public static final int CHANNEL_PRESSURE`
- `public static final int CONTINUE`
- `public static final int CONTROL_CHANGE`
- `public static final int END_OF_EXCLUSIVE`
- `public static final int MIDI_TIME_CODE`
- `public static final int NOTE_OFF`
- `public static final int NOTE_ON`
- `public static final int PITCH_BEND`
- `public static final int POLY_PRESSURE`
- `public static final int PROGRAM_CHANGE`
- `public static final int SONG_POSITION_POINTER`
- `public static final int SONG_SELECT`
- `public static final int START`
- `public static final int STOP`
- `public static final int SYSTEM_RESET`
- `public static final int TIMING_CLOCK`
- `public static final int TUNE_REQUEST`

#### ✓ Implemented Constructors

- `protected javax.sound.midi.ShortMessage(byte[])`
- `public javax.sound.midi.ShortMessage()`
- `public javax.sound.midi.ShortMessage(int)`
- `public javax.sound.midi.ShortMessage(int, int, int)`
- `public javax.sound.midi.ShortMessage(int, int, int, int)`

---

### Class: `Track` ![Coverage](https://img.shields.io/badge/coverage-0.0%25-red)

**Coverage:** 0 / 5 (0.0%)

```
[░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 0.0%
```

#### ✗ Missing Methods

- `public boolean add(javax.sound.midi.MidiEvent)`
- `public boolean remove(javax.sound.midi.MidiEvent)`
- `public int size()`
- `public javax.sound.midi.MidiEvent get(int)`
- `public long ticks()`

---

### Class: `Transmitter` ![Coverage](https://img.shields.io/badge/coverage-33.3%25-orange)

**Coverage:** 1 / 3 (33.3%)

```
[████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 33.3%
```

#### ✓ Implemented Methods

- `public abstract void setReceiver(javax.sound.midi.Receiver)`

#### ✗ Missing Methods

- `public abstract javax.sound.midi.Receiver getReceiver()`
- `public abstract void close()`

---

## Package: `javax.sound.sampled`

**Coverage:** 75 / 123 (61.0%)

```
[██████████████████████████████░░░░░░░░░░░░░░░░░░░░] 61.0%
```

### Class: `AudioFormat` ![Coverage](https://img.shields.io/badge/coverage-61.9%25-yellow)

**Coverage:** 13 / 21 (61.9%)

```
[██████████████████████████████░░░░░░░░░░░░░░░░░░░░] 61.9%
```

#### ✓ Implemented Methods

- `public boolean isBigEndian()`
- `public boolean matches(javax.sound.sampled.AudioFormat)`
- `public float getFrameRate()`
- `public float getSampleRate()`
- `public int getChannels()`
- `public int getFrameSize()`
- `public int getSampleSizeInBits()`
- `public java.lang.String toString()`
- `public javax.sound.sampled.AudioFormat$Encoding getEncoding()`

#### ✗ Missing Methods

- `public java.lang.Object getProperty(java.lang.String)`
- `public java.util.Map properties()`

#### ✓ Implemented Fields

- `protected javax.sound.sampled.AudioFormat$Encoding encoding`

#### ✗ Missing Fields

- `protected boolean bigEndian`
- `protected float frameRate`
- `protected float sampleRate`
- `protected int channels`
- `protected int frameSize`
- `protected int sampleSizeInBits`

#### ✓ Implemented Constructors

- `public javax.sound.sampled.AudioFormat(float, int, int, boolean, boolean)`
- `public javax.sound.sampled.AudioFormat(javax.sound.sampled.AudioFormat$Encoding, float, int, int, int, float, boolean)`
- `public javax.sound.sampled.AudioFormat(javax.sound.sampled.AudioFormat$Encoding, float, int, int, int, float, boolean, java.util.Map)`

---

### Class: `AudioFormat$Encoding` ![Coverage](https://img.shields.io/badge/coverage-66.7%25-yellow)

**Coverage:** 6 / 9 (66.7%)

```
[█████████████████████████████████░░░░░░░░░░░░░░░░░] 66.7%
```

#### ✗ Missing Methods

- `public final boolean equals(java.lang.Object)`
- `public final int hashCode()`
- `public final java.lang.String toString()`

#### ✓ Implemented Fields

- `public static final javax.sound.sampled.AudioFormat$Encoding ALAW`
- `public static final javax.sound.sampled.AudioFormat$Encoding PCM_FLOAT`
- `public static final javax.sound.sampled.AudioFormat$Encoding PCM_SIGNED`
- `public static final javax.sound.sampled.AudioFormat$Encoding PCM_UNSIGNED`
- `public static final javax.sound.sampled.AudioFormat$Encoding ULAW`

#### ✓ Implemented Constructors

- `public javax.sound.sampled.AudioFormat$Encoding(java.lang.String)`

---

### Class: `AudioSystem` ![Coverage](https://img.shields.io/badge/coverage-9.4%25-red)

**Coverage:** 3 / 32 (9.4%)

```
[████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 9.4%
```

#### ✓ Implemented Methods

- `public static javax.sound.sampled.Line getLine(javax.sound.sampled.Line$Info)`
- `public static javax.sound.sampled.SourceDataLine getSourceDataLine(javax.sound.sampled.AudioFormat)`

#### ✗ Missing Methods

- `public static boolean isConversionSupported(javax.sound.sampled.AudioFormat$Encoding, javax.sound.sampled.AudioFormat)`
- `public static boolean isConversionSupported(javax.sound.sampled.AudioFormat, javax.sound.sampled.AudioFormat)`
- `public static boolean isFileTypeSupported(javax.sound.sampled.AudioFileFormat$Type)`
- `public static boolean isFileTypeSupported(javax.sound.sampled.AudioFileFormat$Type, javax.sound.sampled.AudioInputStream)`
- `public static boolean isLineSupported(javax.sound.sampled.Line$Info)`
- `public static int write(javax.sound.sampled.AudioInputStream, javax.sound.sampled.AudioFileFormat$Type, java.io.File)`
- `public static int write(javax.sound.sampled.AudioInputStream, javax.sound.sampled.AudioFileFormat$Type, java.io.OutputStream)`
- `public static javax.sound.sampled.AudioFileFormat getAudioFileFormat(java.io.File)`
- `public static javax.sound.sampled.AudioFileFormat getAudioFileFormat(java.io.InputStream)`
- `public static javax.sound.sampled.AudioFileFormat getAudioFileFormat(java.net.URL)`
- `public static javax.sound.sampled.AudioFileFormat$Type[] getAudioFileTypes()`
- `public static javax.sound.sampled.AudioFileFormat$Type[] getAudioFileTypes(javax.sound.sampled.AudioInputStream)`
- `public static javax.sound.sampled.AudioFormat$Encoding[] getTargetEncodings(javax.sound.sampled.AudioFormat$Encoding)`
- `public static javax.sound.sampled.AudioFormat$Encoding[] getTargetEncodings(javax.sound.sampled.AudioFormat)`
- `public static javax.sound.sampled.AudioFormat[] getTargetFormats(javax.sound.sampled.AudioFormat$Encoding, javax.sound.sampled.AudioFormat)`
- `public static javax.sound.sampled.AudioInputStream getAudioInputStream(java.io.File)`
- `public static javax.sound.sampled.AudioInputStream getAudioInputStream(java.io.InputStream)`
- `public static javax.sound.sampled.AudioInputStream getAudioInputStream(java.net.URL)`
- `public static javax.sound.sampled.AudioInputStream getAudioInputStream(javax.sound.sampled.AudioFormat$Encoding, javax.sound.sampled.AudioInputStream)`
- `public static javax.sound.sampled.AudioInputStream getAudioInputStream(javax.sound.sampled.AudioFormat, javax.sound.sampled.AudioInputStream)`
- `public static javax.sound.sampled.Clip getClip()`
- `public static javax.sound.sampled.Clip getClip(javax.sound.sampled.Mixer$Info)`
- `public static javax.sound.sampled.Line$Info[] getSourceLineInfo(javax.sound.sampled.Line$Info)`
- `public static javax.sound.sampled.Line$Info[] getTargetLineInfo(javax.sound.sampled.Line$Info)`
- `public static javax.sound.sampled.Mixer getMixer(javax.sound.sampled.Mixer$Info)`
- `public static javax.sound.sampled.Mixer$Info[] getMixerInfo()`
- `public static javax.sound.sampled.SourceDataLine getSourceDataLine(javax.sound.sampled.AudioFormat, javax.sound.sampled.Mixer$Info)`
- `public static javax.sound.sampled.TargetDataLine getTargetDataLine(javax.sound.sampled.AudioFormat)`
- `public static javax.sound.sampled.TargetDataLine getTargetDataLine(javax.sound.sampled.AudioFormat, javax.sound.sampled.Mixer$Info)`

#### ✓ Implemented Fields

- `public static final int NOT_SPECIFIED`

---

### Class: `Control` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 3 / 3 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public java.lang.String toString()`
- `public javax.sound.sampled.Control$Type getType()`

#### ✓ Implemented Constructors

- `protected javax.sound.sampled.Control(javax.sound.sampled.Control$Type)`

---

### Class: `Control$Type` ![Coverage](https://img.shields.io/badge/coverage-25.0%25-orange)

**Coverage:** 1 / 4 (25.0%)

```
[████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 25.0%
```

#### ✗ Missing Methods

- `public final boolean equals(java.lang.Object)`
- `public final int hashCode()`
- `public final java.lang.String toString()`

#### ✓ Implemented Constructors

- `protected javax.sound.sampled.Control$Type(java.lang.String)`

---

### Class: `DataLine` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 13 / 13 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract boolean isActive()`
- `public abstract boolean isRunning()`
- `public abstract float getLevel()`
- `public abstract int available()`
- `public abstract int getBufferSize()`
- `public abstract int getFramePosition()`
- `public abstract javax.sound.sampled.AudioFormat getFormat()`
- `public abstract long getLongFramePosition()`
- `public abstract long getMicrosecondPosition()`
- `public abstract void drain()`
- `public abstract void flush()`
- `public abstract void start()`
- `public abstract void stop()`

---

### Class: `DataLine$Info` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 9 / 9 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public boolean isFormatSupported(javax.sound.sampled.AudioFormat)`
- `public boolean matches(javax.sound.sampled.Line$Info)`
- `public int getMaxBufferSize()`
- `public int getMinBufferSize()`
- `public java.lang.String toString()`
- `public javax.sound.sampled.AudioFormat[] getFormats()`

#### ✓ Implemented Constructors

- `public javax.sound.sampled.DataLine$Info(java.lang.Class, javax.sound.sampled.AudioFormat)`
- `public javax.sound.sampled.DataLine$Info(java.lang.Class, javax.sound.sampled.AudioFormat, int)`
- `public javax.sound.sampled.DataLine$Info(java.lang.Class, javax.sound.sampled.AudioFormat[], int, int)`

---

### Class: `Line` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 9 / 9 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract boolean isControlSupported(javax.sound.sampled.Control$Type)`
- `public abstract boolean isOpen()`
- `public abstract javax.sound.sampled.Control getControl(javax.sound.sampled.Control$Type)`
- `public abstract javax.sound.sampled.Control[] getControls()`
- `public abstract javax.sound.sampled.Line$Info getLineInfo()`
- `public abstract void addLineListener(javax.sound.sampled.LineListener)`
- `public abstract void close()`
- `public abstract void open()`
- `public abstract void removeLineListener(javax.sound.sampled.LineListener)`

---

### Class: `Line$Info` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 4 / 4 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public boolean matches(javax.sound.sampled.Line$Info)`
- `public java.lang.Class getLineClass()`
- `public java.lang.String toString()`

#### ✓ Implemented Constructors

- `public javax.sound.sampled.Line$Info(java.lang.Class)`

---

### Class: `LineEvent` ![Coverage](https://img.shields.io/badge/coverage-60.0%25-yellow)

**Coverage:** 3 / 5 (60.0%)

```
[██████████████████████████████░░░░░░░░░░░░░░░░░░░░] 60.0%
```

#### ✓ Implemented Methods

- `public final javax.sound.sampled.Line getLine()`
- `public java.lang.String toString()`

#### ✗ Missing Methods

- `public final javax.sound.sampled.LineEvent$Type getType()`
- `public final long getFramePosition()`

#### ✓ Implemented Constructors

- `public javax.sound.sampled.LineEvent(javax.sound.sampled.Line, javax.sound.sampled.LineEvent$Type, long)`

---

### Class: `LineEvent$Type` ![Coverage](https://img.shields.io/badge/coverage-62.5%25-yellow)

**Coverage:** 5 / 8 (62.5%)

```
[███████████████████████████████░░░░░░░░░░░░░░░░░░░] 62.5%
```

#### ✗ Missing Methods

- `public final boolean equals(java.lang.Object)`
- `public final int hashCode()`
- `public java.lang.String toString()`

#### ✓ Implemented Fields

- `public static final javax.sound.sampled.LineEvent$Type CLOSE`
- `public static final javax.sound.sampled.LineEvent$Type OPEN`
- `public static final javax.sound.sampled.LineEvent$Type START`
- `public static final javax.sound.sampled.LineEvent$Type STOP`

#### ✓ Implemented Constructors

- `protected javax.sound.sampled.LineEvent$Type(java.lang.String)`

---

### Class: `LineListener` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 1 / 1 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract void update(javax.sound.sampled.LineEvent)`

---

### Class: `LineUnavailableException` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 2 / 2 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Constructors

- `public javax.sound.sampled.LineUnavailableException()`
- `public javax.sound.sampled.LineUnavailableException(java.lang.String)`

---

### Class: `SourceDataLine` ![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)

**Coverage:** 3 / 3 (100.0%)

```
[██████████████████████████████████████████████████] 100.0%
```

#### ✓ Implemented Methods

- `public abstract int write(byte[], int, int)`
- `public abstract void open(javax.sound.sampled.AudioFormat)`
- `public abstract void open(javax.sound.sampled.AudioFormat, int)`

---

## Package: `javax.swing`

**Coverage:** 5 / 202 (2.5%)

```
[█░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 2.5%
```

### Class: `JComponent` ![Coverage](https://img.shields.io/badge/coverage-0.7%25-red)

**Coverage:** 1 / 145 (0.7%)

```
[░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 0.7%
```

#### ✗ Missing Methods

- `protected boolean isPaintingOrigin()`
- `protected boolean processKeyBinding(javax.swing.KeyStroke, java.awt.event.KeyEvent, int, boolean)`
- `protected boolean requestFocusInWindow(boolean)`
- `protected java.awt.Graphics getComponentGraphics(java.awt.Graphics)`
- `protected java.lang.String paramString()`
- `protected void fireVetoableChange(java.lang.String, java.lang.Object, java.lang.Object)`
- `protected void paintBorder(java.awt.Graphics)`
- `protected void paintChildren(java.awt.Graphics)`
- `protected void paintComponent(java.awt.Graphics)`
- `protected void printBorder(java.awt.Graphics)`
- `protected void printChildren(java.awt.Graphics)`
- `protected void printComponent(java.awt.Graphics)`
- `protected void processComponentKeyEvent(java.awt.event.KeyEvent)`
- `protected void processKeyEvent(java.awt.event.KeyEvent)`
- `protected void processMouseEvent(java.awt.event.MouseEvent)`
- `protected void processMouseMotionEvent(java.awt.event.MouseEvent)`
- `protected void setUI(javax.swing.plaf.ComponentUI)`
- `public boolean contains(int, int)`
- `public boolean getAutoscrolls()`
- `public boolean getInheritsPopupMenu()`
- `public boolean getVerifyInputWhenFocusTarget()`
- `public boolean isDoubleBuffered()`
- `public boolean isManagingFocus()`
- `public boolean isOpaque()`
- `public boolean isOptimizedDrawingEnabled()`
- `public boolean isPaintingTile()`
- `public boolean isRequestFocusEnabled()`
- `public boolean isValidateRoot()`
- `public boolean requestDefaultFocus()`
- `public boolean requestFocus(boolean)`
- `public boolean requestFocusInWindow()`
- `public final boolean isPaintingForPrint()`
- `public final java.lang.Object getClientProperty(java.lang.Object)`
- `public final javax.swing.ActionMap getActionMap()`
- `public final javax.swing.InputMap getInputMap()`
- `public final javax.swing.InputMap getInputMap(int)`
- `public final void putClientProperty(java.lang.Object, java.lang.Object)`
- `public final void setActionMap(javax.swing.ActionMap)`
- `public final void setInputMap(int, javax.swing.InputMap)`
- `public float getAlignmentX()`
- `public float getAlignmentY()`
- `public int getBaseline(int, int)`
- `public int getConditionForKeyStroke(javax.swing.KeyStroke)`
- `public int getDebugGraphicsOptions()`
- `public int getHeight()`
- `public int getWidth()`
- `public int getX()`
- `public int getY()`
- `public java.awt.Component getNextFocusableComponent()`
- `public java.awt.Component$BaselineResizeBehavior getBaselineResizeBehavior()`
- `public java.awt.Container getTopLevelAncestor()`
- `public java.awt.Dimension getMaximumSize()`
- `public java.awt.Dimension getMinimumSize()`
- `public java.awt.Dimension getPreferredSize()`
- `public java.awt.Dimension getSize(java.awt.Dimension)`
- `public java.awt.FontMetrics getFontMetrics(java.awt.Font)`
- `public java.awt.Graphics getGraphics()`
- `public java.awt.Insets getInsets()`
- `public java.awt.Insets getInsets(java.awt.Insets)`
- `public java.awt.Point getLocation(java.awt.Point)`
- `public java.awt.Point getPopupLocation(java.awt.event.MouseEvent)`
- `public java.awt.Point getToolTipLocation(java.awt.event.MouseEvent)`
- `public java.awt.Rectangle getBounds(java.awt.Rectangle)`
- `public java.awt.Rectangle getVisibleRect()`
- `public java.awt.event.ActionListener getActionForKeyStroke(javax.swing.KeyStroke)`
- `public java.beans.VetoableChangeListener[] getVetoableChangeListeners()`
- `public java.lang.String getToolTipText()`
- `public java.lang.String getToolTipText(java.awt.event.MouseEvent)`
- `public java.lang.String getUIClassID()`
- `public java.util.EventListener[] getListeners(java.lang.Class)`
- `public javax.swing.InputVerifier getInputVerifier()`
- `public javax.swing.JPopupMenu getComponentPopupMenu()`
- `public javax.swing.JRootPane getRootPane()`
- `public javax.swing.JToolTip createToolTip()`
- `public javax.swing.KeyStroke[] getRegisteredKeyStrokes()`
- `public javax.swing.TransferHandler getTransferHandler()`
- `public javax.swing.border.Border getBorder()`
- `public javax.swing.event.AncestorListener[] getAncestorListeners()`
- `public javax.swing.plaf.ComponentUI getUI()`
- `public static boolean isLightweightComponent(java.awt.Component)`
- `public static java.util.Locale getDefaultLocale()`
- `public static void setDefaultLocale(java.util.Locale)`
- `public void addAncestorListener(javax.swing.event.AncestorListener)`
- `public void addNotify()`
- `public void addVetoableChangeListener(java.beans.VetoableChangeListener)`
- `public void computeVisibleRect(java.awt.Rectangle)`
- `public void disable()`
- `public void enable()`
- `public void firePropertyChange(java.lang.String, boolean, boolean)`
- `public void firePropertyChange(java.lang.String, char, char)`
- `public void firePropertyChange(java.lang.String, int, int)`
- `public void grabFocus()`
- `public void hide()`
- `public void paint(java.awt.Graphics)`
- `public void paintImmediately(int, int, int, int)`
- `public void paintImmediately(java.awt.Rectangle)`
- `public void print(java.awt.Graphics)`
- `public void printAll(java.awt.Graphics)`
- `public void registerKeyboardAction(java.awt.event.ActionListener, java.lang.String, javax.swing.KeyStroke, int)`
- `public void registerKeyboardAction(java.awt.event.ActionListener, javax.swing.KeyStroke, int)`
- `public void removeAncestorListener(javax.swing.event.AncestorListener)`
- `public void removeNotify()`
- `public void removeVetoableChangeListener(java.beans.VetoableChangeListener)`
- `public void repaint(java.awt.Rectangle)`
- `public void repaint(long, int, int, int, int)`
- `public void requestFocus()`
- `public void resetKeyboardActions()`
- `public void reshape(int, int, int, int)`
- `public void revalidate()`
- `public void scrollRectToVisible(java.awt.Rectangle)`
- `public void setAlignmentX(float)`
- `public void setAlignmentY(float)`
- `public void setAutoscrolls(boolean)`
- `public void setBackground(java.awt.Color)`
- `public void setBorder(javax.swing.border.Border)`
- `public void setComponentPopupMenu(javax.swing.JPopupMenu)`
- `public void setDebugGraphicsOptions(int)`
- `public void setDoubleBuffered(boolean)`
- `public void setEnabled(boolean)`
- `public void setFocusTraversalKeys(int, java.util.Set)`
- `public void setFont(java.awt.Font)`
- `public void setForeground(java.awt.Color)`
- `public void setInheritsPopupMenu(boolean)`
- `public void setInputVerifier(javax.swing.InputVerifier)`
- `public void setMaximumSize(java.awt.Dimension)`
- `public void setMinimumSize(java.awt.Dimension)`
- `public void setNextFocusableComponent(java.awt.Component)`
- `public void setOpaque(boolean)`
- `public void setPreferredSize(java.awt.Dimension)`
- `public void setRequestFocusEnabled(boolean)`
- `public void setToolTipText(java.lang.String)`
- `public void setTransferHandler(javax.swing.TransferHandler)`
- `public void setVerifyInputWhenFocusTarget(boolean)`
- `public void setVisible(boolean)`
- `public void unregisterKeyboardAction(javax.swing.KeyStroke)`
- `public void update(java.awt.Graphics)`
- `public void updateUI()`

#### ✗ Missing Fields

- `protected javax.swing.event.EventListenerList listenerList`
- `protected javax.swing.plaf.ComponentUI ui`
- `public static final int UNDEFINED_CONDITION`
- `public static final int WHEN_ANCESTOR_OF_FOCUSED_COMPONENT`
- `public static final int WHEN_FOCUSED`
- `public static final int WHEN_IN_FOCUSED_WINDOW`
- `public static final java.lang.String TOOL_TIP_TEXT_KEY`

#### ✓ Implemented Constructors

- `public javax.swing.JComponent()`

---

### Class: `JPanel` ![Coverage](https://img.shields.io/badge/coverage-9.1%25-red)

**Coverage:** 1 / 11 (9.1%)

```
[████░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 9.1%
```

#### ✗ Missing Methods

- `protected java.lang.String paramString()`
- `public java.lang.String getUIClassID()`
- `public javax.accessibility.AccessibleContext getAccessibleContext()`
- `public javax.swing.plaf.ComponentUI getUI()`
- `public javax.swing.plaf.PanelUI getUI()`
- `public void setUI(javax.swing.plaf.PanelUI)`
- `public void updateUI()`

#### ✓ Implemented Constructors

- `public javax.swing.JPanel()`

#### ✗ Missing Constructors

- `public javax.swing.JPanel(boolean)`
- `public javax.swing.JPanel(java.awt.LayoutManager)`
- `public javax.swing.JPanel(java.awt.LayoutManager, boolean)`

---

### Class: `SwingUtilities` ![Coverage](https://img.shields.io/badge/coverage-6.5%25-red)

**Coverage:** 3 / 46 (6.5%)

```
[███░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 6.5%
```

#### ✓ Implemented Methods

- `public static boolean isLeftMouseButton(java.awt.event.MouseEvent)`
- `public static boolean isMiddleMouseButton(java.awt.event.MouseEvent)`
- `public static boolean isRightMouseButton(java.awt.event.MouseEvent)`

#### ✗ Missing Methods

- `public static boolean isDescendingFrom(java.awt.Component, java.awt.Component)`
- `public static boolean isEventDispatchThread()`
- `public static boolean notifyAction(javax.swing.Action, javax.swing.KeyStroke, java.awt.event.KeyEvent, java.lang.Object, int)`
- `public static boolean processKeyBindings(java.awt.event.KeyEvent)`
- `public static final boolean isRectangleContainingRectangle(java.awt.Rectangle, java.awt.Rectangle)`
- `public static int computeStringWidth(java.awt.FontMetrics, java.lang.String)`
- `public static int getAccessibleChildrenCount(java.awt.Component)`
- `public static int getAccessibleIndexInParent(java.awt.Component)`
- `public static java.awt.Component findFocusOwner(java.awt.Component)`
- `public static java.awt.Component getDeepestComponentAt(java.awt.Component, int, int)`
- `public static java.awt.Component getRoot(java.awt.Component)`
- `public static java.awt.Component getUnwrappedView(javax.swing.JViewport)`
- `public static java.awt.Container getAncestorNamed(java.lang.String, java.awt.Component)`
- `public static java.awt.Container getAncestorOfClass(java.lang.Class, java.awt.Component)`
- `public static java.awt.Container getUnwrappedParent(java.awt.Component)`
- `public static java.awt.Point convertPoint(java.awt.Component, int, int, java.awt.Component)`
- `public static java.awt.Point convertPoint(java.awt.Component, java.awt.Point, java.awt.Component)`
- `public static java.awt.Rectangle calculateInnerArea(javax.swing.JComponent, java.awt.Rectangle)`
- `public static java.awt.Rectangle computeIntersection(int, int, int, int, java.awt.Rectangle)`
- `public static java.awt.Rectangle computeUnion(int, int, int, int, java.awt.Rectangle)`
- `public static java.awt.Rectangle convertRectangle(java.awt.Component, java.awt.Rectangle, java.awt.Component)`
- `public static java.awt.Rectangle getLocalBounds(java.awt.Component)`
- `public static java.awt.Rectangle[] computeDifference(java.awt.Rectangle, java.awt.Rectangle)`
- `public static java.awt.Window getWindowAncestor(java.awt.Component)`
- `public static java.awt.Window windowForComponent(java.awt.Component)`
- `public static java.awt.event.MouseEvent convertMouseEvent(java.awt.Component, java.awt.event.MouseEvent, java.awt.Component)`
- `public static java.lang.String layoutCompoundLabel(java.awt.FontMetrics, java.lang.String, javax.swing.Icon, int, int, int, int, java.awt.Rectangle, java.awt.Rectangle, java.awt.Rectangle, int)`
- `public static java.lang.String layoutCompoundLabel(javax.swing.JComponent, java.awt.FontMetrics, java.lang.String, javax.swing.Icon, int, int, int, int, java.awt.Rectangle, java.awt.Rectangle, java.awt.Rectangle, int)`
- `public static javax.accessibility.Accessible getAccessibleAt(java.awt.Component, java.awt.Point)`
- `public static javax.accessibility.Accessible getAccessibleChild(java.awt.Component, int)`
- `public static javax.accessibility.AccessibleStateSet getAccessibleStateSet(java.awt.Component)`
- `public static javax.swing.ActionMap getUIActionMap(javax.swing.JComponent)`
- `public static javax.swing.InputMap getUIInputMap(javax.swing.JComponent, int)`
- `public static javax.swing.JRootPane getRootPane(java.awt.Component)`
- `public static void convertPointFromScreen(java.awt.Point, java.awt.Component)`
- `public static void convertPointToScreen(java.awt.Point, java.awt.Component)`
- `public static void invokeAndWait(java.lang.Runnable)`
- `public static void invokeLater(java.lang.Runnable)`
- `public static void paintComponent(java.awt.Graphics, java.awt.Component, java.awt.Container, int, int, int, int)`
- `public static void paintComponent(java.awt.Graphics, java.awt.Component, java.awt.Container, java.awt.Rectangle)`
- `public static void replaceUIActionMap(javax.swing.JComponent, javax.swing.ActionMap)`
- `public static void replaceUIInputMap(javax.swing.JComponent, int, javax.swing.InputMap)`
- `public static void updateComponentTreeUI(java.awt.Component)`

---

