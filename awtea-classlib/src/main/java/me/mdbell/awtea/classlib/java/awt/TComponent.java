package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.Setter;
import me.mdbell.awtea.classlib.java.awt.awtea.TFocusManager;
import me.mdbell.awtea.classlib.java.awt.event.*;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import me.mdbell.awtea.classlib.java.awt.image.TImageObserver;
import me.mdbell.awtea.classlib.java.awt.image.TImageProducer;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * @see java.awt.Component
 */
public abstract class TComponent implements TImageObserver {

    private static final Logger log = LoggerFactory.getLogger(TComponent.class);

    @Getter
    @Setter
    TContainer parent;

    @Getter
    @Setter
    protected int x, y, width, height;

    @Setter
    @Getter
    private Dimension preferredSize;

    /**
     * Gets the preferred size value (for internal use).
     */
    protected Dimension getPreferredSizeValue() {
        return preferredSize;
    }

    @Getter
    private boolean focusable = true;

    @Getter
    private boolean valid = true;

    @Getter
    private boolean visible = true;

    private Color background;

    private Color foreground;

    protected List<TMouseListener> mouseListeners = new LinkedList<>();
    protected List<TMouseMotionListener> mouseMotionListeners = new LinkedList<>();
    protected List<TMouseWheelListener> mouseWheelListeners = new LinkedList<>();
    protected List<TKeyListener> keyListeners = new LinkedList<>();
    protected List<TFocusListener> focusListeners = new LinkedList<>();

    // used in the event queue for caching
    // we shouldn't touch this directly, and leave it to TEventQueue
    TEventQueue.EventQueueItem[] eventCache;


    public TFontMetrics getFontMetrics(TFont font) {
        return getGraphics().measureText(font);
    }

    public Color getBackground() {
        if (this.background != null) {
            return this.background;
        } else if (this.parent != null) {
            return this.parent.getBackground();
        } else {
            return null;
        }
    }

    public void setFocusable(boolean focusable) {
        boolean oldFocusable = this.focusable;
        this.focusable = focusable;
        firePropertyChange("focusable", oldFocusable, focusable);
    }


    public void setBackground(Color c) {
        Color old = this.background;
        this.background = c;
        firePropertyChange("background", old, c);
    }

    public Color getForeground() {
        if (this.foreground != null) {
            return this.foreground;
        } else if (this.parent != null) {
            return this.parent.getForeground();
        } else {
            return null;
        }
    }

    public void setForeground(Color c) {
        Color old = this.foreground;
        this.foreground = c;
        firePropertyChange("foreground", old, c);
    }

    public void dispatchEvent(TAWTEvent event) {
        if (event.isConsumed()) {
            return;
        }

        TEventQueue.setCurrentEventAndMostRecentTime(event);

        if (event instanceof TKeyEvent) {
            dispatchKeyEvent((TKeyEvent) event);
        } else if (event instanceof TMouseWheelEvent) {
            dispatchMouseWheelEvent((TMouseWheelEvent) event);
        } else if (event instanceof TMouseEvent) {
            dispatchMouseEvent((TMouseEvent) event);
        } else if (event instanceof TFocusEvent) {
            dispatchFocusEvent((TFocusEvent) event);
        } else if (event instanceof TPaintEvent) {
            dispatchPaintEvent((TPaintEvent) event);
        } else if ((!(event instanceof TActionEvent))) {
            // action events are handled at higher level component directly, so we should
            // be ignoring them here to avoid noisy logs.
            log.warn("Unhandled event type: {}", event.getClass().getName());
        }
    }

    protected void dispatchPaintEvent(TPaintEvent event) {
        // paint events are dispatched to the parent container for clipping and double-buffering
        // so we just forward it up the chain
        if (parent != null) {
            parent.dispatchPaintEvent(event);
            return;
        }
        TGraphics graphics = this.getGraphics();
        if (event.getID() == TPaintEvent.PAINT) {
            //TODO: clip rect
            paint(graphics);
        } else if (event.getID() == TPaintEvent.UPDATE) {
            update(graphics);
        } else {
            log.warn("Unhandled paint event id: {}", event.getID());
        }
        graphics.dispose();
    }

    protected void dispatchMouseWheelEvent(TMouseWheelEvent e) {
        dispatchEvent(e, mouseWheelListeners, TMouseWheelListener::mouseWheelMoved);
    }

