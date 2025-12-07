package me.mdbell.awtea.monitor;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
public final class NetworkMonitor extends AbstractMonitor<NetworkMonitor.Entry, NetworkMonitor.Snapshot> {


	public enum State {
		NEW,
		CONNECTING,
		OPEN,
		CLOSING,
		CLOSED,
		ERROR
	}

	@Getter
	@Setter(AccessLevel.PACKAGE)
	public static final class Entry extends MonitorEntry{
		private String host;
		private int port;
		private String route;
		private State state;

		private long bytesIn;
		private long bytesOut;

		// for rate calculation
		private long prevBytesIn;
		private long prevBytesOut;
		private long prevTimeMillis;
		private double inRate;
		private double outRate;

		private int inBufferSize;
		private int outBufferSize;

		Entry(int id, String label) {
			super(id, label);
			this.state = State.NEW;
			this.prevTimeMillis = System.currentTimeMillis();
		}
	}

	@Getter
	public static final class Snapshot extends MonitorSnapshot<Entry> {
		private final String host;
		private final int port;
		private final String route;   // js5 / game / null
		private final State state;
		private final long bytesIn;
		private final long bytesOut;
		private final double inRateBytesPerSec;
		private final double outRateBytesPerSec;

		private final int inBufferSize;
		private final int outBufferSize;

		public Snapshot(Entry e) {
			super(e);
			this.host = e.host;
			this.port = e.port;
			this.route = e.route;
			this.state = e.state;
			this.bytesIn = e.bytesIn;
			this.bytesOut = e.bytesOut;
			this.inRateBytesPerSec = e.inRate;
			this.outRateBytesPerSec = e.outRate;
			this.outBufferSize = e.outBufferSize;
			this.inBufferSize = e.inBufferSize;
		}
	}

	private static final NetworkMonitor INSTANCE = new NetworkMonitor();

	public static NetworkMonitor get() {
		return INSTANCE;
	}

	private NetworkMonitor() {

	}

	public void register(Object socket, String host, int port, String route) {
		Entry e = ensureEntry(socket);
		e.setHost(host);
		e.setPort(port);
		e.setRoute(route);
	}

	public void onConnecting(Object socket) {
		Entry e = ensureEntry(socket);
		e.setState(State.CONNECTING);
	}

	public void onOpen(Object socket) {
		Entry e = ensureEntry(socket);
		e.setState(State.OPEN);
	}

	public void onClosing(Object socket) {
		Entry e = ensureEntry(socket);
		e.setState(State.CLOSING);
	}

	public void onClosed(Object socket) {
		Entry e = ensureEntry(socket);
		e.setState(State.CLOSED);
	}

	public void onError(Object socket, String message) {
		Entry e = ensureEntry(socket);
		e.setState(State.ERROR);
	}

	public void onUpdateBufferSizes(Object target, int inBuffered, int outBuffered) {
		Entry e = ensureEntry(target);
		e.inBufferSize = inBuffered;
		e.outBufferSize = outBuffered;
	}

	public void onUpdateInBuffer(Object target, int availableBytes) {
		Entry e = ensureEntry(target);
		e.inBufferSize = availableBytes;
	}

	public void onUpdateOutBuffer(Object target, int bufferedBytes) {
		Entry e = ensureEntry(target);
		e.outBufferSize = bufferedBytes;
	}

	public void onBytesIn(Object socket, int len) {
		if (len <= 0) {
			return;
		}
		Entry e = ensureEntry(socket);
		e.bytesIn += len;
	}

	public void onBytesOut(Object socket, int len) {
		if (len <= 0) {
			return;
		}
		Entry e = ensureEntry(socket);
		e.bytesOut += len;
	}

	@Override
	protected Entry createEntry(int id, Object target, String label) {
		return new Entry(id, label);
	}

	@Override
	protected Snapshot buildSnapshot(Entry entry) {
		long now = System.currentTimeMillis();
		long dt = now - entry.prevTimeMillis;
		if (dt <= 0) {
			dt = 1;
		}
		long dIn = entry.bytesIn - entry.prevBytesIn;
		long dOut = entry.bytesOut - entry.prevBytesOut;

		double inRate = (dIn * 1000.0) / dt;
		double outRate = (dOut * 1000.0) / dt;

		// simple EMA to smooth a bit
		double alpha = 0.3;
		entry.inRate = entry.inRate * (1.0 - alpha) + inRate * alpha;
		entry.outRate = entry.outRate * (1.0 - alpha) + outRate * alpha;

		entry.prevBytesIn = entry.bytesIn;
		entry.prevBytesOut = entry.bytesOut;
		entry.prevTimeMillis = now;
		return new Snapshot(entry);
	}
}
