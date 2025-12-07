package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.ToString;

/**
 * @see java.awt.AWTEvent
 */
@ToString
public class TAWTEvent {

	@Getter
	private final Object source;
	private final int id;
	@Getter
	protected boolean consumed = false;

	public TAWTEvent(Object source, int id) {
		this.source = source;
		this.id = id;
		//TODO: handle events that are consumed by default
	}

	// java uses "getID" instead of "getId" - so no lombok here
	public int getID() {
		return id;
	}

	public void consume() {
		consumed = true;
	}


	// Event type constants for common AWT events
	public static final int COMPONENT_EVENT_MASK = 0x01;
	public static final int CONTAINER_EVENT_MASK = 0x02;
	public static final int FOCUS_EVENT_MASK = 0x04;
	public static final int KEY_EVENT_MASK = 0x08;
	public static final int MOUSE_EVENT_MASK = 0x10;
	public static final int MOUSE_MOTION_EVENT_MASK = 0x20;
	public static final int WINDOW_EVENT_MASK = 0x40;
	public static final int ACTION_EVENT_MASK = 0x80;
	public static final int ADJUSTMENT_EVENT_MASK = 0x100;
	public static final int ITEM_EVENT_MASK = 0x200;
	public static final int TEXT_EVENT_MASK = 0x400;
	public static final int INPUT_METHOD_EVENT_MASK = 0x800;
	public static final int PAINT_EVENT_MASK = 0x2000;
	public static final int INVOCATION_EVENT_MASK = 0x4000;
	public static final int HIERARCHY_EVENT_MASK = 0x8000;
	public static final int HIERARCHY_BOUNDS_EVENT_MASK = 0x10000;
	public static final int MOUSE_WHEEL_EVENT_MASK = 0x20000;
	public static final int WINDOW_STATE_EVENT_MASK = 0x40000;
	public static final int WINDOW_FOCUS_EVENT_MASK = 0x80000;

	public static final int RESERVED_ID_MAX = 1999;
}
