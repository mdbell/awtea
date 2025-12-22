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
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.teavm.classlib.java.awt.TDimension;

/**
 * @see java.awt.Component
 */
public abstract class TComponent implements TImageObserver {

    private static final Logger log = LoggerFactory.getLogger(TComponent.class);
    
    // Component ID management for GPU-based hit picking
    private static final AtomicInteger nextComponentId = new AtomicInteger(1);
    private static final Map<Integer, TComponent> componentRegistry = new ConcurrentHashMap<>();
    
    @Getter
    private final int componentId;
    
    /**
     * Looks up a component by its unique ID.
     * Used by GPU-based hit picking to resolve component IDs from picking buffer.
     * 
     * @param id the component ID
     * @return the component with the specified ID, or null if not found
     */
    public static TComponent getComponentById(int id) {
        return componentRegistry.get(id);
    }
    
    /**
     * Unregisters a component from the ID registry.
     * Should be called when a component is permanently disposed.
     * 
     * @param component the component to unregister
     */
    static void unregisterComponent(TComponent component) {
        if (component != null) {
            componentRegistry.remove(component.componentId);
        }
    }
    
    /**
     * Constructor that assigns a unique ID to this component.
     */
    public TComponent() {
        this.componentId = nextComponentId.getAndIncrement();
        componentRegistry.put(this.componentId, this);
    }

    @Getter
    @Setter
    TContainer parent;

    @Getter
    @Setter
    protected int x, y, width, height;

    @Setter
    @Getter
    private TDimension preferredSize;

    @Setter
    @Getter
    private TDimension minimumSize;

    @Getter
    private boolean focusable = true;

    private boolean focusTraversalKeysEnabled = true;

    @Getter
    @Setter
    private boolean enabled = true;

    @Getter
    private boolean valid = false;

    @Getter
    private boolean visible = true;

    private Color background = Color.LIGHT_GRAY;

    private Color foreground = Color.BLACK;

    private TFont font;

    protected List<TMouseListener> mouseListeners = new LinkedList<>();
    protected List<TMouseMotionListener> mouseMotionListeners = new LinkedList<>();
    protected List<TMouseWheelListener> mouseWheelListeners = new LinkedList<>();
    protected List<TKeyListener> keyListeners = new LinkedList<>();
    protected List<TFocusListener> focusListeners = new LinkedList<>();

    // used in the event queue for caching
    // we shouldn't touch this directly, and leave it to TEventQueue
    TEventQueue.EventQueueItem[] eventCache;

