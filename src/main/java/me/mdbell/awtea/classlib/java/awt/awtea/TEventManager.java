package me.mdbell.awtea.classlib.java.awt.awtea;

import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.classlib.java.awt.TAWTEvent;
import me.mdbell.awtea.classlib.java.awt.TContainer;
import me.mdbell.awtea.classlib.java.awt.TToolkit;
import me.mdbell.awtea.classlib.java.awt.event.TFocusEvent;
import me.mdbell.awtea.classlib.java.awt.event.TKeyEvent;
import me.mdbell.awtea.classlib.java.awt.event.TMouseEvent;
import me.mdbell.awtea.classlib.java.awt.event.TMouseWheelEvent;
import me.mdbell.awtea.input.MouseEventType;
import me.mdbell.awtea.util.JSObjectsExtensions;
import org.teavm.jso.dom.events.*;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLElement;

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

	private final HTMLElement element;
	private final TContainer container;

	private final List<Registration> registrations;

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
				TAWTEvent awt = TMouseEvent.adapt(container,
					(HTMLCanvasElement) element, me, type.getType());
				post(awt);
			}).track(registrations);
		}
		return this;
	}

	/**
	 * Attach mouse wheel listener, mapping to TMouseWheelEvent.
	 */
	public TEventManager withMouseWheel() {
		element.onEvent("wheel", e -> {
			WheelEvent we = (WheelEvent) e;
			TAWTEvent awt = TMouseWheelEvent.adapt(container,
				(HTMLCanvasElement) element, we, "wheel");
			post(awt);
		}).track(registrations);
		return this;
	}

	/**
	 * Attach keyboard listeners, mapping to TKeyEvent.
	 */
	public TEventManager withKeyboard() {
		for (TKeyEvent.KeyEvent type : TKeyEvent.KeyEvent.values()) {
			element.onEvent(type.getType(), e -> {
				KeyboardEvent ke = (KeyboardEvent) e;
				TKeyEvent awt = TKeyEvent.adapt(container, ke);
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
	}

	@Override
	public void close() throws Exception {
		detach();
	}

	private void post(TAWTEvent event) {
		TToolkit.getEventQueue().postEvent(event);
	}
}
