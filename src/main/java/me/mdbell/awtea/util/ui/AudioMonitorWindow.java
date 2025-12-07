package me.mdbell.awtea.util.ui;

import me.mdbell.awtea.monitor.LineMonitor;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.List;

public final class AudioMonitorWindow extends FloatingWindow {

	private final LineMonitor monitor = LineMonitor.get();

	static {
		AwCss.sheet()
			.createClass("audio-monitor-root")
			.prop("display", "flex")
			.prop("flex-direction", "column")
			.prop("gap", "0.4rem")

			.createClass("audio-monitor-no-lines")
			.prop("font-style", "italic")
			.prop("color", Theme.Var.META_FOREGROUND)

			.createClass("audio-monitor-table")
			.prop("width", "100%")
			.prop("border-collapse", "collapse")
			.prop("font-size", "12px")
			.prop("background", Theme.Var.BACKGROUND)
			.prop("color", Theme.Var.FOREGROUND)

			.createClass("audio-monitor-header-cell")
			.prop("border-bottom")
			.value("1px solid")
			.value(Theme.Var.TABLE_HEADER_BORDER)
			.end()
			.prop("padding", "2px 4px")
			.prop("text-align", "left")
			.prop("background", Theme.Var.TABLE_HEADER_BACKGROUND)
			.prop("color", Theme.Var.FOREGROUND)

			.createClass("audio-monitor-rate-cell")
			.prop("text-align", "right")
			.prop("font-family", "monospace")
			.prop("width", "100px")
			.prop("max-width", "100px")
			.prop("padding", "2px 4px")
			.prop("color", Theme.Var.FOREGROUND)

			.createClass("audio-monitor-data-cell")
			.prop("padding", "2px 4px")
			.prop("white-space", "nowrap")
			.prop("color", Theme.Var.FOREGROUND)

			.createClass("audio-monitor-row")
			.prop("border-bottom")
			.value("1px solid")
			.value(Theme.Var.TABLE_HEADER_BORDER)
			.end()
			.prop("cursor", "default")
			.subClass(":hover")
			.prop("background", Theme.Var.TABLE_ROW_HOVER_BACKGROUND)

			.createClass("line-closed")
			.prop("background", Theme.Var.TABLE_ROW_BACKGROUND)

			.createClass("line-over-target")
			.prop("background", Theme.Var.TABLE_ROW_BACKGROUND)
			.prop("box-shadow")
			.value("inset 2px 0 0 0")
			.value(Theme.Var.METER_BAD)
			.end()

			.createClass("line-near-target")
			.prop("background", Theme.Var.TABLE_ROW_BACKGROUND)
			.prop("box-shadow")
			.value("inset 2px 0 0 0")
			.value(Theme.Var.METER_WARN)
			.end()

			.createClass("line-plenty-headroom")
			.prop("background", Theme.Var.TABLE_ROW_BACKGROUND)
			.prop("box-shadow")
			.value("inset 2px 0 0 0")
			.value(Theme.Var.METER_GOOD)
			.end()

			.createClass("line-normal")
			.prop("background", Theme.Var.TABLE_ROW_ALT_BACKGROUND)

			.createClass("fill-bar-outer")
			.prop("position", "relative")
			.prop("width", "80px")
			.prop("height", "6px")
			.prop("background", Theme.Var.METER_BACKGROUND)
			.prop("border-radius", "3px")
			.prop("overflow", "hidden")

			.createClass("fill-bar-inner")
			.prop("height", "100%")

			.createClass("fill-bar-30")
			.prop("background", Theme.Var.METER_GOOD)

			.createClass("fill-bar-70")
			.prop("background", Theme.Var.METER_WARN)

			.createClass("fill-bar-90")
			.prop("background", Theme.Var.METER_WARN)

			.createClass("fill-bar-100")
			.prop("background", Theme.Var.METER_BAD)

			.end()
			.inject();
	}


