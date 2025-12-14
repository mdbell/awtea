package me.mdbell.awtea.util.logging;

import lombok.Getter;

/**
 * Log levels for the unified logging system.
 * Ordered from most severe to least severe.
 * 
 * Note: The numeric values are defined in the generated LogLevelConstants interface,
 * which is synchronized with C and TypeScript via schemas/log-level.yaml
 */
public enum LogLevel implements LogLevelConstants {
    ERROR(LOG_LEVEL_ERROR),
    WARN(LOG_LEVEL_WARN),
    INFO(LOG_LEVEL_INFO),
    DEBUG(LOG_LEVEL_DEBUG),
    TRACE(LOG_LEVEL_TRACE);

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
