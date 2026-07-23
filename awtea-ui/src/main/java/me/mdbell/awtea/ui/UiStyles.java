package me.mdbell.awtea.ui;

import lombok.experimental.UtilityClass;
import me.mdbell.awtea.instrument.EmbedResource;

/**
 * Stylesheet sources embedded at build time by
 * {@code me.mdbell.awtea.instrument.EmbedResourceTransformer}: each
 * {@code return "";} placeholder is rewritten to the resource contents. Off
 * TeaVM the placeholders are what you get.
 */
@UtilityClass
public class UiStyles {

	@EmbedResource("styles/menubar.css")
	public String menubarCSS() {
		return "";
	}

	@EmbedResource("styles/taskbar.css")
	public String taskbarCSS() {
		return "";
	}

	@EmbedResource("styles/floating-window.css")
	public String floatingWindowCSS() {
		return "";
	}

	@EmbedResource("styles/floating-frame.css")
	public String floatingFrameCSS() {
		return "";
	}

	@EmbedResource("styles/log-frame.css")
	public String logFrameCSS() {
		return "";
	}

	@EmbedResource("styles/monitor-frame.css")
	public String monitorFrameCSS() {
		return "";
	}

}
