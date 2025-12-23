package me.mdbell.awtea.classlib.java.awt;

import java.util.ArrayList;
import java.util.List;

/**
 * A FocusTraversalPolicy that determines traversal order based on the order
 * that child Components were added to a Container. From a particular focus
 * cycle root, the policy makes a pre-order traversal of the Component
 * hierarchy, and traverses a Container's children according to the order
 * they were added.
 * <p>
 * Components that are {@code isFocusable() == false} are not included in
 * the traversal order.
 * <p>
 * By default, ContainerOrderFocusTraversalPolicy implicitly transfers focus
 * down-cycle. That is, during normal forward focus traversal, the Component
 * traversed after a focus cycle root will be the focus-cycle-root's default
 * Component to focus. This behavior can be disabled using the
 * {@code setImplicitDownCycleTraversal} method.
 *
 * @see java.awt.ContainerOrderFocusTraversalPolicy
 */
public class TContainerOrderFocusTraversalPolicy extends TFocusTraversalPolicy {

    private boolean implicitDownCycleTraversal = true;

    /**
     * Returns whether this ContainerOrderFocusTraversalPolicy transfers focus
     * down-cycle implicitly.
     *
     * @return whether this ContainerOrderFocusTraversalPolicy transfers focus
     *         down-cycle implicitly
     * @see #setImplicitDownCycleTraversal
     */
    public boolean getImplicitDownCycleTraversal() {
        return implicitDownCycleTraversal;
    }

    /**
     * Sets whether this ContainerOrderFocusTraversalPolicy transfers focus
     * down-cycle implicitly. If {@code true}, during normal forward focus
     * traversal, the Component traversed after a focus cycle root will be the
     * focus-cycle-root's default Component to focus. If {@code false}, the
     * next Component in the focus traversal cycle rooted at the specified
     * focus cycle root will be traversed instead. The default value for this
     * property is {@code true}.
     *
     * @param implicitDownCycleTraversal whether this
     *        ContainerOrderFocusTraversalPolicy transfers focus down-cycle
     *        implicitly
     * @see #getImplicitDownCycleTraversal
     */
    public void setImplicitDownCycleTraversal(boolean implicitDownCycleTraversal) {
        this.implicitDownCycleTraversal = implicitDownCycleTraversal;
    }

    @Override
    public TComponent getComponentAfter(TContainer aContainer, TComponent aComponent) {
        if (aContainer == null || aComponent == null) {
            throw new IllegalArgumentException("aContainer and aComponent cannot be null");
        }

        List<TComponent> components = getAcceptableComponents(aContainer);
        if (components.isEmpty()) {
            return null;
        }

        int index = components.indexOf(aComponent);
        if (index == -1) {
            // Component not in list, return first
            return components.get(0);
        }

        // Return next component, wrapping around
        return components.get((index + 1) % components.size());
    }

    @Override
    public TComponent getComponentBefore(TContainer aContainer, TComponent aComponent) {
        if (aContainer == null || aComponent == null) {
            throw new IllegalArgumentException("aContainer and aComponent cannot be null");
        }

        List<TComponent> components = getAcceptableComponents(aContainer);
        if (components.isEmpty()) {
            return null;
        }

        int index = components.indexOf(aComponent);
        if (index == -1) {
            // Component not in list, return last
            return components.get(components.size() - 1);
        }

        // Return previous component, wrapping around
        return components.get((index - 1 + components.size()) % components.size());
    }

    @Override
    public TComponent getFirstComponent(TContainer aContainer) {
        if (aContainer == null) {
            throw new IllegalArgumentException("aContainer cannot be null");
        }

        List<TComponent> components = getAcceptableComponents(aContainer);
        return components.isEmpty() ? null : components.get(0);
    }

    @Override
    public TComponent getLastComponent(TContainer aContainer) {
        if (aContainer == null) {
            throw new IllegalArgumentException("aContainer cannot be null");
        }

        List<TComponent> components = getAcceptableComponents(aContainer);
        return components.isEmpty() ? null : components.get(components.size() - 1);
    }

    @Override
    public TComponent getDefaultComponent(TContainer aContainer) {
        return getFirstComponent(aContainer);
    }

    /**
     * Returns a list of all acceptable (focusable and visible) components
     * within the container in traversal order.
     *
     * @param aContainer the container to search
     * @return list of acceptable components
     */
    protected List<TComponent> getAcceptableComponents(TContainer aContainer) {
        List<TComponent> result = new ArrayList<>();
        collectAcceptableComponents(aContainer, result, aContainer);
        return result;
    }

    /**
     * Recursively collects all acceptable components from the container hierarchy.
     *
     * @param current the current component to process
     * @param result the list to collect components into
     * @param root the root container (focus cycle root)
     */
    private void collectAcceptableComponents(TComponent current, List<TComponent> result, TContainer root) {
        // Check if this component is acceptable
        if (accept(current)) {
            result.add(current);
        }

        // If it's a container, recurse into children (unless it's a focus cycle root and not the initial root)
        if (current instanceof TContainer) {
            TContainer container = (TContainer) current;
            
            // Don't traverse into nested focus cycle roots unless implicit down-cycle is enabled
            boolean isNestedCycleRoot = container.isFocusCycleRoot() && container != root;
            if (!isNestedCycleRoot || (implicitDownCycleTraversal && accept(container))) {
                for (TComponent child : container.getComponents()) {
                    if (child.isVisible()) {
                        collectAcceptableComponents(child, result, root);
                    }
                }
            }
        }
    }

    /**
     * Determines whether a Component is an acceptable choice as the new
     * focus owner. The Component must be visible, displayable, enabled, and
     * focusable for it to be acceptable.
     *
     * @param component the Component to test
     * @return true if the Component is acceptable; false otherwise
     */
    protected boolean accept(TComponent component) {
        return component != null
                && component.isVisible()
                && component.isEnabled()
                && component.isFocusable();
    }
}
