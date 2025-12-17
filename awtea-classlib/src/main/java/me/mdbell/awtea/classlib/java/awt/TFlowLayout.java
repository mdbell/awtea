package me.mdbell.awtea.classlib.java.awt;

import java.util.ArrayList;
import java.util.List;

import org.teavm.classlib.java.awt.TDimension;

/**
 * A flow layout arranges components in a directional flow, much
 * like lines of text in a paragraph. The flow direction is determined
 * by the container's {@code componentOrientation} property and may be
 * one of two values:
 * <ul>
 * <li>{@code ComponentOrientation.LEFT_TO_RIGHT}
 * <li>{@code ComponentOrientation.RIGHT_TO_LEFT}
 * </ul>
 * Flow layouts are typically used to arrange buttons in a panel. It
 * arranges buttons horizontally until no more buttons fit on the same line.
 * Each line is centered by default.
 * This is the awtea implementation of java.awt.FlowLayout.
 *
 * @see java.awt.FlowLayout
 */
public class TFlowLayout implements TLayoutManager {

    /**
     * This value indicates that each row of components
     * should be left-justified.
     */
    public static final int LEFT = 0;

    /**
     * This value indicates that each row of components
     * should be centered.
     */
    public static final int CENTER = 1;

    /**
     * This value indicates that each row of components
     * should be right-justified.
     */
    public static final int RIGHT = 2;

    /**
     * This value indicates that each row of components
     * should be justified to the leading edge of the container's
     * orientation, for example, to the left in left-to-right orientations.
     */
    public static final int LEADING = 3;

    /**
     * This value indicates that each row of components
     * should be justified to the trailing edge of the container's
     * orientation, for example, to the right in left-to-right orientations.
     */
    public static final int TRAILING = 4;

    private int align;
    private int hgap;
    private int vgap;
    private boolean alignOnBaseline;

    /**
     * Constructs a new {@code TFlowLayout} with a centered alignment and a
     * default 5-unit horizontal and vertical gap.
     */
    public TFlowLayout() {
        this(CENTER, 5, 5);
    }

    /**
     * Constructs a new {@code TFlowLayout} with the specified
     * alignment and a default 5-unit horizontal and vertical gap.
     * The value of the alignment argument must be one of
     * {@code TFlowLayout.LEFT}, {@code TFlowLayout.RIGHT},
     * {@code TFlowLayout.CENTER}, {@code TFlowLayout.LEADING},
     * or {@code TFlowLayout.TRAILING}.
     *
     * @param align the alignment value
     */
    public TFlowLayout(int align) {
        this(align, 5, 5);
    }

    /**
     * Creates a new flow layout manager with the indicated alignment
     * and the indicated horizontal and vertical gaps.
     *
     * @param align the alignment value
     * @param hgap  the horizontal gap between components and between
     *              the components and the borders of the {@code Container}
     * @param vgap  the vertical gap between components and between
     *              the components and the borders of the {@code Container}
     */
    public TFlowLayout(int align, int hgap, int vgap) {
        this.align = align;
        this.hgap = hgap;
        this.vgap = vgap;
    }

    /**
     * Gets the alignment for this layout.
     *
     * @return the alignment value for this layout
     */
    public int getAlignment() {
        return align;
    }

    /**
     * Sets the alignment for this layout.
     *
     * @param align one of {@code LEFT}, {@code CENTER}, {@code RIGHT},
     *              {@code LEADING}, or {@code TRAILING}
     */
    public void setAlignment(int align) {
        this.align = align;
    }

    /**
     * Gets the horizontal gap between components
     * and between the components and the borders
     * of the {@code Container}
     *
     * @return the horizontal gap between components
     *         and between the components and the borders
     *         of the {@code Container}
     */
    public int getHgap() {
        return hgap;
    }

    /**
     * Sets the horizontal gap between components and
     * between the components and the borders of the
     * {@code Container}.
     *
     * @param hgap the horizontal gap between components
     *             and between the components and the borders
     *             of the {@code Container}
     */
    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    /**
     * Gets the vertical gap between components and
     * between the components and the borders of the
     * {@code Container}.
     *
     * @return the vertical gap between components
     *         and between the components and the borders
     *         of the {@code Container}
     */
    public int getVgap() {
        return vgap;
    }

    /**
     * Sets the vertical gap between components and between
     * the components and the borders of the {@code Container}.
     *
     * @param vgap the vertical gap between components
     *             and between the components and the borders
     *             of the {@code Container}
     */
    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    /**
     * Sets whether or not components should be vertically aligned along their
     * baseline. Components that do not have a baseline will be centered.
     *
     * @param alignOnBaseline whether or not components should be
     *                        vertically aligned on their baseline
     */
    public void setAlignOnBaseline(boolean alignOnBaseline) {
        this.alignOnBaseline = alignOnBaseline;
    }

    /**
     * Returns true if components are to be vertically aligned along
     * their baseline.
     *
     * @return true if components are to be vertically aligned along
     *         their baseline
     */
    public boolean getAlignOnBaseline() {
        return alignOnBaseline;
    }

    @Override
    public void addLayoutComponent(String name, TComponent comp) {
        // FlowLayout doesn't use component names
    }

