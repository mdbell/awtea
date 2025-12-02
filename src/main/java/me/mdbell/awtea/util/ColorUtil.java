package me.mdbell.awtea.util;

import org.teavm.jso.typedarrays.Float32Array;

import java.awt.*;

public class ColorUtil {

    public static String toCSS(Color color) {
        return "rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "," + color.getAlpha() + ")";
    }

    public static int toRGBA(Color color) {
        return (color.getRed() << 24) | (color.getGreen() << 16) | (color.getBlue() << 8) | color.getAlpha();
    }

    public static void normalize(Color color, Float32Array arr) {
        arr.set(0, color.getRed() / 255f);
        arr.set(1, color.getGreen() / 255f);
        arr.set(2, color.getBlue() / 255f);
        arr.set(3, color.getAlpha() / 255f);
    }

    public static void normalize(Color color, float[] arr) {
        arr[0] = color.getRed() / 255f;
        arr[1] = color.getGreen() / 255f;
        arr[2] = color.getBlue() / 255f;
        arr[3] = color.getAlpha() / 255f;
    }

    public static float[] normalize(Color color) {
        float[] arr = new float[4];
        normalize(color, arr);
        return arr;
    }
}
