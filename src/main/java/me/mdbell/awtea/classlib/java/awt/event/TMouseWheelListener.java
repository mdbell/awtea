package me.mdbell.awtea.classlib.java.awt.event;

import org.teavm.classlib.java.util.TEventListener;

/**
 * @see java.awt.event.MouseWheelListener
 */
public interface TMouseWheelListener extends TEventListener {

	void mouseWheelMoved(TMouseWheelEvent event);
}
