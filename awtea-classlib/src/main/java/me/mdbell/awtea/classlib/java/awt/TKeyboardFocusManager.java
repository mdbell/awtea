package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.awtea.TFocusManager;
import me.mdbell.awtea.classlib.java.awt.event.TKeyEvent;

import java.util.*;

/**
 * The KeyboardFocusManager is responsible for managing the active and focused
 * Windows, and the current focus owner. The focus owner is the Component that
 * receives key events. It also manages the focus traversal policy and focus
 * traversal keys.
 * <p>
 * This class maintains a singleton instance that can be accessed via
 * {@link #getCurrentKeyboardFocusManager()}. Applications can replace the
 * current KeyboardFocusManager with a custom one using
 * {@link #setCurrentKeyboardFocusManager(TKeyboardFocusManager)}.
 * <p>
 * The KeyboardFocusManager is integrated with the legacy {@link TFocusManager}
 * to ensure backwards compatibility.
 *
 * @see java.awt.KeyboardFocusManager
 */
public class TKeyboardFocusManager {

    /**
     * Identifier for the forward focus traversal keys.
     */
    public static final int FORWARD_TRAVERSAL_KEYS = 0;

    /**
     * Identifier for the backward focus traversal keys.
     */
    public static final int BACKWARD_TRAVERSAL_KEYS = 1;

    /**
     * Identifier for the up cycle focus traversal keys.
     */
    public static final int UP_CYCLE_TRAVERSAL_KEYS = 2;

    /**
     * Identifier for the down cycle focus traversal keys.
     */
    public static final int DOWN_CYCLE_TRAVERSAL_KEYS = 3;

    private static TKeyboardFocusManager currentManager = new TKeyboardFocusManager();

    private TComponent focusOwner;
    private TComponent permanentFocusOwner;
    private TWindow focusedWindow;
    private TWindow activeWindow;

    private final Map<Integer, Set<Integer>> defaultFocusTraversalKeys;
    private TFocusTraversalPolicy defaultFocusTraversalPolicy;

    /**
     * Creates a new KeyboardFocusManager with default settings.
     */
    protected TKeyboardFocusManager() {
        this.defaultFocusTraversalKeys = new HashMap<>();
        initializeDefaultFocusTraversalKeys();
        this.defaultFocusTraversalPolicy = new TDefaultFocusTraversalPolicy();
    }

    /**
     * Returns the current KeyboardFocusManager instance for the calling
     * thread's context.
     *
     * @return this thread's KeyboardFocusManager
     */
    public static TKeyboardFocusManager getCurrentKeyboardFocusManager() {
        return currentManager;
    }

    /**
     * Sets the current KeyboardFocusManager instance for the calling thread's
     * context. If null is specified, then the current KeyboardFocusManager is
     * replaced with a new instance of the default KeyboardFocusManager.
     *
     * @param newManager the new KeyboardFocusManager
     */
    public static void setCurrentKeyboardFocusManager(TKeyboardFocusManager newManager) {
        currentManager = (newManager != null) ? newManager : new TKeyboardFocusManager();
    }

    /**
     * Returns the focus owner, if the focus owner is in the same context as
     * the calling thread.
     *
     * @return the focus owner, or null if no Component has focus
     */
    public TComponent getFocusOwner() {
        return focusOwner;
    }

    /**
     * Returns the permanent focus owner, if the permanent focus owner is in
     * the same context as the calling thread.
     *
     * @return the permanent focus owner, or null if no Component has permanent focus
     */
    public TComponent getPermanentFocusOwner() {
        return permanentFocusOwner;
    }

    /**
     * Returns the focused Window, if the focused Window is in the same context
     * as the calling thread.
     *
     * @return the focused Window, or null if no Window has focus
     */
    public TWindow getFocusedWindow() {
        return focusedWindow;
    }

    /**
     * Returns the active Window, if the active Window is in the same context
     * as the calling thread.
     *
     * @return the active Window, or null if no Window is active
     */
    public TWindow getActiveWindow() {
        return activeWindow;
    }

