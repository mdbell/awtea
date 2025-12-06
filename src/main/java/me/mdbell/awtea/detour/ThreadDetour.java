package me.mdbell.awtea.detour;

import me.mdbell.awtea.monitor.ThreadMonitor;

@NoDetours
public class ThreadDetour {

	public static Thread init(Runnable target) {
		Thread thread = new Thread(wrapRunnable(target));
		ThreadMonitor.get().register(thread);
		return thread;
	}

	public static Thread init(){
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
		Thread thread = new Thread(wrapRunnable(target), name);
		ThreadMonitor.get().register(thread);
		return thread;
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

	private static Runnable wrapRunnable(Runnable target) {
		return () -> {
			try {
				ThreadMonitor.get().onRunEnter(Thread.currentThread());
				target.run();
			} finally {
				ThreadMonitor.get().onRunExit(Thread.currentThread());
			}
		};
	}
}
