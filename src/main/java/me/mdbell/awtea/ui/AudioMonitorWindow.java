package me.mdbell.awtea.ui;

import me.mdbell.awtea.monitor.LineMonitor;
import org.teavm.jso.dom.html.HTMLElement;

import javax.sound.sampled.AudioFormat;

public final class AudioMonitorWindow extends AbstractMonitorWindow<LineMonitor.Entry, LineMonitor.Snapshot> {

	static final String[] HEADERS = new String[]{
		"ID",
		"Dir",
		"State",
		"Encoding",
		"Name / Format",
		"Buf (used/total)",
		"Fill",
		"W Rate",
		"D Rate",
		"Backlog"
	};

	@Override
	protected String getHeaderCellCssClass(int columnIndex) {
		if (columnIndex == 6 || columnIndex == 7) {
			return "audio-header-right monitor-header-cell";
		}
		return "monitor-header-cell";
	}

	public AudioMonitorWindow() {
		super(
			"audio.monitor",
			"Audio Monitor",
			1050,
			260,
			250,   // refresh every 250ms
			LineMonitor.get()
		);
	}

	@Override
	protected String[] getColumnHeaders() {
		return HEADERS;
	}

	@Override
	protected String getRowCssClass(LineMonitor.Snapshot snapshot, int rowIndex) {
		return "audio-monitor-row";
	}

	@Override
	protected void fillRow(HTMLElement row, LineMonitor.Snapshot line, int rowIndex) {

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

		addCell(row, getHumanReadableEncoding(line.getFormat()));

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
	}

	private String getHumanReadableEncoding(AudioFormat format) {
		AudioFormat.Encoding encoding = format != null ? format.getEncoding() : null;
		if (encoding == null) {
			return "(unknown)";
		}
		if (encoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
			return "PCM Signed";
		} else if (encoding.equals(AudioFormat.Encoding.PCM_UNSIGNED)) {
			return "PCM Unsigned";
		} else if (encoding.equals(AudioFormat.Encoding.ULAW)) {
			return "u-Law";
		} else if (encoding.equals(AudioFormat.Encoding.ALAW)) {
			return "a-Law";
		} else {
			return encoding.toString();
		}
	}

	private static String getClassName(LineMonitor.Snapshot line) {
		String className = "line-closed";

		if (line.isOpen()) {
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

	private HTMLElement createFillCell(LineMonitor.Snapshot line) {
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

	static {
		AwCss.sheet()

			.createClass("audio-header-right")
			.prop("text-align", "right !important")
			.end()

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
}
