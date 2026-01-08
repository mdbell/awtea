package me.mdbell.awtea.monitor;

import lombok.Getter;

@Getter
public abstract class MonitorEntry {

    private final int id;
    private final String label;
    private final long createdMillis;
    private boolean active = true;
    private long lastUpdatedMillis;

    public MonitorEntry(int id, String label) {
        this.id = id;
        this.label = label;
        this.createdMillis = System.currentTimeMillis();
        this.lastUpdatedMillis = this.createdMillis;
    }

    public void setActive(boolean active) {
        this.active = active;
        touch();
    }

    public void touch() {
        this.lastUpdatedMillis = System.currentTimeMillis();
    }
}
