package me.mdbell.awtea.util;

import org.teavm.jso.typedarrays.Float32Array;

import java.awt.*;

public class ColorUtil {

    public static String toCSS(Color color) {
        return "rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "," + color.getAlpha() + ")";
    }
}
