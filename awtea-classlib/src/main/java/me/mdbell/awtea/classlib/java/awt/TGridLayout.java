package me.mdbell.awtea.classlib.java.awt;

import org.teavm.classlib.java.awt.TDimension;

/**
 * The {@code TGridLayout} class is a layout manager that lays out a container's
 * components in a rectangular grid. The container is divided into equal-sized
 * rectangles, and one component is placed in each rectangle.
 * <p>
 * For example, the following is an applet that lays out six buttons
 * into three rows and two columns:
 * 
 * <pre>
 * import java.awt.*;
 * import java.applet.Applet;
 * 
 * public class ButtonGrid extends Applet {
 *     public void init() {
 *         setLayout(new GridLayout(3, 2));
 *         add(new Button("1"));
 *         add(new Button("2"));
 *         add(new Button("3"));
 *         add(new Button("4"));
 *         add(new Button("5"));
 *         add(new Button("6"));
 *     }
 * }
 * </pre>
 * 
 * This is the awtea implementation of java.awt.GridLayout.
 *
 * @see java.awt.GridLayout
 */
public class TGridLayout implements TLayoutManager {

    private int rows;
    private int cols;
    private int hgap;
    private int vgap;

    /**
     * Creates a grid layout with a default of one column per component,
     * in a single row.
     */
    public TGridLayout() {
        this(1, 0, 0, 0);
    }

    /**
     * Creates a grid layout with the specified number of rows and
     * columns. All components in the layout are given equal size.
     * <p>
     * One, but not both, of {@code rows} and {@code cols} can be zero,
     * which means that any number of objects can be placed in a row or in a column.
     *
     * @param rows the rows, with the value zero meaning any number of rows
     * @param cols the columns, with the value zero meaning any number of columns
     */
    public TGridLayout(int rows, int cols) {
        this(rows, cols, 0, 0);
    }

    /**
     * Creates a grid layout with the specified number of rows and columns.
     * All components in the layout are given equal size.
     * <p>
     * In addition, the horizontal and vertical gaps are set to the specified
     * values.
     * One, but not both, of {@code rows} and {@code cols} can be zero,
     * which means that any number of objects can be placed in a row or in a column.
     *
     * @param rows the rows, with the value zero meaning any number of rows
     * @param cols the columns, with the value zero meaning any number of columns
     * @param hgap the horizontal gap
     * @param vgap the vertical gap
     * @throws IllegalArgumentException if the value of both {@code rows} and
     *                                  {@code cols} is set to zero
     */
    public TGridLayout(int rows, int cols, int hgap, int vgap) {
        if ((rows == 0) && (cols == 0)) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.rows = rows;
        this.cols = cols;
        this.hgap = hgap;
        this.vgap = vgap;
    }

    /**
     * Gets the number of rows in this layout.
     *
     * @return the number of rows in this layout
     */
    public int getRows() {
        return rows;
    }

    /**
     * Sets the number of rows in this layout to the specified value.
     *
     * @param rows the number of rows in this layout
     * @throws IllegalArgumentException if the value of both {@code rows} and
     *                                  {@code this.cols} is set to zero
     */
    public void setRows(int rows) {
        if ((rows == 0) && (this.cols == 0)) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.rows = rows;
    }

    /**
     * Gets the number of columns in this layout.
     *
     * @return the number of columns in this layout
     */
    public int getColumns() {
        return cols;
    }

    /**
     * Sets the number of columns in this layout to the specified value.
     *
     * @param cols the number of columns in this layout
     * @throws IllegalArgumentException if the value of both {@code cols} and
     *                                  {@code this.rows} is set to zero
     */
    public void setColumns(int cols) {
        if ((cols == 0) && (this.rows == 0)) {
            throw new IllegalArgumentException("rows and cols cannot both be zero");
        }
        this.cols = cols;
    }

    /**
     * Gets the horizontal gap between components.
     *
     * @return the horizontal gap between components
     */
    public int getHgap() {
        return hgap;
    }

    /**
     * Sets the horizontal gap between components to the specified value.
     *
     * @param hgap the horizontal gap between components
     */
    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    /**
     * Gets the vertical gap between components.
     *
     * @return the vertical gap between components
     */
    public int getVgap() {
        return vgap;
    }

    /**
     * Sets the vertical gap between components to the specified value.
     *
     * @param vgap the vertical gap between components
     */
    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    @Override
    public void addLayoutComponent(String name, TComponent comp) {
        // GridLayout doesn't use component names
    }

