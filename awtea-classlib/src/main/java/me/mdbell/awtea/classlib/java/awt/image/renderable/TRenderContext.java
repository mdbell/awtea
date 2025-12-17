package me.mdbell.awtea.classlib.java.awt.image.renderable;

import me.mdbell.awtea.classlib.java.awt.TRenderingHints;
import me.mdbell.awtea.classlib.java.awt.TShape;
import me.mdbell.awtea.classlib.java.awt.geom.TAffineTransform;

/**
 * TeaVM implementation of java.awt.image.renderable.RenderContext.
 * A RenderContext encapsulates the information needed to produce a specific rendering from a RenderableImage.
 * 
 * @see java.awt.image.renderable.RenderContext
 */
public class TRenderContext {
    
    private TAffineTransform transform;
    private TShape aoi;
    private TRenderingHints hints;
    
    /**
     * Constructs a RenderContext with a given transform.
     * 
     * @param usr2dev an AffineTransform
     * @see java.awt.image.renderable.RenderContext#RenderContext(java.awt.geom.AffineTransform)
     */
    public TRenderContext(TAffineTransform usr2dev) {
        this(usr2dev, null, null);
    }
    
    /**
     * Constructs a RenderContext with a given transform and rendering hints.
     * 
     * @param usr2dev an AffineTransform
     * @param hints a RenderingHints object
     * @see java.awt.image.renderable.RenderContext#RenderContext(java.awt.geom.AffineTransform, java.awt.RenderingHints)
     */
    public TRenderContext(TAffineTransform usr2dev, TRenderingHints hints) {
        this(usr2dev, null, hints);
    }
    
    /**
     * Constructs a RenderContext with a given transform, area of interest, and rendering hints.
     * 
     * @param usr2dev an AffineTransform
     * @param aoi a Shape representing the area of interest
     * @param hints a RenderingHints object
     * @see java.awt.image.renderable.RenderContext#RenderContext(java.awt.geom.AffineTransform, java.awt.Shape, java.awt.RenderingHints)
     */
    public TRenderContext(TAffineTransform usr2dev, TShape aoi, TRenderingHints hints) {
        this.transform = usr2dev != null ? new TAffineTransform(usr2dev) : new TAffineTransform();
        this.aoi = aoi;
        this.hints = hints;
    }
    
    /**
     * Gets the current user-to-device AffineTransform.
     * 
     * @return a reference to the current AffineTransform
     * @see java.awt.image.renderable.RenderContext#getTransform()
     */
    public TAffineTransform getTransform() {
        return transform;
    }
    
    /**
     * Sets the current user-to-device AffineTransform.
     * 
     * @param newTransform the new AffineTransform
     * @see java.awt.image.renderable.RenderContext#setTransform(java.awt.geom.AffineTransform)
     */
    public void setTransform(TAffineTransform newTransform) {
        this.transform = newTransform;
    }
    
    /**
     * Gets the area of interest (AOI), if any.
     * 
     * @return a Shape representing the area of interest, or null
     * @see java.awt.image.renderable.RenderContext#getAreaOfInterest()
     */
    public TShape getAreaOfInterest() {
        return aoi;
    }
    
    /**
     * Sets the area of interest.
     * 
     * @param newAoi the new area of interest
     * @see java.awt.image.renderable.RenderContext#setAreaOfInterest(java.awt.Shape)
     */
    public void setAreaOfInterest(TShape newAoi) {
        this.aoi = newAoi;
    }
    
    /**
     * Gets the rendering hints.
     * 
     * @return a RenderingHints object containing the rendering hints
     * @see java.awt.image.renderable.RenderContext#getRenderingHints()
     */
    public TRenderingHints getRenderingHints() {
        return hints;
    }
    
    /**
     * Sets the rendering hints.
     * 
     * @param hints a RenderingHints object containing the rendering hints
     * @see java.awt.image.renderable.RenderContext#setRenderingHints(java.awt.RenderingHints)
     */
    public void setRenderingHints(TRenderingHints hints) {
        this.hints = hints;
    }
}
