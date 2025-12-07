package me.mdbell.awtea.ui;

import me.mdbell.awtea.monitor.ScheduleTaskMonitor;
import org.teavm.jso.dom.html.HTMLElement;

public class ScheduleTaskMonitorWindow extends AbstractMonitorWindow<ScheduleTaskMonitor.Entry,
	ScheduleTaskMonitor.Snapshot> {

	public static final String[] HEADERS = {
		"ID",
		"Name",
		"Run Count",
		"Next Run Time",
		"Period (ms)"
	};

	public ScheduleTaskMonitorWindow() {
		super("task.monitor",
			"Task Monitor",
			700,
			260,
			50,
			ScheduleTaskMonitor.get());
	}

	@Override
	protected String[] getColumnHeaders() {
		return HEADERS;
	}

	@Override
	protected void fillRow(HTMLElement row, ScheduleTaskMonitor.Snapshot snapshot, int rowIndex) {

		addCell(row, String.valueOf(snapshot.getId()));
		addCell(row, snapshot.getName() != null ? snapshot.getName() : "-");
		addCell(row, String.valueOf(snapshot.getRunCount()));

		addTimeTillCell(row, snapshot.getNextRunTime());

		addCell(row, snapshot.getPeriodMillis() > 0 ?
			String.valueOf(snapshot.getPeriodMillis()) : "-");
	}
}