    @Override
    public void removeLayoutComponent(TComponent comp) {
        // GridLayout doesn't maintain component-specific state
    }

    @Override
    public TDimension preferredLayoutSize(TContainer parent) {
        synchronized (parent.getTreeLock()) {
            TInsets insets = parent.getInsets();
            int ncomponents = parent.getComponents().length;
            int nrows = rows;
            int ncols = cols;

            if (nrows > 0) {
                ncols = (ncomponents + nrows - 1) / nrows;
            } else {
                nrows = (ncomponents + ncols - 1) / ncols;
            }

            int w = 0;
            int h = 0;
            for (TComponent comp : parent.getComponents()) {
                TDimension d = getComponentSize(comp);
                if (w < d.width) {
                    w = d.width;
                }
                if (h < d.height) {
                    h = d.height;
                }
            }

            return new TDimension(
                    insets.left + insets.right + ncols * w + (ncols - 1) * hgap,
                    insets.top + insets.bottom + nrows * h + (nrows - 1) * vgap);
        }
    }

    @Override
    public TDimension minimumLayoutSize(TContainer parent) {
        synchronized (parent.getTreeLock()) {
            TInsets insets = parent.getInsets();
            int ncomponents = parent.getComponents().length;
            int nrows = rows;
            int ncols = cols;

            if (nrows > 0) {
                ncols = (ncomponents + nrows - 1) / nrows;
            } else {
                nrows = (ncomponents + ncols - 1) / ncols;
            }

            int w = 0;
            int h = 0;
            for (TComponent comp : parent.getComponents()) {
                TDimension d = getComponentSize(comp);
                if (w < d.width) {
                    w = d.width;
                }
                if (h < d.height) {
                    h = d.height;
                }
            }

            return new TDimension(
                    insets.left + insets.right + ncols * w + (ncols - 1) * hgap,
                    insets.top + insets.bottom + nrows * h + (nrows - 1) * vgap);
        }
    }

    @Override
    public void layoutContainer(TContainer parent) {
        synchronized (parent.getTreeLock()) {
            TInsets insets = parent.getInsets();
            int ncomponents = parent.getComponents().length;
            int nrows = rows;
            int ncols = cols;

            if (ncomponents == 0) {
                return;
            }

            if (nrows > 0) {
                ncols = (ncomponents + nrows - 1) / nrows;
            } else {
                nrows = (ncomponents + ncols - 1) / ncols;
            }

            int w = parent.getWidth() - (insets.left + insets.right);
            int h = parent.getHeight() - (insets.top + insets.bottom);
            w = (w - (ncols - 1) * hgap) / ncols;
            h = (h - (nrows - 1) * vgap) / nrows;

            TComponent[] components = parent.getComponents();
            for (int c = 0, x = insets.left; c < ncols; c++, x += w + hgap) {
                for (int r = 0, y = insets.top; r < nrows; r++, y += h + vgap) {
                    int i = r * ncols + c;
                    if (i < ncomponents) {
                        components[i].setBounds(x, y, w, h);
                    }
                }
            }
        }
    }

    /**
     * Gets the size of a component for layout calculations.
     * Priority: minimum size -> current size -> preferred size (fallback)
     */
    private TDimension getComponentSize(TComponent comp) {
        // First try minimum size if it's meaningful
        TDimension min = comp.getMinimumSize();
        if (min != null && (min.width > 0 || min.height > 0)) {
            return min;
        }
        
        // Then use current dimensions if set
        int w = comp.getWidth();
        int h = comp.getHeight();
        if (w > 0 || h > 0) {
            return new TDimension(w, h);
        }
        
        // For containers, try getPreferredLayoutSize() as fallback
        if (comp instanceof TContainer) {
            TDimension layoutPref = ((TContainer) comp).getPreferredLayoutSize();
            if (layoutPref != null) {
                return layoutPref;
            }
        }
        
        // Last resort: try preferred size
        TDimension pref = comp.getPreferredSize();
        if (pref != null && (pref.width > 0 || pref.height > 0)) {
            return pref;
        }
        
        // Absolute fallback
        return new TDimension(0, 0);
    }

    /**
     * Returns the string representation of this grid layout's values.
     *
     * @return a string representation of this grid layout
     */
    @Override
    public String toString() {
        return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap +
                ",rows=" + rows + ",cols=" + cols + "]";
    }
}
