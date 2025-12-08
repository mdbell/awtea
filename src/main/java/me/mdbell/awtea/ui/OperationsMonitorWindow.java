package me.mdbell.awtea.ui;

import me.mdbell.awtea.monitor.OperationsMonitor;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

import java.util.List;

// we don't extend AbstractMonitorWindow here
// since this is a 'special' floating window, with multiple
// possible monitors inside it
public class OperationsMonitorWindow extends FloatingWindow {
	public OperationsMonitorWindow() {
		super("awtea.operations.monitor", "AWTEA Operations Monitor", 800, 400, 500);
	}

	@Override
	protected String computeSignature() {
		long revisions = OperationsMonitor.monitors().stream().mapToLong(OperationsMonitor::getRevision).sum();
		return String.valueOf(revisions);
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

		List<OperationsMonitor> monitors = List.copyOf(OperationsMonitor.monitors());

		if (monitors.isEmpty()) {
			HTMLElement noMonitors = createElement("div");
			noMonitors.setTextContent("No Operations Monitors available.");
			header.appendChild(noMonitors);
			return root;
		}

		HTMLInputElement select = createElement("select");
		select.setClassName("operations-select");

		for (OperationsMonitor monitor : monitors) {
			HTMLElement option = createElement("option");
			option.setTextContent(monitor.getName());
			option.setAttribute("value", String.valueOf(monitor.getId()));
			select.appendChild(option);
		}

		header.appendChild(select);

		return root;
	}

	static {
		AwCss.sheet()
			.createClass("operations-monitor-root")
			.prop("display", "flex")
			.prop("flex-direction", "column")
			.prop("height", "100%")

			.createClass("operations-monitor-header")
			.prop("display", "flex")
			.prop("align-items", "center")
			.prop("gap", "0.5rem")
			.prop("font-size", "12px")
			.prop("color", Theme.Var.META_FOREGROUND)

			.createClass("operations-select")
			.prop("padding", "2px 4px")
			.prop("background", Theme.Var.BACKGROUND)
			.prop("color", Theme.Var.FOREGROUND)
			.prop("border")
			.value("1px solid")
			.value(Theme.Var.BORDER)
			.end()
			.prop("border-radius", "3px")
			
			.end()
			.inject();
	}
}