    protected void dispatchKeyEvent(TKeyEvent e) {
        int id = e.getID();
        switch (id) {
            case TKeyEvent.KEY_PRESSED:
                dispatchEvent(e, keyListeners, TKeyListener::keyPressed);
                break;
            case TKeyEvent.KEY_RELEASED:
                dispatchEvent(e, keyListeners, TKeyListener::keyReleased);
                break;
            case TKeyEvent.KEY_TYPED:
                dispatchEvent(e, keyListeners, TKeyListener::keyTyped);
                break;
            default:
                log.warn("Unhandled key event: {}", id);
        }
    }

    protected void dispatchMouseEvent(TMouseEvent e) {
        int id = e.getID();
        switch (id) {
            case TMouseEvent.MOUSE_MOVED:
                dispatchEvent(e, mouseMotionListeners, TMouseMotionListener::mouseMoved);
                break;
            case TMouseEvent.MOUSE_DRAGGED:
                dispatchEvent(e, mouseMotionListeners, TMouseMotionListener::mouseDragged);
                break;
            case TMouseEvent.MOUSE_CLICKED:
                dispatchEvent(e, mouseListeners, TMouseListener::mouseClicked);
                break;
            case TMouseEvent.MOUSE_PRESSED:
                requestFocus();
                dispatchEvent(e, mouseListeners, TMouseListener::mousePressed);
                break;
            case TMouseEvent.MOUSE_RELEASED:
                dispatchEvent(e, mouseListeners, TMouseListener::mouseReleased);
                break;
            case TMouseEvent.MOUSE_ENTERED:
                dispatchEvent(e, mouseListeners, TMouseListener::mouseEntered);
                break;
            case TMouseEvent.MOUSE_EXITED:
                dispatchEvent(e, mouseListeners, TMouseListener::mouseExited);
                break;
            default:
                log.warn("Unhandled mouse event: {}", id);
        }
    }

    protected void dispatchFocusEvent(TFocusEvent e) {

        if (!this.isFocusable()) {
            return;
        }

        int id = e.getID();
        switch (id) {
            case TFocusEvent.FOCUS_GAINED:
                dispatchEvent(e, focusListeners, TFocusListener::focusGained);
                break;
            case TFocusEvent.FOCUS_LOST:
                dispatchEvent(e, focusListeners, TFocusListener::focusLost);
                break;
            default:
                log.warn("Unhandled focus event: {}", id);
        }
    }

    public <T extends TComponentEvent, L> void dispatchEvent(
            T event, List<L> listeners, BiConsumer<L, T> eventHandler) {
        for (L listener : listeners) {
            eventHandler.accept(listener, event);
            if (event.isConsumed()) {
                break; // Stop sending to subsequent listeners
            }
        }
    }

    public void addKeyListener(TKeyListener l) {
        if (l == null) {
            return; // special case to match AWT behavior
        }
        this.keyListeners.add(l);
    }

    public void addMouseListener(TMouseListener l) {
        if (l == null) {
            return; // special case to match AWT behavior
        }
        this.mouseListeners.add(l);
    }

    public void removeKeyListener(TKeyListener l) {
        if (l == null) {
            return; // special case to match AWT behavior
        }
        this.keyListeners.remove(l);
    }

    public void removeMouseListener(TMouseListener l) {
        if (l == null) {
            return; // special case to match AWT behavior
        }
        this.mouseListeners.remove(l);
    }

    public void addFocusListener(TFocusListener l) {
        if (l == null) {
            return; // special case to match AWT behavior
        }
        this.focusListeners.add(l);
    }

    public void addMouseMotionListener(TMouseMotionListener l) {
        if (l == null) {
            return; // special case to match AWT behavior
        }
        this.mouseMotionListeners.add(l);
    }

    public void removeFocusListener(TFocusListener l) {
        if (l == null) {
            return; // special case to match AWT behavior
        }
        this.focusListeners.remove(l);
    }

    public void removeMouseMotionListener(TMouseMotionListener l) {
        if (l == null) {
            return; // special case to match AWT behavior
        }
        this.mouseMotionListeners.remove(l);
    }

    public void addMouseWheelListener(TMouseWheelListener l) {
        if (l == null) {
            return; // special case to match AWT behavior
        }
        this.mouseWheelListeners.add(l);
    }

