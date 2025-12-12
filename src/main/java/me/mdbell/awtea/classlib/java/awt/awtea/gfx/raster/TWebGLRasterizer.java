package me.mdbell.awtea.classlib.java.awt.awtea.gfx.raster;

import me.mdbell.awtea.classlib.java.awt.TRectangle;
import me.mdbell.awtea.classlib.java.awt.TShape;
import me.mdbell.awtea.classlib.java.awt.awtea.gfx.TCachedTexture;
import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;
import me.mdbell.awtea.classlib.java.awt.image.TBufferedImage;
import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.gfx.SurfaceCommand;
import me.mdbell.awtea.impl.Debug;
import me.mdbell.awtea.instrument.Monitored;
import me.mdbell.awtea.util.JSObjectsExtensions;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.ArrayBufferView;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Uint8ClampedArray;
import org.teavm.jso.webgl.*;

import java.awt.*;
import java.util.List;

@Monitored.AllMethods
public class TWebGLRasterizer implements Rasterizer {

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

	private final WebGLFramebuffer backbufferFbo;
	private final WebGLTexture backbufferTex;
	private int backbufferWidth;
	private int backbufferHeight;

	private final TAffineTransform transform = new TAffineTransform();
	private Color color, background;

	private transient WebGLProgramType currentProgram = WebGLProgramType.NONE;

	private TRectangle clip = null;

	public TWebGLRasterizer(HTMLCanvasElement canvas) {
		this(JSObjectsExtensions.getWebGL2Context(canvas), canvas);
	}

