package me.mdbell.awtea.classlib.java.awt.image.renderable;

import me.mdbell.awtea.classlib.java.awt.TRenderingHints;
import me.mdbell.awtea.classlib.java.awt.image.TRenderedImage;

import java.util.Vector;

/**
 * TeaVM implementation of java.awt.image.renderable.RenderableImage.
 * A RenderableImage is a common interface for rendering-independent images (a notion which subsumes
 * resolution independence).
 * 
 * @see java.awt.image.renderable.RenderableImage
 */
public interface TRenderableImage {
    
    /**
     * Gets a property from the property set of this image.
     * 
     * @param name the name of the property to get
     * @return a reference to the property Object, or UNDEFINED if the property is not defined
     * @see java.awt.image.renderable.RenderableImage#getProperty(java.lang.String)
     */
    Object getProperty(String name);
    
    /**
     * Returns a list of names recognized by getProperty.
     * 
     * @return an array of Strings representing property names
     * @see java.awt.image.renderable.RenderableImage#getPropertyNames()
     */
    String[] getPropertyNames();
    
    /**
     * Returns the width of the image in user coordinate space.
     * 
     * @return the width of the image
     * @see java.awt.image.renderable.RenderableImage#getWidth()
     */
    float getWidth();
    
    /**
     * Returns the height of the image in user coordinate space.
     * 
     * @return the height of the image
     * @see java.awt.image.renderable.RenderableImage#getHeight()
     */
    float getHeight();
    
    /**
     * Gets the minimum X coordinate of the rendering-independent image data.
     * 
     * @return the minimum X coordinate
     * @see java.awt.image.renderable.RenderableImage#getMinX()
     */
    float getMinX();
    
    /**
     * Gets the minimum Y coordinate of the rendering-independent image data.
     * 
     * @return the minimum Y coordinate
     * @see java.awt.image.renderable.RenderableImage#getMinY()
     */
    float getMinY();
    
    /**
     * Creates a RenderedImage instance of this image with a default width and height in pixels.
     * 
     * @return a RenderedImage containing the rendered data
     * @see java.awt.image.renderable.RenderableImage#createDefaultRendering()
     */
    TRenderedImage createDefaultRendering();
    
    /**
     * Creates a RenderedImage instance of this image with width w and height h in pixels.
     * 
     * @param w the width of rendered image in pixels
     * @param h the height of rendered image in pixels
     * @return a RenderedImage containing the rendered data
     * @see java.awt.image.renderable.RenderableImage#createScaledRendering(int, int, java.awt.RenderingHints)
     */
    TRenderedImage createScaledRendering(int w, int h, TRenderingHints hints);
    
    /**
     * Creates a RenderedImage that represents a rendering of this image using a given RenderContext.
     * 
     * @param renderContext the RenderContext to use to produce the rendering
     * @return a RenderedImage containing the rendered data
     * @see java.awt.image.renderable.RenderableImage#createRendering(java.awt.image.renderable.RenderContext)
     */
    TRenderedImage createRendering(TRenderContext renderContext);
}
