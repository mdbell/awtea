package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.event.TActionEvent;
import me.mdbell.awtea.classlib.java.awt.event.TActiveEvent;
import me.mdbell.awtea.classlib.java.awt.event.TInvocationEvent;
import me.mdbell.awtea.monitor.EventQueueMonitor;
import me.mdbell.awtea.monitor.EventTypeMonitor;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;

/**
 * @see java.awt.EventQueue
 */
public class TEventQueue {

	private final Queue[] priorityQueues = new Queue[NUM_PRIORITIES];

	private static final int LOW_PRIORITY = 0;
	private static final int NORM_PRIORITY = 1;
	private static final int HIGH_PRIORITY = 2;
	private static final int ULTIMATE_PRIORITY = 3;

	private static final int NUM_PRIORITIES = ULTIMATE_PRIORITY + 1;

	private static TEventDispatchThread eventDispatchThread;

	protected static final Object lock = new Object();

	public TEventQueue() {
		for (int i = 0; i < NUM_PRIORITIES; i++) {
			priorityQueues[i] = new Queue();
		}
	}

	public void postEvent(TAWTEvent event) {
		postEventInternal(getPriorityForEvent(event), event);
	}

	private int getPriorityForEvent(TAWTEvent event) {
		if (event instanceof TInvocationEvent) {
			return ULTIMATE_PRIORITY;
		} else if (event instanceof TActiveEvent) {
			return HIGH_PRIORITY;
		} else if (event instanceof TActionEvent) {
			return LOW_PRIORITY;
		} else {
			return NORM_PRIORITY;
		}
	}

	private void postEventInternal(int priority, TAWTEvent event) {
		synchronized (lock) {
			priorityQueues[priority].offer(event);
			initEventDispatchThread();

			// monitor: after enqueue, compute pending counts
			EventQueueMonitor.get().onPost(this, priority, snapshotPendingCounts());
			EventTypeMonitor.get().onPost(event);

			lock.notifyAll();
		}
	}

	private int[] snapshotPendingCounts() {
		int[] counts = new int[NUM_PRIORITIES];
		for (int i = 0; i < NUM_PRIORITIES; i++) {
			int n = 0;
			Queue q = priorityQueues[i];
			for (TAWTEvent ignored : q) {
				n++;
			}
			counts[i] = n;
		}
		return counts;
	}

	private void initEventDispatchThread() {
		if (eventDispatchThread == null) {
			eventDispatchThread = new TEventDispatchThread("AWTea-EventDispatcher", this);
			eventDispatchThread.start();
		}
	}

	/**
	 * Returns the next event without removing it from the queue.
	 */
	public TAWTEvent peekEvent() {
		synchronized (lock) {
			for (int i = NUM_PRIORITIES - 1; i >= 0; i--) {
				Queue queue = priorityQueues[i];
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
				Queue queue = priorityQueues[i];
				for (TAWTEvent event : queue) {
					if (event.getID() == id) {
						return event;
					}
				}
			}
			return null;
		}
	}

	/**
	 * Removes and returns the next event from the queue.
	 * In TeaVM, this processes events synchronously.
	 * Note: wait() is not supported in browser environment, so this returns immediately.
	 */
	public TAWTEvent getNextEvent() throws InterruptedException {
		synchronized (lock) {
			for (; ; ) {
				// check all priorities, highest first
				for (int i = NUM_PRIORITIES - 1; i >= 0; i--) {
					Queue queue = priorityQueues[i];
					TAWTEvent event = queue.poll();
					if (event != null) {
						return event; // lock is released when we exit this block
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
		return eventDispatchThread != null && Thread.currentThread() == eventDispatchThread.getThread();
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

	static class Queue implements Iterable<TAWTEvent> {
		EventQueueItem head;
		EventQueueItem tail;

		void offer(TAWTEvent event) {
			EventQueueItem item = new EventQueueItem(event);
			if (tail != null) {
				tail.next = item;
				tail = item;
			} else {
				head = tail = item;
			}
		}

		TAWTEvent poll() {
			if (head == null) {
				return null;
			}
			TAWTEvent event = head.event;
			head = head.next;
			if (head == null) {
				tail = null;
			}
			return event;
		}

		TAWTEvent peek() {
			if (head == null) {
				return null;
			}
			return head.event;
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
		EventQueueItem next;

		EventQueueItem(TAWTEvent event) {
			this.event = event;
		}
	}
}
