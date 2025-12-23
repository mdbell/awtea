package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.event.TFocusEvent;
import me.mdbell.awtea.classlib.java.awt.event.TFocusListener;
import me.mdbell.awtea.test.Test;

import java.util.ArrayList;
import java.util.List;

import static me.mdbell.awtea.test.Assert.*;

/**
 * Tests for the Focus Traversal System.
 */
public class FocusTraversalTest {

    private TKeyboardFocusManager focusManager;
    private TFrame frame;
    private TPanel panel;
    private TButton button1;
    private TButton button2;
    private TButton button3;
    private List<String> focusLog;

    private void setUp() {
        focusManager = TKeyboardFocusManager.getCurrentKeyboardFocusManager();
        frame = new TestFrame();
        panel = new TPanel();
        button1 = new TButton("Button 1");
        button2 = new TButton("Button 2");
        button3 = new TButton("Button 3");
        focusLog = new ArrayList<>();

        // Setup component hierarchy
        frame.add(panel);
        panel.add(button1);
        panel.add(button2);
        panel.add(button3);

        // Add focus listeners to track focus changes
        addFocusTracking(button1, "Button1");
        addFocusTracking(button2, "Button2");
        addFocusTracking(button3, "Button3");
    }

    private void addFocusTracking(TComponent component, String name) {
        component.addFocusListener(new TFocusListener() {
            @Override
            public void focusGained(TFocusEvent e) {
                focusLog.add(name + ":gained");
            }

            @Override
            public void focusLost(TFocusEvent e) {
                focusLog.add(name + ":lost");
            }
        });
    }

    @Test
    public void testFocusTraversalPolicyNotNull() {
        setUp();
        assertNotNull(frame.getFocusTraversalPolicy(), 
                "Frame should have a default focus traversal policy");
    }

    @Test
    public void testWindowIsFocusCycleRoot() {
        setUp();
        assertTrue(frame.isFocusCycleRoot(), 
                "TFrame should be a focus cycle root by default");
    }

    @Test
    public void testContainerOrderTraversal() {
        setUp();
        TFocusTraversalPolicy policy = frame.getFocusTraversalPolicy();
        
        TComponent first = policy.getFirstComponent(frame);
        assertNotNull(first, "First component should not be null");
        
        TComponent second = policy.getComponentAfter(frame, first);
        assertNotNull(second, "Second component should not be null");
        assertTrue(first != second, "Second component should be different from first");
        
        TComponent third = policy.getComponentAfter(frame, second);
        assertNotNull(third, "Third component should not be null");
        
        // After the last component, should wrap back to first
        TComponent wrappedFirst = policy.getComponentAfter(frame, third);
        assertTrue(first == wrappedFirst, "Should wrap back to first component");
    }

    @Test
    public void testBackwardTraversal() {
        setUp();
        TFocusTraversalPolicy policy = frame.getFocusTraversalPolicy();
        
        TComponent first = policy.getFirstComponent(frame);
        TComponent last = policy.getLastComponent(frame);
        
        assertNotNull(first, "First component should not be null");
        assertNotNull(last, "Last component should not be null");
        assertTrue(first != last, "First and last should be different");
        
        TComponent beforeFirst = policy.getComponentBefore(frame, first);
        assertTrue(last == beforeFirst, 
                "Component before first should wrap to last");
    }

    @Test
    public void testFocusNextComponent() {
        setUp();
        // Give button1 focus
        button1.requestFocus();
        assertTrue(focusManager.getFocusOwner() == button1, 
                "Button1 should have focus");
        
        // Move focus forward
        focusManager.focusNextComponent();
        assertTrue(focusManager.getFocusOwner() == button2, 
                "Button2 should have focus after forward traversal");
        
        // Move focus forward again
        focusManager.focusNextComponent();
        assertTrue(focusManager.getFocusOwner() == button3, 
                "Button3 should have focus after second forward traversal");
        
        // Move focus forward from last - should wrap to first
        focusManager.focusNextComponent();
        assertTrue(focusManager.getFocusOwner() == button1, 
                "Should wrap back to Button1 after traversing past last");
    }

