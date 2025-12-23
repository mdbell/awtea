package me.mdbell.awtea.gfx.webgl;

import me.mdbell.awtea.testutil.TestCase;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.webgl.WebGL2RenderingContext;

/**
 * Tests for RenderTarget and RenderTargetPool.
 */
public class PostProcessTest extends TestCase {

    private HTMLCanvasElement canvas;
    private WebGL2RenderingContext gl;
    private WebGLSurfaceBackend backend;

    @Override
    public void setUp() {
        HTMLDocument document = HTMLDocument.current();
        canvas = (HTMLCanvasElement) document.createElement("canvas");
        canvas.setWidth(800);
        canvas.setHeight(600);
        backend = new WebGLSurfaceBackend(canvas);
        gl = backend.getGL();
    }

    @Override
    public void tearDown() {
        // Cleanup
    }

    public void testRenderTargetCreation() {
        RenderTarget target = new RenderTarget(gl, 256, 256);
        
        assertNotNull("Target should be created", target);
        assertEquals("Width should match", 256, target.getWidth());
        assertEquals("Height should match", 256, target.getHeight());
        assertNotNull("Texture should exist", target.getTexture());
        assertNotNull("Framebuffer should exist", target.getFramebuffer());
        assertFalse("Should not be destroyed", target.isDestroyed());
        
        target.destroy();
        assertTrue("Should be destroyed", target.isDestroyed());
    }

    public void testRenderTargetBinding() {
        RenderTarget target = new RenderTarget(gl, 256, 256);
        
        try {
            // Should not throw
            target.bind();
            target.unbind();
            
            // Should be able to clear
            target.bind();
            target.clear(1.0f, 0.0f, 0.0f, 1.0f);
            target.unbind();
            
        } finally {
            target.destroy();
        }
    }

    public void testRenderTargetResize() {
        RenderTarget target = new RenderTarget(gl, 256, 256);
        
        try {
            assertEquals("Initial width", 256, target.getWidth());
            assertEquals("Initial height", 256, target.getHeight());
            
            target.resize(512, 512);
            assertEquals("New width", 512, target.getWidth());
            assertEquals("New height", 512, target.getHeight());
            
        } finally {
            target.destroy();
        }
    }

