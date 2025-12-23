package me.mdbell.awtea.gfx.webgl;

import me.mdbell.awtea.util.logging.Logger;
import me.mdbell.awtea.util.logging.LoggerFactory;
import org.teavm.jso.webgl.WebGL2RenderingContext;
import org.teavm.jso.webgl.WebGLRenderingContext;

import java.util.*;

/**
 * Manages a pool of render targets for efficient reuse.
 * 
 * <p>Creating and destroying framebuffers is expensive. This pool allows
 * render targets to be acquired and released without allocation overhead.
 * Targets are keyed by dimensions and reused when dimensions match.</p>
 * 
 * <p>Example usage:
 * <pre>
 * RenderTargetPool pool = new RenderTargetPool(gl);
 * 
 * // Acquire a target
 * RenderTarget target = pool.acquire(800, 600);
 * 
 * // Use the target
 * target.bind();
 * // ... render ...
 * target.unbind();
 * 
 * // Release back to pool for reuse
 * pool.release(target);
 * 
 * // Clean up all pooled targets
 * pool.destroy();
 * </pre>
 * </p>
 */
public class RenderTargetPool {
    
    private static final Logger log = LoggerFactory.getLogger(RenderTargetPool.class);
    
    private final WebGL2RenderingContext gl;
    
    // Pool organized by size key (width_height) for fast lookup
    private final Map<String, Queue<RenderTarget>> pool = new HashMap<>();
    
    // Track all targets for cleanup
    private final Set<RenderTarget> allTargets = new HashSet<>();
    
    // Configuration
    private int maxPooledPerSize = 4;
    private int textureFilter = WebGLRenderingContext.LINEAR;
    private int textureWrap = WebGLRenderingContext.CLAMP_TO_EDGE;
    
    /**
     * Creates a new render target pool.
     * 
     * @param gl the WebGL rendering context
     */
    public RenderTargetPool(WebGL2RenderingContext gl) {
        this.gl = gl;
    }
    
    /**
     * Acquires a render target from the pool, creating a new one if necessary.
     * 
     * @param width the desired width
     * @param height the desired height
     * @return a render target with the specified dimensions
     */
    public RenderTarget acquire(int width, int height) {
        String key = makeKey(width, height);
        Queue<RenderTarget> targets = pool.get(key);
        
        RenderTarget target = null;
        
        // Try to reuse from pool
        if (targets != null && !targets.isEmpty()) {
            target = targets.poll();
            log.trace("Reused render target {}x{} from pool", width, height);
        }
        
        // Create new if pool is empty
        if (target == null) {
            target = new RenderTarget(gl, width, height, textureFilter, textureWrap);
            allTargets.add(target);
            log.trace("Created new render target {}x{}", width, height);
        }
        
        return target;
    }
    
    /**
     * Releases a render target back to the pool for reuse.
     * If the pool for this size is full, the target is destroyed.
     * 
     * @param target the target to release
     */
    public void release(RenderTarget target) {
        if (target == null || target.isDestroyed()) {
            return;
        }
        
        String key = makeKey(target.getWidth(), target.getHeight());
        Queue<RenderTarget> targets = pool.computeIfAbsent(key, k -> new LinkedList<>());
        
        // Add back to pool if there's room
        if (targets.size() < maxPooledPerSize) {
            targets.offer(target);
            log.trace("Returned render target {}x{} to pool (pool size: {})", 
                target.getWidth(), target.getHeight(), targets.size());
        } else {
            // Pool is full, destroy the target
            target.destroy();
            allTargets.remove(target);
            log.trace("Destroyed render target {}x{} (pool full)", 
                target.getWidth(), target.getHeight());
        }
    }
    
    /**
     * Sets the maximum number of targets to pool per size.
     * Default is 4.
     * 
     * @param max the maximum pool size per dimension
     */
    public void setMaxPooledPerSize(int max) {
        if (max < 0) {
            throw new IllegalArgumentException("Max pooled per size must be non-negative");
        }
        this.maxPooledPerSize = max;
    }
    
    /**
     * Sets the texture filter mode for newly created targets.
     * Does not affect existing targets.
     * 
     * @param filter WebGLRenderingContext.LINEAR or WebGLRenderingContext.NEAREST
     */
    public void setTextureFilter(int filter) {
        this.textureFilter = filter;
    }
    
    /**
     * Sets the texture wrap mode for newly created targets.
     * Does not affect existing targets.
     * 
     * @param wrap WebGLRenderingContext.CLAMP_TO_EDGE or WebGLRenderingContext.REPEAT
     */
    public void setTextureWrap(int wrap) {
        this.textureWrap = wrap;
    }
    
    /**
     * Clears the pool, destroying all pooled targets.
     * Does not destroy targets that are currently acquired.
     */
    public void clear() {
        for (Queue<RenderTarget> targets : pool.values()) {
            for (RenderTarget target : targets) {
                target.destroy();
                allTargets.remove(target);
            }
        }
        pool.clear();
        log.debug("Cleared render target pool");
    }
    
    /**
     * Destroys all targets (both pooled and acquired) and clears the pool.
     * After calling this, the pool should not be used.
     */
    public void destroy() {
        // Destroy all targets (pooled and acquired)
        for (RenderTarget target : allTargets) {
            if (!target.isDestroyed()) {
                target.destroy();
            }
        }
        allTargets.clear();
        pool.clear();
        log.debug("Destroyed render target pool");
    }
    
    /**
     * Gets statistics about the current pool state.
     * 
     * @return a map of size keys to pooled target counts
     */
    public Map<String, Integer> getStats() {
        Map<String, Integer> stats = new HashMap<>();
        for (Map.Entry<String, Queue<RenderTarget>> entry : pool.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().size());
        }
        return stats;
    }
    
    /**
     * Gets the total number of targets managed by this pool (pooled + acquired).
     * 
     * @return the total target count
     */
    public int getTotalTargetCount() {
        return allTargets.size();
    }
    
    /**
     * Gets the number of currently pooled (available) targets.
     * 
     * @return the pooled target count
     */
    public int getPooledTargetCount() {
        int count = 0;
        for (Queue<RenderTarget> targets : pool.values()) {
            count += targets.size();
        }
        return count;
    }
    
    private String makeKey(int width, int height) {
        return width + "_" + height;
    }
}
