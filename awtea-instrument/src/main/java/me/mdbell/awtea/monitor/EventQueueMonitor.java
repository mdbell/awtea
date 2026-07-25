package me.mdbell.awtea.monitor;

import lombok.Getter;
import lombok.Setter;

import java.awt.*;

@Getter
public final class EventQueueMonitor extends AbstractMonitor<EventQueueMonitor.Entry,
	EventQueueMonitor.Snapshot> {

	private static final EventQueueMonitor INSTANCE = new EventQueueMonitor();

	public static EventQueueMonitor get() {
		return INSTANCE;
	}

	private EventQueueMonitor() {
	}

	@Getter
	@Setter
	public static final class Entry extends MonitorEntry {

		// per-priority pending counts
		private int[] pendingPerPriority = new int[4];
		private int totalPending;

		// event counters
		private long totalPosted;
		private long totalDispatched;

		// timestamps
		private long lastPostTimeMs;
		private long lastDispatchTimeMs;

		// smoothed rates
		private double postRatePerSec;
		private double dispatchRatePerSec;

		// dispatch timing (EMA)
		private double avgDispatchTimeMs;

		// last dispatched event info
		private String lastEventClass;
		private int lastEventId;

		Entry(int id, String label) {
			super(id, label);
		}
	}

	@Getter
	public static final class Snapshot extends MonitorSnapshot<Entry> {

		private final int[] pendingPerPriority;
		private final int totalPending;

		private final long totalPosted;
		private final long totalDispatched;

		private final double postRatePerSec;
		private final double dispatchRatePerSec;
		private final double avgDispatchTimeMs;

		private final String lastEventClass;
		private final int lastEventId;

		Snapshot(Entry e) {
			super(e);
			this.pendingPerPriority = e.pendingPerPriority.clone();
			this.totalPending = e.totalPending;
			this.totalPosted = e.totalPosted;
			this.totalDispatched = e.totalDispatched;
			this.postRatePerSec = e.postRatePerSec;
			this.dispatchRatePerSec = e.dispatchRatePerSec;
			this.avgDispatchTimeMs = e.avgDispatchTimeMs;
			this.lastEventClass = e.lastEventClass;
			this.lastEventId = e.lastEventId;
		}

		public int fillPercent(int softMax) {
			if (softMax <= 0) return 0;
			return (int) Math.round(totalPending * 100.0 / softMax);
		}
	}

	// ---- monitor API used from TEventQueue ----

	public void onPost(Object queue, int priority, int[] pendingPerPriority) {
		Entry e = ensureEntry(queue);
		e.totalPosted++;

		long now = System.currentTimeMillis();
		updateRate(now, e, true);

		updatePending(e, pendingPerPriority);
	}

	public void onDispatch(Object queue, int[] pendingPerPriority,
										long dispatchTimeMs, Object eventObj) {
		Entry e = ensureEntry(queue);
		e.totalDispatched++;

		AWTEvent event = (AWTEvent) eventObj;

		long now = System.currentTimeMillis();
		updateRate(now, e, false);

		// EMA for dispatch time
		double alpha = 0.2;
		e.avgDispatchTimeMs = dispatchTimeMs <= 0
			? e.avgDispatchTimeMs
			: e.avgDispatchTimeMs * (1.0 - alpha) + dispatchTimeMs * alpha;

		updatePending(e, pendingPerPriority);

		if (event != null) {
			e.lastEventClass = event.getClass().getSimpleName();
			e.lastEventId = event.getID();
		}
	}

	private void updatePending(Entry e, int[] pendingPerPriority) {
		e.pendingPerPriority = pendingPerPriority.clone();
		int total = 0;
		for (int c : pendingPerPriority) total += c;
		e.totalPending = total;
	}

	private void updateRate(long now, Entry e, boolean isPost) {
		double alpha = 0.2;
		if (isPost) {
			if (e.getLastPostTimeMs() != 0) {
				long dt = now - e.getLastPostTimeMs();
				if (dt > 0) {
					double inst = 1000.0 / dt;
					e.postRatePerSec = e.postRatePerSec * (1.0 - alpha) + inst * alpha;
				}
			}
			e.setLastPostTimeMs(now);
		} else {
			if (e.getLastDispatchTimeMs() != 0) {
				long dt = now - e.getLastDispatchTimeMs();
				if (dt > 0) {
					double inst = 1000.0 / dt;
					e.dispatchRatePerSec = e.dispatchRatePerSec * (1.0 - alpha) + inst * alpha;
				}
			}
			e.setLastDispatchTimeMs(now);
		}
	}

	@Override
	protected Entry createEntry(int id, Object target, String label) {
		// Only one queue typically; label can be something like "AWT-EventQueue"
		return new Entry(id, label != null ? label : "EventQueue");
	}

	@Override
	protected Snapshot buildSnapshot(Entry info) {
		return new Snapshot(info);
	}
}
