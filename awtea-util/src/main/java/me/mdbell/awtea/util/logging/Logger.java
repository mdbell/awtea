package me.mdbell.awtea.util.logging;

/**
 * Logger interface for unified logging across awtea modules.
 * Supports log levels and string formatting.
 */
public interface Logger {

    /**
     * Get the name of this logger
     */
    String getName();

    /**
     * Check if ERROR level is enabled
     */
    boolean isErrorEnabled();

    /**
     * Check if WARN level is enabled
     */
    boolean isWarnEnabled();

    /**
     * Check if INFO level is enabled
     */
    boolean isInfoEnabled();

    /**
     * Check if DEBUG level is enabled
     */
    boolean isDebugEnabled();

    /**
     * Check if TRACE level is enabled
     */
    boolean isTraceEnabled();


    /**
     * Log an ERROR message
     */
    void error(String message);

    /**
     * Log an ERROR message with formatting
     */
    void error(String format, Object... args);

    /**
     * Log an ERROR message with exception
     */
    void error(String message, Throwable t);

    /**
     * Log a WARN message
     */
    void warn(String message);

    /**
     * Log a WARN message with formatting
     */
    void warn(String format, Object... args);

    /**
     * Log a WARN message with exception
     */
    void warn(String message, Throwable t);

    /**
     * Log an INFO message
     */
    void info(String message);

    /**
     * Log an INFO message with formatting
     */
    void info(String format, Object... args);

    /**
     * Log an INFO message with exception
     */
    void info(String message, Throwable t);

    /**
     * Log a DEBUG message
     */
    void debug(String message);

    /**
     * Log a DEBUG message with formatting
     */
    void debug(String format, Object... args);

    /**
     * Log a DEBUG message with exception
     */
    void debug(String message, Throwable t);

    /**
     * Log a TRACE message
     */
    void trace(String message);

    /**
     * Log a TRACE message with formatting
     */
    void trace(String format, Object... args);

    /**
     * Log a TRACE message with exception
     */
    void trace(String message, Throwable t);

    /**
     * Log a message at the specified level
     */
    void log(LogLevel level, String message);

    /**
     * Log a message at the specified level with formatting
     */
    void log(LogLevel level, String format, Object... args);

    /**
     * Log a message at the specified level with exception
     */
    void log(LogLevel level, String message, Throwable t);
}
