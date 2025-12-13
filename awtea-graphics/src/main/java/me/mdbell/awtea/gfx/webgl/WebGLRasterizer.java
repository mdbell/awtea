package me.mdbell.awtea.gfx.webgl;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import me.mdbell.awtea.gfx.Rasterizer;
import me.mdbell.awtea.gfx.Surface;
import me.mdbell.awtea.gfx.SurfaceCommand;
import me.mdbell.awtea.gfx.SurfaceContainer;
import me.mdbell.awtea.instrument.Monitored;
import org.teavm.jso.typedarrays.Float32Array;
import org.teavm.jso.typedarrays.Uint8ClampedArray;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLFramebuffer;
import org.teavm.jso.webgl.WebGLRenderingContext;
import org.teavm.jso.webgl.WebGLTexture;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.List;

@Monitored.AllMethods
class WebGLRasterizer implements Rasterizer {

	private static final Logger log = LoggerFactory.getLogger(WebGLRasterizer.class);

	private final WebGLSurfaceBackend backend;
	private final WebGL2RenderingContext gl;
	private final WebGLSurface surface;
	private final WebGLFramebuffer framebuffer;

	private final AffineTransform transform = new AffineTransform();
	private transient final Float32Array transformArray = new Float32Array(9);
	private Rectangle clip = null;

	private Color foreground = Color.WHITE;
	private Color background = Color.BLACK;

	private boolean pushToScreen = false;

	// identity transform array for pushing to screen
	private static final Float32Array identityTransformArray = Float32Array.fromJavaArray(new float[]{
		1f, 0f, 0f,
		0f, 1f, 0f,
		0f, 0f, 1f
	});

	WebGLRasterizer(WebGLSurfaceBackend backend, WebGLSurface surface, boolean pushToScreen) {
		this.backend = backend;
		this.framebuffer = surface.framebuffer;
		this.gl = backend.gl;
		this.surface = surface;
		transform.setToIdentity();
		updateTransformFloats(transform);
		this.clip = new Rectangle(0, 0, surface.getWidth(), surface.getHeight());
		this.pushToScreen = pushToScreen;
	}

	private WebGLRasterizer(WebGLRasterizer other) {
		this.surface = other.surface;
		this.framebuffer = other.framebuffer;
		this.backend = other.backend;
		this.gl = other.gl;
		this.transform.setTransform(other.transform);
		this.foreground = other.foreground;
		this.background = other.background;
		this.clip = other.clip;
		updateTransformFloats(this.transform);
	}

	@Override
	public Rasterizer create() {
		return new WebGLRasterizer(this);
	}

	@Override
	public void reset() {
		this.transform.setToIdentity();
		updateTransformFloats(transform);
		this.foreground = Color.WHITE;
		this.background = Color.BLACK;
		this.clip = new Rectangle(0, 0, surface.getWidth(), surface.getHeight());
	}

	private void fillRect(float x, float y, float width, float height) {
		int h = surface.getHeight();
		y = h - (y + height); // flip Y coordinate
		useColorProgram();

		setColor(foreground);
		backend.setRectBuffer(x, y, width, height);

		gl.drawArrays(WebGLRenderingContext.TRIANGLES, 0, 6);

		surface.markDirty();
	}

	private void drawRect(float x, float y, float width, float height, float lineWidth) {
		// draw 4 filled rects to make the border
		fillRect(x, y, width, lineWidth); // top
		fillRect(x, y + height - lineWidth, width, lineWidth); // bottom
		fillRect(x, y + lineWidth, lineWidth, height - 2 * lineWidth); // left
		fillRect(x + width - lineWidth, y + lineWidth, lineWidth, height - 2 * lineWidth); // right

		surface.markDirty();
	}

	private void setColor(Color c) {
		float r = c.getRed() / 255.0f;
		float g = c.getGreen() / 255.0f;
		float b = c.getBlue() / 255.0f;
		float a = c.getAlpha() / 255.0f;

		backend.setColor(r, g, b, a);
	}

	private void useColorProgram() {
		int width = surface.getWidth();
		int height = surface.getHeight();

		backend.useColorProgram(width, height, this.transformArray);
	}

	private void applyClip() {
		if (clip == null) {
			gl.disable(WebGLRenderingContext.SCISSOR_TEST);
			return;
		}
		gl.enable(WebGLRenderingContext.SCISSOR_TEST);

		int tx = (int) transform.getTranslateX();
		int ty = (int) transform.getTranslateY();
		int h = surface.getHeight();

		int cx = clip.x + tx;
		int cy = clip.y + ty;

		gl.scissor(cx, h - (cy + clip.height), clip.width, clip.height);
	}

