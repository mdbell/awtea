package me.mdbell.awtea.monitor;

import lombok.Getter;
import lombok.Setter;

import java.awt.*;

@Getter
public final class EventTypeMonitor
	extends AbstractMonitor<EventTypeMonitor.Entry, EventTypeMonitor.Snapshot> {

	private static final EventTypeMonitor INSTANCE = new EventTypeMonitor();

	public static EventTypeMonitor get() {
		return INSTANCE;
	}

	private EventTypeMonitor() {
	}

	// ---------------------------------------------------------------------
	// Entry & Snapshot
	// ---------------------------------------------------------------------

	@Getter
	@Setter
	public static final class Entry extends MonitorEntry {

		/**
		 * Number of events of this type currently pending (posted - dispatched).
		 */
		private int pending;

		private long totalPosted;
		private long totalDispatched;

		private long lastPostTimeMs;
		private long lastDispatchTimeMs;

		private double postRatePerSec;
		private double dispatchRatePerSec;

		/**
		 * Smoothed average dispatch time in ms (EMA).
		 */
		private double avgDispatchTimeMs;

		/**
		 * Last seen ID for this event type.
		 */
		private int lastEventId;

		Entry(int id, String label) {
			super(id, label);
		}
	}

	@Getter
	public static final class Snapshot extends MonitorSnapshot<Entry> {

		private final int pending;

		private final long totalPosted;
		private final long totalDispatched;

		private final double postRatePerSec;
		private final double dispatchRatePerSec;

		private final double avgDispatchTimeMs;

		private final int lastEventId;

		Snapshot(Entry e) {
			super(e);
			this.pending = e.getPending();
			this.totalPosted = e.getTotalPosted();
			this.totalDispatched = e.getTotalDispatched();
			this.postRatePerSec = e.getPostRatePerSec();
			this.dispatchRatePerSec = e.getDispatchRatePerSec();
			this.avgDispatchTimeMs = e.getAvgDispatchTimeMs();
			this.lastEventId = e.getLastEventId();
		}

		/**
		 * Optional helper: queue "fill" for this event type, relative to some soft max.
		 */
		public int fillPercent(int softMax) {
			if (softMax <= 0) return 0;
			return (int) Math.round(pending * 100.0 / softMax);
		}
	}

	public synchronized void onPost(Object event) {
		if (event == null) return;
		Class<?> type = event.getClass();

		Entry e = ensureEntry(type);
		e.setTotalPosted(e.getTotalPosted() + 1);
		e.setPending(e.getPending() + 1);

		long now = System.currentTimeMillis();
		updateRate(e, now, true);
		e.setLastPostTimeMs(now);
	}

	public synchronized void onDispatch(Object target, long dispatchTimeMs) {
		AWTEvent event = (AWTEvent) target;
		Class<?> type = event.getClass();

		Entry e = ensureEntry(type);
		e.setTotalDispatched(e.getTotalDispatched() + 1);
		if (e.getPending() > 0) {
			e.setPending(e.getPending() - 1);
		}

		long now = System.currentTimeMillis();
		updateRate(e, now, false);
		e.setLastDispatchTimeMs(now);

		// EMA for dispatch time
		if (dispatchTimeMs > 0) {
			double alpha = 0.2;
			double old = e.getAvgDispatchTimeMs();
			double updated = (old == 0.0)
				? dispatchTimeMs
				: old * (1.0 - alpha) + dispatchTimeMs * alpha;
			e.setAvgDispatchTimeMs(updated);
		}

		e.setLastEventId(event.getID());
	}

	private void updateRate(Entry e, long now, boolean isPost) {
		double alpha = 0.2;
		if (isPost) {
			long last = e.getLastPostTimeMs();
			if (last != 0) {
				long dt = now - last;
				if (dt > 0) {
					double inst = 1000.0 / dt;
					double rate = e.getPostRatePerSec();
					e.setPostRatePerSec(rate * (1.0 - alpha) + inst * alpha);
				}
			}
		} else {
			long last = e.getLastDispatchTimeMs();
			if (last != 0) {
				long dt = now - last;
				if (dt > 0) {
					double inst = 1000.0 / dt;
					double rate = e.getDispatchRatePerSec();
					e.setDispatchRatePerSec(rate * (1.0 - alpha) + inst * alpha);
				}
			}
		}
	}

	@Override
	protected Entry createEntry(int id, Object target, String label) {
		// target == event.getClass()
		String name;
		if (target instanceof Class<?>) {
			Class<?> cls = (Class<?>) target;
			name = cls.getSimpleName();
			if (name.isEmpty()) {
				name = cls.getName();
			}
		} else {
			name = label != null ? label : String.valueOf(target);
		}
		return new Entry(id, name);
	}

	@Override
	protected Snapshot buildSnapshot(Entry info) {
		return new Snapshot(info);
	}
}
