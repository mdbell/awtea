package me.mdbell.awtea.util.ui;

import lombok.Getter;
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

	private static final HTMLBodyElement body;

	static {
		body = Window.current().getDocument().getBody();
		boolean darkMode = getDarkModeFromStorage();
		setDarkMode(darkMode);

		AwCss.sheet()
			.rule(":root")
			.prop(Var.BACKGROUND, "rgba(255,255,255,0.97)")
			.prop(Var.FOREGROUND, "#000")
			.prop(Var.BORDER, "#ccc")
			.prop(Var.SHADOW, "rgba(0,0,0,0.25)")
			.prop(Var.ACCENT, "#0066cc")
			.prop(Var.HEADER_BACKGROUND, "#f0f0f0")
			.prop(Var.HEADER_BORDER, "#ddd")
			.prop(Var.ENTRY_BORDER, "#f0f0f0")
			.prop(Var.META_FOREGROUND, "#777")
			.prop(Var.TYPE_FOREGROUND, "#666")
			.prop(Var.BUTTON_BACKGROUND, "#f5f5f5")
			.prop(Var.BUTTON_BORDER, "#888")
			.prop(Var.ERROR_FOREGROUND, "#b00020")
			.prop(Var.WARNING_FOREGROUND, "#e65c00")
			.prop(Var.INFO_FOREGROUND, "#0066cc")
			.prop(Var.DEBUG_FOREGROUND, "#444")
			.end()
			.createClass(DARK_MODE_CLASS)
			.prop(Var.BACKGROUND, "#1e1e1e")
			.prop(Var.FOREGROUND, "#eee")
			.prop(Var.BORDER, "#444")
			.prop(Var.SHADOW, "rgba(0,0,0,0.6)")
			.prop(Var.ACCENT, "#3399ff")
			.prop(Var.HEADER_BACKGROUND, "#2b2b2b")
			.prop(Var.HEADER_BORDER, "#444")
			.prop(Var.ENTRY_BORDER, "#333")
			.prop(Var.META_FOREGROUND, "#aaa")
			.prop(Var.TYPE_FOREGROUND, "#aaa")
			.prop(Var.BUTTON_BACKGROUND, "#333")
			.prop(Var.BUTTON_BORDER, "#777")
			.prop(Var.ERROR_FOREGROUND, "#ff8080")
			.prop(Var.WARNING_FOREGROUND, "#ffb366")
			.prop(Var.INFO_FOREGROUND, "#3399ff")
			.prop(Var.DEBUG_FOREGROUND, "#ccc")
			.end()
			.inject();
	}

	public static boolean isDarkMode() {
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

	public static void setDarkMode(boolean darkMode) {
		if (isDarkMode() && !darkMode) {
			body.getClassList().remove(DARK_MODE_CLASS);
		} else if (!isDarkMode() && darkMode) {
			body.getClassList().add(DARK_MODE_CLASS);
		}

		if (!localStorage.nullish()) {
			localStorage.setItem(DARK_MODE_STORAGE_KEY, Boolean.toString(darkMode));
		}
	}

	@Getter
	public enum Var implements AwCss.CssKey {
		BACKGROUND("--aw-bg"),
		FOREGROUND("--aw-fg"),
		BORDER("--aw-border"),
		SHADOW("--aw-shadow"),
		ACCENT("--aw-accent"),
		HEADER_BACKGROUND("--aw-header-bg"),
		HEADER_BORDER("--aw-header-border"),
		ENTRY_BORDER("--aw-entry-border"),
		META_FOREGROUND("--aw-meta-fg"),
		TYPE_FOREGROUND("--aw-type-fg"),
		BUTTON_BACKGROUND("--aw-button-bg"),
		BUTTON_BORDER("--aw-button-border"),
		ERROR_FOREGROUND("--aw-error-fg"),
		WARNING_FOREGROUND("--aw-warning-fg"),
		INFO_FOREGROUND("--aw-info-fg"),
		DEBUG_FOREGROUND("--aw-debug-fg");

		private final String cssVarName;

		Var(String cssVarName) {
			this.cssVarName = cssVarName;
		}

		public String toCssKey() {
			return this.cssVarName;
		}

		public String toCssValue() {
			return AwCss.var(this.cssVarName);
		}
	}
}
