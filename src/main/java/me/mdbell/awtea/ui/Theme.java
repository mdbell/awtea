package me.mdbell.awtea.ui;

import lombok.Getter;
import lombok.experimental.ExtensionMethod;
import me.mdbell.awtea.util.JSObjectsExtensions;
import org.teavm.jso.browser.Storage;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLBodyElement;

import java.util.Calendar;
import java.util.Date;

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
			.prop(Var.SCROLLBAR_THUMB, "rgba(0,0,0,0.2)")
			.prop(Var.SCROLLBAR_TRACK, "rgba(0,0,0,0.1)")
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

			.prop(Var.TABLE_HEADER_BACKGROUND, "#f5f5f5")
			.prop(Var.TABLE_HEADER_BORDER, "#dddddd")
			.prop(Var.TABLE_ROW_BACKGROUND, "#ffffff")
			.prop(Var.TABLE_ROW_ALT_BACKGROUND, "#f9f9f9")
			.prop(Var.TABLE_ROW_HOVER_BACKGROUND, "#e6f2ff")
			.prop(Var.TABLE_ROW_BORDER, "#eeeeee")

			.prop(Var.ROW_STATUS_MUTED_BACKGROUND, "rgba(160,160,160,0.18)")   // grey-ish
			.prop(Var.ROW_STATUS_OK_BACKGROUND, "rgba(0,160,0,0.18)")          // green-ish
			.prop(Var.ROW_STATUS_WARN_BACKGROUND, "rgba(230,190,0,0.2)")       // amber
			.prop(Var.ROW_STATUS_ERROR_BACKGROUND, "rgba(220,60,60,0.24)")     // red-ish

			.prop(Var.METER_BACKGROUND, "#dddddd")
			.prop(Var.METER_GOOD, "#4caf50")   // green
			.prop(Var.METER_WARN, "#ffb300")   // amber
			.prop(Var.METER_BAD, "#f44336")    // red
			.end()
			.createClass(DARK_MODE_CLASS)
			.prop(Var.BACKGROUND, "#1e1e1e")
			.prop(Var.FOREGROUND, "#eee")
			.prop(Var.BORDER, "#444")
			.prop(Var.SHADOW, "rgba(0,0,0,0.6)")
			.prop(Var.ACCENT, "#3399ff")
			.prop(Var.SCROLLBAR_THUMB, "rgba(255,255,255,0.2)")
			.prop(Var.SCROLLBAR_TRACK, "rgba(255,255,255,0.1)")
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

			.prop(Var.TABLE_HEADER_BACKGROUND, "#2b2b2b")
			.prop(Var.TABLE_HEADER_BORDER, "#444444")
			.prop(Var.TABLE_ROW_BACKGROUND, "#242424")
			.prop(Var.TABLE_ROW_ALT_BACKGROUND, "#1e1e1e")
			.prop(Var.TABLE_ROW_HOVER_BACKGROUND, "#303846")
			.prop(Var.TABLE_ROW_BORDER, "#333333")

			.prop(Var.ROW_STATUS_MUTED_BACKGROUND, "rgba(140,140,140,0.25)")
			.prop(Var.ROW_STATUS_OK_BACKGROUND, "rgba(0,200,0,0.22)")
			.prop(Var.ROW_STATUS_WARN_BACKGROUND, "rgba(230,210,80,0.24)")
			.prop(Var.ROW_STATUS_ERROR_BACKGROUND, "rgba(230,80,80,0.26)")

			.prop(Var.METER_BACKGROUND, "#333333")
			.prop(Var.METER_GOOD, "#66bb6a")
			.prop(Var.METER_WARN, "#ffca28")
			.prop(Var.METER_BAD, "#ef5350")
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

	public static String humanReadableSize(double bytes) {
		if (bytes < 0) {
			return "?";
		}
		if (bytes < 1024) {
			return ((int) bytes) + " B";
		}

		int unit = 1024;
		String[] units = {"B", "KB", "MB", "GB", "TB"};
		double value = bytes;
		int idx = 0;
		while (value >= unit && idx < units.length - 1) {
			value /= unit;
			idx++;
		}
		return String.format("%.1f %s", value, units[idx]);
	}

	public static String humanReadableTimestamp(long epochMillis) {
		long now = System.currentTimeMillis();
		long diff = now - epochMillis;

		long seconds = diff / 1000;
		long minutes = seconds / 60;
		long hours = minutes / 60;
		long days = hours / 24;

		if (seconds < 60) {
			return "just now";
		}
		if (minutes < 60) {
			return plural(minutes, "min");
		}
		if (hours < 24) {
			return plural(hours, "hour");
		}
		if (days < 7) {
			return plural(days, "day");
		}

		return formatTimestamp(epochMillis, true);
	}

	public static String formatTimestamp(long epochMillis, boolean withDate) {
		Date d = new Date(epochMillis);
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);

		StringBuilder sb = new StringBuilder();

		if (withDate) {
			sb.append(cal.get(Calendar.YEAR)).append("-")
				.append(pad(cal.get(Calendar.MONTH) + 1)).append("-")
				.append(pad(cal.get(Calendar.DAY_OF_MONTH))).append(" ");
		}
		sb.append(pad(cal.get(Calendar.HOUR_OF_DAY))).append(":")
			.append(pad(cal.get(Calendar.MINUTE))).append(":")
			.append(pad(cal.get(Calendar.SECOND)));
		return sb.toString();
	}

	public static String plural(long n, String unit) {
		return n + " " + unit + (n == 1 ? "" : "s") + " ago";
	}

	public static String pad(int n) {
		return (n < 10 ? "0" : "") + n;
	}

	public static String pad(String base, int n, String padChar) {
		StringBuilder sb = new StringBuilder(base);
		int count = sb.length();
		while (count < n) {
			sb.insert(0, padChar);
			count++;
		}
		return sb.toString();
	}

	@Getter
	public enum Var implements AwCss.CssKey {
		BACKGROUND("--aw-bg"),
		FOREGROUND("--aw-fg"),
		BORDER("--aw-border"),
		SHADOW("--aw-shadow"),
		ACCENT("--aw-accent"),
		SCROLLBAR_THUMB("--aw-scrollbar-thumb"),
		SCROLLBAR_TRACK("--aw-scrollbar-track"),
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
		DEBUG_FOREGROUND("--aw-debug-fg"),

		TABLE_HEADER_BACKGROUND("--aw-table-header-bg"),
		TABLE_HEADER_BORDER("--aw-table-header-border"),
		TABLE_ROW_BACKGROUND("--aw-table-row-bg"),
		TABLE_ROW_ALT_BACKGROUND("--aw-table-row-alt-bg"),
		TABLE_ROW_HOVER_BACKGROUND("--aw-table-row-hover-bg"),
		TABLE_ROW_BORDER("--aw-table-row-border"),

		// NEW: reusable “state row” backgrounds
		ROW_STATUS_MUTED_BACKGROUND("--aw-row-status-muted-bg"),
		ROW_STATUS_OK_BACKGROUND("--aw-row-status-ok-bg"),
		ROW_STATUS_WARN_BACKGROUND("--aw-row-status-warn-bg"),
		ROW_STATUS_ERROR_BACKGROUND("--aw-row-status-error-bg"),

		METER_BACKGROUND("--aw-meter-bg"),
		METER_GOOD("--aw-meter-good"),
		METER_WARN("--aw-meter-warn"),
		METER_BAD("--aw-meter-bad");;

		private final String cssVarName;

		Var(String cssVarName) {
			this.cssVarName = cssVarName;
		}

		@Override
		public String toCssKey() {
			return this.cssVarName;
		}

		@Override
		public String toCssValue() {
			return AwCss.var(this.cssVarName);
		}
	}
}
