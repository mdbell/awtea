package me.mdbell.awtea.util.ui;

import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.util.JSObjectsExtensions;
import org.teavm.jso.browser.Storage;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLBodyElement;
import org.teavm.jso.dom.html.HTMLElement;

@ExtensionMethod({JSObjectsExtensions.class})
public class Theme {

	public static final String DARK_MODE_CLASS = "aw-dark-mode";

	private static final String DARK_MODE_STORAGE_KEY = "awtea.dark-mode";

	private static final Storage localStorage = Window.current().getLocalStorage();

	private static final Theme INSTANCE = new Theme();

	HTMLBodyElement body;

	private static boolean injected = false;

	private Theme() {
		body = Window.current().getDocument().getBody();
		injectStyles();

		// Set initial theme based on user preference
		boolean darkMode = getDarkModeFromStorage();
		setDarkMode(darkMode);
	}

	private static void injectStyles() {
		if (injected) {
			return;
		}
		injected = true;

		HTMLElement style = Window.current().getDocument().createElement("style");

		style.setTextContent(":root {" +
			"--aw-bg: rgba(255,255,255,0.97);" +
			"--aw-fg: #000;" +
			"--aw-border: #ccc;" +
			"--aw-shadow: rgba(0,0,0,0.25);" +
			"--aw-header-bg: #f0f0f0;" +
			"--aw-header-border: #ddd;" +
			"--aw-entry-border: #f0f0f0;" +
			"--aw-meta-fg: #777;" +
			"--aw-type-fg: #666;" +
			"--aw-button-bg: #f5f5f5;" +
			"--aw-button-border: #888;" +
			"--aw-error-fg: #b00020;" +
			"}" +
			"." + DARK_MODE_CLASS + " {" +
			"--aw-bg: #1e1e1e;" +
			"--aw-fg: #eee;" +
			"--aw-border: #444;" +
			"--aw-shadow: rgba(0,0,0,0.6);" +
			"--aw-header-bg: #2b2b2b;" +
			"--aw-header-border: #444;" +
			"--aw-entry-border: #333;" +
			"--aw-meta-fg: #aaa;" +
			"--aw-type-fg: #aaa;" +
			"--aw-button-bg: #333;" +
			"--aw-button-border: #777;" +
			"--aw-error-fg: #ff8080;" +
			"}");

		Window.current().getDocument().getHead().appendChild(style);
	}

	public boolean isDarkMode() {
		return body.getClassList().contains(DARK_MODE_CLASS);
	}

	private static boolean getDarkModeFromStorage() {
		if (localStorage.nullish()) {
			return detectInitialDarkMode();
		}
		String storedValue = localStorage.getItem(DARK_MODE_STORAGE_KEY);
		if (storedValue != null) {
			return storedValue.equals("true");
		}
		return detectInitialDarkMode();
	}


	private static boolean detectInitialDarkMode() {
		String mediaQuery = "(prefers-color-scheme: dark)";
		return Window.current().matchMedia(mediaQuery).getMatches();
	}

	public void setDarkMode(boolean darkMode) {
		if (isDarkMode() && !darkMode) {
			body.getClassList().remove(DARK_MODE_CLASS);
		} else if (!isDarkMode() && darkMode) {
			body.getClassList().add(DARK_MODE_CLASS);
		}

		if (!localStorage.nullish()) {
			localStorage.setItem(DARK_MODE_STORAGE_KEY, Boolean.toString(darkMode));
		}
	}

	public static Theme get() {
		return INSTANCE;
	}
}
