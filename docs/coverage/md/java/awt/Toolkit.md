# Class: `Toolkit` ![Coverage](https://img.shields.io/badge/coverage-44.1%25-orange)

**Full Name:** `java.awt.Toolkit`

**Coverage:** 26 / 59 (44.1%)

```
[██████████████████████░░░░░░░░░░░░░░░░░░░░░░░░░░░░] 44.1%
```

## ✓ Implemented Methods

- `protected abstract java.awt.EventQueue getSystemEventQueueImpl()`
- `public abstract boolean prepareImage(java.awt.Image, int, int, java.awt.image.ImageObserver)`
- `public abstract int checkImage(java.awt.Image, int, int, java.awt.image.ImageObserver)`
- `public abstract int getScreenResolution()`
- `public abstract java.awt.Dimension getScreenSize()`
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
- `public java.awt.Dimension getBestCursorSize(int, int)`
- `public java.awt.Image createImage(byte[])`
- `public java.awt.Insets getScreenInsets(java.awt.GraphicsConfiguration)`
- `public static java.awt.Toolkit getDefaultToolkit()`
- `public static java.lang.String getProperty(java.lang.String, java.lang.String)`

## ✗ Missing Methods

- `protected boolean isDynamicLayoutSet()`
- `protected final void setDesktopProperty(java.lang.String, java.lang.Object)`
- `protected java.lang.Object lazilyLoadDesktopProperty(java.lang.String)`
- `protected static java.awt.Container getNativeContainer(java.awt.Component)`
- `protected void initializeDesktopProperties()`
- `protected void loadSystemColors(int[])`
- `public abstract boolean isModalExclusionTypeSupported(java.awt.Dialog$ModalExclusionType)`
- `public abstract boolean isModalityTypeSupported(java.awt.Dialog$ModalityType)`
- `public abstract java.awt.PrintJob getPrintJob(java.awt.Frame, java.lang.String, java.util.Properties)`
- `public abstract java.awt.datatransfer.Clipboard getSystemClipboard()`
- `public abstract java.util.Map mapInputMethodHighlight(java.awt.im.InputMethodHighlight)`
- `public boolean areExtraMouseButtonsEnabled()`
- `public boolean getLockingKeyState(int)`
- `public boolean isAlwaysOnTopSupported()`
- `public boolean isDynamicLayoutActive()`
- `public boolean isFrameStateSupported(int)`
- `public java.awt.Cursor createCustomCursor(java.awt.Image, java.awt.Point, java.lang.String)`
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

## ✗ Missing Fields

- `protected final java.beans.PropertyChangeSupport desktopPropsSupport`
- `protected final java.util.Map desktopProperties`

## ✗ Missing Constructors

- `protected java.awt.Toolkit()`


[← Back to Package](index.md)