    @Test
    public void testFocusPreviousComponent() {
        setUp();
        // Give button1 focus
        button1.requestFocus();
        assertTrue(focusManager.getFocusOwner() == button1, 
                "Button1 should have focus");
        
        // Move focus backward - should wrap to last
        focusManager.focusPreviousComponent();
        assertTrue(focusManager.getFocusOwner() == button3, 
                "Should wrap to Button3 on backward traversal from first");
        
        // Move focus backward again
        focusManager.focusPreviousComponent();
        assertTrue(focusManager.getFocusOwner() == button2, 
                "Button2 should have focus after backward traversal");
    }

    @Test
    public void testTransferFocus() {
        setUp();
        button1.requestFocus();
        assertTrue(focusManager.getFocusOwner() == button1, "Button1 should have focus");
        
        button1.transferFocus();
        assertTrue(focusManager.getFocusOwner() == button2, 
                "transferFocus should move to next component");
    }

    @Test
    public void testTransferFocusBackward() {
        setUp();
        button2.requestFocus();
        assertTrue(focusManager.getFocusOwner() == button2, "Button2 should have focus");
        
        button2.transferFocusBackward();
        assertTrue(focusManager.getFocusOwner() == button1, 
                "transferFocusBackward should move to previous component");
    }

    @Test
    public void testNonFocusableComponentsSkipped() {
        setUp();
        // Make button2 non-focusable
        button2.setFocusable(false);
        
        button1.requestFocus();
        assertTrue(focusManager.getFocusOwner() == button1, "Button1 should have focus");
        
        // Move forward - should skip button2 and go to button3
        focusManager.focusNextComponent();
        assertTrue(focusManager.getFocusOwner() == button3, 
                "Should skip non-focusable button2");
    }

    @Test
    public void testInvisibleComponentsSkipped() {
        setUp();
        // Make button2 invisible
        button2.setVisible(false);
        
        button1.requestFocus();
        assertTrue(focusManager.getFocusOwner() == button1, "Button1 should have focus");
        
        // Move forward - should skip invisible button2 and go to button3
        focusManager.focusNextComponent();
        assertTrue(focusManager.getFocusOwner() == button3, 
                "Should skip invisible button2");
    }

    @Test
    public void testCustomFocusTraversalPolicy() {
        setUp();
        // Create a custom policy that reverses the order
        TFocusTraversalPolicy customPolicy = new TContainerOrderFocusTraversalPolicy() {
            @Override
            public TComponent getComponentAfter(TContainer aContainer, TComponent aComponent) {
                // Reverse: go backwards
                return super.getComponentBefore(aContainer, aComponent);
            }
            
            @Override
            public TComponent getComponentBefore(TContainer aContainer, TComponent aComponent) {
                // Reverse: go forwards
                return super.getComponentAfter(aContainer, aComponent);
            }
        };
        
        frame.setFocusTraversalPolicy(customPolicy);
        assertTrue(frame.isFocusTraversalPolicySet(), 
                "Focus traversal policy should be marked as set");
        
        button1.requestFocus();
        assertTrue(focusManager.getFocusOwner() == button1, "Button1 should have focus");
        
        // With reversed policy, "next" should go to what would normally be "previous"
        focusManager.focusNextComponent();
        assertTrue(focusManager.getFocusOwner() == button3, 
                "Custom policy should reverse traversal order");
    }

    @Test
    public void testFocusCycleRoot() {
        setUp();
        // Create a nested panel that's a focus cycle root
        TPanel nestedPanel = new TPanel();
        nestedPanel.setFocusCycleRoot(true);
        TButton nestedButton = new TButton("Nested");
        nestedPanel.add(nestedButton);
        
        // Add to frame
        panel.add(nestedPanel);
        
        // Verify the nested panel is a cycle root
        assertTrue(nestedPanel.isFocusCycleRoot(), 
                "Nested panel should be a focus cycle root");
        
        // Verify it can get its own policy
        assertNotNull(nestedPanel.getFocusTraversalPolicy(), 
                "Focus cycle root should have a policy");
    }

    @Test
    public void testGetFocusCycleRootAncestor() {
        setUp();
        assertTrue(button1.getFocusCycleRootAncestor() == frame, 
                "Button's focus cycle root ancestor should be the frame");
        
        assertTrue(panel.getFocusCycleRootAncestor() == frame, 
                "Panel's focus cycle root ancestor should be the frame");
        
        // Frame is a focus cycle root, so its ancestor should be null
        assertNull(frame.getFocusCycleRootAncestor(), 
                "Frame's focus cycle root ancestor should be null");
    }

