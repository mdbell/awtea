package me.mdbell.awtea.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.mdbell.awtea.util.EnumLookup;

import static me.mdbell.awtea.input.MouseConstants.*;

@AllArgsConstructor
@Getter
public enum MouseEventType {
    PRESSED("mousedown", MOUSE_PRESSED),
    RELEASED("mouseup", MOUSE_RELEASED),
    MOVED("mousemove", MOUSE_MOVED),
    CLICKED("click", MOUSE_CLICKED),
    WHEEL("wheel", MOUSE_WHEEL),
    UNKNOWN("unknown", -1);

    private static final EnumLookup.StringKey<MouseEventType> LOOKUP = new EnumLookup.StringKey<>(values(),
            MouseEventType::getType);

    private final String type;
    private final int id;

    public static MouseEventType fromHtml(String html) {
        return LOOKUP.getOrDefault(html, UNKNOWN);
    }
}
