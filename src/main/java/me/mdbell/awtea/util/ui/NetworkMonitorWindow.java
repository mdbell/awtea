package me.mdbell.awtea.util.ui;

import me.mdbell.awtea.monitor.NetworkMonitor;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.List;

public final class NetworkMonitorWindow extends FloatingWindow {

	private final NetworkMonitor monitor = NetworkMonitor.get();

	static {
		AwCss.sheet()
			.createClass("net-monitor-root")
			.prop("display", "flex")
			.prop("flex-direction", "column")
			.prop("gap", "0.4rem")

			.createClass("net-monitor-table")
			.prop("width", "100%")
			.prop("border-collapse", "collapse")
			.prop("font-size", "11px")

			.createClass("net-monitor-header-cell")
			.prop("border-bottom", "1px solid rgba(255,255,255,0.15)")
			.prop("padding", "2px 4px")
			.prop("text-align", "left")

			.createClass("net-monitor-data-cell")
			.prop("padding", "2px 4px")
			.prop("white-space", "nowrap")

			.createClass("net-monitor-rate-cell")
			.prop("padding", "2px 4px")
			.prop("text-align", "right")
			.prop("font-family", "monospace")

			.createClass("net-monitor-row")
			.prop("border-bottom", "1px solid rgba(255,255,255,0.07)")
			.prop("cursor", "default")

			.createClass("net-row-open")
			.prop("background", "rgba(0,180,0,0.15)")

			.createClass("net-row-connecting")
			.prop("background", "rgba(80,80,80,0.18)")

			.createClass("net-row-error")
			.prop("background", "rgba(220,0,0,0.2)")

			.createClass("net-row-closed")
			.prop("background", "rgba(255,255,255,0.05)")

			.end()
			.inject();
	}

	public NetworkMonitorWindow() {
		super(
			"network.monitor",
			"Network Monitor",
			1000,
			260,
			250   // refresh
		);
		setScrollable(true);
	}

	@Override
	protected String computeSignature() {
		// simple: always refresh on timer
		return null;
	}

	@Override
	protected HTMLElement buildBodyContent() {
		HTMLElement root = createElement("div");
		root.setClassName("net-monitor-root");

		List<NetworkMonitor.ConnectionSnapshot> conns = monitor.snapshot();
		if (conns.isEmpty()) {
			HTMLElement empty = createElement("div");
			empty.setTextContent("No active network connections.");
			empty.getStyle().setProperty("font-style", "italic");
			empty.getStyle().setProperty("color", Theme.Var.META_FOREGROUND.toCssValue());
			root.appendChild(empty);
			return root;
		}

		HTMLElement table = createElement("table");
		table.setClassName("net-monitor-table");

		// header
		HTMLElement thead = createElement("thead");
		HTMLElement headerRow = createElement("tr");

		String[] headers = {
			"ID",
			"Route",
			"Host",
			"Port",
			"State",
			"Bytes In",
			"Bytes Out",
			"In Rate",
			"Out Rate",
			"Age",
			"Last Activity"
		};

		for (int i = 0; i < headers.length; i++) {
			HTMLElement th = createElement("th");
			th.setClassName("net-monitor-header-cell");
			th.setTextContent(headers[i]);
			if (i >= 7 && i <= 8) {
				th.getStyle().setProperty("text-align", "right");
			}
			headerRow.appendChild(th);
		}

		thead.appendChild(headerRow);
		table.appendChild(thead);

		// body
		HTMLElement tbody = createElement("tbody");

		long now = System.currentTimeMillis();

		for (NetworkMonitor.ConnectionSnapshot c : conns) {
			HTMLElement row = createElement("tr");
			row.setClassName("net-monitor-row");
			row.getClassList().add(classForState(c.state));

			addCell(row, String.valueOf(c.id));
			addCell(row, c.route != null ? c.route : "-");
			addCell(row, c.host != null ? c.host : "-");
			addCell(row, String.valueOf(c.port));
			addCell(row, c.stateText);

			addCell(row, Theme.humanReadableSize(c.bytesIn));
			addCell(row, Theme.humanReadableSize(c.bytesOut));

			addRateCell(row, c.inRateBytesPerSec);
			addRateCell(row, c.outRateBytesPerSec);

			long ageMs = now - c.createdAtMillis;
			long idleMs = now - c.lastActivityMillis;
			addCell(row, formatDuration(ageMs));
			addCell(row, idleMs < 0 ? "?" : formatDuration(idleMs));

			tbody.appendChild(row);
		}

		table.appendChild(tbody);
		root.appendChild(table);
		return root;
	}

	private String classForState(NetworkMonitor.State state) {
		switch (state) {
			case OPEN:
				return "net-row-open";
			case CONNECTING:
			case CLOSING:
				return "net-row-connecting";
			case ERROR:
				return "net-row-error";
			case CLOSED:
			default:
				return "net-row-closed";
		}
	}

	private void addCell(HTMLElement row, String text) {
		HTMLElement td = createElement("td");
		td.setClassName("net-monitor-data-cell");
		td.setTextContent(text);
		row.appendChild(td);
	}

	private void addRateCell(HTMLElement row, double bytesPerSec) {
		HTMLElement td = createElement("td");
		td.setClassName("net-monitor-rate-cell");
		td.setTextContent(formatRate(bytesPerSec));
		row.appendChild(td);
	}

	private String formatRate(double bytesPerSec) {
		double rounded = Math.round(bytesPerSec * 10.0) / 10.0; //
		if (rounded <= 0.0) {
			return "-";
		}

		// Use humanReadableSize BUT clamp precision:
		String text = Theme.humanReadableSize(rounded);
		// theme gives us: "1.2 KB"

		// add "/s" and we're done:
		return text + "/s";
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
