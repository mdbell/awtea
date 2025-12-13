package me.mdbell.awtea.util.logging;

import org.teavm.interop.PlatformMarker;
import org.teavm.jso.JSBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating Logger instances.
 * Supports both native Java and TeaVM environments.
 * 
 * Log level can be configured via system property:
 * -Dme.mdbell.awtea.log.level=DEBUG
 */
public final class LoggerFactory {

	/**
	 * System property for configuring the global log level.
	 * Valid values: ERROR, WARN, INFO, DEBUG (case-insensitive)
	 */
	public static final String LOG_LEVEL_PROPERTY = "me.mdbell.awtea.log.level";

	private static final Map<String, Logger> loggers = new ConcurrentHashMap<>();
	private static final List<LogSink> sinks = new ArrayList<>();
	private static volatile LogLevel globalLevel;

	static {
		// Initialize log level from system property or default to INFO
		globalLevel = initLogLevel();
		
		// Initialize with console sink by default
		addSink(new ConsoleLogSink());
	}

	/**
	 * Initialize log level from system property
	 */
	private static LogLevel initLogLevel() {
		String levelProperty = System.getProperty(LOG_LEVEL_PROPERTY);
		if (levelProperty != null && !levelProperty.isEmpty()) {
			try {
				return LogLevel.valueOf(levelProperty.toUpperCase());
			} catch (IllegalArgumentException e) {
				// Invalid level specified, fall back to INFO
				System.err.println("Invalid log level '" + levelProperty + "' specified in " + 
					LOG_LEVEL_PROPERTY + ". Valid values: ERROR, WARN, INFO, DEBUG. Using INFO.");
			}
		}
		return LogLevel.INFO;
	}

	private LoggerFactory() {
	}

	/**
	 * Get or create a logger for the specified class
	 */
	public static Logger getLogger(Class<?> clazz) {
		return getLogger(clazz.getName());
	}

	/**
	 * Get or create a logger with the specified name
	 */
	public static Logger getLogger(String name) {
		return loggers.computeIfAbsent(name, LoggerFactory::createLogger);
	}

	/**
	 * Set the global log level
	 */
	public static void setGlobalLevel(LogLevel level) {
		globalLevel = level;
	}

	/**
	 * Get the current global log level
	 */
	public static LogLevel getGlobalLevel() {
		return globalLevel;
	}

	/**
	 * Add a log sink for capturing logs
	 */
	public static void addSink(LogSink sink) {
		synchronized (sinks) {
			sinks.add(sink);
		}
	}

	/**
	 * Remove a log sink
	 */
	public static void removeSink(LogSink sink) {
		synchronized (sinks) {
			sinks.remove(sink);
		}
	}

	/**
	 * Clear all log sinks
	 */
	public static void clearSinks() {
		synchronized (sinks) {
			sinks.clear();
		}
	}

	/**
	 * Get all registered sinks (defensive copy)
	 */
	static List<LogSink> getSinks() {
		synchronized (sinks) {
			return new ArrayList<>(sinks);
		}
	}

	private static Logger createLogger(String name) {
		return new SinkLogger(name);
	}

	/**
	 * Logger implementation that delegates to registered sinks
	 */
	private static class SinkLogger extends AbstractLogger {

		SinkLogger(String name) {
			super(name);
		}

		@Override
		protected LogLevel getLevel() {
			return globalLevel;
		}

		@Override
		protected void write(LogLevel level, String message, Throwable t) {
			List<LogSink> currentSinks = getSinks();
			for (LogSink sink : currentSinks) {
				if (sink.accepts(level)) {
					try {
						sink.log(name, level, message, t);
					} catch (Exception e) {
						// Prevent logging failures from breaking the application
						handleSinkError(sink, e);
					}
				}
			}
		}

		@PlatformMarker
		private static boolean isTeaVM() {
			return false;
		}

		private void handleSinkError(LogSink sink, Exception e) {
			// In TeaVM, use console.error directly
			if (isTeaVM()) {
				logTeaVMError(sink.getClass().getName(), e.getMessage());
			} else {
				System.err.println("Error in log sink " + sink.getClass().getName() + ": " + e.getMessage());
			}
		}

		@JSBody(params = {"sinkClass", "errorMessage"}, script = 
			"console.error('Error in log sink ' + sinkClass + ': ' + errorMessage);")
		private static native void logTeaVMError(String sinkClass, String errorMessage);
	}
}
