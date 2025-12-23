package me.mdbell.awtea.classlib.java.awt;

/**
 * A FocusTraversalPolicy that determines traversal order based on the order
 * of child Components in a Container. This is an alias for
 * {@link TContainerOrderFocusTraversalPolicy} and provides the default
 * focus traversal behavior.
 * <p>
 * From a particular focus cycle root, the policy makes a pre-order traversal
 * of the Component hierarchy, and traverses a Container's children according
 * to the order they were added.
 *
 * @see java.awt.DefaultFocusTraversalPolicy
 * @see TContainerOrderFocusTraversalPolicy
 */
public class TDefaultFocusTraversalPolicy extends TContainerOrderFocusTraversalPolicy {
    // This class extends TContainerOrderFocusTraversalPolicy and provides
    // the same behavior. In Java AWT, DefaultFocusTraversalPolicy may have
    // additional platform-specific logic, but for our implementation,
    // container order traversal is sufficient.
}
