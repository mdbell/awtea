package me.mdbell.awtea.monitor;

import lombok.Getter;
import lombok.Setter;
import me.mdbell.awtea.util.ThreadUtils;

public class ScheduleTaskMonitor extends AbstractMonitor<ScheduleTaskMonitor.Entry, ScheduleTaskMonitor.Snapshot> 
		implements ThreadUtils.ScheduleTaskListener {

	private static final ScheduleTaskMonitor INSTANCE = new ScheduleTaskMonitor();

	public static ScheduleTaskMonitor get() {
		return INSTANCE;
	}

	private ScheduleTaskMonitor() {
		// Register this monitor with ThreadUtils
		ThreadUtils.setMonitor(this);
	}

	public void onWaiting(Object task) {
		Entry e = ensureEntry(task);
	}

	public void onCreated(Object task, String name, long nextRunTime, long periodMillis) {
		Entry e = ensureEntry(task);
		e.setName(name);
		e.setNextRunTime(nextRunTime);
		e.setPeriodMillis(periodMillis);
	}

	public void onQueued(Object task) {
		Entry e = ensureEntry(task);
	}

	public void onQueued(Object task, long nextRunTime) {
		Entry e = ensureEntry(task);
		e.setNextRunTime(nextRunTime);
	}

	public void onRunning(Object task) {
		Entry e = ensureEntry(task);
		e.setLastRunTime(System.currentTimeMillis());
		e.runCount++;
	}

	public void onCompleted(Object task) {
		Entry e = ensureEntry(task);
		// nothing to do here yet
	}

	@Override
	protected Entry createEntry(int id, Object target, String label) {
		return new Entry(id, label);
	}

	@Override
	protected Snapshot buildSnapshot(Entry entry) {
		return new Snapshot(entry);
	}

	public enum State {
		RUNNING,
		QUEUED,
		WAITING
	}

	@Getter
	@Setter
	public static class Entry extends MonitorEntry {
		private String name;
		private long nextRunTime;
		private long lastRunTime;
		private long runCount;
		private long periodMillis;

		Entry(int id, String label) {
			super(id, label);
		}
	}

	@Getter
	public static class Snapshot extends MonitorSnapshot<Entry> {
		private final String name;
		private final long nextRunTime;
		private final long lastRunTime;
		private final long runCount;
		private final long periodMillis;

		Snapshot(Entry entry) {
			super(entry);
			this.name = entry.getName();
			this.nextRunTime = entry.getNextRunTime();
			this.lastRunTime = entry.getLastRunTime();
			this.runCount = entry.getRunCount();
			this.periodMillis = entry.getPeriodMillis();
		}
	}
}
