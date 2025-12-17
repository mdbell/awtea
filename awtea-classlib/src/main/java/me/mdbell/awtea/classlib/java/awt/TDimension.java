package me.mdbell.awtea.classlib.java.awt;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.awt.Dimension;

/**
 * The {@code TDimension} class encapsulates the width and height of a component
 * in a single object.
 * This is the awtea implementation of java.awt.Dimension.
 *
 * @see java.awt.Dimension
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TDimension {
    /**
     * The width dimension; negative values can be used.
     */
    public int width;

    /**
     * The height dimension; negative values can be used.
     */
    public int height;

    /**
     * Creates an instance of {@code TDimension} whose width and height
     * are the same as for the specified dimension.
     *
     * @param d the specified dimension for the {@code width} and {@code height} values
     */
    public TDimension(TDimension d) {
        this(d.width, d.height);
    }

    /**
     * Converts a standard AWT Dimension to a TDimension.
     *
     * @param d the AWT Dimension to convert
     * @return the TDimension
     */
    public static TDimension fromAWT(Dimension d) {
        if (d == null) {
            return null;
        }
        return new TDimension(d.width, d.height);
    }

    /**
     * Converts this TDimension to a standard AWT Dimension.
     *
     * @return the AWT Dimension
     */
    public Dimension toAWT() {
        return new Dimension(width, height);
    }
}
