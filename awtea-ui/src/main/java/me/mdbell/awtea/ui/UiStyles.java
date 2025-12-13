package me.mdbell.awtea.ui;

import lombok.experimental.UtilityClass;
import me.mdbell.awtea.gl.CSSSource;
import me.mdbell.awtea.gl.EmbedGenerator;
import org.teavm.backend.javascript.spi.GeneratedBy;

@UtilityClass
public class UiStyles {

	@CSSSource("styles/menubar.css")
	@GeneratedBy(EmbedGenerator.class)
	public native String menubarCSS();

	@CSSSource("styles/taskbar.css")
	@GeneratedBy(EmbedGenerator.class)
	public native String taskbarCSS();

	@CSSSource("styles/floating-window.css")
	@GeneratedBy(EmbedGenerator.class)
	public native String floatingWindowCSS();

	@CSSSource("styles/floating-frame.css")
	@GeneratedBy(EmbedGenerator.class)
	public native String floatingFrameCSS();

}
