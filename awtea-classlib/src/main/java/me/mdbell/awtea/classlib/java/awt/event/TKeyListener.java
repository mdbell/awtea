package me.mdbell.awtea.classlib.java.awt.event;

import java.awt.event.KeyEvent;
import java.util.EventListener;

/**
 * @see java.awt.event.KeyListener
 */
public interface TKeyListener extends EventListener {

    /**
     * Invoked when a key has been typed.
     * See the class description for {@link KeyEvent} for a definition of
     * a key typed event.
     *
     * @param e the event to be processed
     */
    void keyTyped(TKeyEvent e);

    /**
     * Invoked when a key has been pressed.
     * See the class description for {@link KeyEvent} for a definition of
     * a key pressed event.
     *
     * @param e the event to be processed
     */
    void keyPressed(TKeyEvent e);

    /**
     * Invoked when a key has been released.
     * See the class description for {@link KeyEvent} for a definition of
     * a key released event.
     *
     * @param e the event to be processed
     */
    void keyReleased(TKeyEvent e);
}
