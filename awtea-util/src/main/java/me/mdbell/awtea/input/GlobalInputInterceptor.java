package me.mdbell.awtea.input;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public interface GlobalInputInterceptor {

    /**
     * Handles a mouse event.
     *
     * @param e the mouse event
     * @return true to prevent the event being dispatched further, false to allow it.
     */
    boolean onMouseEvent(MouseEvent e);

    /**
     * Handles a key event.
     *
     * @param e the key event
     * @return true to prevent the event being dispatched further, false to allow it.
     */
    boolean onKeyEvent(KeyEvent e);
}
