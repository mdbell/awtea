package me.mdbell.awtea.gfx.webgl;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a multi-pass post-processing pipeline for applying effects to rendered content.
 * 
 * <p>The pipeline allows chaining multiple effects together. Each effect processes
 * the output of the previous effect. The pipeline handles render target allocation
 * and ping-pong buffering automatically.</p>
 * 
 * <p>Example usage:
 * <pre>
 * // Create pipeline
 * PostProcessPipeline pipeline = new PostProcessPipeline(backend, pool);
 * 
 * // Add effects
 * pipeline.addEffect(new BloomEffect());
 * pipeline.addEffect(new ColorCorrectionEffect());
 * 
 * // Apply pipeline
 * RenderTarget input = ...; // Your rendered scene
 * RenderTarget output = pipeline.apply(input);
 * 
 * // Use output texture
 * gl.bindTexture(TEXTURE_2D, output.getTexture());
 * 
 * // Clean up
 * pipeline.destroy();
 * </pre>
 * </p>
 */
public class PostProcessPipeline {
    
    private static final Logger log = LoggerFactory.getLogger(PostProcessPipeline.class);
    
    private final WebGLSurfaceBackend backend;
    private final RenderTargetPool pool;
    private final PostProcessContext context;
    private final List<PostProcessEffect> effects = new ArrayList<>();
    
    /**
     * Creates a new post-processing pipeline.
     * 
     * @param backend the WebGL surface backend
     * @param pool the render target pool for acquiring temporary targets
     */
    public PostProcessPipeline(WebGLSurfaceBackend backend, RenderTargetPool pool) {
        this.backend = backend;
        this.pool = pool;
        this.context = new PostProcessContext(backend);
    }
    
    /**
     * Adds an effect to the end of the pipeline.
     * Effects are applied in the order they are added.
     * 
     * @param effect the effect to add
     * @return this pipeline for chaining
     */
    public PostProcessPipeline addEffect(PostProcessEffect effect) {
        if (effect == null) {
            throw new IllegalArgumentException("Effect cannot be null");
        }
        effects.add(effect);
        log.debug("Added effect to pipeline: {}", effect.getClass().getSimpleName());
        return this;
    }
    
    /**
     * Removes an effect from the pipeline.
     * 
     * @param effect the effect to remove
     * @return true if the effect was removed
     */
    public boolean removeEffect(PostProcessEffect effect) {
        boolean removed = effects.remove(effect);
        if (removed) {
            log.debug("Removed effect from pipeline: {}", effect.getClass().getSimpleName());
        }
        return removed;
    }
    
    /**
     * Clears all effects from the pipeline.
     */
    public void clearEffects() {
        effects.clear();
        log.debug("Cleared all effects from pipeline");
    }
    
    /**
     * Applies all effects in the pipeline to the input.
     * 
     * <p>The pipeline uses ping-pong buffering to chain effects efficiently.
     * Intermediate targets are acquired from the pool and released after use.</p>
     * 
     * @param input the input render target containing the scene to process
     * @return the final output render target (must be released to pool when done)
     */
    public RenderTarget apply(RenderTarget input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }
        
        if (effects.isEmpty()) {
            log.trace("No effects in pipeline, returning input");
            return input;
        }
        
        int width = input.getWidth();
        int height = input.getHeight();
        
        // Acquire temporary targets for ping-pong
        RenderTarget current = input;
        RenderTarget temp = null;
        
        try {
            for (int i = 0; i < effects.size(); i++) {
                PostProcessEffect effect = effects.get(i);
                
                // Acquire output target
                RenderTarget output = pool.acquire(width, height);
                
                // Apply effect
                log.trace("Applying effect {}/{}: {}", 
                    i + 1, effects.size(), effect.getClass().getSimpleName());
                effect.apply(context, current, output);
                
                // Release previous temp target
                if (temp != null) {
                    pool.release(temp);
                }
                
                // Current output becomes next input
                temp = output;
                current = output;
            }
            
            log.trace("Pipeline complete, {} effects applied", effects.size());
            return current;
            
        } catch (Exception e) {
            log.error("Error applying post-process pipeline: {}", e.getMessage(), e);
            
            // Clean up on error
            if (temp != null && temp != input) {
                pool.release(temp);
            }
            
            throw new RuntimeException("Post-process pipeline failed", e);
        }
    }
    
    /**
     * Gets the number of effects in the pipeline.
     * 
     * @return the effect count
     */
    public int getEffectCount() {
        return effects.size();
    }
    
    /**
     * Checks if the pipeline is empty (has no effects).
     * 
     * @return true if empty
     */
    public boolean isEmpty() {
        return effects.isEmpty();
    }
    
    /**
     * Gets the context used for rendering.
     * 
     * @return the post-process context
     */
    public PostProcessContext getContext() {
        return context;
    }
    
    /**
     * Destroys the pipeline and disposes all effects.
     * After calling this, the pipeline should not be used.
     */
    public void destroy() {
        for (PostProcessEffect effect : effects) {
            try {
                effect.dispose();
            } catch (Exception e) {
                log.error("Error disposing effect: {}", e.getMessage(), e);
            }
        }
        effects.clear();
        log.debug("Destroyed post-process pipeline");
    }
}
