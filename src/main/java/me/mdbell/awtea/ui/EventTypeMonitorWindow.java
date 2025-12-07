package me.mdbell.awtea.ui;

import me.mdbell.awtea.monitor.EventTypeMonitor;
import org.teavm.jso.dom.html.HTMLElement;

public final class EventTypeMonitorWindow
	extends AbstractMonitorWindow<EventTypeMonitor.Entry, EventTypeMonitor.Snapshot> {

	private static final String[] HEADERS = {
		"Type",
		"Pending",
		"Post/s",
		"Dispatch/s",
		"Avg ms",
		"Total Posted",
		"Total Dispatched",
		"Last ID"
	};

	public EventTypeMonitorWindow() {
		super(
			"awt.eventtype.monitor",
			"AWT Event Types",
			700,
			200,
			250,                 // refresh every 250ms
			EventTypeMonitor.get()
		);
	}

	@Override
	protected String[] getColumnHeaders() {
		return HEADERS;
	}

	@Override
	protected String getRowCssClass(EventTypeMonitor.Snapshot snapshot, int rowIndex) {
		// Color rows based on how backed up this event type is
		int fill = snapshot.fillPercent(100); // 100 pending == 100%
		if (fill >= 90) return "evt-type-overloaded";
		if (fill >= 50) return "evt-type-busy";
		if (fill > 0) return "evt-type-okay";
		return "evt-type-idle";
	}

	@Override
	protected void fillRow(HTMLElement row, EventTypeMonitor.Snapshot snap, int rowIndex) {

		addCell(row, snap.getLabel()); // Type (class simple name)
		addCell(row, String.valueOf(snap.getPending()));

		addCell(row, formatItemsPerSec(snap.getPostRatePerSec()));
		addCell(row, formatItemsPerSec(snap.getDispatchRatePerSec()));

		addCell(row, formatMs(snap.getAvgDispatchTimeMs()));

		addCell(row, String.valueOf(snap.getTotalPosted()));
		addCell(row, String.valueOf(snap.getTotalDispatched()));

		addCell(row, snap.getLastEventId() > 0 ? String.valueOf(snap.getLastEventId()) : "-");
	}

	private String formatMs(double ms) {
		return ms > 0 ? String.format("%.1f", ms) : "-";
	}

	static {
		AwCss.sheet()
			.createClass("evt-type-idle")
			.prop("background", Theme.Var.TABLE_ROW_BACKGROUND)

			.createClass("evt-type-okay")
			.prop("background", Theme.Var.TABLE_ROW_ALT_BACKGROUND)

			.createClass("evt-type-busy")
			.prop("background", Theme.Var.TABLE_ROW_BACKGROUND)
			.prop("box-shadow")
			.value("inset 2px 0 0 0")
			.value(Theme.Var.METER_WARN)
			.end()

			.createClass("evt-type-overloaded")
			.prop("background", Theme.Var.TABLE_ROW_BACKGROUND)
			.prop("box-shadow")
			.value("inset 2px 0 0 0")
			.value(Theme.Var.METER_BAD)
			.end()
			.end()
			.inject();
	}
}
