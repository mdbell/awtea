package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.event.*;
import me.mdbell.awtea.input.GlobalInputManager;
import me.mdbell.awtea.monitor.EventQueueMonitor;
import me.mdbell.awtea.monitor.EventTypeMonitor;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @see java.awt.EventQueue
 */
public class TEventQueue {

    private final Queue[] queues = new Queue[NUM_PRIORITIES];

    private static final int LOW_PRIORITY = 0;
    private static final int NORM_PRIORITY = 1;
    private static final int HIGH_PRIORITY = 2;
    private static final int ULTIMATE_PRIORITY = 3;

    private static final int NUM_PRIORITIES = ULTIMATE_PRIORITY + 1;

    protected static final Object lock = new Object();

    private static final AtomicInteger threadCounter = new AtomicInteger(0);

    private final String name = "AWTea-EventQueue-" + threadCounter.getAndIncrement();

    private TEventDispatchThread eventDispatchThread;

    private TEventQueue nextQueue;
    private TEventQueue prevQueue;

    private WeakReference<TAWTEvent> currentEvent;

    private long mostRecentEventTime = System.currentTimeMillis();

    private long mostRecentKeyEventTime = System.currentTimeMillis();

    private static Runnable DUMMY = () -> {
        // do nothing, used to wake up the EDT when a new queue is pushed
    };

    private static final int PAINT_CACHE_IDX = 0;
    private static final int UPDATE_CACHE_IDX = 1;
    private static final int MOVE_CACHE_IDX = 2;
    private static final int DRAG_CACHE_IDX = 3;
    private static final int PEER_CACHE_IDX = 4;
    private static final int CACHE_LENGTH = PEER_CACHE_IDX + 1;

    public TEventQueue() {
        for (int i = 0; i < NUM_PRIORITIES; i++) {
            queues[i] = new Queue();
        }
    }

    public void postEvent(TAWTEvent event) {
        if (nextQueue != null) {
            nextQueue.postEvent(event);
            return;
        }

        if (GlobalInputManager.getInstance().handleEvent(event)) {
            // event was consumed by global input manager
            return;
        }

        int priority = getPriorityForEvent(event);

        synchronized (lock) {

            // attempt to coalesce events
            if (coalesceEvent(event)) {
                EventTypeMonitor.get().onCoalesce(event);
                return;
            }

            // monitor: after enqueue, compute pending counts
            EventQueueMonitor.get().onPost(this, priority, snapshotPendingCounts());
            EventTypeMonitor.get().onPost(event);
            postEventInternal(priority, event);
        }
    }

    private boolean coalesceEvent(TAWTEvent event) {
        if (!(event.getSource() instanceof TComponent)) {
            return false;
        }
        int cacheIdx = eventToCacheIndex(event);
        if (cacheIdx == -1) {
            return false;
        }

        TComponent source = (TComponent) event.getSource();
        if (source.eventCache == null) {
            return false;
        }

        switch (event.getID()) {
            case TPaintEvent.PAINT:
            case TPaintEvent.UPDATE:
                return coalescePaintEvent(source, (TPaintEvent) event);
            case TMouseEvent.MOUSE_MOVED:
                return coalesceMouseMoveEvent(source, (TMouseEvent) event);
            default:
                return false;
        }
    }

    private boolean coalescePaintEvent(TComponent source, TPaintEvent event) {
        // if we're an UPDATE event, and there's a PAINT event pending, we can discard the paint
        // since UPDATE is a full repaint
        if (event.getID() == TPaintEvent.UPDATE) {
            if (source.eventCache[PAINT_CACHE_IDX] != null) {
                source.eventCache[PAINT_CACHE_IDX].unlinkFrom(queues[getPriorityForEvent(source.eventCache[PAINT_CACHE_IDX].event)]);
                source.eventCache[PAINT_CACHE_IDX] = null;
            }

            EventQueueItem cachedUpdate = source.eventCache[UPDATE_CACHE_IDX];
            if (cachedUpdate != null) {
                cachedUpdate.event = event; // or merge rectangles here
                return true; // don't enqueue a new one
            }
            // else, let it be enqueued by the main code path
            return false;
        }

        // PAINT event: if there's an UPDATE event pending, discard this PAINT
        if (source.eventCache[UPDATE_CACHE_IDX] != null) {
            return true;
        }

        EventQueueItem cachedPaint = source.eventCache[PAINT_CACHE_IDX];
        if (cachedPaint != null) {
            // merge rectangles
            TPaintEvent cachedEvent = (TPaintEvent) cachedPaint.event;
            TRectangle r1 = cachedEvent.getUpdateRect();
            TRectangle r2 = event.getUpdateRect();
            int x = Math.min(r1.x, r2.x);
            int y = Math.min(r1.y, r2.y);
            int right = Math.max(r1.x + r1.width, r2.x + r2.width);
            int bottom = Math.max(r1.y + r1.height, r2.y + r2.height);
            TRectangle merged = new TRectangle(x, y, right - x, bottom - y);
            cachedEvent.setUpdateRect(merged);
            return true;
        }
        return false;
    }