	private void updateTransformFloats(AffineTransform transform) {
		// Matrix needs to be in column-major order:
		// ---------------
		// | m00 m10  0  |
		// | m01 m11  0  |
		// | m02 m12  1  |
		// ---------------
		transformArray.set(0, (float) transform.getScaleX());
		transformArray.set(1, (float) transform.getShearY());
		transformArray.set(2, 0f);
		transformArray.set(3, (float) transform.getShearX());
		transformArray.set(4, (float) transform.getScaleY());
		transformArray.set(5, 0f);
		transformArray.set(6, (float) transform.getTranslateX());
		transformArray.set(7, (float) transform.getTranslateY());
		transformArray.set(8, 1f);
	}

	private void clearRect(int x, int y, int width, int height) {
		int tx = (int) transform.getTranslateX();
		int ty = (int) transform.getTranslateY();
		int h = surface.getHeight();

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

		surface.markDirty();
	}

	private void setClip(Shape shape) {
		if (shape == null) {
			this.clip = null;
			gl.disable(WebGLRenderingContext.SCISSOR_TEST);
			return;
		}
		this.clip = shape.getBounds();
		applyClip();
	}

	private void drawImage(Object img, int x, int y, int width, int height) {
		if (img instanceof WebGLSurface) {
			drawWebGLSurface((WebGLSurface) img, x, y, width, height);
		} else if (img instanceof Surface) {
			// generic Surface drawing (not optimized - gets copied into GPU texture and then drawn)
			Surface surface = (Surface) img;
			drawSurface(surface, x, y, width, height);
		} else {
			System.err.println("WebGLRasterizer: drawImage: Unsupported image type: " + img.getClass().getName());
		}
	}

	private void drawWebGLSurface(WebGLSurface img, int x, int y, int width, int height) {
		WebGLTexture other = img.texture;

		// no swizzling needed when drawing from one WebGLSurface to another
		drawTexture(other, WebGLSurfaceBackend.SwizzleMode.NONE, x, y, img.getWidth(), img.getHeight(), width, height, null);
	}

