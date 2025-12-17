package me.mdbell.awtea.classlib.java.awt;

import java.util.HashMap;
import java.util.Map;

import org.teavm.classlib.java.awt.TDimension;

/**
 * A border layout lays out a container, arranging and resizing
 * its components to fit in five regions:
 * north, south, east, west, and center.
 * Each region may contain no more than one component, and
 * is identified by a corresponding constant:
 * {@code NORTH}, {@code SOUTH}, {@code EAST},
 * {@code WEST}, and {@code CENTER}.
 * <p>
 * When adding a component to a container with a border layout, use one of these
 * five constants, for example:
 * 
 * <pre>
 * Panel p = new Panel();
 * p.setLayout(new BorderLayout());
 * p.add(new Button("Okay"), BorderLayout.SOUTH);
 * </pre>
 * 
 * This is the awtea implementation of java.awt.BorderLayout.
 *
 * @see java.awt.BorderLayout
 */
public class TBorderLayout implements TLayoutManager2 {

    /**
     * The north layout constraint (top of container).
     */
    public static final String NORTH = "North";

    /**
     * The south layout constraint (bottom of container).
     */
    public static final String SOUTH = "South";

    /**
     * The east layout constraint (right side of container).
     */
    public static final String EAST = "East";

    /**
     * The west layout constraint (left side of container).
     */
    public static final String WEST = "West";

    /**
     * The center layout constraint (middle of container).
     */
    public static final String CENTER = "Center";

    /**
     * Synonym for PAGE_START. Exists for compatibility with previous versions.
     */
    public static final String BEFORE_FIRST_LINE = "First";

    /**
     * Synonym for PAGE_END. Exists for compatibility with previous versions.
     */
    public static final String AFTER_LAST_LINE = "Last";

    /**
     * Synonym for LINE_START. Exists for compatibility with previous versions.
     */
    public static final String BEFORE_LINE_BEGINS = "Before";

    /**
     * Synonym for LINE_END. Exists for compatibility with previous versions.
     */
    public static final String AFTER_LINE_ENDS = "After";

    /**
     * The component comes before the first line of the layout's content.
     * For Western, left-to-right and top-to-bottom orientations, this is equivalent
     * to NORTH.
     */
    public static final String PAGE_START = BEFORE_FIRST_LINE;

    /**
     * The component comes after the last line of the layout's content.
     * For Western, left-to-right and top-to-bottom orientations, this is equivalent
     * to SOUTH.
     */
    public static final String PAGE_END = AFTER_LAST_LINE;

    /**
     * The component goes at the beginning of the line direction for the layout.
     * For Western, left-to-right and top-to-bottom orientations, this is equivalent
     * to WEST.
     */
    public static final String LINE_START = BEFORE_LINE_BEGINS;

    /**
     * The component goes at the end of the line direction for the layout.
     * For Western, left-to-right and top-to-bottom orientations, this is equivalent
     * to EAST.
     */
    public static final String LINE_END = AFTER_LINE_ENDS;

    private TComponent north;
    private TComponent south;
    private TComponent east;
    private TComponent west;
    private TComponent center;

    private int hgap;
    private int vgap;

    /**
     * Constructs a new border layout with no gaps between components.
     */
    public TBorderLayout() {
        this(0, 0);
    }

    /**
     * Constructs a border layout with the specified gaps
     * between components.
     *
     * @param hgap the horizontal gap
     * @param vgap the vertical gap
     */
    public TBorderLayout(int hgap, int vgap) {
        this.hgap = hgap;
        this.vgap = vgap;
    }

    /**
     * Returns the horizontal gap between components.
     *
     * @return the horizontal gap between components
     */
    public int getHgap() {
        return hgap;
    }

    /**
     * Sets the horizontal gap between components.
     *
     * @param hgap the horizontal gap between components
     */
    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    /**
     * Returns the vertical gap between components.
     *
     * @return the vertical gap between components
     */
    public int getVgap() {
        return vgap;
    }

