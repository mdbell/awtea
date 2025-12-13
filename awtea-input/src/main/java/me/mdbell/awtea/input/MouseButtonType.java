package me.mdbell.awtea.input;

import lombok.AllArgsConstructor;
import lombok.Getter;
import static me.mdbell.awtea.input.MouseConstants.*;
import me.mdbell.awtea.util.EnumLookup;

@AllArgsConstructor
@Getter
public enum MouseButtonType {
    LEFT(0, BUTTON1),
    MIDDLE(1, BUTTON2),
    RIGHT(2, BUTTON3),
    BACK(3, NOBUTTON),
    FORWARD(4, NOBUTTON),
    UNKNOWN(-1, NOBUTTON);

    private final int html;
    private final int java;

    private static final EnumLookup.IntegerKey<MouseButtonType> LOOKUP = new EnumLookup.IntegerKey<>(values(),
            MouseButtonType::getHtml);

    public static MouseButtonType fromHtml(int html) {
        return LOOKUP.getOrDefault(html, UNKNOWN);
    }
}
