package me.mdbell.awtea.classlib.java.awt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.teavm.classlib.java.awt.TDimension;

/**
 * A {@code TCardLayout} object is a layout manager for a container.
 * It treats each component in the container as a card.
 * Only one card is visible at a time, and the container acts as a stack of
 * cards.
 * The first component added to a {@code TCardLayout} object is the visible
 * component
 * when the container is first displayed.
 * <p>
 * The ordering of cards is determined by the container's own internal ordering
 * of
 * its component objects. {@code TCardLayout} defines a set of methods
 * that allow an application to flip through these cards sequentially,
 * or to show a specified card. The {@link #addLayoutComponent}
 * method can be used to associate a string identifier with a given card
 * for fast random access.
 * This is the awtea implementation of java.awt.CardLayout.
 *
 * @see java.awt.CardLayout
 */
public class TCardLayout implements TLayoutManager2 {

    private Map<String, TComponent> tab = new HashMap<>();
    private List<TComponent> vector = new ArrayList<>();
    private TComponent currentCard = null;
    private int currentCardIndex = -1;

    private int hgap;
    private int vgap;

    /**
     * Creates a new card layout with gaps of size zero.
     */
    public TCardLayout() {
        this(0, 0);
    }

    /**
     * Creates a new card layout with the specified horizontal and
     * vertical gaps. The horizontal gaps are placed at the left and
     * right edges. The vertical gaps are placed at the top and bottom edges.
     *
     * @param hgap the horizontal gap
     * @param vgap the vertical gap
     */
    public TCardLayout(int hgap, int vgap) {
        this.hgap = hgap;
        this.vgap = vgap;
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
     * Sets the horizontal gap between components.
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
                constraints = "";
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
            if (!vector.contains(comp)) {
                vector.add(comp);
                if (comp.isVisible()) {
                    comp.setVisible(false);
                }
            }
            if (name != null && !name.isEmpty()) {
                tab.put(name, comp);
            }
        }
    }

    @Override
    public void removeLayoutComponent(TComponent comp) {
        synchronized (comp.getParent().getTreeLock()) {
            vector.remove(comp);

            // Remove from name map
            String key = null;
            for (Map.Entry<String, TComponent> entry : tab.entrySet()) {
                if (entry.getValue() == comp) {
                    key = entry.getKey();
                    break;
                }
            }
            if (key != null) {
                tab.remove(key);
            }

            if (currentCard == comp) {
                currentCard = null;
                currentCardIndex = -1;
            }
        }
    }

    @Override
    public TDimension preferredLayoutSize(TContainer parent) {
        synchronized (parent.getTreeLock()) {
            TInsets insets = parent.getInsets();
            int w = 0;
            int h = 0;

            for (TComponent comp : vector) {
                TDimension d = getComponentSize(comp);
                if (d.width > w) {
                    w = d.width;
                }
                if (d.height > h) {
                    h = d.height;
                }
            }

            return new TDimension(
                    insets.left + insets.right + w + hgap * 2,
                    insets.top + insets.bottom + h + vgap * 2);
        }
    }

    @Override
    public TDimension minimumLayoutSize(TContainer parent) {
        synchronized (parent.getTreeLock()) {
            TInsets insets = parent.getInsets();
            int w = 0;
            int h = 0;

            for (TComponent comp : vector) {
                TDimension d = getComponentSize(comp);
                if (d.width > w) {
                    w = d.width;
                }
                if (d.height > h) {
                    h = d.height;
                }
            }

            return new TDimension(
                    insets.left + insets.right + w + hgap * 2,
                    insets.top + insets.bottom + h + vgap * 2);
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
    public void layoutContainer(TContainer parent) {
        synchronized (parent.getTreeLock()) {
            TInsets insets = parent.getInsets();
            int ncomponents = parent.getComponents().length;

            for (int i = 0; i < ncomponents; i++) {
                TComponent comp = parent.getComponents()[i];
                comp.setBounds(
                        hgap + insets.left,
                        vgap + insets.top,
                        parent.getWidth() - (hgap * 2 + insets.left + insets.right),
                        parent.getHeight() - (vgap * 2 + insets.top + insets.bottom));

                // Manage visibility: only the current card should be visible
                // If no current card is set, all cards should be hidden
                boolean shouldBeVisible = (currentCard != null && comp == currentCard);
                if (comp.isVisible() != shouldBeVisible) {
                    comp.setVisible(shouldBeVisible);
                }
            }
        }
    }

    /**
     * Flips to the first card of the container.
     *
     * @param parent the parent container in which to do the layout
     */
    public void first(TContainer parent) {
        synchronized (parent.getTreeLock()) {
            if (vector.size() > 0) {
                showCard(parent, 0);
            }
        }
    }

    /**
     * Flips to the next card of the specified container. If the currently
     * visible card is the last one, this method flips to the first card in the
     * layout.
     *
     * @param parent the parent container in which to do the layout
     */
    public void next(TContainer parent) {
        synchronized (parent.getTreeLock()) {
            if (vector.size() > 0) {
                int nextIndex = (currentCardIndex + 1) % vector.size();
                showCard(parent, nextIndex);
            }
        }
    }

    /**
     * Flips to the previous card of the specified container. If the currently
     * visible card is the first one, this method flips to the last card in the
     * layout.
     *
     * @param parent the parent container in which to do the layout
     */
    public void previous(TContainer parent) {
        synchronized (parent.getTreeLock()) {
            if (vector.size() > 0) {
                int prevIndex = currentCardIndex - 1;
                if (prevIndex < 0) {
                    prevIndex = vector.size() - 1;
                }
                showCard(parent, prevIndex);
            }
        }
    }

    /**
     * Flips to the last card of the container.
     *
     * @param parent the parent container in which to do the layout
     */
    public void last(TContainer parent) {
        synchronized (parent.getTreeLock()) {
            if (vector.size() > 0) {
                showCard(parent, vector.size() - 1);
            }
        }
    }

    /**
     * Flips to the component that was added to this layout with the
     * specified {@code name}, using {@code addLayoutComponent}.
     * If no such component exists, then nothing happens.
     *
     * @param parent the parent container in which to do the layout
     * @param name   the component name
     */
    public void show(TContainer parent, String name) {
        synchronized (parent.getTreeLock()) {
            TComponent comp = tab.get(name);
            if (comp != null) {
                int index = vector.indexOf(comp);
                if (index >= 0) {
                    showCard(parent, index);
                }
            }
        }
    }

    /**
     * Shows the card at the specified index.
     *
     * @param parent the parent container
     * @param index  the index of the card to show
     */
    private void showCard(TContainer parent, int index) {
        if (index < 0 || index >= vector.size()) {
            return;
        }

        // Hide current card
        if (currentCard != null) {
            currentCard.setVisible(false);
        }

        // Show new card
        currentCard = vector.get(index);
        currentCardIndex = index;
        currentCard.setVisible(true);

        parent.validate();
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
     * Returns a string representation of the state of this card layout.
     *
     * @return a string representation of this card layout
     */
    @Override
    public String toString() {
        return getClass().getName() + "[hgap=" + hgap + ",vgap=" + vgap + "]";
    }
}
