package me.mdbell.awtea.classlib.java.awt.event;

import java.awt.event.ComponentEvent;
import java.util.EventListener;

/**
 * @see java.awt.event.ComponentListener
 */
public interface TComponentListener extends EventListener {
    /**
     * Invoked when the component's size changes.
     *
     * @param e the event to be processed
     */
    void componentResized(ComponentEvent e);

    /**
     * Invoked when the component's position changes.
     *
     * @param e the event to be processed
     */
    void componentMoved(ComponentEvent e);

    /**
     * Invoked when the component has been made visible.
     *
     * @param e the event to be processed
     */
    void componentShown(ComponentEvent e);

    /**
     * Invoked when the component has been made invisible.
     *
     * @param e the event to be processed
     */
    void componentHidden(ComponentEvent e);
}
