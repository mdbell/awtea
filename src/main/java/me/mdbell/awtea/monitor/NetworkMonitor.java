package me.mdbell.awtea.monitor;

import java.util.*;


public final class NetworkMonitor {

	public enum State {
		CONNECTING,
		OPEN,
		CLOSING,
		CLOSED,
		ERROR
	}

	public static final class ConnectionSnapshot {
		public final int id;
		public final String host;
		public final int port;
		public final String route;   // js5 / game / null
		public final State state;
		public final String stateText;
		public final long createdAtMillis;
		public final long lastActivityMillis;
		public final long bytesIn;
		public final long bytesOut;
		public final double inRateBytesPerSec;
		public final double outRateBytesPerSec;

		public ConnectionSnapshot(int id,
								  String host,
								  int port,
								  String route,
								  State state,
								  String stateText,
								  long createdAtMillis,
								  long lastActivityMillis,
								  long bytesIn,
								  long bytesOut,
								  double inRateBytesPerSec,
								  double outRateBytesPerSec) {
			this.id = id;
			this.host = host;
			this.port = port;
			this.route = route;
			this.state = state;
			this.stateText = stateText;
			this.createdAtMillis = createdAtMillis;
			this.lastActivityMillis = lastActivityMillis;
			this.bytesIn = bytesIn;
			this.bytesOut = bytesOut;
			this.inRateBytesPerSec = inRateBytesPerSec;
			this.outRateBytesPerSec = outRateBytesPerSec;
		}
	}

	private static final NetworkMonitor INSTANCE = new NetworkMonitor();

	public static NetworkMonitor get() {
		return INSTANCE;
	}

	private static final class Entry {
		final int id;
		final String host;
		final int port;
		final String route;
		State state;
		String stateText;
		long createdAtMillis;
		long lastActivityMillis;

		long bytesIn;
		long bytesOut;

		// for rate calculation
		long prevBytesIn;
		long prevBytesOut;
		long prevTimeMillis;
		double inRate;
		double outRate;

		Entry(int id, String host, int port, String route) {
			this.id = id;
			this.host = host;
			this.port = port;
			this.route = route;
			this.state = State.CONNECTING;
			this.stateText = "connecting";
			long now = System.currentTimeMillis();
			this.createdAtMillis = now;
			this.lastActivityMillis = now;
			this.prevTimeMillis = now;
		}
	}

	private final Map<Object, Entry> entries = new WeakHashMap<>();
	private int nextId = 1;

	private NetworkMonitor() {

	}

	public void register(Object socket, String host, int port, String route) {
		Entry e = new Entry(nextId++, host, port, route);
		entries.put(socket, e);
	}

	public void onOpen(Object socket) {
		Entry e = entries.get(socket);
		if (e == null) return;
		e.state = State.OPEN;
		e.stateText = "open";
		e.lastActivityMillis = System.currentTimeMillis();
	}

	public void onClosing(Object socket) {
		Entry e = entries.get(socket);
		if (e == null) return;
		e.state = State.CLOSING;
		e.stateText = "closing";
		e.lastActivityMillis = System.currentTimeMillis();
	}

	public void onClosed(Object socket) {
		Entry e = entries.get(socket);
		if (e == null) return;
		e.state = State.CLOSED;
		e.stateText = "closed";
		e.lastActivityMillis = System.currentTimeMillis();
	}

	public void onError(Object socket, String message) {
		Entry e = entries.get(socket);
		if (e == null) return;
		e.state = State.ERROR;
		e.stateText = message != null ? message : "error";
		e.lastActivityMillis = System.currentTimeMillis();
	}

	public void onBytesIn(Object socket, int len) {
		if (len <= 0) return;
		Entry e = entries.get(socket);
		if (e == null) return;
		e.bytesIn += len;
		e.lastActivityMillis = System.currentTimeMillis();
	}

	public void onBytesOut(Object socket, int len) {
		if (len <= 0) return;
		Entry e = entries.get(socket);
		if (e == null) return;
		e.bytesOut += len;
		e.lastActivityMillis = System.currentTimeMillis();
	}

	public List<ConnectionSnapshot> snapshot() {
		long now = System.currentTimeMillis();
		List<ConnectionSnapshot> list = new ArrayList<>(entries.size());

		for (Entry e : entries.values()) {
			long dt = now - e.prevTimeMillis;
			if (dt <= 0) {
				dt = 1;
			}
			long dIn = e.bytesIn - e.prevBytesIn;
			long dOut = e.bytesOut - e.prevBytesOut;

			double inRate = (dIn * 1000.0) / dt;
			double outRate = (dOut * 1000.0) / dt;

			// simple EMA to smooth a bit
			double alpha = 0.3;
			e.inRate = e.inRate * (1.0 - alpha) + inRate * alpha;
			e.outRate = e.outRate * (1.0 - alpha) + outRate * alpha;

			e.prevBytesIn = e.bytesIn;
			e.prevBytesOut = e.bytesOut;
			e.prevTimeMillis = now;

			list.add(new ConnectionSnapshot(
				e.id,
				e.host,
				e.port,
				e.route,
				e.state,
				e.stateText,
				e.createdAtMillis,
				e.lastActivityMillis,
				e.bytesIn,
				e.bytesOut,
				e.inRate,
				e.outRate
			));
		}

		return list;
	}
}