    @Override
    public void removeLayoutComponent(TComponent comp) {
        // FlowLayout doesn't maintain component-specific state
    }

    @Override
    public TDimension preferredLayoutSize(TContainer target) {
        synchronized (target.getTreeLock()) {
            TDimension dim = new TDimension(0, 0);
            TComponent[] components = target.getComponents();

            boolean firstVisibleComponent = true;
            for (TComponent comp : components) {
                TDimension d = getComponentSize(comp);
                dim.height = Math.max(dim.height, d.height);
                if (firstVisibleComponent) {
                    firstVisibleComponent = false;
                } else {
                    dim.width += hgap;
                }
                dim.width += d.width;
            }

            TInsets insets = target.getInsets();
            dim.width += insets.left + insets.right + hgap * 2;
            dim.height += insets.top + insets.bottom + vgap * 2;
            return dim;
        }
    }

    @Override
    public TDimension minimumLayoutSize(TContainer target) {
        synchronized (target.getTreeLock()) {
            TDimension dim = new TDimension(0, 0);
            TComponent[] components = target.getComponents();

            boolean firstVisibleComponent = true;
            for (TComponent comp : components) {
                TDimension d = getComponentSize(comp);
                dim.height = Math.max(dim.height, d.height);
                if (firstVisibleComponent) {
                    firstVisibleComponent = false;
                } else {
                    dim.width += hgap;
                }
                dim.width += d.width;
            }

            TInsets insets = target.getInsets();
            dim.width += insets.left + insets.right + hgap * 2;
            dim.height += insets.top + insets.bottom + vgap * 2;
            return dim;
        }
    }

    @Override
    public void layoutContainer(TContainer target) {
        synchronized (target.getTreeLock()) {
            TInsets insets = target.getInsets();
            int maxwidth = target.getWidth() - (insets.left + insets.right + hgap * 2);
            int nmembers = target.getComponents().length;
            int x = 0, y = insets.top + vgap;
            int rowh = 0, start = 0;

            TComponent[] components = target.getComponents();

            boolean ltr = true; // TODO: add component orientation support

            for (int i = 0; i < nmembers; i++) {
                TComponent m = components[i];
                TDimension d = getComponentSize(m);

                // Start a new row if this component won't fit
                if (x > 0 && x + d.width > maxwidth) {
                    moveComponents(target, insets.left + hgap, y, maxwidth - x, rowh,
                            start, i, ltr);
                    x = 0;
                    y += vgap + rowh;
                    rowh = 0;
                    start = i;
                }

                x += d.width + hgap;
                rowh = Math.max(rowh, d.height);
            }

            // Layout the last row
            moveComponents(target, insets.left + hgap, y, maxwidth - x, rowh,
                    start, nmembers, ltr);
        }
    }

    /**
     * Centers the elements in the specified row, if there is any slack.
     *
     * @param target   the container
     * @param x        the x coordinate
     * @param y        the y coordinate
     * @param width    the width dimensions
     * @param height   the height dimensions
     * @param rowStart the start of the row
     * @param rowEnd   the end of the row
     * @param ltr      left-to-right flag
     */
    private void moveComponents(TContainer target, int x, int y, int width, int height,
            int rowStart, int rowEnd, boolean ltr) {
        switch (align) {
            case LEFT:
                x = ltr ? x : x + width;
                break;
            case CENTER:
                x = x + width / 2;
                break;
            case RIGHT:
                x = ltr ? x + width : x;
                break;
            case LEADING:
                break;
            case TRAILING:
                x = x + width;
                break;
        }

        TComponent[] components = target.getComponents();
        for (int i = rowStart; i < rowEnd; i++) {
            TComponent m = components[i];
            TDimension d = getComponentSize(m);

            // Set the component's size to match its preferred size
            m.setSize(d.width, d.height);

            if (ltr) {
                m.setLocation(x, y + (height - d.height) / 2);
            } else {
                m.setLocation(target.getWidth() - x - d.width, y + (height - d.height) / 2);
            }
            x += d.width + hgap;
        }
    }

    /**
     * Gets the size of a component, preferring preferredSize if set,
     * otherwise using current width/height.
     */
    private TDimension getComponentSize(TComponent comp) {
        if (comp instanceof TContainer) {
            TDimension pref = ((TContainer) comp).getPreferredLayoutSize();
            if (pref != null) {
                return pref;
            }
        }
        return new TDimension(comp.getWidth(), comp.getHeight());
    }

    /**
     * Returns a string representation of this {@code TFlowLayout}
     * object and its values.
     *
     * @return a string representation of this layout
     */
    @Override
    public String toString() {
        String str = "";
        switch (align) {
            case LEFT:
                str = ",align=left";
                break;
            case CENTER:
                str = ",align=center";
                break;
            case RIGHT:
                str = ",align=right";
                break;
            case LEADING:
                str = ",align=leading";
                break;
            case TRAILING:
                str = ",align=trailing";
                break;
        }
        return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap + str + "]";
    }

    /**
     * Gets the tree lock for synchronization.
     * For now, we use the container itself as the lock.
     */
    private Object getTreeLock(TContainer target) {
        return target;
    }
}
