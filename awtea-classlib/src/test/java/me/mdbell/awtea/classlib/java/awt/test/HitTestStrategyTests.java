package me.mdbell.awtea.classlib.java.awt.test;

import me.mdbell.awtea.classlib.java.awt.*;
import me.mdbell.awtea.classlib.java.awt.awtea.HitTestStrategy;
import me.mdbell.awtea.classlib.java.awt.awtea.TreeWalkHitTestStrategy;
import me.mdbell.awtea.test.Test;

import static me.mdbell.awtea.test.Assert.*;

/**
 * Tests for component hit-testing strategies.
 */
public class HitTestStrategyTests {

    /**
     * Test that TComponent assigns unique IDs to each component.
     */
    @Test
    public void testComponentIdUniqueness() {
        TPanel panel1 = new TPanel();
        TPanel panel2 = new TPanel();
        TPanel panel3 = new TPanel();
        
        int id1 = panel1.getComponentId();
        int id2 = panel2.getComponentId();
        int id3 = panel3.getComponentId();
        
        assertTrue(id1 != id2, "Component IDs should be unique");
        assertTrue(id2 != id3, "Component IDs should be unique");
        assertTrue(id1 != id3, "Component IDs should be unique");
    }
    
    /**
     * Test that components can be looked up by ID.
     */
    @Test
    public void testComponentRegistry() {
        TPanel panel = new TPanel();
        int id = panel.getComponentId();
        
        TComponent found = TComponent.getComponentById(id);
        
        assertNotNull(found, "Component should be found in registry");
        assertEquals(panel, found, "Found component should be the same instance");
    }
    
    /**
     * Test tree-walk strategy with simple hierarchy.
     */
    @Test
    public void testTreeWalkStrategySimple() {
        // Create a simple hierarchy
        TPanel container = new TPanel();
        container.setBounds(0, 0, 200, 200);
        
        TPanel child1 = new TPanel();
        child1.setBounds(10, 10, 80, 80);
        container.add(child1);
        
        TPanel child2 = new TPanel();
        child2.setBounds(100, 10, 80, 80);
        container.add(child2);
        
        // Create strategy
        HitTestStrategy strategy = new TreeWalkHitTestStrategy(container);
        
        // Test hit on child1
        TComponent hit1 = strategy.getComponentAt(50, 50);
        assertEquals(child1, hit1, "Should hit child1");
        
        // Test hit on child2
        TComponent hit2 = strategy.getComponentAt(150, 50);
        assertEquals(child2, hit2, "Should hit child2");
        
        // Test hit on container background
        TComponent hit3 = strategy.getComponentAt(5, 5);
        assertEquals(container, hit3, "Should hit container");
        
        strategy.dispose();
    }
    
    /**
     * Test tree-walk strategy with nested hierarchy.
     */
    @Test
    public void testTreeWalkStrategyNested() {
        // Create nested hierarchy
        TPanel root = new TPanel();
        root.setBounds(0, 0, 300, 300);
        
        TPanel level1 = new TPanel();
        level1.setBounds(50, 50, 200, 200);
        root.add(level1);
        
        TPanel level2 = new TPanel();
        level2.setBounds(50, 50, 100, 100);
        level1.add(level2);
        
        // Create strategy
        HitTestStrategy strategy = new TreeWalkHitTestStrategy(root);
        
        // Test hit on deepest level
        // level2 is at (50, 50) in level1, which is at (50, 50) in root
        // So level2 absolute position is (100, 100)
        TComponent hit = strategy.getComponentAt(100, 100);
        assertEquals(level2, hit, "Should hit deepest component");
        
        strategy.dispose();
    }
    
    /**
     * Test that invisible components are not hit.
     */
    @Test
    public void testTreeWalkStrategyInvisible() {
        TPanel container = new TPanel();
        container.setBounds(0, 0, 200, 200);
        
        TPanel child = new TPanel();
        child.setBounds(10, 10, 80, 80);
        child.setVisible(false);
        container.add(child);
        
        HitTestStrategy strategy = new TreeWalkHitTestStrategy(container);
        
        // Hit should go to container, not invisible child
        TComponent hit = strategy.getComponentAt(50, 50);
        assertEquals(container, hit, "Should skip invisible component");
        
        strategy.dispose();
    }
    
    /**
     * Test strategy invalidation (should not throw).
     */
    @Test
    public void testStrategyInvalidation() {
        TPanel container = new TPanel();
        HitTestStrategy strategy = new TreeWalkHitTestStrategy(container);
        
        // Invalidation should not throw for tree-walk (it's a no-op)
        strategy.invalidate();
        
        // Should still work after invalidation
        TComponent hit = strategy.getComponentAt(0, 0);
        assertEquals(container, hit, "Strategy should work after invalidate");
        
        strategy.dispose();
    }
}
