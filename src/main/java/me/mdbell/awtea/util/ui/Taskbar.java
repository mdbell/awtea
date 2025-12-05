package me.mdbell.awtea.util.ui;

import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import java.util.HashMap;
import java.util.Map;

import static me.mdbell.awtea.util.ui.UiDispatcher.invokeLater;

public final class Taskbar {
	private static Taskbar instance;

	private final HTMLDocument document;
	private final HTMLElement bar;
	private final Map<String, HTMLElement> buttons = new HashMap<>();

	static {
		Theme.get(); // ensure theme is initialized
	}

	private Taskbar() {
		document = Window.current().getDocument();

		// Create taskbar element
		bar = document.createElement("div");
		bar.setClassName("aw-taskbar");

		boolean isDarkModeInital = Theme.get().isDarkMode();
		HTMLButtonElement darkModeToggle = (HTMLButtonElement) document.createElement("button");
		darkModeToggle.setClassName("aw-taskbar-button");
		darkModeToggle.setTextContent(isDarkModeInital ? "☀" : "☾");
		darkModeToggle.onClick(evt -> {
			invokeLater(() -> {
				boolean isDarkMode = Theme.get().isDarkMode();
				Theme.get().setDarkMode(!isDarkMode);
				darkModeToggle.setTextContent(!isDarkMode ? "☀" : "☾");
			});
		});
		bar.appendChild(darkModeToggle);

		document.getBody().appendChild(bar);

		injectStyles();
	}

	public static Taskbar get() {
		if (instance == null) {
			instance = new Taskbar();
		}
		return instance;
	}

	private void injectStyles() {
		HTMLElement style = document.createElement("style");
		style.setTextContent(
			".aw-taskbar { " +
				"position: fixed;" +
				"left: 0;" +
				"right: 0;" +
				"bottom: 0;" +
				"height: 30px;" +
				"display: flex;" +
				"align-items: center;" +
				"gap: 4px;" +
				"padding: 0 6px;" +
				"background: var(--aw-header-bg, #f0f0f0);" +
				"border-top: 1px solid var(--aw-border);" +
				"font-family: sans-serif;" +
				"font-size: 12px;" +
				"z-index: 9998;" +
				"}" +
				".aw-taskbar-button { " +
				"padding: 2px 8px;" +
				"border-radius: 3px;" +
				"border: 1px solid var(--aw-border);" +
				"background: var(--aw-button-bg);" +
				"color: var(--aw-fg);" +
				"cursor: pointer;" +
				"max-width: 160px;" +
				"overflow: hidden;" +
				"text-overflow: ellipsis;" +
				"white-space: nowrap;" +
				"margin-right: 4px;" +
				"}" +
				".aw-taskbar-button.aw-active { " +
				"font-weight: 600;" +
				"border-color: var(--aw-accent, #4c8bf5);" +
				"}"
		);
		document.getHead().appendChild(style);
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