    public void testRenderTargetInvalidDimensions() {
        try {
            new RenderTarget(gl, 0, 256);
            fail("Should throw for zero width");
        } catch (IllegalArgumentException e) {
            // Expected
        }
        
        try {
            new RenderTarget(gl, 256, -1);
            fail("Should throw for negative height");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    public void testRenderTargetDestroyedCheck() {
        RenderTarget target = new RenderTarget(gl, 256, 256);
        target.destroy();
        
        try {
            target.bind();
            fail("Should throw when using destroyed target");
        } catch (IllegalStateException e) {
            assertTrue("Exception should mention destroyed", 
                e.getMessage().contains("destroyed"));
        }
    }

    public void testPoolAcquireRelease() {
        RenderTargetPool pool = new RenderTargetPool(gl);
        
        try {
            // Acquire new target
            RenderTarget target1 = pool.acquire(256, 256);
            assertNotNull("Should acquire target", target1);
            assertEquals("Width should match", 256, target1.getWidth());
            
            // Release back to pool
            pool.release(target1);
            assertEquals("Should have 1 pooled target", 1, pool.getPooledTargetCount());
            
            // Acquire again - should reuse
            RenderTarget target2 = pool.acquire(256, 256);
            assertEquals("Should reuse same target", target1, target2);
            assertEquals("Pool should be empty", 0, pool.getPooledTargetCount());
            
            pool.release(target2);
            
        } finally {
            pool.destroy();
        }
    }

    public void testPoolDifferentSizes() {
        RenderTargetPool pool = new RenderTargetPool(gl);
        
        try {
            RenderTarget t1 = pool.acquire(256, 256);
            RenderTarget t2 = pool.acquire(512, 512);
            RenderTarget t3 = pool.acquire(256, 256);
            
            // t1 and t3 have same size but should be different instances
            assertNotSame("Should be different targets", t1, t3);
            
            pool.release(t1);
            pool.release(t2);
            pool.release(t3);
            
            assertEquals("Should have 3 pooled targets", 3, pool.getPooledTargetCount());
            
        } finally {
            pool.destroy();
        }
    }

    public void testPoolMaxSize() {
        RenderTargetPool pool = new RenderTargetPool(gl);
        pool.setMaxPooledPerSize(2);
        
        try {
            RenderTarget t1 = pool.acquire(256, 256);
            RenderTarget t2 = pool.acquire(256, 256);
            RenderTarget t3 = pool.acquire(256, 256);
            
            pool.release(t1);
            pool.release(t2);
            pool.release(t3); // This one should be destroyed (pool full)
            
            assertEquals("Pool should have max 2", 2, pool.getPooledTargetCount());
            assertTrue("t3 should be destroyed", t3.isDestroyed());
            assertFalse("t1 should not be destroyed", t1.isDestroyed());
            assertFalse("t2 should not be destroyed", t2.isDestroyed());
            
        } finally {
            pool.destroy();
        }
    }

    public void testPoolClear() {
        RenderTargetPool pool = new RenderTargetPool(gl);
        
        try {
            RenderTarget t1 = pool.acquire(256, 256);
            RenderTarget t2 = pool.acquire(512, 512);
            
            pool.release(t1);
            pool.release(t2);
            
            assertEquals("Should have 2 pooled", 2, pool.getPooledTargetCount());
            
            pool.clear();
            
            assertEquals("Should have 0 pooled", 0, pool.getPooledTargetCount());
            assertTrue("t1 should be destroyed", t1.isDestroyed());
            assertTrue("t2 should be destroyed", t2.isDestroyed());
            
        } finally {
            pool.destroy();
        }
    }

    public void testPoolDestroy() {
        RenderTargetPool pool = new RenderTargetPool(gl);
        
        RenderTarget acquired = pool.acquire(256, 256);
        RenderTarget pooled = pool.acquire(256, 256);
        pool.release(pooled);
        
        assertEquals("Should have 1 pooled", 1, pool.getPooledTargetCount());
        assertEquals("Should have 2 total", 2, pool.getTotalTargetCount());
        
        pool.destroy();
        
        assertTrue("Acquired should be destroyed", acquired.isDestroyed());
        assertTrue("Pooled should be destroyed", pooled.isDestroyed());
        assertEquals("Should have 0 total", 0, pool.getTotalTargetCount());
    }

    public void testPipelineCreation() {
        RenderTargetPool pool = new RenderTargetPool(gl);
        PostProcessPipeline pipeline = new PostProcessPipeline(backend, pool);
        
        try {
            assertNotNull("Pipeline should be created", pipeline);
            assertTrue("Should be empty", pipeline.isEmpty());
            assertEquals("Should have 0 effects", 0, pipeline.getEffectCount());
            
        } finally {
            pipeline.destroy();
            pool.destroy();
        }
    }

    public void testPipelineAddRemove() {
        RenderTargetPool pool = new RenderTargetPool(gl);
        PostProcessPipeline pipeline = new PostProcessPipeline(backend, pool);
        
        try {
            TestEffect effect1 = new TestEffect();
            TestEffect effect2 = new TestEffect();
            
            pipeline.addEffect(effect1);
            assertEquals("Should have 1 effect", 1, pipeline.getEffectCount());
            assertFalse("Should not be empty", pipeline.isEmpty());
            
            pipeline.addEffect(effect2);
            assertEquals("Should have 2 effects", 2, pipeline.getEffectCount());
            
            pipeline.removeEffect(effect1);
            assertEquals("Should have 1 effect", 1, pipeline.getEffectCount());
            
            pipeline.clearEffects();
            assertTrue("Should be empty", pipeline.isEmpty());
            
        } finally {
            pipeline.destroy();
            pool.destroy();
        }
    }

    public void testPipelineApplyEmpty() {
        RenderTargetPool pool = new RenderTargetPool(gl);
        PostProcessPipeline pipeline = new PostProcessPipeline(backend, pool);
        RenderTarget input = new RenderTarget(gl, 256, 256);
        
        try {
            // Empty pipeline should return input
            RenderTarget output = pipeline.apply(input);
            assertEquals("Should return input", input, output);
            
        } finally {
            input.destroy();
            pipeline.destroy();
            pool.destroy();
        }
    }

    public void testPipelineApplySingleEffect() {
        RenderTargetPool pool = new RenderTargetPool(gl);
        PostProcessPipeline pipeline = new PostProcessPipeline(backend, pool);
        RenderTarget input = new RenderTarget(gl, 256, 256);
        
        try {
            TestEffect effect = new TestEffect();
            pipeline.addEffect(effect);
            
            RenderTarget output = pipeline.apply(input);
            
            assertNotNull("Should return output", output);
            assertNotSame("Output should be different from input", input, output);
            assertEquals("Should have called effect", 1, effect.callCount);
            
            // Release output
            pool.release(output);
            
        } finally {
            input.destroy();
            pipeline.destroy();
            pool.destroy();
        }
    }

    public void testPipelineApplyMultipleEffects() {
        RenderTargetPool pool = new RenderTargetPool(gl);
        PostProcessPipeline pipeline = new PostProcessPipeline(backend, pool);
        RenderTarget input = new RenderTarget(gl, 256, 256);
        
        try {
            TestEffect effect1 = new TestEffect();
            TestEffect effect2 = new TestEffect();
            TestEffect effect3 = new TestEffect();
            
            pipeline.addEffect(effect1);
            pipeline.addEffect(effect2);
            pipeline.addEffect(effect3);
            
            RenderTarget output = pipeline.apply(input);
            
            assertNotNull("Should return output", output);
            assertEquals("Effect 1 should be called", 1, effect1.callCount);
            assertEquals("Effect 2 should be called", 1, effect2.callCount);
            assertEquals("Effect 3 should be called", 1, effect3.callCount);
            
            pool.release(output);
            
        } finally {
            input.destroy();
            pipeline.destroy();
            pool.destroy();
        }
    }

    /**
     * Simple test effect that just clears the output.
     */
    private static class TestEffect implements PostProcessEffect {
        int callCount = 0;
        
        @Override
        public void apply(PostProcessContext ctx, RenderTarget input, RenderTarget output) {
            callCount++;
            output.bind();
            output.clear(0, 0, 0, 1);
            output.unbind();
        }
    }
}
