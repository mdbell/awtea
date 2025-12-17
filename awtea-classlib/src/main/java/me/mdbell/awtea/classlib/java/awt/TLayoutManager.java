package me.mdbell.awtea.classlib.java.awt;

import org.teavm.classlib.java.awt.TDimension;

/**
 * Defines the interface for classes that know how to lay out
 * {@code Container}s.
 * This is the awtea implementation of java.awt.LayoutManager.
 *
 * @see TContainer
 * @see java.awt.LayoutManager
 */
public interface TLayoutManager {

    /**
     * Adds the specified component with the specified name to the layout.
     *
     * @param name the name of the component
     * @param comp the component to be added
     */
    void addLayoutComponent(String name, TComponent comp);

    /**
     * Removes the specified component from the layout.
     *
     * @param comp the component to be removed
     */
    void removeLayoutComponent(TComponent comp);

    /**
     * Calculates the preferred size dimensions for the specified container,
     * given the components it contains.
     *
     * @param parent the container to be laid out
     * @return the preferred dimension for the container
     * @see #minimumLayoutSize
     */
    TDimension preferredLayoutSize(TContainer parent);

    /**
     * Calculates the minimum size dimensions for the specified container,
     * given the components it contains.
     *
     * @param parent the container to be laid out
     * @return the minimum dimension for the container
     * @see #preferredLayoutSize
     */
    TDimension minimumLayoutSize(TContainer parent);

    /**
     * Lays out the specified container.
     *
     * @param parent the container to be laid out
     */
    void layoutContainer(TContainer parent);
}
