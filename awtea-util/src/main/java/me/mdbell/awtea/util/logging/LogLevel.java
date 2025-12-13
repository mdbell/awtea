package me.mdbell.awtea.util.logging;

import lombok.Getter;

/**
 * Log levels for the unified logging system.
 * Ordered from most severe to least severe.
 */
public enum LogLevel {
	ERROR(0),
	WARN(1),
	INFO(2),
	DEBUG(3);

	@Getter
	private final int priority;

	LogLevel(int priority) {
		this.priority = priority;
	}

	public boolean isEnabled(LogLevel threshold) {
		return this.priority <= threshold.priority;
	}

	public static LogLevel parse(String name) {
		if (name == null) {
			return INFO;
		}
		for (LogLevel level : values()) {
			if (level.name().equalsIgnoreCase(name)) {
				return level;
			}
		}
		return INFO;
	}
}
