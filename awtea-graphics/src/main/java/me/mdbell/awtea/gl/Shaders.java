package me.mdbell.awtea.gl;

import lombok.experimental.UtilityClass;
import org.teavm.backend.javascript.spi.GeneratedBy;

@UtilityClass
public class Shaders {

	@ShaderSource("shaders/color.vert")
	@GeneratedBy(EmbedGenerator.class)
	public native String colorVertex();

	@ShaderSource("shaders/color.frag")
	@GeneratedBy(EmbedGenerator.class)
	public native String colorFragment();

	@ShaderSource("shaders/tex.vert")
	@GeneratedBy(EmbedGenerator.class)
	public native String textureVertex();

	@ShaderSource("shaders/tex.frag")
	@GeneratedBy(EmbedGenerator.class)
	public native String textureFragment();

}
