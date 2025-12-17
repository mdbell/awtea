package me.mdbell.awtea.classlib.java.awt.image;

import me.mdbell.awtea.classlib.java.awt.geom.TPoint2D;
import me.mdbell.awtea.classlib.java.awt.geom.TRectangle2D;

/**
 * TeaVM implementation of java.awt.image.BufferedImageOp.
 * BufferedImageOp describes single-input/single-output operations performed on BufferedImage objects.
 * 
 * @see java.awt.image.BufferedImageOp
 */
public interface TBufferedImageOp {
    
    /**
     * Performs a single-input/single-output operation on a BufferedImage.
     * 
     * @param src the source BufferedImage
     * @param dest the destination BufferedImage
     * @return the destination BufferedImage
     * @see java.awt.image.BufferedImageOp#filter(java.awt.image.BufferedImage, java.awt.image.BufferedImage)
     */
    TBufferedImage filter(TBufferedImage src, TBufferedImage dest);
    
    /**
     * Returns the bounding box of the filtered destination image.
     * 
     * @param src the source BufferedImage
     * @return a Rectangle2D representing the destination image's bounding box
     * @see java.awt.image.BufferedImageOp#getBounds2D(java.awt.image.BufferedImage)
     */
    TRectangle2D getBounds2D(TBufferedImage src);
    
    /**
     * Creates a zeroed destination image with the correct size and number of bands.
     * 
     * @param src the source BufferedImage
     * @param destCM ColorModel of the destination (can be null)
     * @return a BufferedImage with the correct size and number of bands from the specified ColorModel
     * @see java.awt.image.BufferedImageOp#createCompatibleDestImage(java.awt.image.BufferedImage, java.awt.image.ColorModel)
     */
    TBufferedImage createCompatibleDestImage(TBufferedImage src, TColorModel destCM);
    
    /**
     * Returns the location of the corresponding destination point given a point in the source image.
     * 
     * @param srcPt the specified source Point2D
     * @param destPt the destination Point2D
     * @return the destination Point2D
     * @see java.awt.image.BufferedImageOp#getPoint2D(java.awt.geom.Point2D, java.awt.geom.Point2D)
     */
    TPoint2D getPoint2D(TPoint2D srcPt, TPoint2D destPt);
    
    /**
     * Returns the rendering hints for this operation.
     * 
     * @return the RenderingHints object associated with this operation
     * @see java.awt.image.BufferedImageOp#getRenderingHints()
     */
    me.mdbell.awtea.classlib.java.awt.TRenderingHints getRenderingHints();
}
