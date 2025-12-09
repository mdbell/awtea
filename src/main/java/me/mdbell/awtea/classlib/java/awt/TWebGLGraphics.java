package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import me.mdbell.awtea.classlib.java.awt.image.TDataBufferInt;
import me.mdbell.awtea.classlib.java.awt.image.TImageObserver;
import me.mdbell.awtea.instrument.Monitored;
import org.teavm.jso.canvas.ImageData;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.typedarrays.*;
import org.teavm.jso.webgl.*;

import java.awt.*;

@Monitored.AllMethods
public class TWebGLGraphics extends TCanvasGraphics {

	private final WebGL2RenderingContext gl;

	private final WebGLProgram colorProgram;
	private final WebGLProgram textureProgram;

	private final WebGLBuffer rectBuffer;

	// uniforms / attribs for color program
	private final WebGLUniformLocation uResolutionLocColor;
	private final WebGLUniformLocation uColorLoc;
	private final WebGLUniformLocation uTranslationLocColor;
	private final int aPositionLocColor;

	// uniforms / attribs for texture program
	private final WebGLUniformLocation uSwizzleModeLoc;
	private final WebGLUniformLocation uResolutionLocTex;
	private final WebGLUniformLocation uTranslationLocTex;
	private final int aPositionLocTex;
	private final int aTexCoordLocTex;

	private final WebGLBuffer quadBuffer;
	private final WebGLBuffer quadTexCoordBuffer;

	private int translateX = 0;
	private int translateY = 0;

	private Color color = Color.BLACK;

	// simple rectangular clip; null = no clip
	private TRectangle clip;

	public TWebGLGraphics(HTMLCanvasElement canvas) {
		this((WebGL2RenderingContext) canvas.getContext("webgl2"), canvas);
	}

	public TWebGLGraphics(WebGL2RenderingContext gl, HTMLCanvasElement canvas) {
		super(canvas);
		this.supportsBlit = false;
		this.gl = gl;

		// basic GL state
		gl.enable(WebGLRenderingContext.BLEND);
		gl.blendFunc(WebGLRenderingContext.SRC_ALPHA, WebGLRenderingContext.ONE_MINUS_SRC_ALPHA);

		// ---- build shader programs ----
		this.colorProgram = createProgram(gl, COLOR_VERTEX_SRC, COLOR_FRAGMENT_SRC);
		this.textureProgram = createProgram(gl, TEX_VERTEX_SRC, TEX_FRAGMENT_SRC);

		// color program locations
		gl.useProgram(colorProgram);
		this.aPositionLocColor = gl.getAttribLocation(colorProgram, "a_position");
		this.uResolutionLocColor = gl.getUniformLocation(colorProgram, "u_resolution");
		this.uColorLoc = gl.getUniformLocation(colorProgram, "u_color");
		this.uTranslationLocColor = gl.getUniformLocation(colorProgram, "u_translation");

		// texture program locations
		gl.useProgram(textureProgram);
		this.aPositionLocTex = gl.getAttribLocation(textureProgram, "a_position");
		this.aTexCoordLocTex = gl.getAttribLocation(textureProgram, "a_texCoord");
		this.uResolutionLocTex = gl.getUniformLocation(textureProgram, "u_resolution");
		this.uTranslationLocTex = gl.getUniformLocation(textureProgram, "u_translation");
		this.uSwizzleModeLoc = gl.getUniformLocation(textureProgram, "u_swizzleMode");

		// ---- buffers ----
		// simple rect buffer: two triangles in [0,0]-[1,1] space
		rectBuffer = gl.createBuffer();

		quadBuffer = gl.createBuffer();
		quadTexCoordBuffer = gl.createBuffer();

		// no persistent data; we’ll stream per draw
		reset();
	}

	@Override
	public TGraphics create() {
		// For now, share underlying GL state; clone transforms/color/clip
		TWebGLGraphics g = new TWebGLGraphics(gl, canvas);
		g.translateX = this.translateX;
		g.translateY = this.translateY;
		g.color = this.color;
		g.clip = this.clip;
		return g;
	}

	@Override
	public void setClip(int x, int y, int width, int height) {
		if (width <= 0 || height <= 0) {
			clip = null;
			gl.disable(WebGLRenderingContext.SCISSOR_TEST);
			return;
		}
		clip = new TRectangle(x, y, width, height);
		applyClip();
	}

	private void applyClip() {
		if (clip == null) {
			gl.disable(WebGLRenderingContext.SCISSOR_TEST);
			return;
		}
		gl.enable(WebGLRenderingContext.SCISSOR_TEST);
		// WebGL coordinates start at bottom-left, we’re in top-left
		int cx = clip.x + translateX;
		int cy = clip.y + translateY;
		int h = canvas.getHeight();
		gl.scissor(cx, h - (cy + clip.height), clip.width, clip.height);
	}

