package me.mdbell.awtea.ui;

import lombok.experimental.UtilityClass;
import me.mdbell.awtea.gl.CSSSource;

/**
 * Stylesheet sources embedded at build time by
 * {@code me.mdbell.awtea.gl.EmbedTransformer}: each {@code return "";}
 * placeholder is rewritten to the resource contents. Off TeaVM the
 * placeholders are what you get.
 */
@UtilityClass
public class UiStyles {

	@CSSSource("styles/menubar.css")
	public String menubarCSS() {
		return "";
	}

	@CSSSource("styles/taskbar.css")
	public String taskbarCSS() {
		return "";
	}

	@CSSSource("styles/floating-window.css")
	public String floatingWindowCSS() {
		return "";
	}

	@CSSSource("styles/floating-frame.css")
	public String floatingFrameCSS() {
		return "";
	}

	@CSSSource("styles/log-frame.css")
	public String logFrameCSS() {
		return "";
	}

	@CSSSource("styles/monitor-frame.css")
	public String monitorFrameCSS() {
		return "";
	}

}