    private boolean coalesceMouseMoveEvent(TComponent source, TMouseEvent event) {
        TEventQueue.EventQueueItem cachedItem = source.eventCache[MOVE_CACHE_IDX];
        if (cachedItem != null) {
            // update the coordinates of the cached event
            cachedItem.event = event;
            return true;
        }
        return false;
    }

    private int getPriorityForEvent(TAWTEvent event) {
        if (event instanceof TInvocationEvent) {
            return ULTIMATE_PRIORITY;
        } else if (event instanceof TActiveEvent) {
            return HIGH_PRIORITY;
        } else if (event instanceof TPaintEvent) {
            return LOW_PRIORITY; // Paint events need to be low priority to avoid starving input events
        } else {
            return NORM_PRIORITY;
        }
    }

    private void postEventInternal(int priority, TAWTEvent event) {
        EventQueueItem item = queues[priority].offer(event);
        cacheItem(item);
        initEventDispatchThread();
        lock.notifyAll();
    }

    private int[] snapshotPendingCounts() {
        int[] counts = new int[NUM_PRIORITIES];
        for (int i = 0; i < NUM_PRIORITIES; i++) {
            int n = 0;
            Queue q = queues[i];
            for (TAWTEvent ignored : q) {
                n++;
            }
            counts[i] = n;
        }
        return counts;
    }

    private void initEventDispatchThread() {
        if (eventDispatchThread == null || !eventDispatchThread.getThread().isAlive()) {
            eventDispatchThread = new TEventDispatchThread(name, this);
            eventDispatchThread.start();
        }
    }

    /**
     * Returns the next event without removing it from the queue.
     */
    public TAWTEvent peekEvent() {
        synchronized (lock) {
            for (int i = NUM_PRIORITIES - 1; i >= 0; i--) {
                Queue queue = queues[i];
                TAWTEvent event = queue.peek();
                if (event != null) {
                    return event;
                }
            }
            return null;
        }
    }

    /**
     * Returns the next event of the specified ID without removing it.
     */
    public TAWTEvent peekEvent(int id) {
        synchronized (lock) {
            for (int i = NUM_PRIORITIES - 1; i >= 0; i--) {
                Queue queue = queues[i];
                for (TAWTEvent event : queue) {
                    if (event.getID() == id) {
                        return event;
                    }
                }
            }
            return null;
        }
    }

    TAWTEvent getNextEvent() throws InterruptedException {
        synchronized (lock) {
            for (; ; ) {
                if (nextQueue != null) {
                    // us essentially telling the EDT to re-query the next queue
                    return null;
                }
                // check all priorities, highest first
                for (int i = NUM_PRIORITIES - 1; i >= 0; i--) {
                    Queue queue = queues[i];
                    EventQueueItem item = queue.poll();
                    if (item != null) {
                        uncacheItem(item);
                        return item.event;
                    }
                }

                // no events: park the EDT until someone posts one
                lock.wait();
                // on wake, loop and check queues again
            }
        }
    }

    protected void dispatchEvent(TAWTEvent event) {
        if (event == null) {
            return;
        }

        long start = System.currentTimeMillis();
        try {
            Object source = event.getSource();

            if (event instanceof TActiveEvent) {
                ((TActiveEvent) event).dispatch();
            } else if (source instanceof TComponent) {
                ((TComponent) source).dispatchEvent(event);
            } else {
                // TODO: other event types
            }
        } finally {
            // After dispatch, queue sizes may have changed; we can recompute counts
            int[] counts = snapshotPendingCounts();
            long dt = System.currentTimeMillis() - start;
            EventQueueMonitor.get().onDispatch(this, counts, dt, event);
            EventTypeMonitor.get().onDispatch(event, dt);
        }
    }

