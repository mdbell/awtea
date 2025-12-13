package me.mdbell.awtea.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.awt.event.MouseEvent;
import me.mdbell.awtea.util.EnumLookup;

@AllArgsConstructor
@Getter
public enum MouseEventType {
    PRESSED("mousedown", MouseEvent.MOUSE_PRESSED),
    RELEASED("mouseup", MouseEvent.MOUSE_RELEASED),
    MOVED("mousemove", MouseEvent.MOUSE_MOVED),
    ENTERED("mouseenter", MouseEvent.MOUSE_ENTERED),
    EXITED("mouseout", MouseEvent.MOUSE_EXITED),
    CLICKED("click", MouseEvent.MOUSE_CLICKED),
    WHEEL("wheel", MouseEvent.MOUSE_WHEEL),
    UNKNOWN("unknown", -1);

    private static final EnumLookup.StringKey<MouseEventType> LOOKUP = new EnumLookup.StringKey<>(values(),
            MouseEventType::getType);

    private final String type;
    private final int id;

    public static MouseEventType fromHtml(String html) {
        return LOOKUP.getOrDefault(html, UNKNOWN);
    }
}
