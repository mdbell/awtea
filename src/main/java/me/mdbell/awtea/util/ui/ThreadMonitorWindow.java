package me.mdbell.awtea.util.ui;

import me.mdbell.awtea.monitor.ThreadMonitor;
import org.teavm.jso.dom.html.HTMLElement;

public final class ThreadMonitorWindow extends AbstractMonitorWindow<
	ThreadMonitor.Entry, ThreadMonitor.Snapshot> {

	public static String[] HEADERS = {
		"ID",
		"Name",
		"Group",
		"Priority",
		"Daemon",
		"State",
		"Age",
		"Idle"
	};

	public ThreadMonitorWindow() {
		super(
			"thread.monitor",
			"Thread Monitor",
			700,
			260,
			500,    // refresh every 500ms
			ThreadMonitor.get()
		);
	}

	@Override
	protected String[] getColumnHeaders() {
		return HEADERS;
	}

	@Override
	protected void fillRow(HTMLElement row, ThreadMonitor.Snapshot t, int rowIndex) {
		long now = System.currentTimeMillis();
		ThreadMonitor.State state = t.getState();
		String label = t.getLabel();
		String group = t.getGroupName();
		int priority = t.getPriority();
		boolean daemon = t.isDaemon();

		addCell(row, String.valueOf(t.getId()));
		addCell(row, label);
		addCell(row, group != null ? group : "-");
		addCell(row, String.valueOf(priority));
		addCell(row, daemon ? "yes" : "no");
		addCell(row, state.toString().toLowerCase());

		long ageMs = now - t.getCreatedMillis();
		long idleMs = now - t.getLastUpdatedMillis();
		addCell(row, formatDuration(ageMs));
		addCell(row, idleMs < 0 ? "?" : formatDuration(idleMs));
	}

	protected String getRowCssClass(ThreadMonitor.Snapshot snapshot, int rowIndex) {
		switch (snapshot.getState()) {
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

	static {
		AwCss.sheet()
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
}
