package me.mdbell.awtea.gfx.webgl;

import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLTexture;

public interface PostProcessingCallback {

    /**
     * Applies post processing effect
     *
     * @param rasterizer    The rasterizer to use for rendering
     * @param screenTexture The texture containing the rendered screen
     * @param gl            The WebGL context
     * @return True to continue applying further post processing effects, false to stop further processing.
     * <p>
     * Note: If false is returned, the current effect should render to the screen directly.
     * If true is returned, the effect should render to the rasterizer's framebuffer - if there are
     * no further effects, the framework will handle rendering to the screen.
     * </p>
     */
    public boolean apply(WebGLRasterizer rasterizer, WebGLTexture screenTexture, WebGL2RenderingContext gl);
}
