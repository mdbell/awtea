package me.mdbell.awtea.util.ui;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public final class Taskbar {
	private static Taskbar instance;

	private final HTMLDocument document;
	private final HTMLElement bar;
	private final Map<String, HTMLElement> buttons = new HashMap<>();

	static {

		AwCss.sheet()
			.createClass("aw-taskbar")
			.prop("position", "fixed")
			.prop("left", "0")
			.prop("right", "0")
			.prop("bottom", "0")
			.prop("height", "30px")
			.prop("display", "flex")
			.prop("align-items", "center")
			.prop("gap", "4px")
			.prop("padding", "0 6px")
			.prop("background", Theme.Var.HEADER_BACKGROUND)
			.prop("border-top")
			.value("1px solid")
			.value(Theme.Var.BORDER)
			.end()
			.prop("font-family", "sans-serif")
			.prop("font-size", "12px")
			.prop("z-index", "9998")
			.end()
			.createClass("aw-taskbar-button")
			.prop("padding", "2px 8px")
			.prop("border-radius", "3px")
			.prop("border")
			.value("1px solid")
			.value(Theme.Var.BORDER)
			.end()
			.prop("background", Theme.Var.BUTTON_BACKGROUND)
			.prop("color", Theme.Var.FOREGROUND)
			.prop("cursor", "pointer")
			.prop("max-width", "160px")
			.prop("overflow", "hidden")
			.prop("text-overflow", "ellipsis")
			.prop("white-space", "nowrap")
			.prop("margin-right", "4px")
			.end()
			.createClass("aw-taskbar-button.aw-active")
			.prop("font-weight", "600")
			.prop("border-color", Theme.Var.ACCENT)
			.end()
			.inject();
	}

	private Taskbar() {
		document = Window.current().getDocument();

		// Create taskbar element
		bar = document.createElement("div");
		bar.setClassName("aw-taskbar");

		boolean isDarkModeInital = Theme.isDarkMode();
		HTMLButtonElement darkModeToggle = (HTMLButtonElement) document.createElement("button");
		darkModeToggle.setClassName("aw-taskbar-button");
		darkModeToggle.setTextContent(isDarkModeInital ? "☀" : "☾");
		darkModeToggle.onClick(evt -> {
			EventQueue.invokeLater(() -> {
				boolean isDarkMode = Theme.isDarkMode();
				Theme.setDarkMode(!isDarkMode);
				darkModeToggle.setTextContent(!isDarkMode ? "☀" : "☾");
			});
		});
		bar.appendChild(darkModeToggle);

		document.getBody().appendChild(bar);
	}

	public static Taskbar get() {
		if (instance == null) {
			instance = new Taskbar();
		}
		return instance;
	}

	/**
	 * Called by FloatingWindow when it is created.
	 */
	void registerWindow(FloatingWindow win) {
		String id = win.getWindowId();
		if (buttons.containsKey(id)) {
			return;
		}

		HTMLElement btn = document.createElement("button");
		btn.setClassName("aw-taskbar-button");
		btn.setTextContent(win.getTitle());
		bar.appendChild(btn);

		buttons.put(id, btn);
		win.setTaskbarButton(btn);

		// Default: mark as active and bring to front
		setActive(win, true);

		btn.addEventListener("click", evt -> {
			evt.preventDefault();
			win.toggleMinimized();
		});
	}

	/**
	 * Called by FloatingWindow when closed.
	 */
	void unregisterWindow(FloatingWindow win) {
		String id = win.getWindowId();
		HTMLElement btn = buttons.remove(id);
		if (btn != null && btn.getParentNode() != null) {
			btn.getParentNode().removeChild(btn);
		}
	}

	void setActive(FloatingWindow win, boolean active) {
		HTMLElement btn = buttons.get(win.getWindowId());
		if (btn == null) return;

		String cls = active ? "aw-taskbar-button aw-active" : "aw-taskbar-button";
		btn.setClassName(cls);
	}
}
