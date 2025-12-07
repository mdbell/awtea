package me.mdbell.awtea.util;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import me.mdbell.awtea.monitor.ScheduleTaskMonitor;

import java.util.PriorityQueue;

@UtilityClass
public class ThreadUtils {

	// --- scheduler state ---

	private final Object lock = new Object();

	private final PriorityQueue<ScheduledTask> queue = new PriorityQueue<>();

	private volatile boolean schedulerRunning = false;

	public void runAtFixedRate(String name, Runnable runnable, long periodMillis) {
		synchronized (lock) {
			if (!schedulerRunning) {
				startSchedulerThread();
			}

			long now = System.currentTimeMillis();
			long nextRun = now + periodMillis;

			ScheduledTask task = new ScheduledTask(name, nextRun, periodMillis, runnable);
			ScheduleTaskMonitor.get().onCreated(task, name, nextRun, periodMillis);
			queue.add(task);

			lock.notifyAll();
		}
	}

	public void runOnce(String name, Runnable runnable, long delayMillis) {
		synchronized (lock) {
			if (!schedulerRunning) {
				startSchedulerThread();
			}

			long now = System.currentTimeMillis();
			long nextRun = now + delayMillis;

			ScheduledTask task = new ScheduledTask(name, nextRun, runnable);
			ScheduleTaskMonitor.get().onCreated(task, name, nextRun, 0);
			queue.add(task);

			lock.notifyAll();
		}
	}

	private void startSchedulerThread() {
		schedulerRunning = true;
		Thread schedulerThread = new Thread(ThreadUtils::pump, "AWTea-ThreadUtils-Scheduler");
		schedulerThread.setPriority(5); // normal priority
		schedulerThread.setDaemon(true);
		schedulerThread.start();
	}

	private void pump() {
		try {
			while (schedulerRunning) {
				ScheduledTask task;

				synchronized (lock) {
					task = queue.peek();

					if (task == null) {
						lock.wait(); // wait for tasks
						continue;
					}

					long now = System.currentTimeMillis();

					long delay = task.nextRunTime - now;
					if (delay > 0) {
						ScheduleTaskMonitor.get().onWaiting(task);
						lock.wait(delay); // sleep until the earliest task is due
						ScheduleTaskMonitor.get().onQueued(task);
						continue;
					}

					// it's time to execute
					queue.poll();
				}

				// run task outside lock
				try {
					ScheduleTaskMonitor.get().onRunning(task);
					task.runnable.run();
				} catch (Throwable t) {
					t.printStackTrace();
				}

				if (task.type == TaskType.ONESHOT) {
					ScheduleTaskMonitor.get().onCompleted(task);
					continue; // do not reschedule
				}

				// reschedule
				synchronized (lock) {
					task.nextRunTime = System.currentTimeMillis() + task.periodMillis;
					ScheduleTaskMonitor.get().onQueued(task, task.nextRunTime);
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

	public enum TaskType {
		ONESHOT,
		FIXED_RATE
	}

	@Getter
	public static class ScheduledTask implements Comparable<ScheduledTask> {
		private final String name;
		long nextRunTime;
		final long periodMillis;
		final Runnable runnable;
		final TaskType type;

		public ScheduledTask(String name, long nextRunTime, Runnable runnable) {
			this(name, nextRunTime, 0, runnable);
		}

		public ScheduledTask(String name, long nextRunTime, long periodMillis, Runnable runnable) {
			this.name = name;
			this.nextRunTime = nextRunTime;
			this.periodMillis = periodMillis;
			this.runnable = runnable;
			this.type = periodMillis > 0 ? TaskType.FIXED_RATE : TaskType.ONESHOT;
		}

		@Override
		public int compareTo(ScheduledTask o) {
			return Long.compare(this.nextRunTime, o.nextRunTime);
		}
	}
}
