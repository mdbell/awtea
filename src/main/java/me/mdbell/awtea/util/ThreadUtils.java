package me.mdbell.awtea.util;

import lombok.experimental.UtilityClass;

import java.util.PriorityQueue;

@UtilityClass
public class ThreadUtils {

	private String schedulerThreadName = "ThreadUtils-Scheduler";

	// --- scheduler state ---

	private final Object lock = new Object();

	private final PriorityQueue<ScheduledTask> queue = new PriorityQueue<>();

	private volatile boolean schedulerRunning = false;

	// --- Public API ---

	public void runAtFixedRate(Runnable runnable, long periodMillis) {
		synchronized (lock) {
			if (!schedulerRunning) {
				startSchedulerThread();
			}

			long now = System.currentTimeMillis();
			long nextRun = now + periodMillis;

			queue.add(new ScheduledTask(nextRun, periodMillis, runnable));
			lock.notifyAll();
		}
	}

	// --- scheduler thread loop ---

	private void startSchedulerThread() {
		schedulerRunning = true;
		Thread schedulerThread = new Thread(ThreadUtils::pump, schedulerThreadName);
		schedulerThread.setPriority(5); // normal priority
		schedulerThread.setDaemon(true);
		schedulerThread.start();
	}

	private void pump() {
		try {
			while (schedulerRunning) {
				ScheduledTask task;

				synchronized (lock) {
					while (queue.isEmpty()) {
						lock.wait(); // park here until we have something to do
					}

					long now = System.currentTimeMillis();
					task = queue.peek();

					long delay = task.nextRunTime - now;
					if (delay > 0) {
						lock.wait(delay); // sleep until the earliest task is due
						continue;
					}

					// it's time to execute
					queue.poll();
				}

				// run task outside lock
				try {
					task.runnable.run();
				} catch (Throwable t) {
					t.printStackTrace();
				}

				// reschedule
				synchronized (lock) {
					task.nextRunTime = System.currentTimeMillis() + task.periodMillis;
					queue.add(task);
					lock.notifyAll();
				}
			}
		} catch (InterruptedException e) {
			// exit
		} finally {
			schedulerRunning = false;
		}
	}

	// --- data structure ---

	private static class ScheduledTask implements Comparable<ScheduledTask> {
		long nextRunTime;
		final long periodMillis;
		final Runnable runnable;

		public ScheduledTask(long nextRunTime, long periodMillis, Runnable runnable) {
			this.nextRunTime = nextRunTime;
			this.periodMillis = periodMillis;
			this.runnable = runnable;
		}

		@Override
		public int compareTo(ScheduledTask o) {
			return Long.compare(this.nextRunTime, o.nextRunTime);
		}
	}
}
