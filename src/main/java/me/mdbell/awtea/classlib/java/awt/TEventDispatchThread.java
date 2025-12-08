package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;

class TEventDispatchThread {

	@Getter
	private final Thread thread;
	private TEventQueue eventQueue;
	private boolean running = true;

	protected final Object lock = new Object();

	protected TEventDispatchThread(String name, TEventQueue queue) {
		this.thread = new Thread(this::run, name);
		this.thread.setPriority(Thread.NORM_PRIORITY + 1);
		this.thread.setDaemon(false);
		this.eventQueue = queue;
	}

	void setEventQueue(TEventQueue queue) {
		this.eventQueue = queue;
	}
	
	synchronized TEventQueue getEventQueue() {
		return eventQueue;
	}

	public void start() {
		thread.start();
	}

	public void run() {
		System.out.println("Event Dispatch Thread started.");
		pumpEvents();
	}


	private synchronized void pumpEvents() {
		try {
			while (running && !Thread.currentThread().isInterrupted()) {
				TEventQueue queue = getEventQueue();
				// This will BLOCK until an event is posted
				TAWTEvent event = queue.getNextEvent();
				// eventQueue.getNextEvent() returns only when it has a real event
				eventQueue.dispatchEvent(event);
			}
		} catch (InterruptedException e) {
			// exit gracefully
		} finally {
			running = false;
		}
	}
}
