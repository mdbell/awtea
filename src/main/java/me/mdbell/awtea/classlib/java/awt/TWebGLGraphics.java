package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import me.mdbell.awtea.classlib.java.awt.image.TDataBufferInt;
import me.mdbell.awtea.gl.Shaders;
import me.mdbell.awtea.instrument.Monitored;
import me.mdbell.awtea.util.JSObjectsExtensions;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.typedarrays.*;
import org.teavm.jso.webgl.*;

import java.awt.*;
import java.util.List;

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

	// simple rectangular clip; null = no clip
	private TRectangle clip;

	// second copy of color for blit ops, so we don't
	// mess up the main color state during batching
	private Color blitColor;

	private final WebGLFramebuffer backbufferFbo;
	private final WebGLTexture backbufferTex;
	private int backbufferWidth;
	private int backbufferHeight;

	public TWebGLGraphics(HTMLCanvasElement canvas) {
		this(JSObjectsExtensions.getWebGL2Context(canvas), canvas);
	}

	public TWebGLGraphics(WebGL2RenderingContext gl, HTMLCanvasElement canvas) {
		super(canvas);
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

		backbufferWidth = canvas.getWidth();
		backbufferHeight = canvas.getHeight();

		backbufferTex = gl.createTexture();
		gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, backbufferTex);
		gl.texImage2D(WebGLRenderingContext.TEXTURE_2D,
			0,
			WebGLRenderingContext.RGBA,
			backbufferWidth,
			backbufferHeight,
			0,
			WebGLRenderingContext.RGBA,
			WebGLRenderingContext.UNSIGNED_BYTE,
			(ArrayBufferView) null);
		//TODO: figure out what these mean
		gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_MIN_FILTER, WebGLRenderingContext.NEAREST);
		gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_MAG_FILTER, WebGLRenderingContext.NEAREST);
		gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_WRAP_S, WebGLRenderingContext.CLAMP_TO_EDGE);
		gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_WRAP_T, WebGLRenderingContext.CLAMP_TO_EDGE);

		backbufferFbo = gl.createFramebuffer();
		gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, backbufferFbo);
		gl.framebufferTexture2D(WebGLRenderingContext.FRAMEBUFFER,
			WebGLRenderingContext.COLOR_ATTACHMENT0,
			WebGLRenderingContext.TEXTURE_2D,
			backbufferTex,
			0);

		gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, null);

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

	private void drawTexture(ArrayBufferView data, WebGLTexture tex, TBufferedImage.SwizzleMode swizzleMode, int x, int y, int width, int height) {
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
		// though we dispose of webgl texture on TBufferedImage finalize
	}

	@Override
	protected void performBlit(List<BlitOp> ops) {

		// we draw to a framebuffer so we can have incremental updates without
		// clearing the whole canvas each time
		gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, backbufferFbo);

		// NOTE: Do _not_ clear the framebuffer here; we want to preserve existing content

		for (BlitOp op : ops) {
			switch (op.type) {
				case BLIT_IMAGE:
					drawImageImpl((TBufferedImage) op.obj, op.arg1, op.arg2, op.arg3, op.arg4);
					break;
				case DRAW_RECT:
					drawRectImpl(op.arg1, op.arg2, op.arg3, op.arg4);
					break;
				case FILL_RECT:
					fillRectImpl(op.arg1, op.arg2, op.arg3, op.arg4);
					break;
				case CLEAR_RECT:
					clearRectImpl(op.arg1, op.arg2, op.arg3, op.arg4);
					break;
				case SET_COLOR:
					this.blitColor = (Color) op.obj;
					break;
				default:
					System.err.println("Unsupported blit operation: " + op.type);
					break;
			}
		}

		// unbind framebuffer to render to canvas
		gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, null);

		useTextureProgram(TBufferedImage.SwizzleMode.NONE);
		gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, backbufferTex);

		float[] verts = {
			0, 0,
			backbufferWidth, 0,
			0, backbufferHeight,
			0, backbufferHeight,
			backbufferWidth, 0,
			backbufferWidth, backbufferHeight
		};

		float[] uvs = {
			0f, 1f,
			1f, 1f,
			0f, 0f,
			0f, 0f,
			1f, 1f,
			1f, 0f
		};

		uploadQuadVertices(verts, uvs);
		gl.drawArrays(WebGLRenderingContext.TRIANGLES, 0, 6);
	}

	private void drawImageImpl(TBufferedImage img, int x, int y, int width, int height) {

		WebGLTexture tex = img.getWebglTexture();

		if (tex == null) {
			tex = gl.createTexture();
			img.setWebglTexture(gl, tex);
		}

		Int32Array pixels = ((TDataBufferInt) img.getRaster().getDataBuffer()).getJSArray();
		Uint8Array arr = new Uint8Array(pixels.getBuffer(), pixels.getByteOffset(), pixels.getByteLength());


		drawTexture(arr, tex, img.getSwizzle(), x, y, width, height);
	}

	private void drawRectImpl(int x, int y, int width, int height) {
		// draw four thin rectangles
		fillRectImpl(x, y, width, 1);
		fillRectImpl(x, y + height - 1, width, 1);
		fillRectImpl(x, y, 1, height);
		fillRectImpl(x + width - 1, y, 1, height);
	}

	private void fillRectImpl(int x, int y, int width, int height) {
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

		Color c = this.blitColor != null ? this.blitColor : Color.BLACK;
		float r = c.getRed() / 255.0f;
		float g = c.getGreen() / 255.0f;
		float b = c.getBlue() / 255.0f;
		float a = c.getAlpha() / 255.0f;
		gl.uniform4f(uColorLoc, r, g, b, a);

		gl.drawArrays(WebGLRenderingContext.TRIANGLES, 0, 6);
	}

	private void clearRectImpl(int x, int y, int width, int height) {
		int cx = x + translateX;
		int cy = y + translateY;
		int h = canvas.getHeight();
		gl.enable(WebGLRenderingContext.SCISSOR_TEST);
		gl.scissor(cx, h - (cy + height), width, height);
		gl.clearColor(0f, 0f, 0f, 0f);
		gl.clear(WebGLRenderingContext.COLOR_BUFFER_BIT);
		applyClip();
	}

	private void useColorProgram() {
		gl.useProgram(colorProgram);
		gl.uniform2f(uResolutionLocColor, (float) canvas.getWidth(), (float) canvas.getHeight());
		gl.uniform2f(uTranslationLocColor, (float) translateX, (float) translateY);

		gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, rectBuffer);
		gl.enableVertexAttribArray(aPositionLocColor);
		gl.vertexAttribPointer(aPositionLocColor, 2, WebGLRenderingContext.FLOAT, false, 0, 0);
	}

	private void useTextureProgram(TBufferedImage.SwizzleMode mode) {
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


	// Pixel-space vertex shader with translation + resolution -> NDC
	private static final String COLOR_VERTEX_SRC = Shaders.colorVertex();

	private static final String COLOR_FRAGMENT_SRC = Shaders.colorFragment();

	private static final String TEX_VERTEX_SRC = Shaders.textureVertex();

	private static final String TEX_FRAGMENT_SRC = Shaders.textureFragment();

}
