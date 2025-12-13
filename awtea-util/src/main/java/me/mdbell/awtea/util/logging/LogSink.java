package me.mdbell.awtea.util.logging;

/**
 * Interface for pluggable log destinations (console, file, UI, etc.)
 */
public interface LogSink {

	/**
	 * Write a log message to this sink
	 * 
	 * @param logger the logger name
	 * @param level the log level
	 * @param message the formatted message
	 * @param throwable optional exception (may be null)
	 */
	void log(String logger, LogLevel level, String message, Throwable throwable);

	/**
	 * Check if this sink accepts logs at the given level
	 */
	boolean accepts(LogLevel level);
}
