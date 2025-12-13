package me.mdbell.awtea.util.logging;

import org.teavm.interop.PlatformMarker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating Logger instances.
 * Supports both native Java and TeaVM environments.
 */
public final class LoggerFactory {

	private static final Map<String, Logger> loggers = new ConcurrentHashMap<>();
	private static final List<LogSink> sinks = new ArrayList<>();
	private static volatile LogLevel globalLevel = LogLevel.INFO;

	static {
		// Initialize with console sink by default
		addSink(new ConsoleLogSink());
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
				logTeaVMError(sink, e);
			} else {
				System.err.println("Error in log sink " + sink.getClass().getName() + ": " + e.getMessage());
			}
		}

		private static native void logTeaVMError(LogSink sink, Exception e);
	}
}
