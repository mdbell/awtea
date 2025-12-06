package me.mdbell.awtea.util.ui;

import me.mdbell.awtea.sound.AudioMonitor;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;

import java.util.List;

public final class PcmDockWindow extends FloatingWindow {

	private final AudioMonitor monitor = AudioMonitor.get();

	// simple selection state: current line id, -1 = first line
	private int selectedLineId = -1;

	private boolean suppressRefresh = false;

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

			.createClass("pcm-dock-select")
			.prop("padding", "2px 4px")
			.prop("background", Theme.Var.BACKGROUND)
			.prop("color", Theme.Var.FOREGROUND)
			.prop("border")
			.value("1px solid")
			.value(Theme.Var.BORDER)
			.end()
			.prop("border-radius", "3px")

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

	public PcmDockWindow() {
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
		if(suppressRefresh) {
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

		List<AudioMonitor.PcmSnapshot> snaps = monitor.snapshotPcm();
		if (snaps.isEmpty()) {
			HTMLElement empty = createElement("div");
			empty.setTextContent("No PCM data (no active lines).");
			empty.getStyle().setProperty("font-style", "italic");
			empty.getStyle().setProperty("color", Theme.Var.META_FOREGROUND.toCssValue());
			root.appendChild(empty);
			return root;
		}

		AudioMonitor.PcmSnapshot selected = selectSnapshot(snaps);

		if( selected == null) {
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
		select.setClassName("pcm-dock-select");

		for (AudioMonitor.PcmSnapshot snap : snaps) {
			HTMLElement option = createElement("option");
			option.setAttribute("value", Integer.toString(snap.id));
			option.setTextContent(snap.id + " - " + snap.name);
			if (snap.id == selected.id) {
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
			}
		});

		select.addEventListener("focus", evt -> suppressRefresh = true);
		select.addEventListener("blur", evt -> suppressRefresh = false);

		header.appendChild(select);
		root.appendChild(header);

		// --- wave area ---
		HTMLElement waves = createElement("div");
		waves.setClassName("pcm-dock-wave-container");

		int channels = selected.channels;
		int slots = selected.length;
		float[] peaks = selected.peaks;

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

	private AudioMonitor.PcmSnapshot selectSnapshot(List<AudioMonitor.PcmSnapshot> snaps) {
		if (snaps.isEmpty()) {
			return null;
		}
		if (selectedLineId == -1) {
			return snaps.get(snaps.size() - 1);
		}
		for (AudioMonitor.PcmSnapshot s : snaps) {
			if (s.id == selectedLineId) {
				return s;
			}
		}
		return snaps.get(snaps.size() - 1);
	}
}