    /**
     * Sets the vertical gap between components.
     *
     * @param vgap the vertical gap between components
     */
    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    @Override
    public void addLayoutComponent(TComponent comp, Object constraints) {
        synchronized (comp.getParent().getTreeLock()) {
            if (constraints == null) {
                constraints = CENTER;
            }
            if (constraints instanceof String) {
                addLayoutComponent((String) constraints, comp);
            } else {
                throw new IllegalArgumentException("cannot add to layout: constraint must be a string");
            }
        }
    }

    @Override
    @Deprecated
    public void addLayoutComponent(String name, TComponent comp) {
        synchronized (comp.getParent().getTreeLock()) {
            // Remove the component from any previous position
            if (comp == north) {
                north = null;
            } else if (comp == south) {
                south = null;
            } else if (comp == east) {
                east = null;
            } else if (comp == west) {
                west = null;
            } else if (comp == center) {
                center = null;
            }

            // Add the component to the new position
            if (name == null) {
                name = CENTER;
            }

            if (NORTH.equals(name) || PAGE_START.equals(name) || BEFORE_FIRST_LINE.equals(name)) {
                north = comp;
            } else if (SOUTH.equals(name) || PAGE_END.equals(name) || AFTER_LAST_LINE.equals(name)) {
                south = comp;
            } else if (EAST.equals(name) || LINE_END.equals(name) || AFTER_LINE_ENDS.equals(name)) {
                east = comp;
            } else if (WEST.equals(name) || LINE_START.equals(name) || BEFORE_LINE_BEGINS.equals(name)) {
                west = comp;
            } else if (CENTER.equals(name)) {
                center = comp;
            } else {
                throw new IllegalArgumentException("cannot add to layout: unknown constraint: " + name);
            }
        }
    }

    @Override
    public void removeLayoutComponent(TComponent comp) {
        synchronized (comp.getParent().getTreeLock()) {
            if (comp == north) {
                north = null;
            } else if (comp == south) {
                south = null;
            } else if (comp == east) {
                east = null;
            } else if (comp == west) {
                west = null;
            } else if (comp == center) {
                center = null;
            }
        }
    }

    /**
     * Gets the component at the given position.
     *
     * @param constraints the desired position, one of the border layout constants
     * @return the component at the given position, or null
     */
    public TComponent getLayoutComponent(Object constraints) {
        if (NORTH.equals(constraints) || PAGE_START.equals(constraints) || BEFORE_FIRST_LINE.equals(constraints)) {
            return north;
        } else if (SOUTH.equals(constraints) || PAGE_END.equals(constraints) || AFTER_LAST_LINE.equals(constraints)) {
            return south;
        } else if (WEST.equals(constraints) || LINE_START.equals(constraints)
                || BEFORE_LINE_BEGINS.equals(constraints)) {
            return west;
        } else if (EAST.equals(constraints) || LINE_END.equals(constraints) || AFTER_LINE_ENDS.equals(constraints)) {
            return east;
        } else if (CENTER.equals(constraints)) {
            return center;
        }
        throw new IllegalArgumentException("cannot get component: unknown constraint: " + constraints);
    }

    /**
     * Gets the component that corresponds to the given constraint location.
     *
     * @param target      the container
     * @param constraints the desired absolute position, one of the border layout
     *                    constants
     * @return the component at the given location, or null
     */
    public TComponent getLayoutComponent(TContainer target, Object constraints) {
        return getLayoutComponent(constraints);
    }

    /**
     * Gets the constraints for the specified component.
     *
     * @param comp the component to be queried
     * @return the constraint for the specified component, or null
     */
    public Object getConstraints(TComponent comp) {
        if (comp == north) {
            return NORTH;
        } else if (comp == south) {
            return SOUTH;
        } else if (comp == east) {
            return EAST;
        } else if (comp == west) {
            return WEST;
        } else if (comp == center) {
            return CENTER;
        }
        return null;
    }