    public void removeMouseWheelListener(TMouseWheelListener l) {
        if (l == null) {
            return; // special case to match AWT behavior
        }
        this.mouseWheelListeners.remove(l);
    }

    public void requestFocus() {
        TFocusManager.get().setGlobalFocusOwner(this);
    }

    public void setFocusTraversalKeysEnabled(boolean focusTraversalKeysEnabled) {

    }

    public TGraphics getGraphics() {
        TGraphics parent = this.parent.getGraphics();
        return parent.create(x, y, width, height);
    }

    public void paint(TGraphics g) {
        
    }

    public void update(TGraphics g) {
//		g.clearRect(0, 0, width, height);
        paint(g);
    }

    public void repaint() {
        repaint(0, 0, width, height);
    }

    public void repaint(int x, int y, int width, int height) {
        repaint(0L, x, y, width, height);
    }

    public void repaint(long tm, int x, int y, int width, int height) {
        postEvent(new TPaintEvent(this, TPaintEvent.PAINT, new TRectangle(x, y, width, height)));
    }

    public final boolean contains(int px, int py) {
        return px >= x && px < this.x + this.width && py >= y && py < this.y + this.height;
    }

    public final void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setVisible(boolean b) {
        this.visible = b;
        if (b) {
            repaint();
        }
    }

    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int width, int height) {
        if (this.width != width || this.height != height) {
            this.width = width;
            this.height = height;
            // Invalidate to trigger layout recalculation for children
            invalidate();
            repaint();
        }
    }

    public void revalidate() {
        TContainer root = this.getParent();
        if (root == null) {
            // no parent, so we only validate self
            validate();
        } else {
            // find the top-most validate root
            while (!root.isValidateRoot()) {
                root = root.getParent();
                if (root.getParent() == null) {
                    break;
                }
            }
            root.validate();
        }
    }

    public void validate() {
        boolean wasValid = this.isValid();
        valid = true;
        if (!wasValid) {
            repaint();
        }
    }

    public void invalidate() {
        valid = false;
        if (parent != null) {
            parent.invalidate();
        }
    }

    protected void postEvent(TAWTEvent event) {
        TToolkit.getEventQueue().postEvent(event);
    }

    public boolean imageUpdate(TImage img, int infoflags, int x, int y, int width, int height) {
        int rate = -1;
        if ((infoflags & (FRAMEBITS | ALLBITS)) != 0) {
            rate = 0;
        } else if ((infoflags & SOMEBITS) != 0) {
            rate = 100; // semi-arbitrary value, the JVM hides this behind a flag +  system property
        }
        if (rate >= 0) {
            repaint(rate, 0, 0, width, height);
        }
        return (infoflags & (ALLBITS | ABORT)) == 0;
    }

    public TImage createImage(TImageProducer producer) {
        return TToolkit.getDefaultToolkit().createImage(producer);
    }

    public TImage createImage(int width, int height) {
        // normally a peer would be involved here, but we just create a buffered image directly
        return new TBufferedImage(width, height);
    }

    public int checkImage(TImage image, TImageObserver observer) {
        return checkImage(image, -1, -1, observer);
    }

    //TODO: we already load images kinda sync, so we just stub these for now.
    public int checkImage(TImage image, int width, int height, TImageObserver o) {
        return TImageObserver.ALLBITS;
    }

    public boolean prepareImage(TImage image, TImageObserver observer) {
        return true;
    }

    public boolean prepareImage(TImage image, int width, int height, TImageObserver observer) {
        return true;
    }

    protected void firePropertyChange(String propertyName,
                                      Object oldValue, Object newValue) {
        //TODO: implement property change listeners
    }

    public Point getLocationOnScreen() {
        int absX = this.x;
        int absY = this.y;
        TContainer p = this.parent;
        while (p != null) {
            absX += p.getX();
            absY += p.getY();
            p = p.getParent();
        }
        return new Point(absX, absY);
    }

    public void fireFocusLost() {
        TToolkit.getEventQueue().postEvent(new TFocusEvent(this, TFocusEvent.FOCUS_LOST));
    }

    public void fireFocusGained() {
        TToolkit.getEventQueue().postEvent(new TFocusEvent(this, TFocusEvent.FOCUS_GAINED));
    }
}
