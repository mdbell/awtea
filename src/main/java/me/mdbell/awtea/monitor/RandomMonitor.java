package me.mdbell.awtea.monitor;

import java.util.*;

public final class RandomMonitor {

	public static final class Snapshot {
		public final int id;
		public final long seed;
		public final long createdAt;
		public final long lastUse;

		public final long callsNextInt;
		public final long callsNextIntBound;
		public final long callsNextBoolean;
		public final long callsNextLong;
		public final long callsNextFloat;
		public final long callsNextDouble;
		public final long callsNextGaussian;

		Snapshot(State s) {
			this.id = s.id;
			this.seed = s.seed;
			this.createdAt = s.createdAt;
			this.lastUse = s.lastUse;
			this.callsNextInt = s.callsNextInt;
			this.callsNextIntBound = s.callsNextIntBound;
			this.callsNextBoolean = s.callsNextBoolean;
			this.callsNextLong = s.callsNextLong;
			this.callsNextFloat = s.callsNextFloat;
			this.callsNextDouble = s.callsNextDouble;
			this.callsNextGaussian = s.callsNextGaussian;
		}
	}

	private static final class State {
		final int id;
		long seed;
		final long createdAt;
		long lastUse;

		long callsNextInt;
		long callsNextIntBound;
		long callsNextBoolean;
		long callsNextLong;
		long callsNextFloat;
		long callsNextDouble;
		long callsNextGaussian;

		State(int id, long seed) {
			this.id = id;
			this.seed = seed;
			long now = System.currentTimeMillis();
			this.createdAt = now;
			this.lastUse = now;
		}

		void touch() {
			lastUse = System.currentTimeMillis();
		}
	}

	private static final RandomMonitor INSTANCE = new RandomMonitor();

	public static RandomMonitor get() {
		return INSTANCE;
	}

	private final Map<Object, State> states = new WeakHashMap<>();
	private int nextId = 1;

	private RandomMonitor() {
	}

	public synchronized void register(Object r, long seed) {
		if (states.containsKey(r)) {
			return;
		}
		states.put(r, new State(nextId++, seed));
	}

	public synchronized void setSeed(Object r, long seed) {
		State s = states.get(r);
		if (s == null) {
			register(r, seed);
			s = states.get(r);
		}
		s.seed = seed;
		s.touch();
	}

	private synchronized State state(Object r) {
		State s = states.get(r);
		if (s == null) {
			// unknown instance – register with seed=0 as placeholder
			register(r, 0L);
			s = states.get(r);
		}
		s.touch();
		return s;
	}

	public void onNextInt(Object r) {
		State s = state(r);
		s.callsNextInt++;
	}

	public void onNextIntBound(Object r) {
		State s = state(r);
		s.callsNextIntBound++;
	}

	public void onNextBoolean(Object r) {
		State s = state(r);
		s.callsNextBoolean++;
	}

	public void onNextLong(Object r) {
		State s = state(r);
		s.callsNextLong++;
	}

	public void onNextFloat(Object r) {
		State s = state(r);
		s.callsNextFloat++;
	}

	public void onNextDouble(Object r) {
		State s = state(r);
		s.callsNextDouble++;
	}

	public void onNextGaussian(Object r) {
		State s = state(r);
		s.callsNextGaussian++;
	}

	public synchronized List<Snapshot> snapshot() {
		ArrayList<Snapshot> result = new ArrayList<>(states.size());
		for (State s : states.values()) {
			result.add(new Snapshot(s));
		}
		result.sort(Comparator.comparingInt(a -> a.id));
		return result;
	}
}