    @Override
    public TDimension minimumLayoutSize(TContainer target) {
        synchronized (target.getTreeLock()) {
            TDimension dim = new TDimension(0, 0);

            if (east != null) {
                TDimension d = getComponentSize(east);
                dim.width += d.width + hgap;
                dim.height = Math.max(d.height, dim.height);
            }
            if (west != null) {
                TDimension d = getComponentSize(west);
                dim.width += d.width + hgap;
                dim.height = Math.max(d.height, dim.height);
            }
            if (center != null) {
                TDimension d = getComponentSize(center);
                dim.width += d.width;
                dim.height = Math.max(d.height, dim.height);
            }
            if (north != null) {
                TDimension d = getComponentSize(north);
                dim.width = Math.max(d.width, dim.width);
                dim.height += d.height + vgap;
            }
            if (south != null) {
                TDimension d = getComponentSize(south);
                dim.width = Math.max(d.width, dim.width);
                dim.height += d.height + vgap;
            }

            TInsets insets = target.getInsets();
            dim.width += insets.left + insets.right;
            dim.height += insets.top + insets.bottom;

            return dim;
        }
    }

    @Override
    public TDimension preferredLayoutSize(TContainer target) {
        synchronized (target.getTreeLock()) {
            TDimension dim = new TDimension(0, 0);

            if (east != null) {
                TDimension d = getComponentSize(east);
                dim.width += d.width + hgap;
                dim.height = Math.max(d.height, dim.height);
            }
            if (west != null) {
                TDimension d = getComponentSize(west);
                dim.width += d.width + hgap;
                dim.height = Math.max(d.height, dim.height);
            }
            if (center != null) {
                TDimension d = getComponentSize(center);
                dim.width += d.width;
                dim.height = Math.max(d.height, dim.height);
            }
            if (north != null) {
                TDimension d = getComponentSize(north);
                dim.width = Math.max(d.width, dim.width);
                dim.height += d.height + vgap;
            }
            if (south != null) {
                TDimension d = getComponentSize(south);
                dim.width = Math.max(d.width, dim.width);
                dim.height += d.height + vgap;
            }

            TInsets insets = target.getInsets();
            dim.width += insets.left + insets.right;
            dim.height += insets.top + insets.bottom;

            return dim;
        }
    }

    @Override
    public TDimension maximumLayoutSize(TContainer target) {
        return new TDimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public float getLayoutAlignmentX(TContainer parent) {
        return 0.5f;
    }

    @Override
    public float getLayoutAlignmentY(TContainer parent) {
        return 0.5f;
    }

    @Override
    public void invalidateLayout(TContainer target) {
        // No cached information to invalidate
    }

    @Override
    public void layoutContainer(TContainer target) {
        synchronized (target.getTreeLock()) {
            TInsets insets = target.getInsets();
            int top = insets.top;
            int bottom = target.getHeight() - insets.bottom;
            int left = insets.left;
            int right = target.getWidth() - insets.right;

            if (north != null) {
                TDimension d = getComponentSize(north);
                north.setSize(right - left, d.height);
                north.setBounds(left, top, right - left, d.height);
                top += d.height + vgap;
            }
            if (south != null) {
                TDimension d = getComponentSize(south);
                south.setSize(right - left, d.height);
                south.setBounds(left, bottom - d.height, right - left, d.height);
                bottom -= d.height + vgap;
            }
            if (east != null) {
                TDimension d = getComponentSize(east);
                east.setSize(d.width, bottom - top);
                east.setBounds(right - d.width, top, d.width, bottom - top);
                right -= d.width + hgap;
            }
            if (west != null) {
                TDimension d = getComponentSize(west);
                west.setSize(d.width, bottom - top);
                west.setBounds(left, top, d.width, bottom - top);
                left += d.width + hgap;
            }
            if (center != null) {
                center.setBounds(left, top, right - left, bottom - top);
            }
        }
    }

    /**
     * Gets the preferred size of a component for layout calculations.
     * For containers, asks for their preferred layout size which will recursively
     * query child components and layout managers.
     * For leaf components, uses their explicitly set preferred size or current
     * dimensions.
     */
    private TDimension getComponentSize(TComponent comp) {
        if (comp instanceof TContainer) {
            return ((TContainer) comp).getPreferredLayoutSize();
        }
        // For non-container components, use current dimensions
        return new TDimension(comp.getWidth(), comp.getHeight());
    }

    /**
     * Returns a string representation of the state of this border layout.
     *
     * @return a string representation of this border layout
     */
    @Override
    public String toString() {
        return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap + "]";
    }
}
