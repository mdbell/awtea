package me.mdbell.awtea.util.logging;

import lombok.Getter;

/**
 * Base implementation of Logger with formatting support
 */
public abstract class AbstractLogger implements Logger {

	@Getter
	protected final String name;

	protected AbstractLogger(String name) {
		this.name = name;
	}

	protected abstract LogLevel getLevel();

	protected abstract void write(LogLevel level, String message, Throwable t);

	@Override
	public boolean isErrorEnabled() {
		return LogLevel.ERROR.isEnabled(getLevel());
	}

	@Override
	public boolean isWarnEnabled() {
		return LogLevel.WARN.isEnabled(getLevel());
	}

	@Override
	public boolean isInfoEnabled() {
		return LogLevel.INFO.isEnabled(getLevel());
	}

	@Override
	public boolean isDebugEnabled() {
		return LogLevel.DEBUG.isEnabled(getLevel());
	}

	@Override
	public void error(String message) {
		if (isErrorEnabled()) {
			write(LogLevel.ERROR, message, null);
		}
	}

	@Override
	public void error(String format, Object... args) {
		if (isErrorEnabled()) {
			write(LogLevel.ERROR, format(format, args), null);
		}
	}

	@Override
	public void error(String message, Throwable t) {
		if (isErrorEnabled()) {
			write(LogLevel.ERROR, message, t);
		}
	}

	@Override
	public void warn(String message) {
		if (isWarnEnabled()) {
			write(LogLevel.WARN, message, null);
		}
	}

	@Override
	public void warn(String format, Object... args) {
		if (isWarnEnabled()) {
			write(LogLevel.WARN, format(format, args), null);
		}
	}

	@Override
	public void warn(String message, Throwable t) {
		if (isWarnEnabled()) {
			write(LogLevel.WARN, message, t);
		}
	}

	@Override
	public void info(String message) {
		if (isInfoEnabled()) {
			write(LogLevel.INFO, message, null);
		}
	}

	@Override
	public void info(String format, Object... args) {
		if (isInfoEnabled()) {
			write(LogLevel.INFO, format(format, args), null);
		}
	}

	@Override
	public void info(String message, Throwable t) {
		if (isInfoEnabled()) {
			write(LogLevel.INFO, message, t);
		}
	}

	@Override
	public void debug(String message) {
		if (isDebugEnabled()) {
			write(LogLevel.DEBUG, message, null);
		}
	}

	@Override
	public void debug(String format, Object... args) {
		if (isDebugEnabled()) {
			write(LogLevel.DEBUG, format(format, args), null);
		}
	}

	@Override
	public void debug(String message, Throwable t) {
		if (isDebugEnabled()) {
			write(LogLevel.DEBUG, message, t);
		}
	}

	@Override
	public void log(LogLevel level, String message) {
		if (level.isEnabled(getLevel())) {
			write(level, message, null);
		}
	}

	@Override
	public void log(LogLevel level, String format, Object... args) {
		if (level.isEnabled(getLevel())) {
			write(level, format(format, args), null);
		}
	}

	@Override
	public void log(LogLevel level, String message, Throwable t) {
		if (level.isEnabled(getLevel())) {
			write(level, message, t);
		}
	}

	/**
	 * SLF4J-style string interpolation using {} placeholders
	 */
	protected String format(String format, Object... args) {
		if (args == null || args.length == 0) {
			return format;
		}
		
		StringBuilder result = new StringBuilder();
		int argIndex = 0;
		int i = 0;
		
		while (i < format.length()) {
			// Look for {} placeholder
			if (i < format.length() - 1 && format.charAt(i) == '{' && format.charAt(i + 1) == '}') {
				// Found a placeholder
				if (argIndex < args.length) {
					result.append(args[argIndex]);
					argIndex++;
				} else {
					// No more arguments, keep the placeholder
					result.append("{}");
				}
				i += 2; // Skip past {}
			} else if (i < format.length() - 1 && format.charAt(i) == '\\' && format.charAt(i + 1) == '{') {
				// Escaped brace: \{ -> {
				result.append('{');
				i += 2;
			} else {
				// Regular character
				result.append(format.charAt(i));
				i++;
			}
		}
		
		return result.toString();
	}
}
