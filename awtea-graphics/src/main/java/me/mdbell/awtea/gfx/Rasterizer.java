package me.mdbell.awtea.gfx;

import java.util.List;

public interface Rasterizer {

	Rasterizer create();

	void reset();

	void rasterizeCommands(List<SurfaceCommand> cmds);

	default void dispose() {
		// Default implementation does nothing
		// Implementations can override to clean up resources
	}
	
	/**
	 * Queues a custom rendering callback to be executed during the rendering pipeline.
	 * This is used for advanced rendering operations like custom shaders.
	 * <p>
	 * Default implementation does nothing - only WebGL rasterizer supports this.
	 * </p>
	 * 
	 * @param wrapper the shader callback wrapper containing the shader and callback
	 */
	default void queueRenderCallback(Object wrapper) {
		// Default no-op implementation for rasterizers that don't support custom callbacks
	}
}
