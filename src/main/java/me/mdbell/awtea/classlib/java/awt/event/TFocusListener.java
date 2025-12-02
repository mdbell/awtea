package me.mdbell.awtea.classlib.java.awt.event;

import java.util.EventListener;

/**
 * @see java.awt.event.FocusListener
 */
public interface TFocusListener extends EventListener {

    void focusGained(TFocusEvent e);

    void focusLost(TFocusEvent e);
}
