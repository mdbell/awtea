package me.mdbell.awtea.ui;

import me.mdbell.awtea.monitor.PcmMonitor;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

import java.util.List;

public final class PcmDockFrame extends FloatingFrame {

	private final PcmMonitor monitor = PcmMonitor.get();

	// simple selection state: current line id, -1 = first line
	private int selectedLineId = -1;

	private boolean suppressRefresh = false;

	private boolean hasSelectedAtLeastOnce = false;

	static {
		AwCss.sheet()
			.createClass("pcm-dock-root")
			.prop("display", "flex")
			.prop("flex-direction", "column")
			.prop("gap", "0.4rem")

			.createClass("pcm-dock-header")
			.prop("display", "flex")
			.prop("align-items", "center")
			.prop("gap", "0.5rem")
			.prop("font-size", "12px")
			.prop("color", Theme.Var.META_FOREGROUND)


			.createClass("pcm-dock-wave-container")
			.prop("flex", "1")
			.prop("display", "flex")
			.prop("flex-direction", "column")
			.prop("gap", "4px")
			.prop("overflow", "hidden")

			.createClass("pcm-dock-channel-row")
			.prop("display", "flex")
			.prop("align-items", "flex-end")
			.prop("height", "60px")
			.prop("background", Theme.Var.TABLE_ROW_BACKGROUND)
			.prop("border-radius", "3px")
			.prop("overflow", "hidden")

			.createClass("pcm-dock-bar")
			.prop("flex", "1")
			.prop("background", "transparent")

			.createClass("pcm-dock-bar-inner")
			.prop("width", "100%")
			.prop("background", Theme.Var.METER_GOOD)

			.end()
			.inject();
	}

	public PcmDockFrame() {
		super(
			"audio.pcmDock",
			"PCM Dock",
			600,
			200,
			100      // refresh fairly often
		);
		setScrollable(false);
	}

	@Override
	protected void refreshContent() {
		if (suppressRefresh) {
			return;
		}
		super.refreshContent();
	}

	@Override
	protected String computeSignature() {
		// simplest: always re-render on timer; you can later add a pcmRevision()
		return null;
	}

	@Override
	protected HTMLElement buildBodyContent() {
		HTMLElement root = createElement("div");
		root.setClassName("pcm-dock-root");
		List<PcmMonitor.Snapshot> snaps = monitor.snapshot();
		if (snaps.isEmpty()) {
			HTMLElement empty = createElement("div");
			empty.setTextContent("No PCM data (no active lines).");
			empty.getStyle().setProperty("font-style", "italic");
			empty.getStyle().setProperty("color", Theme.Var.META_FOREGROUND.toCssValue());
			root.appendChild(empty);
			return root;
		}

		PcmMonitor.Snapshot selected = selectSnapshot(snaps);

		if (selected == null) {
			HTMLElement empty = createElement("div");
			empty.setTextContent("No PCM data (no active lines).");
			empty.getStyle().setProperty("font-style", "italic");
			empty.getStyle().setProperty("color", Theme.Var.META_FOREGROUND.toCssValue());
			root.appendChild(empty);
			return root;
		}

		// --- header: line selector ---
		HTMLElement header = createElement("div");
		header.setClassName("pcm-dock-header");

		HTMLElement label = createElement("span");
		label.setTextContent("Line:");
		header.appendChild(label);

		HTMLInputElement select = createElement("select");

		for (PcmMonitor.Snapshot snap : snaps) {
			int id = snap.getId();

			HTMLElement option = createElement("option");
			option.setAttribute("value", Integer.toString(id));
			option.setTextContent(id + " - " + snap.getLabel());
			if (id == selected.getId()) {
				option.setAttribute("selected", "selected");
			}
			select.appendChild(option);
		}

		select.addEventListener("change", evt -> {
			String val = select.getValue();
			try {
				selectedLineId = Integer.parseInt(val);
			} catch (NumberFormatException ignored) {
				selectedLineId = -1;
				hasSelectedAtLeastOnce = false;
			}
			select.blur();
		});

		select.addEventListener("focus", evt -> suppressRefresh = hasSelectedAtLeastOnce = true);
		select.addEventListener("blur", evt -> suppressRefresh = false);

		header.appendChild(select);

		if (!hasSelectedAtLeastOnce) {
			HTMLElement hint = createElement("span");
			hint.setTextContent(" (auto-selecting most active line)");
			hint.getStyle().setProperty("font-style", "italic");
			hint.getStyle().setProperty("color", Theme.Var.META_FOREGROUND.toCssValue());
			header.appendChild(hint);
		}

		root.appendChild(header);

		// --- wave area ---
		HTMLElement waves = createElement("div");
		waves.setClassName("pcm-dock-wave-container");

		int channels = selected.getChannels();
		int slots = selected.getLength();
		float[] peaks = selected.getPeaks();

		for (int ch = 0; ch < channels; ch++) {
			HTMLElement row = createElement("div");
			row.setClassName("pcm-dock-channel-row");

			for (int i = 0; i < slots; i++) {
				int idx = i * channels + ch;
				float v = (idx < peaks.length) ? peaks[idx] : 0f;
				if (v < 0f) v = 0f;
				if (v > 1f) v = 1f;

				HTMLElement bar = createElement("div");
				bar.setClassName("pcm-dock-bar");

				HTMLElement inner = createElement("div");
				inner.setClassName("pcm-dock-bar-inner");
				inner.getStyle().setProperty("height", Math.round(v * 100) + "%");

				// color by magnitude: small = good, large = warn/bad
				if (v > 0.8f) {
					inner.getStyle().setProperty("background", Theme.Var.METER_BAD.toCssValue());
				} else if (v > 0.5f) {
					inner.getStyle().setProperty("background", Theme.Var.METER_WARN.toCssValue());
				} else {
					inner.getStyle().setProperty("background", Theme.Var.METER_GOOD.toCssValue());
				}

				bar.appendChild(inner);
				row.appendChild(bar);
			}

			waves.appendChild(row);
		}

		root.appendChild(waves);
		return root;
	}

	private PcmMonitor.Snapshot selectSnapshot(List<PcmMonitor.Snapshot> snaps) {
		if (snaps.isEmpty()) {
			return null;
		}
		if (!hasSelectedAtLeastOnce || selectedLineId == -1) {
			// finds the most active line
			return snaps.stream().min((a, b) -> {
				float aMax = 0f;
				for (float v : a.getPeaks()) {
					if (v > aMax) aMax = v;
				}
				float bMax = 0f;
				for (float v : b.getPeaks()) {
					if (v > bMax) bMax = v;
				}
				return Float.compare(bMax, aMax);
			}).orElse(null);
		}
		for (PcmMonitor.Snapshot s : snaps) {
			if (s.getId() == selectedLineId) {
				return s;
			}
		}
		return snaps.get(snaps.size() - 1);
	}
}
