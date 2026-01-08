package me.mdbell.awtea.classlib.java.awt;

import lombok.Getter;
import lombok.ToString;

import java.awt.*;
import java.util.Objects;

/**
 * @see java.awt.DisplayMode
 */
@ToString
@Getter
public final class TDisplayMode {

    public static final int BIT_DEPTH_MULTI = -1;
    public static final int REFRESH_RATE_UNKNOWN = 0;


    private final Dimension size;
    private final int bitDepth;
    private final int refreshRate;

    public TDisplayMode(int width, int height, int bitDepth, int refreshRate) {
        this.size = new Dimension(width, height);
        this.bitDepth = bitDepth;
        this.refreshRate = refreshRate;
    }

    public int getWidth() {
        return size.width;
    }

    public int getHeight() {
        return size.height;
    }

    public boolean equals(DisplayMode dm) {
        if (dm == null) {
            return false;
        }
        return dm.getWidth() == getWidth()
                && dm.getHeight() == getHeight()
                && dm.getBitDepth() == getBitDepth()
                && dm.getRefreshRate() == getRefreshRate();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TDisplayMode)) return false;
        TDisplayMode that = (TDisplayMode) o;
        return bitDepth == that.bitDepth && refreshRate == that.refreshRate && Objects.equals(size, that.size);
    }

    @Override
    public int hashCode() {
        return Objects.hash(size, bitDepth, refreshRate);
    }
}