	@Override
	public void setXORMode(Color c1) {

	}

	@Override
	public void setPaintMode() {

	}

	@Override
	public void translate(int deltaX, int deltaY) {
		this.translateX += deltaX;
		this.translateY += deltaY;
		// scissor needs to respect translation
		applyClip();
	}

	@Override
	public TFont getFont() {
		return null;
	}

	@Override
	public void setFont(TFont font) {

	}

	@Override
	public Color getColor() {
		return color;
	}

	@Override
	public void setColor(Color c) {
		this.color = c;
	}

	@Override
	public TFontMetrics getFontMetrics(TFont f) {
		return null;
	}

	@Override
	public TRectangle getClipBounds() {
		return clip;
	}

	@Override
	public void drawString(String str, int x, int y) {

	}

	@Override
	public void drawRect(int x, int y, int width, int height) {
		// draw four thin rectangles
		fillRect(x, y, width, 1);
		fillRect(x, y + height - 1, width, 1);
		fillRect(x, y, 1, height);
		fillRect(x + width - 1, y, 1, height);
	}

	@Override
	public void fillRect(int x, int y, int width, int height) {
		if (width <= 0 || height <= 0) {
			return;
		}
		useColorProgram();

		float[] verts = {
			x, y,
			x + width, y,
			x, y + height,
			x, y + height,
			x + width, y,
			x + width, y + height
		};
		uploadRectVertices(verts);

		Color c = this.color != null ? this.color : Color.BLACK;
		float r = c.getRed() / 255.0f;
		float g = c.getGreen() / 255.0f;
		float b = c.getBlue() / 255.0f;
		float a = c.getAlpha() / 255.0f;
		gl.uniform4f(uColorLoc, r, g, b, a);

		gl.drawArrays(WebGLRenderingContext.TRIANGLES, 0, 6);
	}

	@Override
	public void clearRect(int x, int y, int width, int height) {
		// clear to transparent in that region: fillRect with alpha 0
		Color old = color;
		setColor(new Color(0, 0, 0, 0));
		fillRect(x, y, width, height);
		setColor(old);
	}

