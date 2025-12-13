package me.mdbell.awtea.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.awt.event.MouseEvent;
import me.mdbell.awtea.util.EnumLookup;

@AllArgsConstructor
@Getter
public enum MouseButtonType {
    LEFT(0, MouseEvent.BUTTON1),
    MIDDLE(1, MouseEvent.BUTTON2),
    RIGHT(2, MouseEvent.BUTTON3),
    BACK(3, MouseEvent.NOBUTTON),
    FORWARD(4, MouseEvent.NOBUTTON),
    UNKNOWN(-1, MouseEvent.NOBUTTON);

    private final int html;
    private final int java;

    private static final EnumLookup.IntegerKey<MouseButtonType> LOOKUP = new EnumLookup.IntegerKey<>(values(),
            MouseButtonType::getHtml);

    public static MouseButtonType fromHtml(int html) {
        return LOOKUP.getOrDefault(html, UNKNOWN);
    }
}
