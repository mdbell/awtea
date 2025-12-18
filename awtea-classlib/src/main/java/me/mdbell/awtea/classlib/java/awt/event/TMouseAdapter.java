package me.mdbell.awtea.classlib.java.awt.event;

/**
 * An abstract adapter class for receiving mouse events.
 * The methods in this class are empty. This class exists as
 * convenience for creating listener objects.
 * <p>
 * Extend this class to create a {@code MouseEvent} listener
 * and override the methods for the events of interest. (If you implement the
 * {@code MouseListener} interface, you have to define all of
 * the methods in it. This abstract class defines null methods for them
 * all, so you can only have to define methods for events you care about.)
 * <p>
 * Create a listener object using the extended class and then register it with
 * a component using the component's {@code addMouseListener}
 * method. When a mouse button is pressed, released, or clicked (pressed and
 * released), or when the mouse cursor enters or exits the component,
 * the relevant method in the listener object is invoked
 * and the {@code MouseEvent} is passed to it.
 *
 * @see TMouseEvent
 * @see TMouseListener
 * @see java.awt.event.MouseAdapter
 */
public abstract class TMouseAdapter implements TMouseListener {
    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseClicked(TMouseEvent e) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(TMouseEvent e) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseReleased(TMouseEvent e) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseEntered(TMouseEvent e) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseExited(TMouseEvent e) {}
}