	public TWebGLRasterizer(WebGL2RenderingContext gl, HTMLCanvasElement canvas) {
		this.gl = gl;

		// basic GL state
		gl.enable(WebGLRenderingContext.BLEND);
		gl.blendFunc(WebGLRenderingContext.SRC_ALPHA, WebGLRenderingContext.ONE_MINUS_SRC_ALPHA);

		// ---- build shader programs ----
		//this.colorProgram = createProgram(gl, COLOR_VERTEX_SRC, COLOR_FRAGMENT_SRC);
		//	this.textureProgram = createProgram(gl, TEX_VERTEX_SRC, TEX_FRAGMENT_SRC);
		this.colorProgram = null;
		this.textureProgram = null;

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

	private TWebGLRasterizer(TWebGLRasterizer other) {
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
		this.transform.setTransform(other.transform);
		this.color = other.color;
		this.background = other.background;
		this.clip = other.clip;
	}

	@Override
	public Rasterizer create() {
		return new TWebGLRasterizer(this);
	}

	@Override
	public void reset() {
		clip = null;
		gl.disable(WebGLRenderingContext.SCISSOR_TEST);
	}

	@Override
	public void onResize(int width, int height) {
		if (width == this.backbufferWidth && height == this.backbufferHeight) {
			return;
		}
		resizeBackbuffer(width, height);
	}

	@Override
	public void rasterizeCommands(List<SurfaceCommand> ops) {
//		Debug.trigger();
		// we draw to a framebuffer so we can have incremental updates without
		// clearing the whole canvas each time
		gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, backbufferFbo);

		// NOTE: Do _not_ clear the framebuffer here; we want to preserve existing content

		for (SurfaceCommand op : ops) {
			switch (op.type) {
				case BLIT_IMAGE:
					drawImageImpl(op.obj, op.arg1, op.arg2, op.arg3, op.arg4);
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
				case SET_TRANSFORM:
					this.transform.setTransform((TAffineTransform) op.obj);
					break;
				case SET_COLOR:
					if (op.arg1 == 1) {
						this.background = (Color) op.obj;
					} else {
						this.color = (Color) op.obj;
					}
					break;
				case SET_CLIP_RECT:
					setClip((TShape) op.obj);
					break;
				default:
					System.err.println("Unsupported blit operation: " + op.type);
					break;
			}
		}

		// unbind framebuffer to render to canvas
		gl.bindFramebuffer(WebGLRenderingContext.FRAMEBUFFER, null);

		useTextureProgram(SwizzleMode.NONE);
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

	public void setClip(TShape clip) {
		this.clip = clip == null ? null : clip.getBounds();
		if (clip == null) {
			gl.disable(WebGLRenderingContext.SCISSOR_TEST);
		} else {
			applyClip();
		}
	}

	private void applyClip() {
		if (clip == null) {
			gl.disable(WebGLRenderingContext.SCISSOR_TEST);
			return;
		}
		gl.enable(WebGLRenderingContext.SCISSOR_TEST);

		int tx = (int) transform.getTranslateX();
		int ty = (int) transform.getTranslateY();
		int h = backbufferHeight;

		int cx = clip.x + tx;
		int cy = clip.y + ty;

		gl.scissor(cx, h - (cy + clip.height), clip.width, clip.height);
	}

	// Rasterization methods

	private void drawImageImpl(Object img, int x, int y, int width, int height) {

		WebGLTextureWrapper wrapper = cacheTexture(img);


		drawTexture(wrapper, x, y, width, height);
	}

	private WebGLTextureWrapper cacheTexture(Object imgObj) {
		TBufferedImage img = (TBufferedImage) imgObj;
		if (img.texture != null) {
			return (WebGLTextureWrapper) img.texture;
		}
		WebGLTexture tex = gl.createTexture();
		SwizzleMode swizzleMode = getSwizzleMode(img);
		if (swizzleMode == SwizzleMode.BGR_TO_ARGB) {
			Debug.trigger();
		}
		Uint8ClampedArray pixels = img.getPixelBytes();
		return (WebGLTextureWrapper) (img.texture = new WebGLTextureWrapper(pixels, tex, swizzleMode, img.getWidth(), img.getHeight()));
	}

	private void drawTexture(WebGLTextureWrapper wrapper, int x, int y, int width, int height) {

		WebGLTexture tex = wrapper.texture;
		SwizzleMode swizzleMode = wrapper.swizzleMode;
		Uint8ClampedArray data = wrapper.arr;

		useTextureProgram(swizzleMode);

		gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, tex);

		gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_MIN_FILTER, WebGLRenderingContext.NEAREST);
		gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_MAG_FILTER, WebGLRenderingContext.NEAREST);
		gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_WRAP_S, WebGLRenderingContext.CLAMP_TO_EDGE);
		gl.texParameteri(WebGLRenderingContext.TEXTURE_2D, WebGLRenderingContext.TEXTURE_WRAP_T, WebGLRenderingContext.CLAMP_TO_EDGE);

		gl.texImage2D(WebGLRenderingContext.TEXTURE_2D, // target
			0, // level
			WebGLRenderingContext.RGBA, // internalformat
			wrapper.width, //  width
			wrapper.height, // height
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
			x, y,                     // 0, 1
			x + width, y,             // 2, 3
			x, y + height,             // 4, 5
			x, y + height,             // 6, 7
			x + width, y,             // 8, 9
			x + width, y + height    // 10,11
		};
		uploadRectVertices(verts);

		Color c = this.color != null ? this.color : new Color(0, 0, 0, 255);
		float r = c.getRed() / 255.0f;
		float g = c.getGreen() / 255.0f;
		float b = c.getBlue() / 255.0f;
		float a = c.getAlpha() / 255.0f;
		gl.uniform4f(uColorLoc, r, g, b, a);

		gl.drawArrays(WebGLRenderingContext.TRIANGLES, 0, 6);
	}

	private void clearRectImpl(int x, int y, int width, int height) {
		int tx = (int) transform.getTranslateX();
		int ty = (int) transform.getTranslateY();
		int h = backbufferHeight;

		int cx = x + tx;
		int cy = y + ty;

		gl.enable(WebGLRenderingContext.SCISSOR_TEST);
		gl.scissor(cx, h - (cy + height), width, height);
		if (background != null) {
			float r = background.getRed() / 255.0f;
			float g = background.getGreen() / 255.0f;
			float b = background.getBlue() / 255.0f;
			float a = background.getAlpha() / 255.0f;
			gl.clearColor(r, g, b, a);
		} else {
			gl.clearColor(0f, 0f, 0f, 0f);
		}
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


	// helpers

	private SwizzleMode getSwizzleMode(TBufferedImage img) {
		switch (img.getImageType()) {
			case TBufferedImage.TYPE_INT_ARGB:
				return SwizzleMode.ARGB_TO_RGBA;
			case TBufferedImage.TYPE_INT_RGB:
				return SwizzleMode.RGB_TO_RGBA;
			case TBufferedImage.TYPE_INT_BGR:
				return SwizzleMode.BGR_TO_ARGB;
			default:
				return SwizzleMode.NONE;
		}
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

	private void useTextureProgram(SwizzleMode mode) {

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

	// state managment

	private enum WebGLProgramType {
		NONE,
		COLOR,
		TEXTURE
	}

	private enum SwizzleMode {
		NONE,
		ARGB_TO_RGBA,
		RGB_TO_RGBA,
		BGR_TO_ARGB
	}

	public class WebGLTextureWrapper implements TCachedTexture {
		public WebGLTexture texture;
		private final SwizzleMode swizzleMode;
		private final Uint8ClampedArray arr;
		private final int width;
		private final int height;

		public WebGLTextureWrapper(Uint8ClampedArray pixels, WebGLTexture texture, SwizzleMode mode, int width, int height) {
			this.arr = pixels;
			this.texture = texture;
			this.swizzleMode = mode;
			this.width = width;
			this.height = height;
		}

		public void delete() {
			if (texture != null) {
				gl.deleteTexture(texture);
				texture = null;
			}
		}
	}
}
