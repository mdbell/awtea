package me.mdbell.awtea.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mdbell.awtea.classlib.java.awt.event.TMouseEvent;
import me.mdbell.awtea.util.EnumLookup;

@AllArgsConstructor
@Getter
public enum MouseButtonType {
    LEFT(0, TMouseEvent.BUTTON1),
    MIDDLE(1, TMouseEvent.BUTTON2),
    RIGHT(2, TMouseEvent.BUTTON3),
    BACK(3, TMouseEvent.NOBUTTON),
    FORWARD(4, TMouseEvent.NOBUTTON),
    UNKNOWN(-1, TMouseEvent.NOBUTTON);

    private final int html;
    private final int java;

    private static final EnumLookup.IntegerKey<MouseButtonType> LOOKUP = new EnumLookup.IntegerKey<>(values(),
            MouseButtonType::getHtml);

    public static MouseButtonType fromHtml(int html) {
        return LOOKUP.getOrDefault(html, UNKNOWN);
    }
}
