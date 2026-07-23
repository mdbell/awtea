package me.mdbell.awtea.gl;

import lombok.experimental.UtilityClass;

/**
 * Shader sources embedded at build time by {@link EmbedTransformer}: each
 * {@code return "";} placeholder is rewritten to the (include-preprocessed)
 * resource contents. Off TeaVM the placeholders are what you get.
 */
@UtilityClass
public class Shaders {

	@ShaderSource("shaders/color.vert")
	public String colorVertex() {
		return "";
	}

	@ShaderSource("shaders/color.frag")
	public String colorFragment() {
		return "";
	}

	@ShaderSource("shaders/tex.vert")
	public String textureVertex() {
		return "";
	}

	@ShaderSource("shaders/tex.frag")
	public String textureFragment() {
		return "";
	}

}
