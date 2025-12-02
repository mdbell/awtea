package me.mdbell.awtea.input;

import lombok.Getter;
import me.mdbell.awtea.classlib.java.awt.event.TKeyEvent;
import me.mdbell.awtea.util.EnumLookup;

@Getter
public enum KeyboardKey {
    // Arrow keys
    ARROW_LEFT("ArrowLeft", TKeyEvent.VK_LEFT),
    ARROW_UP("ArrowUp", TKeyEvent.VK_UP),
    ARROW_RIGHT("ArrowRight", TKeyEvent.VK_RIGHT),
    ARROW_DOWN("ArrowDown", TKeyEvent.VK_DOWN),

    // Control keys
    BACKSPACE("Backspace", TKeyEvent.VK_BACK_SPACE),
    TAB("Tab", TKeyEvent.VK_TAB, '\t'),
    ENTER("Enter", TKeyEvent.VK_ENTER, '\n'),
    SHIFT_LEFT("ShiftLeft", TKeyEvent.VK_SHIFT),
    SHIFT_RIGHT("ShiftRight", TKeyEvent.VK_SHIFT),
    CONTROL_LEFT("ControlLeft", TKeyEvent.VK_CONTROL),
    CONTROL_RIGHT("ControlRight", TKeyEvent.VK_CONTROL),
    ALT_LEFT("AltLeft", TKeyEvent.VK_ALT),
    ALT_RIGHT("AltRight", TKeyEvent.VK_ALT),
    CAPS_LOCK("CapsLock", TKeyEvent.VK_CAPS_LOCK),
    ESCAPE("Escape", TKeyEvent.VK_ESCAPE),
    SPACE("Space", TKeyEvent.VK_SPACE, ' '),
    PAGE_UP("PageUp", TKeyEvent.VK_PAGE_UP),
    PAGE_DOWN("PageDown", TKeyEvent.VK_PAGE_DOWN),
    END("End", TKeyEvent.VK_END),
    HOME("Home", TKeyEvent.VK_HOME),
    INSERT("Insert", TKeyEvent.VK_INSERT),
    DELETE("Delete", TKeyEvent.VK_DELETE),

    // Numbers
    NUM_1("Digit1", TKeyEvent.VK_1, '1', '!'),
    NUM_2("Digit2", TKeyEvent.VK_2, '2', '@'),
    NUM_3("Digit3", TKeyEvent.VK_3, '3', '#'),
    NUM_4("Digit4", TKeyEvent.VK_4, '4', '$'),
    NUM_5("Digit5", TKeyEvent.VK_5, '5', '%'),
    NUM_6("Digit6", TKeyEvent.VK_6, '6', '^'),
    NUM_7("Digit7", TKeyEvent.VK_7, '7', '&'),
    NUM_8("Digit8", TKeyEvent.VK_8, '8', '*'),
    NUM_9("Digit9", TKeyEvent.VK_9, '9', '('),
    NUM_0("Digit0", TKeyEvent.VK_0, '0', ')'),

    // Letters
    A("KeyA", TKeyEvent.VK_A, 'a'),
    B("KeyB", TKeyEvent.VK_B, 'b'),
    C("KeyC", TKeyEvent.VK_C, 'c'),
    D("KeyD", TKeyEvent.VK_D, 'd'),
    E("KeyE", TKeyEvent.VK_E, 'e'),
    F("KeyF", TKeyEvent.VK_F, 'f'),
    G("KeyG", TKeyEvent.VK_G, 'g'),
    H("KeyH", TKeyEvent.VK_H, 'h'),
    I("KeyI", TKeyEvent.VK_I, 'i'),
    J("KeyJ", TKeyEvent.VK_J, 'j'),
    K("KeyK", TKeyEvent.VK_K, 'k'),
    L("KeyL", TKeyEvent.VK_L, 'l'),
    M("KeyM", TKeyEvent.VK_M, 'm'),
    N("KeyN", TKeyEvent.VK_N, 'n'),
    O("KeyO", TKeyEvent.VK_O, 'o'),
    P("KeyP", TKeyEvent.VK_P, 'p'),
    Q("KeyQ", TKeyEvent.VK_Q, 'q'),
    R("KeyR", TKeyEvent.VK_R, 'r'),
    S("KeyS", TKeyEvent.VK_S, 's'),
    T("KeyT", TKeyEvent.VK_T, 't'),
    U("KeyU", TKeyEvent.VK_U, 'u'),
    V("KeyV", TKeyEvent.VK_V, 'v'),
    W("KeyW", TKeyEvent.VK_W, 'w'),
    X("KeyX", TKeyEvent.VK_X, 'x'),
    Y("KeyY", TKeyEvent.VK_Y, 'y'),
    Z("KeyZ", TKeyEvent.VK_Z, 'z'),

