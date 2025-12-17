package me.mdbell.awtea.font;

/**
 * Factory for creating {@link FontRenderer} instances.
 * 
 * <p>This factory provides a centralized point for obtaining font renderers,
 * making it easy to:
 * <ul>
 *   <li>Switch between different rendering strategies</li>
 *   <li>Configure rendering parameters</li>
 *   <li>Add new rendering implementations</li>
 *   <li>Test and benchmark different approaches</li>
 * </ul>
 * 
 * <p>The default renderer can be configured via system properties or
 * programmatically at runtime.
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Get the default renderer
 * FontRenderer renderer = FontRendererFactory.getDefaultRenderer();
 * 
 * // Create a specific renderer
 * FontRenderer rasterRenderer = FontRendererFactory.createRasterRenderer(4);
 * }</pre>
 */
public class FontRendererFactory {
	
	private static volatile FontRenderer defaultRenderer;
	
	/**
	 * System property to configure the default font renderer.
	 * Valid values: "raster" (default)
	 * Future values might include: "sdf", "canvas", "vector"
	 */
	private static final String RENDERER_PROPERTY = "me.mdbell.awtea.font.renderer";
	
	/**
	 * System property to configure supersampling for the raster renderer.
	 * Valid values: 1-4 (default: 4)
	 */
	private static final String SUPERSAMPLE_PROPERTY = "me.mdbell.awtea.font.supersample";
	
	/**
	 * System property to enable sub-pixel font rendering (LCD/ClearType-style).
	 * Valid values: "true" or "false" (default: false)
	 * When enabled, uses horizontal RGB stripe sub-pixel rendering for improved clarity.
	 */
	private static final String SUBPIXEL_PROPERTY = "me.mdbell.awtea.font.subpixel";
	
	private FontRendererFactory() {
		// Utility class
	}
	
	/**
	 * Get the default font renderer.
	 * The default renderer is determined by system properties or defaults to
	 * a raster renderer with 4x supersampling.
	 * 
	 * @return the default font renderer
	 */
	public static FontRenderer getDefaultRenderer() {
		if (defaultRenderer == null) {
			synchronized (FontRendererFactory.class) {
				if (defaultRenderer == null) {
					defaultRenderer = createDefaultRenderer();
				}
			}
		}
		return defaultRenderer;
	}
	
	/**
	 * Set the default font renderer.
	 * This allows programmatic override of the default renderer.
	 * 
	 * @param renderer the renderer to use as default (null to reset to system default)
	 */
	public static void setDefaultRenderer(FontRenderer renderer) {
		synchronized (FontRendererFactory.class) {
			defaultRenderer = renderer;
		}
	}
	
	/**
	 * Create a raster-based font renderer with the specified supersampling.
	 * 
	 * @param supersample the supersampling factor (1 = no AA, 2-4 recommended)
	 * @return a new raster font renderer
	 */
	public static FontRenderer createRasterRenderer(int supersample) {
		boolean subpixel = getSubpixelFromProperty();
		return new RasterFontRenderer(supersample, subpixel);
	}
	
	/**
	 * Create a raster-based font renderer with the specified configuration.
	 * 
	 * @param supersample the supersampling factor (1 = no AA, 2-4 recommended)
	 * @param subpixel whether to enable sub-pixel rendering
	 * @return a new raster font renderer
	 */
	public static FontRenderer createRasterRenderer(int supersample, boolean subpixel) {
		return new RasterFontRenderer(supersample, subpixel);
	}
	
	/**
	 * Create a raster-based font renderer with default supersampling (4x).
	 * 
	 * @return a new raster font renderer
	 */
	public static FontRenderer createRasterRenderer() {
		return new RasterFontRenderer();
	}
	
	/**
	 * Create the default renderer based on system properties.
	 */
	private static FontRenderer createDefaultRenderer() {
		String rendererType = System.getProperty(RENDERER_PROPERTY, "atlas").toLowerCase();
		
		switch (rendererType) {
			case "atlas":
				// Use atlas-based renderer for better memory efficiency
				int supersample = getSupersampleFromProperty();
				boolean subpixel = getSubpixelFromProperty();
				me.mdbell.awtea.gfx.DefaultSurfaceBackend backend = 
					me.mdbell.awtea.gfx.DefaultSurfaceBackend.getDefault();
				return new me.mdbell.awtea.font.AtlasBasedFontRenderer(backend, supersample, subpixel);
			
			case "raster":
				// Use original raster renderer (for testing/comparison)
				supersample = getSupersampleFromProperty();
				subpixel = getSubpixelFromProperty();
				return new RasterFontRenderer(supersample, subpixel);
			
			default:
				// Default to atlas-based for best performance
				supersample = getSupersampleFromProperty();
				subpixel = getSubpixelFromProperty();
				backend = me.mdbell.awtea.gfx.DefaultSurfaceBackend.getDefault();
				return new me.mdbell.awtea.font.AtlasBasedFontRenderer(backend, supersample, subpixel);
			
			// Future renderer types can be added here:
			// case "sdf":
			//     return new SDFFontRenderer();
			// case "canvas":
			//     return new CanvasFontRenderer();
		}
	}
	
	/**
	 * Get the supersampling factor from system properties.
	 */
	private static int getSupersampleFromProperty() {
		String supersampleStr = System.getProperty(SUPERSAMPLE_PROPERTY);
		if (supersampleStr != null) {
			try {
				int value = Integer.parseInt(supersampleStr);
				if (value >= 1 && value <= 4) {
					return value;
				}
			} catch (NumberFormatException e) {
				// Fall through to default
			}
		}
		return 4; // Default supersampling
	}
	
	/**
	 * Get the sub-pixel rendering setting from system properties.
	 */
	private static boolean getSubpixelFromProperty() {
		String subpixelStr = System.getProperty(SUBPIXEL_PROPERTY);
		if (subpixelStr != null) {
			return Boolean.parseBoolean(subpixelStr);
		}
		return false; // Default: disabled for backward compatibility
	}
}
