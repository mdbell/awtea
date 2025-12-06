package me.mdbell.awtea.monitor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.mdbell.awtea.detour.NoDetours;

@NoDetours // Prevent detouring of monitor itself - could cause infinite recursion
public final class ThreadMonitor extends AbstractMonitor<ThreadMonitor.Entry, ThreadMonitor.ThreadSnapshot> {

	public enum State {
		NEW,
		STARTED,
		RUNNING,
		SLEEPING,
		TERMINATED
	}

	@Getter
	@Setter(AccessLevel.PACKAGE)
	public static final class Entry extends MonitorEntry {
		private String name;
		private String groupName;
		private boolean daemon;
		private State state;
		private int priority;

		Entry(int id, String label) {
			this(id, label, null, null, 0, false);
		}

		Entry(int id, String label, String name, String groupName, int priority, boolean daemon) {
			super(id, label);
			this.name = name;
			this.groupName = groupName;
			this.daemon = daemon;
			this.priority = priority;
			this.state = State.NEW;
		}
	}

	@Getter
	public static final class ThreadSnapshot extends MonitorSnapshot<Entry> {
		private final int id;
		private final String name;
		private final String groupName;
		private final boolean daemon;
		private final int priority;
		private final State state;

		public ThreadSnapshot(Entry e) {
			super(e);
			this.id = e.getId();
			this.name = e.name;
			this.groupName = e.groupName;
			this.daemon = e.daemon;
			this.priority = e.priority;
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
		if(!(target instanceof Thread)) {
			throw new IllegalArgumentException("ThreadMonitor can only monitor Thread instances");
		}
		Thread thread = (Thread) target;
		String name = thread.getName();
		return name != null ? name : ("Thread-" + id);
	}

	@Override
	protected Entry createEntry(int id, Object target, String label) {
		Thread t = (Thread) target;
		return new Entry(id, label);
	}

	@Override
	protected ThreadSnapshot buildSnapshot(Entry entry) {
		return new ThreadSnapshot(entry);
	}

	public void register(Thread t) {
		Entry e = ensureEntry(t);
		e.setName(t.getName());
		//TeaVM doesn't support ThreadGroup yet
//		ThreadGroup g = t.getThreadGroup();
//		e.setGroupName(g != null ? g.getName() : null);
		e.setDaemon(t.isDaemon());
		e.setPriority(t.getPriority());
	}

	public void onSetPriority(Thread thread, int newPriority) {
		Entry e = ensureEntry(thread);
		e.setPriority(newPriority);
	}

	public void onSetDaemon(Thread thread, boolean on) {
		Entry e = ensureEntry(thread);
		e.setDaemon(on);
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
