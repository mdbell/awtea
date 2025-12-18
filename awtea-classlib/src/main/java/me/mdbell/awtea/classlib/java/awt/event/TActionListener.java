package me.mdbell.awtea.classlib.java.awt.event;

import java.util.EventListener;

/**
 * The listener interface for receiving action events.
 * The class that is interested in processing an action event
 * implements this interface, and the object created with that
 * class is registered with a component, using the component's
 * addActionListener method. When the action event occurs,
 * that object's actionPerformed method is invoked.
 *
 * @see TActionEvent
 * @see java.awt.event.ActionListener
 */
public interface TActionListener extends EventListener {

	/**
	 * Invoked when an action occurs.
	 *
	 * @param e the action event
	 */
	void actionPerformed(TActionEvent e);
}
