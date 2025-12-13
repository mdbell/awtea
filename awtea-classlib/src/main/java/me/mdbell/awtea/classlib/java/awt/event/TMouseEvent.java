package me.mdbell.awtea.classlib.java.awt.event;

import lombok.Getter;
import lombok.ToString;
import me.mdbell.awtea.classlib.java.awt.TComponent;
import me.mdbell.awtea.input.MouseButtonType;

/**
 * @see java.awt.event.MouseEvent
 * 
 * Note: The BUTTON and MOUSE_ constants in this class match those defined in
 * {@link me.mdbell.awtea.input.MouseConstants}. They are redefined here
 * for compatibility with the java.awt.event.MouseEvent API.
 */
@Getter
@ToString(callSuper = true)
public class TMouseEvent extends TInputEvent {

	/**
	 * The first number in the range of ids used for mouse events.
	 */
	public static final int MOUSE_FIRST = 500;

	/**
	 * The last number in the range of ids used for mouse events.
	 */
	public static final int MOUSE_LAST = 507;

	/**
	 * The "mouse clicked" event. This {@code MouseEvent}
	 * occurs when a mouse button is pressed and released.
	 */
	public static final int MOUSE_CLICKED = MOUSE_FIRST;

	/**
	 * The "mouse pressed" event. This {@code MouseEvent}
	 * occurs when a mouse button is pushed down.
	 */
	public static final int MOUSE_PRESSED = 1 + MOUSE_FIRST; //Event.MOUSE_DOWN

	/**
	 * The "mouse released" event. This {@code MouseEvent}
	 * occurs when a mouse button is let up.
	 */
	public static final int MOUSE_RELEASED = 2 + MOUSE_FIRST; //Event.MOUSE_UP

	/**
	 * The "mouse moved" event. This {@code MouseEvent}
	 * occurs when the mouse position changes.
	 */
	public static final int MOUSE_MOVED = 3 + MOUSE_FIRST; //Event.MOUSE_MOVE

	/**
	 * The "mouse entered" event. This {@code MouseEvent}
	 * occurs when the mouse cursor enters the unobscured part of component's
	 * geometry.
	 */
	public static final int MOUSE_ENTERED = 4 + MOUSE_FIRST; //Event.MOUSE_ENTER

	/**
	 * The "mouse exited" event. This {@code MouseEvent}
	 * occurs when the mouse cursor exits the unobscured part of component's
	 * geometry.
	 */
	public static final int MOUSE_EXITED = 5 + MOUSE_FIRST; //Event.MOUSE_EXIT

	/**
	 * The "mouse dragged" event. This {@code MouseEvent}
	 * occurs when the mouse position changes while a mouse button is pressed.
	 */
	public static final int MOUSE_DRAGGED = 6 + MOUSE_FIRST; //Event.MOUSE_DRAG

	/**
	 * The "mouse wheel" event.  This is the only {@code MouseWheelEvent}.
	 * It occurs when a mouse equipped with a wheel has its wheel rotated.
	 *
	 * @since 1.4
	 */
	public static final int MOUSE_WHEEL = 7 + MOUSE_FIRST;

	/**
	 * Indicates no mouse buttons; used by {@link #getButton}.
	 *
	 * @since 1.4
	 */
	public static final int NOBUTTON = 0;

	/**
	 * Indicates mouse button #1; used by {@link #getButton}.
	 *
	 * @since 1.4
	 */
	public static final int BUTTON1 = 1;

	/**
	 * Indicates mouse button #2; used by {@link #getButton}.
	 *
	 * @since 1.4
	 */
	public static final int BUTTON2 = 2;

	/**
	 * Indicates mouse button #3; used by {@link #getButton}.
	 *
	 * @since 1.4
	 */
	public static final int BUTTON3 = 3;

	private final int x, y;

	private final MouseButtonType mouseButton;

	private final boolean meta;

	public TMouseEvent(TComponent component, int id, int x, int y, MouseButtonType button, boolean meta) {
		super(component, id, System.currentTimeMillis(), 0);
		this.x = x;
		this.y = y;
		this.mouseButton = button;
		this.meta = meta;
	}

	public int getButton() {
		return mouseButton.getJava();
	}

	public boolean isPopupTrigger() {
		return false;
	}

	public boolean isMetaDown() {
		return meta;
	}

}
