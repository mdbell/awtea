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

	private static boolean stylesInjected = false;

	public LogWindow() {
		super("logviewer", "Log Viewer", 800, "400px", 0); // no timer needed
		injectStylesOnce();
		setAutoscroll(true);

		// Install console hook
		ConsoleBridge.install((level, msg) -> {
			// Make sure UI changes happen via schedule(), like in FsViewWindow
			schedule(() -> appendLine(level, msg));
		});
	}

	private void injectStylesOnce() {
		if (stylesInjected) return;
		stylesInjected = true;


		HTMLElement style = document.createElement("style");

		style.setTextContent(
			".log-viewer-list {" +
			"list-style-type: none;" +
			"padding: 0;" +
			"margin: 0;" +
			"}" +
			".log-entry {" +
			"font-family: monospace;" +
			"white-space: pre-wrap;" +
			"margin: 0;" +
			"}" +
			".log-entry-error { color: var(--aw-error-fg); }" +
			".log-entry-error:before { content: '[ERR] '; font-weight: bold; }" +
			".log-entry-warn { color: var(--aw-warning-fg); }" +
			".log-entry-warn:before { content: '[WRN] '; font-weight: bold; }" +
			".log-entry-info { color: var(--aw-info-fg); }" +
			".log-entry-info:before { content: '[INF] '; font-weight: bold; }" +
			".log-entry-debug { color: var(--aw-debug-fg); }" +
			".log-entry-debug:before { content: '[DBG] '; font-weight: bold; }"
		);

		document.getHead().appendChild(style);
	}

	private void appendLine(String level, String msg) {
		LogLevel logLevel = LogLevel.parse(level);
		LogEntry entry = new LogEntry(logLevel, msg);
		lines.add(entry);
		if (lines.size() > MAX_LINES) {
			HTMLElement element = lines.get(0).getElement();
			if (element != null && element.getParentNode() != null) {
				element.getParentNode().removeChild(element);
			}
			lines.remove(0);
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
			span.setTextContent(line.getMessage());
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

		public LogEntry(LogLevel level, String message) {
			this.level = level;
			this.message = message;
		}
	}
}
