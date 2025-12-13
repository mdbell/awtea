package me.mdbell.awtea.input;

import lombok.Getter;
import static me.mdbell.awtea.input.KeyConstants.*;
import me.mdbell.awtea.util.EnumLookup;

@Getter
public enum KeyboardKey {
    // Arrow keys
    ARROW_LEFT("ArrowLeft", VK_LEFT),
    ARROW_UP("ArrowUp", VK_UP),
    ARROW_RIGHT("ArrowRight", VK_RIGHT),
    ARROW_DOWN("ArrowDown", VK_DOWN),

    // Control keys
    BACKSPACE("Backspace", VK_BACK_SPACE),
    TAB("Tab", VK_TAB, '\t'),
    ENTER("Enter", VK_ENTER, '\n'),
    SHIFT_LEFT("ShiftLeft", VK_SHIFT),
    SHIFT_RIGHT("ShiftRight", VK_SHIFT),
    CONTROL_LEFT("ControlLeft", VK_CONTROL),
    CONTROL_RIGHT("ControlRight", VK_CONTROL),
    ALT_LEFT("AltLeft", VK_ALT),
    ALT_RIGHT("AltRight", VK_ALT),
    CAPS_LOCK("CapsLock", VK_CAPS_LOCK),
    ESCAPE("Escape", VK_ESCAPE),
    SPACE("Space", VK_SPACE, ' '),
    PAGE_UP("PageUp", VK_PAGE_UP),
    PAGE_DOWN("PageDown", VK_PAGE_DOWN),
    END("End", VK_END),
    HOME("Home", VK_HOME),
    INSERT("Insert", VK_INSERT),
    DELETE("Delete", VK_DELETE),

    // Numbers
    NUM_1("Digit1", VK_1, '1', '!'),
    NUM_2("Digit2", VK_2, '2', '@'),
    NUM_3("Digit3", VK_3, '3', '#'),
    NUM_4("Digit4", VK_4, '4', '$'),
    NUM_5("Digit5", VK_5, '5', '%'),
    NUM_6("Digit6", VK_6, '6', '^'),
    NUM_7("Digit7", VK_7, '7', '&'),
    NUM_8("Digit8", VK_8, '8', '*'),
    NUM_9("Digit9", VK_9, '9', '('),
    NUM_0("Digit0", VK_0, '0', ')'),

    // Letters
    A("KeyA", VK_A, 'a'),
    B("KeyB", VK_B, 'b'),
    C("KeyC", VK_C, 'c'),
    D("KeyD", VK_D, 'd'),
    E("KeyE", VK_E, 'e'),
    F("KeyF", VK_F, 'f'),
    G("KeyG", VK_G, 'g'),
    H("KeyH", VK_H, 'h'),
    I("KeyI", VK_I, 'i'),
    J("KeyJ", VK_J, 'j'),
    K("KeyK", VK_K, 'k'),
    L("KeyL", VK_L, 'l'),
    M("KeyM", VK_M, 'm'),
    N("KeyN", VK_N, 'n'),
    O("KeyO", VK_O, 'o'),
    P("KeyP", VK_P, 'p'),
    Q("KeyQ", VK_Q, 'q'),
    R("KeyR", VK_R, 'r'),
    S("KeyS", VK_S, 's'),
    T("KeyT", VK_T, 't'),
    U("KeyU", VK_U, 'u'),
    V("KeyV", VK_V, 'v'),
    W("KeyW", VK_W, 'w'),
    X("KeyX", VK_X, 'x'),
    Y("KeyY", VK_Y, 'y'),
    Z("KeyZ", VK_Z, 'z'),

    // Symbols
    MINUS("Minus", VK_MINUS, '-', '_'),
    EQUALS("Equal", VK_EQUALS, '=', '+'),
    OPEN_BRACKET("BracketLeft", VK_OPEN_BRACKET, '[', '{'),
    CLOSE_BRACKET("BracketRight", VK_CLOSE_BRACKET, ']', '}'),
    SEMICOLON("Semicolon", VK_SEMICOLON, ';', ':'),
    BACK_SLASH("Backslash", VK_BACK_SLASH, '\\', '|'),

    QUOTE("Quote", VK_QUOTE, '\'', '"'),
    COMMA("Comma", VK_COMMA, ',', '<'),
    PERIOD("Period", VK_PERIOD, '.', '>'),
    SLASH("Slash", VK_SLASH, '/', '?'),

    // Numpad
    NUMPAD_0("Numpad0", VK_NUMPAD0),
    NUMPAD_1("Numpad1", VK_NUMPAD1),
    NUMPAD_2("Numpad2", VK_NUMPAD2),
    NUMPAD_3("Numpad3", VK_NUMPAD3),
    NUMPAD_4("Numpad4", VK_NUMPAD4),
    NUMPAD_5("Numpad5", VK_NUMPAD5),
    NUMPAD_6("Numpad6", VK_NUMPAD6),
    NUMPAD_7("Numpad7", VK_NUMPAD7),
    NUMPAD_8("Numpad8", VK_NUMPAD8),
    NUMPAD_9("Numpad9", VK_NUMPAD9),
    NUMPAD_MULTIPLY("NumpadMultiply", VK_MULTIPLY),
    NUMPAD_ADD("NumpadAdd", VK_ADD),
    NUMPAD_SUBTRACT("NumpadSubtract", VK_SUBTRACT),
    NUMPAD_DECIMAL("NumpadDecimal", VK_DECIMAL),
    NUMPAD_DIVIDE("NumpadDivide", VK_DIVIDE),

    // Function Keys
    F1("F1", VK_F1),
    F2("F2", VK_F2),
    F3("F3", VK_F3),
    F4("F4", VK_F4),
    F5("F5", VK_F5),
    F6("F6", VK_F6),
    F7("F7", VK_F7),
    F8("F8", VK_F8),
    F9("F9", VK_F9),
    F10("F10", VK_F10),
    F11("F11", VK_F11),
    F12("F12", VK_F12),

    UNDEFINED("Undefined", VK_UNDEFINED);

    private static final EnumLookup.StringKey<KeyboardKey> LOOKUP = new EnumLookup.StringKey<>(values(),
            KeyboardKey::getHtmlKeyname);

    private final String htmlKeyname;
    private final int keyCode;
    private final char baseChar;
    private final char shiftedSymbol;

    KeyboardKey(String htmlKeyname, int keyCode) {
        this(htmlKeyname, keyCode, CHAR_UNDEFINED);
    }

    KeyboardKey(String htmlKeyname, int keyCode, char baseChar) {
        this(htmlKeyname, keyCode, baseChar, CHAR_UNDEFINED);
    }

    KeyboardKey(String htmlKeyname, int keyCode, char baseChar, char shiftedSymbol) {
        this.htmlKeyname = htmlKeyname;
        this.keyCode = keyCode;
        this.baseChar = baseChar;
        this.shiftedSymbol = shiftedSymbol;
    }

    public char getBaseChar() {
        if (this.baseChar != CHAR_UNDEFINED) {
            return this.baseChar;
        }
        return (htmlKeyname.length() == 1) ? htmlKeyname.charAt(0) : '\0';
    }

    public static KeyboardKey lookup(String htmlKeyname) {
        return LOOKUP.getOrDefault(htmlKeyname, UNDEFINED);
    }
}
