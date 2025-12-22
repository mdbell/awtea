package me.mdbell.awtea.classlib.java.awt.awtea;

import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.classlib.java.awt.TAWTEvent;
import me.mdbell.awtea.classlib.java.awt.TComponent;
import me.mdbell.awtea.classlib.java.awt.TContainer;
import me.mdbell.awtea.classlib.java.awt.TToolkit;
import me.mdbell.awtea.classlib.java.awt.awtea.THitTestStrategy;
import me.mdbell.awtea.classlib.java.awt.awtea.TreeWalkHitTestStrategy;
import me.mdbell.awtea.classlib.java.awt.event.TFocusEvent;
import me.mdbell.awtea.classlib.java.awt.event.TKeyEvent;
import me.mdbell.awtea.classlib.java.awt.event.TMouseEvent;
import me.mdbell.awtea.classlib.java.awt.event.TMouseWheelEvent;
import me.mdbell.awtea.input.MouseButtonType;
import me.mdbell.awtea.input.MouseEventType;
import me.mdbell.awtea.util.ElementUtils;
import me.mdbell.awtea.util.JSObjectsExtensions;
import me.mdbell.awtea.util.NormalizedPoint;
import me.mdbell.awtea.util.Point;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.dom.events.*;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.TextRectangle;

import java.util.LinkedList;
import java.util.List;

/**
 * Generic "HTML element → AWT events" adapter.
 * <p>
 * Attaches DOM listeners to an element, converts them to our TAWTEvent
 * hierarchy,
 * and posts them to the AWT event queue. All registrations are tracked so they
 * can
 * be removed later via {@link #detach()}.
 */
