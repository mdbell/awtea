package me.mdbell.awtea.ui;

import lombok.*;
import me.mdbell.awtea.util.ConsoleBridge;
import me.mdbell.awtea.util.logging.LogLevel;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.ArrayList;
import java.util.List;

public class LogFrame extends FloatingFrame {

	private final List<LogEntry> lines = new ArrayList<>();
	private static final int MAX_LINES = 500;

	private HTMLElement body;
	private HTMLElement pre;

	static {
		// Inject the CSS from embedded file
		HTMLElement style = Window.current()
			.getDocument()
			.createElement("style");
		style.setTextContent(UiStyles.logFrameCSS());
		Window.current()
			.getDocument()
			.getHead()
			.appendChild(style);
	}

	public LogFrame() {
		super("logviewer", "Log Viewer", 800, 400, 0); // no timer needed
		setAutoscroll(true);

		// Install console hook
		ConsoleBridge.install((level, msg) -> {
			// Make sure UI changes happen via schedule(), like in FsViewWindow
			schedule(() -> appendLine(level, msg));
		});
	}

	void appendLine(String level, String msg) {
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
			span.setClassName("log-entry log-entry-" + line.getLevel().name().toLowerCase());
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
