package me.mdbell.awtea.util.ui;

import lombok.*;
import me.mdbell.awtea.util.ConsoleBridge;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.ArrayList;
import java.util.List;

public class LogWindow extends FloatingWindow {

	private final List<LogEntry> lines = new ArrayList<>();
	private static final int MAX_LINES = 500;

	private HTMLElement body;
	private HTMLElement pre;

	static {
		AwCss.sheet()
			.createClass("log-viewer-list")
			.prop("list-style-type", "none")
			.prop("padding", "0")
			.prop("margin", "0")
			.createClass("log-entry")
			.prop("font-family", "monospace")
			.prop("white-space", "pre-wrap")
			.prop("margin", "0")
			.createClass("log-entry-error")
			.prop("color", Theme.Var.ERROR_FOREGROUND)
			.before()
			.prop("content", "'[ERR]'")
			.prop("font-weight", "bold")
			.createClass("log-entry-warn")
			.prop("color", Theme.Var.WARNING_FOREGROUND)
			.before()
			.prop("content", "'[WRN]'")
			.prop("font-weight", "bold")
			.createClass("log-entry-info")
			.prop("color", Theme.Var.INFO_FOREGROUND)
			.before()
			.prop("content", "'[INF]'")
			.prop("font-weight", "bold")
			.createClass("log-entry-debug")
			.prop("color", Theme.Var.DEBUG_FOREGROUND)
			.before()
			.prop("content", "'[DBG]'")
			.prop("font-weight", "bold")
			.end()
			.inject();
	}

	public LogWindow() {
		super("logviewer", "Log Viewer", 800, 400, 0); // no timer needed
		setAutoscroll(true);

		// Install console hook
		ConsoleBridge.install((level, msg) -> {
			// Make sure UI changes happen via schedule(), like in FsViewWindow
			schedule(() -> appendLine(level, msg));
		});
	}

	private void appendLine(String level, String msg) {
		LogLevel logLevel = LogLevel.parse(level);
		LogEntry entry = new LogEntry(logLevel, msg);
		lines.add(entry);
		if (lines.size() > MAX_LINES) {
			LogEntry e = lines.remove(0);
			HTMLElement element = e.getElement();
			if (element != null && element.getParentNode() != null) {
				element.getParentNode().removeChild(element);
			}
		}
		// Re-render content based on new log state
		render();
	}

	@Override
	protected String computeSignature() {
		// simple signature: count + last line hash
		int size = lines.size();
		if (size == 0) {
			return "0";
		}
		LogEntry last = lines.get(size - 1);
		return size + ":" + last.hashCode();
	}

	@Override
	protected HTMLElement buildBodyContent() {
		if (body == null) {
			body = createElement("div");
			body.setClassName("log-viewer-body");

			pre = createElement("ul");
			pre.setClassName("log-viewer-list");
		}

		for (LogEntry line : lines) {
			if (line == null || line.getElement() != null) {
				continue;
			}
			HTMLElement span = createElement("li");
			span.setClassName("log-entry log-entry-" + line.getLevel().getName());
			span.setTextContent("[" +
				Theme.formatTimestamp(line.getTimestamp(), false) +
				"]: " +
				line.getMessage());
			pre.appendChild(span);

			line.element = span;
		}

		body.appendChild(pre);
		return body;
	}

	@Getter
	public enum LogLevel {
		ERROR("error"),
		WARN("warn"),
		INFO("info"),
		DEBUG("debug");

		private final String name;

		LogLevel(String name) {
			this.name = name;
		}

		public static LogLevel parse(String name) {
			for (LogLevel level : values()) {
				if (level.name.equalsIgnoreCase(name)) {
					return level;
				}
			}
			return DEBUG; // default
		}
	}

	@Getter
	@EqualsAndHashCode
	@ToString
	public static class LogEntry {
		private final LogLevel level;
		private final String message;

		@Getter(AccessLevel.PRIVATE)
		@Setter(AccessLevel.PRIVATE)
		protected HTMLElement element;

		private final long timestamp;

		public LogEntry(LogLevel level, String message) {
			this.level = level;
			this.message = message;
			this.timestamp = System.currentTimeMillis();
		}
	}
}