@ExtensionMethod({ JSObjectsExtensions.class })
public final class TEventManager implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(TEventManager.class);

    public static final int SCROLL_AMOUNT = 3;

    private final HTMLElement element;
    private final TContainer container;

    private final List<Registration> registrations;

    // Track last mouse position to debounce duplicate mousemove events
    private int lastMouseX = Integer.MIN_VALUE;
    private int lastMouseY = Integer.MIN_VALUE;

    // Track the component currently under the mouse for synthesizing enter/exit
    // events
    private TComponent componentUnderMouse = null;
    
    // Hit-testing strategy (tree-walk by default)
    private THitTestStrategy componentHitStrategy;

    public TEventManager(HTMLElement element, TContainer container) {
        this.element = element;
        this.container = container;
        this.registrations = new LinkedList<>();
        
        // Initialize with tree-walk strategy (always available)
        this.componentHitStrategy = new TreeWalkHitTestStrategy(container);
        
        log.debug("TEventManager initialized with tree-walk hit-test strategy");
    }
    
    /**
     * Sets the hit-testing strategy.
     * This allows switching between tree-walk and GPU-based picking.
     * 
     * @param strategy the new hit-test strategy
     */
    public void setHitTestStrategy(THitTestStrategy strategy) {
        if (this.componentHitStrategy != null) {
            this.componentHitStrategy.dispose();
        }
        this.componentHitStrategy = strategy;
        log.debug("Hit-test strategy changed to: {}", strategy.getClass().getSimpleName());
    }
    
    /**
     * Invalidates the hit-test strategy, forcing it to rebuild cached data.
     * Should be called when the component hierarchy or layout changes.
     */
    public void invalidateHitTest() {
        if (componentHitStrategy != null) {
            componentHitStrategy.invalidate();
        }
    }

    /**
     * Suppress the default browser context menu on right-click.
     */
    public TEventManager disableContextMenu() {
        element.onEvent("contextmenu", Event::preventDefault).track(registrations);
        return this;
    }

    /**
     * Attach mouse listeners, mapping to TMouseEvent.
     */
    public TEventManager withMouse() {
        for (MouseEventType type : MouseEventType.values()) {
            if (type == MouseEventType.WHEEL) {
                continue; // Handled separately
            }
            element.onEvent(type.getType(), e -> {
                MouseEvent me = (MouseEvent) e;
                MouseButtonType button = MouseButtonType.fromHtml(me.getButton());
                Point point = new Point(me.getClientX(), me.getClientY());
                translatePoint(point, (HTMLCanvasElement) element);

                TComponent comp = getComponentAt(point);

                log.trace("Mouse event: type={}, button={}, point=({}, {}) on component={}",
                        type, button, point.getX(), point.getY(), comp.getClass().getName());

                // Synthesize MOUSE_ENTERED/MOUSE_EXITED events when the component under mouse
                // changes
                // This should happen on all mouse events (move, press, release, etc.)
                if (comp != componentUnderMouse) {
                    synthesizeComponentTransition(me, button, point, comp);
                }

                java.awt.Point onScreen = comp.getLocationOnScreen();

                point.translate(-onScreen.x, -onScreen.y);

                // Debounce mousemove events - only dispatch if coordinates changed
                if (type == MouseEventType.MOVED) {
                    if (point.getX() == lastMouseX && point.getY() == lastMouseY) {
                        return; // Skip duplicate event
                    }
                    lastMouseX = point.getX();
                    lastMouseY = point.getY();
                }

                TMouseEvent event = new TMouseEvent(comp, type.getId(),
                        point.getX(), point.getY(), button, me.getMetaKey());
                post(event);

            }).track(registrations);
        }

        // Handle when mouse leaves the canvas entirely - fire MOUSE_EXITED
        element.onEvent("mouseout", e -> {
            if (componentUnderMouse != null) {
                MouseEvent me = (MouseEvent) e;
                MouseButtonType button = MouseButtonType.fromHtml(me.getButton());

                // Use last known position relative to the component
                java.awt.Point onScreen = componentUnderMouse.getLocationOnScreen();
                Point point = new Point(me.getClientX(), me.getClientY());
                translatePoint(point, (HTMLCanvasElement) element);
                point.translate(-onScreen.x, -onScreen.y);

                TMouseEvent exitEvent = new TMouseEvent(componentUnderMouse,
                        TMouseEvent.MOUSE_EXITED,
                        point.getX(), point.getY(), button, me.getMetaKey());
                post(exitEvent);
                log.trace("Synthesized MOUSE_EXITED (canvas exit) for component={}",
                        componentUnderMouse.getClass().getName());

                componentUnderMouse = null;
            }
        }).track(registrations);

        return this;
    }

    /**
     * Attach mouse wheel listener, mapping to TMouseWheelEvent.
     */
    public TEventManager withMouseWheel() {
        element.onEvent("wheel", e -> {
            WheelEvent me = (WheelEvent) e;
            MouseButtonType button = MouseButtonType.fromHtml(me.getButton());
            Point point = new Point(me.getClientX(), me.getClientY());
            translatePoint(point, (HTMLCanvasElement) element);

            TComponent comp = getComponentAt(point);

            java.awt.Point onScreen = comp.getLocationOnScreen();

            point.translate(-onScreen.x, -onScreen.y);

            int scrollType = me.getDeltaMode(); // TODO: verify the values map 1-1
            double deltaY = me.getDeltaY();
            int rotation = (int) Math.signum(deltaY);
            int unitsToScroll = rotation * SCROLL_AMOUNT;
            boolean meta = me.getMetaKey();

            TMouseWheelEvent event = new TMouseWheelEvent(comp, MouseEventType.WHEEL.getId(),
                    point.getX(), point.getY(), button, meta,
                    deltaY,
                    SCROLL_AMOUNT,
                    scrollType,
                    unitsToScroll,
                    rotation);
            post(event);
        }).track(registrations);

        return this;
    }

    /**
     * Attach keyboard listeners, mapping to TKeyEvent.
     */
    public TEventManager withKeyboard() {
        for (TKeyEvent.KeyEvent type : TKeyEvent.KeyEvent.values()) {
            element.onEvent(type.getType(), e -> {
                TComponent focusOwner = TFocusManager.get().getGlobalFocusOwner();
                if (focusOwner == null) {
                    log.debug("No focus Owner! Discarding event");
                    return;
                }
                KeyboardEvent ke = (KeyboardEvent) e;
                TKeyEvent awt = TKeyEvent.adapt(focusOwner, ke);
                log.debug("Dispatching key event {}", awt);
                post(awt);
            }).track(registrations);
        }
        return this;
    }

    /**
     * Attach focus / blur listeners, mapping to TFocusEvent.
     */
    public TEventManager withFocus() {

        // need to ensure canvas is focusable
        element.setAttribute("tabindex", "0");

        element.onEvent("focus", e -> {
            post(new TFocusEvent(container, TFocusEvent.FOCUS_GAINED));
        }).track(registrations);

        element.onEvent("blur", e -> {
            post(new TFocusEvent(container, TFocusEvent.FOCUS_LOST));
        }).track(registrations);
        return this;
    }

    public void detach() {
        registrations.cleanup();
        // Reset mouse position tracking to prevent stale coordinates
        lastMouseX = Integer.MIN_VALUE;
        lastMouseY = Integer.MIN_VALUE;
        // Reset component tracking
        componentUnderMouse = null;
        
        // Dispose hit-test strategy
        if (componentHitStrategy != null) {
            componentHitStrategy.dispose();
            componentHitStrategy = null;
        }
    }

    @Override
    public void close() throws Exception {
        detach();
    }

    private TComponent getComponentAt(Point p) {
        if (componentHitStrategy == null) {
            // Fallback to direct container query if no strategy set
            TComponent component = container.getComponentAt(p.getX(), p.getY());
            if (component == null) {
                component = container;
            }
            return component;
        }
        return componentHitStrategy.getComponentAt(p.getX(), p.getY());
    }

    private void post(TAWTEvent event) {
        TToolkit.getEventQueue().postEvent(event);
    }

    /**
     * Handles the transition between components when the mouse moves from one
     * component to another.
     * Fires MOUSE_EXITED to the previous component and MOUSE_ENTERED to the new
     * component.
     *
     * @param me           the browser MouseEvent
     * @param button       the mouse button type
     * @param point        the mouse position (will be modified)
     * @param newComponent the component the mouse has moved to
     */
    private void synthesizeComponentTransition(MouseEvent me, MouseButtonType button,
            Point point, TComponent newComponent) {
        // Fire MOUSE_EXITED to the previous component
        if (componentUnderMouse != null) {
            fireMouseExited(me, button, componentUnderMouse);
        }

        // Fire MOUSE_ENTERED to the new component
        if (newComponent != null) {
            fireMouseEntered(button, point, newComponent, me.getMetaKey());
        }

        componentUnderMouse = newComponent;
    }

    /**
     * Fires a MOUSE_EXITED event to the specified component.
     *
     * @param me        the browser MouseEvent
     * @param button    the mouse button type
     * @param component the component to fire the event to
     */
    private void fireMouseExited(MouseEvent me, MouseButtonType button, TComponent component) {
        // Get the current mouse position in canvas coordinates
        Point canvasPoint = new Point(me.getClientX(), me.getClientY());
        translatePoint(canvasPoint, (HTMLCanvasElement) element);

        // Convert to coordinates relative to the exiting component
        java.awt.Point exitOnScreen = component.getLocationOnScreen();
        Point exitPoint = new Point(canvasPoint.getX(), canvasPoint.getY());
        exitPoint.translate(-exitOnScreen.x, -exitOnScreen.y);

        TMouseEvent exitEvent = new TMouseEvent(component,
                TMouseEvent.MOUSE_EXITED,
                exitPoint.getX(), exitPoint.getY(), button, me.getMetaKey());
        post(exitEvent);
        log.trace("Synthesized MOUSE_EXITED for component={}",
                component.getClass().getName());
    }

    /**
     * Fires a MOUSE_ENTERED event to the specified component.
     *
     * @param button    the mouse button type
     * @param point     the mouse position relative to the component
     * @param component the component to fire the event to
     * @param metaKey   whether the meta key is pressed
     */
    private void fireMouseEntered(MouseButtonType button, Point point,
            TComponent component, boolean metaKey) {
        TMouseEvent enterEvent = new TMouseEvent(component, TMouseEvent.MOUSE_ENTERED,
                point.getX(), point.getY(), button, metaKey);
        post(enterEvent);
        log.trace("Synthesized MOUSE_ENTERED for component={}",
                component.getClass().getName());
    }

    private static void translatePoint(Point p, HTMLCanvasElement element) {
        TextRectangle rect = element.getBoundingClientRect();
        if (ElementUtils.isFullscreen(element)) {

            // the client is scaled based on the hight of the element, and the element gets
            // set to the screen size
            // However the browser _also_ inserts padding to preserve the aspect ratio, so
            // we need to account for that

            double scale = rect.getHeight();
            scale /= element.getHeight();

            double xPadding = (rect.getWidth() - element.getWidth() * scale) / 2;
            double yPadding = (rect.getHeight() - element.getHeight() * scale) / 2;

            p.translate((int) -xPadding, (int) -yPadding);

            NormalizedPoint point = p.normalize((int) (rect.getWidth() - xPadding * 2),
                    (int) (rect.getHeight() - yPadding * 2));

            p.setX(point.getX(element.getWidth()));
            p.setY(point.getY(element.getHeight()));
            return;
        }

        p.translate(-rect.getLeft(), -rect.getTop());
        double xScale = element.getWidth();
        xScale /= rect.getWidth();

        double yScale = element.getHeight();
        yScale /= rect.getHeight();

        p.scale(xScale, yScale);
    }
}
