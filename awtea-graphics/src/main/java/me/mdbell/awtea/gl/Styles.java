package me.mdbell.awtea.gl;

import lombok.experimental.UtilityClass;
import org.teavm.backend.javascript.spi.GeneratedBy;

@UtilityClass
public class Styles {

	@CSSSource("styles/component.css")
	@GeneratedBy(EmbedGenerator.class)
	public native String componentCSS();

}
