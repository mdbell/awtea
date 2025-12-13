package me.mdbell.awtea.gfx;

/**
 * Simple interface for affine transformations to avoid circular dependencies
 * between graphics and classlib modules.
 */
public interface AffineTransform {
    double getScaleX();
    double getScaleY();
    double getShearX();
    double getShearY();
    double getTranslateX();
    double getTranslateY();
}
