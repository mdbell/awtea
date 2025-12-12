package me.mdbell.awtea.gfx.webgl;

import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceBackend;
import me.mdbell.awtea.gl.Shaders;
import me.mdbell.awtea.util.jso.JSRecord;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.webgl.*;

public final class WebGLSurfaceBackend implements SurfaceBackend {

	private final HTMLCanvasElement element;
	final WebGL2RenderingContext gl;

	//TODO: maybe refactor this to move programs to their own class (a.la. WebGLProgramManager/WebGLRenderer)
	// the element would live there, and the backend would just create surfaces with references to the renderer

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
	private final WebGLUniformLocation uTextureLoc;

	private final int aPositionLocTex;
	private final int aTexCoordLocTex;

	private final WebGLBuffer quadBuffer;
	private final WebGLBuffer quadTexCoordBuffer;

	// state tracking

	private WebGLProgramType currentProgram = WebGLProgramType.NONE;

	private final Float32Array rectBufferArray = new Float32Array(12);
	private final ArrayBuffer rectArrayBuffer = rectBufferArray.getBuffer();

	public WebGLSurfaceBackend(HTMLCanvasElement element) {
		this.element = element;

		// Maybe figure out how to pass in options later?
		JSRecord options = JSRecord.create();
		options.put("alpha", false);
		this.gl = (WebGL2RenderingContext) element.getContext("webgl2", options);

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
		this.uTextureLoc = gl.getUniformLocation(textureProgram, "u_texture");
		gl.uniform1i(uTextureLoc, 0); // texture unit 0

		// ---- buffers ----
		// simple rect buffer: two triangles in [0,0]-[1,1] space
		rectBuffer = gl.createBuffer();

		quadBuffer = gl.createBuffer();
		quadTexCoordBuffer = gl.createBuffer();
	}

	void setColor(float r, float g, float b, float a) {
		gl.uniform4f(uColorLoc, r, g, b, a);
	}

	void setRectBuffer(float x, float y, float width, float height) {

		// two triangles forming a rectangle
		// first triangle (x,y)-(x+width,y)-(x,y+height)
		rectBufferArray.set(0, x);
		rectBufferArray.set(1, y);
		rectBufferArray.set(2, x + width);
		rectBufferArray.set(3, y);
		rectBufferArray.set(4, x);
		rectBufferArray.set(5, y + height);

		// second triangle (x,y+height)-(x+width,y)-(x+width,y+height)
		rectBufferArray.set(6, x);
		rectBufferArray.set(7, y + height);
		rectBufferArray.set(8, x + width);
		rectBufferArray.set(9, y);
		rectBufferArray.set(10, x + width);
		rectBufferArray.set(11, y + height);

		uploadRectVertices();
	}

	void uploadRectVertices() {
		gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, rectArrayBuffer, WebGLRenderingContext.STREAM_DRAW);
	}

	void uploadQuadVertices(float[] verts, float[] uvs) {
		ArrayBuffer vertBuf = Float32Array.fromJavaArray(verts).getBuffer();
		ArrayBuffer uvBuf = Float32Array.fromJavaArray(uvs).getBuffer();

		gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, quadBuffer);
		gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, vertBuf, WebGLRenderingContext.STREAM_DRAW);

		gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, quadTexCoordBuffer);
		gl.bufferData(WebGLRenderingContext.ARRAY_BUFFER, uvBuf, WebGLRenderingContext.STREAM_DRAW);
	}

	@Override
	public Surface createCompatibleSurface(int width, int height, int bufferedImageType) {
		return new WebGLSurface(this, width, height, true); // TODO: get forScreen properly, and pass in the type
	}

	@Override
	public Surface createCompatibleSurface(Object cm, Object raster, boolean isRasterPremultiplied, int bufferedImageType) {
		return null;
	}

	float[] identityTransform() {
		return new float[]{
			1f, 0f, 0f,
			0f, 1f, 0f,
			0f, 0f, 1f
		};
	}

	protected void useColorProgram(int width, int height, Float32Array transform) {
		if (currentProgram != WebGLProgramType.COLOR) {
			gl.useProgram(colorProgram);
			currentProgram = WebGLProgramType.COLOR;
		}

		gl.uniform2f(uResolutionLocColor,
			(float) width,
			(float) height);
		gl.uniformMatrix3fv(uTransformLocColor, false, transform);

		gl.bindBuffer(WebGLRenderingContext.ARRAY_BUFFER, rectBuffer);
		gl.enableVertexAttribArray(aPositionLocColor);
		gl.vertexAttribPointer(aPositionLocColor, 2,
			WebGLRenderingContext.FLOAT,
			false, 0, 0);
	}

	void useTextureProgram(SwizzleMode mode, int width, int height, Float32Array transform) {

		if (currentProgram != WebGLProgramType.TEXTURE) {
			gl.useProgram(textureProgram);
			currentProgram = WebGLProgramType.TEXTURE;
		}

		gl.uniform2f(uResolutionLocTex,
			(float) width,
			(float) height);

		gl.uniformMatrix3fv(uTransformLocTex, false,
			transform);

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

	// TODO: it would be nice to remove this and just use Surface pixel format directly
	protected enum SwizzleMode {
		NONE,
		ARGB_TO_RGBA,
		RGB_TO_RGBA,
		BGR_TO_ABGR
	}

	// Shaders

	private static final String COLOR_VERTEX_SRC = Shaders.colorVertex();

	private static final String COLOR_FRAGMENT_SRC = Shaders.colorFragment();

	private static final String TEX_VERTEX_SRC = Shaders.textureVertex();

	private static final String TEX_FRAGMENT_SRC = Shaders.textureFragment();
}
