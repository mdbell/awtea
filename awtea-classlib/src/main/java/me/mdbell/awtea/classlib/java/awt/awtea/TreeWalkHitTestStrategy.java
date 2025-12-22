package me.mdbell.awtea.classlib.java.awt.awtea;

import me.mdbell.awtea.classlib.java.awt.TComponent;
import me.mdbell.awtea.classlib.java.awt.TContainer;
import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

/**
 * Traditional tree-walk hit-testing strategy.
 * <p>
 * This is the default implementation that recursively walks the component tree
 * to find the topmost component at the specified coordinates. This approach
 * has O(n) complexity where n is the number of components in the hierarchy.
 * </p>
 * <p>
 * This strategy is always available and serves as the fallback when GPU-based
 * picking is not supported or disabled.
 * </p>
 */
public class TreeWalkHitTestStrategy implements HitTestStrategy {
    
    private static final Logger log = LoggerFactory.getLogger(TreeWalkHitTestStrategy.class);
    
    private final TContainer rootContainer;
    
    /**
     * Creates a new tree-walk hit-test strategy.
     * 
     * @param rootContainer the root container to search from
     */
    public TreeWalkHitTestStrategy(TContainer rootContainer) {
        this.rootContainer = rootContainer;
    }
    
    @Override
    public TComponent getComponentAt(int x, int y) {
        TComponent component = rootContainer.getComponentAt(x, y);
        if (component == null) {
            component = rootContainer;
        }
        return component;
    }
    
    @Override
    public void invalidate() {
        // No-op for tree-walk strategy - no cached data to invalidate
    }
    
    @Override
    public void dispose() {
        // No-op for tree-walk strategy - no resources to release
    }
}
