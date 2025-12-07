package me.mdbell.awtea.detour;

import me.mdbell.awtea.monitor.ThreadMonitor;

@NoDetours
public class ThreadDetour {

	public static Thread init(Runnable target) {
		Thread thread = new Thread(target);
		ThreadMonitor.get().register(thread);
		return thread;
	}

	public static Thread init() {
		Thread thread = new Thread();
		ThreadMonitor.get().register(thread);
		return thread;
	}

	public static Thread init(String name) {
		Thread thread = new Thread(name);
		ThreadMonitor.get().register(thread);
		return thread;
	}

	public static Thread init(Runnable target, String name) {
		Thread thread = new Thread(target, name);
		ThreadMonitor.get().register(thread);
		return thread;
	}

	public static void run(Thread thread) {
		try {
			ThreadMonitor.get().onRunEnter(thread);
			thread.run();
		} finally {
			ThreadMonitor.get().onRunExit(thread);
		}
	}

	public static void start(Thread thread) {
		ThreadMonitor.get().onStart(thread);
		thread.start();
	}

	public static void sleep(long millis) throws InterruptedException {
		try {
			ThreadMonitor.get().onSleep(Thread.currentThread());
			Thread.sleep(millis);
		} finally {
			ThreadMonitor.get().onWake(Thread.currentThread());
		}
	}
}
