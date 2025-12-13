package me.mdbell.awtea.classlib.java.awt.awtea;

import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.classlib.java.awt.TAWTEvent;
import me.mdbell.awtea.classlib.java.awt.TComponent;
import me.mdbell.awtea.classlib.java.awt.TContainer;
import me.mdbell.awtea.classlib.java.awt.TToolkit;
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
import org.teavm.jso.dom.events.*;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.TextRectangle;

import java.util.LinkedList;
import java.util.List;

/**
 * Generic "HTML element → AWT events" adapter.
 * <p>
 * Attaches DOM listeners to an element, converts them to our TAWTEvent hierarchy,
 * and posts them to the AWT event queue. All registrations are tracked so they can
 * be removed later via {@link #detach()}.
 */
@ExtensionMethod({JSObjectsExtensions.class})
public final class TEventManager implements AutoCloseable {

	public static final int SCROLL_AMOUNT = 3;

	private final HTMLElement element;
	private final TContainer container;

	private final List<Registration> registrations;

	// Track last mouse position to debounce duplicate mousemove events
	private int lastMouseX = Integer.MIN_VALUE;
	private int lastMouseY = Integer.MIN_VALUE;

	public TEventManager(HTMLElement element, TContainer container) {
		this.element = element;
		this.container = container;
		this.registrations = new LinkedList<>();
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
				rotation
			);
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
					return;
				}
				KeyboardEvent ke = (KeyboardEvent) e;
				TKeyEvent awt = TKeyEvent.adapt(focusOwner, ke);
				post(awt);
			}).track(registrations);
		}
		return this;
	}

	/**
	 * Attach focus / blur listeners, mapping to TFocusEvent.
	 */
	public TEventManager withFocus() {

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
	}

	@Override
	public void close() throws Exception {
		detach();
	}

	private TComponent getComponentAt(Point p) {
		TComponent component = container.getComponentAt(p.getX(), p.getY());
		if (component == null) {
			component = container;
		}
		return component;
	}

	private void post(TAWTEvent event) {
		TToolkit.getEventQueue().postEvent(event);
	}

	private static void translatePoint(Point p, HTMLCanvasElement element) {
		TextRectangle rect = element.getBoundingClientRect();
		if (ElementUtils.isFullscreen(element)) {


			// the client is scaled based on the hight of the element, and the element gets set to the screen size
			// However the browser _also_ inserts padding to preserve the aspect ratio, so we need to account for that

			double scale = rect.getHeight();
			scale /= element.getHeight();

			double xPadding = (rect.getWidth() - element.getWidth() * scale) / 2;
			double yPadding = (rect.getHeight() - element.getHeight() * scale) / 2;

			p.translate((int) -xPadding, (int) -yPadding);

			NormalizedPoint point = p.normalize((int) (rect.getWidth() - xPadding * 2), (int) (rect.getHeight() - yPadding * 2));

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
