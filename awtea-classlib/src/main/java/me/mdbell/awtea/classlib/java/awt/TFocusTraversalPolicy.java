package me.mdbell.awtea.classlib.java.awt;

/**
 * A FocusTraversalPolicy defines the order in which Components with a
 * particular focus cycle root are traversed. Instances can apply the policy to
 * arbitrary focus cycle roots, allowing themselves to be shared across
 * Containers. They do not need to be reinitialized when the focus cycle roots
 * of a Component hierarchy change.
 * <p>
 * The core responsibility of a FocusTraversalPolicy is to provide algorithms
 * for determining a Component's focus traversal order. Each FocusTraversalPolicy
 * must provide implementations for the following:
 * <ul>
 * <li>getComponentAfter</li>
 * <li>getComponentBefore</li>
 * <li>getFirstComponent</li>
 * <li>getLastComponent</li>
 * <li>getDefaultComponent</li>
 * </ul>
 * These methods form the basis of focus traversal. Focus traversal APIs will
 * defer to these methods when determining focus transferal.
 * <p>
 * FocusTraversalPolicies can optionally provide an algorithm for determining a
 * Window's initial Component. The initial Component is the first to receive
 * focus when the Window is first made visible.
 * <p>
 * FocusTraversalPolicies can also provide an algorithm for determining a
 * Component to receive focus when a Component is no longer focusable. The
 * default implementation of this algorithm is to return the Component after
 * the specified Component in the focus traversal cycle.
 *
 * @see java.awt.FocusTraversalPolicy
 */
public abstract class TFocusTraversalPolicy {

    /**
     * Returns the Component that should receive the focus after aComponent.
     * aContainer must be a focus cycle root of aComponent or a focus traversal
     * policy provider.
     *
     * @param aContainer a focus cycle root of aComponent or focus traversal
     *        policy provider
     * @param aComponent a (possibly indirect) child of aContainer, or
     *        aContainer itself
     * @return the Component that should receive the focus after aComponent, or
     *         null if no suitable Component can be found
     * @throws IllegalArgumentException if aContainer is not a focus cycle
     *         root of aComponent or a focus traversal policy provider, or if
     *         either aContainer or aComponent is null
     */
    public abstract TComponent getComponentAfter(TContainer aContainer, TComponent aComponent);

    /**
     * Returns the Component that should receive the focus before aComponent.
     * aContainer must be a focus cycle root of aComponent or a focus traversal
     * policy provider.
     *
     * @param aContainer a focus cycle root of aComponent or focus traversal
     *        policy provider
     * @param aComponent a (possibly indirect) child of aContainer, or
     *        aContainer itself
     * @return the Component that should receive the focus before aComponent,
     *         or null if no suitable Component can be found
     * @throws IllegalArgumentException if aContainer is not a focus cycle
     *         root of aComponent or a focus traversal policy provider, or if
     *         either aContainer or aComponent is null
     */
    public abstract TComponent getComponentBefore(TContainer aContainer, TComponent aComponent);

    /**
     * Returns the first Component in the traversal cycle. This method is used
     * to determine the next Component to focus when traversal wraps in the
     * forward direction.
     *
     * @param aContainer the focus cycle root or focus traversal policy provider
     *        whose first Component is to be returned
     * @return the first Component in the traversal cycle of aContainer,
     *         or null if no suitable Component can be found
     * @throws IllegalArgumentException if aContainer is null
     */
    public abstract TComponent getFirstComponent(TContainer aContainer);

    /**
     * Returns the last Component in the traversal cycle. This method is used
     * to determine the next Component to focus when traversal wraps in the
     * reverse direction.
     *
     * @param aContainer the focus cycle root or focus traversal policy provider
     *        whose last Component is to be returned
     * @return the last Component in the traversal cycle of aContainer,
     *         or null if no suitable Component can be found
     * @throws IllegalArgumentException if aContainer is null
     */
    public abstract TComponent getLastComponent(TContainer aContainer);

    /**
     * Returns the default Component to focus. This Component will be the first
     * to receive focus when traversing down into a new focus traversal cycle
     * rooted at aContainer.
     *
     * @param aContainer the focus cycle root or focus traversal policy provider
     *        whose default Component is to be returned
     * @return the default Component in the traversal cycle of aContainer,
     *         or null if no suitable Component can be found
     * @throws IllegalArgumentException if aContainer is null
     */
    public abstract TComponent getDefaultComponent(TContainer aContainer);

    /**
     * Returns the Component that should receive the focus when a Window is
     * made visible for the first time. Once the Window has been made visible
     * by a call to {@code show()} or {@code setVisible(true)}, the initial
     * Component will not be used again. Instead, if the Window loses and
     * subsequently regains focus, or is made invisible or undisplayable and
     * subsequently made visible and displayable, the Window's most recently
     * focused Component will become the focus owner. The default implementation
     * of this method returns the default Component.
     *
     * @param window the Window whose initial Component is to be returned
     * @return the Component that should receive the focus when window is made
     *         visible for the first time, or null if no suitable Component can
     *         be found
     * @see #getDefaultComponent
     * @see TWindow
     */
    public TComponent getInitialComponent(TWindow window) {
        return getDefaultComponent(window);
    }
}
