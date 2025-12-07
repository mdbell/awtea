package me.mdbell.awtea.util.ui;

import me.mdbell.awtea.monitor.NetworkMonitor;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.List;

public final class NetworkMonitorWindow extends AbstractMonitorWindow<NetworkMonitor.Entry, NetworkMonitor.Snapshot> {

	private static String[] HEADERS = new String[]{
		"ID",
			"Route",
			"Host",
			"Port",
			"State",
			"Bytes In",
			"In Buffer",
			"Bytes Out",
			"Out Buffer",
			"In Rate",
			"Out Rate",
			"Age",
			"Last Activity"
	};

	public NetworkMonitorWindow() {
		super(
			"network.monitor",
			"Network Monitor",
			1000,
			260,
			250,   // refresh
			NetworkMonitor.get()
		);
	}

	@Override
	protected String[] getColumnHeaders() {
		return HEADERS;
	}

	@Override
	protected void fillRow(HTMLElement row, NetworkMonitor.Snapshot c, int rowIndex) {
		long now = System.currentTimeMillis();

		NetworkMonitor.State state = c.getState();
		String host = c.getHost();
		String route = c.getRoute();
		int port = c.getPort();

		addCell(row, String.valueOf(c.getId()));
		addCell(row, route != null ? route : "-");
		addCell(row, host != null ? host : "-");
		addCell(row, String.valueOf(port));
		addCell(row, state.toString().toLowerCase());

		addCell(row, Theme.humanReadableSize(c.getBytesIn()));
		addCell(row, Theme.humanReadableSize(c.getInBufferSize()));

		addCell(row, Theme.humanReadableSize(c.getBytesOut()));
		addCell(row, Theme.humanReadableSize(c.getOutBufferSize()));

		addRateCell(row, c.getInRateBytesPerSec());
		addRateCell(row, c.getOutRateBytesPerSec());

		long ageMs = now - c.getCreatedMillis();
		long idleMs = now - c.getLastUpdatedMillis();
		addDurationCell(row, ageMs);
		addDurationCell(row, idleMs);
	}

	protected String getRowCssClass(NetworkMonitor.Snapshot snapshot, int rowIndex) {
		switch (snapshot.getState()) {
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

	static {
		AwCss.sheet()
			.createClass("net-row-open")
			.prop("background", Theme.Var.ROW_STATUS_OK_BACKGROUND)

			.createClass("net-row-connecting")
			.prop("background", Theme.Var.ROW_STATUS_MUTED_BACKGROUND)

			.createClass("net-row-error")
			.prop("background", Theme.Var.ROW_STATUS_ERROR_BACKGROUND)

			.createClass("net-row-closed")
			.prop("background", Theme.Var.ROW_STATUS_MUTED_BACKGROUND)

			.end()
			.inject();
	}
}
