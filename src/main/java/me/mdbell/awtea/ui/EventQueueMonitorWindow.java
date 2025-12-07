package me.mdbell.awtea.ui;

import me.mdbell.awtea.monitor.EventQueueMonitor;
import org.teavm.jso.dom.html.HTMLElement;

public final class EventQueueMonitorWindow
	extends AbstractMonitorWindow<EventQueueMonitor.Entry, EventQueueMonitor.Snapshot> {

	private static final String[] HEADERS = {
		"Total",
		"Low",
		"Norm",
		"High",
		"Ultimate",
		"Post/s",
		"Dispatch/s",
		"Avg ms",
		"Last Event"
	};

	public EventQueueMonitorWindow() {
		super(
			"awt.queue.monitor",
			"AWT Event Queue",
			700,
			200,
			250,           // refresh every 250ms
			EventQueueMonitor.get()
		);
	}

	@Override
	protected String[] getColumnHeaders() {
		return HEADERS;
	}

	@Override
	protected String getRowCssClass(EventQueueMonitor.Snapshot snapshot, int rowIndex) {
		return "event-queue-row";
	}

	@Override
	protected void fillRow(HTMLElement row, EventQueueMonitor.Snapshot snap, int rowIndex) {

		int total = snap.getTotalPending();
		int[] p = snap.getPendingPerPriority();

		addCell(row, String.valueOf(total));
		addCell(row, String.valueOf(p[0]));
		addCell(row, String.valueOf(p[1]));
		addCell(row, String.valueOf(p[2]));
		addCell(row, String.valueOf(p[3]));

		addCell(row, formatItemsPerSec(snap.getPostRatePerSec()));
		addCell(row, formatItemsPerSec(snap.getDispatchRatePerSec()));
		addCell(row, formatMs(snap.getAvgDispatchTimeMs()));

		String last = snap.getLastEventClass() != null
			? snap.getLastEventClass() + " (" + snap.getLastEventId() + ")"
			: "-";
		addCell(row, last);

		// optional: row shading based on queue fullness
		int fill = snap.fillPercent(200); // e.g. 200 events == 100%
		row.getClassList().add(classForFill(fill));
	}

	private String formatMs(double ms) {
		return ms > 0 ? String.format("%.1f", ms) : "-";
	}

	private String classForFill(int fill) {
		if (fill >= 90) return "eq-overloaded";
		if (fill >= 50) return "eq-busy";
		if (fill > 0) return "eq-okay";
		return "eq-idle";
	}

	static {
		AwCss.sheet()
			.createClass("event-queue-row")
			.prop("background", Theme.Var.TABLE_ROW_BACKGROUND)

			.createClass("eq-idle")
			.prop("box-shadow").value("inset 2px 0 0 0").value(Theme.Var.METER_GOOD)
			.end()

			.createClass("eq-okay")
			.prop("box-shadow").value("inset 2px 0 0 0").value(Theme.Var.METER_GOOD)
			.end()

			.createClass("eq-busy")
			.prop("box-shadow").value("inset 2px 0 0 0").value(Theme.Var.METER_WARN)
			.end()

			.createClass("eq-overloaded")
			.prop("box-shadow").value("inset 2px 0 0 0").value(Theme.Var.METER_BAD)
			.end()

			.end()
			.inject();
	}
}
