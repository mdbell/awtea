package me.mdbell.awtea.classlib.java.awt.image;

import me.mdbell.awtea.classlib.java.awt.TGraphics;
import me.mdbell.awtea.classlib.java.awt.geom.TRectangle2D;

/**
 * TeaVM implementation of java.awt.image.RenderedImage.
 * RenderedImage is a common interface for objects which contain or can produce image data in the form of Rasters.
 * 
 * @see java.awt.image.RenderedImage
 */
public interface TRenderedImage {
    
    /**
     * Returns the width of the RenderedImage.
     * 
     * @return the width of this RenderedImage
     * @see java.awt.image.RenderedImage#getWidth()
     */
    int getWidth();
    
    /**
     * Returns the height of the RenderedImage.
     * 
     * @return the height of this RenderedImage
     * @see java.awt.image.RenderedImage#getHeight()
     */
    int getHeight();
    
    /**
     * Returns the minimum X coordinate of this RenderedImage.
     * 
     * @return the minimum X coordinate
     * @see java.awt.image.RenderedImage#getMinX()
     */
    int getMinX();
    
    /**
     * Returns the minimum Y coordinate of this RenderedImage.
     * 
     * @return the minimum Y coordinate
     * @see java.awt.image.RenderedImage#getMinY()
     */
    int getMinY();
    
    /**
     * Returns the ColorModel associated with this image.
     * 
     * @return the ColorModel of this image
     * @see java.awt.image.RenderedImage#getColorModel()
     */
    TColorModel getColorModel();
    
    /**
     * Returns the SampleModel associated with this image.
     * 
     * @return the SampleModel of this image
     * @see java.awt.image.RenderedImage#getSampleModel()
     */
    TSampleModel getSampleModel();
    
    /**
     * Returns tile (tileX, tileY) as a Raster.
     * 
     * @param tileX the X index of the requested tile in the tile array
     * @param tileY the Y index of the requested tile in the tile array
     * @return the tile as a Raster
     * @see java.awt.image.RenderedImage#getTile(int, int)
     */
    TRaster getTile(int tileX, int tileY);
    
    /**
     * Returns the image as one large tile.
     * 
     * @return a Raster that is the entire image
     * @see java.awt.image.RenderedImage#getData()
     */
    TRaster getData();
    
    /**
     * Computes an arbitrary rectangular region of the RenderedImage and copies it into a caller-supplied WritableRaster.
     * 
     * @param rect the region of the RenderedImage to be copied
     * @return a reference to the supplied or created WritableRaster
     * @see java.awt.image.RenderedImage#getData(java.awt.Rectangle)
     */
    TRaster getData(me.mdbell.awtea.classlib.java.awt.TRectangle rect);
}
