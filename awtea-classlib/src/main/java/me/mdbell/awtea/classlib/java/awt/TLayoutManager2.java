package me.mdbell.awtea.classlib.java.awt;

/**
 * Defines an interface for classes that know how to lay out containers
 * based on a layout constraints object.
 * This is the awtea implementation of java.awt.LayoutManager2.
 *
 * @see TLayoutManager
 * @see TContainer
 * @see java.awt.LayoutManager2
 */
public interface TLayoutManager2 extends TLayoutManager {

    /**
     * Adds the specified component to the layout, using the specified
     * constraint object.
     *
     * @param comp        the component to be added
     * @param constraints where/how the component is added to the layout
     */
    void addLayoutComponent(TComponent comp, Object constraints);

    /**
     * Returns the maximum size of this component.
     *
     * @param target the container which needs to be laid out
     * @return the maximum size of the container
     * @see TComponent#getMaximumSize
     * @see TLayoutManager
     */
    TDimension maximumLayoutSize(TContainer target);

    /**
     * Returns the alignment along the x axis. This specifies how
     * the component would like to be aligned relative to other
     * components. The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     *
     * @param target the container
     * @return the alignment along the x axis
     */
    float getLayoutAlignmentX(TContainer target);

    /**
     * Returns the alignment along the y axis. This specifies how
     * the component would like to be aligned relative to other
     * components. The value should be a number between 0 and 1
     * where 0 represents alignment along the origin, 1 is aligned
     * the furthest away from the origin, 0.5 is centered, etc.
     *
     * @param target the container
     * @return the alignment along the y axis
     */
    float getLayoutAlignmentY(TContainer target);

    /**
     * Invalidates the layout, indicating that if the layout manager
     * has cached information it should be discarded.
     *
     * @param target the container
     */
    void invalidateLayout(TContainer target);
}
