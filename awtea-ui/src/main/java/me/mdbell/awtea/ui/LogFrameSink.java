package me.mdbell.awtea.ui;

import me.mdbell.awtea.util.logging.LogLevel;
import me.mdbell.awtea.util.logging.LogSink;

/**
 * LogSink implementation that routes logs to a LogFrame UI component.
 */
public class LogFrameSink implements LogSink {

	private final LogFrame logFrame;
	private LogLevel threshold = LogLevel.DEBUG;

	public LogFrameSink(LogFrame logFrame) {
		this.logFrame = logFrame;
	}

	public LogFrameSink(LogFrame logFrame, LogLevel threshold) {
		this.logFrame = logFrame;
		this.threshold = threshold;
	}

	@Override
	public boolean accepts(LogLevel level) {
		return level.isEnabled(threshold);
	}

	@Override
	public void log(String logger, LogLevel level, String message, Throwable throwable) {
		String fullMessage = message;
		if (throwable != null) {
			fullMessage = message + " - " + throwable.toString();
		}

		// Route to LogFrame via its internal appendLine method
		// Note: This is called from logging framework, so we need to ensure
		// it's done on the proper thread/context
		if (logFrame != null) {
			final String msg = fullMessage;
			logFrame.schedule(() -> logFrame.appendLine(level.name().toLowerCase(), msg));
		}
	}
}