    /**
     * Returns the default FocusTraversalPolicy. Top-level components use this
     * policy if they have not set a focus traversal policy explicitly.
     *
     * @return the default FocusTraversalPolicy, or null if none has been set
     */
    public TFocusTraversalPolicy getDefaultFocusTraversalPolicy() {
        return defaultFocusTraversalPolicy;
    }

    /**
     * Sets the default FocusTraversalPolicy. Top-level components use this
     * policy if they have not set a focus traversal policy explicitly.
     *
     * @param defaultPolicy the new default FocusTraversalPolicy
     */
    public void setDefaultFocusTraversalPolicy(TFocusTraversalPolicy defaultPolicy) {
        if (defaultPolicy == null) {
            throw new IllegalArgumentException("default focus traversal policy cannot be null");
        }
        this.defaultFocusTraversalPolicy = defaultPolicy;
    }

    /**
     * Sets the global focus owner. The focus owner is the Component that will
     * receive key events. If null is specified, the focus owner is cleared.
     *
     * @param focusOwner the focus owner, or null
     */
    public void setGlobalFocusOwner(TComponent focusOwner) {
        TComponent oldFocusOwner = this.focusOwner;

        if (oldFocusOwner == focusOwner) {
            return;
        }

        this.focusOwner = focusOwner;

        if (focusOwner == null || focusOwner.isFocusable()) {
            this.permanentFocusOwner = focusOwner;
        }

        // Update focused window
        if (focusOwner != null) {
            TWindow window = getContainingWindow(focusOwner);
            if (window != null && window != this.focusedWindow) {
                setGlobalFocusedWindow(window);
            }
        }

        // Integrate with legacy TFocusManager
        TFocusManager.get().setGlobalFocusOwner(focusOwner);
    }

    /**
     * Sets the focused window. The focused window is the window that contains
     * the focus owner.
     *
     * @param window the focused window, or null
     */
    protected void setGlobalFocusedWindow(TWindow window) {
        TWindow oldFocusedWindow = this.focusedWindow;
        this.focusedWindow = window;

        // When a window receives focus, also mark it as active
        if (window != null && window != this.activeWindow) {
            setGlobalActiveWindow(window);
        }
    }

    /**
     * Sets the active window. The active window is the window that is the
     * top-level ancestor of the focus owner.
     *
     * @param window the active window, or null
     */
    protected void setGlobalActiveWindow(TWindow window) {
        this.activeWindow = window;
    }

    /**
     * Returns a Set of default focus traversal keys for the given traversal
     * operation.
     *
     * @param id one of FORWARD_TRAVERSAL_KEYS, BACKWARD_TRAVERSAL_KEYS,
     *        UP_CYCLE_TRAVERSAL_KEYS, or DOWN_CYCLE_TRAVERSAL_KEYS
     * @return the Set of default focus traversal keys for the given operation
     * @throws IllegalArgumentException if id is not one of the valid identifiers
     */
    public Set<Integer> getDefaultFocusTraversalKeys(int id) {
        if (id < FORWARD_TRAVERSAL_KEYS || id > DOWN_CYCLE_TRAVERSAL_KEYS) {
            throw new IllegalArgumentException("invalid focus traversal key identifier");
        }
        return Collections.unmodifiableSet(defaultFocusTraversalKeys.get(id));
    }

    /**
     * Sets the default focus traversal keys for a given traversal operation.
     *
     * @param id one of FORWARD_TRAVERSAL_KEYS, BACKWARD_TRAVERSAL_KEYS,
     *        UP_CYCLE_TRAVERSAL_KEYS, or DOWN_CYCLE_TRAVERSAL_KEYS
     * @param keystrokes a Set of key codes for the focus traversal keys
     * @throws IllegalArgumentException if id is not one of the valid identifiers,
     *         or if keystrokes is null or empty
     */
    public void setDefaultFocusTraversalKeys(int id, Set<Integer> keystrokes) {
        if (id < FORWARD_TRAVERSAL_KEYS || id > DOWN_CYCLE_TRAVERSAL_KEYS) {
            throw new IllegalArgumentException("invalid focus traversal key identifier");
        }
        if (keystrokes == null || keystrokes.isEmpty()) {
            throw new IllegalArgumentException("focus traversal keys cannot be null or empty");
        }
        defaultFocusTraversalKeys.put(id, new HashSet<>(keystrokes));
    }

