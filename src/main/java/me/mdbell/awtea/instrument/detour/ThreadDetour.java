package me.mdbell.awtea.instrument.detour;

import me.mdbell.awtea.instrument.DetourMethod;
import me.mdbell.awtea.instrument.DetourReceiver;
import me.mdbell.awtea.instrument.NoDetours;
import me.mdbell.awtea.monitor.ThreadMonitor;

@NoDetours
@DetourReceiver(target = Thread.class)
public class ThreadDetour {

	@DetourMethod(constructor = true)
	public static Thread init(Runnable target) {
		Thread thread = new Thread(target);
		ThreadMonitor.get().register(thread);
		return thread;
	}

	@DetourMethod(constructor = true)
	public static Thread init() {
		Thread thread = new Thread();
		ThreadMonitor.get().register(thread);
		return thread;
	}

	@DetourMethod(constructor = true)
	public static Thread init(String name) {
		Thread thread = new Thread(name);
		ThreadMonitor.get().register(thread);
		return thread;
	}

	@DetourMethod(constructor = true)
	public static Thread init(Runnable target, String name) {
		Thread thread = new Thread(target, name);
		ThreadMonitor.get().register(thread);
		return thread;
	}

	@DetourMethod
	public static void run(Thread thread) {
		try {
			ThreadMonitor.get().onRunEnter(thread);
			thread.run();
		} finally {
			ThreadMonitor.get().onRunExit(thread);
		}
	}

	@DetourMethod
	public static void start(Thread thread) {
		ThreadMonitor.get().onStart(thread);
		thread.start();
	}

	@DetourMethod
	public static void sleep(long millis) throws InterruptedException {
		try {
			ThreadMonitor.get().onSleep(Thread.currentThread());
			Thread.sleep(millis);
		} finally {
			ThreadMonitor.get().onWake(Thread.currentThread());
		}
	}
}
