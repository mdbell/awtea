package me.mdbell.awtea.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mdbell.awtea.classlib.java.awt.event.TMouseEvent;
import me.mdbell.awtea.util.EnumLookup;

@AllArgsConstructor
@Getter
public enum MouseEventType {
    PRESSED("mousedown", TMouseEvent.MOUSE_PRESSED),
    RELEASED("mouseup", TMouseEvent.MOUSE_RELEASED),
    MOVED("mousemove", TMouseEvent.MOUSE_MOVED),
    ENTERED("mouseenter", TMouseEvent.MOUSE_ENTERED),
    EXITED("mouseout", TMouseEvent.MOUSE_EXITED),
    CLICKED("click", TMouseEvent.MOUSE_CLICKED),
    WHEEL("wheel", TMouseEvent.MOUSE_WHEEL),
    UNKNOWN("unknown", -1);

    private static final EnumLookup.StringKey<MouseEventType> LOOKUP = new EnumLookup.StringKey<>(values(),
            MouseEventType::getType);

    private final String type;
    private final int id;

    public static MouseEventType fromHtml(String html) {
        return LOOKUP.getOrDefault(html, UNKNOWN);
    }
}
