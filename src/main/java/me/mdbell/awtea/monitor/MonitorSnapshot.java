package me.mdbell.awtea.monitor;

import lombok.Getter;

@Getter
public abstract class MonitorSnapshot<E extends MonitorEntry> {

	/** Stable numeric ID for the tracked object. */
	protected final int id;

	/** Optional human-readable label (name, class@hash, etc.). */
	protected final String label;

	/** Whether the object is considered active by the monitor. */
	protected final boolean active;

	/** When this object was first registered (millis since epoch). */
	protected final long createdMillis;

	/** When this entry was last updated / touched (millis since epoch). */
	protected final long lastUpdatedMillis;

	protected MonitorSnapshot(E entry) {
		this.id = entry.getId();
		this.label = entry.getLabel();
		this.active = entry.isActive();
		this.createdMillis = entry.getCreatedMillis();
		this.lastUpdatedMillis = entry.getLastUpdatedMillis();
	}

	protected MonitorSnapshot(int id,
							  String label,
							  boolean active,
							  long createdMillis,
							  long lastUpdatedMillis) {
		this.id = id;
		this.label = label;
		this.active = active;
		this.createdMillis = createdMillis;
		this.lastUpdatedMillis = lastUpdatedMillis;
	}
}
