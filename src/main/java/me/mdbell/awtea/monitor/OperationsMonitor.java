package me.mdbell.awtea.monitor;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generic monitor for tracking operations on objects.
 * (e.g., method entry/exit tracking)
 */
@Getter
public class OperationsMonitor extends AbstractMonitor<OperationsMonitor.Entry, OperationsMonitor.Snapshot> {

	// Note: OperationMonitor is not a singleton - multiple instances can be created as needed.
	private static final WeakHashMap<Object, OperationsMonitor> monitors = new WeakHashMap<>();

	private static final AtomicInteger monitorIdGenerator = new AtomicInteger(0);

	private final String name;

	private final int id;

	private OperationsMonitor(String name, boolean weak) {
		super(weak);
		this.name = name;
		this.id = monitorIdGenerator.incrementAndGet();
	}

	public static Collection<OperationsMonitor> monitors() {
		return monitors.values();
	}

	public static synchronized OperationsMonitor get(Object baseTarget, String name) {
		return get(baseTarget, name, true);
	}

	public static synchronized OperationsMonitor get(Object baseTarget, String name, boolean weak) {
		return monitors.computeIfAbsent(baseTarget, k -> new OperationsMonitor(name, weak));
	}

	@Override
	protected Entry createEntry(int id, Object target, String label) {
		return new Entry(id, label);
	}

	public void onOperationEntered(Object target, String operationName) {
		Entry entry = ensureEntry(target);

		Operation op = entry.getOperations().stream()
			.filter(o -> o.getName().equals(operationName))
			.findFirst()
			.orElseGet(() -> {
				Operation newOp = new Operation();
				newOp.setName(operationName);
				entry.getOperations().add(newOp);
				return newOp;
			});

		op.invocationCount++;
		op.setLastEntryTimeMs(System.currentTimeMillis());
	}

	public void onOperationLeft(Object target, String operationName) {
		Entry entry = ensureEntry(target);

		Operation op = entry.getOperations().stream()
			.filter(o -> o.getName().equals(operationName))
			.findFirst()
			.orElse(null);

		if (op != null) {
			long exitTimeMs = System.currentTimeMillis();
			long durationMs = exitTimeMs - op.getLastEntryTimeMs();
			op.totalTimeMs += durationMs;
			op.avgTimeMs = (double) op.totalTimeMs / op.invocationCount;
			op.setLastExitTimeMs(exitTimeMs);
		}
	}

	@Override
	protected Snapshot buildSnapshot(Entry entry) {
		return new Snapshot(entry);
	}

	@Getter
	@Setter
	public static class Operation {
		private String name;
		private long invocationCount;
		private long totalTimeMs;
		private double avgTimeMs;
		private long lastEntryTimeMs;
		private long lastExitTimeMs;
	}

	@Getter
	public static class Entry extends MonitorEntry {
		private final List<Operation> operations;

		public Entry(int id, String label) {
			super(id, label);
			this.operations = new ArrayList<>();
		}
	}

	@Getter
	public static class Snapshot extends MonitorSnapshot<Entry> {
		private final Operation[] operations;

		public Snapshot(Entry e) {
			super(e);
			this.operations = e.getOperations()
				.stream()
				.map(op -> {
					Operation opSnap = new Operation();
					opSnap.setName(op.getName());
					opSnap.setInvocationCount(op.getInvocationCount());
					opSnap.setTotalTimeMs(op.getTotalTimeMs());
					opSnap.setAvgTimeMs(op.getAvgTimeMs());
					opSnap.setLastEntryTimeMs(op.getLastEntryTimeMs());
					opSnap.setLastExitTimeMs(op.getLastExitTimeMs());
					return opSnap;
				})
				.toArray(Operation[]::new);
		}
	}
}
