package me.mdbell.awtea.monitor;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract base class for monitors.
 * <p>
 * Monitors track monitor entries and provide snapshots of their state.
 * <p>
 * At first glance Entries and Snapshots may seem redundant, but they serve different purposes:
 * - Entries represent live objects being monitored, maintaining state and allowing updates.
 * - Snapshots are immutable representations of an entry's state at a specific point in time,
 *  useful for reporting and analysis without affecting the live state.
 *  In addition, we can in theory have different snapshot types for the same entry type,
 *  e.g a lightweight snapshot for frequent polling, and a detailed snapshot for in-depth analysis.
 *
 * @param <E> the type of monitor entries
 * @param <S> the type of monitor snapshots
 */
public abstract class AbstractMonitor<E extends MonitorEntry, S extends MonitorSnapshot<E>> {

	private final WeakHashMap<Object, E> entries = new WeakHashMap<>();

	private final AtomicInteger nextId = new AtomicInteger(1);

	@Getter
	private long revision = 0;

	/**
	 * Ensure that a monitor entry exists for the given target object.
	 * If an entry already exists, it is returned after updating its last touched time.
	 * If no entry exists, a new one is created with a default label,
	 * registered, and returned.
	 * @param target the target object to monitor
	 * @return the existing or newly created monitor entry
	 */
	protected final  E ensureEntry(Object target) {
		return ensureEntry(target, null);
	}

	/**
	 * Ensure that a monitor entry exists for the given target object.
	 * If an entry already exists, it is returned after updating its last touched time.
	 * If no entry exists, a new one is created with the provided label (or a default label if null),
	 * registered, and returned.
	 * @param target the target object to monitor
	 * @param label an optional label for the entry; if null, a default label is generated
	 * @return the existing or newly created monitor entry
	 */
	protected synchronized E ensureEntry(Object target, String label) {
		E entry = entries.get(target);
		if (entry != null) {
			entry.touch();
			return entry;
		}
		int id = nextId.getAndIncrement();

		if(label == null) {
			label = defaultLabelFor(target, id);
		}

		entry = createEntry(id, target, label);
		entries.put(target, entry);
		revision++;
		return entry;
	}

	/**
	 * Generate a default label for the given target object and ID.
	 *
	 * @param target the target object
	 * @param id     the assigned ID
	 * @return a default label in the format "ClassName@hashcode (#id)"
	 */
	protected String defaultLabelFor(Object target, int id) {
		return target.getClass().getName()
			+ "@"
			+ Integer.toHexString(System.identityHashCode(target))
			+ " (#" + id + ")";
	}

	/**
	 * Mark the monitor entry for the given target object as active.
	 *
	 * @param target the target object to mark as active
	 */
	public synchronized void markInactive(Object target) {
		E entry = entries.get(target);
		if (entry != null && entry.isActive()) {
			entry.setActive(false);
			bumpRevision();
		}
	}

	/**
	 * Unregister the monitor entry for the given target object.
	 *
	 * @param target the target object to unregister
	 */
	public synchronized void unregister(Object target) {
		E entry = entries.remove(target);
		if (entry != null) {
			bumpRevision();
		}
	}

	/**
	 * Update the last touched time of the monitor entry for the given target object.
	 *
	 * @param target the target object whose monitor entry should be touched
	 */
	public synchronized void touch(Object target) {
		E entry = entries.get(target);
		if (entry != null) {
			touch(entry);
		}
	}
	/**
	 * Update the last touched time of the given monitor entry and bump the revision.
	 *
	 * @param entry the monitor entry to touch
	 */
	protected void touch(E entry) {
		entry.touch();
		bumpRevision();
	}

	/**
	 * Get the monitor entry for the given target object.
	 * Note: this value is _mutable_ and will reflect in future snapshots. This is
	 * ideally only used if a monitor intends to mutate the observed object directly.
	 *
	 * @param target the target object
	 * @return the monitor entry, or null if none exists
	 */
	public synchronized E getEntry(Object target) {
		return entries.get(target);
	}

	/**
	 * Create a snapshot of all current monitor entries without converting to snapshot form.
	 *
	 * @return a list of the current monitor entries
	 */
	public synchronized List<E> snapshotEntries() {
		return new ArrayList<>(entries.values());
	}

	/**
	 * Increment the monitor's revision number to indicate a state change.
	 */
	protected void bumpRevision() {
		revision++;
	}

	/**
	 * Create a snapshot of all current monitor entries.
	 *
	 * @return a list of snapshots representing the current state of all monitor entries
	 */
	public synchronized List<S> snapshot() {
		ArrayList<S> out = new ArrayList<>();
		for (E entry : entries.values()) {
			S snap = buildSnapshot(entry);
			if (snap != null) {
				out.add(snap);
			}
		}

		// Sort by ID ascending - mostly for UI consistency
		out.sort((a, b) -> Integer.compare(a.getId(), b.getId()));

		return out;
	}

	/**
	 * Create a new monitor entry for the given target object.
	 *
	 * @param id the assigned ID
	 * @param target the target object
	 * @param label the label for the entry
	 * @return the newly created monitor entry
	 */
	protected abstract E createEntry(int id, Object target, String label);

	protected abstract S buildSnapshot(E entry);
}
