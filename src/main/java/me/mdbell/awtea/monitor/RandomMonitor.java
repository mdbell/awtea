package me.mdbell.awtea.monitor;

public final class RandomMonitor extends AbstractMonitor<RandomMonitor.State, RandomMonitor.Snapshot> {

	public static final class Snapshot extends MonitorSnapshot<State> {
		public final long seed;

		public final long callsNextInt;
		public final long callsNextIntBound;
		public final long callsNextBoolean;
		public final long callsNextLong;
		public final long callsNextFloat;
		public final long callsNextDouble;
		public final long callsNextGaussian;

		Snapshot(State s) {
			super(s);
			this.seed = s.seed;
			this.callsNextInt = s.callsNextInt;
			this.callsNextIntBound = s.callsNextIntBound;
			this.callsNextBoolean = s.callsNextBoolean;
			this.callsNextLong = s.callsNextLong;
			this.callsNextFloat = s.callsNextFloat;
			this.callsNextDouble = s.callsNextDouble;
			this.callsNextGaussian = s.callsNextGaussian;
		}
	}

	public static final class State extends MonitorEntry{
		long seed;

		long callsNextInt;
		long callsNextIntBound;
		long callsNextBoolean;
		long callsNextLong;
		long callsNextFloat;
		long callsNextDouble;
		long callsNextGaussian;

		State(int id, String label, long seed) {
			super(id, label);
			this.seed = seed;
		}
	}

	private static final RandomMonitor INSTANCE = new RandomMonitor();

	public static RandomMonitor get() {
		return INSTANCE;
	}

	private RandomMonitor() {
	}

	public void register(Object r, long seed) {
		setSeed(r, seed);
	}

	public void setSeed(Object r, long seed) {
		State s = ensureEntry(r);
		s.seed = seed;
	}

	public void onNextInt(Object r) {
		State s = ensureEntry(r);
		s.callsNextInt++;
	}

	public void onNextIntBound(Object r) {
		State s = ensureEntry(r);
		s.callsNextIntBound++;
	}

	public void onNextBoolean(Object r) {
		State s = ensureEntry(r);
		s.callsNextBoolean++;
	}

	public void onNextLong(Object r) {
		State s = ensureEntry(r);
		s.callsNextLong++;
	}

	public void onNextFloat(Object r) {
		State s = ensureEntry(r);
		s.callsNextFloat++;
	}

	public void onNextDouble(Object r) {
		State s = ensureEntry(r);
		s.callsNextDouble++;
	}

	public void onNextGaussian(Object r) {
		State s = ensureEntry(r);
		s.callsNextGaussian++;
	}

	@Override
	protected State createEntry(int id, Object target, String label) {
		return new State(id, label, 0L);
	}

	@Override
	protected Snapshot buildSnapshot(State entry) {
		return new Snapshot(entry);
	}
}
