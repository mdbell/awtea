package me.mdbell.awtea.util.ui;

import me.mdbell.awtea.monitor.ThreadMonitor;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.List;

public final class ThreadMonitorWindow extends FloatingWindow {

	private final ThreadMonitor monitor = ThreadMonitor.get();

	static {
		AwCss.sheet()
			.createClass("thread-monitor-root")
			.prop("display", "flex")
			.prop("flex-direction", "column")
			.prop("gap", "0.4rem")

			.createClass("thread-monitor-table")
			.prop("width", "100%")
			.prop("border-collapse", "collapse")
			.prop("font-size", "11px")

			.createClass("thread-monitor-header-cell")
			.prop("border-bottom")
			.value("1px solid")
			.value(Theme.Var.TABLE_HEADER_BORDER)
			.end()
			.prop("background", Theme.Var.TABLE_HEADER_BACKGROUND)
			.prop("color", Theme.Var.META_FOREGROUND)
			.prop("padding", "2px 4px")
			.prop("text-align", "left")

			.createClass("thread-monitor-data-cell")
			.prop("padding", "2px 4px")
			.prop("white-space", "nowrap")

			.createClass("thread-monitor-row")
			.prop("border-bottom")
			.value("1px solid")
			.value(Theme.Var.TABLE_ROW_BORDER)
			.end()
			.prop("background", Theme.Var.TABLE_ROW_BACKGROUND)
			.prop("color", Theme.Var.FOREGROUND)
			.prop("cursor", "default")

			.createClass("thread-row-new")
			.prop("background", Theme.Var.ROW_STATUS_MUTED_BACKGROUND)

			.createClass("thread-row-running")
			.prop("background", Theme.Var.ROW_STATUS_OK_BACKGROUND)

			.createClass("thread-row-sleeping")
			.prop("background", Theme.Var.ROW_STATUS_WARN_BACKGROUND)

			.createClass("thread-row-terminated")
			.prop("background", Theme.Var.ROW_STATUS_MUTED_BACKGROUND)

			.end()
			.inject();
	}

	public ThreadMonitorWindow() {
		super(
			"thread.monitor",
			"Thread Monitor",
			700,
			260,
			500    // refresh every 500ms
		);
		setScrollable(true);
	}

	@Override
	protected String computeSignature() {
		// simple: refresh on timer always
		return null;
	}

	@Override
	protected HTMLElement buildBodyContent() {
		HTMLElement root = createElement("div");
		root.setClassName("thread-monitor-root");

		List<ThreadMonitor.ThreadSnapshot> threads = monitor.snapshot();
		if (threads.isEmpty()) {
			HTMLElement empty = createElement("div");
			empty.setTextContent("No tracked threads.");
			empty.getStyle().setProperty("font-style", "italic");
			empty.getStyle().setProperty("color", Theme.Var.META_FOREGROUND.toCssValue());
			root.appendChild(empty);
			return root;
		}

		HTMLElement table = createElement("table");
		table.setClassName("thread-monitor-table");

		// header
		HTMLElement thead = createElement("thead");
		HTMLElement headerRow = createElement("tr");

		String[] headers = {
			"ID",
			"Name",
			"Group",
			"Daemon",
			"State",
			"Age",
			"Idle"
		};

		for (String h : headers) {
			HTMLElement th = createElement("th");
			th.setClassName("thread-monitor-header-cell");
			th.setTextContent(h);
			headerRow.appendChild(th);
		}
		thead.appendChild(headerRow);
		table.appendChild(thead);

		// body
		HTMLElement tbody = createElement("tbody");
		long now = System.currentTimeMillis();

		for (ThreadMonitor.ThreadSnapshot t : threads) {
			HTMLElement row = createElement("tr");
			row.setClassName("thread-monitor-row");
			row.getClassList().add(classForState(t.state));

			addCell(row, String.valueOf(t.id));
			addCell(row, t.name != null ? t.name : "(unnamed)");
			addCell(row, t.groupName != null ? t.groupName : "-");
			addCell(row, t.daemon ? "yes" : "no");
			addCell(row, t.state.toString().toLowerCase());

			long ageMs = now - t.createdAtMillis;
			long idleMs = now - t.lastActivityMillis;
			addCell(row, formatDuration(ageMs));
			addCell(row, idleMs < 0 ? "?" : formatDuration(idleMs));

			tbody.appendChild(row);
		}

		table.appendChild(tbody);
		root.appendChild(table);
		return root;
	}

	private String classForState(ThreadMonitor.State state) {
		switch (state) {
			case RUNNING:
				return "thread-row-running";
			case SLEEPING:
				return "thread-row-sleeping";
			case TERMINATED:
				return "thread-row-terminated";
			case NEW:
			case STARTED:
			default:
				return "thread-row-new";
		}
	}

	private void addCell(HTMLElement row, String text) {
		HTMLElement td = createElement("td");
		td.setClassName("thread-monitor-data-cell");
		td.setTextContent(text);
		row.appendChild(td);
	}

	private String formatDuration(long ms) {
		if (ms < 0) return "?";
		long seconds = ms / 1000;
		if (seconds < 60) {
			return seconds + "s";
		}
		long minutes = seconds / 60;
		if (minutes < 60) {
			long rem = seconds % 60;
			return minutes + "m " + rem + "s";
		}
		long hours = minutes / 60;
		long remMin = minutes % 60;
		return hours + "h " + remMin + "m";
	}
}
