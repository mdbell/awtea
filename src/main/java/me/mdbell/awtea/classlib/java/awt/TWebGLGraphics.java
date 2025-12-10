package me.mdbell.awtea.classlib.java.awt;

import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import me.mdbell.awtea.gl.Shaders;
import me.mdbell.awtea.instrument.Monitored;
import me.mdbell.awtea.util.JSObjectsExtensions;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.ArrayBufferView;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Uint8Array;
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
	private final WebGLUniformLocation uTransformLocColor;
	private final int aPositionLocColor;

	// uniforms / attribs for texture program
	private final WebGLUniformLocation uSwizzleModeLoc;
	private final WebGLUniformLocation uResolutionLocTex;
	private final WebGLUniformLocation uTransformLocTex;

	private final int aPositionLocTex;
	private final int aTexCoordLocTex;

	private final WebGLBuffer quadBuffer;
	private final WebGLBuffer quadTexCoordBuffer;

	// second copy of color for blit ops, so we don't
	// mess up the main color state during batching
	private Color blitColor;

	private final WebGLFramebuffer backbufferFbo;
	private final WebGLTexture backbufferTex;
	private int backbufferWidth;
	private int backbufferHeight;

	private transient WebGLProgramType currentProgram = WebGLProgramType.NONE;

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
		this.uTransformLocColor = gl.getUniformLocation(colorProgram, "u_transform");

		// texture program locations
		gl.useProgram(textureProgram);
		this.aPositionLocTex = gl.getAttribLocation(textureProgram, "a_position");
		this.aTexCoordLocTex = gl.getAttribLocation(textureProgram, "a_texCoord");
		this.uResolutionLocTex = gl.getUniformLocation(textureProgram, "u_resolution");
		this.uTransformLocTex = gl.getUniformLocation(textureProgram, "u_transform");
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

	private TWebGLGraphics(TWebGLGraphics other) {
		super(other);
		this.gl = other.gl;
		this.colorProgram = other.colorProgram;
		this.textureProgram = other.textureProgram;
		this.rectBuffer = other.rectBuffer;
		this.quadBuffer = other.quadBuffer;
		this.quadTexCoordBuffer = other.quadTexCoordBuffer;

		this.uResolutionLocColor = other.uResolutionLocColor;
		this.uColorLoc = other.uColorLoc;
		this.uTransformLocColor = other.uTransformLocColor;
		this.aPositionLocColor = other.aPositionLocColor;
		this.uSwizzleModeLoc = other.uSwizzleModeLoc;
		this.uResolutionLocTex = other.uResolutionLocTex;
		this.uTransformLocTex = other.uTransformLocTex;
		this.aPositionLocTex = other.aPositionLocTex;
		this.aTexCoordLocTex = other.aTexCoordLocTex;
		this.backbufferFbo = other.backbufferFbo;
		this.backbufferTex = other.backbufferTex;
		this.backbufferWidth = other.backbufferWidth;
		this.backbufferHeight = other.backbufferHeight;

		this.blitColor = other.blitColor;
	}

	@Override
	public void onCanvasResize(int width, int height) {
		if (width == this.backbufferWidth && height == this.backbufferHeight) {
			return;
		}
		resizeBackbuffer(width, height);
	}

	@Override
	public TGraphics create() {
		return new TWebGLGraphics(this);
	}

	@Override
	public void setClip(int x, int y, int width, int height) {
		super.setClip(x, y, width, height);
		if (width <= 0 || height <= 0) {
			gl.disable(WebGLRenderingContext.SCISSOR_TEST);
			return;
		}
		applyClip();
	}

	@Override
	public void translate(int deltaX, int deltaY) {
		super.translate(deltaX, deltaY); // updates the AffineTransform
		applyClip();
	}

	private void applyClip() {
		if (clip == null) {
			gl.disable(WebGLRenderingContext.SCISSOR_TEST);
			return;
		}
		gl.enable(WebGLRenderingContext.SCISSOR_TEST);

		int tx = getTx();
		int ty = getTy();
		int h = backbufferHeight;

		int cx = clip.x + tx;
		int cy = clip.y + ty;

		gl.scissor(cx, h - (cy + clip.height), clip.width, clip.height);
	}

	@Override
	public void setXORMode(Color c1) {

	}

	@Override
	public void setPaintMode() {

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
		drawRect(x, y, width, height);
	}

	@Override
	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
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
		super.reset();
		gl.disable(WebGLRenderingContext.SCISSOR_TEST);
	}

	@Override
	public void clipRect(int x, int y, int width, int height) {
		super.clipRect(x, y, width, height);
		if (clip == null) {
			gl.disable(WebGLRenderingContext.SCISSOR_TEST);
		} else {
			applyClip();
		}
	}


	@Override
	public void setClip(TShape clip) {
		super.setClip(clip);
		if (clip == null) {
			gl.disable(WebGLRenderingContext.SCISSOR_TEST);
		} else {
			applyClip();
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

		// identity mat3
		float[] identity = {
			1f, 0f, 0f,
			0f, 1f, 0f,
			0f, 0f, 1f
		};
		gl.uniformMatrix3fv(uTransformLocTex, false,
			Float32Array.fromJavaArray(identity));

		// full-screen quad

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

		Uint8Array arr = img.getPixelBytes();


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
		int tx = getTx();
		int ty = getTy();
		int h = backbufferHeight;

		int cx = x + tx;
		int cy = y + ty;

		gl.enable(WebGLRenderingContext.SCISSOR_TEST);
		gl.scissor(cx, h - (cy + height), width, height);
		gl.clearColor(0f, 0f, 0f, 0f); // or background color
		gl.clear(WebGLRenderingContext.COLOR_BUFFER_BIT);
		applyClip(); // restore previous clip
	}

	protected void resizeBackbuffer(int width, int height) {
		this.backbufferWidth = width;
		this.backbufferHeight = height;

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
	}

	private float[] getTransformMatrix3() {
		TAffineTransform t = transform;

		float m00 = (float) t.getScaleX();
		float m01 = (float) t.getShearX();
		float m02 = (float) t.getTranslateX();

		float m10 = (float) t.getShearY();
		float m11 = (float) t.getScaleY();
		float m12 = (float) t.getTranslateY();

		// WebGL uses column-major order for mat3 uniforms
		return new float[]{
			m00, m10, 0f,
			m01, m11, 0f,
			m02, m12, 1f
		};
	}

	// WebGL programs

	private void useColorProgram() {

		if (currentProgram != WebGLProgramType.COLOR) {
			gl.useProgram(colorProgram);
			currentProgram = WebGLProgramType.COLOR;
		}

		gl.uniform2f(uResolutionLocColor,
			(float) backbufferWidth,
			(float) backbufferHeight);

		float[] m = getTransformMatrix3();
		gl.uniformMatrix3fv(uTransformLocColor, false,
			Float32Array.fromJavaArray(m));

		gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, rectBuffer);
		gl.enableVertexAttribArray(aPositionLocColor);
		gl.vertexAttribPointer(aPositionLocColor, 2,
			WebGLRenderingContext.FLOAT,
			false, 0, 0);
	}

	private void useTextureProgram(TBufferedImage.SwizzleMode mode) {

		if (currentProgram != WebGLProgramType.TEXTURE) {
			gl.useProgram(textureProgram);
			currentProgram = WebGLProgramType.TEXTURE;
		}

		gl.uniform2f(uResolutionLocTex,
			(float) backbufferWidth,
			(float) backbufferHeight);

		float[] m = getTransformMatrix3();
		gl.uniformMatrix3fv(uTransformLocTex, false,
			Float32Array.fromJavaArray(m));

		gl.uniform1i(uSwizzleModeLoc, mode.ordinal());

		gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, quadBuffer);
		gl.enableVertexAttribArray(aPositionLocTex);
		gl.vertexAttribPointer(aPositionLocTex, 2, WebGLRenderingContext.FLOAT, false, 0, 0);

		gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, quadTexCoordBuffer);
		gl.enableVertexAttribArray(aTexCoordLocTex);
		gl.vertexAttribPointer(aTexCoordLocTex, 2,
			WebGLRenderingContext.FLOAT,
			false, 0, 0);
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

	private enum WebGLProgramType {
		NONE,
		COLOR,
		TEXTURE
	}


	// Pixel-space vertex shader with translation + resolution -> NDC
	private static final String COLOR_VERTEX_SRC = Shaders.colorVertex();

	private static final String COLOR_FRAGMENT_SRC = Shaders.colorFragment();

	private static final String TEX_VERTEX_SRC = Shaders.textureVertex();

	private static final String TEX_FRAGMENT_SRC = Shaders.textureFragment();

}
