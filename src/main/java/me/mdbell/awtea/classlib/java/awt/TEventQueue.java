package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.event.TComponentEvent;

import java.util.LinkedList;
import java.util.Queue;

/**
 * TeaVM implementation of java.awt.EventQueue.
 * Provides a simple event dispatching queue for AWT events in the browser environment.
 */
public class TEventQueue {

    private static final TEventQueue systemEventQueue = new TEventQueue();

    private final Queue<TAWTEvent> eventQueue = new LinkedList<>();
    private TComponent dispatchTarget;

    public TEventQueue() {
    }

    /**
     * Gets the system event queue.
     * In TeaVM, this returns a singleton instance since there's only one event thread.
     */
    public static TEventQueue getSystemEventQueue() {
        return systemEventQueue;
    }

    /**
     * Posts an event to the queue.
     * In the browser environment, events are typically dispatched synchronously,
     * but this maintains the queue structure for compatibility.
     */
    public void postEvent(TAWTEvent event) {
        eventQueue.offer(event);
        processEvents();
    }

    /**
     * Returns the next event without removing it from the queue.
     */
    public TAWTEvent peekEvent() {
        return eventQueue.peek();
    }

    /**
     * Returns the next event of the specified ID without removing it.
     */
    public TAWTEvent peekEvent(int id) {
        for (TAWTEvent event : eventQueue) {
            if (event.getId() == id) {
                return event;
            }
        }
        return null;
    }

    /**
     * Removes and returns the next event from the queue.
     * In TeaVM, this processes events synchronously.
     * Note: wait() is not supported in browser environment, so this returns immediately.
     */
    public TAWTEvent getNextEvent() throws InterruptedException {
        return eventQueue.poll();
    }

    /**
     * Dispatches an event to its target component.
     * Component events are dispatched to their target component.
     * Other AWT events may be handled differently based on type.
     */
    public void dispatchEvent(TAWTEvent event) {
        if (event == null) {
            return;
        }

        // If it's a component event, dispatch to the component
        if (event instanceof TComponentEvent) {
            TComponentEvent componentEvent = (TComponentEvent) event;
            TComponent target = componentEvent.getComponent();
            if (target != null) {
                target.dispatchEvent(componentEvent);
            }
        }
        // Other AWTEvent types would be handled here if needed
    }

    /**
     * Processes all pending events in the queue.
     * This is called automatically when events are posted.
     */
    private void processEvents() {
        while (!eventQueue.isEmpty()) {
            TAWTEvent event = eventQueue.poll();
            if (event != null) {
                dispatchEvent(event);
            }
        }
    }

    /**
     * Returns true if the calling thread is the event dispatch thread.
     * In TeaVM/browser environment, all code runs on the main thread.
     */
    public static boolean isDispatchThread() {
        return true;
    }

    /**
     * Causes runnable to be executed on the event dispatch thread.
     * In TeaVM, this executes immediately since we're always on the dispatch thread.
     */
    public static void invokeLater(Runnable runnable) {
        if (runnable != null) {
            runnable.run();
        }
    }

    /**
     * Causes runnable to be executed synchronously on the event dispatch thread.
     * In TeaVM, this executes immediately since we're always on the dispatch thread.
     */
    public static void invokeAndWait(Runnable runnable) throws InterruptedException {
        if (runnable != null) {
            runnable.run();
        }
    }

    /**
     * Returns the most recent event in the queue.
     */
    public TAWTEvent getMostRecentEvent() {
        TAWTEvent mostRecent = null;
        for (TAWTEvent event : eventQueue) {
            mostRecent = event;
        }
        return mostRecent;
    }

    /**
     * Pushes a new EventQueue onto the dispatch thread.
     * Stubbed for TeaVM as we use a simple single-queue model.
     */
    public void push(TEventQueue newEventQueue) {
        // Stubbed - not typically needed in browser environment
    }

    /**
     * Removes this EventQueue from the dispatch thread.
     * Stubbed for TeaVM as we use a simple single-queue model.
     */
    protected void pop() throws java.util.EmptyStackException {
        // Stubbed - not typically needed in browser environment
    }
}