    public static boolean isDispatchThread() {
        TEventQueue queue = TToolkit.getEventQueue();
        return queue.isDispatchThreadImpl();
    }

    private boolean isDispatchThreadImpl() {
        TEventQueue eq = getTailEventQueue();
        return eq.eventDispatchThread != null &&
                Thread.currentThread() == eq.eventDispatchThread.getThread();
    }

    static void setCurrentEventAndMostRecentTime(TAWTEvent event) {
        TToolkit.getEventQueue().setCurrentEventAndMostRecentTimeImpl(event);
    }

    private void setCurrentEventAndMostRecentTimeImpl(TAWTEvent event) {
        this.currentEvent = new WeakReference<>(event);

        long mostRecentTime = resolveWhen(event);

        if (mostRecentTime > mostRecentEventTime) {
            mostRecentEventTime = mostRecentTime;
        }
    }

    private long resolveWhen(TAWTEvent event) {
        if (event instanceof TInputEvent) {
            long when = ((TInputEvent) event).getWhen();
            if (event instanceof TKeyEvent) {
                mostRecentKeyEventTime = when;
            }
            return when;
        } else if (event instanceof TActionEvent) {
            return ((TActionEvent) event).getWhen();
        } else if (event instanceof TInvocationEvent) {
            return ((TInvocationEvent) event).getWhen();
        }
        return Long.MIN_VALUE;
    }


    public static void invokeLater(Runnable runnable) {
        TToolkit.getEventQueue().postEvent(
                new TInvocationEvent(TToolkit.getDefaultToolkit(), runnable));
    }

    /**
     * Causes runnable to be executed synchronously on the event dispatch thread.
     * In TeaVM, this executes immediately since we're always on the dispatch thread.
     */
    public static void invokeAndWait(Runnable runnable) throws InterruptedException, InvocationTargetException {
        invokeAndWait(TToolkit.getDefaultToolkit(), runnable);
    }

    private static void invokeAndWait(Object source, Runnable runnable) throws InvocationTargetException, InterruptedException {
        if (isDispatchThread()) {
            throw new Error("Cannot call invokeAndWait from the event dispatch thread");
        }

        TInvocationEvent event = new TInvocationEvent(source, runnable, null, true);

        TToolkit.getEventQueue().postEvent(event);

        event.getPromise().await();

        Throwable throwable = event.getThrowable();
        if (throwable != null) {
            throw new InvocationTargetException(throwable);
        }
    }

    public static TAWTEvent getCurrentEvent() {
        TEventQueue queue = TToolkit.getEventQueue();
        return queue.getCurrentEventImpl();
    }

    private TAWTEvent getCurrentEventImpl() {
        if (!isDispatchThread()) {
            return null;
        }
        return currentEvent.get();
    }

    public void push(TEventQueue newEventQueue) {
        synchronized (lock) {
            TEventQueue tail = getTailEventQueue();

            // Transfer the event dispatch thread to the new queue
            newEventQueue.eventDispatchThread = tail.eventDispatchThread;
            if (tail.eventDispatchThread != null && tail.eventDispatchThread.getEventQueue() == this) {
                tail.eventDispatchThread.setEventQueue(newEventQueue);
                // ensures the thread isn't waiting on us to post events
                lock.notifyAll();
            }

            // drain all events from the old queue into the new one
            for (int i = NUM_PRIORITIES - 1; i >= 0; i--) {
                Queue queue = tail.queues[i];
                EventQueueItem item = queue.head;
                if (item == null) {
                    continue;
                }
                newEventQueue.queues[i].adopt(queue);
            }

            // Wake up the event dispatch thread if it's waiting
            newEventQueue.postEventInternal(ULTIMATE_PRIORITY, new TInvocationEvent(tail, DUMMY));

            // Link the queues
            tail.nextQueue = newEventQueue;
            newEventQueue.prevQueue = tail;
        }
    }