	public AudioMonitorWindow() {
		super(
			"audio.monitor",
			"Audio Monitor",
			1000,
			260,
			250   // refresh every 250ms
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
		root.setClassName("audio-monitor-root");

		List<LineMonitor.LineSnapshot> lines = monitor.snapshot();

		if (lines.isEmpty()) {
			HTMLElement empty = createElement("div");
			empty.setClassName("audio-monitor-no-lines");
			empty.setTextContent("No active audio lines.");
			root.appendChild(empty);
			return root;
		}

		HTMLElement table = createElement("table");
		table.setClassName("audio-monitor-table");

		// ----- header -----
		HTMLElement thead = createElement("thead");
		thead.setClassName("audio-monitor-header");
		HTMLElement headerRow = createElement("tr");

		String[] headers = {
			"ID",
			"Dir",
			"State",
			"Name / Format",
			"Buf (used/total)",
			"Fill",
			"W Rate",
			"D Rate",
			"Backlog"
		};

		for (int i = 0; i < headers.length; i++) {
			String h = headers[i];
			HTMLElement th = createElement("th");
			th.setClassName("audio-monitor-header-cell");
			th.setTextContent(h);

			// special styling for rate columns
			th.getStyle().setProperty("text-align", i >= 6 && i <= 7 ? "right" : "left");
			if (i == 6 || i == 7) {
				th.getStyle().setProperty("width", "100px");
			}
			headerRow.appendChild(th);
		}

		thead.appendChild(headerRow);
		table.appendChild(thead);

		// ----- body -----
		HTMLElement tbody = createElement("tbody");

		for (LineMonitor.LineSnapshot line : lines) {
			HTMLElement row = createElement("tr");
			row.setClassName("audio-monitor-row");

			int id = line.getId();
			boolean output = line.isOutput();
			boolean open = line.isOpen();
			boolean running = line.isRunning();
			int bufferSizeBytes = line.getBufferSizeBytes();
			int usedBytes = line.getUsedBytes();
			int backlogBytes = line.getBacklogBytes();
			int targetSlackBytes = line.getTargetSlackBytes();

			addCell(row, String.valueOf(id));
			addCell(row, output ? "Out" : "In");

			String state = (open ? "open" : "closed") +
				(running ? ", running" : ", stopped");
			addCell(row, state);

			addCell(row, line.getLabel() + " (" + line.formatSummary() + ")");

			String buf = usedBytes + " / " + bufferSizeBytes + " bytes";
			addCell(row, buf);

			// Fill cell with bar
			row.appendChild(createFillCell(line));

			// write / drain rates (fixed width cells)
			addRateCell(row, line.getWriteRateBytesPerSec());
			addRateCell(row, line.getDrainRateBytesPerSec());

			// backlog vs target
			String backlogText;
			if (targetSlackBytes > 0) {
				backlogText = backlogBytes + " / " + targetSlackBytes + " bytes";
			} else {
				backlogText = "-";
			}
			addCell(row, backlogText);

			String className = getClassName(line);

			row.getClassList().add(className);

			tbody.appendChild(row);
		}

		table.appendChild(tbody);
		root.appendChild(table);

		return root;
	}

	private static String getClassName(LineMonitor.LineSnapshot line) {
		String className = "line-closed";

		if(line.isOpen()) {
			int targetSlack = line.getTargetSlackBytes();
			double ratio = targetSlack == 0
				? 0
				: (double) line.getBacklogBytes() / (double) targetSlack;

			if (ratio >= 2.0) {
				className = "line-over-target";      // red: way over target
			} else if (ratio >= 1.0) {
				className = "line-near-target";    // amber: over target
			} else if (ratio <= 0.2) {
				className = "line-plenty-headroom";      // green: plenty of headroom
			} else {
				className = "line-normal";          // normal
			}
		}
		return className;
	}

	private void addCell(HTMLElement row, String text) {
		HTMLElement td = createElement("td");
		td.setClassName("audio-monitor-data-cell");
		td.setTextContent(text);
		row.appendChild(td);
	}

	private void addRateCell(HTMLElement row, double bytesPerSec) {
		HTMLElement td = createElement("td");
		td.setClassName("audio-monitor-rate-cell");

		td.setTextContent(formatRate(bytesPerSec));

		row.appendChild(td);
	}

	private HTMLElement createFillCell(LineMonitor.LineSnapshot line) {
		HTMLElement td = createElement("td");
		td.getStyle().setProperty("padding", "2px 4px");

		int fill = line.fillPercent();

		HTMLElement outer = createElement("div");
		outer.setClassName("fill-bar-outer");

		HTMLElement inner = createElement("div");
		inner.setClassName("fill-bar-inner");
		inner.getStyle().setProperty("width", Math.max(0, Math.min(100, fill)) + "%");

		String barColor;
		if (fill < 30) {
			barColor = "fill-bar-30";
		} else if (fill < 70) {
			barColor = "fill-bar-70";
		} else if (fill < 90) {
			barColor = "fill-bar-90";
		} else {
			barColor = "fill-bar-100";
		}
		inner.getClassList().add(barColor);

		outer.appendChild(inner);
		td.appendChild(outer);

		return td;
	}

	private String formatRate(double bytesPerSec) {
		if (bytesPerSec <= 0.0) {
			return "-";
		}
		return Theme.humanReadableSize(bytesPerSec) + "/s";
	}
}
