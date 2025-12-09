package me.mdbell.awtea.ui;

import me.mdbell.awtea.monitor.OperationsMonitor;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

import java.util.Comparator;
import java.util.List;

// we don't extend AbstractMonitorWindow here
// since this is a 'special' floating window, with multiple
// possible monitors inside it
public class OperationsMonitorWindow extends FloatingWindow {

	private int selectedMonitorId = -1;

	public OperationsMonitorWindow() {
		super("awtea.operations.monitor", "AWTEA Operations Monitor", 800, 400, 500);
		ensureDelegatedClicks();
	}

	@Override
	protected String computeSignature() {
		long rev = OperationsMonitor.monitors().stream().map(OperationsMonitor::getRevision).reduce(
			0L,
			(a, b) -> a + b
		);
		return String.valueOf(rev);
	}

	@Override
	protected HTMLElement buildBodyContent() {
		HTMLElement root = createElement("div");
		root.setClassName("operations-monitor-root");

		HTMLElement header = createElement("div");
		header.setClassName("operations-monitor-header");

		root.appendChild(header);

		HTMLElement label = createElement("label");
		label.setTextContent("Select Monitor:");
		header.appendChild(label);

		List<OperationsMonitor> monitors = new java.util.ArrayList<>(OperationsMonitor.monitors());

		if (monitors.isEmpty()) {
			HTMLElement noMonitors = createElement("div");
			noMonitors.setTextContent("No Operations Monitors available.");
			header.appendChild(noMonitors);
			return root;
		}

		monitors.sort(Comparator.comparingInt(OperationsMonitor::getId));

		HTMLInputElement select = createElement("select");

		for (OperationsMonitor monitor : monitors) {
			HTMLElement option = createElement("option");
			option.setTextContent(monitor.getId() + " - " + monitor.getName());
			option.setAttribute("value", String.valueOf(monitor.getId()));
			select.appendChild(option);
		}

		if (selectedMonitorId == -1) {
			selectedMonitorId = monitors.get(0).getId();
		}

		select.setValue(String.valueOf(selectedMonitorId));

		select.addEventListener("change", evt -> {
			try {
				selectedMonitorId = Integer.parseInt(select.getValue());
			} catch (NumberFormatException ignored) {
				selectedMonitorId = -1;
			}
			select.blur();
		});

		header.appendChild(select);

		// render selected monitor
		OperationsMonitor selectedMonitor = monitors.stream()
			.filter(m -> m.getId() == selectedMonitorId)
			.findFirst()
			.orElse(null);

		if (selectedMonitor == null) {
			HTMLElement noSelection = createElement("div");
			noSelection.setTextContent("No Operations Monitor selected.");
			root.appendChild(noSelection);
			return root;
		}

		HTMLElement reset = createElement("a");
		reset.setAttribute("href", "#");
		reset.setTextContent("Reset Monitor");
		reset.setAttribute("data-aw-handler", "reset-monitor");
		header.appendChild(reset);

		registerClickHandler("reset-monitor", () -> {
			System.out.println("Resetting Operations Monitor...");
			selectedMonitor.reset();
		});


		HTMLElement container = createElement("div");
		container.setClassName("operations-monitor-container");
		root.appendChild(container);

		HTMLElement table = createElement("table");
		container.appendChild(table);

		// ----- header -----
		String[] headers = {
			"Name",
			"Invocations",
			"Total Duration (ms)",
			"Last Entry Time",
			"Last Exit Time",
			"Average Duration (ms)"
		};

		HTMLElement thead = createElement("thead");

		HTMLElement headerRow = createElement("tr");
		for (int i = 0; i < headers.length; i++) {
			HTMLElement th = createElement("th");
			th.setTextContent(headers[i]);
			headerRow.appendChild(th);
		}
		thead.appendChild(headerRow);
		table.appendChild(thead);

		HTMLElement tbody = createElement("tbody");
		table.appendChild(tbody);

		List<OperationsMonitor.Snapshot> snapshots = selectedMonitor.snapshot();

		for (OperationsMonitor.Snapshot snapshot : snapshots) {
			HTMLElement entryHeaderRow = createElement("tr");
			HTMLElement entryHeaderCell = createElement("td");
			entryHeaderCell.setAttribute("colspan", String.valueOf(headers.length));
			entryHeaderCell.setTextContent("Entry ID " + snapshot.getId() + " - " + snapshot.getLabel());
			entryHeaderRow.appendChild(entryHeaderCell);
			tbody.appendChild(entryHeaderRow);
			for (OperationsMonitor.Operation op : snapshot.getOperations()) {
				HTMLElement opRow = createElement("tr");

				HTMLElement nameCell = createElement("td");
				nameCell.setTextContent(op.getName());
				opRow.appendChild(nameCell);

				HTMLElement invocationsCell = createElement("td");
				invocationsCell.setTextContent(String.valueOf(op.getInvocationCount()));
				opRow.appendChild(invocationsCell);

				HTMLElement totalDurationCell = createElement("td");
				totalDurationCell.setTextContent(String.valueOf(op.getTotalTimeMs()));
				opRow.appendChild(totalDurationCell);

				HTMLElement lastEntryCell = createElement("td");
				lastEntryCell.setTextContent(String.valueOf(op.getLastEntryTimeMs()));
				opRow.appendChild(lastEntryCell);

				HTMLElement lastExitCell = createElement("td");
				lastExitCell.setTextContent(String.valueOf(op.getLastExitTimeMs()));
				opRow.appendChild(lastExitCell);

				double avgTime = Math.round(op.getAvgTimeMs() * 1000) / 1000.0;


				HTMLElement avgDurationCell = createElement("td");
				avgDurationCell.setTextContent(String.valueOf(avgTime));
				opRow.appendChild(avgDurationCell);

				tbody.appendChild(opRow);
			}
		}

		return root;
	}

	static {
		AwCss.sheet()
			.createClass("operations-monitor-root")
			.prop("display", "flex")
			.prop("flex-direction", "column")
			.prop("height", "100%")
			.prop("gap", "0.4rem")

			.createClass("operations-monitor-header")
			.prop("display", "flex")
			.prop("align-items", "center")
			.prop("gap", "0.5rem")
			.prop("font-size", "12px")
			.prop("color", Theme.Var.META_FOREGROUND)

			.createClass("operations-monitor-container")
			.prop("flex", "1")
			.prop("display", "flex")
			.prop("flex-direction", "column")
			.prop("gap", "4px")
			.prop("overflow", "hidden")
			.end()
			.inject();
	}
}
