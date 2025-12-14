/**
 * AUTO-GENERATED FILE - DO NOT EDIT
 * Generated from: schemas/log-level.yaml
 * 
 * Logging levels ordered from most to least severe
 */
package me.mdbell.awtea.util.logging;

public enum LogLevel {
	/** Error messages */
	ERROR(0),
	/** Warning messages */
	WARN(1),
	/** Informational messages */
	INFO(2),
	/** Debug messages */
	DEBUG(3),
	/** Trace messages */
	TRACE(4);

	private final int value;

	LogLevel(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public int getPriority() {
		return value;
	}

	public boolean isEnabled(LogLevel threshold) {
		return this.value <= threshold.value;
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