    // Symbols
    MINUS("Minus", TKeyEvent.VK_MINUS, '-', '_'),
    EQUALS("Equal", TKeyEvent.VK_EQUALS, '=', '+'),
    OPEN_BRACKET("BracketLeft", TKeyEvent.VK_OPEN_BRACKET, '[', '{'),
    CLOSE_BRACKET("BracketRight", TKeyEvent.VK_CLOSE_BRACKET, ']', '}'),
    SEMICOLON("Semicolon", TKeyEvent.VK_SEMICOLON, ';', ':'),
    BACK_SLASH("Backslash", TKeyEvent.VK_BACK_SLASH, '\\', '|'),

    QUOTE("Quote", TKeyEvent.VK_QUOTE, '\'', '"'),
    COMMA("Comma", TKeyEvent.VK_COMMA, ',', '<'),
    PERIOD("Period", TKeyEvent.VK_PERIOD, '.', '>'),
    SLASH("Slash", TKeyEvent.VK_SLASH, '/', '?'),

    // Numpad
    NUMPAD_0("Numpad0", TKeyEvent.VK_NUMPAD0),
    NUMPAD_1("Numpad1", TKeyEvent.VK_NUMPAD1),
    NUMPAD_2("Numpad2", TKeyEvent.VK_NUMPAD2),
    NUMPAD_3("Numpad3", TKeyEvent.VK_NUMPAD3),
    NUMPAD_4("Numpad4", TKeyEvent.VK_NUMPAD4),
    NUMPAD_5("Numpad5", TKeyEvent.VK_NUMPAD5),
    NUMPAD_6("Numpad6", TKeyEvent.VK_NUMPAD6),
    NUMPAD_7("Numpad7", TKeyEvent.VK_NUMPAD7),
    NUMPAD_8("Numpad8", TKeyEvent.VK_NUMPAD8),
    NUMPAD_9("Numpad9", TKeyEvent.VK_NUMPAD9),
    NUMPAD_MULTIPLY("NumpadMultiply", TKeyEvent.VK_MULTIPLY),
    NUMPAD_ADD("NumpadAdd", TKeyEvent.VK_ADD),
    NUMPAD_SUBTRACT("NumpadSubtract", TKeyEvent.VK_SUBTRACT),
    NUMPAD_DECIMAL("NumpadDecimal", TKeyEvent.VK_DECIMAL),
    NUMPAD_DIVIDE("NumpadDivide", TKeyEvent.VK_DIVIDE),

    // Function Keys
    F1("F1", TKeyEvent.VK_F1),
    F2("F2", TKeyEvent.VK_F2),
    F3("F3", TKeyEvent.VK_F3),
    F4("F4", TKeyEvent.VK_F4),
    F5("F5", TKeyEvent.VK_F5),
    F6("F6", TKeyEvent.VK_F6),
    F7("F7", TKeyEvent.VK_F7),
    F8("F8", TKeyEvent.VK_F8),
    F9("F9", TKeyEvent.VK_F9),
    F10("F10", TKeyEvent.VK_F10),
    F11("F11", TKeyEvent.VK_F11),
    F12("F12", TKeyEvent.VK_F12),

    UNDEFINED("Undefined", TKeyEvent.VK_UNDEFINED);

    private static final EnumLookup.StringKey<KeyboardKey> LOOKUP = new EnumLookup.StringKey<>(values(),
            KeyboardKey::getHtmlKeyname);

    private final String htmlKeyname;
    private final int keyCode;
    private final char baseChar;
    private final char shiftedSymbol;

    KeyboardKey(String htmlKeyname, int keyCode) {
        this(htmlKeyname, keyCode, TKeyEvent.CHAR_UNDEFINED);
    }

    KeyboardKey(String htmlKeyname, int keyCode, char baseChar) {
        this(htmlKeyname, keyCode, baseChar, TKeyEvent.CHAR_UNDEFINED);
    }

    KeyboardKey(String htmlKeyname, int keyCode, char baseChar, char shiftedSymbol) {
        this.htmlKeyname = htmlKeyname;
        this.keyCode = keyCode;
        this.baseChar = baseChar;
        this.shiftedSymbol = shiftedSymbol;
    }

    public char getBaseChar() {
        if (this.baseChar != TKeyEvent.CHAR_UNDEFINED) {
            return this.baseChar;
        }
        return (htmlKeyname.length() == 1) ? htmlKeyname.charAt(0) : '\0';
    }

    public static KeyboardKey lookup(String htmlKeyname) {
        return LOOKUP.getOrDefault(htmlKeyname, UNDEFINED);
    }
}
