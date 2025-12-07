package me.mdbell.awtea.util.ui;

import me.mdbell.awtea.monitor.AbstractMonitor;
import me.mdbell.awtea.monitor.MonitorEntry;
import me.mdbell.awtea.monitor.MonitorSnapshot;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.List;

public abstract class AbstractMonitorWindow<E extends MonitorEntry, S extends MonitorSnapshot<E>> extends FloatingWindow {

	protected final AbstractMonitor<E, S> monitor;

	protected AbstractMonitorWindow(
		String windowId,
		String title,
		int widthPx,
		int heightPx,
		int refreshIntervalMs,
		AbstractMonitor<E, S> monitor
	) {
		super(windowId, title, widthPx, heightPx, refreshIntervalMs);
		this.monitor = monitor;
		setScrollable(true);
	}
	
	@Override
	protected String computeSignature() {
		return String.valueOf(monitor.getRevision());
	}

	@Override
	protected HTMLElement buildBodyContent() {
		HTMLElement root = createElement("div");
		root.setClassName(getRootCssClass());

		List<S> snapshots = monitor.snapshot();
		if (snapshots == null || snapshots.isEmpty()) {
			HTMLElement empty = createElement("div");
			empty.setClassName(getEmptyCssClass());
			empty.setTextContent(getEmptyText());
			root.appendChild(empty);
			return root;
		}

		HTMLElement table = createElement("table");
		table.setClassName(getTableCssClass());
		root.appendChild(table);

		// ----- header -----
		String[] headers = getColumnHeaders();
		if (headers != null && headers.length > 0) {
			HTMLElement thead = createElement("thead");
			thead.setClassName(getHeaderCssClass());

			HTMLElement headerRow = createElement("tr");

			for (int i = 0; i < headers.length; i++) {
				HTMLElement th = createElement("th");
				th.setClassName(getHeaderCellCssClass(i));
				th.setTextContent(headers[i]);
				headerRow.appendChild(th);
			}

			thead.appendChild(headerRow);
			table.appendChild(thead);
		}

		// ----- body -----
		HTMLElement tbody = createElement("tbody");
		tbody.setClassName(getBodyCssClass());

		int rowIndex = 0;
		for (S snap : snapshots) {
			HTMLElement row = createElement("tr");
			row.setClassName(getRowCssClass(snap, rowIndex));

			fillRow(row, snap, rowIndex);

			tbody.appendChild(row);
			rowIndex++;
		}

		table.appendChild(tbody);
		return root;
	}

	// ---- Hooks for subclasses ----

	/** Column headers for the table. */
	protected abstract String[] getColumnHeaders();

	/** Fill all cells for a snapshot row. You can call addCell() helpers or build manually. */
	protected abstract void fillRow(HTMLElement row, S snapshot, int rowIndex);

	// ---- Styling hooks (defaults can be overridden per window) ----

	protected String getRootCssClass() {
		return "monitor-root";
	}

	protected String getEmptyCssClass() {
		return "monitor-empty";
	}

	protected String getTableCssClass() {
		return "monitor-table";
	}

	protected String getHeaderCssClass() {
		return "monitor-header";
	}

	protected String getBodyCssClass() {
		return "monitor-body";
	}

	protected String getHeaderCellCssClass(int columnIndex) {
		return "monitor-header-cell";
	}

	/** Base row class; you can override for status coloring, etc. */
	protected String getRowCssClass(S snapshot, int rowIndex) {
		return "monitor-row";
	}

	protected String getEmptyText() {
		return "No data.";
	}

	// Small helper if you like uniform cells
	protected void addCell(HTMLElement row, String text) {
		HTMLElement td = createElement("td");
		td.setClassName("monitor-data-cell");
		td.setTextContent(text);
		row.appendChild(td);
	}
}