	private void drawSurface(Surface surface, int x, int y, int width, int height) {
		WebGLTexture tmp = gl.createTexture();
		gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, tmp);
		gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
			WebGLRenderingContext.TEXTURE_WRAP_S,
			WebGLRenderingContext.CLAMP_TO_EDGE);
		gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
			WebGLRenderingContext.TEXTURE_WRAP_T,
			WebGLRenderingContext.CLAMP_TO_EDGE);
		gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
			WebGLRenderingContext.TEXTURE_MIN_FILTER,
			WebGLRenderingContext.LINEAR);
		gl.texParameteri(WebGLRenderingContext.TEXTURE_2D,
			WebGLRenderingContext.TEXTURE_MAG_FILTER,
			WebGLRenderingContext.LINEAR);
		WebGLSurfaceBackend.SwizzleMode mode = determineSwizzleMode(surface);
		try {
			drawTexture(tmp, mode, x, y, surface.getWidth(), surface.getHeight(), width, height, surface.getPixelData());
		} finally {
			// clean up temporary texture
			gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, null);
			gl.deleteTexture(tmp);
		}
	}

	private WebGLSurfaceBackend.SwizzleMode determineSwizzleMode(Surface surface) {
		switch (surface.getFormat()) {
			case Surface.FORMAT_INT_ARGB:
				return WebGLSurfaceBackend.SwizzleMode.ARGB_TO_RGBA;
			case Surface.FORMAT_INT_RGB:
				return WebGLSurfaceBackend.SwizzleMode.RGB_TO_RGBA;
			case Surface.FORMAT_INT_BGR:
				return WebGLSurfaceBackend.SwizzleMode.BGR_TO_ABGR;
			case Surface.FORMAT_INT_RGBA:
				return WebGLSurfaceBackend.SwizzleMode.NONE;
			default:
				System.err.println("WebGLRasterizer: Unknown surface format: " + surface.getFormat() +
					", defaulting to no swizzling");
				return WebGLSurfaceBackend.SwizzleMode.NONE;
		}
	}

	private void drawTexture(WebGLTexture texture, WebGLSurfaceBackend.SwizzleMode mode,
							 int x, int y, int srcW, int srcH, int width, int height, Uint8ClampedArray pixelData) {
		backend.useTextureProgram(mode, surface.getWidth(), surface.getHeight(), this.transformArray);

		// the surface associated with this rasterizer already has its texture on the GPU,
		// and we have already called gl.bindTexture for it at the start of rasterization,

		// we can skip the texture upload when using a WebGLSurface, as its texture is already on the GPU

		//TODO: optimize vertex buffer usage

		float[] verts = {
			x, y,
			x + width, y,
			x, y + height,
			x, y + height,
			x + width, y,
			x + width, y + height
		};

		float[] uvs = {
			0f, 1f,
			1f, 1f,
			0f, 0f,
			0f, 0f,
			1f, 1f,
			1f, 0f
		};

		backend.uploadQuadVertices(verts, uvs);

		// bind the texture
		gl.activeTexture(WebGLRenderingContext.TEXTURE0);
		gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, texture);

		if (pixelData != null) {
			gl.texImage2D(WebGLRenderingContext.TEXTURE_2D, 0, WebGLRenderingContext.RGBA,
				srcW, srcH, 0, WebGLRenderingContext.RGBA,
				WebGLRenderingContext.UNSIGNED_BYTE, pixelData);

		}

		gl.drawArrays(WebGLRenderingContext.TRIANGLES, 0, 6);

		gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, null);

		surface.markDirty();
	}

	@Override
	public void rasterizeCommands(List<SurfaceCommand> cmds) {

		gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, framebuffer);
		gl.viewport(0, 0, surface.getWidth(), surface.getHeight());
		gl.framebufferTexture2D(WebGLRenderingContext.FRAMEBUFFER, WebGLRenderingContext.COLOR_ATTACHMENT0,
			WebGLRenderingContext.TEXTURE_2D, this.surface.texture, 0);

		gl.enable(WebGLRenderingContext.BLEND);
		gl.blendFunc(WebGLRenderingContext.SRC_ALPHA, WebGLRenderingContext.ONE_MINUS_SRC_ALPHA);

		updateTransformFloats(this.transform);
		applyClip();

		for (SurfaceCommand cmd : cmds) {
			switch (cmd.type) {
				case SET_COLOR:
					Color c = (Color) cmd.obj;
					if (cmd.arg1 == 0) {
						this.foreground = c;
					} else if (cmd.arg1 == 1) {
						this.background = c;
					} else {
						log.error("WebGLRasterizer: Unknown color target: {}", cmd.arg1);
					}
					break;
				case SET_TRANSFORM:
					AffineTransform at = (AffineTransform) cmd.obj;
					this.transform.setTransform(at);
					updateTransformFloats(this.transform);
					break;
				case SET_CLIP_RECT:
					setClip((Shape) cmd.obj);
					break;
				case BLIT_IMAGE:
					Surface s = ((SurfaceContainer) cmd.obj).getSurface();
					drawImage(s, cmd.arg1, cmd.arg2, cmd.arg3, cmd.arg4);
					break;
				case DRAW_RECT:
					drawRect(cmd.arg1, cmd.arg2, cmd.arg3, cmd.arg4, 1.0f);
					break;
				case FILL_RECT:
					fillRect(cmd.arg1, cmd.arg2, cmd.arg3, cmd.arg4);
					break;
				case CLEAR_RECT:
					clearRect(cmd.arg1, cmd.arg2, cmd.arg3, cmd.arg4);
					break;
//				case DRAW_LINE:
//					break;
				case NO_OP:
					// do nothing (shouldn't be in the command list in the first place)
					break;
				default:
					log.error("WebGLRasterizer: Unhandled command type: {}", cmd.type);
					break;
			}
		}

		if (pushToScreen) {
			pushToScreen();
		}
	}

	private void pushToScreen() {
		int width = gl.getCanvas().getWidth();
		int height = gl.getCanvas().getHeight();


		gl.bindFramebuffer(WebGL2RenderingContext.FRAMEBUFFER, null);

		gl.viewport(0, 0, width, height);

		gl.activeTexture(WebGLRenderingContext.TEXTURE0);
		gl.bindTexture(WebGLRenderingContext.TEXTURE_2D, surface.texture);
		// no swizzling when pushing to screen, as the surface texture is already in RGBA format
		backend.useTextureProgram(WebGLSurfaceBackend.SwizzleMode.NONE, surface.getWidth(), surface.getHeight(),
			identityTransformArray);

		// full-screen quad

		float[] verts = {
			0, 0,
			width, 0,
			0, height,
			0, height,
			width, 0,
			width, height
		};

		float[] uvs = {
			0f, 0f,
			1f, 0f,
			0f, 1f,
			0f, 1f,
			1f, 0f,
			1f, 1f
		};

		backend.uploadQuadVertices(verts, uvs);
		gl.drawArrays(WebGLRenderingContext.TRIANGLES, 0, 6);
	}
}
