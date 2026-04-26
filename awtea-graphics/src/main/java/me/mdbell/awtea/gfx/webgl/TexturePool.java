package me.mdbell.awtea.gfx.webgl;

import me.mdbell.awtea.util.ObjectPool;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLTexture;

public class TexturePool {

    public static final int POOL_SIZE = 32;

    private final WebGL2RenderingContext gl;
    private final ObjectPool<WebGLTexture> pool;

    public TexturePool(WebGL2RenderingContext gl) {
        this.gl = gl;
        this.pool = new ObjectPool<>(this::createTexture, null, this::destroyTexture, POOL_SIZE);
    }

    private void destroyTexture(WebGLTexture texture) {
        gl.deleteTexture(texture);
    }

    private WebGLTexture createTexture() {
        WebGLTexture texture = gl.createTexture();
        gl.bindTexture(WebGL2RenderingContext.TEXTURE_2D, texture);
        gl.texParameteri(WebGL2RenderingContext.TEXTURE_2D, WebGL2RenderingContext.TEXTURE_MIN_FILTER, WebGL2RenderingContext.NEAREST);
        gl.texParameteri(WebGL2RenderingContext.TEXTURE_2D, WebGL2RenderingContext.TEXTURE_MAG_FILTER, WebGL2RenderingContext.NEAREST);
        gl.texParameteri(WebGL2RenderingContext.TEXTURE_2D, WebGL2RenderingContext.TEXTURE_WRAP_S, WebGL2RenderingContext.CLAMP_TO_EDGE);
        gl.texParameteri(WebGL2RenderingContext.TEXTURE_2D, WebGL2RenderingContext.TEXTURE_WRAP_T, WebGL2RenderingContext.CLAMP_TO_EDGE);
        return texture;
    }

    public WebGLTexture obtain() {
        return pool.obtain();
    }

    public void release(WebGLTexture texture) {
        pool.release(texture);
    }
}
