package me.mdbell.awtea.classlib.javax.swing;

import me.mdbell.awtea.classlib.java.awt.event.TMouseEvent;
import me.mdbell.awtea.input.MouseButtonType;

public class TSwingUtilities {

    private TSwingUtilities() {

    }

    public static boolean isLeftMouseButton(TMouseEvent event) {
        return event.getMouseButton() == MouseButtonType.LEFT;
    }

    public static boolean isRightMouseButton(TMouseEvent event) {
        return event.getMouseButton() == MouseButtonType.RIGHT;
    }

    public static boolean isMiddleMouseButton(TMouseEvent event) {
        return event.getMouseButton() == MouseButtonType.MIDDLE;
    }
}
