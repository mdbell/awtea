package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import lombok.Getter;

class TEventDispatchThread {

	private static final Logger log = LoggerFactory.getLogger(TEventDispatchThread.class);

	@Getter
	private final Thread thread;
	private TEventQueue eventQueue;
	private boolean running = true;

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
		log.info("Event Dispatch Thread started.");
		pumpEvents();
	}


	private synchronized void pumpEvents() {
		try {
			while (running && !Thread.currentThread().isInterrupted()) {
				TEventQueue queue = getEventQueue();
				// This will BLOCK until an event is posted
				TAWTEvent event = queue.getNextEvent();

				if (event == null) {
					continue;
				}

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