    protected void pop() throws EmptyStackException {
        if (prevQueue == null) {
            throw new EmptyStackException();
        }

        synchronized (lock) {

            // Transfer the event dispatch thread back to the previous queue
            if (eventDispatchThread != null) {
                prevQueue.eventDispatchThread = eventDispatchThread;
                eventDispatchThread.setEventQueue(prevQueue);
                eventDispatchThread = null;
                // ensures the thread isn't waiting on us to post events
                lock.notifyAll();
            }

            // transfer any remaining events back to the previous queue
            for (int i = NUM_PRIORITIES - 1; i >= 0; i--) {
                Queue queue = queues[i];
                EventQueueItem item = queue.head;
                if (item == null) {
                    continue;
                }
                prevQueue.queues[i].adopt(queue);
            }

            // Unlink the queues
            prevQueue.nextQueue = null;
            prevQueue = null;

        }
    }

    private TEventQueue getTailEventQueue() {
        TEventQueue eq = this;
        while (eq.nextQueue != null) {
            eq = eq.nextQueue;
        }
        return eq;
    }

    private boolean noEvents() {
        for (int i = 0; i < NUM_PRIORITIES; i++) {
            if (queues[i].head != null) {
                return false;
            }
        }

        return true;
    }

    private void cacheItem(EventQueueItem item) {
        TAWTEvent event = item.event;
        if (!(event.getSource() instanceof TComponent)) {
            return;
        }
        int cacheIdx = eventToCacheIndex(event);
        if (cacheIdx == -1) {
            return;
        }
        TComponent source = (TComponent) event.getSource();
        if (source.eventCache == null) {
            source.eventCache = new TEventQueue.EventQueueItem[CACHE_LENGTH];
        }
        source.eventCache[cacheIdx] = item;
    }

    private void uncacheItem(EventQueueItem item) {
        TAWTEvent event = item.event;
        if (!(event.getSource() instanceof TComponent)) {
            return;
        }
        int cacheIdx = eventToCacheIndex(event);
        if (cacheIdx == -1) {
            return;
        }
        TComponent source = (TComponent) event.getSource();
        if (source.eventCache == null) {
            return;
        }
        source.eventCache[cacheIdx] = null;
    }

    private static int eventToCacheIndex(TAWTEvent event) {
        switch (event.getID()) {
            case TPaintEvent.UPDATE:
                return UPDATE_CACHE_IDX;
            case TPaintEvent.PAINT:
                return PAINT_CACHE_IDX;
            // update events (TODO)
            case TMouseEvent.MOUSE_MOVED:
                return MOVE_CACHE_IDX;
            // drag events - we presently do not support drag events at all
            // peer events (TODO) - currently not used
            default:
                return -1;
        }
    }

    static class Queue implements Iterable<TAWTEvent> {
        EventQueueItem head;
        EventQueueItem tail;

        EventQueueItem offer(TAWTEvent event) {
            EventQueueItem item = new EventQueueItem(event);
            if (tail != null) {
                item.prev = tail;
                tail.next = item;
                tail = item;
            } else {
                head = tail = item;
            }
            return item;
        }

        EventQueueItem poll() {
            if (head == null) {
                return null;
            }
            EventQueueItem item = head;
            head = head.next;
            if (head == null) {
                tail = null;
            } else {
                head.prev = null;
            }
            item.next = null;
            return item;
        }

        TAWTEvent peek() {
            if (head == null) {
                return null;
            }
            return head.event;
        }

        public void adopt(Queue queue) {
            if (queue.head == null) {
                return;
            }
            if (this.tail != null) {
                this.tail.next = queue.head;
                queue.head.prev = this.tail;
            } else {
                this.head = queue.head;
            }
            this.tail = queue.tail;
            queue.head = null;
            queue.tail = null;
        }


        @Override
        public Iterator<TAWTEvent> iterator() {
            return new Iterator<>() {
                private EventQueueItem current = head;

                @Override
                public boolean hasNext() {
                    return current != null;
                }

                @Override
                public TAWTEvent next() {
                    TAWTEvent event = current.event;
                    current = current.next;
                    return event;
                }
            };
        }
    }

    static class EventQueueItem {
        TAWTEvent event;
        EventQueueItem prev;
        EventQueueItem next;

        EventQueueItem(TAWTEvent event) {
            this.event = event;
        }

        void unlinkFrom(Queue owner) {
            if (prev != null) {
                prev.next = next;
            } else {
                owner.head = next;   // we were head
            }
            if (next != null) {
                next.prev = prev;
            } else {
                owner.tail = prev;   // we were tail
            }
            prev = null;
            next = null;
        }
    }
}
