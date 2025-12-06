package me.mdbell.awtea.monitor;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class ThreadMonitor {

	public enum State {
		NEW,
		STARTED,
		RUNNING,
		SLEEPING,
		TERMINATED
	}

	public static final class ThreadSnapshot {
		public final int id;
		public final String name;
		public final String groupName;
		public final boolean daemon;
		public final State state;
		public final long createdAtMillis;
		public final long lastActivityMillis;

		public ThreadSnapshot(int id,
							  String name,
							  String groupName,
							  boolean daemon,
							  State state,
							  long createdAtMillis,
							  long lastActivityMillis) {
			this.id = id;
			this.name = name;
			this.groupName = groupName;
			this.daemon = daemon;
			this.state = state;
			this.createdAtMillis = createdAtMillis;
			this.lastActivityMillis = lastActivityMillis;
		}
	}

	private static final ThreadMonitor INSTANCE = new ThreadMonitor();

	public static ThreadMonitor get() {
		return INSTANCE;
	}

	private static final class Entry {
		final int id;
		final String name;
		final String groupName;
		final boolean daemon;
		final long createdAtMillis;

		long lastActivityMillis;
		State state;

		Entry(int id, String name, String groupName, boolean daemon) {
			this.id = id;
			this.name = name != null ? name : ("Thread-" + id);
			this.groupName = groupName;
			this.daemon = daemon;
			long now = System.currentTimeMillis();
			this.createdAtMillis = now;
			this.lastActivityMillis = now;
			this.state = State.NEW;
		}
	}

	private final Map<Thread, Entry> entries = new IdentityHashMap<>();
	private int nextId = 1;

	private ThreadMonitor() {
	}

	public void register(Thread t) {
		if (t == null || entries.containsKey(t)) {
			return;
		}
		String name = t.getName();
		//TeaVM does not support ThreadGroup
		// ThreadGroup group = t.getThreadGroup();
		// String groupName = group != null ? group.getName() : null;
		String groupName =  null;

		Entry e = new Entry(nextId++, name, groupName, t.isDaemon());
		entries.put(t, e);
	}

	public void onStart(Thread t) {
		Entry e = entries.get(t);
		if (e == null) {
			register(t);
			e = entries.get(t);
		}
		e.state = State.STARTED;
		e.lastActivityMillis = System.currentTimeMillis();
	}

	public void onRunEnter(Thread t) {
		Entry e = entries.get(t);
		if (e == null) {
			register(t);
			e = entries.get(t);
		}
		e.state = State.RUNNING;
		e.lastActivityMillis = System.currentTimeMillis();
	}

	public void onRunExit(Thread t) {
		Entry e = entries.get(t);
		if (e == null) {
			return;
		}
		e.state = State.TERMINATED;
		e.lastActivityMillis = System.currentTimeMillis();
	}

	public void onSleep(Thread t) {
		Entry e = entries.get(t);
		if (e == null) {
			return;
		}
		e.state = State.SLEEPING;
		e.lastActivityMillis = System.currentTimeMillis();
	}

	public void onWake(Thread t) {
		Entry e = entries.get(t);
		if (e == null) {
			return;
		}
		e.state = State.RUNNING;
		e.lastActivityMillis = System.currentTimeMillis();
	}

	public List<ThreadSnapshot> snapshot() {
		ArrayList<ThreadSnapshot> result = new ArrayList<>(entries.size());
		for (Entry e : entries.values()) {
			result.add(new ThreadSnapshot(
				e.id,
				e.name,
				e.groupName,
				e.daemon,
				e.state,
				e.createdAtMillis,
				e.lastActivityMillis
			));
		}
		result.sort((a, b) -> Integer.compare(a.id, b.id));
		return result;
	}
}
