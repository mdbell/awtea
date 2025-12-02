package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * TeaVM implementation of java.awt.AWTEvent.
 * The root class from which all AWT events are derived.
 */
@RequiredArgsConstructor
@Getter
public class TAWTEvent {

    /**
     * The event's id.
     */
    private final int id;

    /**
     * Controls whether or not the event is sent back down to the peer once the
     * source has processed it - false means it's sent to the peer; true means
     * it's not.
     */
    protected boolean consumed = false;

    /**
     * Consumes this event, if this event can be consumed.
     * Only low-level, system events can be consumed.
     */
    public void consume() {
        consumed = true;
    }

    /**
     * Returns whether this event has been consumed.
     */
    public boolean isConsumed() {
        return consumed;
    }

    /**
     * Returns a String representation of this object.
     */
    @Override
    public String toString() {
        return getClass().getName() + "[id=" + id + ",consumed=" + consumed + "]";
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
