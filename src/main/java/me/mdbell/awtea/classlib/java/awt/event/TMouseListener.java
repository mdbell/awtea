package me.mdbell.awtea.classlib.java.awt.event;

import java.util.EventListener;


/**
 * @see java.awt.event.MouseListener
 */
public interface TMouseListener extends EventListener {

    /**
     * Invoked when the mouse button has been clicked (pressed
     * and released) on a component.
     *
     * @param e the event to be processed
     */
    public void mouseClicked(TMouseEvent e);

    /**
     * Invoked when a mouse button has been pressed on a component.
     *
     * @param e the event to be processed
     */
    public void mousePressed(TMouseEvent e);

    /**
     * Invoked when a mouse button has been released on a component.
     *
     * @param e the event to be processed
     */
    public void mouseReleased(TMouseEvent e);

    /**
     * Invoked when the mouse enters a component.
     *
     * @param e the event to be processed
     */
    public void mouseEntered(TMouseEvent e);

    /**
     * Invoked when the mouse exits a component.
     *
     * @param e the event to be processed
     */
    public void mouseExited(TMouseEvent e);
}