    /**
     * Initializes the default focus traversal keys with standard bindings:
     * - TAB: forward traversal
     * - Shift-TAB: backward traversal
     * - Ctrl-TAB: down cycle traversal
     * - Ctrl-Shift-TAB: up cycle traversal
     */
    private void initializeDefaultFocusTraversalKeys() {
        Set<Integer> forward = new HashSet<>();
        forward.add(TKeyEvent.VK_TAB);
        defaultFocusTraversalKeys.put(FORWARD_TRAVERSAL_KEYS, forward);

        Set<Integer> backward = new HashSet<>();
        backward.add(TKeyEvent.VK_TAB); // Will check for Shift modifier
        defaultFocusTraversalKeys.put(BACKWARD_TRAVERSAL_KEYS, backward);

        Set<Integer> upCycle = new HashSet<>();
        upCycle.add(TKeyEvent.VK_TAB); // Will check for Ctrl+Shift modifiers
        defaultFocusTraversalKeys.put(UP_CYCLE_TRAVERSAL_KEYS, upCycle);

        Set<Integer> downCycle = new HashSet<>();
        downCycle.add(TKeyEvent.VK_TAB); // Will check for Ctrl modifier
        defaultFocusTraversalKeys.put(DOWN_CYCLE_TRAVERSAL_KEYS, downCycle);
    }

    /**
     * Focuses the Component after the current focus owner, according to the
     * focus traversal policy of the current focus owner's focus cycle root.
     */
    public void focusNextComponent() {
        if (focusOwner == null) {
            return;
        }
        focusNextComponent(focusOwner);
    }

    /**
     * Focuses the Component after aComponent, according to the focus traversal
     * policy of aComponent's focus cycle root.
     *
     * @param aComponent the Component that is the basis for the focus traversal
     */
    public void focusNextComponent(TComponent aComponent) {
        if (aComponent == null) {
            return;
        }

        TContainer cycleRoot = getFocusCycleRootAncestor(aComponent);
        if (cycleRoot == null) {
            return;
        }

        TFocusTraversalPolicy policy = cycleRoot.getFocusTraversalPolicy();
        if (policy == null) {
            policy = defaultFocusTraversalPolicy;
        }

        TComponent next = policy.getComponentAfter(cycleRoot, aComponent);
        if (next != null) {
            next.requestFocus();
        }
    }

    /**
     * Focuses the Component before the current focus owner, according to the
     * focus traversal policy of the current focus owner's focus cycle root.
     */
    public void focusPreviousComponent() {
        if (focusOwner == null) {
            return;
        }
        focusPreviousComponent(focusOwner);
    }

    /**
     * Focuses the Component before aComponent, according to the focus traversal
     * policy of aComponent's focus cycle root.
     *
     * @param aComponent the Component that is the basis for the focus traversal
     */
    public void focusPreviousComponent(TComponent aComponent) {
        if (aComponent == null) {
            return;
        }

        TContainer cycleRoot = getFocusCycleRootAncestor(aComponent);
        if (cycleRoot == null) {
            return;
        }

        TFocusTraversalPolicy policy = cycleRoot.getFocusTraversalPolicy();
        if (policy == null) {
            policy = defaultFocusTraversalPolicy;
        }

        TComponent previous = policy.getComponentBefore(cycleRoot, aComponent);
        if (previous != null) {
            previous.requestFocus();
        }
    }

    /**
     * Moves the focus up one focus cycle. This is typically invoked when the
     * user presses Ctrl-Shift-TAB.
     */
    public void upFocusCycle() {
        if (focusOwner == null) {
            return;
        }
        upFocusCycle(focusOwner);
    }

