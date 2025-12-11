package me.mdbell.awtea.gfx.wasm;

import java.awt.image.BufferedImage;

public enum WasmPixelFormat {
    PIXEL_FORMAT_ARGB,
    PIXEL_FORMAT_RGB,
    PIXEL_FORMAT_RGBA,
    PIXEL_FORMAT_ABGR,
    PIXEL_FORMAT_BGR,
    ;

    public static WasmPixelFormat fromBufferedImageType(int bufferedImageType) {
        switch (bufferedImageType) {
            case BufferedImage.TYPE_INT_ARGB:
            case BufferedImage.TYPE_INT_ARGB_PRE:
                return PIXEL_FORMAT_ARGB;
            case BufferedImage.TYPE_INT_RGB:
                return PIXEL_FORMAT_RGB;
            case BufferedImage.TYPE_4BYTE_ABGR:
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                return PIXEL_FORMAT_ABGR;
            case BufferedImage.TYPE_3BYTE_BGR:
                return PIXEL_FORMAT_BGR;
            default:
                System.err.println("WasmPixelFormat.fromBufferedImageType: Unsupported BufferedImage type: " + bufferedImageType);
                return null;
        }
    }
}
