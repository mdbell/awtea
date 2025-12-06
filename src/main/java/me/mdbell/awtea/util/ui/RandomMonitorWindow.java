package me.mdbell.awtea.util.ui;

import me.mdbell.awtea.monitor.RandomMonitor;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.List;

public final class RandomMonitorWindow extends FloatingWindow {

	private final RandomMonitor monitor = RandomMonitor.get();

	static {
		AwCss.sheet()
			// Root
			.createClass("random-monitor-root")
			.prop("display", "flex")
			.prop("flex-direction", "column")
			.prop("gap", "0.4rem")

			.createClass("random-monitor-no-rows")
			.prop("font-style", "italic")
			.prop("color", Theme.Var.META_FOREGROUND)

			// Table
			.createClass("random-monitor-table")
			.prop("width", "100%")
			.prop("border-collapse", "collapse")
			.prop("font-size", "11px")

			.createClass("random-monitor-header-cell")
			.prop("border-bottom")
			.value("1px solid")
			.value(Theme.Var.TABLE_HEADER_BORDER)
			.end()
			.prop("background", Theme.Var.TABLE_HEADER_BACKGROUND)
			.prop("color", Theme.Var.META_FOREGROUND)
			.prop("padding", "2px 4px")
			.prop("text-align", "left")

			.createClass("random-monitor-data-cell")
			.prop("padding", "2px 4px")
			.prop("white-space", "nowrap")

			.createClass("random-monitor-row")
			.prop("border-bottom")
			.value("1px solid")
			.value(Theme.Var.TABLE_ROW_BORDER)
			.end()
			.prop("background", Theme.Var.TABLE_ROW_BACKGROUND)
			.prop("color", Theme.Var.FOREGROUND)
			.prop("cursor", "default")

			// row “heat” variants
			.createClass("rnd-row-cold")
			.prop("background", Theme.Var.ROW_STATUS_MUTED_BACKGROUND)

			.createClass("rnd-row-normal")
			.prop("background", Theme.Var.ROW_STATUS_OK_BACKGROUND)

			.createClass("rnd-row-warm")
			.prop("background", Theme.Var.ROW_STATUS_WARN_BACKGROUND)

			.createClass("rnd-row-hot")
			.prop("background", Theme.Var.ROW_STATUS_ERROR_BACKGROUND)

			// monospace numeric columns
			.createClass("random-monitor-num-cell")
			.prop("font-family", "monospace")
			.prop("text-align", "right")

			.end()
			.inject();
	}

	public RandomMonitorWindow() {
		super(
			"random.monitor",
			"Random Monitor",
			900,
			260,
			500   // refresh every 500ms
		);
		setScrollable(true);
	}

	@Override
	protected String computeSignature() {
		// Just refresh on timer; if you add a revision counter later, you can use it here.
		return null;
	}

	@Override
	protected HTMLElement buildBodyContent() {
		HTMLElement root = createElement("div");
		root.setClassName("random-monitor-root");

		List<RandomMonitor.Snapshot> rows = RandomMonitor.get().snapshot();
		if (rows.isEmpty()) {
			HTMLElement empty = createElement("div");
			empty.setClassName("random-monitor-no-rows");
			empty.setTextContent("No Random instances observed yet.");
			root.appendChild(empty);
			return root;
		}

		HTMLElement table = createElement("table");
		table.setClassName("random-monitor-table");

		// ----- header -----
		HTMLElement thead = createElement("thead");
		HTMLElement headerRow = createElement("tr");

		String[] headers = {
			"ID",
			"Seed",
			"Created",
			"Last use",
			"Age",
			"nextInt",
			"nextInt(bound)",
			"nextLong",
			"nextBool",
			"nextFloat",
			"nextDouble",
			"nextGaussian",
			"Total"
		};

		for (String h : headers) {
			HTMLElement th = createElement("th");
			th.setClassName("random-monitor-header-cell");
			th.setTextContent(h);

			// numbers right-aligned
			if (!h.equals("Seed") && !h.equals("Created") && !h.equals("Last use") && !h.equals("Age")) {
				th.getStyle().setProperty("text-align", "right");
			}
			headerRow.appendChild(th);
		}

		thead.appendChild(headerRow);
		table.appendChild(thead);

		// ----- body -----
		HTMLElement tbody = createElement("tbody");

		long now = System.currentTimeMillis();

		for (RandomMonitor.Snapshot s : rows) {
			HTMLElement tr = createElement("tr");
			tr.setClassName("random-monitor-row");

			// compute totals / heat
			long total =
				s.callsNextInt +
					s.callsNextIntBound +
					s.callsNextLong +
					s.callsNextBoolean +
					s.callsNextFloat +
					s.callsNextDouble +
					s.callsNextGaussian;

			String heatClass = classifyRow(total, now, s);
			tr.getClassList().add(heatClass);

			// ID
			addCell(tr, String.valueOf(s.getId()), false);

			// Seed (hex-ish)
			String seedText = "0x" + Long.toHexString(s.seed);
			addCell(tr, seedText, false);

			// Created / last use / age
			addCell(tr, Theme.formatTimestamp(s.getCreatedMillis(), true), false);
			addCell(tr, Theme.humanReadableTimestamp(s.getLastUpdatedMillis()), false);

			long ageMs = now - s.getCreatedMillis();
			String ageText = Theme.humanReadableTimestamp(now - ageMs); // a bit hacky, but reuses wording
			addCell(tr, ageText, false);

			// Call counts
			addNumCell(tr, s.callsNextInt);
			addNumCell(tr, s.callsNextIntBound);
			addNumCell(tr, s.callsNextLong);
			addNumCell(tr, s.callsNextBoolean);
			addNumCell(tr, s.callsNextFloat);
			addNumCell(tr, s.callsNextDouble);
			addNumCell(tr, s.callsNextGaussian);
			addNumCell(tr, total);

			tbody.appendChild(tr);
		}

		table.appendChild(tbody);
		root.appendChild(table);
		return root;
	}

	private void addCell(HTMLElement row, String text, boolean numeric) {
		HTMLElement td = createElement("td");
		td.setClassName("random-monitor-data-cell");
		if (numeric) {
			td.getClassList().add("random-monitor-num-cell");
		}
		td.setTextContent(text);
		row.appendChild(td);
	}

	private void addNumCell(HTMLElement row, long value) {
		addCell(row, Long.toString(value), true);
	}

	private String classifyRow(long totalCalls, long now, RandomMonitor.Snapshot s) {
		if (totalCalls == 0) {
			return "rnd-row-cold";
		}
		long idleMs = now - s.getLastUpdatedMillis();

		// Very active & recent => hot
		if (totalCalls > 100000 && idleMs < 2000) {
			return "rnd-row-hot";
		}
		// Moderate usage => warm
		if (totalCalls > 1000 && idleMs < 10000) {
			return "rnd-row-warm";
		}
		// Otherwise normal-ish
		return "rnd-row-normal";
	}
}