    @Test
    public void testFocusTraversalKeysEnabled() {
        setUp();
        assertTrue(button1.isFocusTraversalKeysEnabled(), 
                "Focus traversal keys should be enabled by default");
        
        button1.setFocusTraversalKeysEnabled(false);
        assertFalse(button1.isFocusTraversalKeysEnabled(), 
                "Focus traversal keys should be disabled after setting");
    }

    @Test
    public void testDefaultFocusTraversalKeys() {
        setUp();
        // Test that default traversal keys are set
        assertNotNull(focusManager.getDefaultFocusTraversalKeys(
                TKeyboardFocusManager.FORWARD_TRAVERSAL_KEYS), 
                "Forward traversal keys should be set");
        assertNotNull(focusManager.getDefaultFocusTraversalKeys(
                TKeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS), 
                "Backward traversal keys should be set");
        assertNotNull(focusManager.getDefaultFocusTraversalKeys(
                TKeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS), 
                "Up cycle traversal keys should be set");
        assertNotNull(focusManager.getDefaultFocusTraversalKeys(
                TKeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS), 
                "Down cycle traversal keys should be set");
    }

    @Test
    public void testGetFirstAndLastComponent() {
        setUp();
        TFocusTraversalPolicy policy = frame.getFocusTraversalPolicy();
        
        TComponent first = policy.getFirstComponent(frame);
        TComponent last = policy.getLastComponent(frame);
        
        assertNotNull(first, "First component should not be null");
        assertNotNull(last, "Last component should not be null");
        assertTrue(first != last, "First and last should be different");
        
        // Verify they are from our button set
        assertTrue(first == button1 || first == button2 || first == button3, 
                "First component should be one of the buttons");
        assertTrue(last == button1 || last == button2 || last == button3, 
                "Last component should be one of the buttons");
    }

    @Test
    public void testDefaultComponent() {
        setUp();
        TFocusTraversalPolicy policy = frame.getFocusTraversalPolicy();
        
        TComponent defaultComp = policy.getDefaultComponent(frame);
        TComponent first = policy.getFirstComponent(frame);
        
        assertTrue(first == defaultComp, 
                "Default component should be the same as first component");
    }

    @Test
    public void testGlobalFocusOwner() {
        setUp();
        // Clear any existing focus first
        focusManager.setGlobalFocusOwner(null);
        assertNull(focusManager.getFocusOwner(), 
                "Focus owner should be null initially");
        
        button1.requestFocus();
        assertTrue(focusManager.getFocusOwner() == button1, 
                "Focus owner should be button1 after request");
        
        button2.requestFocus();
        assertTrue(focusManager.getFocusOwner() == button2, 
                "Focus owner should change to button2");
    }

    @Test
    public void testPermanentFocusOwner() {
        setUp();
        button1.requestFocus();
        assertTrue(focusManager.getPermanentFocusOwner() == button1, 
                "Permanent focus owner should be button1");
        
        // When focus changes, permanent focus owner should also change
        button2.requestFocus();
        assertTrue(focusManager.getPermanentFocusOwner() == button2, 
                "Permanent focus owner should change to button2");
    }

    @Test
    public void testSetDefaultFocusTraversalPolicy() {
        setUp();
        TFocusTraversalPolicy oldPolicy = focusManager.getDefaultFocusTraversalPolicy();
        assertNotNull(oldPolicy, "Default policy should not be null");
        
        TContainerOrderFocusTraversalPolicy newPolicy = new TContainerOrderFocusTraversalPolicy();
        focusManager.setDefaultFocusTraversalPolicy(newPolicy);
        
        assertTrue(focusManager.getDefaultFocusTraversalPolicy() == newPolicy, 
                "Default policy should be updated");
        
        // Restore old policy
        focusManager.setDefaultFocusTraversalPolicy(oldPolicy);
    }

    /**
     * Helper class to create a testable TFrame that doesn't need peer/surface.
     */
    private static class TestFrame extends TFrame {
        @Override
        public TGraphics getGraphics() {
            // Return null to avoid peer/surface requirements in tests
            return null;
        }
    }
}
