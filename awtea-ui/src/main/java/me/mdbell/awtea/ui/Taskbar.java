package me.mdbell.awtea.ui;

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

		// Inject the CSS from embedded file
		HTMLElement style = Window.current()
			.getDocument()
			.createElement("style");
		style.setTextContent(UiStyles.taskbarCSS());
		Window.current()
			.getDocument()
			.getHead()
			.appendChild(style);
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
	 * Called by FloatingFrame when it is created.
	 */
	void registerWindow(FloatingFrame win) {
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
	 * Called by FloatingFrame when closed.
	 */
	void unregisterWindow(FloatingFrame win) {
		String id = win.getWindowId();
		HTMLElement btn = buttons.remove(id);
		if (btn != null && btn.getParentNode() != null) {
			btn.getParentNode().removeChild(btn);
		}
	}

	void setActive(FloatingFrame win, boolean active) {
		HTMLElement btn = buttons.get(win.getWindowId());
		if (btn == null) return;

		String cls = active ? "aw-taskbar-button aw-active" : "aw-taskbar-button";
		btn.setClassName(cls);
	}
}
