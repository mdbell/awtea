package me.mdbell.awtea.util.logging;

import org.teavm.interop.PlatformMarker;
import org.teavm.jso.JSBody;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Console-based log sink that writes to System.out/System.err.
 * In TeaVM, this routes to browser console with appropriate levels.
 */
public class ConsoleLogSink implements LogSink {

	private LogLevel threshold = LogLevel.TRACE;

	public ConsoleLogSink() {
	}

	public ConsoleLogSink(LogLevel threshold) {
		this.threshold = threshold;
	}

	@Override
	public boolean accepts(LogLevel level) {
		return level.isEnabled(threshold);
	}

	@Override
	public void log(String logger, LogLevel level, String message, Throwable throwable) {
		String formattedMessage = formatMessage(logger, level, message);

		if (isTeaVM()) {
			logToTeaVMConsole(level, formattedMessage, throwable);
		} else {
			logToJavaConsole(level, formattedMessage, throwable);
		}
	}

	@PlatformMarker
	private static boolean isTeaVM() {
		return false;
	}

	private String formatMessage(String logger, LogLevel level, String message) {
		// Extract simple class name for brevity
		String simpleName = logger;
		int lastDot = logger.lastIndexOf('.');
		if (lastDot >= 0 && lastDot < logger.length() - 1) {
			simpleName = logger.substring(lastDot + 1);
		}

		return String.format("[%s] %s - %s", level.name(), simpleName, message);
	}

	private void logToJavaConsole(LogLevel level, String message, Throwable throwable) {
		PrintStream out = (level == LogLevel.ERROR || level == LogLevel.WARN) ? System.err : System.out;
		out.println(message);
		if (throwable != null) {
			throwable.printStackTrace(out);
		}
	}

	private void logToTeaVMConsole(LogLevel level, String message, Throwable throwable) {
		String fullMessage = message;
		if (throwable != null) {
			fullMessage = message + "\n" + getStackTrace(throwable);
		}

		switch (level) {
			case ERROR:
				consoleError(fullMessage);
				break;
			case WARN:
				consoleWarn(fullMessage);
				break;
			case TRACE:
			case INFO:
				consoleInfo(fullMessage);
				break;
			case DEBUG:
				consoleDebug(fullMessage);
				break;
		}
	}

	private static String getStackTrace(Throwable t) {
		try {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			return sw.toString();
		} catch (Exception e) {
			return t.toString();
		}
	}

	@JSBody(params = "message", script = "console.error(message);")
	private static native void consoleError(String message);

	@JSBody(params = "message", script = "console.warn(message);")
	private static native void consoleWarn(String message);

	@JSBody(params = "message", script = "console.info(message);")
	private static native void consoleInfo(String message);

	@JSBody(params = "message", script = "console.log(message);")
	private static native void consoleLog(String message);

	@JSBody(params = "message", script = "console.debug(message);")
	private static native void consoleDebug(String message);
}
