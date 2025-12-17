package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.Setter;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teavm.classlib.java.awt.TDimension;

/**
 * @see java.awt.Container
 */
public class TContainer extends TComponent {

    private static final Logger log = LoggerFactory.getLogger(TContainer.class);

    private List<TComponent> children = new ArrayList<>();
    private Map<TComponent, Object> constraints = new HashMap<>();

    @Getter
    @Setter
    private boolean focusCycleRoot = false;

    @Getter
    private TLayoutManager layoutMgr;

    /**
     * Gets the tree lock object for synchronization.
     *
     * @return the tree lock
     */
    public final Object getTreeLock() {
        return this;
    }

    /**
     * Determines the insets of this container, which indicate the size
     * of the container's border.
     *
     * @return the insets of this container
     */
    public TInsets getInsets() {
        return new TInsets(0, 0, 0, 0);
    }

    public TComponent add(TComponent component) {
        addImpl(component, null, -1);
        return component;
    }

    public void add(TComponent comp, Object constraints) {
        addImpl(comp, constraints, -1);
    }

    public TComponent add(String name, TComponent component) {
        addImpl(component, name, -1);
        return component;
    }

    public TComponent add(TComponent component, int index) {
        addImpl(component, null, index);
        return component;
    }

    public void add(TComponent comp, Object constraints, int index) {
        addImpl(comp, constraints, index);
    }

    protected void addImpl(TComponent comp, Object constraints, int index) {
        if (comp.getParent() != null) {
            comp.getParent().remove(comp);
        }

        if (index == -1) {
            this.children.add(comp);
        } else {
            this.children.add(index, comp);
        }

        comp.setParent(this);

        if (constraints != null) {
            this.constraints.put(comp, constraints);
        }

        if (layoutMgr != null) {
            if (layoutMgr instanceof TLayoutManager2) {
                ((TLayoutManager2) layoutMgr).addLayoutComponent(comp, constraints);
            } else if (constraints instanceof String) {
                layoutMgr.addLayoutComponent((String) constraints, comp);
            }
        }

        invalidate();
    }

    public void remove(TComponent comp) {
        if (comp.getParent() != this) {
            // not us, do nothing
            return;
        }
        comp.setParent(null);
        this.children.remove(comp);
        this.constraints.remove(comp);

        if (layoutMgr != null) {
            layoutMgr.removeLayoutComponent(comp);
        }

        invalidate();
    }

    public void removeAll() {
        for (TComponent child : new ArrayList<>(children)) {
            remove(child);
        }
    }

    public boolean isValidateRoot() {
        return false;
    }

    /**
     * Sets the layout manager for this container.
     *
     * @param mgr the specified layout manager
     */
    public void setLayout(TLayoutManager mgr) {
        this.layoutMgr = mgr;
        invalidate();
    }

    /**
     * Causes this container to lay out its components.
     * Most programs should not call this method directly, but should invoke
     * the {@code validate} method instead.
     */
    public void doLayout() {
        if (layoutMgr != null) {
            layoutMgr.layoutContainer(this);
        }
    }

    /**
     * Returns the preferred size of this container.
     * If layout manager is present, delegates to it.
     *
     * @return the preferred size of this container
     */
    public TDimension getPreferredLayoutSize() {
        // Check if an explicit preferred size was set
        TDimension pref = getPreferredSize();
        if (pref != null) {
            return pref;
        }

        // Otherwise ask the layout manager
        if (layoutMgr != null) {
            return layoutMgr.preferredLayoutSize(this);
        }

        // Fall back to current size
        return new TDimension(getWidth(), getHeight());
    }

    /**
     * Returns the minimum size of this container.
     *
     * @return the minimum size of this container
     */
    public TDimension getMinimumLayoutSize() {
        if (layoutMgr != null) {
            return layoutMgr.minimumLayoutSize(this);
        }
        return new TDimension(0, 0);
    }

    /**
     * Returns the maximum size of this container.
     *
     * @return the maximum size of this container
     */
    public TDimension getMaximumLayoutSize() {
        if (layoutMgr instanceof TLayoutManager2) {
            return ((TLayoutManager2) layoutMgr).maximumLayoutSize(this);
        }
        return new TDimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Gets the layout constraint for the specified component.
     *
     * @param comp the component
     * @return the constraint for the component, or null if none
     */
    public Object getConstraints(TComponent comp) {
        return constraints.get(comp);
    }

    @Override
    public void paint(TGraphics g) {
        for (TComponent component : children) {
            // Skip invisible components
            if (!component.isVisible()) {
                continue;
            }

            int x = component.getX();
            int y = component.getY();
            int width = component.getWidth();
            int height = component.getHeight();

            // Skip components with zero or negative dimensions
            if (width <= 0 || height <= 0) {
                continue;
            }

            // Create a new graphics context for the child component
            TGraphics childGraphics = g.create();
            // g.create() can return null if graphics context creation fails
            if (childGraphics != null) {
                // Translate to the child's position first
                childGraphics.translate(x, y);
                // Then clip to the child's bounds in the child's coordinate system
                // This ensures the clip is at (0, 0) relative to where the child will paint
                childGraphics.setClip(0, 0, width, height);
                // Paint the child
                component.paint(childGraphics);
                childGraphics.dispose();
            }
        }
    }

    public TComponent[] getComponents() {
        return children.toArray(TComponent[]::new);
    }

    public TComponent getComponentAt(int x, int y) {
        log.trace("TContainer.getComponentAt({}, {}) called on {}", x, y, this.getClass().getName());
        x -= this.getX();
        y -= this.getY();
        for (TComponent child : children) {
            if (child.contains(x, y)) {
                log.trace("Point ({}, {}) is within component {}", x, y, child.getClass().getName());
                if (child instanceof TContainer) {
                    TComponent deeper = ((TContainer) child).getComponentAt(x, y);
                    if (deeper != null) {
                        return deeper;
                    }
                }
                return child;
            }
        }
        return this;
    }

    @Override
    public void validate() {
        if (!isValid()) {
            doLayout();
            for (TComponent child : children) {
                if (child instanceof TContainer) {
                    ((TContainer) child).validate();
                } else {
                    child.validate();
                }
            }
        }
        super.validate();
    }
}