    public TFontMetrics getFontMetrics(TFont font) {
        TGraphics g = getGraphics();
        if (g != null) {
            try {
                return g.getFontMetrics(font);
            } finally {
                g.dispose();
            }
        }

        // Fallback: delegate to parent component if available, to avoid surprising
        // nulls
        TContainer p = getParent();
        if (p != null) {
            return p.getFontMetrics(font);
        }

        // No graphics and no parent to delegate to
        return null;
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

    public TFont getFont() {
        if (this.font != null) {
            return this.font;
        } else if (this.parent != null) {
            return this.parent.getFont();
        } else {
            return null;
        }
    }

    public void setFont(TFont font) {
        TFont old = this.font;
        this.font = font;
        firePropertyChange("font", old, font);
        repaint();
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
        // paint events are dispatched to the parent container for clipping and
        // double-buffering
        // so we just forward it up the chain
        if (parent != null) {
            parent.dispatchPaintEvent(event);
            return;
        }
        TGraphics graphics = this.getGraphics();
        if (graphics == null) {
            return;
        }
        try {
            if (event.getID() == TPaintEvent.PAINT) {
                // TODO: clip rect
                paint(graphics);
            } else if (event.getID() == TPaintEvent.UPDATE) {
                update(graphics);
            } else {
                log.warn("Unhandled paint event id: {}", event.getID());
            }
            graphics.dispose();
        } finally {
            graphics.dispose();
        }
    }

    protected void dispatchMouseWheelEvent(TMouseWheelEvent e) {
        dispatchEvent(e, mouseWheelListeners, TMouseWheelListener::mouseWheelMoved);
    }

    protected void dispatchKeyEvent(TKeyEvent e) {
        // First, check if this is a focus traversal key
        if (TKeyboardFocusManager.getCurrentKeyboardFocusManager().processKeyEvent(this, e)) {
            e.consume();
            return;
        }

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
        TKeyboardFocusManager.getCurrentKeyboardFocusManager().setGlobalFocusOwner(this);
    }

    /**
     * Returns whether focus traversal keys are enabled for this Component.
     * Components for which focus traversal keys are disabled receive key
     * events for focus traversal keys. Components for which focus traversal
     * keys are enabled do not see these events; instead, the events are
     * automatically converted to traversal operations.
     *
     * @return whether focus traversal keys are enabled for this Component
     * @see #setFocusTraversalKeysEnabled
     */
    public boolean isFocusTraversalKeysEnabled() {
        return focusTraversalKeysEnabled;
    }

    /**
     * Transfers the focus to the next component, as though this Component were
     * the focus owner.
     *
     * @see #requestFocus()
     */
    public void transferFocus() {
        TKeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(this);
    }

    /**
     * Transfers the focus to the previous component, as though this Component
     * were the focus owner.
     *
     * @see #requestFocus()
     */
    public void transferFocusBackward() {
        TKeyboardFocusManager.getCurrentKeyboardFocusManager().focusPreviousComponent(this);
    }

    /**
     * Transfers the focus up one focus traversal cycle. Typically, the focus
     * owner is set to this Component's focus cycle root, and the current focus
     * cycle root is set to the new focus owner's focus cycle root. If,
     * however, this Component's focus cycle root is a Window, then the focus
     * owner is set to the focus cycle root's default Component to focus, and
     * the current focus cycle root is unchanged.
     */
    public void transferFocusUpCycle() {
        TKeyboardFocusManager.getCurrentKeyboardFocusManager().upFocusCycle(this);
    }

    public void setFocusTraversalKeysEnabled(boolean focusTraversalKeysEnabled) {
        this.focusTraversalKeysEnabled = focusTraversalKeysEnabled;
    }

    /**
     * Returns the Container which is the focus cycle root of this Component's
     * focus traversal cycle. Each focus traversal cycle has only a single
     * focus cycle root and each Component which is not a Container belongs to
     * only a single focus traversal cycle. Containers which are focus cycle
     * roots belong to two cycles: one rooted at the Container itself, and one
     * rooted at the Container's nearest focus-cycle-root ancestor. For such
     * Containers, this method will return the Container's nearest focus-cycle-
     * root ancestor.
     *
     * @return this Component's focus cycle root, or null if no ancestor is a
     *         focus cycle root
     */
    public TContainer getFocusCycleRootAncestor() {
        TContainer parent = this.parent;
        while (parent != null) {
            if (parent.isFocusCycleRoot()) {
                return parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    public TGraphics getGraphics() {
        if (this.parent == null) {
            return null;
        }
        TGraphics parent = this.parent.getGraphics();
        if (parent == null) {
            return null;
        }
        return parent.create(x, y, width, height);
    }

    public void paint(TGraphics g) {

    }

    public void update(TGraphics g) {
        // g.clearRect(0, 0, width, height);
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
            validate();
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
            // force the render to be sync, so no component pop-in
            try (TGraphics g = getGraphics()) {
                update(g);
            }
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
            rate = 100; // semi-arbitrary value, the JVM hides this behind a flag + system property
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
        // normally a peer would be involved here, but we just create a buffered image
        // directly
        return new TBufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    public int checkImage(TImage image, TImageObserver observer) {
        return checkImage(image, -1, -1, observer);
    }

    // TODO: we already load images kinda sync, so we just stub these for now.
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
        // TODO: implement property change listeners
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

    public TRectangle getBounds() {
        return new TRectangle(x, y, width, height);
    }
}
