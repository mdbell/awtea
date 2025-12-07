package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.event.TActiveEvent;
import me.mdbell.awtea.classlib.java.awt.event.TInvocationEvent;

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
		postEventInternal(NORM_PRIORITY, event);
	}

	private void postEventInternal(int priority, TAWTEvent event) {
		priorityQueues[priority].offer(event);
		initEventDispatchThread();

		synchronized (lock) {
			if (eventDispatchThread != null) {
				lock.notifyAll();
			}
		}
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

		Object source = event.getSource();

//		Debug.trigger();

		if (event instanceof TActiveEvent) {
			((TActiveEvent) event).dispatch();
		} else if (source instanceof TComponent) {
			TComponent comp = (TComponent) source;
			comp.dispatchEvent(event);
		} else {
			//TODO: rest of event types, e.g. WindowEvent, ActionEvent, etc.
//			System.err.println("Warning: Unhandled event type: " + event.getClass().getName());
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
