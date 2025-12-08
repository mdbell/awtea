package me.mdbell.awtea.monitor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.mdbell.awtea.instrument.NoDetours;

import java.lang.ref.WeakReference;

@NoDetours // Prevent detouring of monitor itself - could cause infinite recursion
public final class ThreadMonitor extends AbstractMonitor<ThreadMonitor.Entry, ThreadMonitor.Snapshot> {

	public enum State {
		NEW,
		STARTED,
		RUNNING,
		SLEEPING,
		TERMINATED
	}

	public static final class Entry extends MonitorEntry {

		private final WeakReference<Thread> threadRef;
		@Setter(AccessLevel.PACKAGE)
		private State state;

		Entry(int id, String label, Thread t) {
			super(id, label);
			this.state = State.NEW;
			this.threadRef = new WeakReference<>(t);
		}

		public String getName() {
			Thread t = threadRef.get();
			return t != null ? t.getName() : null;
		}

		public String getGroupName() {
			// TeaVM doesn't support ThreadGroup yet
			return null;
		}

		public boolean isDaemon() {
			Thread t = threadRef.get();
			return t != null && t.isDaemon();
		}

		public int getPriority() {
			Thread t = threadRef.get();
			return t != null ? t.getPriority() : Thread.NORM_PRIORITY;
		}
	}

	@Getter
	public static final class Snapshot extends MonitorSnapshot<Entry> {
		private final int id;
		private final String name;
		private final String groupName;
		private final boolean daemon;
		private final int priority;
		private final State state;

		public Snapshot(Entry e) {
			super(e);
			this.id = e.getId();
			this.name = e.getName();
			this.groupName = e.getGroupName();
			this.daemon = e.isDaemon();
			this.priority = e.getPriority();
			this.state = e.state;
		}
	}

	private static final ThreadMonitor INSTANCE = new ThreadMonitor();

	public static ThreadMonitor get() {
		return INSTANCE;
	}

	private ThreadMonitor() {

	}

	@Override
	protected String defaultLabelFor(Object target, int id) {
		if (!(target instanceof Thread)) {
			throw new IllegalArgumentException("ThreadMonitor can only monitor Thread instances");
		}
		Thread thread = (Thread) target;
		String name = thread.getName();
		return name != null ? name : ("Thread-" + id);
	}

	@Override
	protected Entry createEntry(int id, Object target, String label) {
		return new Entry(id, label, (Thread) target);
	}

	@Override
	protected Snapshot buildSnapshot(Entry entry) {
		return new Snapshot(entry);
	}

	public void register(Thread t) {
		Entry e = ensureEntry(t);
		e.setState(State.NEW);
	}

	public void onStart(Thread t) {
		Entry e = ensureEntry(t);
		e.setState(State.STARTED);
	}

	public void onRunEnter(Thread t) {
		Entry e = ensureEntry(t);
		e.setState(State.RUNNING);
	}

	public void onRunExit(Thread t) {
		Entry e = ensureEntry(t);
		e.setState(State.TERMINATED);
	}

	public void onSleep(Thread t) {
		Entry e = ensureEntry(t);
		e.setState(State.SLEEPING);
	}

	public void onWake(Thread t) {
		Entry e = ensureEntry(t);
		e.setState(State.RUNNING);
	}
}
