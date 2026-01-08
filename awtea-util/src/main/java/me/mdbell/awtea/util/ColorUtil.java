package me.mdbell.awtea.util;

import java.awt.*;

public class ColorUtil {

    public static String toCSS(Color color) {
        return "rgba(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + "," + color.getAlpha() + ")";
    }
}