	@Override
	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		// For now, approximate with normal rect
		drawRect(x, y, width, height);
	}

	@Override
	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		// For now, approximate with normal rect
		fillRect(x, y, width, height);
	}

	@Override
	public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {

	}

	@Override
	public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {

	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2) {

	}

	@Override
	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {

	}

	@Override
	public void putImageData(int x, int y, ImageData data) {
		float width = data.getWidth();
		float height = data.getHeight();

		WebGLTexture tex = gl.createTexture();

		drawTexture(data, tex, x, y, (int) width, (int) height);

		gl.deleteTexture(tex);
	}

	private void drawTexture(ImageData data, WebGLTexture tex, int x, int y, int width, int height) {
		drawTexture(data.getData(), tex, SwizzleMode.ARGB_TO_RGBA, x, y, width, height);
	}

	private void drawTexture(ArrayBufferView data, WebGLTexture tex, SwizzleMode swizzleMode, int x, int y, int width, int height) {
		useTextureProgram(swizzleMode);

		gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, tex);

		gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_MIN_FILTER, WebGLRenderingContext.NEAREST);
		gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_MAG_FILTER, WebGLRenderingContext.NEAREST);
		gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_WRAP_S, WebGLRenderingContext.CLAMP_TO_EDGE);
		gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_WRAP_T, WebGLRenderingContext.CLAMP_TO_EDGE);

		gl.texImage2D(WebGLRenderingContext.TEXTURE_2D, // target
			0, // level
			WebGLRenderingContext.RGBA, // internalformat
			width, //  width
			height, // height
			0, // border
			WebGLRenderingContext.RGBA, // format
			WebGLRenderingContext.UNSIGNED_BYTE, // type
			data); // pixels

		float[] verts = {
			x, y,
			x + width, y,
			x, y + height,
			x, y + height,
			x + width, y,
			x + width, y + height
		};
		float[] uvs = {
			0f, 0f,
			1f, 0f,
			0f, 1f,
			0f, 1f,
			1f, 0f,
			1f, 1f
		};

		uploadQuadVertices(verts, uvs);

		gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, tex);

		gl.drawArrays(WebGLRenderingContext.TRIANGLES, 0, 6);
	}

	@Override
	public boolean drawImage(TImage img, int x, int y, int width, int height, TImageObserver observer) {
		if (!(img instanceof TBufferedImage)) {
			return false;
		}
		TBufferedImage bi = (TBufferedImage) img;

		WebGLTexture tex = bi.getWebglTexture();

		if (tex == null) {
			tex = gl.createTexture();
			bi.setWebglTexture(tex);
			//TODO: handle texture cleanup on image dispose
		}

		Int32Array pixels = ((TDataBufferInt) bi.getRaster().getDataBuffer()).getJSArray();
		Uint8Array arr = new Uint8Array(pixels.getBuffer(), pixels.getByteOffset(), pixels.getByteLength());

		SwizzleMode mode = getSwizzleModeForImage(bi);

//		Debug.trigger();

		drawTexture(arr, tex, mode, x, y, width, height);

//		ImageData data = bi.getImageData();
//
		//		putImageData(x, y, data);

		return true;
	}

	private SwizzleMode getSwizzleModeForImage(TBufferedImage img) {
		switch (img.getImageType()) {
			case TBufferedImage.TYPE_INT_ARGB:
				return SwizzleMode.ARGB_TO_RGBA;
			case TBufferedImage.TYPE_INT_RGB:
				return SwizzleMode.RGB_TO_RGBA;
			default:
				return SwizzleMode.NONE;
		}
	}

	@Override
	public boolean drawImage(TImage img, int x, int y, TImageObserver observer) {
		if (!(img instanceof TBufferedImage)) {
			return false;
		}
		TBufferedImage bi = (TBufferedImage) img;
		return drawImage(img, x, y, bi.getWidth(null), bi.getHeight(null), observer);
	}

	@Override
	public TFontMetrics measureText(TFont font) {
		return null;
	}

	@Override
	public void reset() {
		translateX = 0;
		translateY = 0;
		clip = null;
		gl.disable(WebGLRenderingContext.SCISSOR_TEST);
	}

	@Override
	public TShape getClip() {
		return clip;
	}

	@Override
	public void clipRect(int x, int y, int width, int height) {
		if (clip == null) {
			setClip(x, y, width, height);
		} else {
			clip = clip.intersection(new TRectangle(x, y, width, height));
			applyClip();
		}
	}

	@Override
	public void setClip(TShape clip) {
		if (clip instanceof TRectangle) {
			this.clip = (TRectangle) clip;
			applyClip();
		} else if (clip == null) {
			this.clip = null;
			gl.disable(WebGLRenderingContext.SCISSOR_TEST);
		} else {
			// non-rect clips not implemented
			throw new UnsupportedOperationException("Non-rect clip not supported in WebGL yet");
		}
	}

	@Override
	public void dispose() {
		//TODO: cleanup GL resources?
	}

	private void useColorProgram() {
		gl.useProgram(colorProgram);
		gl.uniform2f(uResolutionLocColor, (float) canvas.getWidth(), (float) canvas.getHeight());
		gl.uniform2f(uTranslationLocColor, (float) translateX, (float) translateY);

		gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, rectBuffer);
		gl.enableVertexAttribArray(aPositionLocColor);
		gl.vertexAttribPointer(aPositionLocColor, 2, WebGLRenderingContext.FLOAT, false, 0, 0);
	}

	private void useTextureProgram(SwizzleMode mode) {
		gl.useProgram(textureProgram);
		gl.uniform2f(uResolutionLocTex, (float) canvas.getWidth(), (float) canvas.getHeight());
		gl.uniform2f(uTranslationLocTex, (float) translateX, (float) translateY);

		gl.uniform1i(uSwizzleModeLoc, mode.ordinal());

		gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, quadBuffer);
		gl.enableVertexAttribArray(aPositionLocTex);
		gl.vertexAttribPointer(aPositionLocTex, 2, WebGLRenderingContext.FLOAT, false, 0, 0);

		gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, quadTexCoordBuffer);
		gl.enableVertexAttribArray(aTexCoordLocTex);
		gl.vertexAttribPointer(aTexCoordLocTex, 2, WebGLRenderingContext.FLOAT, false, 0, 0);
	}

	private void uploadRectVertices(float[] verts) {
		ArrayBuffer buf = Float32Array.fromJavaArray(verts).getBuffer();
		gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, buf, WebGLRenderingContext.STREAM_DRAW);
	}

	private void uploadQuadVertices(float[] verts, float[] uvs) {
		ArrayBuffer vertBuf = Float32Array.fromJavaArray(verts).getBuffer();
		ArrayBuffer uvBuf = Float32Array.fromJavaArray(uvs).getBuffer();

		gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, quadBuffer);
		gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, vertBuf, WebGLRenderingContext.STREAM_DRAW);

		gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, quadTexCoordBuffer);
		gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, uvBuf, WebGLRenderingContext.STREAM_DRAW);
	}

	private WebGLProgram createProgram(WebGLRenderingContext gl, String vsSource, String fsSource) {
		WebGLShader vs = compileShader(gl, WebGLRenderingContext.VERTEX_SHADER, vsSource);
		WebGLShader fs = compileShader(gl, WebGLRenderingContext.FRAGMENT_SHADER, fsSource);
		WebGLProgram program = gl.createProgram();
		gl.attachShader(program, vs);
		gl.attachShader(program, fs);
		gl.linkProgram(program);
		if (!gl.getProgramParameterb(program, WebGLRenderingContext.LINK_STATUS)) {
			String log = gl.getProgramInfoLog(program);
			throw new RuntimeException("Could not link WebGL program: " + log);
		}
		return program;
	}

	private WebGLShader compileShader(WebGLRenderingContext gl, int type, String src) {
		WebGLShader shader = gl.createShader(type);
		gl.shaderSource(shader, src);
		gl.compileShader(shader);
		if (!gl.getShaderParameterb(shader, WebGLRenderingContext.COMPILE_STATUS)) {
			String log = gl.getShaderInfoLog(shader);
			throw new RuntimeException("Could not compile shader: " + log);
		}
		return shader;
	}

	private enum SwizzleMode {
		NONE,
		ARGB_TO_RGBA,
		RGB_TO_RGBA
	}

	// Pixel-space vertex shader with translation + resolution -> NDC
	private static final String COLOR_VERTEX_SRC =
		"attribute vec2 a_position;\n" +
			"uniform vec2 u_resolution;\n" +
			"uniform vec2 u_translation;\n" +
			"void main() {\n" +
			"  vec2 pos = a_position + u_translation;\n" +
			"  vec2 zeroToOne = pos / u_resolution;\n" +
			"  vec2 zeroToTwo = zeroToOne * 2.0;\n" +
			"  vec2 clipSpace = zeroToTwo - 1.0;\n" +
			"  gl_Position = vec4(clipSpace * vec2(1, -1), 0, 1);\n" +
			"}";

	private static final String COLOR_FRAGMENT_SRC =
		"precision mediump float;\n" +
			"uniform vec4 u_color;\n" +
			"void main() {\n" +
			"  gl_FragColor = u_color;\n" +
			"}";

	private static final String TEX_VERTEX_SRC =
		"attribute vec2 a_position;\n" +
			"attribute vec2 a_texCoord;\n" +
			"uniform vec2 u_resolution;\n" +
			"uniform vec2 u_translation;\n" +
			"varying vec2 v_texCoord;\n" +
			"void main() {\n" +
			"  vec2 pos = a_position + u_translation;\n" +
			"  vec2 zeroToOne = pos / u_resolution;\n" +
			"  vec2 zeroToTwo = zeroToOne * 2.0;\n" +
			"  vec2 clipSpace = zeroToTwo - 1.0;\n" +
			"  gl_Position = vec4(clipSpace * vec2(1, -1), 0, 1);\n" +
			"  v_texCoord = a_texCoord;\n" +
			"}";

	private static final String TEX_FRAGMENT_SRC =
		"precision mediump float;\n" +
			"varying vec2 v_texCoord;\n" +
			"uniform sampler2D u_texture;\n" +
			"uniform int u_swizzleMode;\n" +
			"\n" +
			"void main() {\n" +
			"  vec4 tex = texture2D(u_texture, v_texCoord);\n" +
			"\n" +
			"  // u_swizzleMode:\n" +
			"  //   0 = plain RGBA\n" +
			"  //   1 = ARGB stored as bytes [A,R,G,B], uploaded as RGBA (tex = A,R,G,B)\n" +
			"  //       want RGBA = (R,G,B,A) = tex.gbar\n" +
			"  //   2 = INT_RGB with alpha=0 -> bytes [0,R,G,B] uploaded as RGBA\n" +
			"  //       tex = (0,R,G,B), want (R,G,B,1) = vec4(tex.gba, 1.0)\n" +
			"  if (u_swizzleMode == 0) {\n" +
			"    gl_FragColor = tex;\n" +
			"  } else if (u_swizzleMode == 1) {\n" +
			"	gl_FragColor = vec4(tex.b, tex.g, tex.r, tex.a);\n" +
			"  } else if (u_swizzleMode == 2) {\n" +
			"    gl_FragColor = vec4(tex.bgr, 1.0);\n" +
			"  } else {\n" +
			"    gl_FragColor = tex;\n" +
			"  }\n" +
			"}\n";

}