    /**
     * Moves the focus up one focus cycle from aComponent. This is typically
     * invoked when the user presses Ctrl-Shift-TAB.
     *
     * @param aComponent the Component that is the basis for the focus cycle
     */
    public void upFocusCycle(TComponent aComponent) {
        if (aComponent == null) {
            return;
        }

        TContainer cycleRoot = getFocusCycleRootAncestor(aComponent);
        if (cycleRoot == null) {
            return;
        }

        // Move focus to the cycle root's focus cycle root ancestor
        TContainer parentCycleRoot = getFocusCycleRootAncestor(cycleRoot);
        if (parentCycleRoot != null) {
            TFocusTraversalPolicy policy = parentCycleRoot.getFocusTraversalPolicy();
            if (policy == null) {
                policy = defaultFocusTraversalPolicy;
            }

            TComponent next = policy.getComponentAfter(parentCycleRoot, cycleRoot);
            if (next != null) {
                next.requestFocus();
            }
        }
    }

    /**
     * Moves the focus down one focus cycle. This is typically invoked when the
     * user presses Ctrl-TAB.
     */
    public void downFocusCycle() {
        if (focusOwner == null) {
            return;
        }
        downFocusCycle(focusOwner);
    }

    /**
     * Moves the focus down one focus cycle from aComponent. This is typically
     * invoked when the user presses Ctrl-TAB.
     *
     * @param aComponent the Component that is the basis for the focus cycle
     */
    public void downFocusCycle(TComponent aComponent) {
        if (aComponent == null) {
            return;
        }

        if (aComponent instanceof TContainer) {
            TContainer container = (TContainer) aComponent;
            if (container.isFocusCycleRoot()) {
                TFocusTraversalPolicy policy = container.getFocusTraversalPolicy();
                if (policy == null) {
                    policy = defaultFocusTraversalPolicy;
                }

                TComponent defaultComponent = policy.getDefaultComponent(container);
                if (defaultComponent != null) {
                    defaultComponent.requestFocus();
                }
            }
        }
    }

    /**
     * Returns the focus cycle root ancestor of the given Component, or null if
     * there is none.
     *
     * @param component the Component whose focus cycle root ancestor is sought
     * @return the focus cycle root ancestor, or null
     */
    private TContainer getFocusCycleRootAncestor(TComponent component) {
        TContainer parent = component.getParent();
        while (parent != null) {
            if (parent.isFocusCycleRoot()) {
                return parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    /**
     * Returns the Window that contains the given Component, or null if the
     * Component is not contained in a Window.
     *
     * @param component the Component whose containing Window is sought
     * @return the containing Window, or null
     */
    private TWindow getContainingWindow(TComponent component) {
        TContainer parent = component.getParent();
        while (parent != null) {
            if (parent instanceof TWindow) {
                return (TWindow) parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    /**
     * Processes a key event for focus traversal. Returns true if the event
     * was handled as a focus traversal key.
     *
     * @param component the component that received the key event
     * @param event the key event
     * @return true if the event was a focus traversal key and was handled
     */
    public boolean processKeyEvent(TComponent component, TKeyEvent event) {
        // Only process KEY_PRESSED events
        if (event.getID() != TKeyEvent.KEY_PRESSED) {
            return false;
        }

        // Check if focus traversal keys are enabled for this component
        if (!component.isFocusTraversalKeysEnabled()) {
            return false;
        }

        int keyCode = event.getKeyCode();
        int modifiers = event.getModifiers();

        // Check for focus traversal keys
        if (keyCode == TKeyEvent.VK_TAB) {
            boolean shift = (modifiers & TKeyEvent.FLAG_SHIFT) != 0;
            boolean ctrl = (modifiers & TKeyEvent.FLAG_CTRL) != 0;

            if (ctrl && shift) {
                // Ctrl-Shift-TAB: up cycle
                upFocusCycle(component);
                return true;
            } else if (ctrl) {
                // Ctrl-TAB: down cycle
                downFocusCycle(component);
                return true;
            } else if (shift) {
                // Shift-TAB: backward
                focusPreviousComponent(component);
                return true;
            } else {
                // TAB: forward
                focusNextComponent(component);
                return true;
            }
        }

        return false;
    }
}
