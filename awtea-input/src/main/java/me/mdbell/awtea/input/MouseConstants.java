package me.mdbell.awtea.input;

/**
 * Constants for mouse events and buttons.
 * These constants match the values defined in java.awt.event.MouseEvent.
 */
public final class MouseConstants {

    private MouseConstants() {
        // Utility class
    }

    /**
     * Indicates no mouse button; used by {@link #getButton}.
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

    /**
     * The first number in the range of ids used for mouse events.
     */
    public static final int MOUSE_FIRST = 500;

    /**
     * The last number in the range of ids used for mouse events.
     */
    public static final int MOUSE_LAST = 507;

    /**
     * The "mouse clicked" event.
     */
    public static final int MOUSE_CLICKED = MOUSE_FIRST;

    /**
     * The "mouse pressed" event.
     */
    public static final int MOUSE_PRESSED = 1 + MOUSE_FIRST;

    /**
     * The "mouse released" event.
     */
    public static final int MOUSE_RELEASED = 2 + MOUSE_FIRST;

    /**
     * The "mouse moved" event.
     */
    public static final int MOUSE_MOVED = 3 + MOUSE_FIRST;

    /**
     * The "mouse entered" event.
     */
    public static final int MOUSE_ENTERED = 4 + MOUSE_FIRST;

    /**
     * The "mouse exited" event.
     */
    public static final int MOUSE_EXITED = 5 + MOUSE_FIRST;

    /**
     * The "mouse dragged" event.
     */
    public static final int MOUSE_DRAGGED = 6 + MOUSE_FIRST;

    /**
     * The "mouse wheel" event.
     */
    public static final int MOUSE_WHEEL = 7 + MOUSE_FIRST;
}
